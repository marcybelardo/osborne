import { createFileRoute, useNavigate, redirect } from '@tanstack/react-router'
import { useMutation } from '@tanstack/react-query'
import React, { useState } from 'react'
import { apiClient, ApiError } from '../api/client'
import { setTokens, isAuthenticated } from '../lib/auth'

export const Route = createFileRoute('/')({
  beforeLoad: () => {
    if (isAuthenticated()) {
      throw redirect({ to: '/dashboard' })
    }
  },
  component: Home,
})

interface LoginResponse {
  token: string
  refreshToken: string
  displayName: string
  id: string
}

function Home() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const loginMutation = useMutation({
    mutationFn: (data: { email: string; password: string }) =>
      apiClient<LoginResponse>('/api/auth/login', {
        method: 'POST',
        body: data,
      }),
    onSuccess: (data) => {
      setTokens(data.token, data.refreshToken)
      navigate({ to: '/dashboard' })
    },
    onError: (err) => {
      if (err instanceof ApiError) {
        setError(err.message)
      } else {
        setError('Login failed. Please try again.')
      }
    },
  })

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setError('')
    loginMutation.mutate({ email, password })
  }

  return (
    <div className="flex flex-col items-center justify-center px-4 py-16">
      <div className="mb-12 text-center">
        <h1 className="text-5xl font-bold tracking-tight text-gray-900">
          Osborne
        </h1>
        <p className="mt-3 text-xl text-gray-500">Budget Manager</p>
      </div>

      <div className="mb-12 grid max-w-2xl gap-6 sm:grid-cols-3">
        <FeatureCard
          title="Budgeting"
          description="Create budgets, track progress, and assign transactions to keep spending in check."
        />
        <FeatureCard
          title="Account Management"
          description="Manage checking, savings, and credit accounts in one place with real-time balances."
        />
        <FeatureCard
          title="Shared Accounts"
          description="Invite others to manage shared accounts. Perfect for household finances."
        />
      </div>

      <div className="w-full max-w-sm rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-lg font-semibold text-gray-900">Log in</h2>

        {error && (
          <div className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label
              htmlFor="email"
              className="block text-sm font-medium text-gray-700"
            >
              Email
            </label>
            <input
              id="email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              placeholder="you@example.com"
            />
          </div>
          <div>
            <label
              htmlFor="password"
              className="block text-sm font-medium text-gray-700"
            >
              Password
            </label>
            <input
              id="password"
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              placeholder="••••••••"
            />
          </div>
          <button
            type="submit"
            disabled={loginMutation.isPending}
            className="w-full rounded-md bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-500 disabled:opacity-50"
          >
            {loginMutation.isPending ? 'Logging in...' : 'Log in'}
          </button>
        </form>

        <p className="mt-4 text-center text-sm text-gray-500">
          Don't have an account?{' '}
          <a
            href="/register"
            className="font-medium text-indigo-600 hover:text-indigo-500"
          >
            Register
          </a>
        </p>
      </div>
    </div>
  )
}

function FeatureCard({
  title,
  description,
}: {
  title: string
  description: string
}) {
  return (
    <div className="rounded-lg border border-gray-200 bg-white px-5 py-6 text-center shadow-sm">
      <h3 className="text-sm font-semibold text-gray-900">{title}</h3>
      <p className="mt-2 text-sm text-gray-500">{description}</p>
    </div>
  )
}
