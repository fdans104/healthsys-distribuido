/** Um campo de formulário com rótulo e mensagem de erro (mesmo visual dos outros formulários). */
export default function Campo({ rotulo, erro, children }) {
  return (
    <label className={`campo ${erro ? 'campo-com-erro' : ''}`}>
      <span>{rotulo}</span>
      {children}
      {erro && <small>{erro}</small>}
    </label>
  );
}
