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
