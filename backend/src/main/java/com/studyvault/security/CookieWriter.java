package com.studyvault.security;

import com.studyvault.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

/** Builds auth cookies honoring environment configuration (secure flag, domain). */
@Component
@RequiredArgsConstructor
public class CookieWriter {

    private final AppProperties props;

    public void writeSession(HttpServletResponse response, String token, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(props.getCookie().getSessionName(), token)
                .httpOnly(true)
                .secure(props.getCookie().isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .domain(blankToNull(props.getCookie().getDomain()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void writeCsrf(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(props.getCookie().getCsrfName(), token)
                .httpOnly(false) // double-submit requires JS read access; it is not an auth secret
                .secure(props.getCookie().isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(60 * 60 * 8)
                .domain(blankToNull(props.getCookie().getDomain()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearSession(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(props.getCookie().getSessionName(), "")
                .httpOnly(true)
                .secure(props.getCookie().isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .domain(blankToNull(props.getCookie().getDomain()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
