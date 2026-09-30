package com.studyvault.orders;

import com.studyvault.common.error.PaymentException;
import com.studyvault.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * DEVELOPMENT-ONLY gateway that lets the full checkout flow run without real
 * Razorpay credentials. Refused in production by AppStartupValidator.
 * Verification requires a non-empty signature token to keep the flow realistic.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.payments.provider", havingValue = "mock")
public class MockGateway implements PaymentGateway {

    public static final String DEV_SIGNATURE = "mock-signature";

    private final AppProperties props;

    @Override
    public String provider() {
        return "mock";
    }

    @Override
    public GatewayOrder createOrder(long amountMinor, String currency, String receipt) {
        return new GatewayOrder("mock_order_" + receipt.toLowerCase().replace("sv-", ""), props.getRazorpay().getKeyId());
    }

    @Override
    public PaymentVerification verify(String providerOrderId, String providerPaymentId, String signature,
                                      long expectedAmountMinor, String expectedCurrency) {
        if (signature == null || signature.isBlank()) {
            return new PaymentVerification(false, "Missing payment signature");
        }
        return new PaymentVerification(true, null);
    }

    @Override
    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        // Dev-only: webhooks are not exercised against the mock gateway.
        return DEV_SIGNATURE.equals(signatureHeader);
    }
}
