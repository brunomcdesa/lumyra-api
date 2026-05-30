package br.com.lumyra.modules.identity.service.impl;

import br.com.lumyra.modules.identity.repository.TenantRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Job de purge LGPD: hard-delete das alunas soft-deleted além da retenção.
 * Roda sem requisição (sem tenant no contexto), então itera os tenants e
 * delega a {@link PurgaTenantService}, que seta o contexto de RLS por tenant.
 * Idempotente: só remove o que já passou da janela.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PurgaDadosJob {

    private final TenantRepositorio tenantRepositorio;
    private final PurgaTenantService purgaTenantService;

    @Value("${app.lgpd.retention-days:30}")
    private long retentionDays;

    @Scheduled(cron = "${app.lgpd.purge-cron:0 0 3 * * *}")
    public void executar() {
        OffsetDateTime limite = OffsetDateTime.now().minusDays(retentionDays);
        int total = 0;
        for (var tenant : tenantRepositorio.findAll()) {
            total += purgaTenantService.purgarTenant(tenant.getId(), limite);
        }
        if (total > 0) {
            log.info("Purge LGPD removeu {} aluna(s) além da retenção de {} dias",
                total, retentionDays);
        }
    }
}
