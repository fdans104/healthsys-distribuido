import { useEffect, useState } from 'react';
import { prontuarioApi } from '../../api.js';
import { podeRegistrarConsulta } from '../../permissoes.js';
import { agoraIsoLocal, formatarDataHora } from '../../formatos.js';
import Campo from '../Campo.jsx';
import { Assinatura, ListaRegistros, SoLeitura } from './Registros.jsx';

const VAZIO = () => ({ data: agoraIsoLocal(), motivo: '', avaliacao: '', diagnostico: '', conduta: '' });

/** RF11: consultas médicas no prontuário (serviço de prontuário, MongoDB). Só o médico registra. */
export default function AbaConsultas({ pacienteId, perfil, mostrarAviso, setErro }) {
  const [itens, setItens] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [campos, setCampos] = useState(VAZIO);
  const [errosCampo, setErrosCampo] = useState({});
  const [enviando, setEnviando] = useState(false);
  const podeRegistrar = podeRegistrarConsulta(perfil);

  async function carregar() {
    setCarregando(true);
    try {
      setItens(await prontuarioApi.listarConsultas(pacienteId));
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
      await prontuarioApi.registrarConsulta(pacienteId, {
        data: campos.data || null,
        motivo: campos.motivo,
        avaliacao: campos.avaliacao || null,
        diagnostico: campos.diagnostico || null,
        conduta: campos.conduta || null,
      });
      setCampos(VAZIO());
      mostrarAviso('Consulta registrada no prontuário.');
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
      <h3 className="titulo-aba">Consultas</h3>

      {podeRegistrar ? (
        <form className="form-ficha" onSubmit={registrar} noValidate>
          <div className="campo-duplo">
            <Campo rotulo="Data e hora" erro={errosCampo.data}>
              <input name="data" type="datetime-local" value={campos.data} onChange={alterar} max={agoraIsoLocal()} />
            </Campo>
            <Campo rotulo="Diagnóstico (opcional)" erro={errosCampo.diagnostico}>
              <input name="diagnostico" value={campos.diagnostico} onChange={alterar} placeholder="Ex.: Faringite aguda" />
            </Campo>
          </div>
          <Campo rotulo="Motivo da consulta" erro={errosCampo.motivo}>
            <input name="motivo" value={campos.motivo} onChange={alterar} placeholder="Queixa que trouxe o paciente" />
          </Campo>
          <Campo rotulo="Avaliação (opcional)" erro={errosCampo.avaliacao}>
            <textarea name="avaliacao" rows={3} value={campos.avaliacao} onChange={alterar} placeholder="História e exame físico" />
          </Campo>
          <Campo rotulo="Conduta (opcional)" erro={errosCampo.conduta}>
            <textarea name="conduta" rows={2} value={campos.conduta} onChange={alterar} placeholder="Orientações, exames, medicamentos, retorno" />
          </Campo>
          <button className="botao" type="submit" disabled={enviando}>{enviando ? 'Registrando…' : 'Registrar consulta'}</button>
        </form>
      ) : (
        <SoLeitura>As consultas são registradas pelo médico.</SoLeitura>
      )}

      <ListaRegistros itens={itens} carregando={carregando} vazio="Nenhuma consulta registrada.">
        {itens.map((c) => (
          <li key={c.id} className="registro">
            <div className="registro-topo">
              <p className="registro-titulo">{c.motivo}</p>
              <span className="num">{formatarDataHora(c.data)}</span>
            </div>
            {c.avaliacao && <p className="registro-texto"><b>Avaliação:</b> {c.avaliacao}</p>}
            {c.diagnostico && <p className="registro-texto"><b>Diagnóstico:</b> {c.diagnostico}</p>}
            {c.conduta && <p className="registro-texto"><b>Conduta:</b> {c.conduta}</p>}
            <Assinatura nome={c.registradoPorNome} perfil={c.registradoPorPerfil} em={c.registradoEm} />
          </li>
        ))}
      </ListaRegistros>
    </>
  );
}
