import { useEffect, useState } from 'react';
import { usuariosApi } from '../api.js';
import { ROTULO_PERFIL } from '../formatos.js';
import useAvisos, { Faixas } from '../hooks/useAvisos.jsx';
import UsuarioForm from '../components/UsuarioForm.jsx';
import UsuarioLista from '../components/UsuarioLista.jsx';

/** Gestão de usuários do sistema (somente ADMIN). */
export default function UsuariosPagina({ usuarioLogado }) {
  const [usuarios, setUsuarios] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [emEdicao, setEmEdicao] = useState(null); // usuário sendo editado, ou null = novo
  const [versaoForm, setVersaoForm] = useState(0); // muda a cada salvamento para o formulário voltar limpo
  const [busca, setBusca] = useState('');
  const [perfilFiltro, setPerfilFiltro] = useState('');
  const { aviso, erro, setErro, mostrarAviso } = useAvisos();

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

  async function aoSalvar(usuario, foiEdicao) {
    setEmEdicao(null);
    setVersaoForm((v) => v + 1);
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

  // Filtro na própria tela: por nome ou e-mail e por perfil.
  const termo = busca.trim().toLowerCase();
  const visiveis = usuarios.filter((u) =>
    (!perfilFiltro || u.perfil === perfilFiltro)
    && (!termo || u.nome.toLowerCase().includes(termo) || u.email.toLowerCase().includes(termo)));

  return (
    <>
      <Faixas aviso={aviso} erro={erro} />

      <main className="conteudo">
        <section className="cartao">
          <UsuarioForm
            key={`${emEdicao?.id ?? 'novo'}-${versaoForm}`}
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
          <div className="filtros-linha">
            <input className="busca" type="search" value={busca} onChange={(e) => setBusca(e.target.value)}
                   placeholder="Buscar por nome ou e-mail" aria-label="Buscar usuários" />
            <select className="busca" value={perfilFiltro} onChange={(e) => setPerfilFiltro(e.target.value)}
                    aria-label="Filtrar por perfil">
              <option value="">Todos os perfis</option>
              {Object.entries(ROTULO_PERFIL).map(([valor, rotulo]) => <option key={valor} value={valor}>{rotulo}</option>)}
            </select>
          </div>
          <UsuarioLista
            usuarios={visiveis}
            carregando={carregando}
            idLogado={usuarioLogado.id}
            onEditar={setEmEdicao}
            onExcluir={aoExcluir}
          />
        </section>
      </main>
    </>
  );
}
