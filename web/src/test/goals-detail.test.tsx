import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClientProvider, QueryClient } from '@tanstack/react-query'
import React from 'react'
import { createTestQueryClient, mockApiClient } from './utils'

vi.mock('@tanstack/react-router', () => ({
  createFileRoute: () => (opts: Record<string, unknown>) => ({
    ...opts,
    options: opts,
    useParams: () => ({ goalId: 'g1' }),
  }),
  createRootRoute: () => ({ options: { component: () => null } }),
  Link: ({ children, to }: { children?: React.ReactNode; to?: string }) =>
    React.createElement('a', { href: to }, children),
  Outlet: () => null,
  useNavigate: () => vi.fn(),
  useParams: () => ({ goalId: 'g1' }),
  useRouterState: () => ({ location: { pathname: '/goals/g1' } }),
  redirect: () => undefined,
  lazyRouteComponent: (fn: () => unknown) => fn(),
}))

import { Route } from '../routes/_authenticated.goals.$goalId'

describe('Goal Detail Page', () => {
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

  it('renders goal detail with progress', async () => {
    mockApiClient.mockImplementation((url: string) => {
      if (url?.includes('/api/goals/g1')) {
        return Promise.resolve({
          id: 'g1',
          name: 'Vacation Fund',
          targetAmount: 3000,
          currentAmount: 1500,
          progressPercent: 50,
          targetDate: '2025-12-31',
          users: [{ id: 'u1', displayName: 'Alice' }],
          transactionIds: [],
          createdAt: '2025-01-01T00:00:00Z',
          updatedAt: '2025-06-15T00:00:00Z',
        })
      }
      return Promise.reject(new Error('Unknown URL'))
    })

    renderPage()

    await vi.waitFor(() => {
      expect(screen.getByText('Vacation Fund')).toBeTruthy()
    })
  })
})
