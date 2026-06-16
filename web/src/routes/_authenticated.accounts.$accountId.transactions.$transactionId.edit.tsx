import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import React, { useState, useEffect } from 'react'
import { apiClient } from '../api/client'
import { ArrowLeft } from 'lucide-react'

export const Route = createFileRoute(
  '/_authenticated/accounts/$accountId/transactions/$transactionId/edit',
)({
  component: EditTransaction,
})

interface BudgetItem {
  id: string
  name: string
}

interface GoalItem {
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
  budgetIds: string[]
  goalIds: string[]
}

interface BudgetPage {
  content: BudgetItem[]
}

interface GoalPage {
  content: GoalItem[]
}

function EditTransaction() {
  const { accountId, transactionId } = Route.useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const [txType, setTxType] = useState<'deposit' | 'withdrawal'>('withdrawal')
  const [description, setDescription] = useState('')
  const [amount, setAmount] = useState('')
  const [category, setCategory] = useState('')
  const [transactionDate, setTransactionDate] = useState('')
  const [selectedBudgetIds, setSelectedBudgetIds] = useState<string[]>([])
  const [selectedGoalIds, setSelectedGoalIds] = useState<string[]>([])
  const [showBudgetPicker, setShowBudgetPicker] = useState(false)
  const [showGoalPicker, setShowGoalPicker] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const transactionQuery = useQuery({
    queryKey: ['accounts', accountId, 'transactions', transactionId],
    queryFn: () =>
      apiClient<TransactionResponse>(
        `/api/accounts/${accountId}/transactions/${transactionId}`,
      ),
  })

  const budgetsQuery = useQuery({
    queryKey: ['budgets'],
    queryFn: () => apiClient<BudgetPage>('/api/budgets?page=0&size=50'),
  })

  const goalsQuery = useQuery({
    queryKey: ['goals'],
    queryFn: () => apiClient<GoalPage>('/api/goals?page=0&size=50'),
  })

  useEffect(() => {
    if (transactionQuery.data) {
      const raw = transactionQuery.data.amount
      setTxType(raw >= 0 ? 'deposit' : 'withdrawal')
      setAmount(String(Math.abs(raw)))
      setDescription(transactionQuery.data.description ?? '')
      setCategory(transactionQuery.data.category ?? '')
      setTransactionDate(transactionQuery.data.transactionDate)
      setSelectedBudgetIds(transactionQuery.data.budgetIds ?? [])
      setSelectedGoalIds(transactionQuery.data.goalIds ?? [])
    }
  }, [transactionQuery.data])

  function toggleBudgetId(id: string) {
    setSelectedBudgetIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    )
  }

  function toggleGoalId(id: string) {
    setSelectedGoalIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    )
  }

  const updateMutation = useMutation({
    mutationFn: (data: {
      amount: number
      description: string
      category: string
      transactionDate: string
      budgetIds: string[]
      goalIds: string[]
    }) =>
      apiClient<TransactionResponse>(
        `/api/accounts/${accountId}/transactions/${transactionId}`,
        {
          method: 'PUT',
          body: data,
        },
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['accounts', accountId, 'transactions'],
      })
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
      queryClient.invalidateQueries({ queryKey: ['budgets'] })
      queryClient.invalidateQueries({ queryKey: ['goals'] })
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
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      setError('Amount must be a positive number.')
      return
    }

    updateMutation.mutate({
      amount: txType === 'withdrawal' ? -parsedAmount : parsedAmount,
      description: description.trim(),
      category: category.trim(),
      transactionDate,
      budgetIds: selectedBudgetIds,
      goalIds: selectedGoalIds,
    })
  }

  if (transactionQuery.isPending) {
    return (
      <div className="mx-auto max-w-lg px-4 py-8">
        <p className="text-sm text-gray-500">Loading transaction...</p>
      </div>
    )
  }

  if (transactionQuery.isError || !transactionQuery.data) {
    return (
      <div className="mx-auto max-w-lg px-4 py-8">
        <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
          Failed to load transaction.
        </div>
        <button
          onClick={() =>
            navigate({
              to: '/accounts/$accountId',
              params: { accountId },
            })
          }
          className="mt-4 inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
        >
          <ArrowLeft size={16} />
          Back to Account
        </button>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-lg px-4 py-8">
      <button
        onClick={() =>
          navigate({ to: '/accounts/$accountId', params: { accountId } })
        }
        className="mb-6 inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
      >
        <ArrowLeft size={16} />
        Back to Account
      </button>

      <h1 className="text-2xl font-bold text-gray-900">Edit Transaction</h1>
      <p className="mt-1 text-sm text-gray-500">
        Update transaction details
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
              htmlFor="txType"
              className="block text-sm font-medium text-gray-700"
            >
              Type
            </label>
            <select
              id="txType"
              value={txType}
              onChange={(e) =>
                setTxType(e.target.value as 'deposit' | 'withdrawal')
              }
              className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              <option value="withdrawal">Withdrawal / Expense</option>
              <option value="deposit">Deposit / Income</option>
            </select>
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
              className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
          </div>

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
              min="0.01"
              required
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
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

          {/* Budget selector */}
          <div className="relative">
            <label className="block text-sm font-medium text-gray-700">
              Budgets
            </label>
            <button
              type="button"
              onClick={() => setShowBudgetPicker(!showBudgetPicker)}
              className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-left text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              {selectedBudgetIds.length === 0
                ? 'Select budgets (optional)'
                : `${selectedBudgetIds.length} budget(s) selected`}
            </button>
            {showBudgetPicker && budgetsQuery.data && (
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
                      checked={selectedBudgetIds.includes(b.id)}
                      onChange={() => toggleBudgetId(b.id)}
                      className="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                    />
                    {b.name}
                  </label>
                ))}
              </div>
            )}
          </div>

          {/* Goal selector */}
          <div className="relative">
            <label className="block text-sm font-medium text-gray-700">
              Goals
            </label>
            <button
              type="button"
              onClick={() => setShowGoalPicker(!showGoalPicker)}
              className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-left text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              {selectedGoalIds.length === 0
                ? 'Select goals (optional)'
                : `${selectedGoalIds.length} goal(s) selected`}
            </button>
            {showGoalPicker && goalsQuery.data && (
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
                      checked={selectedGoalIds.includes(g.id)}
                      onChange={() => toggleGoalId(g.id)}
                      className="rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                    />
                    {g.name}
                  </label>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="mt-6 flex items-center gap-3">
          <button
            type="submit"
            disabled={updateMutation.isPending}
            className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 disabled:opacity-50"
          >
            {updateMutation.isPending ? 'Saving...' : 'Save Changes'}
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
