package com.studyvault.user;

import com.studyvault.common.api.ApiResponse;
import com.studyvault.common.audit.AuditService;
import com.studyvault.common.error.NotFoundException;
import com.studyvault.common.error.UnauthorizedException;
import com.studyvault.config.AppProperties;
import com.studyvault.security.CookieWriter;
import com.studyvault.security.SecurityUser;
import com.studyvault.security.SecurityUtils;
import com.studyvault.user.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SessionService sessionService;
    private final UserRepository users;
    private final CookieWriter cookies;
    private final AppProperties props;
    private final AuditService audit;

    private final SecureRandom secureRandom = new SecureRandom();

    /** Issues the CSRF double-submit cookie used by all mutating requests. */
    @GetMapping("/csrf")
    public ApiResponse<Map<String, String>> csrf(HttpServletResponse response) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        cookies.writeCsrf(response, token);
        return ApiResponse.ok(Map.of("token", token));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserDto> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.register(request, httpRequest),
                "Account created. Please check your email to verify your address.");
    }

    @PostMapping("/login")
    public ApiResponse<UserDto> login(@Valid @RequestBody LoginRequest request,
                                      HttpServletRequest httpRequest, HttpServletResponse response) {
        AuthService.LoginResult result = authService.login(request, httpRequest);
        cookies.writeSession(response, result.sessionToken(), Duration.ofDays(props.getSession().getTtlDays()).toSeconds());
        return ApiResponse.ok(result.user(), "Login successful.");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@CookieValue(value = "sv_session", required = false) String sessionToken,
                                    HttpServletRequest httpRequest, HttpServletResponse response) {
        if (sessionToken != null && !sessionToken.isBlank()) {
            authService.logout(sessionToken);
            audit.record(httpRequest, "AUTH_LOGOUT", "user",
                    SecurityUtils.currentUser().map(SecurityUser::id).map(UUID::toString).orElse("unknown"), Map.of());
        }
        cookies.clearSession(response);
        return ApiResponse.message("Logged out.");
    }

    @PostMapping("/logout-all")
    public ApiResponse<Void> logoutAll(HttpServletRequest httpRequest) {
        UUID userId = requireCurrent().id();
        authService.logoutAll(userId);
        audit.record(httpRequest, "AUTH_LOGOUT_ALL", "user", userId.toString(), Map.of());
        return ApiResponse.message("Signed out from all devices.");
    }

    @GetMapping("/me")
    public ApiResponse<UserDto> me() {
        SecurityUser user = requireCurrent();
        return users.findWithRolesById(user.id())
                .map(u -> ApiResponse.ok(UserDto.from(u)))
                .orElseThrow(() -> new UnauthorizedException("Authentication required."));
    }

    @GetMapping("/sessions")
    public ApiResponse<List<SessionDto>> sessions(@CookieValue(value = "sv_session", required = false) String sessionToken) {
        UUID userId = requireCurrent().id();
        UUID currentSessionId = sessionToken != null ? sessionService.currentSessionId(sessionToken) : null;
        List<SessionDto> list = sessionService.listActive(userId).stream()
                .map(s -> SessionDto.from(s, currentSessionId))
                .toList();
        return ApiResponse.ok(list);
    }

    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> revokeSession(@PathVariable UUID id) {
        UUID userId = requireCurrent().id();
        if (!sessionService.revokeOne(userId, id)) {
            throw new NotFoundException("Session not found.");
        }
        return ApiResponse.message("Session signed out.");
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                            @CookieValue(value = "sv_session", required = false) String sessionToken,
                                            HttpServletRequest httpRequest) {
        UUID userId = requireCurrent().id();
        authService.changePassword(userId, request, sessionToken, httpRequest);
        return ApiResponse.message("Password changed. Other devices have been signed out.");
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                            HttpServletRequest httpRequest) {
        authService.forgotPassword(request, httpRequest);
        // Deliberately identical response whether or not the account exists.
        return ApiResponse.message("If an account exists for that email, a reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                           HttpServletRequest httpRequest) {
        authService.resetPassword(request, httpRequest);
        return ApiResponse.message("Password updated. Please sign in again.");
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ApiResponse.message("Email verified. Thank you!");
    }

    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification(HttpServletRequest httpRequest) {
        authService.resendVerification(requireCurrent().id(), httpRequest);
        return ApiResponse.message("Verification email sent.");
    }

    private SecurityUser requireCurrent() {
        return SecurityUtils.currentUser().orElseThrow(() -> new UnauthorizedException("Authentication required."));
    }
}
