# StudyVault — Deployment

## Topology

- **Frontend**: Vercel (or any Node host / CDN). Set `NEXT_PUBLIC_API_URL=https://api.<domain>`
  and `NEXT_PUBLIC_SITE_URL=https://<domain>` at build time.
- **Backend**: container image (`backend/Dockerfile`, non-root, health-checked) on
  AWS ECS/Fargate, Render or Railway, behind TLS. Run at least 2 replicas with the
  rate limiter moved to Redis (see SECURITY.md roadmap) or keep 1 replica behind a
  per-instance limit.
- **Database**: managed PostgreSQL 15+ with automated backups + PITR.
- **Storage**: private S3 bucket (versioning ON, block all public access).

## First deployment checklist

1. Create the database + user; run migrations (automatic on backend boot).
2. Configure environment (see ENVIRONMENT.md). In `prod` profile the app refuses to
   start unless: `APP_COOKIE_SECURE=true`, CORS origins set, Razorpay keys present,
   S3 bucket set, `PAYMENT_PROVIDER` ≠ mock.
3. Bootstrap the first admin: run once with `ADMIN_BOOTSTRAP_ENABLED=true` +
   strong `ADMIN_BOOTSTRAP_PASSWORD` (≥12 chars). The bootstrap is idempotent and
   disables itself once an admin exists — then set `ADMIN_BOOTSTRAP_ENABLED=false`
   and change the admin password on first login (enforced via `must_change_password`).
4. Razorpay setup: create webhook `https://api.<domain>/api/v1/payments/webhook/razorpay`
   subscribed to `payment.captured`, `payment.failed`, `refund.processed`; store
   `RAZORPAY_WEBHOOK_SECRET`. Use **test keys** in staging first.
5. Point DNS: frontend at `app.<domain>`, API at `api.<domain>` (same registrable
   domain so SameSite=Lax cookies work; CORS allowlist covers the frontend origin).

## CI/CD

`.github/workflows/ci.yml`: on every PR/push → backend compile + unit tests +
Testcontainers integration tests (`mvn verify`), frontend type check + build,
dependency review (fail on high severity). Deploy jobs should be added per platform
(image push + service update) and gated on the test jobs — never deploy a broken build.

## Rollout & rollback

- Backend: migrations are additive and backward-compatible; roll forward to revert
  application bugs. If a migration must be reverted, write a new `V(n+1)` migration —
  never edit history.
- Frontend: instant rollback via Vercel deployments.
- Keep the previous container image tag until the new release is verified.

## Post-deploy verification

1. `GET /actuator/health` → UP (mail indicator disabled; DB probe included).
2. Sign in as admin; dashboard metrics render real data.
3. Upload a test product (file + cover), publish, purchase with Razorpay **test**
   keys, confirm webhook delivery in the Razorpay dashboard, download from library.
4. Verify security headers on both frontend and API responses (curl -I).
