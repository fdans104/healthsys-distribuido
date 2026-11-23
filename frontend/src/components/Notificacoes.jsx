import { useEffect, useRef, useState } from 'react';
import useNotificacoes from '../hooks/useNotificacoes.jsx';
import { tempoDesde } from '../formatos.js';

const TEMPO_DO_AVISO = 8000;

/**
 * Sino de notificações no topo da tela + avisos que aparecem no canto (os de ATENÇÃO e CRÍTICO).
 * Os avisos críticos só somem quando a pessoa fecha: não dá para perder um alerta grave.
 */
export default function Notificacoes({ irPara }) {
  const [aberto, setAberto] = useState(false);
  const [avisos, setAvisos] = useState([]);
  const caixa = useRef(null);

  const { lista, naoLidas, aoVivo, marcarLida, marcarTodas } = useNotificacoes(true, (n) => {
    if (n.gravidade === 'INFO') return;
    setAvisos((atual) => [n, ...atual.filter((x) => x.id !== n.id)].slice(0, 4));
    if (n.gravidade !== 'CRITICO') {
      setTimeout(() => setAvisos((atual) => atual.filter((x) => x.id !== n.id)), TEMPO_DO_AVISO);
    }
  });

  // Fecha a lista ao clicar fora dela.
  useEffect(() => {
    if (!aberto) return undefined;
    function clique(e) {
      if (caixa.current && !caixa.current.contains(e.target)) setAberto(false);
    }
    document.addEventListener('mousedown', clique);
    return () => document.removeEventListener('mousedown', clique);
  }, [aberto]);

  function abrir(n) {
    marcarLida(n);
    setAvisos((atual) => atual.filter((x) => x.id !== n.id));
    setAberto(false);
    if (n.secao) irPara(n.secao);
  }

  return (
    <div className="notificacoes" ref={caixa}>
      <button className="botao botao-leve sino" onClick={() => setAberto((a) => !a)}
              aria-label={`Notificações: ${naoLidas} não lidas`} aria-expanded={aberto}>
        <span aria-hidden="true">🔔</span>
        {naoLidas > 0 && <span className="sino-contagem">{naoLidas > 99 ? '99+' : naoLidas}</span>}
      </button>

      {aberto && (
        <div className="notificacoes-lista" role="dialog" aria-label="Notificações">
          <div className="notificacoes-topo">
            <strong>Notificações</strong>
            <span className={`ao-vivo ${aoVivo ? 'ao-vivo-ok' : ''}`}
                  title={aoVivo ? 'Recebendo em tempo real' : 'Reconectando; a lista é atualizada a cada 30 s'}>
              {aoVivo ? 'ao vivo' : 'reconectando'}
            </span>
            {naoLidas > 0 && (
              <button className="botao-link" onClick={marcarTodas}>Marcar todas como lidas</button>
            )}
          </div>
          {lista.length === 0 && <p className="vazio">Nenhuma notificação por enquanto.</p>}
          <ul>
            {lista.map((n) => (
              <li key={n.id}>
                <button className={`notificacao gravidade-${n.gravidade.toLowerCase()} ${n.lida ? 'notificacao-lida' : ''}`}
                        onClick={() => abrir(n)}>
                  <span className="notificacao-titulo">{n.titulo}</span>
                  <span className="notificacao-texto">{n.mensagem}</span>
                  <span className="linha-secundaria">{tempoDesde(n.criadaEm)}</span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}

      {avisos.length > 0 && (
        <div className="avisos-flutuantes" aria-live="assertive">
          {avisos.map((n) => (
            <div key={n.id} className={`aviso-flutuante gravidade-${n.gravidade.toLowerCase()}`} role="alert">
              <button className="aviso-conteudo" onClick={() => abrir(n)}>
                <strong>{n.titulo}</strong>
                <span>{n.mensagem}</span>
              </button>
              <button className="aviso-fechar" aria-label="Fechar aviso"
                      onClick={() => setAvisos((atual) => atual.filter((x) => x.id !== n.id))}>✕</button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
