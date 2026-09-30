package com.studyvault.catalog;

import com.studyvault.common.audit.AuditService;
import com.studyvault.common.error.BadRequestException;
import com.studyvault.common.error.ConflictException;
import com.studyvault.common.error.NotFoundException;
import com.studyvault.common.util.Slugs;
import com.studyvault.catalog.dto.CategoryDto;
import com.studyvault.catalog.dto.CreateCategoryRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categories;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<CategoryDto> listPublic() {
        return categories.findAll().stream()
                .filter(Category::isActive)
                .sorted(java.util.Comparator.comparingInt(Category::getSortOrder))
                .map(CategoryDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> listAdmin() {
        return categories.findAll().stream()
                .sorted(java.util.Comparator.comparingInt(Category::getSortOrder))
                .map(CategoryDto::from)
                .toList();
    }

    @Transactional
    public CategoryDto create(CreateCategoryRequest request, HttpServletRequest httpRequest) {
        String slug = Slugs.slugify(request.name());
        if (categories.existsBySlug(slug)) {
            throw new ConflictException("A category with a similar name already exists.");
        }
        Category category = new Category();
        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setDescription(request.description());
        category.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        Category saved = categories.save(category);
        audit.record(httpRequest, "CATEGORY_CREATED", "category", saved.getId().toString(),
                Map.of("name", saved.getName()));
        return CategoryDto.from(saved);
    }

    @Transactional
    public CategoryDto update(UUID id, CreateCategoryRequest request, HttpServletRequest httpRequest) {
        Category category = categories.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found."));
        category.setName(request.name().trim());
        category.setDescription(request.description());
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        Category saved = categories.save(category);
        audit.record(httpRequest, "CATEGORY_UPDATED", "category", id.toString(), Map.of("name", saved.getName()));
        return CategoryDto.from(saved);
    }

    @Transactional
    public void deactivate(UUID id, HttpServletRequest httpRequest) {
        Category category = categories.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found."));
        category.setActive(false);
        categories.save(category);
        audit.record(httpRequest, "CATEGORY_DEACTIVATED", "category", id.toString(), Map.of());
    }

    @Transactional
    public void delete(UUID id, HttpServletRequest httpRequest) {
        if (categories.hasProducts(id)) {
            throw new ConflictException("Category has products; deactivate it instead of deleting.");
        }
        categories.deleteById(id);
        audit.record(httpRequest, "CATEGORY_DELETED", "category", id.toString(), Map.of());
    }

    public Category requireBySlug(String slug) {
        return categories.findBySlug(slug).orElseThrow(() -> new NotFoundException("Category not found."));
    }
}
