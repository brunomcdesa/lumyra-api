package br.com.lumyra.core.auth;

import br.com.lumyra.exception.TooManyRequestsException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limit de login por IP usando cache local (Caffeine). Cada IP ganha um
 * contador que expira {@code windowSeconds} após a primeira tentativa (janela
 * fixa) — passou do limite dentro da janela, bloqueia.
 *
 * <p>Store em memória: vale para uma única instância e zera no restart. Trocar
 * por store distribuído quando houver mais de uma instância.
 */
@Service
public class RateLimitService {

    @Value("${app.auth.rate-limit.max-attempts}")
    private int maxAttempts;

    @Value("${app.auth.rate-limit.window-seconds}")
    private long windowSeconds;

    private Cache<String, AtomicInteger> tentativasPorIp;

    @PostConstruct
    void inicializar() {
        tentativasPorIp = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(windowSeconds))
            .build();
    }

    public void checkLoginAttempt(String clientIp) {
        AtomicInteger contador = tentativasPorIp.get(clientIp, chave -> new AtomicInteger(0));
        if (contador.incrementAndGet() > maxAttempts) {
            throw new TooManyRequestsException();
        }
    }
}
