package com.studyvault.security;

import com.studyvault.config.AppProperties;
import com.studyvault.user.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.Optional;

/**
 * Cookie-based session authentication. Loads the server-side session for the opaque
 * cookie token and populates the SecurityContext (user + roles). Stateless by design:
 * no server HTTP session is created.
 */
public class SessionAuthFilter extends OncePerRequestFilter {

    private final SessionService sessionService;
    private final AppProperties props;

    public SessionAuthFilter(SessionService sessionService, AppProperties props) {
        this.sessionService = sessionService;
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Cookie cookie = WebUtils.getCookie(request, props.getCookie().getSessionName());
        if (cookie != null && !cookie.getValue().isBlank()) {
            Optional<Authentication> auth = sessionService.resolve(cookie.getValue());
            auth.ifPresent(a -> SecurityContextHolder.getContext().setAuthentication(a));
        }
        chain.doFilter(request, response);
    }
}
