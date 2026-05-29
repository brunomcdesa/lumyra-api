package br.com.lumyra.core.rls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class RlsTenantContextInterceptorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private RlsTenantContextInterceptor interceptor;

    @AfterEach
    void limparContexto() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Deve executar SET LOCAL app.current_tenant com o tenant do contexto")
    void deveInjetarTenantQuandoPresente() throws Exception {
        TenantContextHolder.set(42);

        interceptor.injectTenantContext();

        // Captura o callback entregue ao JdbcTemplate e o executa contra uma
        // conexão mockada para inspecionar o SQL e o parâmetro usados.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<ConnectionCallback<Void>> captor =
            ArgumentCaptor.forClass(ConnectionCallback.class);
        verify(jdbcTemplate).execute(captor.capture());

        var connection = org.mockito.Mockito.mock(Connection.class);
        var statement = org.mockito.Mockito.mock(PreparedStatement.class);
        when(connection.prepareStatement("SET LOCAL app.current_tenant = ?")).thenReturn(statement);

        captor.getValue().doInConnection(connection);

        verify(connection).prepareStatement("SET LOCAL app.current_tenant = ?");
        verify(statement).setString(1, "42");
        verify(statement).execute();
        verify(statement, times(1)).close();
    }

    @Test
    @DisplayName("Não deve tocar o banco quando não há tenant no contexto")
    void naoDeveExecutarQuandoTenantAusente() {
        assertThat(TenantContextHolder.get()).isNull();

        interceptor.injectTenantContext();

        verifyNoInteractions(jdbcTemplate);
    }
}
