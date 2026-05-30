package br.com.lumyra.modules.identity.service.impl;

import br.com.lumyra.core.auth.RateLimitService;
import br.com.lumyra.core.auth.RefreshTokenData;
import br.com.lumyra.core.auth.RefreshTokenService;
import br.com.lumyra.exception.ExcecaoNegocio;
import br.com.lumyra.modules.identity.dto.AuthResponse;
import br.com.lumyra.modules.identity.dto.LoginRequest;
import br.com.lumyra.modules.identity.dto.RefreshRequest;
import br.com.lumyra.modules.identity.dto.RegisterRequest;
import br.com.lumyra.modules.identity.entity.Professional;
import br.com.lumyra.modules.identity.entity.Student;
import br.com.lumyra.modules.identity.entity.Tenant;
import br.com.lumyra.modules.identity.repository.ProfessionalRepositorio;
import br.com.lumyra.modules.identity.repository.StudentRepositorio;
import br.com.lumyra.modules.identity.repository.TenantRepositorio;
import br.com.lumyra.modules.identity.service.AuthService;
import br.com.lumyra.modules.identity.service.UsuarioDetailsService;
import br.com.lumyra.service.ServicoJwt;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String ROLE_PROFESSIONAL = "PROFESSIONAL";
    private static final String ROLE_STUDENT = "STUDENT";

    private final ProfessionalRepositorio professionalRepositorio;
    private final StudentRepositorio studentRepositorio;
    private final TenantRepositorio tenantRepositorio;
    private final UsuarioDetailsService usuarioDetailsService;
    private final ServicoJwt servicoJwt;
    private final RefreshTokenService refreshTokenService;
    private final RateLimitService rateLimitService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        if (professionalRepositorio.findByEmail(request.email()).isPresent()) {
            throw new ExcecaoNegocio("E-mail já cadastrado");
        }

        var tenant = tenantRepositorio.save(Tenant.builder()
            .nome(request.nome())
            .criadoEm(OffsetDateTime.now())
            .build());

        var professional = professionalRepositorio.save(Professional.builder()
            .tenant(tenant)
            .nome(request.nome())
            .email(request.email())
            .senhaHash(passwordEncoder.encode(request.senha()))
            .cref(request.cref())
            .criadoEm(OffsetDateTime.now())
            .build());

        return buildAuthResponse(professional);
    }

    @Override
    public AuthResponse autenticar(LoginRequest request) {
        var professional = professionalRepositorio.findByEmail(request.email());
        if (professional.isPresent()) {
            if (!passwordEncoder.matches(request.senha(), professional.get().getSenhaHash())) {
                throw new BadCredentialsException("Credenciais inválidas");
            }
            return buildAuthResponse(professional.get());
        }

        var student = studentRepositorio.findByEmail(request.email())
            .filter(s -> s.getSenhaHash() != null && Boolean.TRUE.equals(s.getAtivo()))
            .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.senha(), student.getSenhaHash())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        return buildAuthResponseAluna(student);
    }

    @Override
    public AuthResponse renovar(RefreshRequest request) {
        var data = refreshTokenService.buscar(request.refreshToken())
            .orElseThrow(() -> new BadCredentialsException("Refresh token inválido ou expirado"));

        refreshTokenService.revogar(request.refreshToken());

        if (ROLE_STUDENT.equals(data.role())) {
            var student = studentRepositorio.findById(data.usuarioId())
                .orElseThrow(() -> new BadCredentialsException("Aluna não encontrada"));
            return buildAuthResponseAluna(student);
        }

        var professional = professionalRepositorio.findById(data.usuarioId())
            .orElseThrow(() -> new BadCredentialsException("Profissional não encontrado"));
        return buildAuthResponse(professional);
    }

    @Override
    public void sair(RefreshRequest request) {
        refreshTokenService.revogar(request.refreshToken());
    }

    public AuthResponse autenticarComRateLimit(LoginRequest request, String clientIp) {
        rateLimitService.checkLoginAttempt(clientIp);
        return autenticar(request);
    }

    /** Emite tokens para uma aluna recém-autenticada (ex.: auto-login ao aceitar convite). */
    public AuthResponse emitirTokensAluna(Student aluna) {
        return buildAuthResponseAluna(aluna);
    }

    private AuthResponse buildAuthResponse(Professional professional) {
        var userDetails = usuarioDetailsService.loadUserByUsername(professional.getEmail());

        Map<String, Object> claims = Map.of(
            "tenant_id", professional.getTenant().getId(),
            "role", ROLE_PROFESSIONAL
        );

        String accessToken = servicoJwt.gerarToken(userDetails, claims);

        String refreshToken = refreshTokenService.salvar(new RefreshTokenData(
            professional.getId(),
            professional.getTenant().getId(),
            professional.getEmail(),
            ROLE_PROFESSIONAL
        ));

        return new AuthResponse(accessToken, refreshToken);
    }

    private AuthResponse buildAuthResponseAluna(Student student) {
        var userDetails = usuarioDetailsService.loadUserByUsername(student.getEmail());

        Map<String, Object> claims = Map.of(
            "tenant_id", student.getTenant().getId(),
            "role", ROLE_STUDENT
        );

        String accessToken = servicoJwt.gerarToken(userDetails, claims);

        String refreshToken = refreshTokenService.salvar(new RefreshTokenData(
            student.getId(),
            student.getTenant().getId(),
            student.getEmail(),
            ROLE_STUDENT
        ));

        return new AuthResponse(accessToken, refreshToken);
    }
}
