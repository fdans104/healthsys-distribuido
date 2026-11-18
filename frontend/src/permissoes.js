// Quem pode fazer o quê na tela, pensado como num hospital.
// A regra de verdade fica no servidor (SecurityConfig do patient-service e do auth-service):
// a tela só esconde o que a pessoa não pode usar.

export const podeGerenciarUsuarios = (perfil) => perfil === 'ADMIN';

// A recepção abre e atualiza a ficha cadastral do paciente.
export const podeCadastrarPaciente = (perfil) => perfil === 'RECEPCIONISTA';

// Só um cadastro feito por engano pode ser excluído (o servidor recusa se já houver prontuário).
export const podeExcluirPaciente = (perfil) => perfil === 'ADMIN';

// Dado clínico é sigiloso: só médicos e enfermeiros veem o histórico.
export const podeVerClinico = (perfil) => perfil === 'MEDICO' || perfil === 'ENFERMEIRO';

export const podeRegistrarAtendimento = (perfil) => perfil === 'MEDICO';
export const podeRegistrarVacina = (perfil) => perfil === 'ENFERMEIRO';
export const podeRegistrarAlergia = (perfil) => perfil === 'MEDICO' || perfil === 'ENFERMEIRO';

// Prontuário (record-service)
export const podeRegistrarConsulta = (perfil) => perfil === 'MEDICO';
export const podeSolicitarExame = (perfil) => perfil === 'MEDICO';
export const podeRegistrarResultado = (perfil) => perfil === 'MEDICO' || perfil === 'ENFERMEIRO';
export const podePrescrever = (perfil) => perfil === 'MEDICO';

// Triagem (triage-service): a recepção abre, a enfermagem classifica, o médico atende e finaliza.
export const podeVerTriagens = (perfil) => ['RECEPCIONISTA', 'ENFERMEIRO', 'MEDICO'].includes(perfil);
export const podeAbrirTriagem = (perfil) => perfil === 'RECEPCIONISTA' || perfil === 'ENFERMEIRO';
export const podeAvaliarTriagem = (perfil) => perfil === 'ENFERMEIRO';
export const podeAtenderTriagem = (perfil) => perfil === 'MEDICO';
export const podeCancelarTriagem = (perfil) => podeVerTriagens(perfil);
// A recepção acompanha a fila, mas não vê sintomas nem sinais vitais.
export const podeVerDadosDaTriagem = (perfil) => perfil === 'ENFERMEIRO' || perfil === 'MEDICO';
