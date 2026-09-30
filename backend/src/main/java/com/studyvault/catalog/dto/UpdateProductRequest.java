package com.studyvault.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Partial update - only non-null fields are applied. Price is server-validated. */
public record UpdateProductRequest(
        @Size(min = 1, max = 200) String title,
        @Size(min = 10, max = 300) String shortDescription,
        @Size(min = 20, max = 50000) String description,
        UUID categoryId,
        @Min(0) Long priceMinor,
        @Min(0) Long comparePriceMinor,
        List<String> tags,
        Boolean featured,
        Boolean bestseller) {
}
