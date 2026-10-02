package com.studyvault.orders;

import com.studyvault.catalog.Product;
import com.studyvault.catalog.ProductRepository;
import com.studyvault.common.audit.AuditService;
import com.studyvault.common.email.EmailService;
import com.studyvault.common.error.BadRequestException;
import com.studyvault.common.error.ConflictException;
import com.studyvault.common.error.NotFoundException;
import com.studyvault.common.error.PaymentException;
import com.studyvault.config.AppProperties;
import com.studyvault.user.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Checkout + fulfillment. Prices, discounts and totals are computed exclusively
 * from database values; access is granted only after server-side payment
 * verification, idempotently (safe under duplicate webhooks/retries).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int MAX_CART_ITEMS = 10;

    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final EntitlementRepository entitlements;
    private final CouponRepository coupons;
    private final CouponUsageRepository couponUsages;
    private final ProcessedWebhookEventRepository webhookEvents;
    private final ProductRepository products;
    private final PaymentGateway gateway;
    private final AppProperties props;
    private final AuditService audit;
    private final EmailService emails;

    private final SecureRandom random = new SecureRandom();

    // ------------------------------------------------------------------ checkout

    @Transactional
    public CheckoutDto createCheckout(User user, List<UUID> productIds, String couponCode,
                                      HttpServletRequest httpRequest) {
        if (productIds == null || productIds.isEmpty() || productIds.size() > MAX_CART_ITEMS) {
            throw new BadRequestException("Cart must contain between 1 and " + MAX_CART_ITEMS + " products.");
        }
        List<UUID> distinct = productIds.stream().distinct().toList();

        List<Product> items = distinct.stream()
                .map(id -> products.findWithRelationsById(id)
                        .filter(p -> p.getStatus() == Product.Status.PUBLISHED)
                        .orElseThrow(() -> new BadRequestException("Some products are no longer available.")))
                .toList();

        // Re-purchase protection.
        for (Product p : items) {
            if (entitlements.existsByUserIdAndProductIdAndRevokedAtIsNull(user.getId(), p.getId())) {
                throw new ConflictException("You already own \"" + p.getTitle() + "\".");
            }
        }

        long subtotal = items.stream().mapToLong(Product::getPriceMinor).sum();
        if (subtotal <= 0) {
            throw new BadRequestException("Cart total must be greater than zero.");
        }

        Coupon coupon = null;
        long discount = 0;
        if (couponCode != null && !couponCode.isBlank()) {
            coupon = validateCoupon(couponCode, user, subtotal, items);
            discount = computeDiscount(coupon, subtotal);
        }

        long total = subtotal - discount;

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setSubtotalMinor(subtotal);
        order.setDiscountMinor(discount);
        order.setTotalMinor(total);
        order.setCurrency("INR");
        order.setCoupon(coupon);
        for (Product p : items) {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(p);
            oi.setTitleSnapshot(p.getTitle());
            oi.setUnitPriceMinor(p.getPriceMinor());
            oi.setLineTotalMinor(p.getPriceMinor());
            order.getItems().add(oi);
        }
        Order saved = orders.saveAndFlush(order);

        PaymentGateway.GatewayOrder gatewayOrder = gateway.createOrder(total, saved.getCurrency(), saved.getOrderNumber());
        Payment payment = new Payment();
        payment.setOrder(saved);
        payment.setProvider(gateway.provider());
        payment.setProviderOrderId(gatewayOrder.id());
        payment.setAmountMinor(total);
        payment.setCurrency(saved.getCurrency());
        payment.setStatus(Payment.Status.CREATED);
        payments.save(payment);

        audit.record(httpRequest, "ORDER_CREATED", "order", saved.getId().toString(),
                Map.of("orderNumber", saved.getOrderNumber(), "totalMinor", total,
                        "items", items.size(), "coupon", coupon != null ? coupon.getCode() : ""));

        return new CheckoutDto(
                saved.getOrderNumber(),
                gatewayOrder.id(),
                gatewayOrder.keyIdForClient(),
                gateway.provider(),
                total,
                saved.getCurrency(),
                items.stream().map(i -> new CheckoutDto.Item(i.getId(), i.getTitle(), i.getPriceMinor())).toList());
    }

    // ------------------------------------------------------------------ verification

    @Transactional
    public void verifyAndGrant(User user, String orderNumber, String providerOrderId,
                               String providerPaymentId, String signature, HttpServletRequest httpRequest) {
        Order order = orders.findWithItemsByOrderNumberAndUser(orderNumber, user.getId())
                .orElseThrow(() -> new NotFoundException("Order not found."));
        if (order.getStatus() != Order.Status.PENDING) {
            if (order.getStatus() == Order.Status.PAID) {
                return; // idempotent retry
            }
            throw new ConflictException("This order can no longer be paid.");
        }

        Payment payment = payments.findByProviderAndProviderOrderId(gateway.provider(), providerOrderId)
                .orElseThrow(() -> new NotFoundException("Payment for this order was not found."));
        if (!payment.getOrder().getId().equals(order.getId())) {
            throw new NotFoundException("Payment for this order was not found.");
        }

        PaymentGateway.PaymentVerification result = gateway.verify(
                providerOrderId, providerPaymentId, signature, payment.getAmountMinor(), payment.getCurrency());

        if (!result.success()) {
            payment.setStatus(Payment.Status.FAILED);
            payment.setFailureReason(result.failureReason());
            payment.setProviderPaymentId(providerPaymentId);
            order.setStatus(Order.Status.FAILED);
            audit.record(httpRequest, "PAYMENT_VERIFICATION_FAILED", "order", order.getId().toString(),
                    Map.of("reason", result.failureReason() == null ? "" : result.failureReason()));
            throw new PaymentException("Payment could not be verified. If you were charged, contact support.");
        }

        payment.setProviderPaymentId(providerPaymentId);
        markCapturedAndGrant(payment, httpRequest);
    }

    /**
     * Idempotent capture: an atomic status transition guards against duplicate
     * webhook deliveries and concurrent verifications.
     */
    @Transactional
    public void markCapturedAndGrant(Payment payment, HttpServletRequest httpRequest) {
        int updated = orders.markPaidIfPending(payment.getOrder().getId());
        if (updated == 0) {
            return; // already processed
        }

        Order order = payment.getOrder();
        payment.setStatus(Payment.Status.CAPTURED);
        payments.save(payment);

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (!entitlements.existsByUserIdAndProductIdAndRevokedAtIsNull(order.getUser().getId(), product.getId())) {
                Entitlement e = new Entitlement();
                e.setUser(order.getUser());
                e.setProduct(product);
                e.setOrder(order);
                entitlements.save(e);
            }
        }

        if (order.getCoupon() != null) {
            Coupon coupon = order.getCoupon();
            if (couponUsages.countByCouponAndUser(coupon.getId(), order.getUser().getId()) == 0) {
                CouponUsage usage = new CouponUsage();
                usage.setCoupon(coupon);
                usage.setUser(order.getUser());
                usage.setOrder(order);
                couponUsages.save(usage);
                coupons.incrementUsedCount(coupon.getId());
            }
        }

        audit.record(httpRequest, "ORDER_PAID", "order", order.getId().toString(),
                Map.of("orderNumber", order.getOrderNumber(), "totalMinor", order.getTotalMinor()));

        emails.sendOrderConfirmation(order.getUser().getEmail(), order.getUser().getFullName(),
                order.getOrderNumber(), order.getTotalMinor(),
                order.getItems().stream()
                        .map(i -> new EmailService.OrderLine(i.getTitleSnapshot(), i.getLineTotalMinor()))
                        .toList());
    }

    // ------------------------------------------------------------------ webhooks

    @Transactional
    public void handleWebhook(String eventId, String eventType, String providerPaymentId,
                              String providerOrderId, HttpServletRequest httpRequest) {
        if (webhookEvents.existsByProviderAndEventId(gateway.provider(), eventId)) {
            log.info("Webhook {} already processed - ignoring duplicate delivery", eventId);
            return;
        }

        switch (eventType) {
            case "payment.captured", "order.paid" -> payments
                    .findWithOrder(gateway.provider(), providerOrderId)
                    .ifPresent(payment -> markCapturedAndGrant(payment, httpRequest));
            case "payment.failed" -> payments.findWithOrder(gateway.provider(), providerOrderId)
                    .filter(p -> p.getStatus() == Payment.Status.CREATED)
                    .ifPresent(p -> {
                        p.setStatus(Payment.Status.FAILED);
                        p.getOrder().setStatus(Order.Status.FAILED);
                        payments.save(p);
                        orders.save(p.getOrder());
                    });
            case "refund.processed" -> payments
                    .findByProviderAndProviderPaymentId(gateway.provider(), providerPaymentId)
                    .ifPresent(this::markRefunded);
            default -> log.debug("Ignoring webhook event type {}", eventType);
        }

        ProcessedWebhookEvent processed = new ProcessedWebhookEvent();
        processed.setProvider(gateway.provider());
        processed.setEventId(eventId);
        webhookEvents.save(processed);
    }

    // ------------------------------------------------------------------ refunds (admin)

    @Transactional
    public void markRefunded(Payment payment) {
        Order order = payment.getOrder();
        payment.setStatus(Payment.Status.REFUNDED);
        order.setStatus(Order.Status.REFUNDED);
        payments.save(payment);
        orders.save(order);
        // Revoke access to refunded products.
        for (OrderItem item : order.getItems()) {
            entitlements.findAllByUserIdAndRevokedAtIsNullOrderByGrantedAtDesc(order.getUser().getId()).stream()
                    .filter(e -> e.getProduct().getId().equals(item.getProduct().getId()))
                    .forEach(e -> e.setRevokedAt(Instant.now()));
        }
        audit.record(null, "ORDER_REFUNDED", "order", order.getId().toString(),
                Map.of("orderNumber", order.getOrderNumber(), "amountMinor", payment.getAmountMinor()));
    }

    // ------------------------------------------------------------------ coupons

    private Coupon validateCoupon(String code, User user, long subtotal, List<Product> items) {
        Coupon coupon = coupons.findByCode(code.trim().toUpperCase());
        Instant now = Instant.now();
        if (coupon == null || !coupon.isActive() || !coupon.isWithinWindow(now)) {
            throw new BadRequestException("This coupon is not valid.");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new BadRequestException("This coupon has been fully redeemed.");
        }
        if (couponUsages.countByCouponAndUser(coupon.getId(), user.getId()) >= coupon.getPerUserLimit()) {
            throw new BadRequestException("You have already used this coupon.");
        }
        if (subtotal < coupon.getMinOrderMinor()) {
            throw new BadRequestException("Order does not meet the coupon minimum.");
        }
        boolean productRestricted = !coupon.getRestrictedProducts().isEmpty();
        boolean categoryRestricted = coupon.getCategory() != null;
        if (productRestricted) {
            boolean allMatch = items.stream()
                    .allMatch(p -> coupon.getRestrictedProducts().stream().anyMatch(r -> r.getId().equals(p.getId())));
            if (!allMatch) {
                throw new BadRequestException("This coupon applies only to selected products.");
            }
        } else if (categoryRestricted) {
            boolean allMatch = items.stream()
                    .allMatch(p -> p.getCategory().getId().equals(coupon.getCategory().getId()));
            if (!allMatch) {
                throw new BadRequestException("This coupon applies only to the " + coupon.getCategory().getName() + " category.");
            }
        }
        return coupon;
    }

    private long computeDiscount(Coupon coupon, long subtotal) {
        long discount;
        if (coupon.getDiscountType() == Coupon.DiscountType.PERCENT) {
            discount = Math.floorDiv(subtotal * coupon.getPercentOff(), 100);
            if (coupon.getMaxDiscountMinor() != null) {
                discount = Math.min(discount, coupon.getMaxDiscountMinor());
            }
        } else {
            discount = coupon.getAmountOffMinor();
        }
        return Math.min(discount, subtotal);
    }

    private String generateOrderNumber() {
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        StringBuilder suffix = new StringBuilder();
        String alphabet = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        for (int i = 0; i < 6; i++) {
            suffix.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return "SV-" + date + "-" + suffix;
    }

    // ------------------------------------------------------------------ DTO

    public record CheckoutDto(String orderNumber, String providerOrderId, String providerKeyId,
                              String provider, long amountMinor, String currency, List<Item> items) {
        public record Item(UUID productId, String title, long priceMinor) {
        }
    }
}
