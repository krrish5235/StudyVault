-- =====================================================================
-- StudyVault - initial schema (PostgreSQL 15+)
-- All money values are integer minor units (paise). All IDs are UUIDs.
-- =====================================================================

CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50)  NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                  VARCHAR(255) NOT NULL,
    password_hash          VARCHAR(255) NOT NULL,
    full_name              VARCHAR(120) NOT NULL,
    email_verified         BOOLEAN      NOT NULL DEFAULT FALSE,
    must_change_password   BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled                BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_attempts  INT          NOT NULL DEFAULT 0,
    locked_until           TIMESTAMPTZ,
    last_login_at          TIMESTAMPTZ,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_password_len CHECK (LENGTH(password_hash) >= 20),
    CONSTRAINT ck_users_name_len     CHECK (LENGTH(full_name) BETWEEN 1 AND 120)
);
-- Case-insensitive uniqueness for emails (emails stored lowercased by the app too)
CREATE UNIQUE INDEX uq_users_email_lower ON users (LOWER(email));

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE sessions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash    VARCHAR(64)  NOT NULL UNIQUE,        -- SHA-256 hex of opaque cookie token
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at    TIMESTAMPTZ  NOT NULL,
    last_used_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    revoked_at    TIMESTAMPTZ,
    ip_address    VARCHAR(45),
    user_agent    VARCHAR(400)
);
CREATE INDEX idx_sessions_user    ON sessions (user_id);
CREATE INDEX idx_sessions_expires ON sessions (expires_at) WHERE revoked_at IS NULL;

CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_prt_user ON password_reset_tokens (user_id);

CREATE TABLE email_verification_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_evt_user ON email_verification_tokens (user_id);

-- ---------------------------------------------------------------- catalog

