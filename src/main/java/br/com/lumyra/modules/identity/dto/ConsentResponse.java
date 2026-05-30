package br.com.lumyra.modules.identity.dto;

import java.time.OffsetDateTime;

/**
 * Status de consentimento da aluna. {@code status} = {@code ATIVO} ou
 * {@code AUSENTE}; quando ausente, {@code termsVersion} e {@code registradoEm}
 * são nulos.
 */
public record ConsentResponse(String status, String termsVersion, OffsetDateTime registradoEm) {

    public static final String ATIVO = "ATIVO";
    public static final String AUSENTE = "AUSENTE";
}
