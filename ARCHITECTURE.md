# Architecture

This document explains *why* the project is shaped the way it is, not just what's in it. Read it
once before Phase 2 of `ROADMAP.md`, then come back to specific sections as you build each module.

## 1. What this system is

A small internal tool for a two-person clothing business: track inventory of garments (by product
type, size and color), track money in and out, and record the initial investment. Two people log
in (you and your coworker); everyone else who ever sees it only sees a read-only catalog.

The scale matters for every decision below: this is not a system built to handle thousands of
concurrent users or millions of rows. It's built to be *correct*, *understandable*, and *cheap to
run*, and to teach you the same patterns used in much bigger systems, just without the parts that
only matter at scale (caching layers, message queues, microservices...). Resist the urge to add
those later just because you've read about them - add them if and when you actually hit the
problem they solve.

## 2. Domain model

```
ProductType (1) ---- (N) Product (1) ---- (N) ProductVariant
                                                    |
                                                    | (1)
                                                    |
                                                    (N)
                                            StockMovement
                                       (PRODUCTION_IN / SALE_OUT /
                                        CENTRO_OUT / ADJUSTMENT)

SalesOrder (1) ---- (N) SalesOrderItem ---- (N:1) ProductVariant

CentroShipment (N:1) ProductVariant       <- stock side, no money on this row
CentroIncome (standalone - not linked to any shipment)  <- money side

Material (standalone - no variants, not garments)

MoneyTransaction (INCOME/EXPENSE, category SALE_ONLINE/SALE_CENTRO/RAW_MATERIAL/SERVICE/OTHER)

Investment (standalone - founding capital + later injections)

User (ADMIN | VIEWER)
```

- **ProductType** - a garment category ("Sweater" today, "T-Shirt"/"Beanie" later). Already
  implemented in `product/ProductType.java` as your reference example.
- **Product** - a sellable design within a type, e.g. "Classic Crewneck" (sku, name, description,
  base price).
- **ProductVariant** - one size+color combination of a Product, with its own SKU. This is the unit
  everything else (movements, sale items, centro shipments) actually points at - never the parent
  Product.
- **StockMovement** - see section 3, it's the most important table in the system.
- **SalesOrder / SalesOrderItem** - an online order: customer info, status, date, and the variants
  + quantities + prices sold.
- **CentroShipment** - what you sent to the physical store: variant, quantity, date. Stock side
  only, one row per shipment.
- **CentroIncome** - money the store pays you, days later, for some unspecified subset of what
  you've shipped there (could be half a shipment, a third, across several shipments - you don't
  track which). A standalone row: amount, date received, notes. See section 2a for why this is a
  separate entity from `CentroShipment` rather than an `income_amount` column on it.
- **Material** - raw materials (fabric, thread, etc.), tracked separately from garments because
  they aren't sized/colored variants.
- **MoneyTransaction** - every income and expense, with a category so you can later build a report
  like "income by category, this month."
- **Investment** - founding capital and any later injections. A table, not a single number, so
  adding money later doesn't need a schema change.
- **User** - your login account and your coworker's. See section 5.

The full column-level definition is in `backend/src/main/resources/db/migration/V1__init_schema.sql`
- read the comments there, they explain the same decisions at the column level.

## 2a. Why Centro is two entities, not one

The original design had a single `CentroShipment` row carrying both the shipment (variant,
quantity, date) and an `income_amount` column. That modeled "we shipped X and got paid Y for
exactly that shipment" - a 1:1 relationship. It doesn't match how the business actually works:

1. You ship products to Centro today.
2. Some days later, Centro pays you for *some* of what's been sold - not necessarily everything
   you've shipped, and not broken down by which variant or which shipment it came from.

That's a many-to-many-ish, untracked relationship between shipments and payments, so forcing it
onto one row per shipment would mean either leaving `income_amount` at 0 until you arbitrarily
decide which shipment a payment "belongs to" (inventing information you don't actually have), or
lumping unrelated payments onto whichever shipment happens to be open (corrupting your records).

Splitting into two independent tables sidesteps the problem entirely:

- `CentroShipment` answers "what have we sent to Centro" (feeds the stock ledger).
- `CentroIncome` answers "what has Centro paid us" (feeds the money ledger).

Neither references the other. The Centro dashboard/page just shows both lists side by side plus
their totals - which is exactly what you described wanting: what was sent, what came in, and the
running total, without pretending to know a per-shipment breakdown you don't have.

## 3. Why a ledger, not a counter

The tempting first design for "current stock" is a `quantity` column directly on `ProductVariant`,
incremented and decremented as things happen. **Don't do this.** Two reasons:

1. **It's a lie waiting to happen.** The moment one code path forgets to update the counter (a bug,
   a manual SQL fix, a failed transaction that partially applied), the number on screen no longer
   matches reality, and you have no way to know *why* it's wrong or *when* it went wrong.
