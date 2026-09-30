package com.studyvault.reviews;

import com.studyvault.catalog.Product;
import com.studyvault.catalog.dto.ProductSummaryDto;
import com.studyvault.catalog.ProductRepository;
import com.studyvault.common.error.NotFoundException;
import com.studyvault.user.User;
import com.studyvault.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlists;
    private final ProductRepository products;
    private final UserRepository users;

    @Transactional
    public void add(UUID userId, UUID productId) {
        Wishlist wishlist = wishlists.findByUserId(userId).orElseGet(() -> {
            Wishlist w = new Wishlist();
            w.setUser(users.getReferenceById(userId));
            return wishlists.saveAndFlush(w);
        });
        Product product = products.getReferenceById(productId);
        boolean present = wishlist.getItems().stream().anyMatch(i -> i.getProduct().getId().equals(productId));
        if (!present) {
            wishlist.getItems().add(Wishlist.itemFor(wishlist, product));
            wishlists.save(wishlist);
        }
    }

    @Transactional
    public void remove(UUID userId, UUID productId) {
        wishlists.findWithItemsByUserId(userId).ifPresent(w -> {
            w.getItems().removeIf(i -> i.getProduct().getId().equals(productId));
            wishlists.save(w);
        });
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryDto> list(UUID userId) {
        return wishlists.findWithItemsByUserId(userId)
                .map(w -> w.getItems().stream()
                        .map(i -> ProductSummaryDto.from(i.getProduct()))
                        .toList())
                .orElse(List.of());
    }
}
