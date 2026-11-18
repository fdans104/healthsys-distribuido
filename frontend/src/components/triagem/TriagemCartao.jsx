import { formatarDataHora, tempoDesde } from '../../formatos.js';
import {
  podeAtenderTriagem, podeAvaliarTriagem, podeCancelarTriagem, podeVerDadosDaTriagem,
} from '../../permissoes.js';
import { RiscoEtiqueta, StatusEtiqueta } from './RiscoEtiqueta.jsx';

/** Um paciente na fila da triagem, com os botões que o perfil logado pode usar. */
export default function TriagemCartao({
  triagem: t, perfil, onAvaliar, onMudarStatus, mostrarPaciente = true, somenteLeitura = false,
}) {
  const verDados = podeVerDadosDaTriagem(perfil);
  const encerrada = t.status === 'FINALIZADA' || t.status === 'CANCELADA';
  const sinais = [
    t.pressaoArterial && `PA ${t.pressaoArterial}`,
    t.temperatura != null && `${String(t.temperatura).replace('.', ',')} °C`,
    t.frequenciaCardiaca != null && `FC ${t.frequenciaCardiaca} bpm`,
    t.saturacao != null && `SpO₂ ${t.saturacao}%`,
  ].filter(Boolean);

  return (
    <li className={`triagem-cartao triagem-${(t.classificacaoRisco ?? 'sem').toLowerCase()}`}>
      <div className="registro-topo">
        <div className="triagem-titulo">
          <RiscoEtiqueta risco={t.classificacaoRisco} />
          {mostrarPaciente && <strong>{t.pacienteNome ?? `Paciente #${t.pacienteId}`}</strong>}
          <StatusEtiqueta status={t.status} />
        </div>
        <span className="num" title={formatarDataHora(t.abertaEm)}>
          {encerrada ? formatarDataHora(t.atualizadaEm) : `chegou ${tempoDesde(t.abertaEm)}`}
        </span>
      </div>

      <p className="registro-texto">Queixa: {t.queixaPrincipal}</p>
      {verDados && t.sintomas && <p className="registro-texto">Sintomas: {t.sintomas}</p>}
      {verDados && sinais.length > 0 && <p className="linha-secundaria">{sinais.join(' · ')}</p>}
      <p className="linha-secundaria">
        Aberta por {t.abertaPorNome}{t.avaliadaPorNome ? ` · avaliada por ${t.avaliadaPorNome}` : ''}
        {encerrada && t.atualizadaPorNome ? ` · encerrada por ${t.atualizadaPorNome}` : ''}
      </p>

      {!encerrada && !somenteLeitura && (
        <div className="acoes acoes-cartao">
          {podeAvaliarTriagem(perfil) && t.status === 'PENDENTE' && (
            <button className="botao" onClick={() => onAvaliar(t)}>
              {t.classificacaoRisco ? 'Reavaliar' : 'Avaliar e classificar'}
            </button>
          )}
          {podeAtenderTriagem(perfil) && t.status === 'PENDENTE' && t.classificacaoRisco && (
            <button className="botao" onClick={() => onMudarStatus(t, 'EM_ATENDIMENTO')}>Chamar para atendimento</button>
          )}
          {podeAtenderTriagem(perfil) && t.status === 'EM_ATENDIMENTO' && (
            <>
              <button className="botao" onClick={() => onMudarStatus(t, 'FINALIZADA')}>Finalizar</button>
              <button className="botao botao-leve" onClick={() => onMudarStatus(t, 'PENDENTE')}>Devolver à fila</button>
            </>
          )}
          {podeCancelarTriagem(perfil) && (
            <button className="botao botao-perigo" onClick={() => onMudarStatus(t, 'CANCELADA')}>Cancelar</button>
          )}
        </div>
      )}
    </li>
  );
}
