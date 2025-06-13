package dev.tsvinc.r2p.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.AppRoleAuthentication;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.repository.configuration.EnableVaultRepositories;
import org.springframework.vault.support.SslConfiguration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.context.annotation.Primary;
import org.springframework.vault.client.RestTemplateFactory;
import org.springframework.vault.config.AbstractVaultConfiguration.ClientFactoryWrapper;

import java.net.URI;
import java.util.Objects;
import java.io.File;

@Configuration
@ConditionalOnProperty(name = "spring.cloud.vault.enabled", havingValue = "true", matchIfMissing = true)
@EnableVaultRepositories
public class VaultConfiguration extends AbstractVaultConfiguration {

    private final org.springframework.core.env.Environment environment;

    public VaultConfiguration(org.springframework.core.env.Environment environment) {
        this.environment = environment;
    }

    @Override
    public VaultEndpoint vaultEndpoint() {
        String uri = environment.getProperty("spring.cloud.vault.uri");
        return VaultEndpoint.from(URI.create(uri));
    }

    @Override
    public ClientAuthentication clientAuthentication() {
        AppRoleAuthenticationOptions options = AppRoleAuthenticationOptions.builder()
                .roleId(AppRoleAuthenticationOptions.RoleId.provided(environment.getProperty("spring.cloud.vault.app-role.role-id")))
                .secretId(AppRoleAuthenticationOptions.SecretId.provided(environment.getProperty("spring.cloud.vault.app-role.secret-id")))
                .path(environment.getProperty("spring.cloud.vault.app-role.path", "auth/approle"))
                .build();

        return new AppRoleAuthentication(options, restOperations());
    }

    @Bean
    public VaultTemplate vaultTemplate() {
        return new VaultTemplate(vaultEndpoint(), clientAuthentication());
    }

    @Override
    public SslConfiguration sslConfiguration() {
        String trustStorePath = environment.getProperty("spring.cloud.vault.ssl.trust-store");
        String trustStorePassword = environment.getProperty("spring.cloud.vault.ssl.trust-store-password");
        if (trustStorePath == null || trustStorePath.isBlank()) {
            return SslConfiguration.unconfigured();
        }
        try {
            Resource resource = new DefaultResourceLoader().getResource(trustStorePath);
            if (!resource.exists()) {
                return SslConfiguration.unconfigured();
            }
            return SslConfiguration.forTrustStore(
                    resource,
                    trustStorePassword != null ? trustStorePassword.toCharArray() : new char[0]
            );
        } catch (Exception e) {
            return SslConfiguration.unconfigured();
        }
    }

    @Bean
    @Primary
    @Override
    public RestTemplateFactory restTemplateFactory(ClientFactoryWrapper clientFactoryWrapper) {
        return super.restTemplateFactory(clientFactoryWrapper);
    }
}