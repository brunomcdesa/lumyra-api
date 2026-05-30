package br.com.lumyra.modules.identity.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Exportação dos dados pessoais da aluna (direito de portabilidade — LGPD).
 */
public record ExportacaoDadosResponse(
    Integer id,
    String nome,
    String email,
    LocalDate dataNascimento,
    OffsetDateTime criadoEm,
    Boolean ativo,
    List<ConsentimentoItem> consentimentos,
    List<String> profissionaisVinculados
) {

    public record ConsentimentoItem(
        String termsVersion,
        OffsetDateTime registradoEm,
        OffsetDateTime revogadoEm
    ) {
    }
}
