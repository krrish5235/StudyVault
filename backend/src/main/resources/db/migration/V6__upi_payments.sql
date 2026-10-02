-- =====================================================================
-- StudyVault - manual UPI payments: buyer pays via UPI QR, submits the
-- transaction reference (UTR), admin approves -> entitlement granted.
-- =====================================================================

CREATE TABLE payment_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    transaction_ref VARCHAR(100) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    admin_note      VARCHAR(300),
    reviewed_by     UUID         REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- one UTR can never be claimed twice
    CONSTRAINT uq_payment_requests_ref UNIQUE (transaction_ref)
);
CREATE INDEX idx_payment_requests_status ON payment_requests (status, created_at DESC);
CREATE INDEX idx_payment_requests_order  ON payment_requests (order_id);

INSERT INTO site_settings (key, value) VALUES
  ('payment.upiId', '"krrishg5235@okicici"'),
  ('payment.payeeName', '"Krrish Gupta"'),
  ('payment.qrUrl', '"/upi-qr.jpeg"');
