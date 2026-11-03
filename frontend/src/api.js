// Toda conversa com o backend passa por aqui.
// O endereço é relativo (/api/...): quem repassa para o auth-service é o nginx (ou o Vite em dev).
const BASE = '/api/v1/usuarios';

export class ApiError extends Error {
  constructor(mensagem, campos = {}) {
    super(mensagem);
    this.campos = campos; // erros por campo, ex.: { email: "email inválido" }
  }
}

async function requisicao(caminho = '', opcoes = {}) {
  let resposta;
  try {
    resposta = await fetch(BASE + caminho, {
      headers: { 'Content-Type': 'application/json' },
      ...opcoes,
    });
  } catch {
    throw new ApiError('Não foi possível falar com o servidor. O backend está ligado?');
  }

  if (resposta.status === 204) return null;

  const corpo = await resposta.json().catch(() => null);
  if (!resposta.ok) {
    if (!corpo) throw new ApiError(`Servidor indisponível (erro ${resposta.status}).`);
    throw new ApiError(corpo.detail || `Erro ${resposta.status}`, corpo.erros || {});
  }
  return corpo;
}

export const usuariosApi = {
  listar: () => requisicao(),
  criar: (dados) => requisicao('', { method: 'POST', body: JSON.stringify(dados) }),
  atualizar: (id, dados) => requisicao(`/${id}`, { method: 'PUT', body: JSON.stringify(dados) }),
  remover: (id) => requisicao(`/${id}`, { method: 'DELETE' }),
};
