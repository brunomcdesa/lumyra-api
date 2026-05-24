package br.com.lumyra.dto.requisicao;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequisicaoCriarCliente(
    @NotBlank String nome,
    @NotBlank @Email String email
) {}
