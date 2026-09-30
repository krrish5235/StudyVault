# StudyVault — Database

PostgreSQL 15+ (17 recommended). Schema is owned exclusively by Flyway migrations in
`backend/src/main/resources/db/migration/` — never edit a shipped migration; add `V(n)__*.sql`.

| Migration | Contents |
|---|---|
| `V1__initial_schema` | 25 tables, constraints, indexes, triggers, pg_trgm search indexes |
| `V2__reference_data` | roles (USER/ADMIN), default categories, site settings defaults |
| `V3__add_coupon_usages` | coupon redemption ledger (append-only fix to V1 gap) |

## Entity map

```
users ─┬─ user_roles ─── roles
       ├─ sessions            (token_hash unique; expiry slide)
       ├─ password_reset_tokens / email_verification_tokens
       ├─ orders ── order_items ── products
       ├─ entitlements   UNIQUE(user_id, product_id)   ← single source of paid access
       ├─ wishlists ── wishlist_items                   UNIQUE(wishlist, product)
       ├─ reviews        UNIQUE(product_id, user_id)
       └─ notifications

categories ── products ── product_tags ── tags
                  ├─ product_files  (versioned; partial UNIQUE current per product)
                  └─ product_previews
coupons ── coupon_products / coupon_usages (UNIQUE order_id)
payments        UNIQUE(provider, provider_order_id)
processed_webhook_events  UNIQUE(provider, event_id)
audit_logs · site_settings · downloads
```

## Conventions

- **UUID primary keys** (`gen_random_uuid()`), created/updated timestamps maintained by
  `set_updated_at()` triggers.
- **Money = integer minor units (paise)** in `BIGINT` — never floats.
- **Integrity at the DB level**: unique emails (case-insensitive index on `lower(email)`),
  unique slugs, coupon codes, one current file version per product (partial unique index),
  one entitlement/review per (user, product), CHECK constraints on money and enums.
- **Cascades chosen deliberately**: user→sessions CASCADE (privacy), product→files CASCADE,
  orders→payments RESTRICT (financial records are never orphan-destroyed); products are
  soft-deleted (`ARCHIVED`) rather than removed when referenced by orders.
- **Search**: `pg_trgm` GIN indexes on title/short description (created defensively —
  search degrades to plain ILIKE if the extension is unavailable).

## Operations

- Migrations run automatically on boot (`spring.flyway.enabled=true`, `validate-on-migrate`).
- Local psql (portable toolchain): `.tools/pgsql/bin/psql -U studyvault -d studyvault`.
- Full index inventory lives in `V1__initial_schema.sql` — keep query-driven indexes
  (e.g. new dashboard aggregates) in future migrations.
