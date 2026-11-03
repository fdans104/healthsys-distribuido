const ROTULO_PERFIL = { ADMIN: 'Administrador', MEDICO: 'Médico', ENFERMEIRO: 'Enfermeiro' };

function formatarData(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

export default function UsuarioLista({ usuarios, carregando, onEditar, onExcluir }) {
  if (carregando && usuarios.length === 0) return <p className="vazio">Carregando…</p>;
  if (usuarios.length === 0) return <p className="vazio">Nenhum usuário cadastrado ainda.</p>;

  return (
    <div className="tabela-rolagem">
      <table className="tabela">
        <thead>
          <tr>
            <th>#</th>
            <th>Nome</th>
            <th>E-mail</th>
            <th>Perfil</th>
            <th>Criado em</th>
            <th aria-label="Ações"></th>
          </tr>
        </thead>
        <tbody>
          {usuarios.map((u) => (
            <tr key={u.id}>
              <td className="num">{u.id}</td>
              <td>{u.nome}</td>
              <td>{u.email}</td>
              <td><span className={`etiqueta etiqueta-${u.perfil.toLowerCase()}`}>{ROTULO_PERFIL[u.perfil] ?? u.perfil}</span></td>
              <td className="num">{formatarData(u.criadoEm)}</td>
              <td className="acoes-linha">
                <button className="botao botao-leve" onClick={() => onEditar(u)}>Editar</button>
                <button className="botao botao-perigo" onClick={() => onExcluir(u)}>Excluir</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
