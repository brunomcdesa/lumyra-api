package br.com.lumyra.modules.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsentRequest(
    @NotBlank(message = "Versão do termo é obrigatória")
    @Size(max = 20)
    String termsVersion
) {
}
