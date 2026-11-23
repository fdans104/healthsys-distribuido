import { useCallback, useEffect, useState } from 'react';
import { leitosApi, monitoramentoApi } from '../api.js';
import useAvisos, { Faixas } from '../hooks/useAvisos.jsx';
import { ROTULO_SETOR, ROTULO_STATUS_LEITO, SETORES, tempoDesde } from '../formatos.js';
import { podeGerenciarLeitos, podeHigienizar, podeInternar, podeMonitorar } from '../permissoes.js';
import Modal from '../components/Modal.jsx';
import InternarForm from '../components/leitos/InternarForm.jsx';
import NovoLeitoForm from '../components/leitos/NovoLeitoForm.jsx';
import { AlertasAtivos, MonitorLeito } from '../components/leitos/MonitorAoVivo.jsx';

const ATUALIZAR_MONITOR_MS = 3000;
const ATUALIZAR_LEITOS_MS = 15000;

/**
 * Monitoramento hospitalar (monitoring-service):
 * - Ocupação por setor (todos os perfis).
 * - Sinais vitais ao vivo, vindos dos monitores dos leitos por MQTT, e alertas (médico e enfermagem).
 * - Leitos: internar, dar alta, confirmar higienização (médico/enfermagem) e cadastrar leitos (administrador).
 */
