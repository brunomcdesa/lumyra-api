package br.com.lumyra.core.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "lumyra:rt:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public String salvar(RefreshTokenData data) {
        String uuid = UUID.randomUUID().toString();
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(KEY_PREFIX + uuid, json,
                Duration.ofMillis(refreshExpirationMs));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Erro ao serializar refresh token", ex);
        }
        return uuid;
    }

    public Optional<RefreshTokenData> buscar(String uuid) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + uuid);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, RefreshTokenData.class));
        } catch (JsonProcessingException ex) {
            return Optional.empty();
        }
    }

    public void revogar(String uuid) {
        redisTemplate.delete(KEY_PREFIX + uuid);
    }
}
