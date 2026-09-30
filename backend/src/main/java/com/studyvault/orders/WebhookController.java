package com.studyvault.orders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Provider webhook endpoint. Authentication is by HMAC signature over the raw
 * body (never by session cookies); processing is idempotent via event-id
 * de-duplication. CSRF is exempted for this path in the security filter.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentGateway gateway;
    private final OrderService orderService;
    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/{provider}")
    public ResponseEntity<Map<String, String>> receive(@PathVariable String provider,
                                                       @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
                                                       @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventIdHeader,
                                                       @RequestBody String rawBody) {
        if (!provider.equals(gateway.provider())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown provider"));
        }
        if (!gateway.verifyWebhookSignature(rawBody, signature)) {
            log.warn("Webhook signature verification FAILED for provider {}", provider);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid signature"));
        }

        try {
            JsonNode body = mapper.readTree(rawBody);
            String event = body.path("event").asText("");
            JsonNode payment = body.path("payload").path("payment").path("entity");
            JsonNode refund = body.path("payload").path("refund").path("entity");
            String paymentId = payment.path("id").asText(refund.path("payment_id").asText(""));
            String orderId = payment.path("order_id").asText("");
            String eventId = eventIdHeader != null && !eventIdHeader.isBlank()
                    ? eventIdHeader
                    : "sha256-" + com.studyvault.user.TokenService.sha256Hex(rawBody).substring(0, 32);

            orderService.handleWebhook(eventId, event, paymentId, orderId, null);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            log.error("Webhook processing error: {}", e.getMessage());
            // 200 anyway: signature was valid; malformed events must not trigger provider retries forever.
            return ResponseEntity.ok(Map.of("status", "accepted"));
        }
    }
}
