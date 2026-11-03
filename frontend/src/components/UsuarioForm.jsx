import { useState } from 'react';
import { usuariosApi } from '../api.js';

const PERFIS = [
  { valor: 'MEDICO', rotulo: 'Médico' },
  { valor: 'ENFERMEIRO', rotulo: 'Enfermeiro' },
  { valor: 'ADMIN', rotulo: 'Administrador' },
];

/** Formulário de cadastro. Se receber "usuario", vira formulário de edição. */
export default function UsuarioForm({ usuario, onSalvo, onCancelar }) {
  const editando = Boolean(usuario);
  const [campos, setCampos] = useState({
    nome: usuario?.nome ?? '',
    email: usuario?.email ?? '',
    senha: '',
    perfil: usuario?.perfil ?? 'MEDICO',
  });
  const [errosCampo, setErrosCampo] = useState({});
  const [erroGeral, setErroGeral] = useState('');
  const [enviando, setEnviando] = useState(false);

  function alterar(e) {
    setCampos({ ...campos, [e.target.name]: e.target.value });
  }

  async function enviar(e) {
    e.preventDefault();
    setEnviando(true);
    setErrosCampo({});
    setErroGeral('');

    // Na edição, senha em branco significa "manter a senha atual".
    const dados = { ...campos, senha: campos.senha === '' && editando ? null : campos.senha };

    try {
      const salvo = editando
        ? await usuariosApi.atualizar(usuario.id, dados)
        : await usuariosApi.criar(dados);
      onSalvo(salvo, editando);
    } catch (err) {
      setErrosCampo(err.campos || {});
      setErroGeral(err.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <form onSubmit={enviar} noValidate>
      <h2>{editando ? `Editar usuário #${usuario.id}` : 'Novo usuário'}</h2>

      <Campo rotulo="Nome" erro={errosCampo.nome}>
        <input name="nome" value={campos.nome} onChange={alterar} placeholder="Dra. Ana Souza" />
      </Campo>

      <Campo rotulo="E-mail" erro={errosCampo.email}>
        <input name="email" type="email" value={campos.email} onChange={alterar} placeholder="ana@hospital.com" />
      </Campo>

      <Campo
        rotulo={editando ? 'Nova senha (deixe em branco para manter)' : 'Senha'}
        erro={errosCampo.senha}
      >
        <input name="senha" type="password" value={campos.senha} onChange={alterar} placeholder="mínimo 8 caracteres" />
      </Campo>

      <Campo rotulo="Perfil" erro={errosCampo.perfil}>
        <select name="perfil" value={campos.perfil} onChange={alterar}>
          {PERFIS.map((p) => (
            <option key={p.valor} value={p.valor}>{p.rotulo}</option>
          ))}
        </select>
      </Campo>

      {erroGeral && <p className="erro-geral">{erroGeral}</p>}

      <div className="acoes">
        <button className="botao" type="submit" disabled={enviando}>
          {enviando ? 'Salvando…' : editando ? 'Salvar alterações' : 'Cadastrar'}
        </button>
        {editando && (
          <button className="botao botao-leve" type="button" onClick={onCancelar}>
            Cancelar
          </button>
        )}
      </div>
    </form>
  );
}

function Campo({ rotulo, erro, children }) {
  return (
    <label className={`campo ${erro ? 'campo-com-erro' : ''}`}>
      <span>{rotulo}</span>
      {children}
      {erro && <small>{erro}</small>}
    </label>
  );
}
