import { createFileRoute } from '@tanstack/react-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
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

interface GoalResponse {
  id: string
  name: string
  targetAmount: number
  currentAmount: number
  progressPercent: number
  targetDate: string | null
}

interface GoalPage {
  content: GoalResponse[]
}

interface ReminderResponse {
  id: string
  message: string
  status: string
  transactionId: string | null
}

interface ReminderPage {
  content: ReminderResponse[]
}

function Dashboard() {
  const queryClient = useQueryClient()

  const accountsQuery = useQuery({
    queryKey: ['accounts'],
    queryFn: () => apiClient<AccountPage>('/api/accounts?page=0&size=20'),
  })

  const budgetsQuery = useQuery({
    queryKey: ['budgets'],
    queryFn: () => apiClient<BudgetPage>('/api/budgets?page=0&size=20'),
  })

  const goalsQuery = useQuery({
    queryKey: ['goals'],
    queryFn: () => apiClient<GoalPage>('/api/goals?page=0&size=20'),
  })

  const remindersQuery = useQuery({
    queryKey: ['reminders'],
    queryFn: () =>
      apiClient<ReminderPage>('/api/reminders?status=PENDING&page=0&size=10'),
  })

  const pendingCountQuery = useQuery({
    queryKey: ['reminders', 'pending-count'],
    queryFn: () => apiClient<number>('/api/reminders/pending/count'),
  })

  const acknowledgeMutation = useMutation({
    mutationFn: (id: string) =>
      apiClient(`/api/reminders/${id}/acknowledge`, { method: 'PUT' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reminders'] })
    },
  })

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
          <p className="mt-1 text-sm text-gray-500">
            Overview of your accounts, budgets, and goals
          </p>
        </div>
        {pendingCountQuery.data != null && pendingCountQuery.data > 0 && (
          <span className="rounded-full bg-amber-100 px-3 py-1 text-xs font-medium text-amber-800">
            {pendingCountQuery.data} reminder{pendingCountQuery.data !== 1 ? 's' : ''}
          </span>
        )}
      </div>

      <div className="mt-8 grid gap-8 lg:grid-cols-2">
        <section>
          <h2 className="mb-3 text-lg font-semibold text-gray-900">Accounts</h2>
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
              {accountsQuery.data.content.map((a) => (
                <li key={a.id} className="px-4 py-3">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-medium text-gray-900">{a.name}</p>
                      <p className="text-xs text-gray-500">
                        {formatType(a.type)} · {a.currency}
                      </p>
                    </div>
                    <p className="text-sm font-semibold text-gray-900">
                      {formatCurrency(a.currentBalance)}
                    </p>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section>
          <h2 className="mb-3 text-lg font-semibold text-gray-900">Budgets</h2>
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
              {budgetsQuery.data.content.map((b) => (
                <li key={b.id} className="px-4 py-3">
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-medium text-gray-900">{b.name}</p>
                    <div className="text-right">
                      <p className="text-sm font-semibold text-gray-900">
                        {formatCurrency(b.spent)}
                      </p>
                      <p className="text-xs text-gray-500">
                        of {formatCurrency(b.amount)}
                      </p>
                    </div>
                  </div>
                  <div className="mt-2 h-2 w-full rounded-full bg-gray-100">
                    <div
                      className="h-2 rounded-full bg-indigo-500"
                      style={{ width: `${Math.min((b.spent / b.amount) * 100, 100)}%` }}
                    />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section>
          <h2 className="mb-3 text-lg font-semibold text-gray-900">Goals</h2>
          {goalsQuery.isPending && (
            <p className="text-sm text-gray-500">Loading goals...</p>
          )}
          {goalsQuery.isError && (
            <p className="text-sm text-red-600">Failed to load goals.</p>
          )}
          {goalsQuery.data && goalsQuery.data.content.length === 0 && (
            <p className="text-sm text-gray-500">No goals yet.</p>
          )}
          {goalsQuery.data && goalsQuery.data.content.length > 0 && (
            <ul className="divide-y divide-gray-200 rounded-lg border border-gray-200 bg-white">
              {goalsQuery.data.content.map((g) => (
                <li key={g.id} className="px-4 py-3">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-medium text-gray-900">{g.name}</p>
                      <p className="text-xs text-gray-500">
                        {g.targetDate
                          ? `by ${new Date(g.targetDate).toLocaleDateString()}`
                          : 'No deadline'}
                      </p>
                    </div>
                    <p className="text-sm font-semibold text-gray-900">
                      {Math.round(g.progressPercent)}%
                    </p>
                  </div>
                  <div className="mt-2 h-2 w-full rounded-full bg-gray-100">
                    <div
                      className="h-2 rounded-full bg-emerald-500"
                      style={{ width: `${Math.min(g.progressPercent, 100)}%` }}
                    />
                  </div>
                  <p className="mt-1 text-xs text-gray-500">
                    {formatCurrency(g.currentAmount)} of {formatCurrency(g.targetAmount)}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section>
          <h2 className="mb-3 text-lg font-semibold text-gray-900">Reminders</h2>
          {remindersQuery.isPending && (
            <p className="text-sm text-gray-500">Loading reminders...</p>
          )}
          {remindersQuery.isError && (
            <p className="text-sm text-red-600">Failed to load reminders.</p>
          )}
          {remindersQuery.data && remindersQuery.data.content.length === 0 && (
            <p className="text-sm text-gray-500">No reminders.</p>
          )}
          {remindersQuery.data && remindersQuery.data.content.length > 0 && (
            <ul className="divide-y divide-gray-200 rounded-lg border border-gray-200 bg-white">
              {remindersQuery.data.content.map((r) => (
                <li key={r.id} className="flex items-center justify-between px-4 py-3">
                  <p className="text-sm text-gray-700">{r.message}</p>
                  <button
                    onClick={() => acknowledgeMutation.mutate(r.id)}
                    className="ml-2 shrink-0 rounded-md px-2 py-1 text-xs font-medium text-indigo-600 hover:bg-indigo-50"
                  >
                    Done
                  </button>
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

function formatCurrency(amount: number): string {
  return `$${amount.toFixed(2)}`
}
