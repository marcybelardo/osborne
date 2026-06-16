import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'
import React from 'react'
import { createTestQueryClient } from './utils'

vi.mock('@tanstack/react-router', () => ({
  createFileRoute: () => (opts: Record<string, unknown>) => ({ ...opts, options: opts }),
  createRootRoute: () => ({ options: { component: () => null } }),
  Link: ({ children, to }: { children?: React.ReactNode; to?: string }) =>
    React.createElement('a', { href: to }, children),
  Outlet: () => null,
  useNavigate: () => vi.fn(),
  useParams: () => ({}),
  useRouterState: () => ({ location: { pathname: '/' } }),
  redirect: () => undefined,
  lazyRouteComponent: (fn: () => unknown) => fn(),
  useMatchRoute: () => vi.fn(),
  useSearch: () => ({}),
  useLoaderData: () => ({}),
}))

vi.mock('../lib/auth', () => ({
  isAuthenticated: () => false,
  setTokens: vi.fn(),
}))

import { Route } from '../routes/index'

describe('Index (Landing) Page', () => {
  const queryClient = createTestQueryClient()

  function renderPage() {
    const Component = Route.options.component!
    return render(
      <QueryClientProvider client={queryClient}>
        <Component />
      </QueryClientProvider>,
    )
  }

  it('renders the landing page with title', () => {
    renderPage()
    expect(screen.getByText('Osborne')).toBeTruthy()
    expect(screen.getByText('Budget Manager')).toBeTruthy()
  })

  it('renders feature cards', () => {
    renderPage()
    expect(screen.getByText('Budgeting')).toBeTruthy()
    expect(screen.getByText('Account Management')).toBeTruthy()
    expect(screen.getByText('Shared Accounts')).toBeTruthy()
  })

  it('renders login form', () => {
    renderPage()
    const logins = screen.getAllByText('Log in')
    expect(logins.length).toBeGreaterThanOrEqual(1)
    expect(screen.getByLabelText('Email')).toBeTruthy()
    expect(screen.getByLabelText('Password')).toBeTruthy()
    expect(screen.getByPlaceholderText('you@example.com')).toBeTruthy()
  })
})
