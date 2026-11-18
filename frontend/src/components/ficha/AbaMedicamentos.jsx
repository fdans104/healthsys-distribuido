import { useEffect, useState } from 'react';
import { prontuarioApi } from '../../api.js';
import { podePrescrever } from '../../permissoes.js';
import { ROTULO_VIA, formatarData, formatarDataHora, hojeIso } from '../../formatos.js';
import Campo from '../Campo.jsx';
import { ListaRegistros, SoLeitura } from './Registros.jsx';

const VIAS = Object.entries(ROTULO_VIA);
const VAZIO = () => ({ nome: '', dosagem: '', via: 'ORAL', frequencia: '', dataInicio: hojeIso(), dataFim: '', observacoes: '' });

/** RF13: medicamentos. O médico prescreve e, quando for o caso, suspende informando o motivo. */
export default function AbaMedicamentos({ pacienteId, perfil, mostrarAviso, setErro }) {
  const [itens, setItens] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [campos, setCampos] = useState(VAZIO);
  const [errosCampo, setErrosCampo] = useState({});
  const [enviando, setEnviando] = useState(false);
  const medico = podePrescrever(perfil);

  async function carregar() {
    setCarregando(true);
    try {
      setItens(await prontuarioApi.listarMedicamentos(pacienteId));
    } catch (e) {
      setErro(e.message);
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    carregar();
  }, [pacienteId]);

  function alterar(e) {
    setCampos({ ...campos, [e.target.name]: e.target.value });
  }

  async function prescrever(e) {
    e.preventDefault();
    setEnviando(true);
    setErrosCampo({});
    setErro('');
    try {
      const m = await prontuarioApi.prescrever(pacienteId, {
        ...campos,
        dataInicio: campos.dataInicio || null,
        dataFim: campos.dataFim || null,
        observacoes: campos.observacoes || null,
      });
      setCampos(VAZIO());
      mostrarAviso(`${m.nome} prescrito.`);
      await carregar();
    } catch (err) {
      setErrosCampo(err.campos || {});
      setErro(err.message);
    } finally {
      setEnviando(false);
    }
  }

  async function suspender(m) {
    const motivo = window.prompt(`Motivo para suspender ${m.nome}:`);
    if (motivo == null) return;
    setErro('');
    try {
      await prontuarioApi.suspender(pacienteId, m.id, { motivo });
      mostrarAviso(`${m.nome} suspenso.`);
      await carregar();
    } catch (err) {
      setErro(err.campos?.motivo ?? err.message);
    }
  }

  const ativos = itens.filter((m) => m.status === 'ATIVO');
  const suspensos = itens.filter((m) => m.status === 'SUSPENSO');

  return (
    <>
      <h3 className="titulo-aba">Medicamentos</h3>

      {medico ? (
        <form className="form-ficha" onSubmit={prescrever} noValidate>
          <div className="campo-duplo">
            <Campo rotulo="Medicamento" erro={errosCampo.nome}>
              <input name="nome" value={campos.nome} onChange={alterar} placeholder="Ex.: Amoxicilina" />
            </Campo>
            <Campo rotulo="Dosagem" erro={errosCampo.dosagem}>
              <input name="dosagem" value={campos.dosagem} onChange={alterar} placeholder="Ex.: 500 mg" />
            </Campo>
          </div>
          <div className="campo-duplo">
            <Campo rotulo="Via" erro={errosCampo.via}>
              <select name="via" value={campos.via} onChange={alterar}>
                {VIAS.map(([valor, rotulo]) => <option key={valor} value={valor}>{rotulo}</option>)}
              </select>
            </Campo>
            <Campo rotulo="Frequência" erro={errosCampo.frequencia}>
              <input name="frequencia" value={campos.frequencia} onChange={alterar} placeholder="Ex.: de 8 em 8 horas" />
            </Campo>
          </div>
          <div className="campo-duplo">
            <Campo rotulo="Início" erro={errosCampo.dataInicio}>
              <input name="dataInicio" type="date" value={campos.dataInicio} onChange={alterar} />
            </Campo>
            <Campo rotulo="Fim (opcional)" erro={errosCampo.dataFim}>
              <input name="dataFim" type="date" value={campos.dataFim} onChange={alterar} min={campos.dataInicio || undefined} />
            </Campo>
          </div>
          <Campo rotulo="Observações (opcional)" erro={errosCampo.observacoes}>
            <input name="observacoes" value={campos.observacoes} onChange={alterar} placeholder="Ex.: tomar após as refeições" />
          </Campo>
          <button className="botao" type="submit" disabled={enviando}>{enviando ? 'Prescrevendo…' : 'Prescrever'}</button>
        </form>
      ) : (
        <SoLeitura>Os medicamentos são prescritos e suspensos pelo médico.</SoLeitura>
      )}

      <h4 className="subtitulo-aba">Em uso</h4>
      <ListaRegistros itens={ativos} carregando={carregando} vazio="Nenhum medicamento em uso.">
        {ativos.map((m) => (
          <Item key={m.id} m={m} acao={medico && (
            <button className="botao botao-perigo" onClick={() => suspender(m)}>Suspender</button>
          )} />
        ))}
      </ListaRegistros>

      {suspensos.length > 0 && (
        <>
          <h4 className="subtitulo-aba">Suspensos</h4>
          <ul className="registros">
            {suspensos.map((m) => <Item key={m.id} m={m} />)}
          </ul>
        </>
      )}
    </>
  );
}

function Item({ m, acao }) {
  return (
    <li className={`registro ${m.status === 'SUSPENSO' ? 'registro-apagado' : ''}`}>
      <div className="registro-topo">
        <p className="registro-titulo">{m.nome} {m.dosagem}</p>
        {acao}
      </div>
      <p className="registro-texto">
        {ROTULO_VIA[m.via] ?? m.via} · {m.frequencia} · a partir de {formatarData(m.dataInicio)}
        {m.dataFim ? ` até ${formatarData(m.dataFim)}` : ''}
      </p>
      {m.observacoes && <p className="registro-texto">{m.observacoes}</p>}
      <p className="linha-secundaria">Prescrito por {m.prescritoPorNome} em {formatarDataHora(m.prescritoEm)}</p>
      {m.status === 'SUSPENSO' && (
        <p className="linha-secundaria">
          Suspenso por {m.suspensoPorNome} em {formatarDataHora(m.suspensoEm)}: {m.motivoSuspensao}
        </p>
      )}
    </li>
  );
}
