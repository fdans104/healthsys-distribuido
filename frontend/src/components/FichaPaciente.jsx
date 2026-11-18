import { useState } from 'react';
import useAvisos, { Faixas } from '../hooks/useAvisos.jsx';
import { podeVerClinico, podeVerTriagens } from '../permissoes.js';
import { ROTULO_SEXO, formatarCpf, formatarData } from '../formatos.js';
import AbaDados from './ficha/AbaDados.jsx';
import AbaHistorico from './ficha/AbaHistorico.jsx';
import AbaAlergias from './ficha/AbaAlergias.jsx';
import AbaVacinas from './ficha/AbaVacinas.jsx';
import AbaAtendimentos from './ficha/AbaAtendimentos.jsx';
import AbaConsultas from './ficha/AbaConsultas.jsx';
import AbaExames from './ficha/AbaExames.jsx';
import AbaMedicamentos from './ficha/AbaMedicamentos.jsx';
import AbaTriagens from './ficha/AbaTriagens.jsx';

// As abas clínicas só existem para médicos e enfermeiros (dado clínico é sigiloso).
const ABAS_CLINICAS = [
  { id: 'historico', rotulo: 'Histórico' },
  { id: 'consultas', rotulo: 'Consultas' },
  { id: 'exames', rotulo: 'Exames' },
  { id: 'medicamentos', rotulo: 'Medicamentos' },
  { id: 'alergias', rotulo: 'Alergias' },
  { id: 'vacinas', rotulo: 'Vacinas' },
  { id: 'atendimentos', rotulo: 'Atendimentos' },
];

/** Ficha do paciente: dados cadastrais e, para médicos e enfermeiros, o prontuário. */
export default function FichaPaciente({ paciente, usuarioLogado, onVoltar, onEditar }) {
  const perfil = usuarioLogado.perfil;
  const verClinico = podeVerClinico(perfil);
  const abas = [
    { id: 'dados', rotulo: 'Dados' },
    ...(verClinico ? ABAS_CLINICAS : []),
    ...(podeVerTriagens(perfil) ? [{ id: 'triagens', rotulo: 'Triagens' }] : []),
  ];
  const [aba, setAba] = useState(verClinico ? 'historico' : 'dados');
  const { aviso, erro, setErro, mostrarAviso } = useAvisos();

  function trocarAba(id) {
    setErro('');
    setAba(id);
  }

  // O que cada aba clínica recebe: qual paciente, quem está logado e como avisar a tela.
  const propsAba = { pacienteId: paciente.id, paciente, perfil, mostrarAviso, setErro };

  return (
    <>
      <Faixas aviso={aviso} erro={erro} />

      <main className="ficha">
        <div className="ficha-topo">
          <button className="botao botao-leve" onClick={onVoltar}>← Voltar</button>
          <div className="ficha-titulo">
            <h2>{paciente.nome}</h2>
            <p className="linha-secundaria">
              {[
                paciente.cpf ? `CPF ${formatarCpf(paciente.cpf)}` : null,
                `nasc. ${formatarData(paciente.dataNascimento)}`,
                paciente.idade != null ? `${paciente.idade} anos` : null,
                ROTULO_SEXO[paciente.sexo],
              ].filter(Boolean).join(' · ')}
            </p>
          </div>
          {onEditar && (
            <button className="botao" onClick={() => onEditar(paciente)}>Editar dados</button>
          )}
        </div>

        <nav className="abas abas-ficha" aria-label="Seções da ficha">
          {abas.map((a) => (
            <button
              key={a.id}
              className={`aba ${aba === a.id ? 'aba-ativa' : ''}`}
              aria-current={aba === a.id ? 'page' : undefined}
              onClick={() => trocarAba(a.id)}
            >
              {a.rotulo}
            </button>
          ))}
        </nav>

        <section className="cartao">
          {aba === 'dados' && <AbaDados paciente={paciente} verClinico={verClinico} />}
          {aba === 'historico' && <AbaHistorico {...propsAba} />}
          {aba === 'alergias' && <AbaAlergias {...propsAba} />}
          {aba === 'vacinas' && <AbaVacinas {...propsAba} />}
          {aba === 'atendimentos' && <AbaAtendimentos {...propsAba} />}
          {aba === 'consultas' && <AbaConsultas {...propsAba} />}
          {aba === 'exames' && <AbaExames {...propsAba} />}
          {aba === 'medicamentos' && <AbaMedicamentos {...propsAba} />}
          {aba === 'triagens' && <AbaTriagens {...propsAba} />}
        </section>
      </main>
    </>
  );
}
