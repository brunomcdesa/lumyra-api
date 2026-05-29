package br.com.lumyra.core.rls;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Injeta o tenant corrente na conexão JDBC antes de qualquer query de
 * repositório, executando {@code SET LOCAL app.current_tenant = ?}.
 *
 * <p>É a metade de aplicação da defesa em camadas do G1: junto com as policies
 * de RLS no Postgres (migração V003), garante que uma requisição só enxergue
 * dados do próprio tenant.
 *
 * <p>{@code SET LOCAL} tem escopo de transação, então o valor é limpo
 * automaticamente no commit/rollback. Se não houver tenant no contexto (rota
 * pública, sem autenticação), nada é executado e as policies, fail-safe, não
 * retornam nenhuma linha.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RlsTenantContextInterceptor {

    private static final String SET_TENANT_SQL = "SET LOCAL app.current_tenant = ?";

    private final JdbcTemplate jdbcTemplate;

    @Before("execution(* br.com.lumyra..repository..*.*(..))")
    public void injectTenantContext() {
        var tenantId = TenantContextHolder.get();
        if (tenantId == null) {
            return;
        }

        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (var statement = connection.prepareStatement(SET_TENANT_SQL)) {
                statement.setString(1, String.valueOf(tenantId));
                statement.execute();
            }
            return null;
        });
    }
}
