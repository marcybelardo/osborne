import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClientProvider, QueryClient } from '@tanstack/react-query'
import React from 'react'
import { createTestQueryClient, mockApiClient } from './utils'

vi.mock('@tanstack/react-router', () => ({
  createFileRoute: () => (opts: Record<string, unknown>) => ({ ...opts, options: opts }),
  createRootRoute: () => ({ options: { component: () => null } }),
  Link: ({ children, to }: { children?: React.ReactNode; to?: string }) =>
    React.createElement('a', { href: to }, children),
  Outlet: () => null,
  useNavigate: () => vi.fn(),
  useParams: () => ({}),
  useRouterState: () => ({ location: { pathname: '/budgets' } }),
  redirect: () => undefined,
  lazyRouteComponent: (fn: () => unknown) => fn(),
}))

import { Route } from '../routes/_authenticated.budgets'

describe('Budgets List Page', () => {
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

  it('renders page title and new budget button', () => {
    renderPage()
    const budgets = screen.getAllByText('Budgets')
    expect(budgets.length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText('New Budget')).toBeTruthy()
  })

  it('shows loading state', () => {
    renderPage()
    expect(screen.getByText('Loading budgets...')).toBeTruthy()
  })

  it('shows empty state when no budgets', async () => {
    mockApiClient.mockResolvedValue({ content: [] })

    renderPage()

    await vi.waitFor(() => {
      expect(screen.getByText('No budgets yet.')).toBeTruthy()
    })
  })

  it('renders budgets with progress bars and timeframe labels', async () => {
    mockApiClient.mockResolvedValue({
      content: [
        {
          id: 'b1',
          name: 'Groceries',
          description: null,
          timeframe: 'MONTHLY',
          startDate: null,
          endDate: null,
          periodStart: '2025-06-01',
          periodEnd: '2025-06-30',
          periodLabel: 'June 2025',
          amount: 600,
          currentSpending: 300,
          users: [{ id: 'u1', displayName: 'Alice' }],
          transactionIds: [],
          createdAt: '2025-06-01T00:00:00Z',
          updatedAt: '2025-06-15T00:00:00Z',
        },
      ],
    })

    renderPage()

    await vi.waitFor(() => {
      expect(screen.getByText('Groceries')).toBeTruthy()
      expect(screen.getByText(/Monthly/)).toBeTruthy()
    })
  })
})
