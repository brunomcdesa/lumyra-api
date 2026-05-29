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
import br.com.lumyra.modules.identity.entity.Tenant;
import br.com.lumyra.modules.identity.repository.ProfessionalRepositorio;
import br.com.lumyra.modules.identity.repository.TenantRepositorio;
import br.com.lumyra.modules.identity.service.AuthService;
import br.com.lumyra.modules.identity.service.ProfessionalDetailsService;
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

    private final ProfessionalRepositorio professionalRepositorio;
    private final TenantRepositorio tenantRepositorio;
    private final ProfessionalDetailsService professionalDetailsService;
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
        var professional = professionalRepositorio.findByEmail(request.email())
            .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.senha(), professional.getSenhaHash())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        return buildAuthResponse(professional);
    }

    @Override
    public AuthResponse renovar(RefreshRequest request) {
        var data = refreshTokenService.buscar(request.refreshToken())
            .orElseThrow(() -> new BadCredentialsException("Refresh token inválido ou expirado"));

        refreshTokenService.revogar(request.refreshToken());

        var professional = professionalRepositorio.findById(data.professionalId())
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

    private AuthResponse buildAuthResponse(Professional professional) {
        var userDetails = professionalDetailsService.loadUserByUsername(professional.getEmail());

        Map<String, Object> claims = Map.of(
            "tenant_id", professional.getTenant().getId(),
            "role", "PROFESSIONAL"
        );

        String accessToken = servicoJwt.gerarToken(userDetails, claims);

        String refreshToken = refreshTokenService.salvar(new RefreshTokenData(
            professional.getId(),
            professional.getTenant().getId(),
            professional.getEmail()
        ));

        return new AuthResponse(accessToken, refreshToken);
    }
}
