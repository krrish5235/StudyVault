package com.studyvault.orders;

/**
 * Payment provider abstraction. The backend is the only source of truth for
 * amounts, currency and status - implementations must verify everything
 * server-side and never trust client-supplied values.
 */
public interface PaymentGateway {

    String provider();

    GatewayOrder createOrder(long amountMinor, String currency, String receipt);

    /**
     * Full verification of a checkout callback: signature check + server-side
     * payment lookup + amount/currency/status cross-check.
     */
    PaymentVerification verify(String providerOrderId, String providerPaymentId, String signature,
                               long expectedAmountMinor, String expectedCurrency);

    boolean verifyWebhookSignature(String rawBody, String signatureHeader);

    record GatewayOrder(String id, String keyIdForClient) {
    }

    record PaymentVerification(boolean success, String failureReason) {
    }
}
