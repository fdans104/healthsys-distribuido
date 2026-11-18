import { useEffect, useState } from 'react';
import { fichaApi } from '../../api.js';
import { podeRegistrarAtendimento } from '../../permissoes.js';
import { ROTULO_TIPO_ATENDIMENTO, agoraIsoLocal, formatarDataHora } from '../../formatos.js';
import Campo from '../Campo.jsx';
import { Assinatura, ListaRegistros, SoLeitura } from './Registros.jsx';

const TIPOS = Object.entries(ROTULO_TIPO_ATENDIMENTO);

/** RF06: atendimentos do paciente. Só o médico registra (consulta, retorno, urgência, emergência). */
export default function AbaAtendimentos({ pacienteId, perfil, mostrarAviso, setErro }) {
  const [itens, setItens] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [campos, setCampos] = useState({ tipo: 'CONSULTA', data: agoraIsoLocal(), observacoes: '' });
  const [errosCampo, setErrosCampo] = useState({});
  const [enviando, setEnviando] = useState(false);
  const podeRegistrar = podeRegistrarAtendimento(perfil);

  async function carregar() {
    setCarregando(true);
    try {
      setItens(await fichaApi.listarAtendimentos(pacienteId));
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

  async function registrar(e) {
    e.preventDefault();
    setEnviando(true);
    setErrosCampo({});
    setErro('');
    try {
      const dados = { tipo: campos.tipo, data: campos.data || null, observacoes: campos.observacoes || null };
      const novo = await fichaApi.registrarAtendimento(pacienteId, dados);
      setCampos({ tipo: 'CONSULTA', data: agoraIsoLocal(), observacoes: '' });
      mostrarAviso(`${ROTULO_TIPO_ATENDIMENTO[novo.tipo] ?? 'Atendimento'} registrado.`);
      await carregar();
    } catch (err) {
      setErrosCampo(err.campos || {});
      setErro(err.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <>
      <h3 className="titulo-aba">Atendimentos</h3>

      {podeRegistrar ? (
        <form className="form-ficha" onSubmit={registrar} noValidate>
          <div className="campo-duplo">
            <Campo rotulo="Tipo" erro={errosCampo.tipo}>
              <select name="tipo" value={campos.tipo} onChange={alterar}>
                {TIPOS.map(([valor, rotulo]) => <option key={valor} value={valor}>{rotulo}</option>)}
              </select>
            </Campo>
            <Campo rotulo="Data e hora" erro={errosCampo.data}>
              <input name="data" type="datetime-local" value={campos.data} onChange={alterar} max={agoraIsoLocal()} />
            </Campo>
          </div>
          <Campo rotulo="Observações (opcional)" erro={errosCampo.observacoes}>
            <textarea name="observacoes" rows={4} value={campos.observacoes} onChange={alterar}
                      placeholder="Queixa, avaliação e conduta" />
          </Campo>
          <button className="botao" type="submit" disabled={enviando}>
            {enviando ? 'Registrando…' : 'Registrar atendimento'}
          </button>
        </form>
      ) : (
        <SoLeitura>O registro de atendimentos é feito pelo médico.</SoLeitura>
      )}

      <ListaRegistros itens={itens} carregando={carregando} vazio="Nenhum atendimento registrado.">
        {itens.map((a) => (
          <li key={a.id} className="registro">
            <div className="registro-topo">
              <span className="etiqueta etiqueta-atendimento">{ROTULO_TIPO_ATENDIMENTO[a.tipo] ?? a.tipo}</span>
              <span className="num">{formatarDataHora(a.data)}</span>
            </div>
            {a.observacoes && <p className="registro-texto">{a.observacoes}</p>}
            <Assinatura nome={a.registradoPorNome} perfil={a.registradoPorPerfil} em={a.registradoEm} />
          </li>
        ))}
      </ListaRegistros>
    </>
  );
}
