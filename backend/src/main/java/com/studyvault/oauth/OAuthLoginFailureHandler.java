package com.studyvault.oauth;

import com.studyvault.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Routes OAuth failures back to the frontend with a safe, generic error code. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthLoginFailureHandler implements AuthenticationFailureHandler {

    private final AppProperties props;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.error("OAuth sign-in failed: {}: {}", exception.getClass().getSimpleName(), exception.getMessage());
        String code = "oauth_failed";
        if (exception instanceof OAuth2AuthenticationException oae && oae.getError() != null) {
            code = switch (oae.getError().getErrorCode()) {
                case "no_verified_email" -> "no_verified_email";
                case "invalid_state" -> "expired_or_invalid_request";
                default -> "oauth_failed";
            };
        }
        response.sendRedirect(props.getFrontendUrl() + "/login?error=" + code);
    }
}
