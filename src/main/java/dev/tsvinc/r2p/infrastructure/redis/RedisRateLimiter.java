package dev.tsvinc.r2p.infrastructure.redis;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RedisRateLimiter {
    private final ReactiveRedisTemplate<String, Long> redisTemplate;
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 60;

    public RedisRateLimiter(ReactiveRedisTemplate<String, Long> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> isAllowed(String key) {
        String redisKey = "rate_limiter:" + key;
        return redisTemplate.opsForValue().increment(redisKey)
                .flatMap(count -> {
                    if (count == 1) {
                        return redisTemplate.expire(redisKey, Duration.ofMinutes(1))
                                .thenReturn(true);
                    }
                    return Mono.just(count <= DEFAULT_REQUESTS_PER_MINUTE);
                });
    }
}
