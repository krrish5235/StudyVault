package com.studyvault.security;

import com.studyvault.common.error.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<SecurityUser> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser su) {
            return Optional.of(su);
        }
        return Optional.empty();
    }

    public static SecurityUser requireUser() {
        return currentUser().orElseThrow(() -> new UnauthorizedException("Authentication required."));
    }

    public static boolean hasRole(String roleName) {
        return currentUser()
                .map(u -> u.authorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(a -> a.equals("ROLE_" + roleName)))
                .orElse(false);
    }
}
