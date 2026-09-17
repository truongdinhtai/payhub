# PayHub Web (Angular)

Frontend for the PayHub SaaS Subscription Billing Platform. Angular 20 (standalone
components), Angular Material, HttpClient + interceptor, Reactive Forms, RxJS/signals for
state (no NgRx).

## Features

- Google OAuth2 login (redirect flow via the backend; JWT read from the callback fragment)
- Dashboard: current plan, renewal date, usage limits, cancel
- Plans: choose / upgrade, with Stripe Checkout redirect for paid plans
- Transactions: full-text search + status/date filters, paginated table
- Settings: edit name, view billing summary
- Auth interceptor: attaches the Bearer token and redirects to `/login` on 401

## Run locally

```bash
npm install
npm start          # ng serve on http://localhost:4200
```

Requires the backend on `http://localhost:8080` (see `../Backend`). The API base URL is set
per environment in `src/environments/`:

- `environment.ts` — local (`http://localhost:8080`)
- `environment.prod.ts` — production build; edit `apiBaseUrl` to your deployed backend

## Build & Docker

```bash
npm run build              # production build to dist/payhub-web/browser
docker build -t payhub-web .   # multi-stage: Node build -> Nginx
```

Or run the whole stack (db + api + web) from the repo root:

```bash
docker compose up --build   # web on http://localhost:4200
```

## Auth flow

`Login` → backend `/oauth2/authorization/google` → Google → backend →
redirect to `/oauth2/callback#token=<jwt>` → token stored in `localStorage` → `/dashboard`.

## Structure

```
src/app/
├── core/          # models, services (auth/subscription/payment), interceptor, guard
├── layout/        # shell (toolbar + nav)
└── features/      # login, oauth-callback, dashboard, plans, transactions, settings
```
