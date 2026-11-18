import { useEffect, useRef, useState } from 'react';
import { pacientesApi } from '../api.js';
import useAvisos, { Faixas } from '../hooks/useAvisos.jsx';
import { podeCadastrarPaciente, podeExcluirPaciente } from '../permissoes.js';
import Modal from '../components/Modal.jsx';
import PacienteForm from '../components/PacienteForm.jsx';
import PacienteLista from '../components/PacienteLista.jsx';
import FichaPaciente from '../components/FichaPaciente.jsx';

/**
 * Tela de pacientes.
 * - Todos os perfis buscam pacientes e abrem a ficha.
 * - Só a recepção cadastra e edita os dados cadastrais (em uma janela por cima da lista).
 * - Só o administrador exclui (e o servidor recusa se o paciente já tiver prontuário).
 */
export default function PacientesPagina({ usuarioLogado }) {
  const [pacientes, setPacientes] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [busca, setBusca] = useState('');
  const [formulario, setFormulario] = useState(null); // null = fechado; { paciente: null } = novo; { paciente } = edição
  const [fichaAberta, setFichaAberta] = useState(null); // paciente cuja ficha está aberta, ou null = lista
  const { aviso, erro, setErro, mostrarAviso } = useAvisos();
  const ultimaBusca = useRef(0);

  const perfil = usuarioLogado.perfil;
  const podeCadastrar = podeCadastrarPaciente(perfil);
  const podeExcluir = podeExcluirPaciente(perfil);

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
    setFormulario(null);
    mostrarAviso(foiEdicao ? `${paciente.nome} foi atualizado.` : `${paciente.nome} foi cadastrado.`);
    await carregar();
  }

  // RN54: alguém salvou este paciente antes. Fecha a edição, recarrega a lista (versão mais recente)
  // e só então mostra o aviso, porque carregar() limpa a faixa de erro quando termina.
  async function aoConflito(mensagem) {
    setFormulario(null);
    await carregar();
    setErro(mensagem);
  }

  async function aoExcluir(paciente) {
    if (!window.confirm(`Excluir o paciente ${paciente.nome}? Essa ação não pode ser desfeita.`)) return;
    try {
      await pacientesApi.remover(paciente.id);
      mostrarAviso(`${paciente.nome} foi excluído.`);
      await carregar();
    } catch (e) {
      setErro(e.message);
    }
  }

  function editarPelaFicha(paciente) {
    setFichaAberta(null);
    setFormulario({ paciente });
  }

  if (fichaAberta) {
    return (
      <FichaPaciente
        paciente={fichaAberta}
        usuarioLogado={usuarioLogado}
        onVoltar={() => { setFichaAberta(null); carregar(); }}
        onEditar={podeCadastrar ? editarPelaFicha : null}
      />
    );
  }

  return (
    <>
      <Faixas aviso={aviso} erro={erro} />

      <main className="conteudo conteudo-unico">
        <section className="cartao">
          <div className="cartao-topo">
            <h2>Pacientes</h2>
            <div className="acoes">
              <button className="botao botao-leve" onClick={() => carregar()}>Atualizar</button>
              {podeCadastrar && (
                <button className="botao" onClick={() => setFormulario({ paciente: null })}>+ Novo paciente</button>
              )}
            </div>
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
            podeEditar={podeCadastrar}
            podeExcluir={podeExcluir}
            onAbrirFicha={setFichaAberta}
            onEditar={(p) => setFormulario({ paciente: p })}
            onExcluir={aoExcluir}
          />
        </section>
      </main>

      {formulario && (
        <Modal
          titulo={formulario.paciente ? `Editar paciente #${formulario.paciente.id}` : 'Novo paciente'}
          aoFechar={() => setFormulario(null)}
        >
          <PacienteForm
            paciente={formulario.paciente}
            mostrarTitulo={false}
            onSalvo={aoSalvar}
            onCancelar={() => setFormulario(null)}
            onConflito={aoConflito}
          />
        </Modal>
      )}
    </>
  );
}
