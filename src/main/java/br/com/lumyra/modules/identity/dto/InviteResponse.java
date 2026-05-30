package br.com.lumyra.modules.identity.dto;

import java.time.OffsetDateTime;

/**
 * Resposta do convite. O {@code link} carrega o token bruto (só aqui ele
 * trafega); enquanto não há SMTP, o link é devolvido ao profissional.
 */
public record InviteResponse(String link, OffsetDateTime expiraEm) {
}
