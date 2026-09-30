package com.studyvault.user;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findWithRolesByEmail(String email);

    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findWithRolesById(UUID id);

    @Query("select count(u) > 0 from User u join u.roles r where r.name = :roleName")
    boolean existsByRoleName(@Param("roleName") String roleName);

    @org.springframework.data.jpa.repository.Query("select u from User u where lower(u.email) like :like or lower(u.fullName) like :like")
    org.springframework.data.domain.Page<User> search(@Param("like") String like,
                                                      org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Query("update User u set u.failedLoginAttempts = 0, u.lockedUntil = null where u.id = :userId")
    void resetLoginFailures(@Param("userId") UUID userId);

    long count();
}
