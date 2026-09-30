package com.studyvault.catalog;

import com.studyvault.common.api.ApiResponse;
import com.studyvault.common.api.PageResponse;
import com.studyvault.catalog.dto.AdminProductDto;
import com.studyvault.catalog.dto.CategoryDto;
import com.studyvault.catalog.dto.CreateCategoryRequest;
import com.studyvault.catalog.dto.CreateProductRequest;
import com.studyvault.catalog.dto.UpdateProductRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Admin catalog management. RBAC enforced at the backend (hasRole ADMIN). */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {

    private final ProductService productService;
    private final CategoryService categoryService;

    // ------------------------------------------------------------ products

    @GetMapping("/products")
    public ApiResponse<PageResponse<AdminProductDto>> products(
            @RequestParam(required = false) Product.Status status,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(productService.adminList(status, query, page, size));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<AdminProductDto> product(@PathVariable UUID id) {
        return ApiResponse.ok(productService.adminGet(id));
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminProductDto> createProduct(@Valid @RequestBody CreateProductRequest request,
                                                      HttpServletRequest httpRequest) {
        return ApiResponse.ok(productService.adminCreate(request, httpRequest), "Product created as draft.");
    }

    @PatchMapping("/products/{id}")
    public ApiResponse<AdminProductDto> updateProduct(@PathVariable UUID id,
                                                      @Valid @RequestBody UpdateProductRequest request,
                                                      HttpServletRequest httpRequest) {
        return ApiResponse.ok(productService.adminUpdate(id, request, httpRequest));
    }

    @PostMapping("/products/{id}/publish")
    public ApiResponse<AdminProductDto> publish(@PathVariable UUID id, HttpServletRequest httpRequest) {
        return ApiResponse.ok(productService.adminSetStatus(id, Product.Status.PUBLISHED, httpRequest),
                "Product is now live.");
    }

    @PostMapping("/products/{id}/unpublish")
    public ApiResponse<AdminProductDto> unpublish(@PathVariable UUID id, HttpServletRequest httpRequest) {
        return ApiResponse.ok(productService.adminSetStatus(id, Product.Status.DRAFT, httpRequest),
                "Product moved back to draft.");
    }

    /** Destructive operations are soft: archive preserves order/entitlement history. */
    @DeleteMapping("/products/{id}")
    public ApiResponse<Void> archive(@PathVariable UUID id, HttpServletRequest httpRequest) {
        productService.adminArchive(id, httpRequest);
        return ApiResponse.message("Product archived.");
    }

    // ------------------------------------------------------------ categories

    @GetMapping("/categories")
    public ApiResponse<List<CategoryDto>> categories() {
        return ApiResponse.ok(categoryService.listAdmin());
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryDto> createCategory(@Valid @RequestBody CreateCategoryRequest request,
                                                   HttpServletRequest httpRequest) {
        return ApiResponse.ok(categoryService.create(request, httpRequest));
    }

    @PatchMapping("/categories/{id}")
    public ApiResponse<CategoryDto> updateCategory(@PathVariable UUID id,
                                                   @Valid @RequestBody CreateCategoryRequest request,
                                                   HttpServletRequest httpRequest) {
        return ApiResponse.ok(categoryService.update(id, request, httpRequest));
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deactivateCategory(@PathVariable UUID id, HttpServletRequest httpRequest) {
        categoryService.deactivate(id, httpRequest);
        return ApiResponse.message("Category deactivated.");
    }
}
