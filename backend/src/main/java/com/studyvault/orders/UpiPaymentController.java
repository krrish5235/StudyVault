package com.studyvault.orders;

import com.studyvault.common.api.ApiResponse;
import com.studyvault.common.error.BadRequestException;
import com.studyvault.common.error.ConflictException;
import com.studyvault.common.error.NotFoundException;
import com.studyvault.security.SecurityUtils;
import com.studyvault.security.SecurityUser;
import com.studyvault.user.User;
import com.studyvault.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Buyer side of the manual UPI flow: submit a UTR claim, check its status. */
@RestController
@RequestMapping("/api/v1/payments/upi")
@RequiredArgsConstructor
public class UpiPaymentController {

    private final OrderRepository orders;
    private final PaymentRequestRepository paymentRequests;
    private final PaymentRepository payments;
    private final UserRepository users;

    public record ClaimRequest(@NotBlank String orderNumber,
                               @NotBlank @Size(min = 6, max = 100)
                               @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "Transaction ID may contain letters, digits and dashes only")
                               String transactionRef) {
    }

    @PostMapping("/claim")
    @Transactional
    public ApiResponse<Map<String, Object>> claim(@Valid @RequestBody ClaimRequest request,
                                                  HttpServletRequest httpRequest) {
        SecurityUser su = SecurityUtils.requireUser();
        Order order = orders.findWithItemsByOrderNumberAndUser(request.orderNumber(), su.id())
                .orElseThrow(() -> new NotFoundException("Order not found."));
        if (order.getStatus() != Order.Status.PENDING) {
            throw new ConflictException("This order can no longer be paid.");
        }
        String ref = request.transactionRef().trim().toUpperCase();
        if (paymentRequests.existsByTransactionRef(ref)) {
            throw new ConflictException("This transaction ID has already been submitted.");
        }
        // one open claim per order
        if (paymentRequests.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), PaymentRequest.Status.PENDING).isPresent()) {
            throw new ConflictException("A payment verification is already pending for this order.");
        }

        PaymentRequest claim = new PaymentRequest();
        claim.setOrder(order);
        claim.setUser(users.getReferenceById(su.id()));
        claim.setTransactionRef(ref);
        PaymentRequest saved = paymentRequests.save(claim);
        return ApiResponse.ok(Map.of(
                        "claimId", saved.getId().toString(),
                        "status", saved.getStatus().name(),
                        "orderNumber", order.getOrderNumber()),
                "Payment details submitted. You will get access once the payment is verified (usually within 30 minutes).");
    }

    @GetMapping("/claim/{orderNumber}")
    public ApiResponse<Map<String, Object>> claimStatus(@PathVariable String orderNumber) {
        SecurityUser su = SecurityUtils.requireUser();
        Order order = orders.findByOrderNumber(orderNumber)
                .filter(o -> o.getUser().getId().equals(su.id()))
                .orElseThrow(() -> new NotFoundException("Order not found."));
        Map<String, Object> claim = paymentRequests
                .findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), PaymentRequest.Status.PENDING)
                .<Map<String, Object>>map(r -> Map.of("status", "PENDING", "submittedAt", r.getCreatedAt().toString()))
                .orElseGet(() -> paymentRequests
                        .findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), PaymentRequest.Status.APPROVED)
                        .<Map<String, Object>>map(r -> Map.of("status", "APPROVED", "submittedAt", r.getCreatedAt().toString()))
                        .orElse(null));
        return ApiResponse.ok(Map.of(
                "orderNumber", orderNumber,
                "orderStatus", order.getStatus().name(),
                "claim", claim));
    }
}
