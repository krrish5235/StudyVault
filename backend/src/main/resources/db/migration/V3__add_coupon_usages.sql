-- =====================================================================
-- StudyVault - coupon usage tracking (missed in V1; added as V3 since
-- V1 was already applied - migrations are append-only, never edited)
-- =====================================================================

CREATE TABLE coupon_usages (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coupon_id  UUID        NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    order_id   UUID        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    used_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_coupon_usages_order UNIQUE (order_id)
);
CREATE INDEX idx_coupon_usages_coupon_user ON coupon_usages (coupon_id, user_id);
