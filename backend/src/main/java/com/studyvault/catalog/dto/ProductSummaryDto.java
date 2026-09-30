package com.studyvault.catalog.dto;

import com.studyvault.catalog.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Public product projection - never exposes internal state like drafts or admin flags. */
public record ProductSummaryDto(UUID id, String title, String slug, String shortDescription,
                                String categoryName, String categorySlug, List<String> tags,
                                long priceMinor, Long comparePriceMinor, String currency,
                                boolean featured, boolean bestseller,
                                BigDecimal avgRating, int reviewCount, String coverImageKey) {

    public static ProductSummaryDto from(Product p) {
        return new ProductSummaryDto(p.getId(), p.getTitle(), p.getSlug(), p.getShortDescription(),
                p.getCategory().getName(), p.getCategory().getSlug(),
                p.getTags().stream().map(com.studyvault.catalog.Tag::getName).sorted().toList(),
                p.getPriceMinor(), p.getComparePriceMinor(), p.getCurrency(),
                p.isFeatured(), p.isBestseller(), p.getAvgRating(), p.getReviewCount(), p.getCoverImageKey());
    }
}
