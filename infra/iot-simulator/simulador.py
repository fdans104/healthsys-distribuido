"""
Simulador dos monitores multiparamétricos dos leitos (IoT) do HealthSys.

Cada leito tem um "monitor" que, a cada INTERVALO_SEGUNDOS, publica no broker MQTT:
    tópico:  healthsys/leitos/<CODIGO>/sinais
    payload: {"frequenciaCardiaca": 82, "saturacao": 97, "temperatura": 36.8,
              "pressaoSistolica": 120, "pressaoDiastolica": 80, "medidoEm": "2026-11-22T10:00:00Z"}

O monitoring-service recebe essas leituras, mostra os sinais ao vivo e gera alertas.

Para provocar uma piora de propósito (útil na apresentação e nos testes de falha):
    docker exec healthsys-mosquitto mosquitto_pub -t healthsys/simulador/comando -m "piorar UTI-01"
    docker exec healthsys-mosquitto mosquitto_pub -t healthsys/simulador/comando -m "normalizar UTI-01"
"""

import json
import os
import random
import signal
import sys
import threading
import time
from datetime import datetime, timezone

import paho.mqtt.client as mqtt

HOST = os.getenv("MQTT_HOST", "localhost")
PORTA = int(os.getenv("MQTT_PORT", "1883"))
INTERVALO = float(os.getenv("INTERVALO_SEGUNDOS", "3"))
PIORA_AUTOMATICA = os.getenv("PIORA_AUTOMATICA", "true").lower() == "true"
# Chance, a cada leitura, de um leito começar a piorar sozinho (0,2% por leito por leitura).
CHANCE_DE_PIORA = float(os.getenv("CHANCE_DE_PIORA", "0.002"))
# Quantas leituras dura uma piora automática (20 leituras x 3 s = 1 minuto).
DURACAO_DA_PIORA = int(os.getenv("DURACAO_DA_PIORA", "20"))
LEITOS = [c.strip().upper() for c in os.getenv(
    "LEITOS",
    "UTI-01,UTI-02,UTI-03,UTI-04,ENF-01,ENF-02,ENF-03,ENF-04,ENF-05,ENF-06,EMG-01,EMG-02",
).split(",") if c.strip()]

TOPICO_SINAIS = "healthsys/leitos/{}/sinais"
TOPICO_COMANDO = "healthsys/simulador/comando"

# Valores para onde os sinais vão quando o paciente piora.
ALVO_PIORA = {"fc": 138.0, "spo2": 85.0, "temp": 38.9, "pas": 85.0, "pad": 50.0}


def limitar(valor, minimo, maximo):
    return max(minimo, min(maximo, valor))


class Monitor:
    """Um monitor de leito com sinais que variam aos poucos, como num paciente real."""

    def __init__(self, codigo):
        self.codigo = codigo
        self.base = {
            "fc": random.uniform(68, 88),
            "spo2": random.uniform(96, 99),
            "temp": random.uniform(36.3, 37.1),
            "pas": random.uniform(110, 128),
            "pad": random.uniform(70, 82),
        }
        self.atual = dict(self.base)
        self.leituras_piorando = 0  # > 0: está piorando; -1: piora manual sem prazo

    def piorar(self, leituras=None):
        self.leituras_piorando = -1 if leituras is None else leituras

    def normalizar(self):
        self.leituras_piorando = 0

    @property
    def piorando(self):
        return self.leituras_piorando != 0

    def proxima_leitura(self):
        alvo = ALVO_PIORA if self.piorando else self.base
        # Anda 25% em direção ao alvo a cada leitura (piora e melhora graduais) + um pequeno ruído.
        for sinal, ruido in (("fc", 2.0), ("spo2", 0.4), ("temp", 0.05), ("pas", 2.0), ("pad", 1.5)):
            self.atual[sinal] += (alvo[sinal] - self.atual[sinal]) * 0.25 + random.uniform(-ruido, ruido)
        if self.leituras_piorando > 0:
            self.leituras_piorando -= 1

        return {
            "frequenciaCardiaca": int(round(limitar(self.atual["fc"], 30, 200))),
            "saturacao": int(round(limitar(self.atual["spo2"], 70, 100))),
            "temperatura": round(limitar(self.atual["temp"], 33.0, 42.0), 1),
            "pressaoSistolica": int(round(limitar(self.atual["pas"], 60, 220))),
            "pressaoDiastolica": int(round(limitar(self.atual["pad"], 30, 130))),
            "medidoEm": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        }


monitores = {codigo: Monitor(codigo) for codigo in LEITOS}
trava = threading.Lock()
parar = threading.Event()


def ao_conectar(cliente, dados, flags, codigo_motivo, propriedades):
    if codigo_motivo == 0:
        print(f"Conectado ao broker MQTT {HOST}:{PORTA}. Monitores: {', '.join(LEITOS)}", flush=True)
        # Assina de novo a cada (re)conexão.
        cliente.subscribe(TOPICO_COMANDO, qos=0)
    else:
        print(f"Broker recusou a conexão: {codigo_motivo}", flush=True)


def ao_desconectar(cliente, dados, flags, codigo_motivo, propriedades):
    print(f"Desconectado do broker MQTT ({codigo_motivo}). Tentando reconectar...", flush=True)


def ao_receber_comando(cliente, dados, mensagem):
    """Comandos: 'piorar UTI-01' ou 'normalizar UTI-01'."""
    try:
        partes = mensagem.payload.decode("utf-8").split()
        if len(partes) != 2:
            raise ValueError("use: piorar <LEITO> ou normalizar <LEITO>")
        acao, codigo = partes[0].lower(), partes[1].upper()
        with trava:
            monitor = monitores.get(codigo)
            if monitor is None:
                raise ValueError(f"leito {codigo} não existe no simulador")
            if acao == "piorar":
                monitor.piorar()
            elif acao == "normalizar":
                monitor.normalizar()
            else:
                raise ValueError(f"ação '{acao}' desconhecida")
        print(f"Comando recebido: {acao} {codigo}", flush=True)
    except Exception as erro:  # um comando errado não pode derrubar o simulador
        print(f"Comando ignorado: {erro}", flush=True)


def encerrar(*_):
    parar.set()


def main():
    cliente = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2,
                          client_id=f"simulador-leitos-{random.randint(1000, 9999)}")
    cliente.on_connect = ao_conectar
    cliente.on_disconnect = ao_desconectar
    cliente.on_message = ao_receber_comando
    # Se o broker cair, tenta de novo de 1 em 1 segundo até no máximo 10 s entre tentativas.
    cliente.reconnect_delay_set(min_delay=1, max_delay=10)
    cliente.connect_async(HOST, PORTA, keepalive=30)
    cliente.loop_start()  # a reconexão automática roda nesta thread

    signal.signal(signal.SIGTERM, encerrar)
    signal.signal(signal.SIGINT, encerrar)

    while not parar.is_set():
        with trava:
            for codigo, monitor in monitores.items():
                if PIORA_AUTOMATICA and not monitor.piorando and random.random() < CHANCE_DE_PIORA:
                    monitor.piorar(DURACAO_DA_PIORA)
                    print(f"Piora automática simulada no leito {codigo}", flush=True)
                leitura = monitor.proxima_leitura()
                if cliente.is_connected():
                    cliente.publish(TOPICO_SINAIS.format(codigo), json.dumps(leitura), qos=0)
        parar.wait(INTERVALO)

    cliente.loop_stop()
    cliente.disconnect()
    print("Simulador encerrado.", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
