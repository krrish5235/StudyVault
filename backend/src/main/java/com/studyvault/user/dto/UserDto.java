package com.studyvault.user.dto;

import com.studyvault.user.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Safe projection of a user - never includes password hashes or tokens. */
public record UserDto(UUID id, String email, String fullName, boolean emailVerified,
                      boolean mustChangePassword, List<String> roles, Instant createdAt) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getFullName(),
                user.isEmailVerified(), user.isMustChangePassword(),
                List.copyOf(user.getRoleNames()), user.getCreatedAt());
    }
}
