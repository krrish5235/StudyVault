package com.studyvault.reviews;

import com.studyvault.common.api.ApiResponse;
import com.studyvault.catalog.dto.ProductSummaryDto;
import com.studyvault.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ApiResponse<List<ProductSummaryDto>> list() {
        return ApiResponse.ok(wishlistService.list(SecurityUtils.requireUser().id()));
    }

    @PostMapping("/{productId}")
    public ApiResponse<Void> add(@PathVariable UUID productId) {
        wishlistService.add(SecurityUtils.requireUser().id(), productId);
        return ApiResponse.message("Added to wishlist.");
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> remove(@PathVariable UUID productId) {
        wishlistService.remove(SecurityUtils.requireUser().id(), productId);
        return ApiResponse.message("Removed from wishlist.");
    }
}
