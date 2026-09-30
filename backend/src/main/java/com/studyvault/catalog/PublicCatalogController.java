package com.studyvault.catalog;

import com.studyvault.common.api.ApiResponse;
import com.studyvault.common.api.PageResponse;
import com.studyvault.catalog.dto.CategoryDto;
import com.studyvault.catalog.dto.ProductDetailDto;
import com.studyvault.catalog.dto.ProductSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/** Public storefront catalog. Read-only; only PUBLISHED products are ever returned. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PublicCatalogController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping("/categories")
    public ApiResponse<List<CategoryDto>> categories() {
        return ApiResponse.ok(categoryService.listPublic());
    }

    @GetMapping("/products")
    public ApiResponse<PageResponse<ProductSummaryDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.ok(productService.search(query, category, tag, minPrice, maxPrice, minRating, sort, page, size));
    }

    @GetMapping("/products/featured")
    public ApiResponse<List<ProductSummaryDto>> featured(@RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.ok(productService.featured(limit));
    }

    @GetMapping("/products/bestsellers")
    public ApiResponse<List<ProductSummaryDto>> bestsellers(@RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.ok(productService.bestsellers(limit));
    }

    @GetMapping("/products/{slug}")
    public ApiResponse<ProductDetailDto> detail(@PathVariable String slug) {
        return ApiResponse.ok(productService.getBySlug(slug));
    }
}
