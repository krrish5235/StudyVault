package com.studyvault.orders;

import com.studyvault.catalog.Category;
import com.studyvault.catalog.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Coupon {

    public enum DiscountType { PERCENT, FIXED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Stored uppercase; validation is always server-side. */
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 10)
    private DiscountType discountType;

    @Column(name = "percent_off")
    private Integer percentOff;

    @Column(name = "amount_off_minor")
    private Long amountOffMinor;

    @Column(name = "min_order_minor", nullable = false)
    private long minOrderMinor = 0;

    @Column(name = "max_discount_minor")
    private Long maxDiscountMinor;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    private int usedCount = 0;

    @Column(name = "per_user_limit", nullable = false)
    private int perUserLimit = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /** Empty = applies to the whole catalog; otherwise every cart item must match. */
    @ManyToMany
    @JoinTable(name = "coupon_products",
            joinColumns = @JoinColumn(name = "coupon_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id"))
    private Set<Product> restrictedProducts = new HashSet<>();

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isWithinWindow(Instant now) {
        return (startsAt == null || !startsAt.isAfter(now)) && (expiresAt == null || expiresAt.isAfter(now));
    }
}
