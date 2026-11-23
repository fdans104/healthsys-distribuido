package com.healthsys.monitoring.sinais;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * IoT: assina o tópico MQTT "healthsys/leitos/+/sinais", onde cada monitor de leito publica suas leituras.
 * - Se o broker MQTT (Mosquitto) estiver fora do ar, tenta conectar de novo a cada 5 segundos, sem travar o serviço.
 * - Uma mensagem com defeito é descartada e registrada no log, sem derrubar a conexão.
 */
@Component
public class ReceptorMqtt implements MqttCallback, DisposableBean {

    public static final String TOPICO = "healthsys/leitos/+/sinais";

    private static final Logger log = LoggerFactory.getLogger(ReceptorMqtt.class);

    private final MqttClient cliente;
    private final MqttConnectOptions opcoes;
    private final MonitoramentoService service;
    private final ObjectMapper mapper;
    private final String url;

    public ReceptorMqtt(@Value("${mqtt.url:tcp://localhost:1883}") String url, MonitoramentoService service,
                        ObjectMapper mapper, MeterRegistry metricas) throws MqttException {
        this.url = url;
        this.service = service;
        this.mapper = mapper;
        this.cliente = new MqttClient(url, "monitoring-service-" + UUID.randomUUID(), new MemoryPersistence());
        this.cliente.setCallback(this);
        this.opcoes = new MqttConnectOptions();
        this.opcoes.setCleanSession(true);
        this.opcoes.setAutomaticReconnect(false);
        this.opcoes.setConnectionTimeout(5);
        this.opcoes.setKeepAliveInterval(20);
        Gauge.builder("healthsys.mqtt.conectado", () -> conectado() ? 1 : 0)
                .description("1 quando o serviço está recebendo os monitores dos leitos (MQTT), 0 quando não")
                .register(metricas);
    }

    public boolean conectado() {
        return cliente.isConnected();
    }

    /** Conecta (ou reconecta) ao broker e assina o tópico dos monitores. */
    @Scheduled(fixedDelay = 5000, initialDelay = 2000)
    public synchronized void manterConectado() {
        if (cliente.isConnected()) {
            return;
        }
        try {
            cliente.connect(opcoes);
            cliente.subscribe(TOPICO, 0);
            log.info("Conectado ao broker MQTT {} e recebendo o tópico {}", url, TOPICO);
        } catch (MqttException e) {
            log.warn("Broker MQTT {} indisponível ({}). Nova tentativa em 5 segundos.", url, e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable causa) {
        log.warn("Conexão com o broker MQTT perdida: {}. Reconectando...", causa == null ? "?" : causa.getMessage());
    }

    @Override
    public void messageArrived(String topico, MqttMessage mensagem) {
        String codigo = codigoDoLeito(topico);
        if (codigo == null) {
            return;
        }
        try {
            LeituraSinais leitura = mapper.readValue(mensagem.getPayload(), LeituraSinais.class);
            service.registrarLeitura(codigo, leitura);
        } catch (Exception e) {
            // Nunca deixar a exceção sair daqui: no Paho, isso derrubaria a conexão.
            log.warn("Leitura do leito {} descartada: {}", codigo, e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // este serviço só recebe mensagens
    }

    @Override
    public void destroy() {
        try {
            if (cliente.isConnected()) {
                cliente.disconnect();
            }
            cliente.close();
        } catch (MqttException e) {
            log.debug("Erro ao fechar a conexão MQTT: {}", e.getMessage());
        }
    }

    /** "healthsys/leitos/UTI-01/sinais" → "UTI-01". Qualquer outro formato é ignorado. */
    static String codigoDoLeito(String topico) {
        if (topico == null) {
            return null;
        }
        String[] partes = topico.split("/");
        if (partes.length != 4 || !"healthsys".equals(partes[0]) || !"leitos".equals(partes[1])
                || !"sinais".equals(partes[3]) || partes[2].isBlank()) {
            return null;
        }
        return partes[2].toUpperCase(java.util.Locale.ROOT);
    }
}
