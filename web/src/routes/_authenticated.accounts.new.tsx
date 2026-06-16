import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import React, { useState } from 'react'
import { apiClient } from '../api/client'
import { ArrowLeft } from 'lucide-react'

export const Route = createFileRoute('/_authenticated/accounts/new')({
  component: CreateAccount,
})

const ACCOUNT_TYPES = ['ASSET', 'CASH', 'CREDIT_CARD', 'EXPENSE', 'REVENUE'] as const

interface AccountResponse {
  id: string
  name: string
  type: string
  currency: string
  initialBalance: number
  currentBalance: number
  userIds: string[]
}

function CreateAccount() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const [name, setName] = useState('')
  const [type, setType] = useState<string>(ACCOUNT_TYPES[0])
  const [currency, setCurrency] = useState('USD')
  const [initialBalance, setInitialBalance] = useState('0')
  const [error, setError] = useState<string | null>(null)

  const createMutation = useMutation({
    mutationFn: (data: {
      name: string
      type: string
      currency: string
      initialBalance: number
    }) =>
      apiClient<AccountResponse>('/api/accounts', {
        method: 'POST',
        body: data,
      }),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] })
      navigate({ to: '/accounts/$accountId', params: { accountId: data.id } })
    },
    onError: (err: Error) => {
      setError(err.message)
    },
  })

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError(null)

    if (!name.trim()) {
      setError('Account name is required.')
      return
    }

    createMutation.mutate({
      name: name.trim(),
      type,
      currency: currency.trim() || 'USD',
      initialBalance: parseFloat(initialBalance) || 0,
    })
  }

  return (
    <div className="mx-auto max-w-lg px-4 py-8">
      <button
        onClick={() => navigate({ to: '/accounts' })}
        className="mb-6 inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
      >
        <ArrowLeft size={16} />
        Back to Accounts
      </button>

      <h1 className="text-2xl font-bold text-gray-900">New Account</h1>
      <p className="mt-1 text-sm text-gray-500">
        Create a new account to track your finances
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
              htmlFor="name"
              className="block text-sm font-medium text-gray-700"
            >
              Name
            </label>
            <input
              id="name"
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Joint Checking"
              className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label
              htmlFor="type"
              className="block text-sm font-medium text-gray-700"
            >
              Type
            </label>
            <select
              id="type"
              value={type}
              onChange={(e) => setType(e.target.value)}
              className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              {ACCOUNT_TYPES.map((t) => (
                <option key={t} value={t}>
                  {formatType(t)}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label
              htmlFor="currency"
              className="block text-sm font-medium text-gray-700"
            >
              Currency
            </label>
            <input
              id="currency"
              type="text"
              value={currency}
              onChange={(e) => setCurrency(e.target.value)}
              placeholder="USD"
              className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label
              htmlFor="initialBalance"
              className="block text-sm font-medium text-gray-700"
            >
              Initial Balance
            </label>
            <input
              id="initialBalance"
              type="number"
              step="0.01"
              value={initialBalance}
              onChange={(e) => setInitialBalance(e.target.value)}
              className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
            <p className="mt-1 text-xs text-gray-400">
              This value cannot be changed after creation.
            </p>
          </div>
        </div>

        <div className="mt-6 flex items-center gap-3">
          <button
            type="submit"
            disabled={createMutation.isPending}
            className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 disabled:opacity-50"
          >
            {createMutation.isPending ? 'Creating...' : 'Create Account'}
          </button>
          <button
            type="button"
            onClick={() => navigate({ to: '/accounts' })}
            className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}

function formatType(type: string): string {
  return type
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase())
}
