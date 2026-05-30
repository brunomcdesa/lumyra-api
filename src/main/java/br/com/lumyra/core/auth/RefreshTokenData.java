package br.com.lumyra.core.auth;

/**
 * Dados associados a um refresh token. {@code role} é
 * {@code PROFESSIONAL} ou {@code STUDENT} e direciona qual repositório recarrega
 * o usuário na renovação.
 */
public record RefreshTokenData(Integer usuarioId, Integer tenantId, String email, String role) {
}
