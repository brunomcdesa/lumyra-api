package br.com.lumyra.config;

import br.com.lumyra.core.rls.TenantContextHolder;
import br.com.lumyra.service.ServicoJwt;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class FiltroAutenticacaoJwt extends OncePerRequestFilter {

    private static final int BEARER_PREFIX_LENGTH = 7;

    private final ServicoJwt servicoJwt;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        var authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        var token = authHeader.substring(BEARER_PREFIX_LENGTH);

        try {
            autenticarRequisicao(request, token);
            filterChain.doFilter(request, response);
        } finally {
            // Evita vazar o tenant entre requisições na mesma thread do pool.
            TenantContextHolder.clear();
        }
    }

    private void autenticarRequisicao(HttpServletRequest request, String token) {
        var email = servicoJwt.extrairNomeUsuario(token);

        if (email == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        var userDetails = userDetailsService.loadUserByUsername(email);
        if (!servicoJwt.isTokenValido(token, userDetails)) {
            return;
        }

        var authToken = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // Disponibiliza o tenant para o interceptor de RLS (G1).
        TenantContextHolder.set(servicoJwt.extrairTenantId(token));
    }
}
