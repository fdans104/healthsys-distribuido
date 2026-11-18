import { useState } from 'react';
import { triagemApi } from '../../api.js';
import { RISCOS } from '../../formatos.js';
import Campo from '../Campo.jsx';

/** RF14/RF15: a enfermagem registra sintomas, sinais vitais e a classificação de risco. */
export default function AvaliacaoForm({ triagem, onAvaliada, onConflito, onCancelar }) {
  const [campos, setCampos] = useState({
    sintomas: triagem.sintomas ?? '',
    pressaoArterial: triagem.pressaoArterial ?? '',
    temperatura: triagem.temperatura ?? '',
    frequenciaCardiaca: triagem.frequenciaCardiaca ?? '',
    saturacao: triagem.saturacao ?? '',
    classificacaoRisco: triagem.classificacaoRisco ?? '',
  });
  const [errosCampo, setErrosCampo] = useState({});
  const [erroGeral, setErroGeral] = useState('');
  const [enviando, setEnviando] = useState(false);

  function alterar(e) {
    setCampos({ ...campos, [e.target.name]: e.target.value });
  }

  const numeroOuNulo = (v) => (v === '' || v == null ? null : Number(String(v).replace(',', '.')));

  async function enviar(e) {
    e.preventDefault();
    setEnviando(true);
    setErrosCampo({});
    setErroGeral('');
    try {
      const salva = await triagemApi.avaliar(triagem.id, {
        sintomas: campos.sintomas,
        pressaoArterial: campos.pressaoArterial || null,
        temperatura: numeroOuNulo(campos.temperatura),
        frequenciaCardiaca: numeroOuNulo(campos.frequenciaCardiaca),
        saturacao: numeroOuNulo(campos.saturacao),
        classificacaoRisco: campos.classificacaoRisco || null,
        versao: triagem.versao, // RN54
      });
      onAvaliada(salva);
    } catch (err) {
      if (err.campos?.versao && onConflito) {
        onConflito(err.message);
        return;
      }
      setErrosCampo(err.campos || {});
      setErroGeral(err.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <form onSubmit={enviar} noValidate>
      <p className="linha-secundaria">Queixa principal: {triagem.queixaPrincipal}</p>

      <Campo rotulo="Sintomas" erro={errosCampo.sintomas}>
        <textarea name="sintomas" rows={3} value={campos.sintomas} onChange={alterar}
                  placeholder="O que o paciente relata e o que foi observado" />
      </Campo>

      <div className="campo-quatro">
        <Campo rotulo="Pressão" erro={errosCampo.pressaoArterial}>
          <input name="pressaoArterial" value={campos.pressaoArterial} onChange={alterar} placeholder="120/80" />
        </Campo>
        <Campo rotulo="Temperatura (°C)" erro={errosCampo.temperatura}>
          <input name="temperatura" value={campos.temperatura} onChange={alterar} inputMode="decimal" placeholder="36,5" />
        </Campo>
        <Campo rotulo="Freq. cardíaca (bpm)" erro={errosCampo.frequenciaCardiaca}>
          <input name="frequenciaCardiaca" value={campos.frequenciaCardiaca} onChange={alterar} inputMode="numeric" placeholder="80" />
        </Campo>
        <Campo rotulo="Saturação (%)" erro={errosCampo.saturacao}>
          <input name="saturacao" value={campos.saturacao} onChange={alterar} inputMode="numeric" placeholder="98" />
        </Campo>
      </div>

      <fieldset className={`riscos ${errosCampo.classificacaoRisco ? 'campo-com-erro' : ''}`}>
        <legend>Classificação de risco</legend>
        {RISCOS.map((r) => (
          <label key={r.valor} className={`risco-opcao risco-borda-${r.valor.toLowerCase()} ${campos.classificacaoRisco === r.valor ? 'risco-opcao-marcada' : ''}`}>
            <input type="radio" name="classificacaoRisco" value={r.valor}
                   checked={campos.classificacaoRisco === r.valor} onChange={alterar} />
            <span className={`risco risco-${r.valor.toLowerCase()}`}>{r.rotulo}</span>
            <span className="linha-secundaria">{r.descricao}</span>
          </label>
        ))}
        {errosCampo.classificacaoRisco && <small className="erro-campo">{errosCampo.classificacaoRisco}</small>}
      </fieldset>

      {erroGeral && <p className="erro-geral">{erroGeral}</p>}

      <div className="acoes">
        <button className="botao" type="submit" disabled={enviando}>{enviando ? 'Salvando…' : 'Salvar avaliação'}</button>
        {onCancelar && <button className="botao botao-leve" type="button" onClick={onCancelar}>Cancelar</button>}
      </div>
    </form>
  );
}
