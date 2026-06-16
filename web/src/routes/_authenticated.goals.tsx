import { createFileRoute, Link, Outlet, useRouterState } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import { Plus } from 'lucide-react'

export const Route = createFileRoute('/_authenticated/goals')({
  component: GoalsLayout,
})

interface GoalResponse {
  id: string
  name: string
  targetAmount: number
  currentAmount: number
  progressPercent: number
  targetDate: string | null
  users: { id: string; displayName: string }[]
  transactionIds: string[]
}

interface GoalPage {
  content: GoalResponse[]
}

function GoalsLayout() {
  const routerState = useRouterState()
  const isIndex = routerState.location.pathname === '/goals'
  const goalsQuery = useQuery({
    queryKey: ['goals'],
    queryFn: () => apiClient<GoalPage>('/api/goals?page=0&size=50'),
    enabled: isIndex,
  })

  if (!isIndex) {
    return <Outlet />
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Goals</h1>
          <p className="mt-1 text-sm text-gray-500">
            Track your savings targets
          </p>
        </div>
        <Link
          to="/goals/new"
          className="inline-flex items-center gap-1.5 rounded-md bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500"
        >
          <Plus size={16} />
          New Goal
        </Link>
      </div>

      <div className="mt-8">
        {goalsQuery.isPending && (
          <p className="text-sm text-gray-500">Loading goals...</p>
        )}
        {goalsQuery.isError && (
          <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
            Failed to load goals. Please try again.
          </div>
        )}
        {goalsQuery.data && goalsQuery.data.content.length === 0 && (
          <div className="rounded-lg border border-gray-200 bg-white p-12 text-center">
            <p className="text-sm text-gray-500">No goals yet.</p>
            <p className="mt-1 text-xs text-gray-400">
              Create your first savings goal to get started.
            </p>
          </div>
        )}
        {goalsQuery.data && goalsQuery.data.content.length > 0 && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {goalsQuery.data.content.map((goal) => (
              <Link
                key={goal.id}
                to="/goals/$goalId"
                params={{ goalId: goal.id }}
                className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm transition hover:border-emerald-300 hover:shadow-md"
              >
                <div className="flex items-start justify-between">
                  <p className="text-sm font-semibold text-gray-900">
                    {goal.name}
                  </p>
                  {goal.users.length > 1 && (
                    <span className="text-xs text-gray-400">
                      {goal.users.length}
                    </span>
                  )}
                </div>
                <div className="mt-4">
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-gray-500">Saved</span>
                    <span className="font-medium text-gray-900">
                      {Math.round(goal.progressPercent)}%
                    </span>
                  </div>
                  <div className="mt-1 h-2 w-full rounded-full bg-gray-100">
                    <div
                      className="h-2 rounded-full bg-emerald-500"
                      style={{
                        width: `${Math.min(goal.progressPercent, 100)}%`,
                      }}
                    />
                  </div>
                  <p className="mt-1 text-xs text-gray-400">
                    {formatCurrency(goal.currentAmount)} of{' '}
                    {formatCurrency(goal.targetAmount)}
                  </p>
                  {goal.targetDate && (
                    <p className="mt-1 text-xs text-gray-400">
                      by {new Date(goal.targetDate).toLocaleDateString()}
                    </p>
                  )}
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

function formatCurrency(amount: number): string {
  return `$${amount.toFixed(2)}`
}
