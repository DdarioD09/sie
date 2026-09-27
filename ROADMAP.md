# Roadmap

A checklist to work through with me, one phase at a time. Read `ARCHITECTURE.md` first - it
explains the *why* behind everything referenced here. Each phase lists what you'll learn and what
"done" looks like. Come back to this conversation for each phase; tell me which one you're
starting and I'll walk you through it in the "guided, I code" style - I'll explain the concept and
the shape of the solution, you write the code, I'll review it.

## Phase 0 - Environment, done for you ✅

What you got in this scaffold: a Spring Boot 4 (Java 25, Maven) backend and an Angular 20 frontend,
both generated from their official CLIs (not hand-typed), a Postgres schema via Flyway, a working
JWT login flow, and one complete vertical slice (`ProductType`, backend and frontend) proven to run
end-to-end. Docker Compose ties it all together for local dev and self-hosting. Tailwind CSS is
wired into the frontend build (`ARCHITECTURE.md` section 7). A GitHub Actions CI pipeline
(`.github/workflows/ci.yml`) builds and tests both projects on every push - it starts running the
moment you push this to GitHub, nothing more to set up (`ARCHITECTURE.md` section 11).

**Your task right now:** get it running on your own machine.

1. Install Docker Desktop (or Docker Engine) if you don't have it, and a Java 25 + Node 22 setup if
   you want to run backend/frontend outside Docker while developing (faster iteration than
   rebuilding containers every change).
2. `git init`, commit this scaffold, create an empty repo on GitHub, push it. This is your first
   hands-on task - if you haven't set up SSH/token auth for git push before, this is worth doing
   properly now rather than later.
3. `docker compose up --build` from the project root. Confirm `http://localhost:4200` loads the
   login page and `http://localhost:8080/actuator/health` returns `{"status":"UP"}`.
4. Come back here once it's running (or if it doesn't) and we'll debug together before moving on.

## Phase 1 - Create your first real login (30-60 min)

**Learn:** BCrypt password hashing, why you never store or transmit plain-text passwords, a
practical gotcha with shell quoting.

1. Generate a BCrypt hash for your password without installing anything extra, using Docker:
   ```bash
   docker run --rm python:3.12-alpine sh -c "pip install bcrypt -q && python3 -c \"import bcrypt; print(bcrypt.hashpw(b'YOUR_REAL_PASSWORD', bcrypt.gensalt()).decode())\""
   ```
2. **Important:** don't insert the hash with a one-line `psql -c "INSERT ... '$HASH' ..."` command
   through `su`/`sudo` - the `$` characters in a BCrypt hash (it looks like `$2b$10$...`) can get
   silently mangled by a second shell re-interpreting them as variables (we hit this exact bug
   while building this scaffold). Instead, write the INSERT into a `.sql` file and run
   `psql -f that_file.sql` - no re-interpretation, no corruption.
3. Insert two rows (you and your coworker) into `users` with `role = 'ADMIN'`.
4. Confirm you can log in from `http://localhost:4200/login` and land on the catalog page.

## Phase 2 - Build out the Catalog (Product + ProductVariant)

**Learn:** modeling a one-to-many relationship in JPA, request validation, how to extend a
reference implementation instead of starting from a blank file.

1. Re-read `product/ProductType.java` through `ProductTypeController.java` - that's your template.
2. Add `Product` (references `ProductType`) and `ProductVariant` (references `Product`; size,
   color, sku_variant) - entity, repository, service, controller, DTOs, same shape.
3. Add an endpoint that returns "current stock per variant" - this is your first read against
   `stock_movements` (it'll return 0 for everything until Phase 3 exists - that's fine, verify the
   query and endpoint work independent of Phase 3).
4. Frontend: a `Product`/`ProductVariant` model + service + component, following `catalog/` exactly.

## Phase 3 - The inventory ledger (StockMovement) + Dashboard

**Learn:** the ledger pattern from `ARCHITECTURE.md` section 3 in practice; aggregate SQL queries
via Spring Data (`@Query` with `SUM`/`GROUP BY`); building a real-time-feeling dashboard from a
query, not a cached number.

