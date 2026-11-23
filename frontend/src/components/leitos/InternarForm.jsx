import { useEffect, useState } from 'react';
import { leitosApi, pacientesApi } from '../../api.js';
import { formatarCpf } from '../../formatos.js';
import Campo from '../Campo.jsx';

/** Internação: escolhe o paciente pela busca e ocupa o leito. */
export default function InternarForm({ leito, onInternado, onConflito, onCancelar }) {
  const [paciente, setPaciente] = useState(null);
  const [busca, setBusca] = useState('');
  const [resultados, setResultados] = useState([]);
  const [erro, setErro] = useState('');
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
        .catch((e) => { if (ativo) setErro(e.message); });
    }, 300);
    return () => { ativo = false; clearTimeout(t); };
  }, [busca, paciente]);

  async function enviar(e) {
    e.preventDefault();
    if (!paciente) {
      setErro('Escolha o paciente.');
      return;
    }
    setEnviando(true);
    setErro('');
    try {
      onInternado(await leitosApi.ocupar(leito.id, { pacienteId: paciente.id, versao: leito.versao }));
    } catch (err) {
      if (err.status === 409 && err.campos?.versao) {
        onConflito(err.message);
        return;
      }
      setErro(err.message);
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
          <button type="button" className="botao botao-leve" onClick={() => setPaciente(null)}>Trocar</button>
        </div>
      ) : (
        <Campo rotulo="Paciente">
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

      <p className="nota">
        Assim que o paciente é internado, o monitor do leito {leito.codigo} passa a mostrar os sinais vitais dele ao vivo.
      </p>

      {erro && <p className="erro-geral">{erro}</p>}

      <div className="acoes">
        <button className="botao" type="submit" disabled={enviando}>{enviando ? 'Internando…' : `Internar no ${leito.codigo}`}</button>
        <button className="botao botao-leve" type="button" onClick={onCancelar}>Cancelar</button>
      </div>
    </form>
  );
}
