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
 * Educador(a) com CREF, dono(a) dos dados das alunas do seu tenant.
 */
@Table(name = "professional")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EqualsAndHashCode(of = "id")
public class Professional {

    @Id
    @GeneratedValue(generator = "seq_professional", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "seq_professional", sequenceName = "seq_professional", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 20)
    private String cref;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
