package br.com.lumyra.modules.identity.service;

import br.com.lumyra.exception.ExcecaoNegocio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Token de convite no formato {@code "{tenantId}.{segredoAleatório}"}.
 *
 * <p>O tenant vai embutido porque o endpoint de aceite é público e precisa
 * setar o contexto de RLS (G1) antes de qualquer query. Apenas o hash SHA-256
 * do token é persistido (G4); o token bruto só viaja no link enviado à aluna.
 */
public final class TokenConviteUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SEGREDO_BYTES = 32;

    private TokenConviteUtil() {
    }

    public static String gerar(Integer tenantId) {
        byte[] bytes = new byte[SEGREDO_BYTES];
        RANDOM.nextBytes(bytes);
        String segredo = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return tenantId + "." + segredo;
    }

    public static Integer extrairTenant(String token) {
        int separador = token == null ? -1 : token.indexOf('.');
        if (separador <= 0) {
            throw new ExcecaoNegocio("Convite inválido");
        }
        try {
            return Integer.valueOf(token.substring(0, separador));
        } catch (NumberFormatException ex) {
            throw new ExcecaoNegocio("Convite inválido");
        }
    }

    public static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] resumo = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível", ex);
        }
    }
}
