import { ROTULO_PERFIL, formatarDataHora } from '../../formatos.js';

/** Lista de registros da ficha, com as mensagens de "carregando" e "nenhum registro". */
export function ListaRegistros({ itens, carregando, vazio, children }) {
  if (carregando && itens.length === 0) return <p className="vazio">Carregando…</p>;
  if (itens.length === 0) return <p className="vazio">{vazio}</p>;
  return <ul className="registros">{children}</ul>;
}

/** "Registrado por Dra. Ana (Médico) em 10/11/2026 09:30": a assinatura do registro no prontuário. */
export function Assinatura({ nome, perfil, em }) {
  if (!nome) return null;
  const quem = perfil ? `${nome} (${ROTULO_PERFIL[perfil] ?? perfil})` : nome;
  return (
    <p className="linha-secundaria">
      Registrado por {quem}{em ? ` em ${formatarDataHora(em)}` : ''}
    </p>
  );
}

/** Aviso para quem não pode registrar naquela aba. */
export function SoLeitura({ children }) {
  return <p className="nota">{children}</p>;
}
