package com.studyvault.oauth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;
import java.util.UUID;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {

    @EntityGraph(attributePaths = {"user", "user.roles"})
    Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
}
