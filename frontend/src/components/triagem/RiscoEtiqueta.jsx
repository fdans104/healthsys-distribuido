import { ROTULO_RISCO, ROTULO_STATUS_TRIAGEM } from '../../formatos.js';

/** Etiqueta colorida da classificação de risco (cores do protocolo de Manchester). */
export function RiscoEtiqueta({ risco }) {
  if (!risco) return <span className="risco risco-sem">Sem classificação</span>;
  return <span className={`risco risco-${risco.toLowerCase()}`}>{ROTULO_RISCO[risco] ?? risco}</span>;
}

export function StatusEtiqueta({ status }) {
  return <span className={`etiqueta status-${status.toLowerCase()}`}>{ROTULO_STATUS_TRIAGEM[status] ?? status}</span>;
}
