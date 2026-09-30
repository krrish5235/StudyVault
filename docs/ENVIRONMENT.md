# StudyVault — Environment & local setup

## One-time portable toolchain

The project vendors a portable toolchain into `.tools/` (gitignored, per-machine):

```bash
mkdir -p .tools && cd .tools
curl -sSL -o maven.zip https://dlcdn.apache.org/maven/maven-3/3.9.16/binaries/apache-maven-3.9.16-bin.zip
C:/Windows/System32/tar.exe -xf maven.zip && rm maven.zip
curl -sSL -o node.zip https://nodejs.org/dist/latest-v22.x/node-v22.23.3-win-x64.zip
C:/Windows/System32/tar.exe -xf node.zip && rm node.zip
curl -sSL -o pg.zip https://get.enterprisedb.com/postgresql/postgresql-17.6-1-windows-x64-binaries.zip
C:/Windows/System32/tar.exe -xf pg.zip pgsql/bin pgsql/lib pgsql/share && rm pg.zip
```

(Any system-wide JDK 21+, Maven, Node 20+, PostgreSQL 15+ works equally well.)

## Required environment variables

| Variable | Used by | Notes |
|---|---|---|
| `DATABASE_URL` | backend | `jdbc:postgresql://host:5432/studyvault` |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | backend | DB credentials |
| `APP_CORS_ALLOWED_ORIGINS` | backend | Comma-separated frontend origins |
| `APP_FRONTEND_URL` | backend | Used in email links |
| `APP_COOKIE_SECURE` | backend | `true` in production (HTTPS) |
| `APP_COOKIE_DOMAIN` | backend | Optional; e.g. `.yourdomain.com` |
| `STORAGE_PROVIDER` | backend | `local` (dev) / `s3` (prod) |
| `LOCAL_STORAGE_DIR` | backend | Dev-only private storage root |
| `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_S3_BUCKET` | backend | S3 config (or instance role) |
| `EMAIL_PROVIDER` | backend | `log` (dev) / `smtp` |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `EMAIL_FROM` | backend | SMTP when `smtp` |
| `PAYMENT_PROVIDER` | backend | `razorpay` / `mock` (dev only — prod startup fails on `mock`) |
| `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET` | backend | Razorpay credentials |
| `ADMIN_BOOTSTRAP_ENABLED`, `ADMIN_BOOTSTRAP_EMAIL`, `ADMIN_BOOTSTRAP_PASSWORD` | backend | One-time first-admin creation |
| `NEXT_PUBLIC_API_URL` | frontend | Base URL of the API, e.g. `http://localhost:8080` |

## Environments

| | Development | Staging | Production |
|---|---|---|---|
| Database | local/pg portable or compose | managed, isolated | managed + PITR |
| Storage | `local` dir or MinIO | private bucket `…-staging` | private bucket, versioning on |
| Payments | Razorpay **test** keys or `mock` | Razorpay **test** keys | Razorpay **live** keys |
| Email | `log` provider | SMTP + test inbox | SMTP provider |
| Cookies | `APP_COOKIE_SECURE=false` | `true` | `true` |
| Bootstrap admin | optional | optional | run once, then disable |

**Rule:** production credentials are never present in development shells. The app
refuses to start with `prod` profile if critical config is missing/unsafe (see
`AppStartupValidator`) or if `PAYMENT_PROVIDER=mock` in production.
