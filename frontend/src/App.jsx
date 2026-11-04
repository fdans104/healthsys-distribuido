import { useEffect, useState } from 'react';
import { definirAoSessaoExpirar, sessao } from './api.js';
import { ROTULO_PERFIL } from './formatos.js';
import Login from './components/Login.jsx';
import PacientesPagina from './paginas/PacientesPagina.jsx';
import UsuariosPagina from './paginas/UsuariosPagina.jsx';

export default function App() {
  const [usuario, setUsuario] = useState(() => sessao.obter()?.usuario ?? null);
  const [aba, setAba] = useState('pacientes');
  const [avisoSessao, setAvisoSessao] = useState('');

  // Se o servidor recusar o token (expirado/inválido), volta para a tela de login.
  useEffect(() => {
    definirAoSessaoExpirar(() => {
      setUsuario(null);
      setAvisoSessao('Sua sessão expirou. Faça login novamente.');
    });
  }, []);

  function entrar(resposta) {
    sessao.salvar(resposta);
    setUsuario(resposta.usuario);
    setAba('pacientes');
    setAvisoSessao('');
  }

  function sair() {
    sessao.limpar();
    setUsuario(null);
  }

  if (!usuario) {
    return (
      <>
        {avisoSessao && <div className="faixa faixa-erro faixa-flutuante" role="alert">{avisoSessao}</div>}
        <Login onEntrar={entrar} />
      </>
    );
  }

  const ehAdmin = usuario.perfil === 'ADMIN';
  const abaAtual = aba === 'usuarios' && ehAdmin ? 'usuarios' : 'pacientes';

  return (
    <div className="pagina">
      <header className="topo">
        <h1>HealthSys</h1>

        <nav className="abas" aria-label="Seções">
          <button className={`aba ${abaAtual === 'pacientes' ? 'aba-ativa' : ''}`}
                  aria-current={abaAtual === 'pacientes' ? 'page' : undefined}
                  onClick={() => setAba('pacientes')}>
            Pacientes
          </button>
          {ehAdmin && (
            <button className={`aba ${abaAtual === 'usuarios' ? 'aba-ativa' : ''}`}
                    aria-current={abaAtual === 'usuarios' ? 'page' : undefined}
                    onClick={() => setAba('usuarios')}>
              Usuários
            </button>
          )}
        </nav>

        <div className="sessao">
          <span className="sessao-nome">{usuario.nome}</span>
          <span className={`etiqueta etiqueta-${usuario.perfil.toLowerCase()}`}>
            {ROTULO_PERFIL[usuario.perfil] ?? usuario.perfil}
          </span>
          <button className="botao botao-leve" onClick={sair}>Sair</button>
        </div>
      </header>

      {abaAtual === 'usuarios'
        ? <UsuariosPagina usuarioLogado={usuario} />
        : <PacientesPagina usuarioLogado={usuario} />}
    </div>
  );
}
