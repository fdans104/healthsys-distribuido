import { useState } from 'react';
import { authApi } from '../api.js';

/** Tela de login: troca e-mail e senha por um token JWT (POST /api/v1/auth/login). */
export default function Login({ onEntrar }) {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [errosCampo, setErrosCampo] = useState({});
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);

  async function enviar(e) {
    e.preventDefault();
    setEnviando(true);
    setErro('');
    setErrosCampo({});
    try {
      onEntrar(await authApi.login(email, senha));
    } catch (err) {
      setErrosCampo(err.campos || {});
      setErro(err.message);
      setEnviando(false);
    }
  }

  return (
    <div className="login-pagina">
      <form className="cartao login-cartao" onSubmit={enviar} noValidate>
        <h1 className="login-marca">HealthSys</h1>
        <p className="login-subtitulo">Entre com sua conta para continuar</p>

        <label className={`campo ${errosCampo.email ? 'campo-com-erro' : ''}`}>
          <span>E-mail</span>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                 autoComplete="username" autoFocus placeholder="voce@hospital.com" />
          {errosCampo.email && <small>{errosCampo.email}</small>}
        </label>

        <label className={`campo ${errosCampo.senha ? 'campo-com-erro' : ''}`}>
          <span>Senha</span>
          <input type="password" value={senha} onChange={(e) => setSenha(e.target.value)}
                 autoComplete="current-password" />
          {errosCampo.senha && <small>{errosCampo.senha}</small>}
        </label>

        {erro && <p className="erro-geral" role="alert">{erro}</p>}

        <button className="botao botao-largo" type="submit" disabled={enviando}>
          {enviando ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
    </div>
  );
}
