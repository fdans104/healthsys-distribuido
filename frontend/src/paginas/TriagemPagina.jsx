import { useEffect, useState } from 'react';
import { triagemApi } from '../api.js';
import useAvisos, { Faixas } from '../hooks/useAvisos.jsx';
import { podeAbrirTriagem } from '../permissoes.js';
import { ROTULO_STATUS_TRIAGEM } from '../formatos.js';
import Modal from '../components/Modal.jsx';
import NovaTriagemForm from '../components/triagem/NovaTriagemForm.jsx';
import AvaliacaoForm from '../components/triagem/AvaliacaoForm.jsx';
import TriagemCartao from '../components/triagem/TriagemCartao.jsx';

const FILTROS = [
  { id: 'ATIVAS', rotulo: 'Na fila', filtro: (t) => t.status === 'PENDENTE' || t.status === 'EM_ATENDIMENTO' },
  { id: 'FINALIZADA', rotulo: 'Finalizadas', filtro: (t) => t.status === 'FINALIZADA' },
  { id: 'CANCELADA', rotulo: 'Canceladas', filtro: (t) => t.status === 'CANCELADA' },
  { id: 'TODAS', rotulo: 'Todas', filtro: () => true },
];

const CONFIRMACAO = {
  EM_ATENDIMENTO: 'Chamar este paciente para atendimento?',
  FINALIZADA: 'Finalizar o atendimento desta triagem? Depois de finalizada, ela não muda mais.',
  PENDENTE: 'Devolver o paciente à fila?',
  CANCELADA: 'Cancelar esta triagem (por exemplo, o paciente desistiu)? Depois de cancelada, ela não muda mais.',
};

/**
 * Fila do pronto-atendimento (RF14 a RF16).
 * A recepção abre a triagem, a enfermagem avalia e classifica o risco, o médico chama, atende e finaliza.
 * A fila já vem do servidor na ordem certa: em atendimento, sem avaliação, depois do risco mais grave ao mais leve.
 */
export default function TriagemPagina({ usuarioLogado }) {
  const perfil = usuarioLogado.perfil;
  const [triagens, setTriagens] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [filtro, setFiltro] = useState('ATIVAS');
  const [janela, setJanela] = useState(null); // { tipo: 'nova' } ou { tipo: 'avaliar', triagem }
  const { aviso, erro, setErro, mostrarAviso } = useAvisos();

  async function carregar() {
    setCarregando(true);
    try {
      setTriagens(await triagemApi.listar());
      setErro('');
    } catch (e) {
      setErro(e.message);
    } finally {
      setCarregando(false);
    }
  }

  // Carrega ao abrir e atualiza a fila sozinha a cada 30 segundos.
  useEffect(() => {
    carregar();
    const t = setInterval(carregar, 30000);
    return () => clearInterval(t);
  }, []);

  async function mudarStatus(triagem, status) {
    if (!window.confirm(CONFIRMACAO[status])) return;
    try {
      await triagemApi.mudarStatus(triagem.id, { status, versao: triagem.versao });
      mostrarAviso(`${triagem.pacienteNome}: ${ROTULO_STATUS_TRIAGEM[status].toLowerCase()}.`);
      await carregar();
    } catch (e) {
      await carregar(); // RN54: se alguém mudou antes, a fila mostra a versão mais recente
      setErro(e.message);
    }
  }

  async function aoConflito(mensagem) {
    setJanela(null);
    await carregar();
    setErro(mensagem);
  }

  const visiveis = triagens.filter(FILTROS.find((f) => f.id === filtro).filtro);
  const contagem = (f) => triagens.filter(f.filtro).length;

  return (
    <>
      <Faixas aviso={aviso} erro={erro} />

      <main className="conteudo conteudo-unico">
        <section className="cartao">
          <div className="cartao-topo">
            <h2>Triagem</h2>
            <div className="acoes">
              <button className="botao botao-leve" onClick={carregar}>Atualizar</button>
              {podeAbrirTriagem(perfil) && (
                <button className="botao" onClick={() => setJanela({ tipo: 'nova' })}>+ Nova triagem</button>
              )}
            </div>
          </div>

          <nav className="filtros" aria-label="Filtrar por status">
            {FILTROS.map((f) => (
              <button key={f.id} className={`filtro ${filtro === f.id ? 'filtro-ativo' : ''}`} onClick={() => setFiltro(f.id)}>
                {f.rotulo} <span className="filtro-contagem">{contagem(f)}</span>
              </button>
            ))}
          </nav>

          {carregando && triagens.length === 0 && <p className="vazio">Carregando…</p>}
          {!carregando && visiveis.length === 0 && <p className="vazio">Nenhuma triagem aqui.</p>}
          <ul className="registros">
            {visiveis.map((t) => (
              <TriagemCartao key={t.id} triagem={t} perfil={perfil}
                             onAvaliar={(tr) => setJanela({ tipo: 'avaliar', triagem: tr })}
                             onMudarStatus={mudarStatus} />
            ))}
          </ul>
        </section>
      </main>

      {janela?.tipo === 'nova' && (
        <Modal titulo="Nova triagem" aoFechar={() => setJanela(null)}>
          <NovaTriagemForm
            onAberta={(t) => { setJanela(null); mostrarAviso(`${t.pacienteNome} entrou na fila da triagem.`); carregar(); }}
            onCancelar={() => setJanela(null)}
          />
        </Modal>
      )}
      {janela?.tipo === 'avaliar' && (
        <Modal titulo={`Avaliação: ${janela.triagem.pacienteNome}`} aoFechar={() => setJanela(null)} larga>
          <AvaliacaoForm
            triagem={janela.triagem}
            onAvaliada={(t) => { setJanela(null); mostrarAviso(`${t.pacienteNome} foi classificado.`); carregar(); }}
            onConflito={aoConflito}
            onCancelar={() => setJanela(null)}
          />
        </Modal>
      )}
    </>
  );
}
