import { useState } from 'react';
import { pacientesApi } from '../api.js';
import { formatarCpf, formatarTelefone, hojeIso } from '../formatos.js';

const SEXOS = [
  { valor: 'F', rotulo: 'Feminino' },
  { valor: 'M', rotulo: 'Masculino' },
  { valor: 'O', rotulo: 'Outro' },
];

/** Formulário de cadastro de paciente. Se receber "paciente", vira formulário de edição. */
export default function PacienteForm({ paciente, onSalvo, onCancelar }) {
  const editando = Boolean(paciente);
  const [campos, setCampos] = useState({
    nome: paciente?.nome ?? '',
    cpf: formatarCpf(paciente?.cpf ?? ''),
    dataNascimento: paciente?.dataNascimento ?? '',
    sexo: paciente?.sexo ?? '',
    telefone: formatarTelefone(paciente?.telefone ?? ''),
    email: paciente?.email ?? '',
    endereco: paciente?.endereco ?? '',
  });
  const [errosCampo, setErrosCampo] = useState({});
  const [erroGeral, setErroGeral] = useState('');
  const [enviando, setEnviando] = useState(false);

  const mascaras = { cpf: formatarCpf, telefone: formatarTelefone };

  function alterar(e) {
    const { name, value } = e.target;
    const formatar = mascaras[name];
    setCampos({ ...campos, [name]: formatar ? formatar(value) : value });
  }

  async function enviar(e) {
    e.preventDefault();
    setEnviando(true);
    setErrosCampo({});
    setErroGeral('');

    // Campos opcionais vazios vão como null; o backend guarda o CPF só com dígitos.
    const dados = {
      ...campos,
      sexo: campos.sexo || null,
      dataNascimento: campos.dataNascimento || null,
      telefone: campos.telefone || null,
      email: campos.email || null,
      endereco: campos.endereco || null,
    };

    try {
      const salvo = editando
        ? await pacientesApi.atualizar(paciente.id, dados)
        : await pacientesApi.criar(dados);
      onSalvo(salvo, editando);
    } catch (err) {
      setErrosCampo(err.campos || {});
      setErroGeral(err.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <form onSubmit={enviar} noValidate>
      <h2>{editando ? `Editar paciente #${paciente.id}` : 'Novo paciente'}</h2>

      <Campo rotulo="Nome completo" erro={errosCampo.nome}>
        <input name="nome" value={campos.nome} onChange={alterar} placeholder="Maria da Silva" />
      </Campo>

      <Campo rotulo="CPF" erro={errosCampo.cpf}>
        <input name="cpf" value={campos.cpf} onChange={alterar} inputMode="numeric" placeholder="000.000.000-00" />
      </Campo>

      <div className="campo-duplo">
        <Campo rotulo="Nascimento" erro={errosCampo.dataNascimento}>
          <input name="dataNascimento" type="date" value={campos.dataNascimento} onChange={alterar} max={hojeIso()} />
        </Campo>

        <Campo rotulo="Sexo" erro={errosCampo.sexo}>
          <select name="sexo" value={campos.sexo} onChange={alterar}>
            <option value="">Selecione</option>
            {SEXOS.map((s) => (
              <option key={s.valor} value={s.valor}>{s.rotulo}</option>
            ))}
          </select>
        </Campo>
      </div>

      <Campo rotulo="Telefone (opcional)" erro={errosCampo.telefone}>
        <input name="telefone" value={campos.telefone} onChange={alterar} inputMode="tel" placeholder="(85) 98888-0000" />
      </Campo>

      <Campo rotulo="E-mail (opcional)" erro={errosCampo.email}>
        <input name="email" type="email" value={campos.email} onChange={alterar} placeholder="maria@email.com" />
      </Campo>

      <Campo rotulo="Endereço (opcional)" erro={errosCampo.endereco}>
        <input name="endereco" value={campos.endereco} onChange={alterar} placeholder="Rua, número, bairro, cidade" />
      </Campo>

      {erroGeral && <p className="erro-geral">{erroGeral}</p>}

      <div className="acoes">
        <button className="botao" type="submit" disabled={enviando}>
          {enviando ? 'Salvando…' : editando ? 'Salvar alterações' : 'Cadastrar'}
        </button>
        {editando && (
          <button className="botao botao-leve" type="button" onClick={onCancelar}>
            Cancelar
          </button>
        )}
      </div>
    </form>
  );
}

function Campo({ rotulo, erro, children }) {
  return (
    <label className={`campo ${erro ? 'campo-com-erro' : ''}`}>
      <span>{rotulo}</span>
      {children}
      {erro && <small>{erro}</small>}
    </label>
  );
}
