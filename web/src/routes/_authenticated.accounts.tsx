import { createFileRoute, Link, Outlet, useRouterState } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import { Plus } from 'lucide-react'

export const Route = createFileRoute('/_authenticated/accounts')({
  component: AccountsLayout,
})

interface AccountResponse {
  id: string
  name: string
  type: string
  currency: string
  initialBalance: number
  currentBalance: number
  userIds: string[]
}

interface AccountPage {
  content: AccountResponse[]
}

function AccountsLayout() {
  const routerState = useRouterState()
  const isIndex = routerState.location.pathname === '/accounts'
  const accountsQuery = useQuery({
    queryKey: ['accounts'],
    queryFn: () => apiClient<AccountPage>('/api/accounts?page=0&size=50'),
    enabled: isIndex,
  })

  if (!isIndex) {
    return <Outlet />
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Accounts</h1>
          <p className="mt-1 text-sm text-gray-500">
            Manage your accounts and transactions
          </p>
        </div>
        <Link
          to="/accounts/new"
          className="inline-flex items-center gap-1.5 rounded-md bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500"
        >
          <Plus size={16} />
          New Account
        </Link>
      </div>

      <div className="mt-8">
        {accountsQuery.isPending && (
          <p className="text-sm text-gray-500">Loading accounts...</p>
        )}
        {accountsQuery.isError && (
          <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
            Failed to load accounts. Please try again.
          </div>
        )}
        {accountsQuery.data && accountsQuery.data.content.length === 0 && (
          <div className="rounded-lg border border-gray-200 bg-white p-12 text-center">
            <p className="text-sm text-gray-500">No accounts yet.</p>
            <p className="mt-1 text-xs text-gray-400">
              Create your first account to get started.
            </p>
          </div>
        )}
        {accountsQuery.data && accountsQuery.data.content.length > 0 && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {accountsQuery.data.content.map((account) => (
              <Link
                key={account.id}
                to="/accounts/$accountId"
                params={{ accountId: account.id }}
                className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm transition hover:border-indigo-300 hover:shadow-md"
              >
                <div className="flex items-start justify-between">
                  <div>
                    <p className="text-sm font-semibold text-gray-900">
                      {account.name}
                    </p>
                    <p className="mt-0.5 text-xs text-gray-500">
                      {formatType(account.type)} · {account.currency}
                    </p>
                  </div>
                </div>
                <p
                  className={`mt-4 text-lg font-bold ${
                    account.currentBalance >= 0
                      ? 'text-gray-900'
                      : 'text-red-600'
                  }`}
                >
                  {formatCurrency(account.currentBalance)}
                </p>
                {account.userIds.length > 1 && (
                  <p className="mt-2 text-xs text-gray-400">
                    Shared with {account.userIds.length - 1} other
                    {account.userIds.length - 1 !== 1 ? 's' : ''}
                  </p>
                )}
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

function formatType(type: string): string {
  return type
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase())
}

function formatCurrency(amount: number): string {
  return `$${amount.toFixed(2)}`
}
