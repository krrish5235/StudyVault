package com.studyvault.security;

import com.studyvault.common.web.JsonResponses;
import com.studyvault.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

/**
 * CSRF protection for cookie-authenticated APIs: double-submit pattern.
 * Every mutating request must carry the token both in the csrf cookie and in the
 * X-XSRF-TOKEN header; an attacker on another origin can read neither.
 * Webhook endpoints are exempt (authenticated by provider signature, not cookies).
 */
public class CsrfDoubleSubmitFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-XSRF-TOKEN";
    private static final Set<String> PROTECTED_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String WEBHOOK_PREFIX = "/api/v1/payments/webhook/";

    private final AppProperties props;

    public CsrfDoubleSubmitFilter(AppProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!PROTECTED_METHODS.contains(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return path.startsWith(WEBHOOK_PREFIX) || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        Cookie cookie = WebUtils.getCookie(request, props.getCookie().getCsrfName());
        if (header == null || cookie == null
                || !MessageDigest.isEqual(header.getBytes(StandardCharsets.UTF_8), cookie.getValue().getBytes(StandardCharsets.UTF_8))) {
            JsonResponses.write(response, 403, "CSRF token missing or invalid. Refresh the page and try again.");
            return;
        }
        chain.doFilter(request, response);
    }
}
