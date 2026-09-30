package com.studyvault.catalog.dto;

import com.studyvault.catalog.Category;

import java.util.UUID;

public record CategoryDto(UUID id, String name, String slug, String description, int sortOrder, boolean active) {

    public static CategoryDto from(Category category) {
        return new CategoryDto(category.getId(), category.getName(), category.getSlug(),
                category.getDescription(), category.getSortOrder(), category.isActive());
    }
}