2. **You lose history.** "How much stock did we have on July 1st?" or "show me everything that
   happened to this variant" become impossible to answer if all you ever kept was the latest total.

Instead, `stock_movements` is an **append-only ledger**: every event that changes stock (a
production income, a sale, a centro shipment, a manual correction) is one row, never updated or
deleted. Current stock for a variant is simply:

```sql
SELECT COALESCE(SUM(quantity), 0)
FROM stock_movements
WHERE product_variant_id = ?;
```

Incomes (`PRODUCTION_IN`) are stored as positive quantities, outcomes (`SALE_OUT`, `CENTRO_OUT`) as
negative, so a plain `SUM` gives you the running balance. `ADJUSTMENT` exists for the rare manual
correction (found a miscount during a physical inventory count) and should always carry a note
explaining why.

This is the same idea as double-entry bookkeeping, or how a bank shows you a list of transactions
instead of just a balance: the balance is a *derived view* of the ledger, not a separate fact you
maintain by hand. `money_transactions` uses the identical pattern for cash instead of stock.

**Performance note, for later:** once you have tens of thousands of movements, summing on every
Dashboard load might get slow. The fix, if you ever need it, is a materialized view or a cached
`current_stock` table that's recomputed on a schedule or on write - but that's an optimization on
top of the ledger, not a replacement for it. Don't build it until you've actually measured a slow
query; at your real-world scale (one small business) you likely never will.

## 4. What a status/event triggers

A few actions in the UI are really "write to two tables in one transaction." Doing this in a
`@Transactional` service method (not in the controller, not as two separate API calls from
Angular) is what guarantees you never end up with a stock movement and no matching money
transaction (or vice versa) if something fails halfway through.

