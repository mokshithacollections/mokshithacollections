# Mokshitha Collections

E-commerce web application for Mokshitha Collections — Spring Boot 4 + PostgreSQL + Thymeleaf.

The frontend (Thymeleaf templates + vanilla JS) is in `src/main/resources/templates` and `src/main/resources/static`. The backend is a layered Spring application: controllers → services → repositories → JPA entities, with all REST surfaces under `/api/**` and view routes serving the `.html` templates.

---

## Stack

- **Java 17** (Spring Boot 4.0.0 / Spring 7)
- **PostgreSQL 13+** (any modern version)
- **Spring Data JPA / Hibernate**
- **Spring Security** — session-cookie auth, CSRF (cookie + header), BCrypt(12), persistent remember-me
- **Thymeleaf** for views
- **Lombok**
- **Maven** wrapper bundled (`./mvnw` / `mvnw.cmd`)

---

## Quick start (Docker — recommended)

Bring up the app + a fresh Postgres in one command:

```bash
docker compose up --build
```

That's it — open `http://localhost:8081`. Default seeded admin: `admin@mokshitha.local` / `ChangeMe!2026`.

To override the defaults (admin password, DB password, exposed ports), copy `.env.example` to `.env` and edit the values **before** the first `up`. The `.env` file is gitignored.

```bash
cp .env.example .env
# edit .env, then:
docker compose up --build
```

Useful follow-up commands:

```bash
docker compose logs -f app           # tail the app logs
docker compose down                  # stop containers, keep data
docker compose down -v               # stop + wipe the postgres + uploads volumes
docker compose up -d --build         # rebuild the app image and restart
docker compose exec postgres psql -U mokshitha mokshitha_collections   # open psql
```

The `postgres_data` and `uploads_data` Docker volumes persist between restarts, so your products / orders / image uploads survive `docker compose down` (use `-v` to wipe).

---

## Quick start (without Docker, native)

### 1. Prerequisites

- JDK 17 or newer
- PostgreSQL running locally
- Maven (the wrapper handles this)

### 2. Create the database

```sql
CREATE DATABASE mokshitha_collections;
```

