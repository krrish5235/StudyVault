package com.studyvault.orders;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, UUID> {

    boolean existsByTransactionRef(String transactionRef);

    Optional<PaymentRequest> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(UUID orderId, PaymentRequest.Status status);

    @EntityGraph(attributePaths = {"order", "order.user"})
    Page<PaymentRequest> findAllByStatusOrderByCreatedAtDesc(PaymentRequest.Status status, Pageable pageable);

    @EntityGraph(attributePaths = {"order", "order.user", "user"})
    Page<PaymentRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
