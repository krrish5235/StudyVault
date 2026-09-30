package com.studyvault.catalog;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = {"category", "tags"})
    Optional<Product> findWithRelationsBySlug(String slug);

    @EntityGraph(attributePaths = {"category", "tags"})
    Optional<Product> findWithRelationsById(UUID id);

    @EntityGraph(attributePaths = {"category", "tags"})
    List<Product> findByStatusAndFeaturedTrueOrderByCreatedAtDesc(Product.Status status);

    @EntityGraph(attributePaths = {"category", "tags"})
    List<Product> findTop8ByStatusAndBestsellerTrueOrderByViewCountDesc(Product.Status status);

    @Modifying
    @Query("update Product p set p.viewCount = p.viewCount + 1 where p.id = :id")
    void incrementViewCount(@Param("id") UUID id);
}
