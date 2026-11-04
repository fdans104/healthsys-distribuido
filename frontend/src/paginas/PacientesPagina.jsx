import { useEffect, useRef, useState } from 'react';
import { pacientesApi } from '../api.js';
import useAvisos, { Faixas } from '../hooks/useAvisos.jsx';
import PacienteForm from '../components/PacienteForm.jsx';
import PacienteLista from '../components/PacienteLista.jsx';

/** Cadastro de pacientes: todos os perfis cadastram/editam; só ADMIN exclui. */
export default function PacientesPagina({ usuarioLogado }) {
  const [pacientes, setPacientes] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [busca, setBusca] = useState('');
  const [emEdicao, setEmEdicao] = useState(null);
  const [versaoForm, setVersaoForm] = useState(0); // muda a cada salvamento para o formulário voltar limpo
  const { aviso, erro, setErro, mostrarAviso } = useAvisos();
  const ultimaBusca = useRef(0);

  const podeExcluir = usuarioLogado.perfil === 'ADMIN';

  async function carregar(termo = busca) {
    const id = ++ultimaBusca.current; // descarta respostas de buscas antigas que cheguem atrasadas
    setCarregando(true);
    try {
      const lista = await pacientesApi.listar(termo);
      if (id !== ultimaBusca.current) return;
      setPacientes(lista);
      setErro('');
    } catch (e) {
      if (id === ultimaBusca.current) setErro(e.message);
    } finally {
      if (id === ultimaBusca.current) setCarregando(false);
    }
  }

  // Busca enquanto digita, esperando 300 ms de pausa para não disparar uma requisição por tecla.
  useEffect(() => {
    const t = setTimeout(() => carregar(busca), 300);
    return () => clearTimeout(t);
  }, [busca]);

  async function aoSalvar(paciente, foiEdicao) {
    setEmEdicao(null);
    setVersaoForm((v) => v + 1);
    mostrarAviso(foiEdicao ? `${paciente.nome} foi atualizado.` : `${paciente.nome} foi cadastrado.`);
    await carregar();
  }

  async function aoExcluir(paciente) {
    if (!window.confirm(`Excluir o paciente ${paciente.nome}? Essa ação não pode ser desfeita.`)) return;
    try {
      await pacientesApi.remover(paciente.id);
      if (emEdicao?.id === paciente.id) setEmEdicao(null);
      mostrarAviso(`${paciente.nome} foi excluído.`);
      await carregar();
    } catch (e) {
      setErro(e.message);
    }
  }

  return (
    <>
      <Faixas aviso={aviso} erro={erro} />

      <main className="conteudo">
        <section className="cartao">
          <PacienteForm
            key={`${emEdicao?.id ?? 'novo'}-${versaoForm}`}
            paciente={emEdicao}
            onSalvo={aoSalvar}
            onCancelar={() => setEmEdicao(null)}
          />
        </section>

        <section className="cartao">
          <div className="cartao-topo">
            <h2>Pacientes cadastrados</h2>
            <button className="botao botao-leve" onClick={() => carregar()}>Atualizar</button>
          </div>
          <input
            className="busca"
            type="search"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            placeholder="Buscar por nome ou CPF"
            aria-label="Buscar pacientes por nome ou CPF"
          />
          <PacienteLista
            pacientes={pacientes}
            carregando={carregando}
            buscando={busca.trim() !== ''}
            podeExcluir={podeExcluir}
            onEditar={setEmEdicao}
            onExcluir={aoExcluir}
          />
        </section>
      </main>
    </>
  );
}
