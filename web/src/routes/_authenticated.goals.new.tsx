import { createFileRoute, useNavigate } from '@tanstack/react-router'
import React, { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import { ArrowLeft } from 'lucide-react'

export const Route = createFileRoute('/_authenticated/goals/new')({
  component: CreateGoal,
})

interface GoalResponse {
  id: string
  name: string
  targetAmount: number
  currentAmount: number
  progressPercent: number
  targetDate: string | null
  users: { id: string; displayName: string }[]
}

function CreateGoal() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [name, setName] = useState('')
  const [targetAmount, setTargetAmount] = useState('')
  const [targetDate, setTargetDate] = useState('')
  const [error, setError] = useState<string | null>(null)

  const createMutation = useMutation({
    mutationFn: (data: {
      name: string
      targetAmount: number
      targetDate: string | null
    }) =>
      apiClient<GoalResponse>('/api/goals', {
        method: 'POST',
        body: data,
      }),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['goals'] })
      navigate({ to: '/goals/$goalId', params: { goalId: data.id } })
    },
    onError: (err: Error) => {
      setError(err.message)
    },
  })

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError(null)

    if (!name.trim()) {
      setError('Goal name is required.')
      return
    }

    const parsed = parseFloat(targetAmount)
    if (isNaN(parsed) || parsed <= 0) {
      setError('Target amount must be a positive number.')
      return
    }

    createMutation.mutate({
      name: name.trim(),
      targetAmount: parsed,
      targetDate: targetDate || null,
    })
  }

  return (
    <div className="mx-auto max-w-lg px-4 py-8">
      <button
        onClick={() => navigate({ to: '/goals' })}
        className="mb-6 inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
      >
        <ArrowLeft size={16} />
        Back to Goals
      </button>

      <h1 className="text-2xl font-bold text-gray-900">New Goal</h1>
      <p className="mt-1 text-sm text-gray-500">
        Set a savings goal to work toward
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
            <label htmlFor="name" className="block text-sm font-medium text-gray-700">
              Goal Name
            </label>
            <input
              id="name"
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Emergency Fund"
              className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label htmlFor="targetAmount" className="block text-sm font-medium text-gray-700">
              Target Amount
            </label>
            <input
              id="targetAmount"
              type="number"
              step="0.01"
              min="0.01"
              required
              value={targetAmount}
              onChange={(e) => setTargetAmount(e.target.value)}
              placeholder="0.00"
              className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label htmlFor="targetDate" className="block text-sm font-medium text-gray-700">
              Target Date
            </label>
            <input
              id="targetDate"
              type="date"
              value={targetDate}
              onChange={(e) => setTargetDate(e.target.value)}
              className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            />
            <p className="mt-1 text-xs text-gray-400">
              Optional deadline for this goal
            </p>
          </div>
        </div>

        <div className="mt-6 flex items-center gap-3">
          <button
            type="submit"
            disabled={createMutation.isPending}
            className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 disabled:opacity-50"
          >
            {createMutation.isPending ? 'Creating...' : 'Create Goal'}
          </button>
          <button
            type="button"
            onClick={() => navigate({ to: '/goals' })}
            className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
