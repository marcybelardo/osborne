import { createFileRoute } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../api/client'

export const Route = createFileRoute('/_authenticated/dashboard')({
  component: Dashboard,
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

interface BudgetResponse {
  id: string
  name: string
  amount: number
  spent: number
}

interface BudgetPage {
  content: BudgetResponse[]
}

function Dashboard() {
  const accountsQuery = useQuery({
    queryKey: ['accounts'],
    queryFn: () =>
      apiClient<AccountPage>('/api/accounts?page=0&size=20'),
  })

  const budgetsQuery = useQuery({
    queryKey: ['budgets'],
    queryFn: () =>
      apiClient<BudgetPage>('/api/budgets?page=0&size=20'),
  })

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
      <p className="mt-1 text-sm text-gray-500">
        Overview of your accounts and budgets
      </p>

      <div className="mt-8 grid gap-8 lg:grid-cols-2">
        <section>
          <h2 className="mb-3 text-lg font-semibold text-gray-900">
            Accounts
          </h2>
          {accountsQuery.isPending && (
            <p className="text-sm text-gray-500">Loading accounts...</p>
          )}
          {accountsQuery.isError && (
            <p className="text-sm text-red-600">Failed to load accounts.</p>
          )}
          {accountsQuery.data && accountsQuery.data.content.length === 0 && (
            <p className="text-sm text-gray-500">No accounts yet.</p>
          )}
          {accountsQuery.data && accountsQuery.data.content.length > 0 && (
            <ul className="divide-y divide-gray-200 rounded-lg border border-gray-200 bg-white">
              {accountsQuery.data.content.map((account) => (
                <li key={account.id} className="px-4 py-3">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-medium text-gray-900">
                        {account.name}
                      </p>
                      <p className="text-xs text-gray-500">
                        {formatType(account.type)} · {account.currency}
                      </p>
                    </div>
                    <p className="text-sm font-semibold text-gray-900">
                      {formatCurrency(account.currentBalance, account.currency)}
                    </p>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section>
          <h2 className="mb-3 text-lg font-semibold text-gray-900">
            Budgets
          </h2>
          {budgetsQuery.isPending && (
            <p className="text-sm text-gray-500">Loading budgets...</p>
          )}
          {budgetsQuery.isError && (
            <p className="text-sm text-red-600">Failed to load budgets.</p>
          )}
          {budgetsQuery.data && budgetsQuery.data.content.length === 0 && (
            <p className="text-sm text-gray-500">No budgets yet.</p>
          )}
          {budgetsQuery.data && budgetsQuery.data.content.length > 0 && (
            <ul className="divide-y divide-gray-200 rounded-lg border border-gray-200 bg-white">
              {budgetsQuery.data.content.map((budget) => (
                <li key={budget.id} className="px-4 py-3">
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-medium text-gray-900">
                      {budget.name}
                    </p>
                    <div className="text-right">
                      <p className="text-sm font-semibold text-gray-900">
                        {formatCurrency(budget.spent, 'USD')}
                      </p>
                      <p className="text-xs text-gray-500">
                        of {formatCurrency(budget.amount, 'USD')}
                      </p>
                    </div>
                  </div>
                  <div className="mt-2 h-2 w-full rounded-full bg-gray-100">
                    <div
                      className="h-2 rounded-full bg-indigo-500"
                      style={{
                        width: `${Math.min(
                          (budget.spent / budget.amount) * 100,
                          100,
                        )}%`,
                      }}
                    />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
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

function formatCurrency(amount: number, _currency: string): string {
  return `$${amount.toFixed(2)}`
}
