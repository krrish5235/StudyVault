# StudyVault — Architecture

## 1. System overview

```
                         ┌─────────────────────────────┐
        Browser ─HTTPS──►│  CDN / Edge (Vercel)        │
                         └──────────────┬──────────────┘
                                        │
                         ┌──────────────▼──────────────┐
                         │  Next.js frontend (SSR/ISR) │
                         │  storefront + admin panel   │
                         └──────────────┬──────────────┘
                                        │ REST /api/v1 (JSON, cookie session)
                         ┌──────────────▼──────────────┐
                         │  Spring Boot backend        │
                         │  Controller→Service→Repo    │
                         │  Spring Security + filters  │
                         └───┬───────┬───────┬────┬────┘
                             │       │       │    │
                  ┌──────────▼─┐ ┌───▼───┐ ┌─▼──┐ └───────────┐
                  │ PostgreSQL │ │  S3   │ │ Razorpay │ Email   │
                  │ (Flyway)   │ │private│ │ payments │ (SMTP)  │
                  └────────────┘ └───────┘ └──────────┘ └─────────┘
```

Frontend and backend are **independently deployable** and communicate only through the
versioned REST API. All environment-specific configuration comes from environment
variables — no secrets in code, images, or the frontend bundle.

## 2. Backend layering

```
com.studyvault
├── common/        API envelope, error handling, rate limiting, audit, email, utils
├── config/        Spring configuration (security, CORS, storage, properties)
├── security/      Session auth filter, CSRF filter, rate-limit filter, RBAC
├── user/          Registration, login, sessions, password reset, verification
├── catalog/       Categories, products, tags, files, search (public + admin)
├── orders/        Orders, payments (Razorpay), webhooks, entitlements
├── library/       Purchased library + entitlement-gated downloads
├── reviews/ wishlist/ coupons/
└── admin/         Dashboard metrics, audit log, command center, site settings
```

Rules enforced across all layers:

- **Controller → Service → Repository** only; controllers never touch repositories.
- DTOs at the boundary — JPA entities never leave the service layer.
- Authorization checks via Spring Security method annotations; services validate
  ownership (entitlement) for user-scoped resources (defends against IDOR).
- All money is stored as integer **minor units** (paise) — never floats.
- All multi-step state changes (order + payment + entitlements, price changes + audit)
  run in database transactions.

## 3. Key flows

### Authentication
```
login ──► verify Argon2id hash ──► create session row (token_hash = SHA-256(opaque))
      ◄── HttpOnly cookie (opaque token)
every request ──► SessionAuthFilter: hash cookie, load session, check expiry/revocation,
                  slide last_used, populate SecurityContext with ROLE_USER/ROLE_ADMIN
CSRF: every mutating request must echo the double-submit token (cookie + X-XSRF-TOKEN)
```
Sessions are server-side and revocable instantly (logout / logout-all / password change
/ admin ban). Rotation: expiry slides with activity, capped at max lifetime.

### Purchase (Razorpay)
```
checkout ──► backend loads products by ID, prices from DB only, validates coupon
             server-side, creates Order(PENDING)+items, calls Razorpay create-order
       ◄── razorpay_order_id + public key (client opens Razorpay Checkout)
payment ──► POST /payments/verify: HMAC signature check + gateway API cross-check
             (amount, currency, status) + idempotent transition → Order PAID,
             Payment CAPTURED, entitlements granted, coupon usage recorded, emails
webhook ──► POST /payments/webhook/razorpay: raw-body HMAC verify → same idempotent
             grant path; event IDs de-duplicated in processed_webhook_events
```
The client can never influence amount, product list, or payment status.

### Paid file download
```
GET /library/{productId}/download
  ──► authenticated? owns entitlement? product active? rate-limited?
  ──► record download ──► 302 to presigned S3 URL (5 min TTL)
Local dev storage serves the same contract through the backend with an HMAC-signed,
expiring path — identical authorization semantics.
```

## 4. Data model

25 tables — full DDL in `backend/src/main/resources/db/migration/V1__initial_schema.sql`,
diagram and column docs in [DATABASE.md](DATABASE.md). All IDs are UUIDs, all tables
carry created_at/updated_at (trigger-maintained), FKs with explicit cascade rules,
unique constraints at the DB level for: emails, slugs, coupon codes, session/token
hashes, (user, product) entitlements and reviews, (provider, provider_order_id).

## 5. Frontend architecture

- App Router with server components for SEO-critical pages (product detail, sitemap).
- Product URLs are slugs: `/products/backend-development-complete-notes`.
- Client state kept minimal: session/profile via API, cart persisted locally
  (prices always recomputed server-side at checkout).
- 3D hero (React Three Fiber) is dynamically imported, disabled for
  `prefers-reduced-motion` and low-power/mobile devices, geometry-only (no heavy assets).
- Admin panel is a client app guarded by backend RBAC; middleware only does UX redirects.

## 6. Deployment topology (production)

- Frontend: Vercel (CDN, ISR). Recommended: host frontend on `app.yourdomain.com` and
  API on `api.yourdomain.com` — same registrable domain so `SameSite=Lax` session
  cookies work cross-subdomain, with a strict CORS allowlist on top.
- Backend: container on AWS ECS/Render/Railway behind TLS-terminating proxy
  (`forward-headers-strategy: framework`).
- Database: managed PostgreSQL (automated backups + PITR; see [BACKUP.md](BACKUP.md)).
- Storage: private S3 bucket, versioning on, no public access (see [BACKUP.md](BACKUP.md)).
- Environments: dev / staging / production with isolated databases, buckets, and
  Razorpay test vs live keys (see [ENVIRONMENT.md](ENVIRONMENT.md)).

## 7. Design decisions & trade-offs

| Decision | Rationale |
|---|---|
| Opaque server-side sessions over JWT | Instant revocation, no token storage in browser, simpler and safer for a cookie-based SPA |
| Integers for money (paise) | No floating-point rounding errors; matches Razorpay amounts exactly |
| Razorpay called via thin REST client | Avoids the legacy Java SDK dependency chain; HMAC verification is trivial and explicit |
| Custom double-submit CSRF | Predictable SPA behavior (deferred-token complications), small auditable filter |
| Slug + UUID | SEO-friendly URLs without leaking sequence IDs |
| Soft-delete products (ARCHIVED) | Orders/entitlements must remain resolvable; hard delete only when unreferenced |
| pg_trgm extension for search | Fast `ILIKE '%q%'` on title/short description without a search cluster |
