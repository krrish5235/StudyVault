package com.studyvault.catalog;

import com.studyvault.common.audit.AuditService;
import com.studyvault.common.api.PageResponse;
import com.studyvault.common.error.BadRequestException;
import com.studyvault.common.error.ConflictException;
import com.studyvault.common.error.NotFoundException;
import com.studyvault.common.util.Slugs;
import com.studyvault.catalog.dto.AdminProductDto;
import com.studyvault.catalog.dto.CreateProductRequest;
import com.studyvault.catalog.dto.ProductDetailDto;
import com.studyvault.catalog.dto.ProductSummaryDto;
import com.studyvault.catalog.dto.UpdateProductRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int MAX_PAGE_SIZE = 50;
    /** Whitelisted sort keys - user input never reaches Sort directly. */
    private static final Map<String, Sort> SORTS = new LinkedHashMap<>(Map.of(
            "newest", Sort.by(Sort.Direction.DESC, "createdAt"),
            "price_asc", Sort.by(Sort.Direction.ASC, "priceMinor"),
            "price_desc", Sort.by(Sort.Direction.DESC, "priceMinor"),
            "popular", Sort.by(Sort.Direction.DESC, "viewCount"),
            "rating", Sort.by(Sort.Direction.DESC, "avgRating")));

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final TagRepository tags;
    private final AuditService audit;

    // ------------------------------------------------------------------ public

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryDto> search(String query, String categorySlug, String tagSlug,
                                                  Long minPriceMinor, Long maxPriceMinor, BigDecimal minRating,
                                                  String sort, int page, int size) {
        Specification<Product> spec = ProductSpecifications.hasStatus(Product.Status.PUBLISHED);
        if (query != null && !query.isBlank()) {
            spec = spec.and(ProductSpecifications.matchesQuery(query.trim()));
        }
        if (categorySlug != null && !categorySlug.isBlank()) {
            spec = spec.and(ProductSpecifications.inCategory(categorySlug));
        }
        if (tagSlug != null && !tagSlug.isBlank()) {
            spec = spec.and(ProductSpecifications.hasTag(tagSlug));
        }
        if (minPriceMinor != null) {
            spec = spec.and(ProductSpecifications.priceMin(minPriceMinor));
        }
        if (maxPriceMinor != null) {
            spec = spec.and(ProductSpecifications.priceMax(maxPriceMinor));
        }
        if (minRating != null) {
            spec = spec.and(ProductSpecifications.ratingAtLeast(minRating));
        }

        Sort sortOrder = SORTS.getOrDefault(sort == null ? "newest" : sort, SORTS.get("newest"));
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        Page<Product> result = products.findAll(spec, PageRequest.of(safePage, safeSize, sortOrder));
        return PageResponse.of(result, result.map(ProductSummaryDto::from).getContent());
    }

    @Transactional
    public ProductDetailDto getBySlug(String slug) {
        Product product = products.findWithRelationsBySlug(slug)
                .filter(p -> p.getStatus() == Product.Status.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Product not found."));
        products.incrementViewCount(product.getId());
        return ProductDetailDto.from(product);
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryDto> featured(int limit) {
        return products.findByStatusAndFeaturedTrueOrderByCreatedAtDesc(Product.Status.PUBLISHED).stream()
                .limit(Math.min(Math.max(1, limit), 12))
                .map(ProductSummaryDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryDto> bestsellers(int limit) {
        return products.findTop8ByStatusAndBestsellerTrueOrderByViewCountDesc(Product.Status.PUBLISHED).stream()
                .limit(Math.min(Math.max(1, limit), 12))
                .map(ProductSummaryDto::from)
                .toList();
    }

    // ------------------------------------------------------------------ admin

    @Transactional(readOnly = true)
    public PageResponse<AdminProductDto> adminList(Product.Status status, String query, int page, int size) {
        Specification<Product> spec = (status != null) ? ProductSpecifications.hasStatus(status)
                : (root, q, cb) -> cb.conjunction();
        if (query != null && !query.isBlank()) {
            spec = spec.and(ProductSpecifications.matchesQuery(query.trim()));
        }
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Page<Product> result = products.findAll(spec, PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(result, result.map(AdminProductDto::from).getContent());
    }

    @Transactional(readOnly = true)
    public AdminProductDto adminGet(UUID id) {
        return AdminProductDto.from(getWithRelations(id));
    }

    @Transactional
    public AdminProductDto adminCreate(CreateProductRequest request, HttpServletRequest httpRequest) {
        Category category = categories.findById(request.categoryId())
                .orElseThrow(() -> new BadRequestException("Selected category does not exist."));

        Product product = new Product();
        applyCreate(product, request, category);
        product.setSlug(generateUniqueSlug(request.title()));
        attachTags(product, request.tags());
        Product saved = products.saveAndFlush(product);

        audit.record(httpRequest, "PRODUCT_CREATED", "product", saved.getId().toString(),
                Map.of("title", saved.getTitle(), "priceMinor", saved.getPriceMinor()));
        return AdminProductDto.from(saved);
    }

    @Transactional
    public AdminProductDto adminUpdate(UUID id, UpdateProductRequest request, HttpServletRequest httpRequest) {
        Product product = getWithRelations(id);
        Map<String, Object> changes = new LinkedHashMap<>();

        if (request.title() != null && !request.title().equals(product.getTitle())) {
            changes.put("title", Map.of("from", product.getTitle(), "to", request.title()));
            product.setTitle(request.title().trim());
        }
        if (request.shortDescription() != null) {
            product.setShortDescription(request.shortDescription().trim());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
            product.setContentVersion(product.getContentVersion() + 1); // content changed → version bump
        }
        if (request.categoryId() != null && !request.categoryId().equals(product.getCategory().getId())) {
            Category category = categories.findById(request.categoryId())
                    .orElseThrow(() -> new BadRequestException("Selected category does not exist."));
            changes.put("category", category.getName());
            product.setCategory(category);
        }
        if (request.priceMinor() != null && !request.priceMinor().equals(product.getPriceMinor())) {
            changes.put("priceMinor", Map.of("from", product.getPriceMinor(), "to", request.priceMinor()));
            product.setPriceMinor(request.priceMinor());
        }
        if (request.comparePriceMinor() != null && product.getPriceMinor() != null
                && request.comparePriceMinor() < product.getPriceMinor()) {
            throw new BadRequestException("Compare-at price must be greater than or equal to the price.");
        }
        product.setComparePriceMinor(request.comparePriceMinor());
        if (request.featured() != null) {
            product.setFeatured(request.featured());
        }
        if (request.bestseller() != null) {
            product.setBestseller(request.bestseller());
        }
        if (request.tags() != null) {
            product.getTags().clear();
            attachTags(product, request.tags());
        }

        Product saved = products.saveAndFlush(product);
        audit.record(httpRequest, "PRODUCT_UPDATED", "product", id.toString(), changes);
        return AdminProductDto.from(saved);
    }

    @Transactional
    public AdminProductDto adminSetStatus(UUID id, Product.Status status, HttpServletRequest httpRequest) {
        Product product = getWithRelations(id);
        Product.Status old = product.getStatus();
        if (status == Product.Status.PUBLISHED && product.getPriceMinor() <= 0) {
            throw new BadRequestException("Set a price greater than zero before publishing.");
        }
        product.setStatus(status);
        Product saved = products.saveAndFlush(product);
        audit.record(httpRequest, "PRODUCT_STATUS_CHANGED", "product", id.toString(),
                Map.of("from", old.name(), "to", status.name()));
        return AdminProductDto.from(saved);
    }

    /**
     * Soft delete: products referenced by orders/entitlements must remain resolvable,
     * so deletion archives the product instead of destroying it.
     */
    @Transactional
    public void adminArchive(UUID id, HttpServletRequest httpRequest) {
        Product product = getWithRelations(id);
        product.setStatus(Product.Status.ARCHIVED);
        product.setFeatured(false);
        product.setBestseller(false);
        products.save(product);
        audit.record(httpRequest, "PRODUCT_ARCHIVED", "product", id.toString(), Map.of("title", product.getTitle()));
    }

    // ------------------------------------------------------------------ helpers

    private Product getWithRelations(UUID id) {
        return products.findWithRelationsById(id)
                .orElseThrow(() -> new NotFoundException("Product not found."));
    }

    private void applyCreate(Product product, CreateProductRequest request, Category category) {
        product.setTitle(request.title().trim());
        product.setShortDescription(request.shortDescription().trim());
        product.setDescription(request.description());
        product.setCategory(category);
        product.setPriceMinor(request.priceMinor());
        if (request.comparePriceMinor() != null && request.comparePriceMinor() < request.priceMinor()) {
            throw new BadRequestException("Compare-at price must be greater than or equal to the price.");
        }
        product.setComparePriceMinor(request.comparePriceMinor());
        product.setFeatured(request.featured());
        product.setBestseller(request.bestseller());
    }

    private void attachTags(Product product, List<String> tagNames) {
        if (tagNames == null) {
            return;
        }
        for (String raw : tagNames.stream().limit(10).toList()) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String name = raw.trim();
            if (name.length() > 60) {
                throw new BadRequestException("Tags must be at most 60 characters.");
            }
            String slug = Slugs.slugify(name);
            Tag tag = tags.findBySlug(slug).orElseGet(() -> {
                Tag t = new Tag();
                t.setName(name);
                t.setSlug(slug);
                return tags.save(t);
            });
            product.getTags().add(tag);
        }
    }

    private String generateUniqueSlug(String title) {
        String base = Slugs.slugify(title);
        String candidate = base;
        int suffix = 2;
        while (products.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
            if (suffix > 50) {
                throw new ConflictException("Could not generate a unique slug for this title.");
            }
        }
        return candidate;
    }
}
