package com.healthsys.auth.usuario;

/**
 * Perfis de acesso do sistema. Gravados como texto na coluna usuarios.perfil.
 * - ADMIN: gerencia os usuários do sistema (TI). Não acessa dados clínicos.
 * - RECEPCIONISTA: cadastra e atualiza os dados cadastrais dos pacientes. Não acessa dados clínicos.
 * - MEDICO: consulta o histórico clínico, registra atendimentos e alergias.
 * - ENFERMEIRO: consulta o histórico clínico, registra vacinas e alergias.
 */
public enum Perfil {
    ADMIN,
    RECEPCIONISTA,
    MEDICO,
    ENFERMEIRO
}
