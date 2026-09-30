package com.studyvault.reviews;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @EntityGraph(attributePaths = {"user"})
    Page<Review> findAllByProductIdAndStatusOrderByCreatedAtDesc(UUID productId, Review.Status status, Pageable pageable);

    Optional<Review> findByProductIdAndUserId(UUID productId, UUID userId);

    @Query("select coalesce(avg(r.rating), 0) from Review r where r.product.id = :productId and r.status = 'PUBLISHED'")
    double averageRating(@Param("productId") UUID productId);

    long countByProductIdAndStatus(UUID productId, Review.Status status);

    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
