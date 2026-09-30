package com.studyvault.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductFileRepository extends JpaRepository<ProductFile, UUID> {

    java.util.Optional<ProductFile> findByProductIdAndCurrentTrue(UUID productId);

    java.util.List<ProductFile> findAllByProductIdOrderByVersionDesc(UUID productId);
}
