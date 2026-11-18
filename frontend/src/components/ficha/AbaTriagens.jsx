import { useEffect, useState } from 'react';
import { triagemApi } from '../../api.js';
import { podeAbrirTriagem } from '../../permissoes.js';
import NovaTriagemForm from '../triagem/NovaTriagemForm.jsx';
import TriagemCartao from '../triagem/TriagemCartao.jsx';
import { ListaRegistros } from './Registros.jsx';

/** Triagens deste paciente. Recepção e enfermagem podem abrir uma nova direto daqui. */
export default function AbaTriagens({ paciente, perfil, mostrarAviso, setErro }) {
  const [itens, setItens] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [abrindo, setAbrindo] = useState(false);

  async function carregar() {
    setCarregando(true);
    try {
      setItens(await triagemApi.listar({ pacienteId: paciente.id }));
    } catch (e) {
      setErro(e.message);
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    carregar();
  }, [paciente.id]);

  const emAndamento = itens.some((t) => t.status === 'PENDENTE' || t.status === 'EM_ATENDIMENTO');

  return (
    <>
      <h3 className="titulo-aba">Triagens</h3>

      {podeAbrirTriagem(perfil) && !emAndamento && (
        abrindo ? (
          <div className="form-ficha">
            <NovaTriagemForm
              pacienteFixo={paciente}
              onAberta={async () => { setAbrindo(false); mostrarAviso(`${paciente.nome} entrou na fila da triagem.`); await carregar(); }}
              onCancelar={() => setAbrindo(false)}
            />
          </div>
        ) : (
          <div className="acoes form-ficha">
            <button className="botao" onClick={() => setAbrindo(true)}>+ Abrir triagem</button>
          </div>
        )
      )}
      {emAndamento && <p className="nota">Este paciente já está na fila. Acompanhe pela seção Triagem.</p>}

      <ListaRegistros itens={itens} carregando={carregando} vazio="Nenhuma triagem para este paciente.">
        {itens.map((t) => (
          <TriagemCartao key={t.id} triagem={t} perfil={perfil} mostrarPaciente={false} somenteLeitura />
        ))}
      </ListaRegistros>
    </>
  );
}
