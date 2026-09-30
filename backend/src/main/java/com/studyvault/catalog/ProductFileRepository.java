package com.studyvault.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductFileRepository extends JpaRepository<ProductFile, UUID> {

    java.util.Optional<ProductFile> findByProductIdAndCurrentTrue(UUID productId);

    java.util.List<ProductFile> findAllByProductIdOrderByVersionDesc(UUID productId);

    @Modifying
    @Query("update ProductFile f set f.current = false where f.product.id = :productId and f.current = true")
    void clearCurrentForProduct(@Param("productId") UUID productId);
}
