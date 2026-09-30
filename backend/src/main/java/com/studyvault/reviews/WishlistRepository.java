package com.studyvault.reviews;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {

    Optional<Wishlist> findByUserId(UUID userId);

    @EntityGraph(attributePaths = {"items", "items.product", "items.product.category"})
    Optional<Wishlist> findWithItemsByUserId(UUID userId);
}
