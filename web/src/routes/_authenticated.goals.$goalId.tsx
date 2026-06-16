import { createFileRoute, Link, Outlet, useNavigate, useRouterState } from '@tanstack/react-router'
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import { ArrowLeft, Pencil, Trash2 } from 'lucide-react'

export const Route = createFileRoute('/_authenticated/goals/$goalId')({
  component: GoalDetailLayout,
})

interface UserSummary {
  id: string
  displayName: string
}

interface GoalResponse {
  id: string
  name: string
  targetAmount: number
  currentAmount: number
  progressPercent: number
  targetDate: string | null
  users: UserSummary[]
  transactionIds: string[]
  createdAt: string
  updatedAt: string
}

function GoalDetailLayout() {
  const { goalId } = Route.useParams()
  const routerState = useRouterState()
  const isIndex = routerState.location.pathname === `/goals/${goalId}`
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)

  const goalQuery = useQuery({
    queryKey: ['goals', goalId],
    queryFn: () => apiClient<GoalResponse>(`/api/goals/${goalId}`),
    enabled: isIndex,
  })

  const deleteMutation = useMutation({
    mutationFn: () =>
      apiClient(`/api/goals/${goalId}`, { method: 'DELETE' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['goals'] })
      navigate({ to: '/goals' })
    },
  })

  if (!isIndex) {
    return <Outlet />
  }

  const goal = goalQuery.data

  if (goalQuery.isPending) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-8">
        <p className="text-sm text-gray-500">Loading goal...</p>
      </div>
    )
  }

  if (goalQuery.isError) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-8">
        <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
          Failed to load goal.
        </div>
        <button
          onClick={() => navigate({ to: '/goals' })}
          className="mt-4 inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
        >
          <ArrowLeft size={16} />
          Back to Goals
        </button>
      </div>
    )
  }

  if (!goal) return null

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <button
        onClick={() => navigate({ to: '/goals' })}
        className="mb-6 inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
      >
        <ArrowLeft size={16} />
        Back to Goals
      </button>

      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{goal.name}</h1>
          <p className="mt-1 text-sm text-gray-500">
            Created {new Date(goal.createdAt).toLocaleDateString()}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Link
            to="/goals/$goalId/edit"
            params={{ goalId }}
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
          <p className="text-xs font-medium uppercase text-gray-400">Target Amount</p>
          <p className="mt-1 text-2xl font-bold text-gray-900">
            {formatCurrency(goal.targetAmount)}
          </p>
        </div>
        <div className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
          <p className="text-xs font-medium uppercase text-gray-400">Current Savings</p>
          <p className="mt-1 text-2xl font-bold text-emerald-600">
            {formatCurrency(goal.currentAmount)}
          </p>
        </div>
      </div>

      <div className="mt-6">
        <div className="flex items-center justify-between text-sm">
          <span className="text-gray-500">Progress</span>
          <span className="font-medium text-gray-900">
            {Math.round(goal.progressPercent)}%
          </span>
        </div>
        <div className="mt-1 h-3 w-full rounded-full bg-gray-100">
          <div
            className="h-3 rounded-full bg-emerald-500"
            style={{ width: `${Math.min(goal.progressPercent, 100)}%` }}
          />
        </div>
        <p className="mt-1 text-xs text-gray-400">
          {formatCurrency(goal.currentAmount)} of {formatCurrency(goal.targetAmount)}
        </p>
      </div>

      <div className="mt-4 flex flex-wrap gap-x-6 gap-y-1 text-xs text-gray-400">
        {goal.targetDate && (
          <span>Target date: {new Date(goal.targetDate).toLocaleDateString()}</span>
        )}
        <span>Updated {new Date(goal.updatedAt).toLocaleDateString()}</span>
        {goal.users.length > 0 && (
          <span title={goal.users.map((u) => u.displayName).join(', ')}>
            Shared with {goal.users.map((u) => u.displayName).join(', ')}
          </span>
        )}
        <span>{goal.transactionIds.length} transaction(s) allocated</span>
      </div>

      {showDeleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="mx-4 w-full max-w-md rounded-lg bg-white p-6 shadow-lg">
            <h3 className="text-lg font-semibold text-gray-900">Delete Goal</h3>
            <p className="mt-2 text-sm text-gray-600">
              Are you sure you want to delete <strong>{goal.name}</strong>? This action cannot be undone.
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
