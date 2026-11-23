import { useCallback, useEffect, useRef, useState } from 'react';
import { notificacoesApi } from '../api.js';

const ESPERA_MINIMA = 2000;
const ESPERA_MAXIMA = 30000;
const CONSULTA_DE_SEGURANCA = 30000;

/**
 * Notificações da pessoa logada.
 * - Tempo real: mantém o canal SSE aberto; se cair, reconecta sozinho (2 s, 4 s, 8 s... até 30 s).
 * - Segurança: a cada 30 s confere a lista pelo jeito normal, caso algum aviso tenha chegado com o canal fora.
 * - aoChegar(notificacao) é chamado para cada aviso novo recebido em tempo real (usado para os alertas na tela).
 */
export default function useNotificacoes(ativo, aoChegar) {
  const [lista, setLista] = useState([]);
  const [naoLidas, setNaoLidas] = useState(0);
  const [aoVivo, setAoVivo] = useState(false);
  const aoChegarRef = useRef(aoChegar);
  aoChegarRef.current = aoChegar;

  const recarregar = useCallback(async () => {
    try {
      const [itens, contagem] = await Promise.all([notificacoesApi.listar(), notificacoesApi.naoLidas()]);
      setLista(itens);
      setNaoLidas(contagem.total);
    } catch {
      // serviço de notificações fora do ar: o resto do sistema continua funcionando
    }
  }, []);

  useEffect(() => {
    if (!ativo) return undefined;
    let encerrado = false;
    const controle = new AbortController();
    let espera = ESPERA_MINIMA;
    let temporizador = null;

    async function conectar() {
      try {
        await notificacoesApi.ouvir({
          sinal: controle.signal,
          aoConectar: () => {
            espera = ESPERA_MINIMA;
            setAoVivo(true);
            recarregar();
          },
          aoReceber: (n) => {
            setLista((atual) => [n, ...atual.filter((x) => x.id !== n.id)].slice(0, 50));
            setNaoLidas((total) => total + 1);
            aoChegarRef.current?.(n);
          },
        });
      } catch (e) {
        if (e?.status === 401) return; // sessão encerrada: o App volta para o login
      }
      if (encerrado) return;
      setAoVivo(false);
      temporizador = setTimeout(conectar, espera);
      espera = Math.min(espera * 2, ESPERA_MAXIMA);
    }

    recarregar();
    conectar();
    const consulta = setInterval(recarregar, CONSULTA_DE_SEGURANCA);
    return () => {
      encerrado = true;
      controle.abort();
      clearTimeout(temporizador);
      clearInterval(consulta);
    };
  }, [ativo, recarregar]);

  async function marcarLida(n) {
    if (n.lida) return;
    setLista((atual) => atual.map((x) => (x.id === n.id ? { ...x, lida: true } : x)));
    setNaoLidas((total) => Math.max(0, total - 1));
    try {
      await notificacoesApi.marcarLida(n.id);
    } catch {
      recarregar();
    }
  }

  async function marcarTodas() {
    setLista((atual) => atual.map((x) => ({ ...x, lida: true })));
    setNaoLidas(0);
    try {
      await notificacoesApi.marcarTodas();
    } catch {
      recarregar();
    }
  }

  return { lista, naoLidas, aoVivo, marcarLida, marcarTodas, recarregar };
}
