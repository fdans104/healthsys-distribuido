import { ROTULO_SEXO, formatarCpf, formatarData, formatarDataHora, formatarTelefone } from '../../formatos.js';

/** Dados cadastrais do paciente (todos os perfis veem). */
export default function AbaDados({ paciente, verClinico }) {
  const linhas = [
    ['Nome', paciente.nome],
    ['CPF', paciente.cpf ? formatarCpf(paciente.cpf) : null],
    ['Nascimento', paciente.dataNascimento
      ? `${formatarData(paciente.dataNascimento)}${paciente.idade != null ? ` (${paciente.idade} anos)` : ''}`
      : null],
    ['Sexo', ROTULO_SEXO[paciente.sexo]],
    ['Telefone', paciente.telefone ? formatarTelefone(paciente.telefone) : null],
    ['E-mail', paciente.email],
    ['Endereço', paciente.endereco],
    ['Cadastrado em', paciente.criadoEm ? formatarDataHora(paciente.criadoEm) : null],
  ];

  return (
    <>
      <h3 className="titulo-aba">Dados cadastrais</h3>
      <dl className="dados">
        {linhas.map(([rotulo, valor]) => (
          <div key={rotulo} className="dados-linha">
            <dt>{rotulo}</dt>
            <dd>{valor || '—'}</dd>
          </div>
        ))}
      </dl>
      {!verClinico && (
        <p className="nota">
          O prontuário (consultas, exames, medicamentos, alergias, vacinas e atendimentos) é sigiloso e só aparece para médicos e enfermeiros.
        </p>
      )}
    </>
  );
}
