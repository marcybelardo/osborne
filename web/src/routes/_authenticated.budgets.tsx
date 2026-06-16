import { createFileRoute, Link, Outlet, useRouterState } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import { Plus } from 'lucide-react'

export const Route = createFileRoute('/_authenticated/budgets')({
  component: BudgetsLayout,
})

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
  users: { id: string; displayName: string }[]
  transactionIds: string[]
  createdAt: string
  updatedAt: string
}

interface BudgetPage {
  content: BudgetResponse[]
}

function BudgetsLayout() {
  const routerState = useRouterState()
  const isIndex = routerState.location.pathname === '/budgets'
  const budgetsQuery = useQuery({
    queryKey: ['budgets'],
    queryFn: () => apiClient<BudgetPage>('/api/budgets?page=0&size=50'),
    enabled: isIndex,
  })

  if (!isIndex) {
    return <Outlet />
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Budgets</h1>
          <p className="mt-1 text-sm text-gray-500">
            Track your spending limits
          </p>
        </div>
        <Link
          to="/budgets/new"
          className="inline-flex items-center gap-1.5 rounded-md bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500"
        >
          <Plus size={16} />
          New Budget
        </Link>
      </div>

      <div className="mt-8">
        {budgetsQuery.isPending && (
          <p className="text-sm text-gray-500">Loading budgets...</p>
        )}
        {budgetsQuery.isError && (
          <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
            Failed to load budgets. Please try again.
          </div>
        )}
        {budgetsQuery.data && budgetsQuery.data.content.length === 0 && (
          <div className="rounded-lg border border-gray-200 bg-white p-12 text-center">
            <p className="text-sm text-gray-500">No budgets yet.</p>
            <p className="mt-1 text-xs text-gray-400">
              Create your first budget to start tracking spending.
            </p>
          </div>
        )}
        {budgetsQuery.data && budgetsQuery.data.content.length > 0 && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {budgetsQuery.data.content.map((budget) => {
              const remaining = Math.max(budget.amount - budget.currentSpending, 0)
              const progress = budget.amount > 0
                ? (remaining / budget.amount) * 100
                : 0
              return (
                <Link
                  key={budget.id}
                  to="/budgets/$budgetId"
                  params={{ budgetId: budget.id }}
                  className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm transition hover:border-indigo-300 hover:shadow-md"
                >
                  <div className="flex items-start justify-between">
                    <p className="text-sm font-semibold text-gray-900">
                      {budget.name}
                    </p>
                    {budget.users.length > 1 && (
                      <span className="text-xs text-gray-400">
                        {budget.users.length}
                      </span>
                    )}
                  </div>
                  {budget.startDate && budget.endDate && budget.timeframe === 'CUSTOM' && (
                    <p className="mt-0.5 text-xs text-gray-400">
                      {new Date(budget.startDate).toLocaleDateString()} – {new Date(budget.endDate).toLocaleDateString()}
                    </p>
                  )}
                  {budget.timeframe && budget.timeframe !== 'CUSTOM' && (
                    <p className="mt-0.5 text-xs text-gray-400">
                      {budget.timeframe.charAt(0) + budget.timeframe.slice(1).toLowerCase()} · {budget.periodLabel}
                    </p>
                  )}
                  <div className="mt-4">
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-gray-500">Remaining</span>
                      <span className="font-medium text-gray-900">
                        {formatCurrency(remaining)}
                      </span>
                    </div>
                    <div className="mt-1 h-2 w-full rounded-full bg-gray-100">
                      <div
                        className={`h-2 rounded-full ${
                          progress <= 25 ? 'bg-red-500' : progress <= 75 ? 'bg-amber-500' : 'bg-emerald-500'
                        }`}
                        style={{ width: `${progress}%` }}
                      />
                    </div>
                    <p className="mt-1 text-xs text-gray-400">
                      of {formatCurrency(budget.amount)}
                    </p>
                  </div>
                </Link>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}

function formatCurrency(amount: number): string {
  return `$${amount.toFixed(2)}`
}