export default function LeitosPagina({ usuarioLogado }) {
  const perfil = usuarioLogado.perfil;
  const monitora = podeMonitorar(perfil);
  const [leitos, setLeitos] = useState([]);
  const [ocupacao, setOcupacao] = useState([]);
  const [painel, setPainel] = useState(null);
  const [alertas, setAlertas] = useState([]);
  const [monitorFalhou, setMonitorFalhou] = useState(false);
  const [carregando, setCarregando] = useState(true);
  const [janela, setJanela] = useState(null); // { tipo: 'internar', leito } | { tipo: 'novo' }
  const { aviso, erro, setErro, mostrarAviso } = useAvisos();

  const carregarLeitos = useCallback(async () => {
    try {
      const [lista, porSetor] = await Promise.all([leitosApi.listar(), leitosApi.ocupacao()]);
      setLeitos(lista);
      setOcupacao(porSetor);
      setErro('');
    } catch (e) {
      setErro(e.message);
    } finally {
      setCarregando(false);
    }
  }, [setErro]);

  const carregarMonitor = useCallback(async () => {
    if (!monitora) return;
    try {
      const [p, a] = await Promise.all([monitoramentoApi.leitos(), monitoramentoApi.alertas(true)]);
      setPainel(p);
      setAlertas(a);
      setMonitorFalhou(false);
    } catch {
      setMonitorFalhou(true);
    }
  }, [monitora]);

  useEffect(() => {
    carregarLeitos();
    carregarMonitor();
    const t1 = setInterval(carregarLeitos, ATUALIZAR_LEITOS_MS);
    const t2 = monitora ? setInterval(carregarMonitor, ATUALIZAR_MONITOR_MS) : null;
    return () => {
      clearInterval(t1);
      if (t2) clearInterval(t2);
    };
  }, [carregarLeitos, carregarMonitor, monitora]);

  async function agir(acao, mensagemOk, confirmar) {
    if (confirmar && !window.confirm(confirmar)) return;
    try {
      await acao();
      mostrarAviso(mensagemOk);
    } catch (e) {
      setErro(e.message);
    }
    await Promise.all([carregarLeitos(), carregarMonitor()]);
  }

  async function reconhecer(alerta) {
    await agir(() => monitoramentoApi.reconhecer(alerta.id),
      `Alerta do leito ${alerta.leitoCodigo} reconhecido.`);
  }

  async function aoConflito(mensagem) {
    setJanela(null);
    await carregarLeitos();
    setErro(mensagem);
  }

  return (
    <>
      <Faixas aviso={aviso} erro={erro} />

      <main className="inicio">
        <section className="cartao">
          <div className="cartao-topo">
            <h2>Ocupação hospitalar</h2>
            <div className="acoes">
              <button className="botao botao-leve" onClick={() => { carregarLeitos(); carregarMonitor(); }}>Atualizar</button>
              {podeGerenciarLeitos(perfil) && (
                <button className="botao" onClick={() => setJanela({ tipo: 'novo' })}>+ Novo leito</button>
              )}
            </div>
          </div>
          <div className="painel">
            {ocupacao.map((o) => (
              <div key={o.setor} className={`painel-cartao ocupacao ${o.taxaOcupacao >= 90 ? 'painel-alerta' : ''}`}>
                <span className="painel-numero">{o.taxaOcupacao}%</span>
                <span className="painel-titulo">{ROTULO_SETOR[o.setor]}</span>
                <span className="linha-secundaria">
                  {o.ocupados} ocupado(s) · {o.livres} livre(s){o.emHigienizacao ? ` · ${o.emHigienizacao} em limpeza` : ''}
                </span>
                <span className="barra" aria-hidden="true"><span style={{ width: `${o.taxaOcupacao}%` }} /></span>
              </div>
            ))}
          </div>
          {perfil === 'ADMIN' && (
            <p className="nota">Como administrador, você vê a situação dos leitos, mas não quem está internado (dado de saúde).</p>
          )}
        </section>

        {monitora && (
          <section className="cartao">
            <div className="cartao-topo">
              <h2>Monitoramento ao vivo</h2>
              <span className={`ao-vivo ${painel?.monitoresConectados && !monitorFalhou ? 'ao-vivo-ok' : ''}`}>
                {monitorFalhou
                  ? 'serviço de monitoramento fora do ar'
                  : painel?.monitoresConectados ? 'monitores conectados' : 'sem conexão com os monitores'}
              </span>
            </div>

            <h3 className="subtitulo-aba">Alertas abertos</h3>
            <AlertasAtivos alertas={alertas} onReconhecer={reconhecer} />

            <h3 className="subtitulo-aba">Leitos ocupados</h3>
            {painel && painel.leitos.length === 0 && (
              <p className="vazio">Nenhum paciente internado. Interne um paciente em um leito abaixo para ver os sinais vitais.</p>
            )}
            <ul className="monitores">
              {(painel?.leitos ?? []).map((m) => <MonitorLeito key={m.leitoId} monitor={m} />)}
            </ul>
            <p className="linha-secundaria">
              Limites de alerta simplificados para demonstração (ex.: SpO₂ abaixo de 94% pede atenção; abaixo de 90% é crítico).
            </p>
          </section>
        )}

        <section className="cartao">
          <h2>Leitos</h2>
          {carregando && leitos.length === 0 && <p className="vazio">Carregando…</p>}
          {SETORES.map((setor) => {
            const doSetor = leitos.filter((l) => l.setor === setor);
            if (doSetor.length === 0) return null;
            return (
              <div key={setor}>
                <h3 className="subtitulo-aba">{ROTULO_SETOR[setor]}</h3>
                <ul className="leitos">
                  {doSetor.map((l) => (
                    <li key={l.id} className={`leito leito-${l.status.toLowerCase()}`}>
                      <div className="leito-topo">
                        <strong>{l.codigo}</strong>
                        <span className={`leito-status leito-status-${l.status.toLowerCase()}`}>{ROTULO_STATUS_LEITO[l.status]}</span>
                      </div>
                      {l.status === 'OCUPADO' && l.pacienteNome && (
                        <p className="leito-paciente">{l.pacienteNome}
                          <span className="linha-secundaria"> · {tempoDesde(l.ocupadoEm)}</span></p>
                      )}
                      {l.status === 'HIGIENIZACAO' && <p className="linha-secundaria">Aguardando limpeza</p>}
                      <div className="acoes-linha">
                        {l.status === 'LIVRE' && podeInternar(perfil) && (
                          <button className="botao botao-leve" onClick={() => setJanela({ tipo: 'internar', leito: l })}>Internar</button>
                        )}
                        {l.status === 'OCUPADO' && podeInternar(perfil) && (
                          <button className="botao botao-leve"
                                  onClick={() => agir(() => leitosApi.liberar(l.id, l.versao),
                                    `Alta do leito ${l.codigo}: aguardando higienização.`,
                                    `Dar alta do leito ${l.codigo} (${l.pacienteNome})? Os alertas abertos dele serão encerrados.`)}>
                            Dar alta
                          </button>
                        )}
                        {l.status === 'HIGIENIZACAO' && podeHigienizar(perfil) && (
                          <button className="botao botao-leve"
                                  onClick={() => agir(() => leitosApi.higienizado(l.id, l.versao), `Leito ${l.codigo} livre.`)}>
                            Higienizado
                          </button>
                        )}
                        {l.status === 'LIVRE' && podeGerenciarLeitos(perfil) && (
                          <button className="botao botao-perigo"
                                  onClick={() => agir(() => leitosApi.remover(l.id), `Leito ${l.codigo} removido.`,
                                    `Remover o leito ${l.codigo} do sistema?`)}>
                            Remover
                          </button>
                        )}
                      </div>
                    </li>
                  ))}
                </ul>
              </div>
            );
          })}
        </section>
      </main>

      {janela?.tipo === 'internar' && (
        <Modal titulo={`Internar no leito ${janela.leito.codigo}`} aoFechar={() => setJanela(null)}>
          <InternarForm
            leito={janela.leito}
            onInternado={(l) => { setJanela(null); mostrarAviso(`${l.pacienteNome} internado no leito ${l.codigo}.`); carregarLeitos(); carregarMonitor(); }}
            onConflito={aoConflito}
            onCancelar={() => setJanela(null)}
          />
        </Modal>
      )}
      {janela?.tipo === 'novo' && (
        <Modal titulo="Novo leito" aoFechar={() => setJanela(null)}>
          <NovoLeitoForm
            onCriado={(l) => { setJanela(null); mostrarAviso(`Leito ${l.codigo} cadastrado.`); carregarLeitos(); }}
            onCancelar={() => setJanela(null)}
          />
        </Modal>
      )}
    </>
  );
}
