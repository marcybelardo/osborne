import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, type RenderOptions } from '@testing-library/react'
import { vi } from 'vitest'
import React from 'react'

// Mock apiClient
export const mockApiClient = vi.fn()

vi.mock('../api/client', () => ({
  apiClient: mockApiClient,
  ApiError: class extends Error {
    status: number
    constructor(status: number, message: string) {
      super(message)
      this.status = status
      this.name = 'ApiError'
    }
  },
}))

// Create a test QueryClient that doesn't retry
export function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, staleTime: 0 },
      mutations: { retry: false },
    },
  })
}

interface WrapperOptions {
  queryClient?: QueryClient
}

export function createWrapper(options: WrapperOptions = {}) {
  const queryClient = options.queryClient ?? createTestQueryClient()

  return function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        {children}
      </QueryClientProvider>
    )
  }
}

// Custom render with providers
export function renderWithClient(
  ui: React.ReactElement,
  options: RenderOptions = {},
) {
  const queryClient = createTestQueryClient()
  const Wrapper = createWrapper({ queryClient })

  return {
    ...render(ui, { wrapper: Wrapper, ...options }),
    queryClient,
  }
}

export { render }
