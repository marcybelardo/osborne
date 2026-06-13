import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/')({ component: Home })

function Home() {
  return (
    <div className="flex flex-col items-center justify-center px-4 py-24">
      <h1 className="text-5xl font-bold tracking-tight text-gray-900">Osborne</h1>
      <p className="mt-4 text-lg text-gray-500">Budget Manager</p>
    </div>
  )
}
