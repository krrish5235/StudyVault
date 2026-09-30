package com.studyvault.user.dto;

import com.studyvault.user.UserSession;

import java.time.Instant;
import java.util.UUID;

public record SessionDto(UUID id, Instant createdAt, Instant lastUsedAt, Instant expiresAt,
                         String ipAddress, String userAgent, boolean current) {

    public static SessionDto from(UserSession session, UUID currentSessionId) {
        return new SessionDto(session.getId(), session.getCreatedAt(), session.getLastUsedAt(),
                session.getExpiresAt(), session.getIpAddress(),
                session.getUserAgent(), session.getId().equals(currentSessionId));
    }
}
