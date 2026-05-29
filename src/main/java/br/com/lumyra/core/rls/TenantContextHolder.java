package br.com.lumyra.core.rls;

/**
 * Guarda o tenant da requisição corrente em um {@link ThreadLocal}.
 *
 * <p>O filtro de autenticação JWT extrai o {@code tenant_id} do token e o
 * armazena aqui. O {@link RlsTenantContextInterceptor} lê esse valor para
 * executar {@code SET LOCAL app.current_tenant = ?} na conexão JDBC antes das
 * queries, ativando as policies de RLS (G1).
 *
 * <p>É obrigatório chamar {@link #clear()} ao fim de cada requisição para não
 * vazar o tenant entre threads reutilizadas do pool do servidor.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<Integer> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void set(Integer tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static Integer get() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
