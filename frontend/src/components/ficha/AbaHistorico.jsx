import { useEffect, useState } from 'react';
import { fichaApi, prontuarioApi, triagemApi } from '../../api.js';
import { ROTULO_RISCO, ROTULO_STATUS_TRIAGEM, formatarDataHora } from '../../formatos.js';
import { Assinatura, ListaRegistros } from './Registros.jsx';

const ROTULO_CATEGORIA = {
  TRIAGEM: 'Triagem',
  ATENDIMENTO: 'Atendimento',
  CONSULTA: 'Consulta',
  EXAME: 'Exame',
  MEDICAMENTO: 'Medicamento',
  VACINA: 'Vacina',
  ALERGIA: 'Alergia',
};

/**
 * RF07/RF10: tudo o que já aconteceu com o paciente, do mais recente para o mais antigo.
 * Junta três microsserviços: pacientes (atendimentos, vacinas, alergias), prontuário (consultas,
 * exames, medicamentos) e triagem. Se um deles não responder, mostra o resto e avisa (RNF01).
 */
export default function AbaHistorico({ pacienteId }) {
  const [itens, setItens] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [falhas, setFalhas] = useState([]);

  useEffect(() => {
    let ativo = true;
    setCarregando(true);
    Promise.allSettled([
      fichaApi.historico(pacienteId),
      prontuarioApi.completo(pacienteId),
      triagemApi.listar({ pacienteId }),
    ]).then(([clinico, prontuario, triagens]) => {
      if (!ativo) return;
      const todos = [];
      const falhou = [];
      if (clinico.status === 'fulfilled') todos.push(...clinico.value);
      else falhou.push('atendimentos, vacinas e alergias');
      if (prontuario.status === 'fulfilled') todos.push(...doProntuario(prontuario.value));
      else falhou.push('prontuário (consultas, exames e medicamentos)');
      if (triagens.status === 'fulfilled') todos.push(...dasTriagens(triagens.value));
      else falhou.push('triagens');
      todos.sort((a, b) => String(b.data ?? '').localeCompare(String(a.data ?? '')));
      setItens(todos);
      setFalhas(falhou);
      setCarregando(false);
    });
    return () => { ativo = false; };
  }, [pacienteId]);

  return (
    <>
      <h3 className="titulo-aba">Histórico clínico</h3>
      {falhas.length > 0 && (
        <p className="nota">Não foi possível carregar agora: {falhas.join('; ')}. O restante do histórico aparece abaixo.</p>
      )}
      <ListaRegistros itens={itens} carregando={carregando} vazio="Nenhum registro clínico para este paciente ainda.">
        {itens.map((item) => (
          <li key={`${item.categoria}-${item.id}-${item.data}`} className="registro">
            <div className="registro-topo">
              <span className={`etiqueta etiqueta-${item.categoria.toLowerCase()}`}>
                {ROTULO_CATEGORIA[item.categoria] ?? item.categoria}
              </span>
              <span className="num">{formatarDataHora(item.data)}</span>
            </div>
            <p className="registro-titulo">{item.titulo}</p>
            {item.detalhe && <p className="registro-texto">{item.detalhe}</p>}
            <Assinatura nome={item.registradoPorNome} perfil={item.registradoPorPerfil} />
          </li>
        ))}
      </ListaRegistros>
    </>
  );
}

function doProntuario({ consultas = [], exames = [], medicamentos = [] }) {
  const itens = [];
  for (const c of consultas) {
    itens.push({
      categoria: 'CONSULTA', id: c.id, data: c.data, titulo: `Consulta: ${c.motivo}`,
      detalhe: [c.diagnostico && `Diagnóstico: ${c.diagnostico}`, c.conduta && `Conduta: ${c.conduta}`].filter(Boolean).join('\n') || null,
      registradoPorNome: c.registradoPorNome, registradoPorPerfil: c.registradoPorPerfil,
    });
  }
  for (const e of exames) {
    itens.push({
      categoria: 'EXAME', id: `${e.id}-s`, data: e.solicitadoEm, titulo: `Exame solicitado: ${e.nome}`,
      detalhe: e.observacoes, registradoPorNome: e.solicitadoPorNome, registradoPorPerfil: e.solicitadoPorPerfil,
    });
    if (e.status === 'CONCLUIDO') {
      itens.push({
        categoria: 'EXAME', id: `${e.id}-r`, data: e.resultadoRegistradoEm, titulo: `Resultado: ${e.nome}`,
        detalhe: e.resultado, registradoPorNome: e.resultadoRegistradoPorNome, registradoPorPerfil: e.resultadoRegistradoPorPerfil,
      });
    }
  }
  for (const m of medicamentos) {
    itens.push({
      categoria: 'MEDICAMENTO', id: `${m.id}-p`, data: m.prescritoEm, titulo: `Prescrição: ${m.nome} ${m.dosagem}`,
      detalhe: m.frequencia, registradoPorNome: m.prescritoPorNome, registradoPorPerfil: m.prescritoPorPerfil,
    });
    if (m.status === 'SUSPENSO') {
      itens.push({
        categoria: 'MEDICAMENTO', id: `${m.id}-s`, data: m.suspensoEm, titulo: `Suspenso: ${m.nome}`,
        detalhe: m.motivoSuspensao, registradoPorNome: m.suspensoPorNome, registradoPorPerfil: 'MEDICO',
      });
    }
  }
  return itens;
}

function dasTriagens(triagens) {
  return triagens.map((t) => ({
    categoria: 'TRIAGEM', id: t.id, data: t.abertaEm, titulo: `Triagem: ${t.queixaPrincipal}`,
    detalhe: [
      t.classificacaoRisco ? `Risco ${ROTULO_RISCO[t.classificacaoRisco].toLowerCase()}` : 'Sem classificação de risco',
      ROTULO_STATUS_TRIAGEM[t.status],
    ].join(' · '),
    registradoPorNome: t.abertaPorNome, registradoPorPerfil: t.abertaPorPerfil,
  }));
}