This models **cash on delivery**, which is how your online sales actually work today: money isn't
real until the order is confirmed delivered, but the stock is already spoken for the moment you
book it (you've set those specific items aside for that customer).

| Action | Writes |
|---|---|
| Create a `SalesOrder` (status `BOOKED`) | One `SALE_OUT` `StockMovement` per item. **No money transaction yet.** |
| Change status to `IN_PROGRESS` | Just updates `SalesOrder.status` - nothing else changes |
| Change status to `DELIVERED` | One `INCOME` `MoneyTransaction` (category `SALE_ONLINE`) for the order total - this is the only point at which the sale produces money |
| Change status to `CANCELLED` (only allowed from `BOOKED` or `IN_PROGRESS` - `DELIVERED` is terminal) | A reversing `ADJUSTMENT` `StockMovement` per item (puts stock back). **No money to reverse** - none was ever recorded, since cancellation only ever happens before delivery |
| Create a `CentroShipment` | One `CENTRO_OUT` `StockMovement`. **No money transaction** - see section 2a |
| Create a `CentroIncome` | One `INCOME` `MoneyTransaction` (category `SALE_CENTRO`) - no stock effect, no link back to a specific shipment |
| Create a `Material` purchase | One `EXPENSE` `MoneyTransaction` (category `RAW_MATERIAL`) |
| Record a production income | Just a `PRODUCTION_IN` `StockMovement` - no money transaction (the fabric's cost was already recorded as a `Material` expense; if you pay someone to *make* the garments, that's a `SERVICE` expense you record separately) |

**Staying flexible for "paid at booking" later:** nothing about the schema ties money to a
particular status - `money_transactions.reference_type`/`reference_id` can point at a sales order
from any transition. The *only* thing that encodes "pay on delivery" is which line of
`SalesOrderService` calls `MoneyTransactionService.record(...)` (the `DELIVERED` handler, today).
Moving that one call to the `BOOKED` handler switches the whole business to "paid upfront" - no
migration, no column changes. This is the same lesson as section 3: keep business rules in service
code, where they're one method away from changing, not baked into column defaults or constraints
that would need a migration to revisit.

## 5. Security model

Two roles: `ADMIN` (you and your coworker - full read/write access to everything) and `VIEWER`
(read-only catalog access - not used today, but modeled now so a future "customer can browse the
catalog" feature is a role check, not a rewrite).

Authentication is a stateless JWT, not server-side sessions:

- `POST /api/auth/login` verifies username/password against the `users` table (password hashed
  with BCrypt, never stored in plain text) and returns a signed token.
- Every other request sends that token in an `Authorization: Bearer <token>` header.
- `JwtAuthFilter` runs once per request, validates the signature and expiry, and tells Spring
  Security who's making the request - no database lookup needed on every request, no server-side
  session store to keep in sync if you ever run more than one backend instance.
- There's deliberately no logout endpoint: invalidating a token before it expires needs a
  denylist, which is unnecessary complexity for a 2-person internal tool where a token just expires
  after 8 hours (`app.jwt.expiration-minutes`, see `application.yml`).

Accounts are **not** self-registered. You'll insert exactly two rows into `users` by hand (see
`ROADMAP.md` Phase 3) - there is intentionally no public sign-up form, matching "only my coworker
and I can log in."

## 6. Backend layering

Every feature package (`product`, `movement`, `sales`, `centro`, `materials`, `finance`, `auth`)
follows the same shape, demonstrated fully in `product/ProductType*.java`:

```
Controller  ->  translates HTTP <-> Java. No business logic here.
   |
Service     ->  business rules, transactions, validation that needs the database.
   |
Repository  ->  Spring Data interface, talks to Postgres. No logic here either.
   |
Entity      ->  a @Entity class, one per table.

DTOs (Request/Response records) sit between Controller and the outside world - entities are
never returned directly from a controller, so you can change the database shape without
breaking the API contract, and never accidentally expose a column (like password_hash) you
didn't mean to.
```

This is "package by feature" (`com.sie.core.sales` has its controller+service+repo+entity
all together) rather than "package by layer" (`controllers/`, `services/`, `repositories/` as
top-level packages). Package-by-feature scales much better as the app grows: to understand or
change how sales orders work, you open one folder, not four.

`common/` holds the handful of things every feature needs: `BaseEntity` (id + audit timestamps),
`GlobalExceptionHandler` (turns exceptions into a consistent JSON error shape), `NotFoundException`.

## 7. Frontend architecture

Angular 20, standalone components (no `NgModule`s), signals for local component state. Folder
structure mirrors the backend:

```
core/           - things every feature needs: auth (service, guard), http (JWT interceptor)
shared/         - models and reusable UI shared across features
features/
  auth/         - login page
  catalog/      - reference implementation - study this before building the rest
  dashboard/, sales/, production/, centro/, materials/, finance/  - one per backend module,
                  empty until you build them in ROADMAP.md's later phases
```

The `catalog` feature is the complete worked example: `catalog.service.ts` (HTTP calls, typed with
the `ProductType` model that mirrors the backend's DTO), `catalog.component.ts/html/scss` (a
signal-based component that loads and displays data). Copy this shape for every other feature.

`core/http/auth.interceptor.ts` is a **functional interceptor**: it runs for every outgoing
request, attaches the JWT if present, and logs you out + redirects to `/login` if the backend ever
responds 401. `core/auth/auth.guard.ts` is a **functional guard**: it blocks navigation to a
protected route unless you're logged in. Both patterns replace the class-based
guards/interceptors used in older Angular tutorials - if you find a tutorial using
`implements CanActivate` or `implements HttpInterceptor`, know that the function-based
equivalent shown here is the current idiomatic style (Angular 15+).

In local development, `ng serve` proxies any request to `/api/*` to `http://localhost:8080` (see
`proxy.conf.json`) so the browser only ever talks to one origin (`localhost:4200`) and never has to
deal with CORS. In production, nginx does the same job (see `frontend/nginx.conf`) - it's the same
architecture in both places, just a different reverse proxy doing the forwarding.

**Styling: Tailwind CSS.** Utility classes (`class="flex gap-2 rounded-md bg-slate-800 p-4"`)
instead of hand-written SCSS per component. Wired in via `@tailwindcss/postcss` (see
`.postcssrc.json`) - Angular's build already runs PostCSS, so this is the only file needed, no
`tailwind.config.js` required with Tailwind v4's default setup. `src/styles.scss` pulls it in with
a single `@import "tailwindcss";`. You can still write plain SCSS in a component's own `.scss` file
for anything a utility class doesn't cover (like the few hand-written styles already in
`app.scss`/`catalog.component.scss` from Phase 0) - Tailwind and component SCSS coexist fine.

