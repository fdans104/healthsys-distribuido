import { useEffect, useState } from 'react';
import { usuariosApi } from './api.js';
import UsuarioForm from './components/UsuarioForm.jsx';
import UsuarioLista from './components/UsuarioLista.jsx';

export default function App() {
  const [usuarios, setUsuarios] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [aviso, setAviso] = useState('');
  const [emEdicao, setEmEdicao] = useState(null); // usuário sendo editado, ou null = novo

  async function carregar() {
    setCarregando(true);
    try {
      setUsuarios(await usuariosApi.listar());
      setErro('');
    } catch (e) {
      setErro(e.message);
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    carregar();
  }, []);

  function mostrarAviso(texto) {
    setAviso(texto);
    setTimeout(() => setAviso(''), 3000);
  }

  async function aoSalvar(usuario, foiEdicao) {
    setEmEdicao(null);
    mostrarAviso(foiEdicao ? `${usuario.nome} foi atualizado.` : `${usuario.nome} foi cadastrado.`);
    await carregar();
  }

  async function aoExcluir(usuario) {
    if (!window.confirm(`Excluir o usuário ${usuario.nome}?`)) return;
    try {
      await usuariosApi.remover(usuario.id);
      if (emEdicao?.id === usuario.id) setEmEdicao(null);
      mostrarAviso(`${usuario.nome} foi excluído.`);
      await carregar();
    } catch (e) {
      setErro(e.message);
    }
  }

  return (
    <div className="pagina">
      <header className="topo">
        <h1>HealthSys</h1>
        <span className="subtitulo">Cadastro de usuários</span>
      </header>

      {aviso && <div className="faixa faixa-ok">{aviso}</div>}
      {erro && <div className="faixa faixa-erro">{erro}</div>}

      <main className="conteudo">
        <section className="cartao">
          <UsuarioForm
            key={emEdicao?.id ?? 'novo'}
            usuario={emEdicao}
            onSalvo={aoSalvar}
            onCancelar={() => setEmEdicao(null)}
          />
        </section>

        <section className="cartao">
          <div className="cartao-topo">
            <h2>Usuários cadastrados</h2>
            <button className="botao botao-leve" onClick={carregar}>Atualizar</button>
          </div>
          <UsuarioLista
            usuarios={usuarios}
            carregando={carregando}
            onEditar={setEmEdicao}
            onExcluir={aoExcluir}
          />
        </section>
      </main>
    </div>
  );
}
