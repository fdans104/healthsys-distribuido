// Formatação de valores para exibição e máscaras de digitação.

export const ROTULO_PERFIL = { ADMIN: 'Administrador', MEDICO: 'Médico', ENFERMEIRO: 'Enfermeiro' };
export const ROTULO_SEXO = { M: 'Masculino', F: 'Feminino', O: 'Outro' };

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
