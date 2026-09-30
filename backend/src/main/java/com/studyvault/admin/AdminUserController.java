package com.studyvault.admin;

import com.studyvault.common.api.ApiResponse;
import com.studyvault.common.api.PageResponse;
import com.studyvault.common.audit.AuditService;
import com.studyvault.common.error.BadRequestException;
import com.studyvault.common.error.NotFoundException;
import com.studyvault.user.User;
import com.studyvault.user.UserRepository;
import com.studyvault.user.UserSessionRepository;
import com.studyvault.user.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository users;
    private final SessionService sessions;
    private final AuditService audit;

    public record AdminUserDto(UUID id, String email, String fullName, boolean emailVerified,
                               boolean enabled, boolean mustChangePassword, java.util.List<String> roles,
                               Instant createdAt, Instant lastLoginAt) {
        public static AdminUserDto from(User u) {
            return new AdminUserDto(u.getId(), u.getEmail(), u.getFullName(), u.isEmailVerified(),
                    u.isEnabled(), u.isMustChangePassword(), java.util.List.copyOf(u.getRoleNames()),
                    u.getCreatedAt(), u.getLastLoginAt());
        }
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminUserDto>> list(@RequestParam(required = false) String query,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> result;
        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim().toLowerCase() + "%";
            result = users.search(like, pageable);
        } else {
            result = users.findAll(pageable);
        }
        return ApiResponse.ok(PageResponse.of(result, result.map(AdminUserDto::from).getContent()));
    }

    /** Disabling an account instantly revokes all its sessions. */
    @PatchMapping("/{id}/enabled")
    @Transactional
    public ApiResponse<AdminUserDto> setEnabled(@PathVariable UUID id, @RequestBody Map<String, Boolean> body,
                                                HttpServletRequest httpRequest) {
        User user = users.findById(id).orElseThrow(() -> new NotFoundException("User not found."));
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            throw new BadRequestException("Provide the enabled field.");
        }
        user.setEnabled(enabled);
        if (!enabled) {
            sessions.revokeAllForUser(id);
        }
        User saved = users.saveAndFlush(user);
        audit.record(httpRequest, enabled ? "USER_ENABLED" : "USER_DISABLED", "user", id.toString(), Map.of());
        return ApiResponse.ok(AdminUserDto.from(saved));
    }
}
