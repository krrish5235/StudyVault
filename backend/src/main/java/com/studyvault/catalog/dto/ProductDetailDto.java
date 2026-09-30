package com.studyvault.catalog.dto;

import com.studyvault.catalog.Product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductDetailDto(UUID id, String title, String slug, String shortDescription, String description,
                               String categoryName, String categorySlug, java.util.List<String> tags,
                               long priceMinor, Long comparePriceMinor, String currency,
                               BigDecimal avgRating, int reviewCount, String coverImageKey,
                               int contentVersion, java.time.Instant updatedAt) {

    public static ProductDetailDto from(Product p) {
        return new ProductDetailDto(p.getId(), p.getTitle(), p.getSlug(), p.getShortDescription(), p.getDescription(),
                p.getCategory().getName(), p.getCategory().getSlug(),
                p.getTags().stream().map(com.studyvault.catalog.Tag::getName).sorted().toList(),
                p.getPriceMinor(), p.getComparePriceMinor(), p.getCurrency(),
                p.getAvgRating(), p.getReviewCount(), p.getCoverImageKey(),
                p.getContentVersion(), p.getUpdatedAt());
    }
}
