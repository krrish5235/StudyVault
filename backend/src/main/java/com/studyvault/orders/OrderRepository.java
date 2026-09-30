package com.studyvault.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    org.springframework.data.domain.Page<Order> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Order> findAllByStatusOrderByCreatedAtDesc(Order.Status status,
                                                                                     org.springframework.data.domain.Pageable pageable);

    @Query("select o from Order o join fetch o.items where o.orderNumber = :orderNumber and o.user.id = :userId")
    Optional<Order> findWithItemsByOrderNumberAndUser(@Param("orderNumber") String orderNumber, @Param("userId") UUID userId);

    /** Atomic guard for idempotent capture - exactly one concurrent caller wins. */
    @Modifying
    @Query("update Order o set o.status = 'PAID' where o.id = :orderId and o.status = 'PENDING'")
    int markPaidIfPending(@Param("orderId") UUID orderId);

    @Query("select o from Order o join fetch o.items where o.id = :id")
    Optional<Order> findWithItemsById(@Param("id") UUID id);
}
