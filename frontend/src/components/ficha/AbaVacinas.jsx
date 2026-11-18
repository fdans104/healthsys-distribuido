import { useEffect, useState } from 'react';
import { fichaApi } from '../../api.js';
import { podeRegistrarVacina } from '../../permissoes.js';
import { formatarData, hojeIso } from '../../formatos.js';
import Campo from '../Campo.jsx';
import { Assinatura, ListaRegistros, SoLeitura } from './Registros.jsx';

const VAZIO = { nomeVacina: '', dataAplicacao: '', dose: '', lote: '' };

/** RF09: vacinas aplicadas. Só a enfermagem registra (é ela quem aplica). */
export default function AbaVacinas({ pacienteId, perfil, mostrarAviso, setErro }) {
  const [itens, setItens] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [campos, setCampos] = useState({ ...VAZIO, dataAplicacao: hojeIso() });
  const [errosCampo, setErrosCampo] = useState({});
  const [enviando, setEnviando] = useState(false);
  const podeRegistrar = podeRegistrarVacina(perfil);

  async function carregar() {
    setCarregando(true);
    try {
      setItens(await fichaApi.listarVacinas(pacienteId));
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
      const dados = {
        nomeVacina: campos.nomeVacina,
        dataAplicacao: campos.dataAplicacao || null,
        dose: campos.dose || null,
        lote: campos.lote || null,
      };
      const nova = await fichaApi.registrarVacina(pacienteId, dados);
      setCampos({ ...VAZIO, dataAplicacao: hojeIso() });
      mostrarAviso(`Vacina ${nova.nomeVacina} registrada.`);
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
      <h3 className="titulo-aba">Vacinas</h3>

      {podeRegistrar ? (
        <form className="form-ficha" onSubmit={registrar} noValidate>
          <Campo rotulo="Vacina" erro={errosCampo.nomeVacina}>
            <input name="nomeVacina" value={campos.nomeVacina} onChange={alterar} placeholder="Ex.: Hepatite B" />
          </Campo>
          <div className="campo-duplo">
            <Campo rotulo="Data da aplicação" erro={errosCampo.dataAplicacao}>
              <input name="dataAplicacao" type="date" value={campos.dataAplicacao} onChange={alterar} max={hojeIso()} />
            </Campo>
            <Campo rotulo="Dose (opcional)" erro={errosCampo.dose}>
              <input name="dose" value={campos.dose} onChange={alterar} placeholder="Ex.: 1ª dose" />
            </Campo>
          </div>
          <Campo rotulo="Lote (opcional)" erro={errosCampo.lote}>
            <input name="lote" value={campos.lote} onChange={alterar} />
          </Campo>
          <button className="botao" type="submit" disabled={enviando}>
            {enviando ? 'Registrando…' : 'Registrar vacina'}
          </button>
        </form>
      ) : (
        <SoLeitura>O registro de vacinas é feito pela enfermagem, que aplica a vacina.</SoLeitura>
      )}

      <ListaRegistros itens={itens} carregando={carregando} vazio="Nenhuma vacina registrada.">
        {itens.map((v) => (
          <li key={v.id} className="registro">
            <div className="registro-topo">
              <p className="registro-titulo">{v.nomeVacina}</p>
              <span className="num">{formatarData(v.dataAplicacao)}</span>
            </div>
            {(v.dose || v.lote) && (
              <p className="registro-texto">{[v.dose, v.lote ? `lote ${v.lote}` : null].filter(Boolean).join(' · ')}</p>
            )}
            <Assinatura nome={v.registradoPorNome} perfil={v.registradoPorPerfil} em={v.registradoEm} />
          </li>
        ))}
      </ListaRegistros>
    </>
  );
}
