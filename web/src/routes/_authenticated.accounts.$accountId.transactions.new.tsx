import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import React, { useState } from 'react'
import { apiClient } from '../api/client'
import { ArrowLeft } from 'lucide-react'

export const Route = createFileRoute(
  '/_authenticated/accounts/$accountId/transactions/new',
)({
  component: CreateTransaction,
})

interface AccountResponse {
  id: string
  name: string
}

interface TransactionResponse {
  id: string
  amount: number
  description: string | null
  category: string | null
  transactionDate: string
  accountId: string
}

function todayString(): string {
  const d = new Date()
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

function CreateTransaction() {
  const { accountId } = Route.useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')
  const [category, setCategory] = useState('')
  const [transactionDate, setTransactionDate] = useState(todayString())
  const [error, setError] = useState<string | null>(null)

  const accountQuery = useQuery({
    queryKey: ['accounts', accountId],
    queryFn: () =>
      apiClient<AccountResponse>(`/api/accounts/${accountId}`),
  })

  const createMutation = useMutation({
    mutationFn: (data: {
      amount: number
      description: string
      category: string
      transactionDate: string
    }) =>
      apiClient<TransactionResponse>(
        `/api/accounts/${accountId}/transactions`,
        {
          method: 'POST',
          body: data,
        },
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['accounts', accountId, 'transactions'],
      })
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
      navigate({
        to: '/accounts/$accountId',
        params: { accountId },
      })
    },
    onError: (err: Error) => {
      setError(err.message)
    },
  })

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError(null)

    const parsedAmount = parseFloat(amount)
    if (isNaN(parsedAmount)) {
      setError('A valid amount is required.')
      return
    }

    createMutation.mutate({
      amount: parsedAmount,
      description: description.trim(),
      category: category.trim(),
      transactionDate,
    })
  }

  const accountName = accountQuery.data?.name ?? 'Account'

  return (
    <div className="mx-auto max-w-lg px-4 py-8">
      <button
        onClick={() =>
          navigate({ to: '/accounts/$accountId', params: { accountId } })
        }
        className="mb-6 inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
      >
        <ArrowLeft size={16} />
        Back to {accountName}
      </button>

      <h1 className="text-2xl font-bold text-gray-900">New Transaction</h1>
      <p className="mt-1 text-sm text-gray-500">
        Add a transaction to {accountName}
      </p>

      <form
        onSubmit={handleSubmit}
        className="mt-6 rounded-lg border border-gray-200 bg-white p-6 shadow-sm"
      >
        {error && (
          <div className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </div>
        )}

        <div className="space-y-4">
          <div>
            <label
              htmlFor="amount"
              className="block text-sm font-medium text-gray-700"
            >
              Amount
            </label>
            <input
              id="amount"
              type="number"
              step="0.01"
              required
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="0.00"
              className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
            <p className="mt-1 text-xs text-gray-400">
              Use negative amounts for expenses/withdrawals.
            </p>
          </div>

          <div>
            <label
              htmlFor="description"
              className="block text-sm font-medium text-gray-700"
            >
              Description
            </label>
            <input
              id="description"
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="e.g. Groceries"
              className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label
              htmlFor="category"
              className="block text-sm font-medium text-gray-700"
            >
              Category
            </label>
            <input
              id="category"
              type="text"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              placeholder="e.g. Food & Dining"
              className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label
              htmlFor="transactionDate"
              className="block text-sm font-medium text-gray-700"
            >
              Date
            </label>
            <input
              id="transactionDate"
              type="date"
              required
              value={transactionDate}
              onChange={(e) => setTransactionDate(e.target.value)}
              className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
          </div>
        </div>

        <div className="mt-6 flex items-center gap-3">
          <button
            type="submit"
            disabled={createMutation.isPending}
            className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 disabled:opacity-50"
          >
            {createMutation.isPending
              ? 'Creating...'
              : 'Create Transaction'}
          </button>
          <button
            type="button"
            onClick={() =>
              navigate({
                to: '/accounts/$accountId',
                params: { accountId },
              })
            }
            className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
