import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClientProvider, QueryClient } from '@tanstack/react-query'
import React from 'react'
import { createTestQueryClient, mockApiClient } from './utils'

vi.mock('@tanstack/react-router', () => ({
  createFileRoute: () => (opts: Record<string, unknown>) => ({
    ...opts,
    options: opts,
    useParams: () => ({ budgetId: 'b1' }),
  }),
  createRootRoute: () => ({ options: { component: () => null } }),
  Link: ({ children, to }: { children?: React.ReactNode; to?: string }) =>
    React.createElement('a', { href: to }, children),
  Outlet: () => null,
  useNavigate: () => vi.fn(),
  useParams: () => ({ budgetId: 'b1' }),
  useRouterState: () => ({ location: { pathname: '/budgets/b1' } }),
  redirect: () => undefined,
  lazyRouteComponent: (fn: () => unknown) => fn(),
}))

import { Route } from '../routes/_authenticated.budgets.$budgetId'

describe('Budget Detail Page', () => {
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

  it('renders budget detail with name', async () => {
    mockApiClient.mockImplementation((url: string) => {
      if (url?.includes('/api/budgets/b1')) {
        return Promise.resolve({
          id: 'b1',
          name: 'Groceries',
          description: 'Monthly grocery budget',
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
        })
      }
      return Promise.reject(new Error('Unknown URL'))
    })

    renderPage()

    await vi.waitFor(() => {
      expect(screen.getByText('Groceries')).toBeTruthy()
    })
  })
})
