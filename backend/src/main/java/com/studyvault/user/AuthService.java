package com.studyvault.user;

import com.studyvault.common.audit.AuditService;
import com.studyvault.common.email.EmailService;
import com.studyvault.common.error.BadRequestException;
import com.studyvault.common.error.ConflictException;
import com.studyvault.common.error.UnauthorizedException;
import com.studyvault.config.AppProperties;
import com.studyvault.user.dto.ChangePasswordRequest;
import com.studyvault.user.dto.ForgotPasswordRequest;
import com.studyvault.user.dto.LoginRequest;
import com.studyvault.user.dto.RegisterRequest;
import com.studyvault.user.dto.ResetPasswordRequest;
import com.studyvault.user.dto.UserDto;
import com.studyvault.user.dto.VerifyEmailRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Authentication use-cases. Design notes:
 * - Generic "Invalid email or password." for every login failure (no user enumeration);
 *   a dummy hash comparison keeps timing similar for unknown emails.
 * - Password reset always answers 200 at the controller regardless of account existence.
 * - Successful password reset / change revokes sessions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);
    private static final Duration VERIFY_TOKEN_TTL = Duration.ofHours(24);

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordResetTokenRepository resetTokens;
    private final EmailVerificationTokenRepository verificationTokens;
    private final SessionService sessions;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailService emails;
    private final AuditService audit;
    private final AppProperties props;

    /** Hash of a throwaway password so unknown-email logins cost the same as known ones. */
    private String dummyHash;

    @jakarta.annotation.PostConstruct
    void initDummyHash() {
        dummyHash = passwordEncoder.encode("timing-equalizer-password-1A");
    }

    @Transactional
    public UserDto register(RegisterRequest request, HttpServletRequest httpRequest) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists.");
        }
        User user = new User();
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmailVerified(false);
        roles.findByName(Role.USER).ifPresent(user.getRoles()::add);
        users.saveAndFlush(user); // flush so timestamps are populated before mapping

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        TokenService.Issued issued = tokenService.issue(VERIFY_TOKEN_TTL);
        token.setTokenHash(issued.hash());
        token.setExpiresAt(issued.expiresAt());
        verificationTokens.save(token);

        audit.record(httpRequest, "AUTH_REGISTER", "user", user.getId().toString(), Map.of("email", email));
        emails.sendWelcome(email, user.getFullName());
        emails.sendVerification(email, user.getFullName(), issued.raw());
        return UserDto.from(user);
    }

    public record LoginResult(UserDto user, String sessionToken) {
    }

    @Transactional
    public LoginResult login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.email().trim().toLowerCase();
        User user = users.findWithRolesByEmail(email).orElse(null);

        boolean accountLockActive = user != null && user.isCurrentlyLocked(Instant.now());
        boolean matches;
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            matches = false;
        } else {
            matches = !accountLockActive && passwordEncoder.matches(request.password(), user.getPasswordHash());
        }

        if (!matches) {
            if (user != null && !accountLockActive) {
                int attempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(attempts);
                if (attempts >= MAX_FAILED_ATTEMPTS) {
                    user.setLockedUntil(Instant.now().plus(LOCK_DURATION));
                    user.setFailedLoginAttempts(0);
                    audit.record(httpRequest, "SECURITY_ACCOUNT_LOCKED", "user", user.getId().toString(), Map.of("email", email));
                    emails.sendAccountLocked(email);
                }
            }
            audit.record(httpRequest, "AUTH_LOGIN_FAILED", "user", user != null ? user.getId().toString() : "unknown", Map.of("email", email));
            throw new UnauthorizedException("Invalid email or password.");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        String token = sessions.create(user,
                httpRequest != null ? httpRequest.getRemoteAddr() : null,
                httpRequest != null ? httpRequest.getHeader("User-Agent") : null);

        audit.record(httpRequest, "AUTH_LOGIN", "user", user.getId().toString(), Map.of("email", email));
        return new LoginResult(UserDto.from(user), token);
    }

    @Transactional
    public void logout(String rawSessionToken) {
        sessions.revoke(rawSessionToken);
    }

    @Transactional
    public void logoutAll(UUID userId) {
        sessions.revokeAllForUser(userId);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request, String currentSessionToken,
                               HttpServletRequest httpRequest) {
        User user = users.findById(userId).orElseThrow(() -> new BadRequestException("Account not found."));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        if (currentSessionToken != null) {
            sessions.revokeAllForUserExcept(userId, currentSessionToken);
        } else {
            sessions.revokeAllForUser(userId);
        }
        audit.record(httpRequest, "AUTH_PASSWORD_CHANGED", "user", userId.toString(), Map.of());
        emails.sendPasswordChanged(user.getEmail(), user.getFullName());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        String email = request.email().trim().toLowerCase();
        users.findByEmail(email).ifPresent(user -> {
            resetTokens.invalidateAllForUser(user.getId(), Instant.now());
            TokenService.Issued issued = tokenService.issue(RESET_TOKEN_TTL);
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(issued.hash());
            token.setExpiresAt(issued.expiresAt());
            resetTokens.save(token);
            audit.record(httpRequest, "AUTH_PASSWORD_RESET_REQUESTED", "user", user.getId().toString(), Map.of());
            emails.sendPasswordReset(email, issued.raw());
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest) {
        PasswordResetToken token = resetTokens.findByTokenHash(TokenService.sha256Hex(request.token()))
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset link."));
        if (!token.isUsable(Instant.now())) {
            throw new BadRequestException("Invalid or expired reset link.");
        }
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        token.markUsed(Instant.now());
        sessions.revokeAllForUser(user.getId());
        audit.record(httpRequest, "AUTH_PASSWORD_RESET_COMPLETED", "user", user.getId().toString(), Map.of());
        emails.sendPasswordChanged(user.getEmail(), user.getFullName());
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        EmailVerificationToken token = verificationTokens.findByTokenHash(TokenService.sha256Hex(request.token()))
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification link."));
        if (!token.isUsable(Instant.now())) {
            throw new BadRequestException("Invalid or expired verification link.");
        }
        User user = token.getUser();
        user.setEmailVerified(true);
        token.markUsed(Instant.now());
    }

    @Transactional
    public void resendVerification(UUID userId, HttpServletRequest httpRequest) {
        User user = users.findById(userId).orElseThrow(() -> new BadRequestException("Account not found."));
        if (user.isEmailVerified()) {
            throw new ConflictException("Email is already verified.");
        }
        verificationTokens.invalidateAllForUser(user.getId(), Instant.now());
        TokenService.Issued issued = tokenService.issue(VERIFY_TOKEN_TTL);
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(issued.hash());
        token.setExpiresAt(issued.expiresAt());
        verificationTokens.save(token);
        audit.record(httpRequest, "AUTH_VERIFICATION_RESENT", "user", user.getId().toString(), Map.of());
        emails.sendVerification(user.getEmail(), user.getFullName(), issued.raw());
    }
}
