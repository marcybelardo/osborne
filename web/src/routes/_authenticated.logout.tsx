import { createFileRoute, redirect } from '@tanstack/react-router'
import { clearTokens, getAccessToken } from '../lib/auth'

export const Route = createFileRoute('/_authenticated/logout')({
  loader: async () => {
    const token = getAccessToken()
    if (token) {
      try {
        await fetch('http://localhost:8080/api/auth/logout', {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        })
      } catch {
        // Proceed with local logout even if API call fails
      }
    }
    clearTokens()
    throw redirect({ to: '/' })
  },
  component: Logout,
})

function Logout() {
  return (
    <div className="flex items-center justify-center px-4 py-16">
      <p className="text-sm text-gray-500">Logging out...</p>
    </div>
  )
}
