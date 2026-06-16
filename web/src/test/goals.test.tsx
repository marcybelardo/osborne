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
  useRouterState: () => ({ location: { pathname: '/goals' } }),
  redirect: () => undefined,
  lazyRouteComponent: (fn: () => unknown) => fn(),
}))

import { Route } from '../routes/_authenticated.goals'

describe('Goals List Page', () => {
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

  it('renders page title and new goal button', () => {
    renderPage()
    const goals = screen.getAllByText('Goals')
    expect(goals.length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText('New Goal')).toBeTruthy()
  })

  it('shows loading state', () => {
    renderPage()
    expect(screen.getByText('Loading goals...')).toBeTruthy()
  })

  it('shows empty state when no goals', async () => {
    mockApiClient.mockResolvedValue({ content: [] })

    renderPage()

    await vi.waitFor(() => {
      expect(screen.getByText('No goals yet.')).toBeTruthy()
    })
  })

  it('renders goals with progress bars', async () => {
    mockApiClient.mockResolvedValue({
      content: [
        {
          id: 'g1',
          name: 'Vacation Fund',
          targetAmount: 3000,
          currentAmount: 1500,
          progressPercent: 50,
          targetDate: '2025-12-31',
          users: [{ id: 'u1', displayName: 'Alice' }],
          transactionIds: [],
        },
        {
          id: 'g2',
          name: 'Emergency Fund',
          targetAmount: 10000,
          currentAmount: 2000,
          progressPercent: 20,
          targetDate: null,
          users: [{ id: 'u1', displayName: 'Alice' }],
          transactionIds: [],
        },
      ],
    })

    renderPage()

    await vi.waitFor(() => {
      expect(screen.getByText('Vacation Fund')).toBeTruthy()
      expect(screen.getByText('Emergency Fund')).toBeTruthy()
      expect(screen.getByText('50%')).toBeTruthy()
    })
  })
})
