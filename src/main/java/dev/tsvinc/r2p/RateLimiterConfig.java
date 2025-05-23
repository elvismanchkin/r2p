package dev.tsvinc.r2p;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RateLimiterConfig {

    @Bean
    public ReactiveRedisTemplate<String, Long> redisRateLimiterTemplate(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<Long> valueSerializer = new Jackson2JsonRedisSerializer<>(Long.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, Long> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, Long> context = builder
                .value(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public RedisRateLimiter redisRateLimiter(ReactiveRedisTemplate<String, Long> redisTemplate) {
        return new RedisRateLimiter(redisTemplate);
    }
}