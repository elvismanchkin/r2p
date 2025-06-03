package dev.tsvinc.r2p.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

@Configuration
public class WebClientConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.cloud.vault.enabled", havingValue = "false")
    public WebClient insecureWebClient() {
        return WebClient.builder().build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cloud.vault.enabled", havingValue = "true", matchIfMissing = true)
    public WebClient secureWebClient() throws Exception {
        String keystorePath = System.getProperty("javax.net.ssl.keyStore");

        // If SSL properties not set (e.g., in tests), return simple WebClient
        if (keystorePath == null) {
            return WebClient.builder().build();
        }

        // Otherwise configure with SSL
        SslContext sslContext = SslContextBuilder.forClient()
                .keyManager(getKeyManagerFactory())
                .trustManager(getTrustManagerFactory())
                .protocols("TLSv1.2", "TLSv1.3")
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    @Primary
    public WebClient webClient() {
        // This will inject either secureWebClient or insecureWebClient based on conditions
        return WebClient.builder().build();
    }

    private KeyManagerFactory getKeyManagerFactory() throws Exception {
        String keystorePath = System.getProperty("javax.net.ssl.keyStore");
        String keystorePassword = System.getProperty("javax.net.ssl.keyStorePassword");

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, keystorePassword.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keystorePassword.toCharArray());
        return kmf;
    }

    private TrustManagerFactory getTrustManagerFactory() throws Exception {
        String truststorePath = System.getProperty("javax.net.ssl.trustStore");
        String truststorePassword = System.getProperty("javax.net.ssl.trustStorePassword");

        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(truststorePath)) {
            trustStore.load(fis, truststorePassword.toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        return tmf;
    }
}