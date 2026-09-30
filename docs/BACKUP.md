# StudyVault — Backup & disaster recovery

## What must survive a disaster

| Asset | Where | Protection |
|---|---|---|
| PostgreSQL (users, orders, payments, entitlements, audit) | managed instance | automated daily snapshots **+ PITR** (WAL archiving), retention ≥ 30 days |
| Paid digital files | S3 bucket | S3 **versioning** + cross-region replication (recommended); never rely on a single bucket lifecycle |
| Marketing images (covers/previews) | S3 `public/` namespace | same bucket, versioning |
| Configuration/secrets | hosting platform env vars / secret manager | exported and stored in the operator's password manager |
| Code + migrations | git | remote origin; migrations are the schema backup |

> Platform "daily database backups" alone are NOT a backup strategy: enable PITR and
> test the restore path quarterly.

## Backup schedule (recommended)

- **RPO ≤ 5 min**: WAL-based PITR on PostgreSQL.
- **Full snapshot**: daily, retention 30 days; monthly snapshot retained 1 year.
- **Audit logs**: same durability as the DB — they are compliance-relevant.

## Restore procedure

1. Provision a fresh PostgreSQL instance at the target PITR timestamp.
2. Start the backend against it — Flyway validates the schema matches migrations.
3. Point DNS/load balancer at the restored environment (or swap database URL).
4. Verify: admin login works, `/admin/dashboard` shows pre-incident revenue,
   a test entitlement download succeeds.

## File storage restore

1. List object versions for affected keys (`aws s3api list-object-versions`).
2. Restore prior versions or copy from the replica bucket.
3. Cross-check `product_files` rows against storage keys — the admin file-version
   UI shows which version should be current (`is_current`); reactivate if needed.

## Recovery runbook quick reference

- Lost session data only: sessions are disposable — users re-login; no restore needed.
- Compromised admin account: disable via Users admin (revokes sessions instantly),
  rotate `ADMIN_BOOTSTRAP` (must stay disabled in prod), audit `audit_logs` for actions
  taken, rotate Razorpay/AWS/SMTP credentials.
- Ransomware/deletion of bucket: restore from versioning/replica; entitlements map
  users→products in PostgreSQL, so re-uploading current versions restores service.
