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
  useRouterState: () => ({ location: { pathname: '/accounts' } }),
  redirect: () => undefined,
  lazyRouteComponent: (fn: () => unknown) => fn(),
}))

import { Route } from '../routes/_authenticated.accounts'

describe('Accounts List Page', () => {
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

  it('renders page title and new account button', () => {
    renderPage()
    expect(screen.getByText('Accounts')).toBeTruthy()
    expect(screen.getByText('New Account')).toBeTruthy()
  })

  it('shows loading state', () => {
    renderPage()
    expect(screen.getByText('Loading accounts...')).toBeTruthy()
  })

  it('shows empty state when no accounts', async () => {
    mockApiClient.mockResolvedValue({ content: [] })

    renderPage()

    await vi.waitFor(() => {
      expect(screen.getByText('No accounts yet.')).toBeTruthy()
    })
  })

  it('renders shared account badges', async () => {
    mockApiClient.mockResolvedValue({
      content: [
        {
          id: '1',
          name: 'Joint Checking',
          type: 'ASSET',
          currency: 'USD',
          initialBalance: 5000,
          currentBalance: 5200,
          users: [
            { id: 'u1', displayName: 'Alice' },
            { id: 'u2', displayName: 'Bob' },
          ],
        },
        {
          id: '2',
          name: 'My Wallet',
          type: 'ASSET',
          currency: 'USD',
          initialBalance: 100,
          currentBalance: 200,
          users: [{ id: 'u1', displayName: 'Alice' }],
        },
      ],
    })

    renderPage()

    await vi.waitFor(() => {
      expect(screen.getByText('Joint Checking')).toBeTruthy()
      expect(screen.getByText('My Wallet')).toBeTruthy()
      expect(screen.getByText(/Shared with 1 other/)).toBeTruthy()
    })
  })
})
