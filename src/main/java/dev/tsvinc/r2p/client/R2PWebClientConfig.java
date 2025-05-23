package dev.tsvinc.r2p.client;

import dev.tsvinc.r2p.R2PBusinessException;
import dev.tsvinc.r2p.R2PTransactionProcessingException;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class R2PWebClientConfig {

    @Value("${r2p.visa.base-url:https://sandbox.api.visa.com}")
    private String visaBaseUrl;

    @Value("${r2p.webclient.timeout.connection:5000}")
    private int connectionTimeout;

    @Value("${r2p.webclient.timeout.read:30000}")
    private int readTimeout;

    @Value("${r2p.webclient.timeout.write:30000}")
    private int writeTimeout;

    @Bean("r2pWebClient")
    public WebClient r2pWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                .responseTimeout(Duration.ofMillis(readTimeout))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl(visaBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .filter(errorHandler())
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("R2P Request: {} {}", clientRequest.method(), clientRequest.url());
            log.trace("R2P Request Headers: {}", clientRequest.headers());
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.debug("R2P Response Status: {}", clientResponse.statusCode());
            log.trace("R2P Response Headers: {}", clientResponse.headers().asHttpHeaders());
            return Mono.just(clientResponse);
        });
    }

    private ExchangeFilterFunction errorHandler() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().is4xxClientError()) {
                return clientResponse.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            log.error("R2P Client Error {}: {}", clientResponse.statusCode(), errorBody);
                            return Mono.error(new R2PBusinessException("API call failed: " + errorBody));
                        });
            } else if (clientResponse.statusCode().is5xxServerError()) {
                return clientResponse.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            log.error("R2P Server Error {}: {}", clientResponse.statusCode(), errorBody);
                            return Mono.error(new R2PTransactionProcessingException("Server error: " + errorBody, null));
                        });
            }
            return Mono.just(clientResponse);
        });
    }
}