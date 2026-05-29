package br.com.lumyra.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class ServicoJwt {

    @Value("${app.jwt.secret}")
    private String segredoJwt;

    @Value("${app.jwt.expiration-ms}")
    private long expiracaoMs;

    public String gerarToken(UserDetails userDetails, Map<String, Object> claimsExtras) {
        return Jwts.builder()
            .claims(claimsExtras)
            .subject(userDetails.getUsername())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiracaoMs))
            .signWith(obterChaveAssinatura())
            .compact();
    }

    public boolean isTokenValido(String token, UserDetails userDetails) {
        String nomeUsuario = extrairNomeUsuario(token);
        return nomeUsuario.equals(userDetails.getUsername()) && !isTokenExpirado(token);
    }

    public String extrairNomeUsuario(String token) {
        return extrairClaim(token, Claims::getSubject);
    }

    public Integer extrairTenantId(String token) {
        return extrairClaim(token, claims -> claims.get("tenant_id", Integer.class));
    }

    private boolean isTokenExpirado(String token) {
        return extrairClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extrairClaim(String token, Function<Claims, T> resolverClaim) {
        return resolverClaim.apply(extrairTodosClaims(token));
    }

    private Claims extrairTodosClaims(String token) {
        return Jwts.parser()
            .verifyWith(obterChaveAssinatura())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey obterChaveAssinatura() {
        byte[] bytesChave = Decoders.BASE64.decode(segredoJwt);
        return Keys.hmacShaKeyFor(bytesChave);
    }
}
