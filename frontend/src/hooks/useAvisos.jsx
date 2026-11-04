import { useEffect, useRef, useState } from 'react';

/** Faixas de aviso (somem sozinhas após 3s) e de erro, compartilhadas pelas páginas. */
export default function useAvisos() {
  const [aviso, setAviso] = useState('');
  const [erro, setErro] = useState('');
  const timer = useRef(null);

  useEffect(() => () => clearTimeout(timer.current), []);

  function mostrarAviso(texto) {
    clearTimeout(timer.current);
    setAviso(texto);
    timer.current = setTimeout(() => setAviso(''), 3000);
  }

  return { aviso, erro, setErro, mostrarAviso };
}

export function Faixas({ aviso, erro }) {
  return (
    <>
      {aviso && <div className="faixa faixa-ok" role="status">{aviso}</div>}
      {erro && <div className="faixa faixa-erro" role="alert">{erro}</div>}
    </>
  );
}
