import { ROTULO_PERFIL, formatarDataHora } from '../formatos.js';

export default function UsuarioLista({ usuarios, carregando, idLogado, onEditar, onExcluir }) {
  if (carregando && usuarios.length === 0) return <p className="vazio">Carregando…</p>;
  if (usuarios.length === 0) return <p className="vazio">Nenhum usuário cadastrado ainda.</p>;

  return (
    <div className="tabela-rolagem">
      <table className="tabela">
        <thead>
          <tr>
            <th>#</th>
            <th>Usuário</th>
            <th>Perfil</th>
            <th>Criado em</th>
            <th aria-label="Ações"></th>
          </tr>
        </thead>
        <tbody>
          {usuarios.map((u) => (
            <tr key={u.id}>
              <td className="num">{u.id}</td>
              <td>
                <div>{u.nome}{u.id === idLogado && <span className="secundario"> (você)</span>}</div>
                <div className="linha-secundaria">{u.email}</div>
              </td>
              <td><span className={`etiqueta etiqueta-${u.perfil.toLowerCase()}`}>{ROTULO_PERFIL[u.perfil] ?? u.perfil}</span></td>
              <td className="num">{formatarDataHora(u.criadoEm)}</td>
              <td>
                <div className="acoes-linha">
                  <button className="botao botao-leve" onClick={() => onEditar(u)}>Editar</button>
                  {/* O próprio usuário logado não pode se excluir (regra também validada no backend). */}
                  {u.id !== idLogado && (
                    <button className="botao botao-perigo" onClick={() => onExcluir(u)}>Excluir</button>
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
