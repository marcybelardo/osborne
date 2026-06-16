import { createFileRoute, Link, Outlet, useNavigate, useRouterState } from '@tanstack/react-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { apiClient } from '../api/client'
import { ArrowLeft, Pencil, Plus, Trash2 } from 'lucide-react'

export const Route = createFileRoute('/_authenticated/accounts/$accountId')({
  component: AccountDetailLayout,
})

interface UserSummary {
  id: string
  displayName: string
}

interface AccountResponse {
  id: string
  name: string
  type: string
  currency: string
  initialBalance: number
  currentBalance: number
  users: UserSummary[]
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
  createdAt: string
  updatedAt: string
}

interface TransactionPage {
  content: TransactionResponse[]
}

function AccountDetailLayout() {
  const { accountId } = Route.useParams()
  const routerState = useRouterState()
  const isIndex = routerState.location.pathname === `/accounts/${accountId}`
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [deletingTxId, setDeletingTxId] = useState<string | null>(null)

  const accountQuery = useQuery({
    queryKey: ['accounts', accountId],
    queryFn: () =>
      apiClient<AccountResponse>(`/api/accounts/${accountId}`),
    enabled: isIndex,
  })

  const transactionsQuery = useQuery({
    queryKey: ['accounts', accountId, 'transactions'],
    queryFn: () =>
      apiClient<TransactionPage>(
        `/api/accounts/${accountId}/transactions?page=0&size=50`,
      ),
    enabled: isIndex,
  })

  if (!isIndex) {
    return <Outlet />
  }

  const deleteAccountMutation = useMutation({
    mutationFn: () =>
      apiClient(`/api/accounts/${accountId}`, { method: 'DELETE' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
      navigate({ to: '/accounts' })
    },
  })

  const deleteTransactionMutation = useMutation({
    mutationFn: (txId: string) =>
      apiClient(`/api/accounts/${accountId}/transactions/${txId}`, {
        method: 'DELETE',
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['accounts', accountId, 'transactions'],
      })
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
      setDeletingTxId(null)
    },
  })

  const account = accountQuery.data
  const transactions = transactionsQuery.data?.content ?? []

  if (accountQuery.isPending) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-8">
        <p className="text-sm text-gray-500">Loading account...</p>
      </div>
    )
  }

  if (accountQuery.isError) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-8">
        <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
          Failed to load account.
        </div>
        <button
          onClick={() => navigate({ to: '/accounts' })}
          className="mt-4 inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
        >
          <ArrowLeft size={16} />
          Back to Accounts
        </button>
      </div>
    )
  }

  if (!account) return null

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <button
        onClick={() => navigate({ to: '/accounts' })}
        className="mb-6 inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
      >
        <ArrowLeft size={16} />
        Back to Accounts
      </button>

      {/* Account header */}
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{account.name}</h1>
          <p className="mt-1 text-sm text-gray-500">
            {formatType(account.type)} · {account.currency}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Link
            to="/accounts/$accountId/edit"
            params={{ accountId }}
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

      {/* Balance cards */}
      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        <div className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
          <p className="text-xs font-medium uppercase text-gray-400">
            Current Balance
          </p>
          <p
            className={`mt-1 text-2xl font-bold ${
              account.currentBalance >= 0 ? 'text-gray-900' : 'text-red-600'
            }`}
          >
            {formatCurrency(account.currentBalance)}
          </p>
        </div>
        <div className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
          <p className="text-xs font-medium uppercase text-gray-400">
            Initial Balance
          </p>
          <p className="mt-1 text-2xl font-bold text-gray-900">
            {formatCurrency(account.initialBalance)}
          </p>
        </div>
      </div>

      {/* Meta info */}
      <div className="mt-4 flex flex-wrap gap-x-6 gap-y-1 text-xs text-gray-400">
        <span>
          Created {new Date(account.createdAt).toLocaleDateString()}
        </span>
        <span>
          Updated {new Date(account.updatedAt).toLocaleDateString()}
        </span>
        {account.users.length > 0 && (
          <span title={account.users.map((u) => u.displayName).join(', ')}>
            Shared with {account.users.map((u) => u.displayName).join(', ')}
          </span>
        )}
      </div>

      {/* Transactions */}
      <div className="mt-8">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-gray-900">Transactions</h2>
          <Link
            to="/accounts/$accountId/transactions/new"
            params={{ accountId }}
            className="inline-flex items-center gap-1 rounded-md bg-indigo-600 px-3 py-1.5 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500"
          >
            <Plus size={14} />
            New Transaction
          </Link>
        </div>

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
                <p className="text-sm text-gray-500">No transactions yet.</p>
              </div>
            )}
          {transactions.length > 0 && (
            <div className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">
                      Date
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">
                      Description
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">
                      Category
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500">
                      Amount
                    </th>
                    <th className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {transactions.map((tx) => (
                    <tr key={tx.id} className="hover:bg-gray-50">
                      <td className="whitespace-nowrap px-4 py-3 text-sm text-gray-900">
                        {new Date(tx.transactionDate).toLocaleDateString()}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-700">
                        {tx.description || (
                          <span className="text-gray-400">—</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-500">
                        {tx.category || (
                          <span className="text-gray-400">—</span>
                        )}
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
                        <div className="flex items-center justify-end gap-2">
                          <Link
                            to="/accounts/$accountId/transactions/$transactionId/edit"
                            params={{
                              accountId,
                              transactionId: tx.id,
                            }}
                            className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-indigo-600"
                            title="Edit transaction"
                          >
                            <Pencil size={14} />
                          </Link>
                          <button
                            onClick={() => setDeletingTxId(tx.id)}
                            className="rounded p-1 text-gray-400 hover:bg-red-50 hover:text-red-600"
                            title="Delete transaction"
                          >
                            <Trash2 size={14} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Delete account confirmation dialog */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="mx-4 w-full max-w-md rounded-lg bg-white p-6 shadow-lg">
            <h3 className="text-lg font-semibold text-gray-900">
              Delete Account
            </h3>
            <p className="mt-2 text-sm text-gray-600">
              Are you sure you want to delete <strong>{account.name}</strong>?
              All transactions in this account will also be permanently deleted.
              This action cannot be undone.
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
                  deleteAccountMutation.mutate()
                }}
                disabled={deleteAccountMutation.isPending}
                className="rounded-md bg-red-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-red-500 disabled:opacity-50"
              >
                {deleteAccountMutation.isPending ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete transaction confirmation dialog */}
      {deletingTxId != null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="mx-4 w-full max-w-md rounded-lg bg-white p-6 shadow-lg">
            <h3 className="text-lg font-semibold text-gray-900">
              Delete Transaction
            </h3>
            <p className="mt-2 text-sm text-gray-600">
              Are you sure you want to delete this transaction? This action
              cannot be undone.
            </p>
            <div className="mt-6 flex items-center justify-end gap-3">
              <button
                onClick={() => setDeletingTxId(null)}
                className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={() => deleteTransactionMutation.mutate(deletingTxId)}
                disabled={deleteTransactionMutation.isPending}
                className="rounded-md bg-red-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-red-500 disabled:opacity-50"
              >
                {deleteTransactionMutation.isPending ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
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
