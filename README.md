# Stock App

Inventory + finance tracking for a sweater entrepreneurship - and a learning project for Java
(Spring Boot 4), Angular (20), PostgreSQL and Docker.

Start with **`ARCHITECTURE.md`** (why it's built this way) and **`ROADMAP.md`** (what to build next,
phase by phase). This README just covers running what already exists.

## Prerequisites

- Docker + Docker Compose (easiest way to run everything)
- For running backend/frontend outside Docker during development: Java 21, Maven (or use the
  included `./mvnw`), Node 22+, npm

## Run everything with Docker Compose

```bash
docker compose up --build
```

- Frontend: http://localhost:4200
- Backend API: http://localhost:8080 (health check: http://localhost:8080/actuator/health)
- Postgres: localhost:5432 (user/password/db: `stockapp`)

First time only: you need at least one user row to log in - see **Phase 1** in `ROADMAP.md` for how
to create one safely (there's a shell-quoting gotcha with BCrypt hashes, explained there).

## Run backend and frontend separately (faster local development)

Backend (needs Postgres running - `docker compose up db` starts just the database):

```bash
cd backend
./mvnw spring-boot:run
```

Frontend (proxies `/api/*` to `localhost:8080` automatically - see `proxy.conf.json`):

```bash
cd frontend
npm install
npm start
```

Then open http://localhost:4200.

## Project layout

```
backend/    Spring Boot API (Java 21, Maven, PostgreSQL via Flyway, JWT auth)
frontend/   Angular 20 app (standalone components, signals, Tailwind CSS)
.github/workflows/ci.yml   builds + tests both projects on every push (see ARCHITECTURE.md §11)
docker-compose.yml   wires db + backend + frontend together
ARCHITECTURE.md       why the project is structured this way
ROADMAP.md            what to build next, phase by phase
```

## Pushing this to your own GitHub repo

```bash
git init
git add .
git commit -m "Initial project scaffold"
git branch -M main
git remote add origin <your-empty-github-repo-url>
git push -u origin main
```
