import { useEffect, useState } from 'react';
import { pacientesApi, triagemApi } from '../../api.js';
import { formatarCpf } from '../../formatos.js';
import Campo from '../Campo.jsx';

/**
 * RF14: abrir a triagem quando o paciente chega. Escolhe o paciente pela busca
 * (ou já vem escolhido, quando aberta pela ficha) e informa a queixa principal.
 */
export default function NovaTriagemForm({ pacienteFixo, onAberta, onCancelar }) {
  const [paciente, setPaciente] = useState(pacienteFixo ?? null);
  const [busca, setBusca] = useState('');
  const [resultados, setResultados] = useState([]);
  const [queixa, setQueixa] = useState('');
  const [errosCampo, setErrosCampo] = useState({});
  const [erroGeral, setErroGeral] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    if (paciente || busca.trim().length < 2) {
      setResultados([]);
      return undefined;
    }
    let ativo = true;
    const t = setTimeout(() => {
      pacientesApi.listar(busca)
        .then((lista) => { if (ativo) setResultados(lista.slice(0, 6)); })
        .catch((e) => { if (ativo) setErroGeral(e.message); });
    }, 300);
    return () => { ativo = false; clearTimeout(t); };
  }, [busca, paciente]);

  async function enviar(e) {
    e.preventDefault();
    if (!paciente) {
      setErrosCampo({ pacienteId: 'Escolha o paciente' });
      return;
    }
    setEnviando(true);
    setErrosCampo({});
    setErroGeral('');
    try {
      const triagem = await triagemApi.abrir({ pacienteId: paciente.id, queixaPrincipal: queixa });
      onAberta(triagem);
    } catch (err) {
      setErrosCampo(err.campos || {});
      setErroGeral(err.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <form onSubmit={enviar} noValidate>
      {paciente ? (
        <div className="escolhido">
          <div>
            <strong>{paciente.nome}</strong>
            {paciente.cpf && <div className="linha-secundaria">CPF {formatarCpf(paciente.cpf)}</div>}
          </div>
          {!pacienteFixo && (
            <button type="button" className="botao botao-leve" onClick={() => setPaciente(null)}>Trocar</button>
          )}
        </div>
      ) : (
        <Campo rotulo="Paciente" erro={errosCampo.pacienteId}>
          <input value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Digite o nome ou o CPF" autoFocus />
          {resultados.length > 0 && (
            <ul className="sugestoes">
              {resultados.map((p) => (
                <li key={p.id}>
                  <button type="button" onClick={() => setPaciente(p)}>
                    {p.nome} <span className="linha-secundaria">{p.cpf ? `· CPF ${formatarCpf(p.cpf)}` : ''}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </Campo>
      )}

      <Campo rotulo="Queixa principal" erro={errosCampo.queixaPrincipal}>
        <input value={queixa} onChange={(e) => setQueixa(e.target.value)} placeholder="Ex.: dor no peito, febre há 2 dias" />
      </Campo>

      {erroGeral && <p className="erro-geral">{erroGeral}</p>}

      <div className="acoes">
        <button className="botao" type="submit" disabled={enviando}>{enviando ? 'Abrindo…' : 'Abrir triagem'}</button>
        {onCancelar && <button className="botao botao-leve" type="button" onClick={onCancelar}>Cancelar</button>}
      </div>
    </form>
  );
}
