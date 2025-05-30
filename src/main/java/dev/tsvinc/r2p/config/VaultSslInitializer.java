package dev.tsvinc.r2p.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", havingValue = "true", matchIfMissing = true)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class VaultSslInitializer implements ApplicationRunner {

    private final VaultTemplate vaultTemplate;

    public VaultSslInitializer(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initializeMutualTls();
    }

    private void initializeMutualTls() throws Exception {
        // Retrieve certificates from Vault
        VaultResponse response = vaultTemplate.read("secret/ssl-certs");
        if (response.getData() == null) {
            throw new IllegalStateException("SSL certificates not found in Vault");
        }

        Map<String, Object> data = response.getData();
        String keystore = (String) data.get("keystore");
        String keystorePassword = (String) data.get("keystore-password");
        String truststore = (String) data.get("truststore");
        String truststorePassword = (String) data.get("truststore-password");

        // Create temporary files for certificates
        Path keystorePath = createTempKeystoreFile(keystore);
        Path truststorePath = createTempTruststoreFile(truststore);

        // Set system properties for mutual TLS
        setSystemProperties(keystorePath, keystorePassword, truststorePath, truststorePassword);

        // Configure server SSL for incoming connections
        configureServerSsl(keystorePath, keystorePassword, truststorePath, truststorePassword);
    }

    private Path createTempKeystoreFile(String keystoreData) throws Exception {
        byte[] decodedKeystore = Base64.getDecoder().decode(keystoreData);
        Path keystorePath = Files.createTempFile("app-keystore", ".p12");
        Files.write(keystorePath, decodedKeystore);
        keystorePath.toFile().deleteOnExit();
        return keystorePath;
    }

    private Path createTempTruststoreFile(String truststoreData) throws Exception {
        byte[] decodedTruststore = Base64.getDecoder().decode(truststoreData);
        Path truststorePath = Files.createTempFile("app-truststore", ".jks");
        Files.write(truststorePath, decodedTruststore);
        truststorePath.toFile().deleteOnExit();
        return truststorePath;
    }

    private void setSystemProperties(Path keystorePath, String keystorePassword,
                                     Path truststorePath, String truststorePassword) {
        // For outgoing HTTPS connections
        System.setProperty("javax.net.ssl.keyStore", keystorePath.toString());
        System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);
        System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");

        System.setProperty("javax.net.ssl.trustStore", truststorePath.toString());
        System.setProperty("javax.net.ssl.trustStorePassword", truststorePassword);
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");

        // Enable client authentication
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");
    }

    private void configureServerSsl(Path keystorePath, String keystorePassword,
                                    Path truststorePath, String truststorePassword) {
        // Set server SSL properties for incoming connections
        System.setProperty("server.ssl.enabled", "true");
        System.setProperty("server.ssl.key-store", keystorePath.toString());
        System.setProperty("server.ssl.key-store-password", keystorePassword);
        System.setProperty("server.ssl.key-store-type", "PKCS12");

        System.setProperty("server.ssl.trust-store", truststorePath.toString());
        System.setProperty("server.ssl.trust-store-password", truststorePassword);
        System.setProperty("server.ssl.trust-store-type", "JKS");

        // Require client authentication
        System.setProperty("server.ssl.client-auth", "need");
        System.setProperty("server.ssl.protocol", "TLS");
        System.setProperty("server.ssl.enabled-protocols", "TLSv1.2,TLSv1.3");
    }
}
