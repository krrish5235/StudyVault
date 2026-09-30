package com.studyvault.orders;

import com.studyvault.catalog.Category;
import com.studyvault.catalog.CategoryRepository;
import com.studyvault.catalog.Product;
import com.studyvault.catalog.ProductRepository;
import com.studyvault.common.audit.AuditService;
import com.studyvault.common.error.BadRequestException;
import com.studyvault.common.error.ConflictException;
import com.studyvault.common.error.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {

    private final CouponRepository coupons;
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final AuditService audit;

    public record CreateCouponRequest(@NotBlank @Size(max = 40) String code,
                                      @NotBlank String discountType,
                                      @Min(1) @Max(100) Integer percentOff,
                                      @Min(1) Long amountOffMinor,
                                      @Min(0) Long minOrderMinor,
                                      @Min(1) Long maxDiscountMinor,
                                      Instant startsAt, Instant expiresAt,
                                      @Min(1) Integer usageLimit,
                                      @Min(1) Integer perUserLimit,
                                      UUID categoryId,
                                      List<UUID> productIds) {
    }

    public record CouponDto(UUID id, String code, String discountType, Integer percentOff, Long amountOffMinor,
                            Long minOrderMinor, Long maxDiscountMinor, Instant startsAt, Instant expiresAt,
                            Integer usageLimit, int usedCount, int perUserLimit, boolean active,
                            String categoryName, List<String> productTitles) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public com.studyvault.common.api.ApiResponse<com.studyvault.common.api.PageResponse<CouponDto>> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Page<Coupon> result = coupons.findAll(PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "createdAt")));
        return com.studyvault.common.api.ApiResponse.ok(com.studyvault.common.api.PageResponse.of(result.map(this::toDto)));
    }

    @PostMapping
    @Transactional
    public CouponDto create(@Valid @RequestBody CreateCouponRequest request, HttpServletRequest httpRequest) {
        String code = request.code().trim().toUpperCase();
        if (coupons.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("A coupon with this code already exists.");
        }
        Coupon coupon = new Coupon();
        apply(coupon, request, code);
        Coupon saved = coupons.saveAndFlush(coupon);
        audit.record(httpRequest, "COUPON_CREATED", "coupon", saved.getId().toString(), Map.of("code", code));
        return toDto(saved);
    }

    @PatchMapping("/{id}")
    @Transactional
    public CouponDto update(@PathVariable UUID id, @Valid @RequestBody CreateCouponRequest request,
                            HttpServletRequest httpRequest) {
        Coupon coupon = coupons.findById(id).orElseThrow(() -> new NotFoundException("Coupon not found."));
        apply(coupon, request, request.code().trim().toUpperCase());
        Coupon saved = coupons.saveAndFlush(coupon);
        audit.record(httpRequest, "COUPON_UPDATED", "coupon", id.toString(), Map.of("code", saved.getCode()));
        return toDto(saved);
    }

    @PatchMapping("/{id}/active")
    @Transactional
    public CouponDto setActive(@PathVariable UUID id, @RequestBody Map<String, Boolean> body,
                               HttpServletRequest httpRequest) {
        Coupon coupon = coupons.findById(id).orElseThrow(() -> new NotFoundException("Coupon not found."));
        Boolean active = body.get("active");
        if (active != null) {
            coupon.setActive(active);
            coupons.saveAndFlush(coupon);
            audit.record(httpRequest, active ? "COUPON_ACTIVATED" : "COUPON_DEACTIVATED",
                    "coupon", id.toString(), Map.of("code", coupon.getCode()));
        }
        return toDto(coupon);
    }

    private void apply(Coupon coupon, CreateCouponRequest request, String code) {
        coupon.setCode(code);
        Coupon.DiscountType type;
        try {
            type = Coupon.DiscountType.valueOf(request.discountType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("discountType must be PERCENT or FIXED.");
        }
        coupon.setDiscountType(type);
        coupon.setPercentOff(type == Coupon.DiscountType.PERCENT ? request.percentOff() : null);
        coupon.setAmountOffMinor(type == Coupon.DiscountType.FIXED ? request.amountOffMinor() : null);
        coupon.setMinOrderMinor(request.minOrderMinor() == null ? 0 : request.minOrderMinor());
        coupon.setMaxDiscountMinor(request.maxDiscountMinor());
        coupon.setStartsAt(request.startsAt());
        coupon.setExpiresAt(request.expiresAt());
        coupon.setUsageLimit(request.usageLimit());
        coupon.setPerUserLimit(request.perUserLimit() == null ? 1 : request.perUserLimit());
        if (request.categoryId() != null) {
            coupon.setCategory(categories.findById(request.categoryId())
                    .orElseThrow(() -> new BadRequestException("Category not found.")));
        } else {
            coupon.setCategory(null);
        }
        coupon.getRestrictedProducts().clear();
        if (request.productIds() != null) {
            request.productIds().stream().limit(50)
                    .forEach(pid -> coupon.getRestrictedProducts().add(products.getReferenceById(pid)));
        }
        coupon.setActive(true);
    }

    private CouponDto toDto(Coupon c) {
        return new CouponDto(c.getId(), c.getCode(), c.getDiscountType().name(), c.getPercentOff(),
                c.getAmountOffMinor(), c.getMinOrderMinor(), c.getMaxDiscountMinor(), c.getStartsAt(),
                c.getExpiresAt(), c.getUsageLimit(), c.getUsedCount(), c.getPerUserLimit(), c.isActive(),
                c.getCategory() != null ? c.getCategory().getName() : null,
                c.getRestrictedProducts().stream().map(Product::getTitle).toList());
    }
}
