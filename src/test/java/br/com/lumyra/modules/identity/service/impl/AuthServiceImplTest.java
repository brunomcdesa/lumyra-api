package br.com.lumyra.modules.identity.service.impl;

import br.com.lumyra.core.auth.RateLimitService;
import br.com.lumyra.core.auth.RefreshTokenData;
import br.com.lumyra.core.auth.RefreshTokenService;
import br.com.lumyra.exception.ExcecaoNegocio;
import br.com.lumyra.modules.identity.dto.LoginRequest;
import br.com.lumyra.modules.identity.dto.RefreshRequest;
import br.com.lumyra.modules.identity.dto.RegisterRequest;
import br.com.lumyra.modules.identity.entity.Professional;
import br.com.lumyra.modules.identity.entity.Tenant;
import br.com.lumyra.modules.identity.repository.ProfessionalRepositorio;
import br.com.lumyra.modules.identity.repository.TenantRepositorio;
import br.com.lumyra.modules.identity.service.ProfessionalDetailsService;
import br.com.lumyra.service.ServicoJwt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private ProfessionalRepositorio professionalRepositorio;
    @Mock
    private TenantRepositorio tenantRepositorio;
    @Mock
    private ProfessionalDetailsService professionalDetailsService;
    @Mock
    private ServicoJwt servicoJwt;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final Tenant TENANT = Tenant.builder().id(1).nome("Bruno").build();
    private static final Professional PROFESSIONAL = Professional.builder()
        .id(10)
        .tenant(TENANT)
        .nome("Bruno")
        .email("bruno@email.com")
        .senhaHash("$argon2hash")
        .cref("CREF-123")
        .criadoEm(OffsetDateTime.now())
        .build();
    private static final UserDetails USER_DETAILS = User.builder()
        .username("bruno@email.com")
        .password("$argon2hash")
        .roles("PROFESSIONAL")
        .build();

    @Test
    @DisplayName("registrar: cria tenant, professional e retorna tokens")
    void registrar_deveCriarTenantEProfessionalERetornarTokens() {
        when(professionalRepositorio.findByEmail("bruno@email.com")).thenReturn(Optional.empty());
        when(tenantRepositorio.save(any())).thenReturn(TENANT);
        when(passwordEncoder.encode("senha123")).thenReturn("$argon2hash");
        when(professionalRepositorio.save(any())).thenReturn(PROFESSIONAL);
        when(professionalDetailsService.loadUserByUsername("bruno@email.com"))
            .thenReturn(USER_DETAILS);
        when(servicoJwt.gerarToken(any(), any(Map.class))).thenReturn("access-token");
        when(refreshTokenService.salvar(any())).thenReturn("refresh-uuid");

        var request = new RegisterRequest("Bruno", "bruno@email.com", "senha123", "CREF-123");
        var response = authService.registrar(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-uuid");
        verify(tenantRepositorio).save(any());
        verify(professionalRepositorio).save(any());
    }

    @Test
    @DisplayName("registrar: email duplicado lança ExcecaoNegocio")
    void registrar_emailDuplicado_deveLancarExcecaoNegocio() {
        when(professionalRepositorio.findByEmail("bruno@email.com"))
            .thenReturn(Optional.of(PROFESSIONAL));

        var request = new RegisterRequest("Bruno", "bruno@email.com", "senha123", "CREF-123");

        assertThatThrownBy(() -> authService.registrar(request))
            .isInstanceOf(ExcecaoNegocio.class)
            .hasMessageContaining("E-mail já cadastrado");

        verify(tenantRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("autenticar: credenciais válidas retorna tokens")
    void autenticar_credenciaisValidas_deveRetornarTokens() {
        when(professionalRepositorio.findByEmail("bruno@email.com"))
            .thenReturn(Optional.of(PROFESSIONAL));
        when(passwordEncoder.matches("senha123", "$argon2hash")).thenReturn(true);
        when(professionalDetailsService.loadUserByUsername("bruno@email.com"))
            .thenReturn(USER_DETAILS);
        when(servicoJwt.gerarToken(any(), any(Map.class))).thenReturn("access-token");
        when(refreshTokenService.salvar(any())).thenReturn("refresh-uuid");

        var response = authService.autenticar(new LoginRequest("bruno@email.com", "senha123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-uuid");
    }

    @Test
    @DisplayName("autenticar: email inexistente lança BadCredentialsException")
    void autenticar_emailInexistente_deveLancarBadCredentials() {
        when(professionalRepositorio.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.autenticar(
            new LoginRequest("x@email.com", "senha123")))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("autenticar: senha errada lança BadCredentialsException")
    void autenticar_senhaErrada_deveLancarBadCredentials() {
        when(professionalRepositorio.findByEmail("bruno@email.com"))
            .thenReturn(Optional.of(PROFESSIONAL));
        when(passwordEncoder.matches("errada", "$argon2hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.autenticar(
            new LoginRequest("bruno@email.com", "errada")))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("autenticarComRateLimit: chama rateLimitService antes da auth")
    void autenticarComRateLimit_deveChamarRateLimitAntesDaAuth() {
        doNothing().when(rateLimitService).checkLoginAttempt("127.0.0.1");
        when(professionalRepositorio.findByEmail("bruno@email.com"))
            .thenReturn(Optional.of(PROFESSIONAL));
        when(passwordEncoder.matches("senha123", "$argon2hash")).thenReturn(true);
        when(professionalDetailsService.loadUserByUsername("bruno@email.com"))
            .thenReturn(USER_DETAILS);
        when(servicoJwt.gerarToken(any(), any(Map.class))).thenReturn("access-token");
        when(refreshTokenService.salvar(any())).thenReturn("refresh-uuid");

        authService.autenticarComRateLimit(new LoginRequest("bruno@email.com", "senha123"),
            "127.0.0.1");

        verify(rateLimitService).checkLoginAttempt("127.0.0.1");
    }

    @Test
    @DisplayName("renovar: refresh válido retorna novos tokens")
    void renovar_refreshValido_deveRetornarNovosTokens() {
        var data = new RefreshTokenData(10, 1, "bruno@email.com");
        when(refreshTokenService.buscar("refresh-uuid")).thenReturn(Optional.of(data));
        when(professionalRepositorio.findById(10)).thenReturn(Optional.of(PROFESSIONAL));
        when(professionalDetailsService.loadUserByUsername("bruno@email.com"))
            .thenReturn(USER_DETAILS);
        when(servicoJwt.gerarToken(any(), any(Map.class))).thenReturn("new-access");
        when(refreshTokenService.salvar(any())).thenReturn("new-refresh");

        var response = authService.renovar(new RefreshRequest("refresh-uuid"));

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenService).revogar("refresh-uuid");
    }

    @Test
    @DisplayName("renovar: refresh inválido lança BadCredentialsException")
    void renovar_refreshInvalido_deveLancarBadCredentials() {
        when(refreshTokenService.buscar("invalido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.renovar(new RefreshRequest("invalido")))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("sair: revoga o refresh token")
    void sair_deveRevogarRefreshToken() {
        authService.sair(new RefreshRequest("refresh-uuid"));
        verify(refreshTokenService).revogar("refresh-uuid");
    }
}
