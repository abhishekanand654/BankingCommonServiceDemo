package com.bank.platform.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PoC token provider.
 * In real enterprise setups this might call an OAuth2 client-credentials flow
 * or retrieve a token from Vault/STS.
 */
@Component
public class ServiceAuthTokenProvider {

    @Value("${bank.serviceAuth.staticToken:}")
    private String staticToken;

    private final AtomicReference<CachedToken> cache = new AtomicReference<>();

    public String getServiceToken() {
        // For PoC: allow a static token from config.
        if (staticToken != null && !staticToken.isBlank()) {
            return staticToken;
        }

        // Otherwise return a cached dummy token to show the pattern
        CachedToken current = cache.get();
        if (current != null && current.expiresAt.isAfter(Instant.now().plusSeconds(10))) {
            return current.token;
        }

        CachedToken newToken = new CachedToken("dummy-token-" + Instant.now().toEpochMilli(),
                Instant.now().plusSeconds(300));
        cache.set(newToken);
        return newToken.token;
    }

    private record CachedToken(String token, Instant expiresAt) {}
}
