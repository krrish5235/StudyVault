package com.studyvault.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, UUID> {

    @Query("select count(cu) from CouponUsage cu where cu.coupon.id = :couponId and cu.user.id = :userId")
    long countByCouponAndUser(@Param("couponId") UUID couponId, @Param("userId") UUID userId);
}
