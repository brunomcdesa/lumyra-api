package br.com.lumyra.core.auth;

public record RefreshTokenData(Integer professionalId, Integer tenantId, String email) {
}
