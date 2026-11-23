import { useEffect, useState } from 'react';
import { leitosApi, monitoramentoApi, pacientesApi, triagemApi, usuariosApi } from '../api.js';
import { ROTULO_PERFIL } from '../formatos.js';
import { podeGerenciarUsuarios, podeMonitorar, podeVerTriagens } from '../permissoes.js';

// O que cada perfil faz no sistema, como num hospital.
const PAPEIS = {
  ADMIN: [
    'Cadastra e gerencia os usuários do sistema.',
    'Cadastra os leitos do hospital e acompanha a ocupação.',
    'Consulta os dados cadastrais dos pacientes.',
    'Não acessa dados clínicos (prontuário e triagem).',
  ],
  RECEPCIONISTA: [
    'Cadastra e atualiza os dados dos pacientes.',
    'Abre a triagem quando o paciente chega ao pronto-atendimento.',
    'Acompanha a fila e a ocupação dos leitos, sem ver os dados clínicos.',
  ],
  ENFERMEIRO: [
    'Avalia o paciente na triagem e classifica o risco.',
    'Registra vacinas aplicadas e alergias.',
    'Consulta o prontuário e registra resultados de exames.',
    'Interna pacientes nos leitos, acompanha os sinais vitais e confirma a higienização.',
  ],
  MEDICO: [
    'Chama o paciente da fila, atende e finaliza a triagem.',
    'Registra consultas e atendimentos, solicita exames e prescreve medicamentos.',
    'Registra alergias e consulta todo o histórico clínico.',
    'Acompanha os monitores dos leitos e é avisado quando um paciente piora.',
  ],
};

/** RF19 (início): resumo do dia de acordo com o perfil, com atalhos para as seções. */
export default function InicioPagina({ usuarioLogado, irPara }) {
  const perfil = usuarioLogado.perfil;
  const [dados, setDados] = useState({ carregando: true });

  useEffect(() => {
    let ativo = true;
    Promise.allSettled([
      pacientesApi.listar(),
      podeVerTriagens(perfil) ? triagemApi.listar() : Promise.resolve(null),
      podeGerenciarUsuarios(perfil) ? usuariosApi.listar() : Promise.resolve(null),
      leitosApi.ocupacao(),
      podeMonitorar(perfil) ? monitoramentoApi.alertas(true) : Promise.resolve(null),
    ]).then((resultados) => {
      if (!ativo) return;
      const [pacientes, triagens, usuarios, ocupacao, alertas] = resultados.map(
        (r) => (r.status === 'fulfilled' ? r.value : null));
      setDados({
        carregando: false,
        pacientes, triagens, usuarios, ocupacao, alertas,
        falhou: resultados.some((r) => r.status === 'rejected'),
      });
    });
    return () => { ativo = false; };
  }, [perfil]);

  const cartoes = montarCartoes(perfil, dados);

  return (
    <main className="inicio">
      <section className="cartao inicio-boas-vindas">
        <h2>Olá, {usuarioLogado.nome.split(' ')[0]}</h2>
        <p className="linha-secundaria">Você entrou como {ROTULO_PERFIL[perfil] ?? perfil}. No HealthSys, o seu perfil:</p>
        <ul className="lista-papel">
          {(PAPEIS[perfil] ?? []).map((p) => <li key={p}>{p}</li>)}
        </ul>
      </section>

      {dados.falhou && (
        <p className="nota">Alguns números não puderam ser carregados agora: um dos serviços não respondeu.</p>
      )}

      <section className="painel">
        {cartoes.map((c) => (
          <button key={c.titulo} className={`cartao painel-cartao ${c.destaque ? `painel-${c.destaque}` : ''}`}
                  onClick={() => c.ir && irPara(c.ir)} disabled={!c.ir}>
            <span className="painel-numero">{dados.carregando ? '…' : (c.valor ?? '—')}</span>
            <span className="painel-titulo">{c.titulo}</span>
            {c.detalhe && <span className="linha-secundaria">{c.detalhe}</span>}
          </button>
        ))}
      </section>
    </main>
  );
}

function contar(lista, filtro) {
  return lista ? lista.filter(filtro).length : null;
}

function montarCartoes(perfil, dados) {
  const lista = montarCartoesDoPerfil(perfil, dados);
  const { ocupacao, alertas } = dados;
  if (ocupacao) {
    const ocupados = ocupacao.reduce((t, o) => t + o.ocupados, 0);
    const total = ocupacao.reduce((t, o) => t + o.total, 0);
    const uti = ocupacao.find((o) => o.setor === 'UTI');
    lista.push({ titulo: 'Leitos ocupados', valor: `${ocupados}/${total}`, ir: 'leitos',
      detalhe: uti ? `UTI com ${uti.taxaOcupacao}% de ocupação` : null,
      destaque: uti && uti.taxaOcupacao >= 90 ? 'alerta' : null });
  }
  if (alertas) {
    const criticos = alertas.filter((a) => a.gravidade === 'CRITICO').length;
    lista.push({ titulo: 'Alertas dos monitores', valor: alertas.length, ir: 'leitos',
      detalhe: criticos ? `${criticos} crítico(s) sem reconhecimento` : 'Sinais vitais dos leitos',
      destaque: alertas.length ? 'alerta' : null });
  }
  return lista;
}

function montarCartoesDoPerfil(perfil, { pacientes, triagens, usuarios }) {
  const totalPacientes = { titulo: 'Pacientes cadastrados', valor: pacientes?.length, ir: 'pacientes' };
  const semAvaliacao = contar(triagens, (t) => t.status === 'PENDENTE' && !t.classificacaoRisco);
  const aguardandoMedico = contar(triagens, (t) => t.status === 'PENDENTE' && t.classificacaoRisco);
  const graves = contar(triagens, (t) => t.status === 'PENDENTE' && ['CRITICO', 'ALTO'].includes(t.classificacaoRisco));
  const emAtendimento = contar(triagens, (t) => t.status === 'EM_ATENDIMENTO');
  const naFila = contar(triagens, (t) => t.status === 'PENDENTE');

  if (perfil === 'ADMIN') {
    const porPerfil = (p) => contar(usuarios, (u) => u.perfil === p);
    return [
      { titulo: 'Usuários do sistema', valor: usuarios?.length, ir: 'usuarios',
        detalhe: usuarios ? `${porPerfil('MEDICO')} médicos · ${porPerfil('ENFERMEIRO')} enfermeiros · ${porPerfil('RECEPCIONISTA')} recepção` : null },
      totalPacientes,
    ];
  }
  if (perfil === 'RECEPCIONISTA') {
    return [
      totalPacientes,
      { titulo: 'Na fila da triagem', valor: naFila, ir: 'triagem' },
      { titulo: 'Em atendimento', valor: emAtendimento, ir: 'triagem' },
    ];
  }
  if (perfil === 'ENFERMEIRO') {
    return [
      { titulo: 'Aguardando avaliação', valor: semAvaliacao, ir: 'triagem', destaque: semAvaliacao ? 'alerta' : null,
        detalhe: 'Pacientes sem classificação de risco' },
      { titulo: 'Aguardando o médico', valor: aguardandoMedico, ir: 'triagem' },
      totalPacientes,
    ];
  }
  // MEDICO
  return [
    { titulo: 'Aguardando atendimento', valor: aguardandoMedico, ir: 'triagem',
      detalhe: graves ? `${graves} com risco alto ou crítico` : 'Já classificados pela enfermagem',
      destaque: graves ? 'alerta' : null },
    { titulo: 'Em atendimento', valor: emAtendimento, ir: 'triagem' },
    totalPacientes,
  ];
}
