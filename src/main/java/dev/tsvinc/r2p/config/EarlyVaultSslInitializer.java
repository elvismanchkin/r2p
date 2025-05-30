package dev.tsvinc.r2p.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultTemplate;

// Alternative: Using @EventListener for earlier initialization
//@Component
//public class EarlyVaultSslInitializer {
//
//    private final VaultTemplate vaultTemplate;
//
//    public EarlyVaultSslInitializer(VaultTemplate vaultTemplate) {
//        this.vaultTemplate = vaultTemplate;
//    }
//
//    @EventListener(ApplicationEnvironmentPreparedEvent.class)
//    public void onApplicationEnvironmentPrepared(ApplicationEnvironmentPreparedEvent event) {
//        try {
//            initializeMutualTls();
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to initialize SSL from Vault", e);
//        }
//    }
//
//    // Same initializeMutualTls() method as above
//}