// Formatação de valores para exibição e máscaras de digitação.

export const ROTULO_PERFIL = {
  ADMIN: 'Administrador',
  RECEPCIONISTA: 'Recepcionista',
  MEDICO: 'Médico',
  ENFERMEIRO: 'Enfermeiro',
};
export const ROTULO_RISCO = { CRITICO: 'Crítico', ALTO: 'Alto', MEDIO: 'Médio', BAIXO: 'Baixo' };
// Classificação de risco (RN38), do mais grave para o mais leve, com o que cada nível significa na fila.
export const RISCOS = [
  { valor: 'CRITICO', rotulo: 'Crítico', descricao: 'Emergência: atendimento imediato' },
  { valor: 'ALTO', rotulo: 'Alto', descricao: 'Muito urgente' },
  { valor: 'MEDIO', rotulo: 'Médio', descricao: 'Urgente' },
  { valor: 'BAIXO', rotulo: 'Baixo', descricao: 'Pouco urgente' },
];
export const ROTULO_STATUS_TRIAGEM = {
  PENDENTE: 'Aguardando',
  EM_ATENDIMENTO: 'Em atendimento',
  FINALIZADA: 'Finalizada',
  CANCELADA: 'Cancelada',
};
export const ROTULO_TIPO_EXAME = { LABORATORIAL: 'Laboratorial', IMAGEM: 'Imagem', OUTRO: 'Outro' };
export const ROTULO_VIA = {
  ORAL: 'Oral',
  INTRAVENOSA: 'Intravenosa',
  INTRAMUSCULAR: 'Intramuscular',
  SUBCUTANEA: 'Subcutânea',
  TOPICA: 'Tópica',
  INALATORIA: 'Inalatória',
  OUTRA: 'Outra',
};
export const ROTULO_TIPO_ATENDIMENTO = {
  CONSULTA: 'Consulta',
  RETORNO: 'Retorno',
  URGENCIA: 'Urgência',
  EMERGENCIA: 'Emergência',
};
export const ROTULO_SEXO = { M: 'Masculino', F: 'Feminino', O: 'Outro' };
export const ROTULO_SETOR = { UTI: 'UTI', ENFERMARIA: 'Enfermaria', EMERGENCIA: 'Emergência' };
export const SETORES = ['UTI', 'ENFERMARIA', 'EMERGENCIA'];
export const ROTULO_STATUS_LEITO = { LIVRE: 'Livre', OCUPADO: 'Ocupado', HIGIENIZACAO: 'Em higienização' };
export const ROTULO_GRAVIDADE = { CRITICO: 'Crítico', ATENCAO: 'Atenção', INFO: 'Informação' };

const digitos = (v) => (v ?? '').replace(/\D/g, '');

/** 52998224725 -> 529.982.247-25 (também serve de máscara enquanto digita). */
export function formatarCpf(valor) {
  const d = digitos(valor).slice(0, 11);
  return d
    .replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d{1,2})$/, '.$1-$2');
}

/** 85988880001 -> (85) 98888-0001 ; 8532120000 -> (85) 3212-0000 */
export function formatarTelefone(valor) {
  const d = digitos(valor).slice(0, 11);
  if (d.length <= 2) return d.length ? `(${d}` : '';
  const ddd = d.slice(0, 2);
  const resto = d.slice(2);
  const corte = d.length === 11 ? 5 : 4;
  return resto.length > corte
    ? `(${ddd}) ${resto.slice(0, corte)}-${resto.slice(corte)}`
    : `(${ddd}) ${resto}`;
}

/** "1990-05-15" -> "15/05/1990" (sem converter fuso, que poderia mudar o dia). */
export function formatarData(iso) {
  if (!iso) return '—';
  const [ano, mes, dia] = iso.slice(0, 10).split('-');
  return `${dia}/${mes}/${ano}`;
}

export function formatarDataHora(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

export function hojeIso() {
  const d = new Date();
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

/** Data e hora de agora no formato do campo datetime-local ("2026-11-16T15:30"). */
export function agoraIsoLocal() {
  const d = new Date();
  const p = (n) => String(n).padStart(2, '0');
  return `${hojeIso()}T${p(d.getHours())}:${p(d.getMinutes())}`;
}

/** "há 25 min", "há 2 h": quanto tempo passou desde uma data (usado na fila da triagem). */
export function tempoDesde(iso) {
  if (!iso) return '';
  const minutos = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 60000));
  if (minutos < 1) return 'agora';
  if (minutos < 60) return `há ${minutos} min`;
  const horas = Math.floor(minutos / 60);
  if (horas < 24) return `há ${horas} h${minutos % 60 ? ` ${minutos % 60} min` : ''}`;
  return `há ${Math.floor(horas / 24)} dia(s)`;
}
