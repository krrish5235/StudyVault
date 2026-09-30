# StudyVault — API

Base URL: `{NEXT_PUBLIC_API_URL}/api/v1`. All responses use the envelope
`{"success": true, "data": …, "message": …}` or `{"success": false, "message": …, "errors": {field: msg}}`.
Authentication is a session cookie (`sv_session`, HttpOnly). Every mutating request must
echo the CSRF cookie in the `X-XSRF-TOKEN` header (get one from `GET /auth/csrf`).

## Auth

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET  | `/auth/csrf` | – | Issues CSRF cookie + token |
| POST | `/auth/register` | – | `{fullName, email, password}` → 201 |
| POST | `/auth/login` | – | Sets session cookie; generic 401 on failure |
| POST | `/auth/logout` | user | Revokes current session |
| POST | `/auth/logout-all` | user | Revokes all sessions |
| GET  | `/auth/me` | user | Current user (roles, verification flags) |
| GET  | `/auth/sessions` | user | Active sessions with device info |
| DELETE | `/auth/sessions/{id}` | user | Revoke one session |
| POST | `/auth/change-password` | user | Revokes other sessions |
| POST | `/auth/forgot-password` | – | Always generic 200 |
| POST | `/auth/reset-password` | – | Single-use token; revokes all sessions |
| POST | `/auth/verify-email` | – | Single-use token |
| POST | `/auth/resend-verification` | user | Rate-limited |

## Catalog (public)

| Method | Path | Notes |
|---|---|---|
| GET | `/categories` | Active categories |
| GET | `/products` | `query, category, tag, minPrice, maxPrice, minRating, sort(newest\|popular\|rating\|price_asc\|price_desc), page, size(≤50)` |
| GET | `/products/featured?limit=` | Featured, published |
| GET | `/products/bestsellers?limit=` | Bestsellers |
| GET | `/products/{slug}` | Detail; increments view counter |
| GET | `/products/{id}/reviews` | Published reviews, paginated |
| GET | `/files/public/**` | Cover/preview images (`public/` namespace only) |

## Wishlist · Library

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/wishlist` | user | Products saved for later |
| POST/DELETE | `/wishlist/{productId}` | user | Add / remove |
| GET | `/library` | user | Purchased products (entitlement-backed) |
| GET | `/library/{productId}/download` | user | Entitlement check → audit row → presigned URL (302) or authenticated stream |

## Payments

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/payments/orders` | user | `{productIds, couponCode?}` → `{orderNumber, providerOrderId, providerKeyId, amountMinor}` — prices computed server-side |
| POST | `/payments/verify` | user | Checkout callback; signature + gateway cross-check; idempotent |
| POST | `/payments/webhook/{provider}` | signature | HMAC over raw body; event-id de-duplication; CSRF-exempt |
| GET | `/orders`, `/orders/{orderNumber}` | user | Own order history |

## Admin (RBAC: `hasRole('ADMIN')`)

| Method | Path | Notes |
|---|---|---|
| GET/POST | `/admin/products` · PATCH/DELETE `/admin/products/{id}` | CRUD; DELETE = archive (soft) |
| POST | `/admin/products/{id}/publish` · `/unpublish` | Status transitions with validation |
| POST | `/admin/products/{id}/files/digital` | Multipart upload; magic-byte validated; new version |
| GET | `/admin/products/{id}/files` · POST `/{fileId}/activate` | Version list + rollback |
| POST | `/admin/products/{id}/cover` · `/previews` | Image uploads (public namespace) |
| GET/POST/PATCH/DELETE | `/admin/categories` | Delete blocked when products exist |
| GET | `/admin/orders` · `/{orderNumber}` | Filter by status |
| POST | `/admin/orders/{orderNumber}/refund` | Mark refunded; revokes access |
| GET | `/admin/users` · PATCH `/{id}/enabled` | Disable = instant session revocation |
| GET/POST/PATCH | `/admin/coupons` (+`/{id}/active`) | Coupon CRUD |
| GET | `/admin/reviews` (+`/{id}/hide`, `/{id}/publish`) | Moderation |
| GET | `/admin/dashboard` | Real aggregates: revenue, orders, AOV, conversion, 14-day series, top products |
| GET | `/admin/audit-logs` | Who/what/when/details |
| GET/PUT | `/settings/site` (GET public) | DB-backed site content |
| POST | `/admin/command` (+`/confirm`) | Whitelisted NL commands; destructive ⇒ signed confirmation |

## Status codes

`400` validation · `401` unauthenticated · `403` CSRF/forbidden · `404` missing ·
`409` conflict (duplicate email/review, already owned) · `413` upload too large ·
`429` rate limited (+`Retry-After`) · `500` generic safe message (correlation id in logs,
`X-Request-Id` echoed on every response).
