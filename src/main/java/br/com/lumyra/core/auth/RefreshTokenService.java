package br.com.lumyra.core.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Guarda refresh tokens em cache local (Caffeine) com expiração automática.
 *
 * <p>Store em memória: vale para uma única instância (WEB_CONCURRENCY=1) e é
 * perdido a cada restart/redeploy — nesse caso as sessões em aberto precisam
 * refazer login. Suficiente para o estágio atual; trocar por store distribuído
 * quando houver mais de uma instância.
 */
@Service
public class RefreshTokenService {

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private Cache<String, RefreshTokenData> tokens;

    @PostConstruct
    void inicializar() {
        tokens = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMillis(refreshExpirationMs))
            .build();
    }

    public String salvar(RefreshTokenData data) {
        String uuid = UUID.randomUUID().toString();
        tokens.put(uuid, data);
        return uuid;
    }

    public Optional<RefreshTokenData> buscar(String uuid) {
        return Optional.ofNullable(tokens.getIfPresent(uuid));
    }

    public void revogar(String uuid) {
        tokens.invalidate(uuid);
    }
}
