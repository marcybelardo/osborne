import { createFileRoute, Outlet, redirect } from '@tanstack/react-router'
import { isAuthenticated } from '../lib/auth'

export const Route = createFileRoute('/_authenticated')({
  beforeLoad: async () => {
    if (!isAuthenticated()) {
      throw redirect({ to: '/' })
    }
  },
  component: AuthenticatedLayout,
})

function AuthenticatedLayout() {
  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-b border-gray-200 bg-white">
        <div className="mx-auto max-w-4xl flex items-center justify-between px-4 py-3">
          <a
            href="/dashboard"
            className="text-lg font-semibold text-gray-900"
          >
            Osborne
          </a>
          <nav className="flex items-center gap-4">
            <a
              href="/dashboard"
              className="text-sm text-gray-600 hover:text-gray-900"
            >
              Dashboard
            </a>
            <a
              href="/logout"
              className="text-sm text-gray-600 hover:text-gray-900"
            >
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
