import { formatarDataHora, tempoDesde } from '../../formatos.js';

const SINAIS = [
  { id: 'frequenciaCardiaca', rotulo: 'FC', unidade: 'bpm', valor: (l) => l.frequenciaCardiaca },
  { id: 'saturacao', rotulo: 'SpO₂', unidade: '%', valor: (l) => l.saturacao },
  { id: 'temperatura', rotulo: 'Temp.', unidade: '°C',
    valor: (l) => (l.temperatura == null ? null : Number(l.temperatura).toFixed(1).replace('.', ',')) },
  { id: 'pressao', rotulo: 'PA', unidade: 'mmHg',
    valor: (l) => (l.pressaoSistolica == null ? null : `${l.pressaoSistolica}/${l.pressaoDiastolica ?? '–'}`) },
];

/** Um leito ocupado com os sinais vitais que o monitor mandou por último (atualiza a cada 3 s). */
export function MonitorLeito({ monitor }) {
  const l = monitor.leitura;
  const pior = Object.values(monitor.estados ?? {}).includes('CRITICO')
    ? 'critico'
    : Object.values(monitor.estados ?? {}).includes('ATENCAO') ? 'atencao' : 'normal';

  return (
    <li className={`monitor monitor-${monitor.semSinal ? 'sem-sinal' : pior}`}>
      <div className="monitor-topo">
        <strong>{monitor.codigo}</strong>
        <span className="monitor-paciente">{monitor.pacienteNome}</span>
        {monitor.alertasAtivos > 0 && <span className="risco risco-critico">{monitor.alertasAtivos} alerta(s)</span>}
      </div>
      {monitor.semSinal ? (
        <p className="monitor-aviso">
          Sem sinal do monitor{monitor.recebidaEm ? ` desde ${formatarDataHora(monitor.recebidaEm)}` : ''}. Verifique o aparelho.
        </p>
      ) : (
        <div className="sinais">
          {SINAIS.map((s) => {
            const valor = l ? s.valor(l) : null;
            const estado = (monitor.estados?.[s.id] ?? 'NORMAL').toLowerCase();
            return (
              <div key={s.id} className={`sinal sinal-${estado}`}>
                <span className="sinal-rotulo">{s.rotulo}</span>
                <span className="sinal-valor">{valor ?? '–'}</span>
                <span className="sinal-unidade">{s.unidade}</span>
              </div>
            );
          })}
        </div>
      )}
      <p className="linha-secundaria">
        Internado {tempoDesde(monitor.ocupadoEm)}{monitor.recebidaEm && !monitor.semSinal ? ' · ao vivo' : ''}
      </p>
    </li>
  );
}

/** Alertas que ainda ninguém reconheceu, do mais recente para o mais antigo. */
export function AlertasAtivos({ alertas, onReconhecer }) {
  if (alertas.length === 0) {
    return <p className="vazio">Nenhum alerta aberto.</p>;
  }
  return (
    <ul className="registros">
      {alertas.map((a) => (
        <li key={a.id} className={`alerta alerta-${a.gravidade.toLowerCase()}`}>
          <div className="registro-topo">
            <span className="registro-titulo">
              {a.gravidade === 'CRITICO' ? 'CRÍTICO' : 'Atenção'} · leito {a.leitoCodigo} · {a.pacienteNome}
            </span>
            <span className="linha-secundaria">{tempoDesde(a.criadoEm)}</span>
          </div>
          <p>{a.mensagem.charAt(0).toUpperCase() + a.mensagem.slice(1)}.</p>
          <div className="acoes-linha">
            <button className="botao botao-leve" onClick={() => onReconhecer(a)}>Estou cuidando (reconhecer)</button>
          </div>
        </li>
      ))}
    </ul>
  );
}
