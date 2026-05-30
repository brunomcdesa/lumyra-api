package br.com.lumyra.modules.identity.controller;

import br.com.lumyra.modules.identity.dto.AceitarConviteRequest;
import br.com.lumyra.modules.identity.dto.AuthResponse;
import br.com.lumyra.modules.identity.dto.LoginRequest;
import br.com.lumyra.modules.identity.dto.RefreshRequest;
import br.com.lumyra.modules.identity.dto.RegisterRequest;
import br.com.lumyra.modules.identity.service.InviteService;
import br.com.lumyra.modules.identity.service.impl.AuthServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServiceImpl authService;
    private final InviteService inviteService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @PostMapping("/accept-invite")
    public ResponseEntity<AuthResponse> acceptInvite(
        @Valid @RequestBody AceitarConviteRequest request
    ) {
        return ResponseEntity.ok(inviteService.aceitar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest
    ) {
        String clientIp = resolveClientIp(httpRequest);
        return ResponseEntity.ok(authService.autenticarComRateLimit(request, clientIp));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.renovar(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.sair(request);
        return ResponseEntity.noContent().build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
