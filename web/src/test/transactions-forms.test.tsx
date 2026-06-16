import { describe, it, expect, vi } from 'vitest'
import React from 'react'

vi.mock('@tanstack/react-router', () => ({
  createFileRoute: () => (opts: Record<string, unknown>) => ({ ...opts, options: opts }),
  createRootRoute: () => ({ options: { component: () => null } }),
  Link: ({ children, to }: { children?: React.ReactNode; to?: string }) =>
    React.createElement('a', { href: to }, children),
  Outlet: () => null,
  useNavigate: () => vi.fn(),
  useParams: () => ({ accountId: '123' }),
  useRouterState: () => ({ location: { pathname: '/' } }),
  redirect: () => undefined,
  lazyRouteComponent: (fn: () => unknown) => fn(),
}))

vi.mock('../api/client', () => ({
  apiClient: vi.fn(),
  ApiError: class extends Error {
    status: number
    constructor(status: number, message: string) {
      super(message)
      this.status = status
      this.name = 'ApiError'
    }
  },
}))

vi.mock('../lib/auth', () => ({
  getAccessToken: () => null,
  setTokens: vi.fn(),
  clearTokens: vi.fn(),
  isAuthenticated: () => false,
}))

describe('Transaction Forms', () => {
  describe('Create Transaction Form', () => {
    it('imports and defines Route', async () => {
      const mod = await import('../routes/_authenticated.accounts.$accountId.transactions.new')
      expect(mod.Route).toBeDefined()
    })
  })

  describe('Edit Transaction Form', () => {
    it('imports and defines Route', async () => {
      const mod = await import('../routes/_authenticated.accounts.$accountId.transactions.$transactionId.edit')
      expect(mod.Route).toBeDefined()
    })
  })
})