1. `StockMovement` entity/repository (no direct "create" endpoint yet - it's written *by* other
   services, not directly by users, except for `ADJUSTMENT`).
2. A `MovementService.recordProductionIncome(variantId, quantity, date)` - this is what Phase 4's
   "Production" screen will call.
3. A Dashboard endpoint: current stock per variant (and, once Product/ProductType exist, roll that
   up per product and per type too).
4. Frontend Dashboard page showing the table, refreshed on load (a "real-time feel" without needing
   websockets - reloading a fast query on navigation/interval is enough at this scale).

## Phase 4 - Sales, Production, Centro, Materials screens

**Learn:** transactional multi-table writes (`ARCHITECTURE.md` section 4), enums mapped to Postgres
CHECK constraints, building simple CRUD-with-a-twist screens fast now that the pattern is familiar.

1. `SalesOrder`/`SalesOrderItem` + status transitions (BOOKED/IN_PROGRESS/DELIVERED/CANCELLED),
   cash-on-delivery: booking writes stock only, `DELIVERED` is what writes the `MoneyTransaction`,
   `CANCELLED` (only from BOOKED/IN_PROGRESS) reverses stock only - see `ARCHITECTURE.md` section 4.
2. `CentroShipment` (stock out) and `CentroIncome` (money in) - two independent screens/entities
   feeding one Centro page, per `ARCHITECTURE.md` section 2a. Don't try to link them.
3. `Materials` - simplest of all, a straightforward CRUD + one `MoneyTransaction` on create.
4. Frontend pages for each, plus the customer-info form (name/address/phone) for Sales.

## Phase 5 - Finance module

**Learn:** aggregating money in vs. out into a simple financial summary; modeling "founding
capital" separately from ongoing income/expense.

1. `MoneyTransaction`/`Investment` entities (mostly already written to by Phase 4's services).
2. A summary endpoint: total income, total expense, net, total invested, running balance
   (investment + net income/expense since inception).
3. Frontend finance page: the summary numbers plus a simple list of recent transactions.

## Phase 6 - Catalog for non-admins

**Learn:** role-based access control (RBAC) in Spring Security beyond "authenticated or not."

1. Revisit `SecurityConfig`: today, `/api/product-types/**` requires *any* authenticated user; add
   `.hasRole("ADMIN")` to every write endpoint and every non-catalog read endpoint, leaving catalog
   reads open to `ADMIN` or `VIEWER`.
2. Frontend: hide admin-only navigation/actions based on `AuthService.role()`.
   (Remember: hiding a button is a UX nicety, not security - the backend check from step 1 is what
   actually protects the data. Never rely on the frontend alone to enforce a permission.)

## Phase 7 - Dockerize fully + deploy

**Learn:** the difference between a dev and prod build, environment variables in a hosted platform,
what a health check is for.

1. Confirm `docker compose up --build` still works end-to-end after all the above.
2. Create a Railway (or Render) project: one Postgres service, one service from `backend/Dockerfile`,
   one from `frontend/Dockerfile` (or serve the Angular build as a static site if the platform
   offers that directly - simpler than a whole nginx container).
3. Set the same environment variables `docker-compose.yml` sets locally (`SPRING_DATASOURCE_URL`,
   `APP_JWT_SECRET` - **generate a new, real secret, don't reuse the local-dev one**) in the
   platform's dashboard instead.
4. Update `SecurityConfig`'s CORS `allowedOrigins` (if you end up not routing everything through
   nginx) and `frontend/nginx.conf`'s `proxy_pass` target to point at the deployed backend's URL.
5. Share the URL with your coworker, log in, celebrate.

## Later ideas (not scoped yet - ask when you get here)

- More product types beyond sweaters (the schema already supports this - it's just data).
- Reports/exports (CSV of transactions, monthly summaries).
- Basic tests (a good moment to learn JUnit + Testcontainers for the backend, and Angular's Karma
  setup for the frontend - we skipped these in the scaffold to focus on the architecture first).
