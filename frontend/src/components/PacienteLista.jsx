import { ROTULO_SEXO, formatarCpf, formatarData, formatarTelefone } from '../formatos.js';

export default function PacienteLista({ pacientes, carregando, buscando, podeExcluir, onEditar, onExcluir }) {
  if (carregando && pacientes.length === 0) return <p className="vazio">Carregando…</p>;
  if (pacientes.length === 0) {
    return (
      <p className="vazio">
        {buscando ? 'Nenhum paciente encontrado para essa busca.' : 'Nenhum paciente cadastrado ainda.'}
      </p>
    );
  }

  return (
    <div className="tabela-rolagem">
      <table className="tabela">
        <thead>
          <tr>
            <th>#</th>
            <th>Paciente</th>
            <th>Nascimento</th>
            <th>Telefone</th>
            <th aria-label="Ações"></th>
          </tr>
        </thead>
        <tbody>
          {pacientes.map((p) => (
            <tr key={p.id}>
              <td className="num">{p.id}</td>
              <td>
                <div>{p.nome}</div>
                <div className="linha-secundaria num">{p.cpf ? `CPF ${formatarCpf(p.cpf)}` : 'CPF não informado'}</div>
              </td>
              <td className="num">
                <div>{formatarData(p.dataNascimento)}</div>
                <div className="linha-secundaria">
                  {[p.idade != null ? `${p.idade} anos` : null, ROTULO_SEXO[p.sexo]].filter(Boolean).join(' · ')}
                </div>
              </td>
              <td className="num">{p.telefone ? formatarTelefone(p.telefone) : '—'}</td>
              <td>
                <div className="acoes-linha">
                  <button className="botao botao-leve" onClick={() => onEditar(p)}>Editar</button>
                  {podeExcluir && (
                    <button className="botao botao-perigo" onClick={() => onExcluir(p)}>Excluir</button>
                  )}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
