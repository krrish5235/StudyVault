package com.studyvault.orders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyvault.common.error.PaymentException;
import com.studyvault.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Razorpay integration (orders API + checkout signature + webhook HMAC).
 * Uses a thin REST client instead of the legacy Java SDK to keep dependencies
 * controlled and the verification logic explicit.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.payments.provider", havingValue = "razorpay")
public class RazorpayGateway implements PaymentGateway {

    private static final String API_BASE = "https://api.razorpay.com/v1";

    private final AppProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private org.springframework.web.client.RestClient client;

    public RazorpayGateway(AppProperties props) {
        this.props = props;
    }

    @jakarta.annotation.PostConstruct
    void init() {
        var keyId = props.getRazorpay().getKeyId();
        var keySecret = props.getRazorpay().getKeySecret();
        this.client = org.springframework.web.client.RestClient.builder()
                .baseUrl(API_BASE)
                .defaultHeaders(h -> h.setBasicAuth(keyId, keySecret))
                .build();
    }

    @Override
    public String provider() {
        return "razorpay";
    }

    @Override
    public GatewayOrder createOrder(long amountMinor, String currency, String receipt) {
        try {
            String body = mapper.writeValueAsString(java.util.Map.of(
                    "amount", amountMinor,
                    "currency", currency,
                    "receipt", receipt));
            JsonNode response = client.post()
                    .uri("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || response.path("id").asText("").isBlank()) {
                throw new PaymentException("Gateway did not return an order id");
            }
            return new GatewayOrder(response.get("id").asText(), props.getRazorpay().getKeyId());
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Razorpay createOrder failed: {}", e.getMessage());
            throw new PaymentException("Could not create the payment order. Please try again.");
        }
    }

    @Override
    public PaymentVerification verify(String providerOrderId, String providerPaymentId, String signature,
                                      long expectedAmountMinor, String expectedCurrency) {
        if (providerOrderId == null || providerPaymentId == null || signature == null || signature.isBlank()) {
            return new PaymentVerification(false, "Missing payment details");
        }

        String expected = hmacSha256(providerOrderId + "|" + providerPaymentId, props.getRazorpay().getKeySecret());
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Razorpay checkout signature verification FAILED for order {}", providerOrderId);
            return new PaymentVerification(false, "Payment signature verification failed");
        }

        // Cross-check the payment server-side: never trust the callback alone.
        JsonNode payment = fetchPayment(providerPaymentId);
        if (payment == null) {
            return new PaymentVerification(false, "Payment could not be confirmed");
        }
        String status = payment.path("status").asText("");
        long amount = payment.path("amount").asLong(-1);
        String currency = payment.path("currency").asText("");
        String orderId = payment.path("order_id").asText("");

        if (!orderId.equals(providerOrderId)) {
            return new PaymentVerification(false, "Payment does not belong to this order");
        }
        if (!"captured".equals(status) && !"authorized".equals(status)) {
            return new PaymentVerification(false, "Payment is not completed (status: " + status + ")");
        }
        if (amount != expectedAmountMinor || !currency.equals(expectedCurrency)) {
            log.error("Razorpay amount mismatch for {}: expected {} {} got {} {}",
                    providerOrderId, expectedAmountMinor, expectedCurrency, amount, currency);
            return new PaymentVerification(false, "Payment amount mismatch");
        }
        return new PaymentVerification(true, null);
    }

    private JsonNode fetchPayment(String providerPaymentId) {
        try {
            return client.get().uri("/payments/{id}", providerPaymentId).retrieve().body(JsonNode.class);
        } catch (Exception e) {
            log.error("Razorpay fetchPayment failed: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        if (signatureHeader == null || props.getRazorpay().getWebhookSecret() == null
                || props.getRazorpay().getWebhookSecret().isBlank()) {
            return false;
        }
        String expected = hmacSha256(rawBody, props.getRazorpay().getWebhookSecret());
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signatureHeader.getBytes(StandardCharsets.UTF_8));
    }

    static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }
}
