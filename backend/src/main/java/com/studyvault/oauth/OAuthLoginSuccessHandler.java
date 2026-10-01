package com.studyvault.oauth;

import com.studyvault.common.audit.AuditService;
import com.studyvault.common.web.ClientIpResolver;
import com.studyvault.config.AppProperties;
import com.studyvault.security.CookieWriter;
import com.studyvault.user.SessionService;
import com.studyvault.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/** Establishes the StudyVault session on OAuth sign-in and redirects to the frontend. */
@Component
@RequiredArgsConstructor
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthAccountService oauthAccounts;
    private final SessionService sessions;
    private final CookieWriter cookies;
    private final AppProperties props;
    private final AuditService audit;
    private final ClientIpResolver ipResolver;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String provider = (String) principal.getAttributes().get(CustomOAuth2UserService.ATTR_PROVIDER);
        String providerId = String.valueOf(principal.getAttributes().get(CustomOAuth2UserService.ATTR_PROVIDER_ID));
        String email = (String) principal.getAttributes().get(CustomOAuth2UserService.ATTR_EMAIL);
        String name = (String) principal.getAttributes().get(CustomOAuth2UserService.ATTR_NAME);

        try {
            OAuthAccountService.ResolvedUser resolved =
                    oauthAccounts.findOrCreate(provider, providerId, email, name, ipResolver.resolve(request));
            User user = resolved.user();
            if (!user.isEnabled()) {
                response.sendRedirect(props.getFrontendUrl() + "/login?error=account_disabled");
                return;
            }

            String token = sessions.create(user, ipResolver.resolve(request), request.getHeader("User-Agent"));
            cookies.writeSession(response, token, Duration.ofDays(props.getSession().getTtlDays()).toSeconds());
            audit.record(request, resolved.newlyCreated() ? "AUTH_OAUTH_REGISTER" : "AUTH_OAUTH_LOGIN",
                    "user", user.getId().toString(), Map.of("provider", provider, "email", user.getEmail()));
            response.sendRedirect(props.getFrontendUrl() + "/oauth/success?provider=" + provider);
        } catch (Exception e) {
            // Email collision or persistence failure - never surface internals to the browser.
            org.slf4j.LoggerFactory.getLogger(OAuthLoginSuccessHandler.class)
                    .error("OAuth sign-in post-processing failed: {}", e.getMessage());
            audit.record(null, email, request, "AUTH_OAUTH_FAILED", "user", providerId,
                    Map.of("provider", provider));
            response.sendRedirect(props.getFrontendUrl() + "/login?error=oauth_failed");
        }
    }
}
