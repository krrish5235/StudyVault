package com.studyvault.user;

import com.studyvault.config.AppProperties;
import com.studyvault.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-side session lifecycle. Opaque cookie tokens are stored only as SHA-256
 * hashes; sessions are revocable instantly and slide their expiry with activity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository sessions;
    private final AppProperties props;
    private final TokenService tokens;

    @Transactional
    public String create(User user, String ip, String userAgent) {
        TokenService.Issued issued = tokens.issue(Duration.ofDays(props.getSession().getTtlDays()));
        UserSession session = new UserSession();
        session.setUser(user);
        session.setTokenHash(issued.hash());
        session.setExpiresAt(issued.expiresAt());
        session.setLastUsedAt(Instant.now());
        session.setIpAddress(truncate(ip, 45));
        session.setUserAgent(truncate(userAgent, 400));
        sessions.save(session);
        return issued.raw();
    }

    @Transactional
    public Optional<Authentication> resolve(String rawToken) {
        UserSession session = sessions.findByTokenHash(TokenService.sha256Hex(rawToken)).orElse(null);
        if (session == null) {
            return Optional.empty();
        }
        Instant now = Instant.now();
        if (session.getRevokedAt() != null || session.getExpiresAt().isBefore(now)) {
            return Optional.empty();
        }
        var user = session.getUser();
        if (!user.isEnabled()) {
            return Optional.empty();
        }

        // Sliding expiry, capped at the maximum lifetime from creation.
        if (session.getLastUsedAt().isBefore(now.minus(props.getSession().getSlideAfterHours(), ChronoUnit.HOURS))) {
            Instant maxExpiry = session.getCreatedAt().plus(props.getSession().getMaxLifetimeDays(), ChronoUnit.DAYS);
            Instant extended = now.plus(props.getSession().getTtlDays(), ChronoUnit.DAYS);
            session.setExpiresAt(maxExpiry.isBefore(extended) ? maxExpiry : extended);
            session.setLastUsedAt(now);
        }

        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .toList();
        SecurityUser principal = new SecurityUser(user.getId(), user.getEmail(), user.getFullName(), authorities);
        return Optional.of(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    @Transactional(readOnly = true)
    public UUID currentSessionId(String rawToken) {
        return sessions.findByTokenHash(TokenService.sha256Hex(rawToken))
                .map(UserSession::getId)
                .orElse(null);
    }

    @Transactional
    public void revoke(String rawToken) {
        sessions.findByTokenHash(TokenService.sha256Hex(rawToken))
                .filter(s -> s.getRevokedAt() == null)
                .ifPresent(s -> s.setRevokedAt(Instant.now()));
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        sessions.revokeAllActive(userId, Instant.now());
    }

    @Transactional
    public void revokeAllForUserExcept(UUID userId, String keepRawToken) {
        sessions.revokeAllActiveExcept(userId, TokenService.sha256Hex(keepRawToken), Instant.now());
    }

    @Transactional
    public boolean revokeOne(UUID userId, UUID sessionId) {
        return sessions.findByIdAndUserId(sessionId, userId)
                .filter(s -> s.getRevokedAt() == null)
                .map(s -> {
                    s.setRevokedAt(Instant.now());
                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<UserSession> listActive(UUID userId) {
        return sessions.findAllByUserIdAndRevokedAtIsNullOrderByLastUsedAtDesc(userId);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }
}
