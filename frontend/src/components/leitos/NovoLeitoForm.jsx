import { useState } from 'react';
import { leitosApi } from '../../api.js';
import { ROTULO_SETOR, SETORES } from '../../formatos.js';
import Campo from '../Campo.jsx';

/** O administrador cadastra um leito novo na estrutura do hospital. */
export default function NovoLeitoForm({ onCriado, onCancelar }) {
  const [codigo, setCodigo] = useState('');
  const [setor, setSetor] = useState('ENFERMARIA');
  const [errosCampo, setErrosCampo] = useState({});
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);

  async function enviar(e) {
    e.preventDefault();
    setEnviando(true);
    setErrosCampo({});
    setErro('');
    try {
      onCriado(await leitosApi.criar({ codigo, setor }));
    } catch (err) {
      setErrosCampo(err.campos || {});
      setErro(err.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <form onSubmit={enviar} noValidate>
      <Campo rotulo="Código do leito" erro={errosCampo.codigo}>
        <input value={codigo} onChange={(e) => setCodigo(e.target.value.toUpperCase())} placeholder="Ex.: ENF-07" autoFocus />
      </Campo>
      <Campo rotulo="Setor" erro={errosCampo.setor}>
        <select value={setor} onChange={(e) => setSetor(e.target.value)}>
          {SETORES.map((s) => <option key={s} value={s}>{ROTULO_SETOR[s]}</option>)}
        </select>
      </Campo>
      <p className="nota">Para o leito aparecer no monitoramento ao vivo, o monitor dele precisa publicar no tópico
        <code> healthsys/leitos/{codigo || 'CODIGO'}/sinais</code>.</p>
      {erro && <p className="erro-geral">{erro}</p>}
      <div className="acoes">
        <button className="botao" type="submit" disabled={enviando}>{enviando ? 'Salvando…' : 'Cadastrar leito'}</button>
        <button className="botao botao-leve" type="button" onClick={onCancelar}>Cancelar</button>
      </div>
    </form>
  );
}
