package com.studyvault.admin;

import com.studyvault.common.api.ApiResponse;
import com.studyvault.common.api.PageResponse;
import com.studyvault.common.audit.AuditService;
import com.studyvault.common.error.NotFoundException;
import com.studyvault.orders.Order;
import com.studyvault.orders.OrderRepository;
import com.studyvault.orders.Payment;
import com.studyvault.orders.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final com.studyvault.orders.EntitlementRepository entitlements;
    private final AuditService audit;

    public record AdminOrderDto(String orderNumber, String userEmail, String status, long subtotalMinor,
                                long discountMinor, long totalMinor, String currency, String paymentStatus,
                                String providerPaymentId, String createdAt) {
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminOrderDto>> list(@RequestParam(required = false) Order.Status status,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> result = (status != null)
                ? orders.findAllByStatusOrderByCreatedAtDesc(status, pageable)
                : orders.findAllByOrderByCreatedAtDesc(pageable);
        return ApiResponse.ok(PageResponse.of(result, result.map(this::toDto).getContent()));
    }

    @GetMapping("/{orderNumber}")
    public ApiResponse<AdminOrderDto> detail(@PathVariable String orderNumber) {
        Order order = orders.findByOrderNumber(orderNumber).orElseThrow(() -> new NotFoundException("Order not found."));
        return ApiResponse.ok(toDto(order));
    }

    /**
     * Records a refund performed in the payment provider dashboard and revokes
     * access to the refunded products immediately.
     */
    @PostMapping("/{orderNumber}/refund")
    @Transactional
    public ApiResponse<Void> refund(@PathVariable String orderNumber, HttpServletRequest httpRequest) {
        Order order = orders.findWithItemsById(orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Order not found.")).getId())
                .orElseThrow(() -> new NotFoundException("Order not found."));
        if (order.getStatus() != Order.Status.PAID) {
            return ApiResponse.message("Only paid orders can be refunded.");
        }
        Payment payment = payments.findFirstByOrderId(order.getId())
                .orElseThrow(() -> new NotFoundException("No payment exists for this order."));
        Instant now = Instant.now();
        payment.setStatus(Payment.Status.REFUNDED);
        order.setStatus(Order.Status.REFUNDED);
        payments.save(payment);
        orders.save(order);
        // Revoke access to everything in the refunded order.
        for (var item : order.getItems()) {
            entitlements.findByUserIdAndProductIdAndRevokedAtIsNull(order.getUser().getId(), item.getProduct().getId())
                    .ifPresent(e -> e.setRevokedAt(now));
        }
        audit.record(httpRequest, "ORDER_REFUNDED", "order", order.getId().toString(),
                Map.of("orderNumber", orderNumber, "amountMinor", payment.getAmountMinor()));
        return ApiResponse.message("Order marked refunded. Access revoked.");
    }

    private AdminOrderDto toDto(Order order) {
        Payment payment = payments.findFirstByOrderId(order.getId()).orElse(null);
        return new AdminOrderDto(order.getOrderNumber(), order.getUser().getEmail(), order.getStatus().name(),
                order.getSubtotalMinor(), order.getDiscountMinor(), order.getTotalMinor(), order.getCurrency(),
                payment == null ? "-" : payment.getStatus().name(),
                payment == null ? null : payment.getProviderPaymentId(),
                order.getCreatedAt().toString());
    }
}
