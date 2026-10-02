package com.studyvault.admin;

import com.studyvault.common.api.ApiResponse;
import com.studyvault.common.api.PageResponse;
import com.studyvault.common.audit.AuditService;
import com.studyvault.common.error.ConflictException;
import com.studyvault.common.error.NotFoundException;
import com.studyvault.orders.Order;
import com.studyvault.orders.OrderRepository;
import com.studyvault.orders.Payment;
import com.studyvault.orders.PaymentRepository;
import com.studyvault.orders.PaymentRequest;
import com.studyvault.orders.PaymentRequestRepository;
import com.studyvault.orders.OrderService;
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
import java.util.UUID;

/** Admin queue for manual UPI payment claims. Approve = grant access, Reject = ask buyer to resubmit. */
@RestController
@RequestMapping("/api/v1/admin/payment-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentRequestController {

    private final PaymentRequestRepository requests;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final OrderService orderService;
    private final com.studyvault.user.UserRepository users;
    private final AuditService audit;

    public record AdminRequestDto(UUID id, String orderNumber, String userEmail, long totalMinor,
                                  String currency, String transactionRef, String status,
                                  String submittedAt, String reviewedAt, String adminNote) {
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminRequestDto>> list(@RequestParam(required = false) PaymentRequest.Status status,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PaymentRequest> result = (status != null)
                ? requests.findAllByStatusOrderByCreatedAtDesc(status, pageable)
                : requests.findAll(pageable);
        return ApiResponse.ok(PageResponse.of(result, result.map(this::toDto).getContent()));
    }

    /** Approves the claim: order -> PAID, entitlements granted, confirmation email sent. */
    @PostMapping("/{id}/approve")
    @Transactional
    public ApiResponse<Void> approve(@PathVariable UUID id, HttpServletRequest httpRequest) {
        PaymentRequest request = requests.findById(id).orElseThrow(() -> new NotFoundException("Payment request not found."));
        if (request.getStatus() != PaymentRequest.Status.PENDING) {
            throw new ConflictException("This request was already reviewed.");
        }
        Order order = orders.findWithItemsById(request.getOrder().getId())
                .orElseThrow(() -> new NotFoundException("Order not found."));
        Payment payment = payments.findFirstByOrderId(order.getId())
                .orElseThrow(() -> new NotFoundException("No payment record exists for this order."));

        orderService.markCapturedAndGrant(payment, httpRequest); // idempotent atomic grant + email

        request.setStatus(PaymentRequest.Status.APPROVED);
        request.setReviewedAt(Instant.now());
        request.setReviewedBy(com.studyvault.security.SecurityUtils.currentUser()
                .map(u -> users.getReferenceById(u.id())).orElse(null));
        requests.save(request);
        audit.record(httpRequest, "UPI_CLAIM_APPROVED", "order", order.getId().toString(),
                Map.of("orderNumber", order.getOrderNumber(), "utr", request.getTransactionRef()));
        return ApiResponse.message("Payment approved. The buyer now has access.");
    }

    /** Rejects the claim; the buyer can submit a corrected transaction reference. */
    @PostMapping("/{id}/reject")
    @Transactional
    public ApiResponse<Void> reject(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> body,
                                    HttpServletRequest httpRequest) {
        PaymentRequest request = requests.findById(id).orElseThrow(() -> new NotFoundException("Payment request not found."));
        if (request.getStatus() != PaymentRequest.Status.PENDING) {
            throw new ConflictException("This request was already reviewed.");
        }
        request.setStatus(PaymentRequest.Status.REJECTED);
        request.setAdminNote(body == null ? null : body.get("note"));
        request.setReviewedAt(Instant.now());
        request.setReviewedBy(com.studyvault.security.SecurityUtils.currentUser()
                .map(u -> users.getReferenceById(u.id())).orElse(null));
        requests.save(request);
        audit.record(httpRequest, "UPI_CLAIM_REJECTED", "order", request.getOrder().getId().toString(),
                Map.of("orderNumber", request.getOrder().getOrderNumber(), "utr", request.getTransactionRef()));
        return ApiResponse.message("Payment request rejected. The buyer can submit a corrected transaction ID.");
    }

    private AdminRequestDto toDto(PaymentRequest r) {
        return new AdminRequestDto(r.getId(), r.getOrder().getOrderNumber(), r.getUser().getEmail(),
                r.getOrder().getTotalMinor(), r.getOrder().getCurrency(), r.getTransactionRef(),
                r.getStatus().name(), r.getCreatedAt().toString(),
                r.getReviewedAt() == null ? null : r.getReviewedAt().toString(), r.getAdminNote());
    }
}
