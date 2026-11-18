import { useEffect, useState } from 'react';
import { fichaApi } from '../../api.js';
import { podeRegistrarAlergia } from '../../permissoes.js';
import Campo from '../Campo.jsx';
import { Assinatura, ListaRegistros } from './Registros.jsx';

/** RF08: alergias do paciente. Médicos e enfermeiros registram. */
export default function AbaAlergias({ pacienteId, perfil, mostrarAviso, setErro }) {
  const [itens, setItens] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [descricao, setDescricao] = useState('');
  const [errosCampo, setErrosCampo] = useState({});
  const [enviando, setEnviando] = useState(false);

  async function carregar() {
    setCarregando(true);
    try {
      setItens(await fichaApi.listarAlergias(pacienteId));
    } catch (e) {
      setErro(e.message);
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    carregar();
  }, [pacienteId]);

  async function registrar(e) {
    e.preventDefault();
    setEnviando(true);
    setErrosCampo({});
    setErro('');
    try {
      const nova = await fichaApi.registrarAlergia(pacienteId, { descricao });
      setDescricao('');
      mostrarAviso(`Alergia a ${nova.descricao} registrada.`);
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
      <h3 className="titulo-aba">Alergias</h3>

      {podeRegistrarAlergia(perfil) && (
        <form className="form-ficha" onSubmit={registrar} noValidate>
          <Campo rotulo="Nova alergia" erro={errosCampo.descricao}>
            <input value={descricao} onChange={(e) => setDescricao(e.target.value)}
                   placeholder="Ex.: Dipirona, Penicilina, Frutos do mar" />
          </Campo>
          <button className="botao" type="submit" disabled={enviando}>
            {enviando ? 'Registrando…' : 'Registrar alergia'}
          </button>
        </form>
      )}

      <ListaRegistros itens={itens} carregando={carregando} vazio="Nenhuma alergia registrada.">
        {itens.map((a) => (
          <li key={a.id} className="registro">
            <p className="registro-titulo">{a.descricao}</p>
            <Assinatura nome={a.registradoPorNome} perfil={a.registradoPorPerfil} em={a.registradoEm} />
          </li>
        ))}
      </ListaRegistros>
    </>
  );
}
