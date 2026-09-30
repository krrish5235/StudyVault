package com.studyvault.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank
        String token,

        @NotBlank(message = "Password is required")
        @Size(min = 10, max = 128, message = "Password must be 10-128 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                message = "Password must contain uppercase, lowercase and a digit")
        String newPassword) {
}