CREATE TABLE categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500),
    sort_order  INT          NOT NULL DEFAULT 0,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE tags (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(60) NOT NULL UNIQUE,
    slug       VARCHAR(80) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE products (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title               VARCHAR(200) NOT NULL,
    slug                VARCHAR(220) NOT NULL UNIQUE,
    short_description   VARCHAR(300) NOT NULL,
    description         TEXT         NOT NULL,
    category_id         UUID         NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    price_minor         BIGINT       NOT NULL CHECK (price_minor >= 0),
    compare_price_minor BIGINT       CHECK (compare_price_minor IS NULL OR compare_price_minor >= price_minor),
    currency            VARCHAR(3)   NOT NULL DEFAULT 'INR',
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    featured            BOOLEAN      NOT NULL DEFAULT FALSE,
    bestseller          BOOLEAN      NOT NULL DEFAULT FALSE,
    cover_image_key     VARCHAR(500),
    content_version     INT          NOT NULL DEFAULT 1,
    avg_rating          NUMERIC(3,2) NOT NULL DEFAULT 0 CHECK (avg_rating BETWEEN 0 AND 5),
    review_count        INT          NOT NULL DEFAULT 0,
    view_count          BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_products_status   ON products (status);
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_price    ON products (price_minor);
CREATE INDEX idx_products_created  ON products (created_at DESC);
CREATE INDEX idx_products_featured ON products (featured, created_at DESC) WHERE status = 'PUBLISHED';

CREATE TABLE product_tags (
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    tag_id     UUID NOT NULL REFERENCES tags(id)     ON DELETE CASCADE,
    PRIMARY KEY (product_id, tag_id)
);

CREATE TABLE product_files (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id        UUID        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    version           INT         NOT NULL,
    storage_key       VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255),
    content_type      VARCHAR(100) NOT NULL,
    size_bytes        BIGINT      NOT NULL CHECK (size_bytes > 0),
    is_current        BOOLEAN     NOT NULL DEFAULT TRUE,
    uploaded_by       UUID        REFERENCES users(id) ON DELETE SET NULL,
    uploaded_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (product_id, version)
);
-- Only one current (downloadable) version per product
CREATE UNIQUE INDEX uq_product_files_current ON product_files (product_id) WHERE is_current;
CREATE INDEX idx_product_files_product ON product_files (product_id, version DESC);

CREATE TABLE product_previews (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    storage_key VARCHAR(500) NOT NULL,
    sort_order  INT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_product_previews_product ON product_previews (product_id, sort_order);

-- ---------------------------------------------------------------- coupons

CREATE TABLE coupons (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code               VARCHAR(40) NOT NULL UNIQUE,   -- stored UPPERCASE
    discount_type      VARCHAR(10) NOT NULL CHECK (discount_type IN ('PERCENT', 'FIXED')),
    percent_off        INT,
    amount_off_minor   BIGINT,
    min_order_minor    BIGINT      NOT NULL DEFAULT 0,
    max_discount_minor BIGINT,
    starts_at          TIMESTAMPTZ,
    expires_at         TIMESTAMPTZ,
    usage_limit        INT,
    used_count         INT         NOT NULL DEFAULT 0,
    per_user_limit     INT         NOT NULL DEFAULT 1,
    category_id        UUID        REFERENCES categories(id) ON DELETE SET NULL,
    active             BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_coupon_value CHECK (
        (discount_type = 'PERCENT' AND percent_off BETWEEN 1 AND 100 AND amount_off_minor IS NULL)
        OR
        (discount_type = 'FIXED'   AND amount_off_minor > 0                AND percent_off IS NULL)
    )
);
CREATE INDEX idx_coupons_code ON coupons (code) WHERE active;

CREATE TABLE coupon_products (
    coupon_id  UUID NOT NULL REFERENCES coupons(id)  ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    PRIMARY KEY (coupon_id, product_id)
);

-- ---------------------------------------------------------------- orders

CREATE TABLE orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number    VARCHAR(30) NOT NULL UNIQUE,
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'CANCELLED', 'REFUNDED', 'PARTIALLY_REFUNDED')),
    subtotal_minor  BIGINT      NOT NULL CHECK (subtotal_minor >= 0),
    discount_minor  BIGINT      NOT NULL DEFAULT 0 CHECK (discount_minor >= 0),
    total_minor     BIGINT      NOT NULL CHECK (total_minor >= 0),
    currency        VARCHAR(3)  NOT NULL DEFAULT 'INR',
    coupon_id       UUID        REFERENCES coupons(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_orders_user      ON orders (user_id);
CREATE INDEX idx_orders_status    ON orders (status);
CREATE INDEX idx_orders_created   ON orders (created_at DESC);

CREATE TABLE order_items (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id          UUID   NOT NULL REFERENCES orders(id)   ON DELETE CASCADE,
    product_id        UUID   NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    title_snapshot    VARCHAR(200) NOT NULL,
    unit_price_minor  BIGINT NOT NULL CHECK (unit_price_minor >= 0),
    line_total_minor  BIGINT NOT NULL CHECK (line_total_minor >= 0)
);
CREATE INDEX idx_order_items_order   ON order_items (order_id);
CREATE INDEX idx_order_items_product ON order_items (product_id);

CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            UUID        NOT NULL REFERENCES orders(id) ON DELETE RESTRICT,
    provider            VARCHAR(30) NOT NULL,
    provider_order_id   VARCHAR(100),
    provider_payment_id VARCHAR(100),
    amount_minor        BIGINT      NOT NULL CHECK (amount_minor >= 0),
    currency            VARCHAR(3)  NOT NULL DEFAULT 'INR',
    status              VARCHAR(20) NOT NULL DEFAULT 'CREATED'
                        CHECK (status IN ('CREATED', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'REFUNDED')),
    failure_reason      VARCHAR(500),
    raw_payload         JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_payments_provider_order ON payments (provider, provider_order_id) WHERE provider_order_id IS NOT NULL;
CREATE INDEX idx_payments_provider_payment ON payments (provider_payment_id);
CREATE INDEX idx_payments_status ON payments (status);

CREATE TABLE entitlements (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    product_id UUID        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    order_id   UUID        NOT NULL REFERENCES orders(id)   ON DELETE RESTRICT,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ,
    CONSTRAINT uq_entitlements_user_product UNIQUE (user_id, product_id)
);
CREATE INDEX idx_entitlements_user ON entitlements (user_id) WHERE revoked_at IS NULL;

CREATE TABLE downloads (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    product_id UUID        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    file_id    UUID        NOT NULL REFERENCES product_files(id) ON DELETE CASCADE,
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_downloads_user    ON downloads (user_id);
CREATE INDEX idx_downloads_product ON downloads (product_id);
CREATE INDEX idx_downloads_created ON downloads (created_at DESC);

-- ---------------------------------------------------------------- wishlists

CREATE TABLE wishlists (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE wishlist_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wishlist_id UUID        NOT NULL REFERENCES wishlists(id)   ON DELETE CASCADE,
    product_id  UUID        NOT NULL REFERENCES products(id)    ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_wishlist_items UNIQUE (wishlist_id, product_id)
);
CREATE INDEX idx_wishlist_items_wishlist ON wishlist_items (wishlist_id);

-- ---------------------------------------------------------------- reviews

CREATE TABLE reviews (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    rating     INT         NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT,
    status     VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' CHECK (status IN ('PUBLISHED', 'HIDDEN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_reviews_user_product UNIQUE (product_id, user_id)
);
CREATE INDEX idx_reviews_product ON reviews (product_id) WHERE status = 'PUBLISHED';

-- ---------------------------------------------------------------- platform

CREATE TABLE site_settings (
    key        VARCHAR(100) PRIMARY KEY,
    value      JSONB        NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by UUID         REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE audit_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id      UUID        REFERENCES users(id) ON DELETE SET NULL,
    actor_email   VARCHAR(255),
    action        VARCHAR(100) NOT NULL,
    resource_type VARCHAR(60),
    resource_id   VARCHAR(64),
    details       JSONB,
    ip_address    VARCHAR(45),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_created  ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_logs_actor    ON audit_logs (actor_id);
CREATE INDEX idx_audit_logs_action   ON audit_logs (action);

CREATE TABLE notifications (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type       VARCHAR(50)  NOT NULL,
    title      VARCHAR(200) NOT NULL,
    body       VARCHAR(1000),
    read_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user ON notifications (user_id, created_at DESC);

CREATE TABLE processed_webhook_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider    VARCHAR(30)  NOT NULL,
    event_id    VARCHAR(150) NOT NULL,
    received_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_processed_events UNIQUE (provider, event_id)
);

-- ---------------------------------------------------------------- search support
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS pg_trgm;
    CREATE INDEX idx_products_title_trgm   ON products USING gin (title gin_trgm_ops);
    CREATE INDEX idx_products_short_trgm   ON products USING gin (short_description gin_trgm_ops);
EXCEPTION WHEN OTHERS THEN
    NULL; -- pg_trgm unavailable: catalog search falls back to plain ILIKE
END $$;

-- ---------------------------------------------------------------- updated_at triggers
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated             BEFORE UPDATE ON users           FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_categories_updated        BEFORE UPDATE ON categories      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_products_updated          BEFORE UPDATE ON products        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_coupons_updated           BEFORE UPDATE ON coupons         FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_orders_updated            BEFORE UPDATE ON orders          FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_payments_updated          BEFORE UPDATE ON payments        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_reviews_updated           BEFORE UPDATE ON reviews         FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_site_settings_updated     BEFORE UPDATE ON site_settings   FOR EACH ROW EXECUTE FUNCTION set_updated_at();
