package br.com.lumyra.modules.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AceitarConviteRequest(
    @NotBlank(message = "Token é obrigatório")
    String token,

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    String senha
) {
}
