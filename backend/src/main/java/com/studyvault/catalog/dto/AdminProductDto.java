package com.studyvault.catalog.dto;

import com.studyvault.catalog.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Admin projection including operational fields hidden from the storefront. */
public record AdminProductDto(UUID id, String title, String slug, String shortDescription,
                              UUID categoryId, String categoryName, List<String> tags,
                              long priceMinor, Long comparePriceMinor, String currency,
                              Product.Status status, boolean featured, boolean bestseller,
                              BigDecimal avgRating, int reviewCount, long viewCount,
                              int contentVersion, String coverImageKey, Instant createdAt, Instant updatedAt) {

    public static AdminProductDto from(Product p) {
        return new AdminProductDto(p.getId(), p.getTitle(), p.getSlug(), p.getShortDescription(),
                p.getCategory().getId(), p.getCategory().getName(),
                p.getTags().stream().map(com.studyvault.catalog.Tag::getName).sorted().toList(),
                p.getPriceMinor(), p.getComparePriceMinor(), p.getCurrency(),
                p.getStatus(), p.isFeatured(), p.isBestseller(),
                p.getAvgRating(), p.getReviewCount(), p.getViewCount(),
                p.getContentVersion(), p.getCoverImageKey(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
