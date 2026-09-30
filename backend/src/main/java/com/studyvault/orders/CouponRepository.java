package com.studyvault.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    @Query("select c from Coupon c left join fetch c.restrictedProducts where upper(c.code) = upper(:code)")
    Coupon findByCode(@Param("code") String code);

    boolean existsByCodeIgnoreCase(String code);

    @Modifying
    @Query("update Coupon c set c.usedCount = c.usedCount + 1 where c.id = :id")
    void incrementUsedCount(@Param("id") UUID id);
}
