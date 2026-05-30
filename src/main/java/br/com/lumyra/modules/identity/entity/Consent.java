package br.com.lumyra.modules.identity.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Consentimento LGPD da aluna (base legal para tratar dado sensível — G2).
 * Ativo enquanto {@code revogadoEm} for nulo. Tabela com dado de aluna:
 * protegida por RLS por tenant (G1).
 */
@Table(name = "consent")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EqualsAndHashCode(of = "id")
public class Consent {

    @Id
    @GeneratedValue(generator = "seq_consent", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "seq_consent", sequenceName = "seq_consent", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "terms_version", nullable = false, length = 20)
    private String termsVersion;

    @Column(name = "registrado_em", nullable = false)
    private OffsetDateTime registradoEm;

    @Column(name = "revogado_em")
    private OffsetDateTime revogadoEm;
}
