package br.com.lumyra.modules.identity.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Unidade de isolamento multi-tenant. Um profissional/assinatura corresponde a
 * um tenant; todo dado de aluna referencia o tenant para fins de RLS (G1).
 */
@Table(name = "tenant")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EqualsAndHashCode(of = "id")
public class Tenant {

    @Id
    @GeneratedValue(generator = "seq_tenant", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "seq_tenant", sequenceName = "seq_tenant", allocationSize = 1)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}
