# Osborne

A family-first budget manager with shared accounts, goals, and reminders.

## Features

- **Shared accounts** — invite others to manage checking, savings, and credit accounts together
- **Shared budgets** — set spending limits and track progress across household members
- **Savings goals** — track progress toward financial targets with deadlines and contributions
- **Transaction tracking** — record income and expenses with categories
- **Reminders** — get notified about bill discrepancies and goal milestones
- **Simple, fast UI** — built for daily use, not power-user complexity

## Architecture

Osborne is a single-page application backed by a REST API:

```
Browser ──→ React SPA (Vite) ──→ Spring Boot REST API ──→ PostgreSQL
```

The backend is stateless (JWT auth) and the frontend communicates exclusively over JSON — no server-rendered pages, no session cookies.

## Quick Start (Docker)

```bash
docker compose up --build
```

This starts PostgreSQL, the API, and the web frontend. Open [http://localhost:3000](http://localhost:3000).

To run in the background:

```bash
docker compose up --build -d
docker compose down         # stop (keeps DB data)
docker compose down -v      # stop and wipe database
```

## Local Development

### Prerequisites

- Java 21+
- Maven
- Node.js 22+
- pnpm
- PostgreSQL 16 (or Docker for the DB)

### API setup

```bash
cd api

# Start PostgreSQL (or use Docker: docker compose up db)
# Create the database:
createdb osborne

# Run the API
mvn spring-boot:run
```

The API starts on [http://localhost:8080](http://localhost:8080).

### Frontend setup

```bash
cd web
pnpm install
pnpm dev
```

The frontend starts on [http://localhost:3000](http://localhost:3000).

### Environment variables

Copy `.env.example` to `.env` and fill in your values:

| Variable | Description |
|---|---|
| `DB_URL` | JDBC URL for PostgreSQL |
| `DB_USER` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | 64-character hex string for JWT signing (generate with `openssl rand -hex 32`) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origins |
| `VITE_API_URL` | API base URL for the frontend |

## API Overview

### Authentication

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Log in (returns JWT + refresh token) |
| `POST` | `/api/auth/refresh` | Refresh an expired access token |
| `POST` | `/api/auth/logout` | Revoke refresh token |

### Users

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/users/me` | Get current user profile |
| `POST` | `/api/users` | Create a user (admin only) |

### Accounts

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/accounts` | List accounts for current user |
| `GET` | `/api/accounts/{id}` | Get account by ID |
| `POST` | `/api/accounts` | Create an account |
| `PUT` | `/api/accounts/{id}` | Update an account |
| `DELETE` | `/api/accounts/{id}` | Delete an account |
| `GET` | `/api/accounts/{id}/users` | List users on an account |
| `POST` | `/api/accounts/{id}/users` | Add a user to an account |
| `DELETE` | `/api/accounts/{id}/users/{userId}` | Remove a user from an account |

### Transactions

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/accounts/{id}/transactions` | List transactions for an account |
| `GET` | `/api/accounts/{id}/transactions/{txId}` | Get a transaction by ID |
| `POST` | `/api/accounts/{id}/transactions` | Create a transaction |
| `PUT` | `/api/accounts/{id}/transactions/{txId}` | Update a transaction |
| `DELETE` | `/api/accounts/{id}/transactions/{txId}` | Delete a transaction |

### Budgets

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/budgets` | List budgets for current user |
| `GET` | `/api/budgets/{id}` | Get budget by ID |
| `POST` | `/api/budgets` | Create a budget |
| `PUT` | `/api/budgets/{id}` | Update a budget |
| `DELETE` | `/api/budgets/{id}` | Delete a budget |
| `GET` | `/api/budgets/{id}/users` | List users on a budget |
| `POST` | `/api/budgets/{id}/users` | Add a user to a budget |
| `DELETE` | `/api/budgets/{id}/users/{userId}` | Remove a user from a budget |
| `GET` | `/api/budgets/{id}/transactions` | List transactions in a budget |
| `POST` | `/api/budgets/{id}/transactions` | Add a transaction to a budget |
| `DELETE` | `/api/budgets/{id}/transactions/{txId}` | Remove a transaction from a budget |

### Goals

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/goals` | List goals for current user |
| `GET` | `/api/goals/{id}` | Get goal by ID |
| `POST` | `/api/goals` | Create a goal |
| `PUT` | `/api/goals/{id}` | Update a goal |
| `DELETE` | `/api/goals/{id}` | Delete a goal |
| `GET` | `/api/goals/{id}/users` | List users on a goal |
| `POST` | `/api/goals/{id}/users` | Add a user to a goal |
| `DELETE` | `/api/goals/{id}/users/{userId}` | Remove a user from a goal |
| `GET` | `/api/goals/{id}/transactions` | List transactions in a goal |
| `POST` | `/api/goals/{id}/transactions` | Add a transaction to a goal |
| `DELETE` | `/api/goals/{id}/transactions/{txId}` | Remove a transaction from a goal |

### Reminders

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/reminders` | List reminders (filterable by status) |
| `GET` | `/api/reminders/pending/count` | Count pending reminders |
| `PUT` | `/api/reminders/{id}/acknowledge` | Mark a reminder as acknowledged |
| `PUT` | `/api/reminders/{id}/dismiss` | Mark a reminder as dismissed |

### Health

| Method | Path | Description |
|---|---|---|
| `GET` | `/actuator/health` | Health check (DB connectivity, disk space) |
| `GET` | `/actuator/info` | Application info (name, version) |

## Tech Stack

### Backend

- **Spring Boot 4** — application framework
- **PostgreSQL** — database
- **Flyway** — schema migrations
- **JJWT** — JWT token handling
- **Lombok** — boilerplate reduction

### Frontend

- **React 19** — UI library
- **Vite 8** — build tool
- **TanStack Router** — file-based routing
- **TanStack Query** — server state management
- **Tailwind CSS 4** — utility-first styling

## Testing

```bash
# Backend
cd api
mvn test

# Frontend
cd web
pnpm test
```

## License

MIT
