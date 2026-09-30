package com.studyvault.user;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    @EntityGraph(attributePaths = {"user", "user.roles"})
    Optional<UserSession> findByTokenHash(String tokenHash);

    Optional<UserSession> findByIdAndUserId(UUID id, UUID userId);

    List<UserSession> findAllByUserIdAndRevokedAtIsNullOrderByLastUsedAtDesc(UUID userId);

    @Modifying
    @Query("update UserSession s set s.revokedAt = :now where s.user.id = :userId and s.revokedAt is null")
    int revokeAllActive(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("update UserSession s set s.revokedAt = :now where s.user.id = :userId and s.revokedAt is null and s.tokenHash <> :keepHash")
    int revokeAllActiveExcept(@Param("userId") UUID userId, @Param("keepHash") String keepHash, @Param("now") Instant now);
}
