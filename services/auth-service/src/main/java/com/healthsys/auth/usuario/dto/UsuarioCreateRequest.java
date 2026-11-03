package com.healthsys.auth.usuario.dto;

import com.healthsys.auth.usuario.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo do POST /api/v1/usuarios. */
public record UsuarioCreateRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 150, message = "nome deve ter no máximo 150 caracteres")
        String nome,

        @NotBlank(message = "email é obrigatório")
        @Email(message = "email inválido")
        @Size(max = 150, message = "email deve ter no máximo 150 caracteres")
        String email,

        @NotBlank(message = "senha é obrigatória")
        @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
        String senha,

        @NotNull(message = "perfil é obrigatório (ADMIN, MEDICO ou ENFERMEIRO)")
        Perfil perfil
) {
}
