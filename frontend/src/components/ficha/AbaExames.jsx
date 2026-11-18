import { useEffect, useState } from 'react';
import { prontuarioApi } from '../../api.js';
import { podeRegistrarResultado, podeSolicitarExame } from '../../permissoes.js';
import { ROTULO_PERFIL, ROTULO_TIPO_EXAME, formatarData, formatarDataHora, hojeIso } from '../../formatos.js';
import Campo from '../Campo.jsx';
import { ListaRegistros, SoLeitura } from './Registros.jsx';

const TIPOS = Object.entries(ROTULO_TIPO_EXAME);

/**
 * RF12: exames. O médico solicita; depois o médico ou a enfermagem registra o resultado.
 * Com o resultado, o exame fica concluído e não muda mais.
 */
export default function AbaExames({ pacienteId, perfil, mostrarAviso, setErro }) {
  const [itens, setItens] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [campos, setCampos] = useState({ nome: '', tipo: 'LABORATORIAL', observacoes: '' });
  const [errosCampo, setErrosCampo] = useState({});
  const [enviando, setEnviando] = useState(false);
  const [resultadoAberto, setResultadoAberto] = useState(null); // id do exame com o formulário de resultado aberto

  async function carregar() {
    setCarregando(true);
    try {
      setItens(await prontuarioApi.listarExames(pacienteId));
    } catch (e) {
      setErro(e.message);
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    carregar();
  }, [pacienteId]);

  async function solicitar(e) {
    e.preventDefault();
    setEnviando(true);
    setErrosCampo({});
    setErro('');
    try {
      const exame = await prontuarioApi.solicitarExame(pacienteId, {
        nome: campos.nome, tipo: campos.tipo, observacoes: campos.observacoes || null,
      });
      setCampos({ nome: '', tipo: 'LABORATORIAL', observacoes: '' });
      mostrarAviso(`Exame ${exame.nome} solicitado.`);
      await carregar();
    } catch (err) {
      setErrosCampo(err.campos || {});
      setErro(err.message);
    } finally {
      setEnviando(false);
    }
  }

  const pendentes = itens.filter((e) => e.status === 'SOLICITADO').length;

  return (
    <>
      <h3 className="titulo-aba">Exames {pendentes > 0 && <span className="etiqueta">{pendentes} aguardando resultado</span>}</h3>

      {podeSolicitarExame(perfil) ? (
        <form className="form-ficha" onSubmit={solicitar} noValidate>
          <div className="campo-duplo">
            <Campo rotulo="Exame" erro={errosCampo.nome}>
              <input value={campos.nome} onChange={(e) => setCampos({ ...campos, nome: e.target.value })}
                     placeholder="Ex.: Hemograma completo" />
            </Campo>
            <Campo rotulo="Tipo" erro={errosCampo.tipo}>
              <select value={campos.tipo} onChange={(e) => setCampos({ ...campos, tipo: e.target.value })}>
                {TIPOS.map(([valor, rotulo]) => <option key={valor} value={valor}>{rotulo}</option>)}
              </select>
            </Campo>
          </div>
          <Campo rotulo="Observações (opcional)" erro={errosCampo.observacoes}>
            <input value={campos.observacoes} onChange={(e) => setCampos({ ...campos, observacoes: e.target.value })}
                   placeholder="Ex.: em jejum de 8 horas" />
          </Campo>
          <button className="botao" type="submit" disabled={enviando}>{enviando ? 'Solicitando…' : 'Solicitar exame'}</button>
        </form>
      ) : (
        <SoLeitura>Os exames são solicitados pelo médico. A enfermagem pode registrar o resultado.</SoLeitura>
      )}

      <ListaRegistros itens={itens} carregando={carregando} vazio="Nenhum exame registrado.">
        {itens.map((ex) => (
          <li key={ex.id} className="registro">
            <div className="registro-topo">
              <p className="registro-titulo">
                {ex.nome} <span className="linha-secundaria">· {ROTULO_TIPO_EXAME[ex.tipo] ?? ex.tipo}</span>
              </p>
              <span className={`etiqueta ${ex.status === 'CONCLUIDO' ? 'status-finalizada' : 'status-pendente'}`}>
                {ex.status === 'CONCLUIDO' ? 'Concluído' : 'Aguardando resultado'}
              </span>
            </div>
            {ex.observacoes && <p className="registro-texto">{ex.observacoes}</p>}
            <p className="linha-secundaria">
              Solicitado por {ex.solicitadoPorNome} em {formatarDataHora(ex.solicitadoEm)}
            </p>
            {ex.status === 'CONCLUIDO' && (
              <>
                <p className="registro-texto"><b>Resultado ({formatarData(ex.dataResultado)}):</b> {ex.resultado}</p>
                <p className="linha-secundaria">
                  Registrado por {ex.resultadoRegistradoPorNome}
                  {ex.resultadoRegistradoPorPerfil ? ` (${ROTULO_PERFIL[ex.resultadoRegistradoPorPerfil] ?? ex.resultadoRegistradoPorPerfil})` : ''}
                </p>
              </>
            )}
            {ex.status === 'SOLICITADO' && podeRegistrarResultado(perfil) && (
              resultadoAberto === ex.id ? (
                <ResultadoForm pacienteId={pacienteId} exame={ex} setErro={setErro}
                               onCancelar={() => setResultadoAberto(null)}
                               onSalvo={async () => { setResultadoAberto(null); mostrarAviso(`Resultado de ${ex.nome} registrado.`); await carregar(); }} />
              ) : (
                <div className="acoes acoes-cartao">
                  <button className="botao botao-leve" onClick={() => setResultadoAberto(ex.id)}>Registrar resultado</button>
                </div>
              )
            )}
          </li>
        ))}
      </ListaRegistros>
    </>
  );
}

function ResultadoForm({ pacienteId, exame, setErro, onSalvo, onCancelar }) {
  const [resultado, setResultado] = useState('');
  const [dataResultado, setDataResultado] = useState(hojeIso());
  const [errosCampo, setErrosCampo] = useState({});
  const [enviando, setEnviando] = useState(false);

  async function salvar(e) {
    e.preventDefault();
    setEnviando(true);
    setErrosCampo({});
    setErro('');
    try {
      await prontuarioApi.registrarResultado(pacienteId, exame.id, { resultado, dataResultado: dataResultado || null });
      await onSalvo();
    } catch (err) {
      setErrosCampo(err.campos || {});
      setErro(err.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <form className="form-inline" onSubmit={salvar} noValidate>
      <Campo rotulo="Resultado" erro={errosCampo.resultado}>
        <textarea rows={2} value={resultado} onChange={(e) => setResultado(e.target.value)} placeholder="Ex.: Sem alterações" />
      </Campo>
      <Campo rotulo="Data do resultado" erro={errosCampo.dataResultado}>
        <input type="date" value={dataResultado} onChange={(e) => setDataResultado(e.target.value)} max={hojeIso()} />
      </Campo>
      <div className="acoes">
        <button className="botao" type="submit" disabled={enviando}>{enviando ? 'Salvando…' : 'Salvar resultado'}</button>
        <button className="botao botao-leve" type="button" onClick={onCancelar}>Cancelar</button>
      </div>
    </form>
  );
}
