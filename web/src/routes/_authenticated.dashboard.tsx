import { createFileRoute, Link } from '@tanstack/react-router'
import { useQueries, useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import { ArrowDown, ArrowUp, AlertTriangle, CheckCircle, Bell } from 'lucide-react'
import { useState } from 'react'

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
  users: { id: string; displayName: string }[]
}

interface AccountPage {
  content: AccountResponse[]
}

interface BudgetResponse {
  id: string
  name: string
  timeframe: string
  periodLabel: string
  amount: number
  currentSpending: number
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
  type: string
  userId: string
  transactionId: string | null
  createdAt: string
}

interface ReminderPage {
  content: ReminderResponse[]
}

interface TransactionResponse {
  id: string
  amount: number
  description: string | null
  category: string | null
  transactionDate: string
  accountId: string
}

interface TransactionPage {
  content: TransactionResponse[]
}

interface FeedItem {
  id: string
  date: Date
  type: 'withdrawal' | 'deposit' | 'bill_mismatch' | 'goal_milestone'
  // transaction fields
  accountName?: string
  accountId?: string
  amount?: number
  description?: string
  category?: string
  transactionDate?: string
  // reminder fields
  message?: string
  reminderId?: string
  reminderStatus?: string
  relatedTransactionId?: string | null
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
    queryKey: ['reminders', 'feed'],
    queryFn: () =>
      apiClient<ReminderPage>('/api/reminders?page=0&size=50'),
  })

  const accountIds = accountsQuery.data?.content.map((a) => a.id) ?? []

  const accountTransactionsQueries = useQueries({
    queries: accountIds.map((id) => ({
      queryKey: ['accounts', id, 'transactions', 'recent'],
      queryFn: () =>
        apiClient<TransactionPage>(
          `/api/accounts/${id}/transactions?page=0&size=5`,
        ),
      enabled: !!accountsQuery.data,
    })),
  })

  const acknowledgeMutation = useMutation({
    mutationFn: (id: string) =>
      apiClient(`/api/reminders/${id}/acknowledge`, { method: 'PUT' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reminders'] })
    },
  })

  // Quick-add transaction form state
  const [qaAccountId, setQaAccountId] = useState('')
  const [qaTxType, setQaTxType] = useState<'deposit' | 'withdrawal'>('withdrawal')
  const [qaAmount, setQaAmount] = useState('')
  const [qaDescription, setQaDescription] = useState('')
  const [qaBudgetIds, setQaBudgetIds] = useState<string[]>([])
  const [qaGoalIds, setQaGoalIds] = useState<string[]>([])
  const [qaShowBudgetPicker, setQaShowBudgetPicker] = useState(false)
  const [qaShowGoalPicker, setQaShowGoalPicker] = useState(false)
  const [qaError, setQaError] = useState<string | null>(null)
  const [qaSuccess, setQaSuccess] = useState(false)

  const quickAddMutation = useMutation({
    mutationFn: (data: {
      accountId: string
      amount: number
      description: string
      budgetIds: string[]
      goalIds: string[]
    }) =>
      apiClient<TransactionResponse>(
        `/api/accounts/${data.accountId}/transactions`,
        {
          method: 'POST',
          body: {
            amount: data.amount,
            description: data.description,
            transactionDate: new Date().toISOString().slice(0, 10),
            budgetIds: data.budgetIds,
            goalIds: data.goalIds,
          },
        },
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
      queryClient.invalidateQueries({ queryKey: ['budgets'] })
      queryClient.invalidateQueries({ queryKey: ['goals'] })
      queryClient.invalidateQueries({ queryKey: ['reminders'] })
      // Invalidate all account transaction queries
      accountIds.forEach((id) => {
        queryClient.invalidateQueries({ queryKey: ['accounts', id, 'transactions'] })
      })
      setQaAmount('')
      setQaDescription('')
      setQaBudgetIds([])
      setQaGoalIds([])
      setQaError(null)
      setQaSuccess(true)
      setTimeout(() => setQaSuccess(false), 3000)
    },
    onError: (err: Error) => {
      setQaError(err.message)
    },
  })

  function toggleQaBudgetId(id: string) {
    setQaBudgetIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    )
  }

  function toggleQaGoalId(id: string) {
    setQaGoalIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    )
  }

  function handleQuickAdd(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setQaError(null)
    setQaSuccess(false)

    if (!qaAccountId) {
      setQaError('Please select an account.')
      return
    }

    const parsedAmount = parseFloat(qaAmount)
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      setQaError('Amount must be a positive number.')
      return
    }

    quickAddMutation.mutate({
      accountId: qaAccountId,
      amount: qaTxType === 'withdrawal' ? -parsedAmount : parsedAmount,
      description: qaDescription.trim(),
      budgetIds: qaBudgetIds,
      goalIds: qaGoalIds,
    })
  }

  // Build feed items
  const feedItems: FeedItem[] = []

  // Add transactions from all accounts
  accountTransactionsQueries.forEach((q, i) => {
    const account = accountsQuery.data?.content[i]
    if (q.data && account) {
      q.data.content.forEach((tx) => {
        feedItems.push({
          id: `tx-${tx.id}`,
          date: new Date(tx.transactionDate + 'T00:00:00'),
          type: tx.amount >= 0 ? 'deposit' : 'withdrawal',
          accountName: account.name,
          accountId: account.id,
          amount: tx.amount,
          description: tx.description ?? undefined,
          category: tx.category ?? undefined,
          transactionDate: tx.transactionDate,
        })
      })
    }
  })

  // Add reminders
  if (remindersQuery.data) {
    remindersQuery.data.content.forEach((r) => {
      feedItems.push({
        id: `rem-${r.id}`,
        date: new Date(r.createdAt),
        type: r.type === 'BILL_MISMATCH' ? 'bill_mismatch' : 'goal_milestone',
        message: r.message,
        reminderId: r.id,
        reminderStatus: r.status,
        relatedTransactionId: r.transactionId,
      })
    })
  }

  // Sort by date descending
  feedItems.sort((a, b) => b.date.getTime() - a.date.getTime())

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
          <p className="mt-1 text-sm text-gray-500">
            Overview of your accounts, budgets, and goals
          </p>
        </div>
      </div>

      {/* Summary cards */}
      <div className="grid gap-8 md:grid-cols-3">
        <section>
          <Link
            to="/accounts"
            className="mb-3 block text-lg font-semibold text-gray-900 hover:text-indigo-600"
          >
            Accounts
          </Link>
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
                  <Link
                    to="/accounts/$accountId"
                    params={{ accountId: a.id }}
                    className="flex items-center justify-between"
                  >
                    <div>
                      <p className="text-sm font-medium text-gray-900">
                        {a.name}
                      </p>
                      <p className="text-xs text-gray-500">
                        {formatType(a.type)} · {a.currency}
                      </p>
                    </div>
                    <p className="text-sm font-semibold text-gray-900">
                      {formatCurrency(a.currentBalance)}
                    </p>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section>
          <Link
            to="/budgets"
            className="mb-3 block text-lg font-semibold text-gray-900 hover:text-indigo-600"
          >
            Budgets
          </Link>
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
                  <Link
                    to="/budgets/$budgetId"
                    params={{ budgetId: b.id }}
                    className="block"
                  >
                    <div className="flex items-center justify-between">
                      <p className="text-sm font-medium text-gray-900">
                        {b.name}
                      </p>
                      <div className="text-right">
                        <p className="text-sm font-semibold text-gray-900">
                          {formatCurrency(Math.max(b.amount - b.currentSpending, 0))}
                        </p>
                        <p className="text-xs text-gray-500">
                          of {formatCurrency(b.amount)}
                        </p>
                      </div>
                    </div>
                    {b.timeframe && (
                      <p className="mt-0.5 text-xs text-gray-400">
                        {b.timeframe.charAt(0) + b.timeframe.slice(1).toLowerCase()}{b.timeframe !== 'CUSTOM' ? ` · ${b.periodLabel}` : ''}
                      </p>
                    )}
                    <div className="mt-2 h-2 w-full rounded-full bg-gray-100">
                      <div
                        className="h-2 rounded-full bg-emerald-500"
                        style={{
                          width: `${Math.min(Math.max((b.amount - b.currentSpending) / b.amount * 100, 0), 100)}%`,
                        }}
                      />
                    </div>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section>
          <Link
            to="/goals"
            className="mb-3 block text-lg font-semibold text-gray-900 hover:text-indigo-600"
          >
            Goals
          </Link>
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
                  <Link
                    to="/goals/$goalId"
                    params={{ goalId: g.id }}
                    className="block"
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-sm font-medium text-gray-900">
                          {g.name}
                        </p>
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
                        style={{
                          width: `${Math.min(g.progressPercent, 100)}%`,
                        }}
                      />
                    </div>
                    <p className="mt-1 text-xs text-gray-500">
                      {formatCurrency(g.currentAmount)} of{' '}
                      {formatCurrency(g.targetAmount)}
                    </p>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      {/* Quick-add transaction — always visible */}
      <div className="mt-8">
        <div className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
          <h3 className="mb-4 text-sm font-semibold text-gray-900">Quick Add Transaction</h3>

          {qaError && (
            <div className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{qaError}</div>
          )}
          {qaSuccess && (
            <div className="mb-4 rounded-md bg-green-50 px-3 py-2 text-sm text-green-700">Transaction added successfully!</div>
          )}

          <form onSubmit={handleQuickAdd} className="space-y-3">
            <div className="grid gap-3 sm:grid-cols-4">
              <div>
                <label className="block text-xs font-medium text-gray-700">Account</label>
                <select
                  value={qaAccountId}
                  onChange={(e) => setQaAccountId(e.target.value)}
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                >
                  <option value="">Select account</option>
                  {accountsQuery.data?.content.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700">Type</label>
                <select
                  value={qaTxType}
                  onChange={(e) => setQaTxType(e.target.value as 'deposit' | 'withdrawal')}
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                >
                  <option value="withdrawal">Withdrawal</option>
                  <option value="deposit">Deposit</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700">Amount</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  required
                  value={qaAmount}
                  onChange={(e) => setQaAmount(e.target.value)}
                  placeholder="0.00"
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-medium text-gray-700">Description</label>
                <input
                  type="text"
                  value={qaDescription}
                  onChange={(e) => setQaDescription(e.target.value)}
                  placeholder="e.g. Groceries"
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                />
              </div>
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <div className="relative">
                <label className="block text-xs font-medium text-gray-700">Budgets</label>
                <button
                  type="button"
                  onClick={() => setQaShowBudgetPicker(!qaShowBudgetPicker)}
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-left text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                >
                  {qaBudgetIds.length === 0
                    ? 'Select budgets (optional)'
                    : `${qaBudgetIds.length} budget(s) selected`}
                </button>
                {qaShowBudgetPicker && budgetsQuery.data && (
                  <div className="absolute z-10 mt-1 w-full rounded-md border border-gray-200 bg-white shadow-lg">
                    {budgetsQuery.data.content.length === 0 && (
                      <p className="px-3 py-2 text-xs text-gray-400">No budgets available</p>
                    )}
                    {budgetsQuery.data.content.map((b) => (
                      <label
                        key={b.id}
                        className="flex items-center gap-2 px-3 py-2 text-sm hover:bg-gray-50 cursor-pointer"
                      >
                        <input
                          type="checkbox"
                          checked={qaBudgetIds.includes(b.id)}
                          onChange={() => toggleQaBudgetId(b.id)}
                          className="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                        />
                        {b.name}
                      </label>
                    ))}
                  </div>
                )}
              </div>

              <div className="relative">
                <label className="block text-xs font-medium text-gray-700">Goals</label>
                <button
                  type="button"
                  onClick={() => setQaShowGoalPicker(!qaShowGoalPicker)}
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-left text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                >
                  {qaGoalIds.length === 0
                    ? 'Select goals (optional)'
                    : `${qaGoalIds.length} goal(s) selected`}
                </button>
                {qaShowGoalPicker && goalsQuery.data && (
                  <div className="absolute z-10 mt-1 w-full rounded-md border border-gray-200 bg-white shadow-lg">
                    {goalsQuery.data.content.length === 0 && (
                      <p className="px-3 py-2 text-xs text-gray-400">No goals available</p>
                    )}
                    {goalsQuery.data.content.map((g) => (
                      <label
                        key={g.id}
                        className="flex items-center gap-2 px-3 py-2 text-sm hover:bg-gray-50 cursor-pointer"
                      >
                        <input
                          type="checkbox"
                          checked={qaGoalIds.includes(g.id)}
                          onChange={() => toggleQaGoalId(g.id)}
                          className="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                        />
                        {g.name}
                      </label>
                    ))}
                  </div>
                )}
              </div>
            </div>

            <div className="flex items-center gap-2 pt-1">
              <button
                type="submit"
                disabled={quickAddMutation.isPending}
                className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 disabled:opacity-50"
              >
                {quickAddMutation.isPending ? 'Adding...' : 'Add Transaction'}
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Recent Activity Feed */}
      <section className="mt-10">
        <h2 className="mb-4 text-lg font-semibold text-gray-900">
          Recent Activity
        </h2>

        {feedItems.length === 0 &&
          !remindersQuery.isPending &&
          accountTransactionsQueries.every((q) => !q.isPending) && (
            <div className="rounded-lg border border-gray-200 bg-white p-12 text-center">
              <Bell className="mx-auto h-8 w-8 text-gray-300" />
              <p className="mt-3 text-sm text-gray-500">
                No recent activity to show.
              </p>
            </div>
          )}

        {(remindersQuery.isPending ||
          accountTransactionsQueries.some((q) => q.isPending)) && (
          <p className="text-sm text-gray-500">Loading activity...</p>
        )}

        {feedItems.length > 0 && (
          <div className="space-y-3">
            {feedItems.map((item) => (
              <div key={item.id}>
                {item.type === 'bill_mismatch' && (
                  <div className="rounded-lg border border-red-200 bg-red-50 p-4 shadow-sm">
                    <div className="flex items-start gap-3">
                      <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-red-500" />
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium text-red-800">
                          {item.message}
                        </p>
                        <p className="mt-0.5 text-xs text-red-600">
                          {timeAgo(item.date)}
                        </p>
                      </div>
                      {item.reminderId &&
                        item.reminderStatus === 'PENDING' && (
                          <button
                            onClick={() =>
                              acknowledgeMutation.mutate(item.reminderId!)
                            }
                            className="shrink-0 rounded-md bg-red-100 px-3 py-1 text-xs font-medium text-red-700 hover:bg-red-200"
                          >
                            Acknowledge
                          </button>
                        )}
                    </div>
                  </div>
                )}

                {item.type === 'goal_milestone' && (
                  <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-4 shadow-sm">
                    <div className="flex items-start gap-3">
                      <CheckCircle className="mt-0.5 h-5 w-5 shrink-0 text-emerald-500" />
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium text-emerald-800">
                          {item.message}
                        </p>
                        <p className="mt-0.5 text-xs text-emerald-600">
                          {timeAgo(item.date)}
                        </p>
                      </div>
                      {item.reminderId &&
                        item.reminderStatus === 'PENDING' && (
                          <button
                            onClick={() =>
                              acknowledgeMutation.mutate(item.reminderId!)
                            }
                            className="shrink-0 rounded-md bg-emerald-100 px-3 py-1 text-xs font-medium text-emerald-700 hover:bg-emerald-200"
                          >
                            Acknowledge
                          </button>
                        )}
                    </div>
                  </div>
                )}

                {item.type === 'withdrawal' && (
                  <Link
                    to="/accounts/$accountId"
                    params={{ accountId: item.accountId! }}
                    className="flex items-center gap-3 rounded-lg border border-gray-200 bg-white p-4 shadow-sm transition hover:border-gray-300"
                  >
                    <div className="flex h-8 w-8 items-center justify-center rounded-full bg-red-50">
                      <ArrowDown className="h-4 w-4 text-red-500" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-medium text-gray-900">
                        {item.description || 'Transaction'}
                      </p>
                      <p className="text-xs text-gray-500">
                        {item.accountName}
                        {item.category ? ` · ${item.category}` : ''}
                        {' · '}
                        {item.transactionDate
                          ? new Date(
                              item.transactionDate + 'T00:00:00',
                            ).toLocaleDateString()
                          : timeAgo(item.date)}
                      </p>
                    </div>
                    <p className="shrink-0 text-sm font-semibold text-red-600">
                      {item.amount != null
                        ? `-${formatCurrency(Math.abs(item.amount))}`
                        : ''}
                    </p>
                  </Link>
                )}

                {item.type === 'deposit' && (
                  <Link
                    to="/accounts/$accountId"
                    params={{ accountId: item.accountId! }}
                    className="flex items-center gap-3 rounded-lg border border-gray-200 bg-white p-4 shadow-sm transition hover:border-gray-300"
                  >
                    <div className="flex h-8 w-8 items-center justify-center rounded-full bg-emerald-50">
                      <ArrowUp className="h-4 w-4 text-emerald-500" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-medium text-gray-900">
                        {item.description || 'Transaction'}
                      </p>
                      <p className="text-xs text-gray-500">
                        {item.accountName}
                        {item.category ? ` · ${item.category}` : ''}
                        {' · '}
                        {item.transactionDate
                          ? new Date(
                              item.transactionDate + 'T00:00:00',
                            ).toLocaleDateString()
                          : timeAgo(item.date)}
                      </p>
                    </div>
                    <p className="shrink-0 text-sm font-semibold text-emerald-600">
                      +{item.amount != null ? formatCurrency(item.amount) : ''}
                    </p>
                  </Link>
                )}
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}

function timeAgo(date: Date): string {
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMins < 1) return 'just now'
  if (diffMins < 60) return `${diffMins}m ago`
  if (diffHours < 24) return `${diffHours}h ago`
  if (diffDays < 7) return `${diffDays}d ago`
  return date.toLocaleDateString()
}

function formatType(type: string): string {
  return type
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase())
}

function formatCurrency(amount: number): string {
  return `$${Math.abs(amount).toFixed(2)}`
}
