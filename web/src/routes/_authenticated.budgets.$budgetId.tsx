import { createFileRoute, Link, Outlet, useNavigate, useRouterState } from '@tanstack/react-router'
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import { ArrowLeft, Pencil, Trash2 } from 'lucide-react'

export const Route = createFileRoute('/_authenticated/budgets/$budgetId')({
  component: BudgetDetailLayout,
})

interface UserSummary {
  id: string
  displayName: string
}

interface BudgetResponse {
  id: string
  name: string
  description: string | null
  timeframe: string
  startDate: string | null
  endDate: string | null
  periodStart: string
  periodEnd: string
  periodLabel: string
  amount: number
  currentSpending: number
  users: UserSummary[]
  transactionIds: string[]
  createdAt: string
  updatedAt: string
}

interface TransactionResponse {
  id: string
  amount: number
  description: string | null
  category: string | null
  transactionDate: string
  accountId: string
  budgetIds: string[]
  goalIds: string[]
}

interface TransactionPage {
  content: TransactionResponse[]
}

function BudgetDetailLayout() {
  const { budgetId } = Route.useParams()
  const routerState = useRouterState()
  const isIndex = routerState.location.pathname === `/budgets/${budgetId}`
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)

  const budgetQuery = useQuery({
    queryKey: ['budgets', budgetId],
    queryFn: () => apiClient<BudgetResponse>(`/api/budgets/${budgetId}`),
    enabled: isIndex,
  })

  const transactionsQuery = useQuery({
    queryKey: ['budgets', budgetId, 'transactions'],
    queryFn: () =>
      apiClient<TransactionPage>(
        `/api/budgets/${budgetId}/transactions?page=0&size=50`,
      ),
    enabled: isIndex,
  })

  const deleteMutation = useMutation({
    mutationFn: () =>
      apiClient(`/api/budgets/${budgetId}`, { method: 'DELETE' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['budgets'] })
      navigate({ to: '/budgets' })
    },
  })

  const removeTransactionMutation = useMutation({
    mutationFn: (txId: string) =>
      apiClient(`/api/budgets/${budgetId}/transactions/${txId}`, {
        method: 'DELETE',
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['budgets', budgetId, 'transactions'] })
      queryClient.invalidateQueries({ queryKey: ['budgets'] })
    },
  })

  if (!isIndex) {
    return <Outlet />
  }

  const budget = budgetQuery.data
  const transactions = transactionsQuery.data?.content ?? []

  if (budgetQuery.isPending) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-8">
        <p className="text-sm text-gray-500">Loading budget...</p>
      </div>
    )
  }

  if (budgetQuery.isError) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-8">
        <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
          Failed to load budget.
        </div>
        <button
          onClick={() => navigate({ to: '/budgets' })}
          className="mt-4 inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
        >
          <ArrowLeft size={16} />
          Back to Budgets
        </button>
      </div>
    )
  }

  if (!budget) return null

  const remaining = Math.max(budget.amount - budget.currentSpending, 0)
  const progress =
    budget.amount > 0
      ? (remaining / budget.amount) * 100
      : 0

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <button
        onClick={() => navigate({ to: '/budgets' })}
        className="mb-6 inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
      >
        <ArrowLeft size={16} />
        Back to Budgets
      </button>

      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            {budget.name}
          </h1>
          {budget.description && (
            <p className="mt-1 text-sm text-gray-500">{budget.description}</p>
          )}
          <p className="mt-1 text-xs text-gray-400">
            Created {new Date(budget.createdAt).toLocaleDateString()}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Link
            to="/budgets/$budgetId/edit"
            params={{ budgetId }}
            className="inline-flex items-center gap-1 rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50"
          >
            <Pencil size={14} />
            Edit
          </Link>
          <button
            onClick={() => setShowDeleteConfirm(true)}
            className="inline-flex items-center gap-1 rounded-md bg-red-600 px-3 py-1.5 text-sm font-semibold text-white shadow-sm hover:bg-red-500"
          >
            <Trash2 size={14} />
            Delete
          </button>
        </div>
      </div>

      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        <div className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
          <p className="text-xs font-medium uppercase text-gray-400">Budget Amount</p>
          <p className="mt-1 text-2xl font-bold text-gray-900">
            {formatCurrency(budget.amount)}
          </p>
        </div>
        <div className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
          <p className="text-xs font-medium uppercase text-gray-400">Remaining</p>
          <p
            className={`mt-1 text-2xl font-bold ${
              remaining > 0 ? 'text-gray-900' : 'text-red-600'
            }`}
          >
            {formatCurrency(remaining)}
          </p>
        </div>
      </div>

      <div className="mt-4">
        <div className="h-3 w-full rounded-full bg-gray-100">
          <div
            className={`h-3 rounded-full ${
              progress <= 25 ? 'bg-red-500' : progress <= 75 ? 'bg-amber-500' : 'bg-emerald-500'
            }`}
            style={{ width: `${progress}%` }}
          />
        </div>
        <p className="mt-1 text-xs text-gray-400">
          {Math.round(progress)}% remaining
        </p>
      </div>

      <div className="mt-4 flex flex-wrap gap-x-6 gap-y-1 text-xs text-gray-400">
        {budget.timeframe && budget.timeframe !== 'CUSTOM' && (
          <span className="font-medium text-gray-500">
            {budget.timeframe.charAt(0) + budget.timeframe.slice(1).toLowerCase()} · {budget.periodLabel}
          </span>
        )}
        {budget.startDate && budget.endDate && budget.timeframe === 'CUSTOM' && (
          <span>
            {new Date(budget.startDate).toLocaleDateString()} – {new Date(budget.endDate).toLocaleDateString()}
          </span>
        )}
        <span>Updated {new Date(budget.updatedAt).toLocaleDateString()}</span>
        {budget.users.length > 1 && (
          <span title={budget.users.map((u) => u.displayName).join(', ')}>
            Shared with {budget.users.map((u) => u.displayName).slice(1).join(', ')}
          </span>
        )}
        <span>{budget.transactionIds.length} transaction(s) allocated</span>
      </div>

      <div className="mt-8">
        <h2 className="text-lg font-semibold text-gray-900">Allocated Transactions</h2>
        <div className="mt-4">
          {transactionsQuery.isPending && (
            <p className="text-sm text-gray-500">Loading transactions...</p>
          )}
          {transactionsQuery.isError && (
            <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
              Failed to load transactions.
            </div>
          )}
          {!transactionsQuery.isPending &&
            !transactionsQuery.isError &&
            transactions.length === 0 && (
              <div className="rounded-lg border border-gray-200 bg-white p-8 text-center">
                <p className="text-sm text-gray-500">
                  No transactions allocated to this budget yet.
                </p>
                <p className="mt-1 text-xs text-gray-400">
                  Allocate transactions from the transaction edit page.
                </p>
              </div>
            )}
          {transactions.length > 0 && (
            <div className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Date</th>
                    <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Description</th>
                    <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Category</th>
                    <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500">Amount</th>
                    <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500">Remove</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {transactions.map((tx) => (
                    <tr key={tx.id} className="hover:bg-gray-50">
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-gray-900">
                        {new Date(tx.transactionDate).toLocaleDateString()}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-700">
                        {tx.description || <span className="text-gray-400">—</span>}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-500">
                        {tx.category || <span className="text-gray-400">—</span>}
                      </td>
                      <td
                        className={`whitespace-nowrap px-4 py-3 text-right text-sm font-medium ${
                          tx.amount >= 0 ? 'text-emerald-600' : 'text-red-600'
                        }`}
                      >
                        {tx.amount >= 0 ? '+' : ''}
                        {formatCurrency(tx.amount)}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 text-right">
                        <button
                          onClick={() => removeTransactionMutation.mutate(tx.id)}
                          disabled={removeTransactionMutation.isPending}
                          className="rounded p-1 text-gray-400 hover:bg-red-50 hover:text-red-600"
                          title="Remove from budget"
                        >
                          <Trash2 size={14} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {showDeleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="mx-4 w-full max-w-md rounded-lg bg-white p-6 shadow-lg">
            <h3 className="text-lg font-semibold text-gray-900">Delete Budget</h3>
            <p className="mt-2 text-sm text-gray-600">
              Are you sure you want to delete <strong>{budget.name}</strong>? This action cannot be undone.
            </p>
            <div className="mt-6 flex items-center justify-end gap-3">
              <button
                onClick={() => setShowDeleteConfirm(false)}
                className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  setShowDeleteConfirm(false)
                  deleteMutation.mutate()
                }}
                disabled={deleteMutation.isPending}
                className="rounded-md bg-red-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-red-500 disabled:opacity-50"
              >
                {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function formatCurrency(amount: number): string {
  return `$${amount.toFixed(2)}`
}
