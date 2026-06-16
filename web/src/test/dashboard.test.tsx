import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClientProvider, QueryClient } from '@tanstack/react-query'
import React from 'react'
import { createTestQueryClient, mockApiClient } from './utils'

vi.mock('@tanstack/react-router', () => ({
  createFileRoute: () => (opts: Record<string, unknown>) => ({
    ...opts,
    options: opts,
    useParams: () => ({}),
  }),
  createRootRoute: () => ({ options: { component: () => null } }),
  Link: ({ children, to }: { children?: React.ReactNode; to?: string }) =>
    React.createElement('a', { href: to }, children),
  Outlet: () => null,
  useNavigate: () => vi.fn(),
  useParams: () => ({}),
  useRouterState: () => ({ location: { pathname: '/' } }),
  redirect: () => undefined,
  lazyRouteComponent: (fn: () => unknown) => fn(),
}))

import { Route } from '../routes/_authenticated.dashboard'

describe('Dashboard Page', () => {
  let queryClient: QueryClient

  beforeEach(() => {
    queryClient = createTestQueryClient()
    mockApiClient.mockReset()
  })

  function renderPage() {
    const Component = Route.options.component!
    return render(
      <QueryClientProvider client={queryClient}>
        <Component />
      </QueryClientProvider>,
    )
  }

  it('renders dashboard title', () => {
    renderPage()
    expect(screen.getByText('Dashboard')).toBeTruthy()
    expect(screen.getByText(/Overview of your accounts/)).toBeTruthy()
  })

  it('renders summary section links', () => {
    renderPage()
    expect(screen.getByText('Accounts')).toBeTruthy()
    const budgets = screen.getAllByText('Budgets')
    expect(budgets.length).toBeGreaterThanOrEqual(1)
    const goals = screen.getAllByText('Goals')
    expect(goals.length).toBeGreaterThanOrEqual(1)
  })

  it('shows loading state for accounts', () => {
    renderPage()
    expect(screen.getByText('Loading accounts...')).toBeTruthy()
  })

  it('renders quick-add transaction form', () => {
    renderPage()
    expect(screen.getByText('Quick Add Transaction')).toBeTruthy()
    expect(screen.getByText('Add Transaction')).toBeTruthy()
  })
})
