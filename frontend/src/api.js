// Toda conversa com o backend passa por aqui.
// O endereço é relativo (/api/...): o nginx (ou o Vite em dev) repassa ao API Gateway,
// que encaminha para o microsserviço certo (auth, pacientes, prontuário, triagem, notificações ou monitoramento).

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

// Notificações (notification-service): avisos de cada perfil, em tempo real.
const NOTIFICACOES = '/api/v1/notificacoes';
export const notificacoesApi = {
  listar: () => requisicao(NOTIFICACOES),
  naoLidas: () => requisicao(`${NOTIFICACOES}/nao-lidas`),
  marcarLida: (id) => requisicao(`${NOTIFICACOES}/${encodeURIComponent(id)}/lida`, { method: 'PUT' }),
  marcarTodas: () => requisicao(`${NOTIFICACOES}/lidas`, { method: 'PUT' }),

  /**
   * Abre o canal em tempo real (Server-Sent Events) e chama aoReceber(notificacao) a cada aviso novo.
   * Usa fetch (e não EventSource) para poder mandar o token no cabeçalho.
   * A promessa termina quando a conexão cai; quem chamou decide quando reconectar.
   */
  async ouvir({ aoReceber, aoConectar, sinal }) {
    const token = tokenAtual();
    if (!token) throw new ApiError('Sem sessão.', {}, 401);
    const resposta = await fetch(`${NOTIFICACOES}/stream`, {
      headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
      signal: sinal,
    });
    if (resposta.status === 401) {
      sessao.limpar();
      aoSessaoExpirar();
      throw new ApiError('Sua sessão expirou. Faça login novamente.', {}, 401);
    }
    if (!resposta.ok || !resposta.body) {
      throw new ApiError(`Canal de notificações indisponível (erro ${resposta.status}).`, {}, resposta.status);
    }
    aoConectar?.();
    const leitor = resposta.body.getReader();
    const decodificador = new TextDecoder();
    let pendente = '';
    for (;;) {
      const { value, done } = await leitor.read();
      if (done) return;
      pendente += decodificador.decode(value, { stream: true }).replace(/\r\n/g, '\n');
      let fim;
      while ((fim = pendente.indexOf('\n\n')) >= 0) {
        const bloco = pendente.slice(0, fim);
        pendente = pendente.slice(fim + 2);
        let evento = 'message';
        const dados = [];
        for (const linha of bloco.split('\n')) {
          if (linha.startsWith('event:')) evento = linha.slice(6).trim();
          else if (linha.startsWith('data:')) dados.push(linha.slice(5).replace(/^ /, ''));
        }
        if (evento === 'notificacao' && dados.length) {
          try {
            aoReceber(JSON.parse(dados.join('\n')));
          } catch {
            // aviso com formato inesperado: ignora
          }
        }
      }
    }
  },
};

// Leitos (monitoring-service): ocupação, internação, alta e higienização.
const LEITOS = '/api/v1/leitos';
export const leitosApi = {
  listar: () => requisicao(LEITOS),
  ocupacao: () => requisicao(`${LEITOS}/ocupacao`),
  criar: (dados) => requisicao(LEITOS, json('POST', dados)),
  remover: (id) => requisicao(`${LEITOS}/${id}`, { method: 'DELETE' }),
  ocupar: (id, dados) => requisicao(`${LEITOS}/${id}/ocupar`, json('PUT', dados)),
  liberar: (id, versao) => requisicao(`${LEITOS}/${id}/liberar`, json('PUT', { versao })),
  higienizado: (id, versao) => requisicao(`${LEITOS}/${id}/higienizado`, json('PUT', { versao })),
};

// Monitoramento ao vivo dos sinais vitais (monitores dos leitos via MQTT) e alertas. Só médicos e enfermeiros.
const MONITORAMENTO = '/api/v1/monitoramento';
export const monitoramentoApi = {
  leitos: () => requisicao(`${MONITORAMENTO}/leitos`),
  alertas: (ativos = true) => requisicao(`${MONITORAMENTO}/alertas?ativos=${ativos}`),
  reconhecer: (id) => requisicao(`${MONITORAMENTO}/alertas/${id}/reconhecer`, { method: 'PUT' }),
};
