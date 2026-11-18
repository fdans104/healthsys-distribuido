// Toda conversa com o backend passa por aqui.
// O endereço é relativo (/api/...): o nginx (ou o Vite em dev) repassa ao API Gateway,
// que encaminha para o microsserviço certo (auth-service ou patient-service).

export class ApiError extends Error {
  constructor(mensagem, campos = {}, status = 0) {
    super(mensagem);
    this.campos = campos; // erros por campo, ex.: { email: "email inválido" }
    this.status = status;
  }
}

// ---------- Sessão (token JWT) ----------
const CHAVE_SESSAO = 'healthsys.sessao';

export const sessao = {
  /** Devolve { token, expiraEm, usuario } ou null se não houver sessão válida. */
  obter() {
    try {
      const s = JSON.parse(localStorage.getItem(CHAVE_SESSAO));
      if (!s?.token || new Date(s.expiraEm) <= new Date()) return null;
      return s;
    } catch {
      return null;
    }
  },
  salvar({ token, expiraEm, usuario }) {
    try {
      localStorage.setItem(CHAVE_SESSAO, JSON.stringify({ token, expiraEm, usuario }));
    } catch {
      // navegador sem localStorage (ex.: aba privada): a sessão vale até recarregar a página
    }
    sessaoEmMemoria = { token, expiraEm, usuario };
  },
  limpar() {
    try {
      localStorage.removeItem(CHAVE_SESSAO);
    } catch {
      // ignora
    }
    sessaoEmMemoria = null;
  },
};

let sessaoEmMemoria = null;
function tokenAtual() {
  return (sessao.obter() ?? sessaoEmMemoria)?.token;
}

// O App registra aqui o que fazer quando o servidor recusar o token (volta para o login).
let aoSessaoExpirar = () => {};
export function definirAoSessaoExpirar(fn) {
  aoSessaoExpirar = fn;
}

// ---------- Requisição genérica ----------
async function requisicao(caminho, opcoes = {}) {
  const token = tokenAtual();
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;

  let resposta;
  try {
    resposta = await fetch(caminho, { ...opcoes, headers });
  } catch {
    throw new ApiError('Não foi possível falar com o servidor. O backend está ligado?');
  }

  if (resposta.status === 204) return null;

  const corpo = await resposta.json().catch(() => null);

  if (resposta.status === 401 && token) {
    sessao.limpar();
    aoSessaoExpirar();
    throw new ApiError('Sua sessão expirou. Faça login novamente.', {}, 401);
  }
  if (!resposta.ok) {
    if (!corpo) throw new ApiError(`Servidor indisponível (erro ${resposta.status}).`, {}, resposta.status);
    throw new ApiError(corpo.detail || `Erro ${resposta.status}`, corpo.erros || {}, resposta.status);
  }
  return corpo;
}

const json = (metodo, dados) => ({ method: metodo, body: JSON.stringify(dados) });

// ---------- Endpoints ----------
export const authApi = {
  login: (email, senha) => requisicao('/api/v1/auth/login', json('POST', { email, senha })),
  eu: () => requisicao('/api/v1/auth/me'),
};

const USUARIOS = '/api/v1/usuarios';
export const usuariosApi = {
  listar: () => requisicao(USUARIOS),
  criar: (dados) => requisicao(USUARIOS, json('POST', dados)),
  atualizar: (id, dados) => requisicao(`${USUARIOS}/${id}`, json('PUT', dados)),
  remover: (id) => requisicao(`${USUARIOS}/${id}`, { method: 'DELETE' }),
};

const PACIENTES = '/api/v1/pacientes';
export const pacientesApi = {
  listar: (busca = '') =>
    requisicao(busca.trim() ? `${PACIENTES}?busca=${encodeURIComponent(busca.trim())}` : PACIENTES),
  criar: (dados) => requisicao(PACIENTES, json('POST', dados)),
  atualizar: (id, dados) => requisicao(`${PACIENTES}/${id}`, json('PUT', dados)),
  remover: (id) => requisicao(`${PACIENTES}/${id}`, { method: 'DELETE' }),
};

// Ficha clínica do paciente (só médicos e enfermeiros têm acesso).
export const fichaApi = {
  historico: (pacienteId) => requisicao(`${PACIENTES}/${pacienteId}/historico`),
  listarAlergias: (pacienteId) => requisicao(`${PACIENTES}/${pacienteId}/alergias`),
  registrarAlergia: (pacienteId, dados) => requisicao(`${PACIENTES}/${pacienteId}/alergias`, json('POST', dados)),
  listarVacinas: (pacienteId) => requisicao(`${PACIENTES}/${pacienteId}/vacinas`),
  registrarVacina: (pacienteId, dados) => requisicao(`${PACIENTES}/${pacienteId}/vacinas`, json('POST', dados)),
  listarAtendimentos: (pacienteId) => requisicao(`${PACIENTES}/${pacienteId}/atendimentos`),
  registrarAtendimento: (pacienteId, dados) =>
    requisicao(`${PACIENTES}/${pacienteId}/atendimentos`, json('POST', dados)),
};

// Prontuário eletrônico (record-service): consultas, exames e medicamentos. Só médicos e enfermeiros.
const PRONTUARIOS = '/api/v1/prontuarios';
export const prontuarioApi = {
  completo: (pacienteId) => requisicao(`${PRONTUARIOS}/${pacienteId}`),
  listarConsultas: (pacienteId) => requisicao(`${PRONTUARIOS}/${pacienteId}/consultas`),
  registrarConsulta: (pacienteId, dados) => requisicao(`${PRONTUARIOS}/${pacienteId}/consultas`, json('POST', dados)),
  listarExames: (pacienteId) => requisicao(`${PRONTUARIOS}/${pacienteId}/exames`),
  solicitarExame: (pacienteId, dados) => requisicao(`${PRONTUARIOS}/${pacienteId}/exames`, json('POST', dados)),
  registrarResultado: (pacienteId, exameId, dados) =>
    requisicao(`${PRONTUARIOS}/${pacienteId}/exames/${exameId}/resultado`, json('PUT', dados)),
  listarMedicamentos: (pacienteId) => requisicao(`${PRONTUARIOS}/${pacienteId}/medicamentos`),
  prescrever: (pacienteId, dados) => requisicao(`${PRONTUARIOS}/${pacienteId}/medicamentos`, json('POST', dados)),
  suspender: (pacienteId, medicamentoId, dados) =>
    requisicao(`${PRONTUARIOS}/${pacienteId}/medicamentos/${medicamentoId}/suspensao`, json('PUT', dados)),
};

// Teletriagem (triage-service): fila do pronto-atendimento.
const TRIAGENS = '/api/v1/triagens';
export const triagemApi = {
  listar: ({ status, pacienteId } = {}) => {
    const params = new URLSearchParams();
    if (status) params.set('status', status);
    if (pacienteId) params.set('pacienteId', pacienteId);
    const qs = params.toString();
    return requisicao(qs ? `${TRIAGENS}?${qs}` : TRIAGENS);
  },
  abrir: (dados) => requisicao(TRIAGENS, json('POST', dados)),
  avaliar: (id, dados) => requisicao(`${TRIAGENS}/${id}/avaliacao`, json('PUT', dados)),
  mudarStatus: (id, dados) => requisicao(`${TRIAGENS}/${id}/status`, json('PUT', dados)),
};
