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
}))

import { Route } from '../routes/register'

describe('Register Page', () => {
  const queryClient = createTestQueryClient()

  function renderPage() {
    const Component = Route.options.component!
    return render(
      <QueryClientProvider client={queryClient}>
        <Component />
      </QueryClientProvider>,
    )
  }

  it('renders registration form title', () => {
    renderPage()
    expect(screen.getByText('Create an account')).toBeTruthy()
  })

  it('renders all form fields', () => {
    renderPage()
    expect(screen.getByLabelText('Display name')).toBeTruthy()
    expect(screen.getByLabelText('Email')).toBeTruthy()
    expect(screen.getByLabelText('Password')).toBeTruthy()
  })

  it('renders register button', () => {
    renderPage()
    expect(screen.getByText('Register')).toBeTruthy()
  })

  it('renders link to log in', () => {
    renderPage()
    expect(screen.getByText('Log in')).toBeTruthy()
  })
})
