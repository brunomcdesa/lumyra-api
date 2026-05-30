package br.com.lumyra.modules.identity.service.impl;

import br.com.lumyra.core.auth.RateLimitService;
import br.com.lumyra.core.auth.RefreshTokenData;
import br.com.lumyra.core.auth.RefreshTokenService;
import br.com.lumyra.exception.ExcecaoNegocio;
import br.com.lumyra.modules.identity.dto.LoginRequest;
import br.com.lumyra.modules.identity.dto.RefreshRequest;
import br.com.lumyra.modules.identity.dto.RegisterRequest;
import br.com.lumyra.modules.identity.entity.Professional;
import br.com.lumyra.modules.identity.entity.Student;
import br.com.lumyra.modules.identity.entity.Tenant;
import br.com.lumyra.modules.identity.repository.ProfessionalRepositorio;
import br.com.lumyra.modules.identity.repository.StudentRepositorio;
import br.com.lumyra.modules.identity.repository.TenantRepositorio;
import br.com.lumyra.modules.identity.service.UsuarioDetailsService;
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
    private StudentRepositorio studentRepositorio;
    @Mock
    private TenantRepositorio tenantRepositorio;
    @Mock
    private UsuarioDetailsService usuarioDetailsService;
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
    private static final Student STUDENT = Student.builder()
        .id(20)
        .tenant(TENANT)
        .nome("Ana")
        .email("ana@email.com")
        .senhaHash("$argon2aluna")
        .ativo(true)
        .criadoEm(OffsetDateTime.now())
        .build();
    private static final UserDetails USER_DETAILS = User.builder()
        .username("bruno@email.com")
        .password("$argon2hash")
        .roles("PROFESSIONAL")
        .build();
    private static final UserDetails USER_DETAILS_ALUNA = User.builder()
        .username("ana@email.com")
        .password("$argon2aluna")
        .roles("STUDENT")
        .build();

    @Test
    @DisplayName("registrar: cria tenant, professional e retorna tokens")
    void registrar_deveCriarTenantEProfessionalERetornarTokens() {
        when(professionalRepositorio.findByEmail("bruno@email.com")).thenReturn(Optional.empty());
        when(tenantRepositorio.save(any())).thenReturn(TENANT);
        when(passwordEncoder.encode("senha123")).thenReturn("$argon2hash");
        when(professionalRepositorio.save(any())).thenReturn(PROFESSIONAL);
        when(usuarioDetailsService.loadUserByUsername("bruno@email.com")).thenReturn(USER_DETAILS);
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
    @DisplayName("autenticar: profissional com credenciais válidas retorna tokens")
    void autenticar_profissionalValido_deveRetornarTokens() {
        when(professionalRepositorio.findByEmail("bruno@email.com"))
            .thenReturn(Optional.of(PROFESSIONAL));
        when(passwordEncoder.matches("senha123", "$argon2hash")).thenReturn(true);
        when(usuarioDetailsService.loadUserByUsername("bruno@email.com")).thenReturn(USER_DETAILS);
        when(servicoJwt.gerarToken(any(), any(Map.class))).thenReturn("access-token");
        when(refreshTokenService.salvar(any())).thenReturn("refresh-uuid");

        var response = authService.autenticar(new LoginRequest("bruno@email.com", "senha123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-uuid");
    }

    @Test
    @DisplayName("autenticar: aluna (não é profissional) com senha válida retorna tokens")
    void autenticar_alunaValida_deveRetornarTokens() {
        when(professionalRepositorio.findByEmail("ana@email.com")).thenReturn(Optional.empty());
        when(studentRepositorio.findByEmail("ana@email.com")).thenReturn(Optional.of(STUDENT));
        when(passwordEncoder.matches("senhaAluna", "$argon2aluna")).thenReturn(true);
        when(usuarioDetailsService.loadUserByUsername("ana@email.com"))
            .thenReturn(USER_DETAILS_ALUNA);
        when(servicoJwt.gerarToken(any(), any(Map.class))).thenReturn("access-aluna");
        when(refreshTokenService.salvar(any())).thenReturn("refresh-aluna");

        var response = authService.autenticar(new LoginRequest("ana@email.com", "senhaAluna"));

        assertThat(response.accessToken()).isEqualTo("access-aluna");
        assertThat(response.refreshToken()).isEqualTo("refresh-aluna");
    }

    @Test
    @DisplayName("autenticar: e-mail inexistente em ambas as tabelas lança BadCredentials")
    void autenticar_emailInexistente_deveLancarBadCredentials() {
        when(professionalRepositorio.findByEmail(anyString())).thenReturn(Optional.empty());
        when(studentRepositorio.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.autenticar(
            new LoginRequest("x@email.com", "senha123")))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("autenticar: profissional com senha errada lança BadCredentials")
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
        when(usuarioDetailsService.loadUserByUsername("bruno@email.com")).thenReturn(USER_DETAILS);
        when(servicoJwt.gerarToken(any(), any(Map.class))).thenReturn("access-token");
        when(refreshTokenService.salvar(any())).thenReturn("refresh-uuid");

        authService.autenticarComRateLimit(new LoginRequest("bruno@email.com", "senha123"),
            "127.0.0.1");

        verify(rateLimitService).checkLoginAttempt("127.0.0.1");
    }

    @Test
    @DisplayName("renovar: refresh de profissional retorna novos tokens")
    void renovar_refreshProfissional_deveRetornarNovosTokens() {
        var data = new RefreshTokenData(10, 1, "bruno@email.com", "PROFESSIONAL");
        when(refreshTokenService.buscar("refresh-uuid")).thenReturn(Optional.of(data));
        when(professionalRepositorio.findById(10)).thenReturn(Optional.of(PROFESSIONAL));
        when(usuarioDetailsService.loadUserByUsername("bruno@email.com")).thenReturn(USER_DETAILS);
        when(servicoJwt.gerarToken(any(), any(Map.class))).thenReturn("new-access");
        when(refreshTokenService.salvar(any())).thenReturn("new-refresh");

        var response = authService.renovar(new RefreshRequest("refresh-uuid"));

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenService).revogar("refresh-uuid");
    }

    @Test
    @DisplayName("renovar: refresh de aluna recarrega pela StudentRepositorio")
    void renovar_refreshAluna_deveRetornarNovosTokens() {
        var data = new RefreshTokenData(20, 1, "ana@email.com", "STUDENT");
        when(refreshTokenService.buscar("refresh-aluna")).thenReturn(Optional.of(data));
        when(studentRepositorio.findById(20)).thenReturn(Optional.of(STUDENT));
        when(usuarioDetailsService.loadUserByUsername("ana@email.com"))
            .thenReturn(USER_DETAILS_ALUNA);
        when(servicoJwt.gerarToken(any(), any(Map.class))).thenReturn("new-access-aluna");
        when(refreshTokenService.salvar(any())).thenReturn("new-refresh-aluna");

        var response = authService.renovar(new RefreshRequest("refresh-aluna"));

        assertThat(response.accessToken()).isEqualTo("new-access-aluna");
        verify(studentRepositorio).findById(20);
        verify(refreshTokenService).revogar("refresh-aluna");
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