## 8. Config, the 12-factor way

`application.yml` reads every environment-specific value (`spring.datasource.url`,
`app.jwt.secret`, ...) from an environment variable with a safe local default:
`${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/stockapp}`. This means the exact same
jar/Docker image runs unchanged on your laptop, in `docker-compose.yml`, and on Railway/Render -
only the environment variables differ, injected by whichever platform is running it. Never
hardcode an environment-specific value (a real password, a production URL) into a `.java` or
`.yml` file that gets committed to git.

## 9. Deployment target

Locally and for self-hosting: `docker compose up --build` runs Postgres + backend + frontend
together (see `docker-compose.yml`). For sharing with your coworker/friend without keeping your own
laptop on: push the same three containers to a free-tier host (Railway or Render were your pick) -
Phase 7 in `ROADMAP.md` walks through it once the app itself is further along.

## 10. A note on versions

This scaffold was generated in September 2026 using the current stable releases: **Spring Boot 4**
(released late 2025) and **Angular 20**. Both are recent enough that some tutorials and Stack
Overflow answers you find will target the previous major version (Spring Boot 3.x /
Spring Security 6, or Angular's older NgModule-based style) - the *concepts* transfer directly, but
some API names differ (e.g. Spring Boot 4 splits `spring-boot-starter-web` into
`spring-boot-starter-webmvc`, and ships separate `-test` starter artifacts, both visible in
`backend/pom.xml`). When something in a tutorial doesn't compile, that version gap - not a mistake
on your part - is the first thing to suspect.

**Java version:** running on **Java 25** (the current LTS release, replacing Java 21 from the
initial scaffold) since that's what's already on your machine. Confirmed compatible before making
the switch: Spring Boot 4.1.1's own system requirements list support from Java 17 up to Java 26, so
this isn't pushing past what the framework is built for. Three places encode the Java version and
all three now say 25, kept in sync deliberately: `backend/pom.xml`'s `<java.version>`,
`backend/Dockerfile`'s two `FROM` lines (the Maven build stage and the JRE runtime stage both need
to match - a jar compiled for 25 won't run on a 21 JRE), and `.github/workflows/ci.yml`'s
`setup-java` step. If you ever bump this again, all three need to move together.

## 11. Continuous Integration (GitHub Actions)

`.github/workflows/ci.yml` runs automatically on every push and every pull request against `main`.
Two independent jobs, `backend` and `frontend`, run in parallel on GitHub's own throwaway virtual
machines ("runners" - you never see or manage them, GitHub provisions and destroys one per job
run):

- `backend`: checks out your code, installs Java 25 (with Maven's dependency cache restored from a
  previous run so it doesn't re-download the internet every time), then runs `mvn -B verify`.
- `frontend`: checks out your code, installs Node 22, runs `npm ci` (a stricter, reproducible
  version of `npm install` - it uses only what's in `package-lock.json`, never resolves new
  versions, and is what CI should always use), then `npm run build`.

If either job fails, GitHub shows a red X on the commit/PR instead of a green check - that's the
entire point of CI: a failing build or test gets caught the moment code is pushed, not weeks later
when someone happens to run it manually. Right now there are no real tests yet (see "Later ideas"
in `ROADMAP.md`), so this pipeline is mostly verifying "does it still compile/build" - still
valuable, since the rename you just asked for is exactly the kind of change that could have quietly
broken the build without you noticing until you tried to run it again.

A few concepts worth knowing by name since you'll see them everywhere in GitHub Actions docs:

- **Workflow** - the whole YAML file; **job** - a group of steps that runs on one runner (jobs run
  in parallel by default, like `backend` and `frontend` here); **step** - one command or one
  reusable "action" (`actions/checkout@v4` is an action someone else published that just clones
  your repo onto the runner).
- **Trigger** (`on:`) - what causes the workflow to run. Ours is `push`/`pull_request` on `main`;
  other common ones are a schedule (cron) or a manual button click.
- **Secrets** - encrypted values (an API key, a deploy token) stored in the repo's Settings ->
  Secrets, referenced as `${{ secrets.NAME }}` in the YAML, never visible in logs. You don't need
  any yet, but Phase 7 (deploying) is where a real project would add one (e.g. a Railway deploy
  token) to fully automate deployment on every merge to `main` - worth coming back to once Phase 7
  is in front of you.
