import { useEffect, useState } from 'react';
import { definirAoSessaoExpirar, sessao } from './api.js';
import { ROTULO_PERFIL } from './formatos.js';
import { podeGerenciarUsuarios, podeVerLeitos, podeVerTriagens, recebeNotificacoes } from './permissoes.js';
import Login from './components/Login.jsx';
import Notificacoes from './components/Notificacoes.jsx';
import InicioPagina from './paginas/InicioPagina.jsx';
import LeitosPagina from './paginas/LeitosPagina.jsx';
import PacientesPagina from './paginas/PacientesPagina.jsx';
import TriagemPagina from './paginas/TriagemPagina.jsx';
import UsuariosPagina from './paginas/UsuariosPagina.jsx';

/** Seções do menu. Cada perfil vê só as suas (ver permissoes.js). */
function secoesDoPerfil(perfil) {
  return [
    { id: 'inicio', rotulo: 'Início' },
    { id: 'pacientes', rotulo: 'Pacientes' },
    podeVerTriagens(perfil) && { id: 'triagem', rotulo: 'Triagem' },
    podeVerLeitos(perfil) && { id: 'leitos', rotulo: 'Leitos' },
    podeGerenciarUsuarios(perfil) && { id: 'usuarios', rotulo: 'Usuários' },
  ].filter(Boolean);
}

export default function App() {
  const [usuario, setUsuario] = useState(() => sessao.obter()?.usuario ?? null);
  const [aba, setAba] = useState('inicio');
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
    setAba('inicio');
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

  const secoes = secoesDoPerfil(usuario.perfil);
  const abaAtual = secoes.some((s) => s.id === aba) ? aba : 'inicio';

  return (
    <div className="pagina">
      <header className="topo">
        <h1>HealthSys</h1>

        <nav className="abas" aria-label="Seções">
          {secoes.map((s) => (
            <button key={s.id}
                    className={`aba ${abaAtual === s.id ? 'aba-ativa' : ''}`}
                    aria-current={abaAtual === s.id ? 'page' : undefined}
                    onClick={() => setAba(s.id)}>
              {s.rotulo}
            </button>
          ))}
        </nav>

        <div className="sessao">
          {recebeNotificacoes(usuario.perfil) && <Notificacoes key={usuario.id} irPara={setAba} />}
          <span className="sessao-nome">{usuario.nome}</span>
          <span className={`etiqueta etiqueta-${usuario.perfil.toLowerCase()}`}>
            {ROTULO_PERFIL[usuario.perfil] ?? usuario.perfil}
          </span>
          <button className="botao botao-leve" onClick={sair}>Sair</button>
        </div>
      </header>

      {abaAtual === 'inicio' && <InicioPagina usuarioLogado={usuario} irPara={setAba} />}
      {abaAtual === 'pacientes' && <PacientesPagina usuarioLogado={usuario} />}
      {abaAtual === 'triagem' && <TriagemPagina usuarioLogado={usuario} />}
      {abaAtual === 'leitos' && <LeitosPagina usuarioLogado={usuario} />}
      {abaAtual === 'usuarios' && <UsuariosPagina usuarioLogado={usuario} />}
    </div>
  );
}
