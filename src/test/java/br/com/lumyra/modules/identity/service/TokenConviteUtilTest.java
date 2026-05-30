package br.com.lumyra.modules.identity.service;

import br.com.lumyra.exception.ExcecaoNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenConviteUtilTest {

    @Test
    @DisplayName("gerar: embute o tenant no prefixo e gera segredo aleatório")
    void gerar_deveEmbutirTenant() {
        String token = TokenConviteUtil.gerar(42);

        assertThat(token).startsWith("42.");
        assertThat(TokenConviteUtil.extrairTenant(token)).isEqualTo(42);
        assertThat(TokenConviteUtil.gerar(42)).isNotEqualTo(token);
    }

    @Test
    @DisplayName("hash: determinístico e diferente do token bruto")
    void hash_deveSerDeterministico() {
        String token = "7.abc123";

        assertThat(TokenConviteUtil.hash(token))
            .isEqualTo(TokenConviteUtil.hash(token))
            .isNotEqualTo(token)
            .hasSize(64);
    }

    @Test
    @DisplayName("extrairTenant: token sem prefixo válido lança ExcecaoNegocio")
    void extrairTenant_invalido_deveLancar() {
        assertThatThrownBy(() -> TokenConviteUtil.extrairTenant("sem-ponto"))
            .isInstanceOf(ExcecaoNegocio.class);
        assertThatThrownBy(() -> TokenConviteUtil.extrairTenant("abc.xyz"))
            .isInstanceOf(ExcecaoNegocio.class);
    }
}
