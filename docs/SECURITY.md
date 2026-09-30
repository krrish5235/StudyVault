# StudyVault — Security

Threat model: public internet storefront selling digital goods. Highest-value assets:
user credentials, payment integrity, paid files. This document maps controls to risks.

## Authentication & sessions

| Control | Implementation |
|---|---|
| Password storage | Argon2id (spring-security-crypto, 64 MB / 3 iterations — OWASP-aligned defaults) |
| Password policy | Min 10 chars, upper+lower+digit, enforced server-side via Bean Validation |
| Brute force | Per-IP rate limit on login/register/reset + per-account lockout (5 fails → 15 min) + generic error messages |
| Session transport | Opaque 256-bit token in `HttpOnly` `SameSite=Lax` cookie (`Secure` in production); SHA-256 hash stored server-side |
| Session attacks | Token generated with `SecureRandom`, 7-day TTL, activity-sliding up to 30-day cap, revocable (logout / logout-all / password change) |
| CSRF | Double-submit cookie + `X-XSRF-TOKEN` header enforced on all mutating endpoints, incl. login (login-CSRF) |
| Tokens (reset/verify) | 256-bit random, SHA-256-hashed at rest, single-use, short TTL (30 min / 24 h), all sessions revoked on password reset |
| Secrets | Env vars only; `.env` gitignored; fail-fast startup validation in `prod` profile |

## Authorization (RBAC)

- Roles: `USER`, `ADMIN` in `roles`/`user_roles`; future roles (`SUPER_ADMIN`, `SUPPORT`) are additive rows.
- `/**/admin/**` endpoints: `@PreAuthorize("hasRole('ADMIN')")` + filter-level role check — not reachable by users even with crafted requests.
- Ownership checks in services: library, downloads, orders, reviews, sessions.
- Reference: OWASP A01 (Broken Access Control). Tested by `SecurityAccessTests` (user→admin = 403, anonymous→protected = 401, IDOR checks).

## Payment integrity

- Prices, discounts, coupons, totals computed **server-side only** from DB values.
- Razorpay checkout signature verified (HMAC-SHA256) **and** payment fetched from
  Razorpay API to cross-check amount/currency/status before granting access.
- Webhook: raw-body HMAC verify, event-ID de-duplication, idempotent grant path —
  duplicate deliveries cannot double-grant or double-count coupon usage.
- Entitlement uniqueness `(user_id, product_id)` at the DB level.
- No card data ever touches StudyVault servers (checkout happens on Razorpay).

## File security

- Paid files live in a **private** bucket (or local private dir in dev); no public URLs.
- Download = authenticate → verify entitlement → rate limit → audit → 302 to 5-minute
  presigned URL. Presigning happens only in the backend.
- Uploads (admin only): extension whitelist, MIME sniffing of magic bytes
  (`%PDF-`, `PK\x03\x04`, PNG/JPEG/WEBP signatures), size caps, UUID filenames,
  path-traversal-safe key construction. Original filenames never used as storage keys.

## API hardening

- Centralized validation (Jakarta Bean Validation + Zod on the frontend).
- Rate limiting per route class (auth 10/min, password-reset 3/15 min, checkout 10/min,
  downloads 30/min, admin 120/min, default 240/min per IP).
- CORS: explicit origin allowlist, credentials enabled, empty default.
- Security headers (backend + frontend): CSP, HSTS, `X-Content-Type-Options: nosniff`,
  `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy`,
  `X-Frame-Options: DENY` / `frame-ancestors 'none'`.
- Errors: safe generic messages, correlation IDs, no stack traces, no SQL/ORM details.
- JPA/Hibernate parameter binding everywhere — no string-concatenated SQL.
- Request size caps (multipart + header size) to blunt DoS.

## Logging & monitoring

- Structured logs with correlation ID; authentication events, payment events, admin
  actions and security events are logged **and** written to `audit_logs` (who/what/
  when/which resource/result).
- Never logged: passwords, tokens, payment secrets, card data (no card data exists).
- Actuator limited to `health,info`; no env/heap endpoints exposed.

## Known limitations / roadmap

- Rate limiter is per-instance (in-memory); move to Redis when running >1 replica.
- Add 2FA (TOTP) for admin accounts.
- Add automated dependency scanning gates in CI (currently advisory).
