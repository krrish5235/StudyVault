package com.studyvault.orders;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntitlementRepository extends JpaRepository<Entitlement, UUID> {

    boolean existsByUserIdAndProductIdAndRevokedAtIsNull(UUID userId, UUID productId);

    boolean existsByProductId(UUID productId);

    @EntityGraph(attributePaths = {"product", "product.category"})
    List<Entitlement> findAllByUserIdAndRevokedAtIsNullOrderByGrantedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"product", "product.category"})
    Optional<Entitlement> findByUserIdAndProductIdAndRevokedAtIsNull(UUID userId, UUID productId);
}
