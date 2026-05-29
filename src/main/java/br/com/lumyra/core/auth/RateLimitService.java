package br.com.lumyra.core.auth;

import br.com.lumyra.exception.TooManyRequestsException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final String KEY_PREFIX = "lumyra:rl:login:";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.auth.rate-limit.max-attempts}")
    private int maxAttempts;

    @Value("${app.auth.rate-limit.window-seconds}")
    private long windowSeconds;

    public void checkLoginAttempt(String clientIp) {
        String key = KEY_PREFIX + clientIp;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }
        if (count != null && count > maxAttempts) {
            throw new TooManyRequestsException();
        }
    }
}
