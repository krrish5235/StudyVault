package com.studyvault.oauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * OAuth providers are enabled purely by environment variables:
 *   GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET
 *   GITHUB_CLIENT_ID / GITHUB_CLIENT_SECRET
 * Providers without credentials are simply not registered - the rest of the
 * app works identically without them.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OAuthClientConfig {

    private final Environment env;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> registrations = new ArrayList<>();

        String googleId = env.getProperty("GOOGLE_CLIENT_ID");
        String googleSecret = env.getProperty("GOOGLE_CLIENT_SECRET");
        if (isConfigured(googleId, googleSecret)) {
            registrations.add(ClientRegistration.withRegistrationId("google")
                    .clientId(googleId)
                    .clientSecret(googleSecret)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("openid", "profile", "email")
                    .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                    .tokenUri("https://oauth2.googleapis.com/token")
                    .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                    .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                    .userNameAttributeName("sub")
                    .clientName("Google")
                    .build());
            log.info("OAuth provider enabled: google");
        }

        String githubId = env.getProperty("GITHUB_CLIENT_ID");
        String githubSecret = env.getProperty("GITHUB_CLIENT_SECRET");
        if (isConfigured(githubId, githubSecret)) {
            registrations.add(ClientRegistration.withRegistrationId("github")
                    .clientId(githubId)
                    .clientSecret(githubSecret)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("read:user", "user:email")
                    .authorizationUri("https://github.com/login/oauth/authorize")
                    .tokenUri("https://github.com/login/oauth/access_token")
                    .userInfoUri("https://api.github.com/user")
                    .userNameAttributeName("id")
                    .clientName("GitHub")
                    .build());
            log.info("OAuth provider enabled: github");
        }

        if (registrations.isEmpty()) {
            // Placeholder registration keeps the repository valid; it is never
            // advertised (GET /auth/oauth/providers returns an empty list and
            // oauth2Login is not wired when this state exists).
            registrations.add(ClientRegistration.withRegistrationId("none")
                    .clientId("placeholder")
                    .clientSecret("placeholder")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .tokenUri("https://invalid.local/token")
                    .authorizationUri("https://invalid.local/authorize")
                    .build());
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }

    public boolean anyProviderEnabled() {
        return isConfigured(env.getProperty("GOOGLE_CLIENT_ID"), env.getProperty("GOOGLE_CLIENT_SECRET"))
                || isConfigured(env.getProperty("GITHUB_CLIENT_ID"), env.getProperty("GITHUB_CLIENT_SECRET"));
    }

    public List<String> enabledProviders() {
        List<String> providers = new ArrayList<>();
        if (isConfigured(env.getProperty("GOOGLE_CLIENT_ID"), env.getProperty("GOOGLE_CLIENT_SECRET"))) {
            providers.add("google");
        }
        if (isConfigured(env.getProperty("GITHUB_CLIENT_ID"), env.getProperty("GITHUB_CLIENT_SECRET"))) {
            providers.add("github");
        }
        return providers;
    }

    private boolean isConfigured(String id, String secret) {
        return StringUtils.hasText(id) && StringUtils.hasText(secret)
                && !id.equals("placeholder") && !secret.equals("placeholder");
    }
}
