# StudyVault — Testing

## Layers

| Layer | What | Where |
|---|---|---|
| Unit tests | Password-token hashing, rate-limit buckets & per-client isolation, slug rules, upload magic-byte validation (incl. disguised executables, oversized files) | `backend/src/test/java/...` |
| Integration tests | Full auth/security flow against real PostgreSQL via Testcontainers: CSRF enforcement, register→login→me→logout, session cookie flags (HttpOnly), 401/403 RBAC boundaries, weak-password rejection, generic login errors | `backend/src/test/java/com/studyvault/it/AuthSecurityFlowIT.java` |
| Frontend | TypeScript strict type check + production build (`npm run build` = type check gate) | CI |
| Scripted E2E (local, no Docker) | Full business flow against a live server — see below | manual/scripted |

Run tests: `cd backend && mvn verify` (unit tests always; Testcontainers ITs skip automatically
when Docker is absent — in CI they run against real PostgreSQL 17).

## Scripted E2E (used to verify every phase locally)

Prerequisites: local PostgreSQL running, backend started with `PAYMENT_PROVIDER=mock`,
admin bootstrapped via env.

1. `GET /api/v1/auth/csrf` → token + cookie
2. Admin login → create product → upload digital file (`multipart`, real PDF) → publish
3. Malicious upload attempt (EXE bytes named `.pdf`) → 400
4. User: create order (server computes ₹total) → verify (mock signature) → verify again
   (idempotent, no duplicate entitlement)
5. `GET /library` shows product; pre-purchase download attempt → 403, post-purchase → PDF bytes
6. Review by purchaser → 201; duplicate → 409; non-purchaser → 403
7. Coupon created by admin → applied at checkout server-side
8. `GET /admin/dashboard` reflects real revenue/orders/AOV
9. Command Center: `"show revenue"` executes; `"change price of X to 299"` requires
   confirmation; confirm executes; `"drop table users"` is rejected as unrecognized
10. Audit log rows exist for every step above

## Security regression checklist (manual gate before releases)

- [ ] Anonymous → protected endpoint = 401; USER → `/api/v1/admin/**` = 403 (backend, not UI)
- [ ] POST without CSRF token = 403 (including login)
- [ ] Session cookie: HttpOnly, SameSite=Lax, Secure in prod; revoked after logout/reset
- [ ] Brute force: 6th rapid login attempt → locked/generic 401, security email sent
- [ ] Upload: EXE/HTML bytes rejected; oversized rejected; storage keys contain no client input
- [ ] Download without entitlement → 403; presigned URL expires after 5 minutes (prod)
- [ ] Webhook with bad signature → 401; duplicate event id → ignored, no double grant
- [ ] Price tampering: client-supplied amounts never influence the final charge
- [ ] `PAYMENT_PROVIDER=mock` fails startup validation in `prod` profile
