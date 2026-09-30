package com.studyvault.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByProviderAndProviderOrderId(String provider, String providerOrderId);

    Optional<Payment> findByProviderAndProviderPaymentId(String provider, String providerPaymentId);

    @Query("select p from Payment p join fetch p.order where p.provider = :provider and p.providerOrderId = :providerOrderId")
    Optional<Payment> findWithOrder(@Param("provider") String provider, @Param("providerOrderId") String providerOrderId);

    Optional<Payment> findFirstByOrderId(UUID orderId);
}
