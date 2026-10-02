package com.studyvault.orders;

import com.studyvault.common.error.PaymentException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Manual UPI flow: buyers pay to the store owner's UPI QR, submit the UTR,
 * and an admin approves the claim. No external gateway call happens at
 * checkout; verification is a human decision recorded in the admin panel.
 */
@Component
@ConditionalOnProperty(name = "app.payments.provider", havingValue = "upi")
public class ManualUpiGateway implements PaymentGateway {

    @Override
    public String provider() {
        return "upi";
    }

    @Override
    public GatewayOrder createOrder(long amountMinor, String currency, String receipt) {
        return new GatewayOrder("upi-" + receipt.toLowerCase(), "");
    }

    @Override
    public PaymentVerification verify(String providerOrderId, String providerPaymentId, String signature,
                                      long expectedAmountMinor, String expectedCurrency) {
        // Not used by the manual flow - approval happens through the admin panel.
        throw new PaymentException("UPI payments are verified manually after the buyer submits the transaction reference.");
    }

    @Override
    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        return false; // no webhooks in the manual flow
    }
}
