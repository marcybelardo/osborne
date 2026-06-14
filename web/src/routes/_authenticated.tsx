import { createFileRoute, Outlet, redirect } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { isAuthenticated } from '../lib/auth'
import { apiClient } from '../api/client'

export const Route = createFileRoute('/_authenticated')({
  beforeLoad: async () => {
    if (!isAuthenticated()) {
      throw redirect({ to: '/' })
    }
  },
  component: AuthenticatedLayout,
})

function AuthenticatedLayout() {
  const pendingQuery = useQuery({
    queryKey: ['reminders', 'pending-count'],
    queryFn: () => apiClient<number>('/api/reminders/pending/count'),
  })

  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-b border-gray-200 bg-white">
        <div className="mx-auto max-w-4xl flex items-center justify-between px-4 py-3">
          <a href="/dashboard" className="text-lg font-semibold text-gray-900">
            Osborne
          </a>
          <nav className="flex items-center gap-4">
            <a href="/dashboard" className="flex items-center gap-1 text-sm text-gray-600 hover:text-gray-900">
              Dashboard
              {pendingQuery.data != null && pendingQuery.data > 0 && (
                <span className="rounded-full bg-amber-100 px-1.5 py-0.5 text-xs font-medium text-amber-800">
                  {pendingQuery.data}
                </span>
              )}
            </a>
            <a href="/logout" className="text-sm text-gray-600 hover:text-gray-900">
              Log out
            </a>
          </nav>
        </div>
      </header>
      <main className="flex-1">
        <Outlet />
      </main>
    </div>
  )
}
