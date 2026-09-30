package com.studyvault.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(min = 10, max = 300) String shortDescription,
        @NotBlank @Size(min = 20, max = 50000) String description,
        @NotNull UUID categoryId,
        @NotNull @Min(0) Long priceMinor,
        @Min(0) Long comparePriceMinor,
        List<String> tags,
        boolean featured,
        boolean bestseller) {
}
