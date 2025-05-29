package dev.tsvinc.r2p.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class MetricsFilter implements WebFilter {

    private final MeterRegistry meterRegistry;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Instant start = Instant.now();
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    Duration duration = Duration.between(start, Instant.now());
                    int statusCode = exchange.getResponse().getStatusCode() != null ?
                            exchange.getResponse().getStatusCode().value() : 0;

                    // Record metrics
                    Timer.start(meterRegistry)
                            .stop(Timer.builder("r2p.request.duration")
                                    .tag("method", method)
                                    .tag("path", path)
                                    .tag("status", String.valueOf(statusCode))
                                    .register(meterRegistry));

                    Counter.builder("r2p.requests.total")
                            .tag("method", method)
                            .tag("path", path)
                            .tag("status", String.valueOf(statusCode))
                            .register(meterRegistry)
                            .increment();
                });
    }
}