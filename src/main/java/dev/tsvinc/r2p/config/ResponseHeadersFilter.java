package dev.tsvinc.r2p.config;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Component
public class ResponseHeadersFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange)
                .doOnSuccess(unused -> addStandardHeaders(exchange));
    }

    private void addStandardHeaders(ServerWebExchange exchange) {
        var headers = exchange.getResponse().getHeaders();

        // Standard R2P headers
        if (!headers.containsKey("x-correlation-id")) {
            headers.add("x-correlation-id", UUID.randomUUID().toString());
        }

        headers.add("x-response-time", Instant.now().toString());
        headers.add("x-api-version", "v1");
        headers.add("x-service", "r2p-service");

        // Security headers
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("X-Frame-Options", "DENY");
        headers.add("X-XSS-Protection", "1; mode=block");

        // Cache control
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
    }
}