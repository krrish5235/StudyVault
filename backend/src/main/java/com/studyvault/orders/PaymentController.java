package com.studyvault.orders;

import com.studyvault.common.api.ApiResponse;
import com.studyvault.common.error.NotFoundException;
import com.studyvault.security.SecurityUtils;
import com.studyvault.security.SecurityUser;
import com.studyvault.user.User;
import com.studyvault.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;
    private final OrderRepository orders;
    private final UserRepository users;

    public record CreateOrderRequest(@NotEmpty List<UUID> productIds, String couponCode) {
    }

    public record VerifyRequest(@NotBlank String orderNumber, @NotBlank String razorpayOrderId,
                                @NotBlank String razorpayPaymentId, @NotBlank String razorpaySignature) {
    }

    public record OrderDto(String orderNumber, String status, long subtotalMinor, long discountMinor,
                           long totalMinor, String currency, String createdAt,
                           List<ItemDto> items) {
        public record ItemDto(String title, long priceMinor) {
        }
    }

    @PostMapping("/payments/orders")
    public ApiResponse<OrderService.CheckoutDto> createOrder(@Valid @RequestBody CreateOrderRequest request,
                                                             HttpServletRequest httpRequest) {
        User user = requireUser();
        return ApiResponse.ok(orderService.createCheckout(user, request.productIds(), request.couponCode(), httpRequest));
    }

    @PostMapping("/payments/verify")
    public ApiResponse<Void> verify(@Valid @RequestBody VerifyRequest request, HttpServletRequest httpRequest) {
        User user = requireUser();
        orderService.verifyAndGrant(user, request.orderNumber(), request.razorpayOrderId(),
                request.razorpayPaymentId(), request.razorpaySignature(), httpRequest);
        return ApiResponse.message("Payment verified. Your products are available in your library.");
    }

    @GetMapping("/orders")
    public ApiResponse<List<OrderDto>> myOrders() {
        SecurityUser su = SecurityUtils.requireUser();
        List<OrderDto> list = orders.findAllByUserIdOrderByCreatedAtDesc(su.id()).stream()
                .map(this::toDto)
                .toList();
        return ApiResponse.ok(list);
    }

    @GetMapping("/orders/{orderNumber}")
    public ApiResponse<OrderDto> order(@PathVariable String orderNumber) {
        SecurityUser su = SecurityUtils.requireUser();
        Order order = orders.findWithItemsByOrderNumberAndUser(orderNumber, su.id())
                .orElseThrow(() -> new NotFoundException("Order not found."));
        return ApiResponse.ok(toDto(order));
    }

    private OrderDto toDto(Order order) {
        return new OrderDto(order.getOrderNumber(), order.getStatus().name(),
                order.getSubtotalMinor(), order.getDiscountMinor(), order.getTotalMinor(),
                order.getCurrency(), order.getCreatedAt().toString(),
                order.getItems().stream().map(i -> new OrderDto.ItemDto(i.getTitleSnapshot(), i.getLineTotalMinor())).toList());
    }

    private User requireUser() {
        SecurityUser su = SecurityUtils.requireUser();
        return users.findById(su.id()).orElseThrow(() -> new NotFoundException("Account not found."));
    }
}
