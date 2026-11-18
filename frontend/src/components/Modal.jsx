import { useEffect } from 'react';

/** Janela por cima da tela (fecha com Esc ou clicando fora). */
export default function Modal({ titulo, aoFechar, children, larga = false }) {
  useEffect(() => {
    function tecla(e) {
      if (e.key === 'Escape') aoFechar();
    }
    window.addEventListener('keydown', tecla);
    return () => window.removeEventListener('keydown', tecla);
  }, [aoFechar]);

  return (
    <div className="modal-fundo" onMouseDown={(e) => { if (e.target === e.currentTarget) aoFechar(); }}>
      <div className={`modal ${larga ? 'modal-larga' : ''}`} role="dialog" aria-modal="true" aria-label={titulo}>
        <div className="modal-topo">
          <h2>{titulo}</h2>
          <button className="botao botao-leve" onClick={aoFechar} aria-label="Fechar">✕</button>
        </div>
        {children}
      </div>
    </div>
  );
}
