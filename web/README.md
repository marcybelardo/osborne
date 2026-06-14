# Osborne Web Frontend

React single-page application for the Osborne budget manager.

## Quick Start

```bash
pnpm install
pnpm dev
```

Opens on [http://localhost:3000](http://localhost:3000).

## Tech Stack

- React 19
- Vite 8
- TanStack Router (file-based routing)
- TanStack Query (data fetching)
- Tailwind CSS 4 (styling)
- Lucide React (icons)

## Project Structure

```
src/
├── main.tsx               # App entry point (QueryClientProvider + RouterProvider)
├── styles.css              # Tailwind imports and base styles
├── api/
│   └── client.ts           # Fetch wrapper with auth header + 401 interceptor
├── lib/
│   ├── auth.ts             # Token storage (localStorage)
│   └── query.ts            # QueryClient configuration
├── routes/
│   ├── __root.tsx          # Root layout + devtools
│   ├── index.tsx           # Landing page with login
│   ├── register.tsx        # Registration form
│   ├── _authenticated.tsx  # Auth gate layout + nav bar
│   ├── _authenticated.dashboard.tsx  # Accounts, budgets, goals, reminders
│   └── _authenticated.logout.tsx     # Logout (clears tokens)
└── routeTree.gen.ts        # Auto-generated route tree (do not edit)
```

## Scripts

| Command | Description |
|---|---|
| `pnpm dev` | Start dev server on port 3000 |
| `pnpm build` | Build for production |
| `pnpm preview` | Preview production build |
| `pnpm test` | Run tests with Vitest |
| `pnpm run generate-routes` | Regenerate route tree from file-based routes |
