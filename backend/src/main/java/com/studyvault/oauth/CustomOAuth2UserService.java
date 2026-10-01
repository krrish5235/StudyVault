package com.studyvault.oauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Normalizes provider identities into a verified email + display name.
 * An OAuth sign-in is refused unless the provider attests the email is verified
 * (prevents account takeover by unverified-email collision).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    public static final String ATTR_EMAIL = "studyvault_email";
    public static final String ATTR_NAME = "studyvault_name";
    public static final String ATTR_PROVIDER = "studyvault_provider";
    public static final String ATTR_PROVIDER_ID = "studyvault_provider_id";
    public static final String ATTR_VERIFIED = "studyvault_verified";

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attrs = new HashMap<>(oauth.getAttributes());

        String email;
        boolean verified;
        String name;
        String providerId;

        switch (provider) {
            case "google" -> {
                email = str(attrs.get("email"));
                verified = Boolean.TRUE.equals(attrs.get("email_verified"))
                        || "true".equals(str(attrs.get("email_verified")));
                name = StringUtils.hasText(str(attrs.get("name"))) ? str(attrs.get("name")) : email;
                providerId = str(attrs.get("sub"));
            }
            case "github" -> {
                providerId = str(attrs.get("id"));
                name = StringUtils.hasText(str(attrs.get("name"))) ? str(attrs.get("name")) : str(attrs.get("login"));
                email = str(attrs.get("email"));
                verified = email != null;
                if (email == null) {
                    // GitHub hides private emails - fetch from the emails endpoint.
                    String[] fetched = fetchGithubVerifiedEmail(userRequest);
                    email = fetched[0];
                    verified = fetched[1] != null;
                }
            }
            default -> throw new OAuth2AuthenticationException(new OAuth2Error("unsupported_provider"),
                    "Unsupported provider: " + provider);
        }

        if (email == null || !verified) {
            log.warn("OAuth sign-in refused: no verified email from provider {}", provider);
            throw new OAuth2AuthenticationException(new OAuth2Error("no_verified_email"),
                    "Your provider account has no verified email address.");
        }
        if (providerId == null || providerId.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_provider_id"),
                    "Provider did not return an account id.");
        }

        attrs.put(ATTR_EMAIL, email.toLowerCase());
        attrs.put(ATTR_NAME, name == null ? email : name);
        attrs.put(ATTR_PROVIDER, provider);
        attrs.put(ATTR_PROVIDER_ID, providerId);
        attrs.put(ATTR_VERIFIED, true);

        return new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_OAUTH2")),
                Map.copyOf(attrs), ATTR_PROVIDER_ID);
    }

    /** Returns {email, "true"|"null"} for the primary verified GitHub email. */
    private String[] fetchGithubVerifiedEmail(OAuth2UserRequest userRequest) {
        try {
            var emails = RestClient.builder()
                    .baseUrl("https://api.github.com/user/emails")
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + userRequest.getAccessToken().getTokenValue())
                    .defaultHeader("Accept", "application/vnd.github+json")
                    .build()
                    .get()
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });
            if (emails != null) {
                for (Map<String, Object> e : emails) {
                    if (Boolean.TRUE.equals(e.get("primary")) && Boolean.TRUE.equals(e.get("verified"))) {
                        return new String[]{str(e.get("email")), "true"};
                    }
                }
                for (Map<String, Object> e : emails) {
                    if (Boolean.TRUE.equals(e.get("verified"))) {
                        return new String[]{str(e.get("email")), "true"};
                    }
                }
            }
        } catch (Exception e) {
            log.error("GitHub email lookup failed: {}", e.getMessage());
        }
        return new String[]{null, null};
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
