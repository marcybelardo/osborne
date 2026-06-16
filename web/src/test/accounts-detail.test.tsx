import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClientProvider, QueryClient } from '@tanstack/react-query'
import React from 'react'
import { createTestQueryClient, mockApiClient } from './utils'

vi.mock('@tanstack/react-router', () => ({
  createFileRoute: () => (opts: Record<string, unknown>) => ({
    ...opts,
    options: opts,
    useParams: () => ({ accountId: '123' }),
  }),
  createRootRoute: () => ({ options: { component: () => null } }),
  Link: ({ children, to }: { children?: React.ReactNode; to?: string }) =>
    React.createElement('a', { href: to }, children),
  Outlet: () => null,
  useNavigate: () => vi.fn(),
  useParams: () => ({ accountId: '123' }),
  useRouterState: () => ({ location: { pathname: '/accounts/123' } }),
  redirect: () => undefined,
  lazyRouteComponent: (fn: () => unknown) => fn(),
}))

import { Route } from '../routes/_authenticated.accounts.$accountId'

describe('Account Detail Page', () => {
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

  it('shows loading state', () => {
    renderPage()
    expect(screen.getByText('Loading account...')).toBeTruthy()
  })

  it('shows error state when query fails', async () => {
    mockApiClient.mockRejectedValue(new Error('Failed to load'))
    renderPage()

    await vi.waitFor(() => {
      expect(screen.getByText('Failed to load account.')).toBeTruthy()
    })
  })

  it('renders account detail with balance and transactions', async () => {
    mockApiClient.mockImplementation((url: string) => {
      if (url?.includes('/api/accounts/')) {
        return Promise.resolve({
          id: '123',
          name: 'Checking',
          type: 'ASSET',
          currency: 'USD',
          initialBalance: 1000,
          currentBalance: 1500,
          users: [{ id: 'u1', displayName: 'Alice' }],
          createdAt: '2025-01-01T00:00:00Z',
          updatedAt: '2025-06-01T00:00:00Z',
        })
      }
      if (url?.includes('/transactions')) {
        return Promise.resolve({
          content: [
            {
              id: 'tx1',
              amount: -50,
              description: 'Groceries',
              category: 'Food',
              transactionDate: '2025-06-15',
              accountId: '123',
              budgetIds: [],
              goalIds: [],
              createdAt: '2025-06-15T00:00:00Z',
              updatedAt: '2025-06-15T00:00:00Z',
            },
          ],
        })
      }
      return Promise.reject(new Error('Unknown URL'))
    })

    renderPage()

    await vi.waitFor(() => {
      expect(screen.getByText('Checking')).toBeTruthy()
      expect(screen.getByText('Current Balance')).toBeTruthy()
      expect(screen.getByText('Initial Balance')).toBeTruthy()
    })
  })
})