The default dev credentials are `postgres` / `postgres`. Override via env vars (see [Environment variables](#environment-variables)) if your local Postgres is different.

### 3. Run

**Windows / PowerShell:**

```powershell
.\mvnw.cmd spring-boot:run
```

**macOS / Linux:**

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8081` under the `dev` profile by default.

On first start, an admin user is auto-created (`admin@mokshitha.local` / `ChangeMe!2026`). **Change that password immediately** by signing in and `POST /account/password`, or by setting `ADMIN_EMAIL` / `ADMIN_PASSWORD` env vars before the first start.

---

## Environment variables

All secrets are externalised. Defaults are dev-only — override every one in production.

| Variable | Used by | Default | Notes |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Spring | `dev` | Set to `prod` in production |
| `SERVER_PORT` | Spring | `8081` | |
| `DB_URL` | Datasource | `jdbc:postgresql://localhost:5432/mokshitha_collections` | |
| `DB_USERNAME` | Datasource | `postgres` | |
| `DB_PASSWORD` | Datasource | `postgres` | **Required** in prod |
| `JPA_DDL_AUTO` | Hibernate | `update` (dev), `validate` (prod) | Use migrations in prod |
| `JPA_SHOW_SQL` | Hibernate | `false` | Toggle for debugging |
| `REMEMBER_ME_KEY` | Spring Security | `dev-only-...` | **Required** in prod — 32+ chars |
| `ADMIN_EMAIL` | `AdminSeeder` | `admin@mokshitha.local` | |
| `ADMIN_PASSWORD` | `AdminSeeder` | `ChangeMe!2026` | **Required** in prod |
| `UPLOADS_ROOT` | `ImageStorageService` | `./uploads` | Filesystem path for product images |

`StartupSecretsValidator` will refuse to boot under the `prod` profile if `DB_PASSWORD`, `REMEMBER_ME_KEY`, or `ADMIN_PASSWORD` are still at their dev defaults.

---

## Running in production

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:postgresql://db.internal:5432/mokshitha
export DB_USERNAME=mokshitha_app
export DB_PASSWORD=<secret>
export REMEMBER_ME_KEY=<32+ random chars>
export ADMIN_EMAIL=ops@yourdomain.com
export ADMIN_PASSWORD=<secret>
export UPLOADS_ROOT=/var/lib/mokshitha/uploads

./mvnw -DskipTests package
java -jar target/mokshitha_collections-0.0.1-SNAPSHOT.jar
```

The prod profile additionally:
- Sets `spring.jpa.hibernate.ddl-auto=validate` — schema must be migrated separately (Flyway/Liquibase recommended next)
- Marks the session cookie `Secure` (HTTPS-only)
- Emits `Strict-Transport-Security` (1-year HSTS, includeSubDomains)
- Quietens log levels

You should sit it behind HTTPS (nginx, Cloudflare, ALB) — the app already trusts `X-Forwarded-*` via `server.forward-headers-strategy=framework`.

---

## Architecture overview

```
src/main/java/com/ec/mokshitha_collections/
├── config/             SecurityConfig, WebMvcConfig, AdminSeeder, GlobalModelAdvice,
│                       StartupSecretsValidator, RequestIdFilter
├── controller/         View + REST controllers
│   └── admin/          /api/admin/** REST controllers (ROLE_ADMIN gated)
├── dto/                Request / response shapes per domain
├── entity/             JPA entities
├── exception/          Custom exceptions + GlobalExceptionHandler
├── repository/         Spring Data JPA repositories
├── security/           CustomUserDetails(Service), LoginAttemptService, CsrfCookieFilter
└── service/            Business logic (transactional)
    └── admin/          Admin-side services
    └── product/        ProductSpecifications (composable JPA criteria)
```

### Routing convention

- **`/api/**`** — REST endpoints returning JSON
- All other routes — Thymeleaf views returning HTML

This separation matters: registering a view at `/cart` and a REST endpoint at `GET /cart` is an `Ambiguous mapping` error at startup.

### Security

- **Auth**: session cookie (`MC_SESSIONID`), 30-min idle timeout, single concurrent session per user, session fixation migrate-on-login.
- **Passwords**: BCrypt cost 12. Stored hash never leaves `CustomUserDetails`.
- **CSRF**: `CookieCsrfTokenRepository.withHttpOnlyFalse()` issues `XSRF-TOKEN`; clients must echo `X-XSRF-TOKEN` on every state-changing request. The frontend `getCsrfToken()` helper handles this.
- **Remember-me**: `PersistentTokenBasedRememberMeServices` — DB-backed single-use tokens, 14-day default, cookie `MC_REMEMBER_ME`.
- **Login throttling**: 5 failed attempts per IP in a 15-min window → 429 for the rest of the window. Clears on success. In-memory only (single instance).
- **Admin**: `hasRole("ADMIN")` on `/api/admin/**`. The seeder creates one admin on first boot.
- **Headers**: `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: same-origin`, plus HSTS in prod.
- **Validation**: every request DTO is `@Valid`-annotated; `GlobalExceptionHandler` turns Bean Validation errors into 400 JSON without leaking internals.
- **Logs**: every request is tagged with a short `rid` MDC field and reflected as `X-Request-Id` so a single user flow can be grepped.

---

## API surface

### Auth (`/auth`)

| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/login` | Form-urlencoded `email/password/rememberMe` → 200 + session cookie |
| POST | `/auth/register` | Form-urlencoded — creates user, returns user info |
| POST | `/auth/logout` | Invalidates session + remember-me cookie |
| GET | `/auth/check-session` | `{ loggedIn, userId, email, firstName, isAdmin }` |

### Catalog (public)

| Method | Path |
|---|---|
| GET | `/api/products?categoryId=&categorySlug=&minPrice=&maxPrice=&search=&page=&sort=` |
| GET | `/api/products/{id}` |
| GET | `/api/products/{productId}/reviews` |
| GET | `/api/categories` |
| GET | `/api/categories/{id}` |
| GET | `/api/categories/slug/{slug}` |

### Authenticated user

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/cart` | Current cart with totals |
| POST | `/api/cart/items` | `variantId`, `quantity` |
| POST | `/api/cart/items/{id}` | `quantity` (update) |
| DELETE | `/api/cart/items/{id}` | Remove line |
| POST | `/api/cart/toggle/{productId}` | Add/remove (frontend-compat) |
| POST | `/wishlist/toggle/{productId}` | |
| POST | `/account/address/add` / `update/{id}` / `delete/{id}` / `default/{id}` | |
| POST | `/account/password` | `currentPassword`, `newPassword`, `confirmPassword` |
| POST | `/api/orders` | `addressId`, `paymentMethod=COD` |
| GET | `/api/orders?page=&size=` | Paged order history |
| GET | `/api/orders/{id}` | Order detail |
| POST | `/api/orders/{id}/cancel` | Cancel + restock |
| POST | `/api/products/{id}/reviews` | Write a review (one per product) |
| POST | `/api/products/{id}/reviews/{reviewId}` | Update own review |
| DELETE | `/api/products/{id}/reviews/{reviewId}` | Delete own review |

### Admin (`ROLE_ADMIN`)

All under `/api/admin/**`.

| Method | Path |
|---|---|
| `GET / POST / POST {id} / DELETE {id}` | `/api/admin/products` |
| `POST {id}/variants`, `POST {id}/variants/{vid}`, `DELETE {id}/variants/{vid}` | |
| `POST {id}/variants/{vid}/images` (multipart) / `DELETE /images/{imageId}` | |
| `GET / POST / POST {id} / DELETE {id}` | `/api/admin/categories` |
| `GET ?status=`, `GET {id}`, `POST {id}/status` | `/api/admin/orders` |
| `GET /pending`, `POST {id}/approve`, `DELETE {id}` | `/api/admin/reviews` |
| `GET ?search=`, `POST {id}/activate`, `POST {id}/deactivate`, `POST {id}/promote`, `POST {id}/demote` | `/api/admin/users` |

---

## Calling the API from JavaScript

The CSRF helper is already present in the bundled JS:

```js
function getCsrfToken() {
    const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
    return m ? decodeURIComponent(m[1]) : '';
}

fetch('/api/cart/items', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded',
    'X-XSRF-TOKEN': getCsrfToken()
  },
  body: new URLSearchParams({ variantId: 1, quantity: 2 })
});
```

Every POST/PUT/DELETE must include the `X-XSRF-TOKEN` header — Spring will reject it otherwise.

---

## Tests

```bash
./mvnw test
```

The suite uses an H2 in-memory DB in PostgreSQL-compat mode (see `src/test/resources/application-test.properties`), so no Postgres is required for tests.

`AuthControllerIT` covers the auth happy path + duplicate email + bad password + login rate-limiting. Use it as the starting pattern for cart/order/admin tests.

---

## Things that are intentionally simple

These are areas you may want to harden later — they're not bugs, just lean choices that fit a small shop:

- **Login throttling is in-memory.** Single-instance only; swap for Redis if you scale horizontally.
- **Stock check at checkout has no row-level lock.** Two simultaneous buyers on the last-in-stock item could both succeed. Add `@Version` on `ProductVariant` (optimistic) or `SELECT … FOR UPDATE` if it ever matters.
- **Schema is managed by Hibernate `ddl-auto=update` in dev / `validate` in prod.** No migration tool wired yet — Flyway/Liquibase is the obvious next step.
- **Payment is Cash-on-Delivery only.** `PaymentMethod` enum has slots ready for online/UPI/card when you wire a gateway.
- **Reviews require admin approval.** A reviewer who just posted won't see their own review until approved — flag if you want a "show pending if my own" branch.
- **Image uploads go to local FS.** Move to S3/Cloudinary by replacing `ImageStorageService` if you ever run multiple instances.

---

## Default admin credentials

On a fresh boot the seeder creates `admin@mokshitha.local` / `ChangeMe!2026` (and logs a loud warning). The first thing to do is sign in and change that password.
