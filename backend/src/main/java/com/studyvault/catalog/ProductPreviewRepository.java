package com.studyvault.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductPreviewRepository extends JpaRepository<ProductPreview, UUID> {

    List<ProductPreview> findAllByProductIdOrderBySortOrderAsc(UUID productId);
}
