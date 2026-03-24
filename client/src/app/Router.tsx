import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom'
import AppShell from '@/ui/layout/AppShell/AppShell'
import SettingsLayout from '@/features/settings/layout/SettingsLayout'

// ---------------------------------------------------------------------------
// routeLazy — wraps a dynamic import so React Router can:
//   1. fetch the chunk when the user first navigates to the route
//   2. set navigation.state = 'loading' while fetching  (drives TopLoadingBar)
//   3. keep the previous page visible until the chunk is ready (no flash)
//      when RouterProvider has future.v7_startTransition = true
// ---------------------------------------------------------------------------

function routeLazy(fn: () => Promise<{ default: React.ComponentType }>) {
  return async () => {
    const { default: Component } = await fn()
    return { Component }
  }
}

// ---------------------------------------------------------------------------
// Placeholder — for routes not yet implemented
// ---------------------------------------------------------------------------

function PlaceholderPage({ title }: { title: string }) {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] text-center gap-2">
      <p className="text-3xl">{title.split(' ')[0]}</p>
      <h1 className="text-xl font-bold text-[#0D1B2A] dark:text-white">{title.split(' ').slice(1).join(' ')}</h1>
      <p className="text-sm text-c-muted">Page coming soon</p>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Router — each child route loads its chunk on first navigation only
// ---------------------------------------------------------------------------

const router = createBrowserRouter([
  // Root redirect
  { path: '/', element: <Navigate to="/dashboard" replace /> },

  // Authenticated shell
  {
    element: <AppShell />,
    children: [
      { path: '/dashboard',    lazy: routeLazy(() => import('@/features/dashboard/pages/DashboardPage')) },
      { path: '/invoices',     lazy: routeLazy(() => import('@/features/invoices/pages/InvoiceListPage')) },
      { path: '/invoices/:id', lazy: routeLazy(() => import('@/features/invoices/pages/InvoiceDetailPage')) },
      { path: '/templates',   lazy: routeLazy(() => import('@/features/templates/pages/TemplatesPage')) },
      { path: '/followups',    lazy: routeLazy(() => import('@/features/followups/pages/FollowupsPage')) },
      { path: '/approvals',   lazy: routeLazy(() => import('@/features/approvals/pages/ApprovalsPage')) },
      { path: '/clients',     lazy: routeLazy(() => import('@/features/clients/pages/ClientListPage')) },
      { path: '/clients/:id', lazy: routeLazy(() => import('@/features/clients/pages/ClientDetailPage')) },
      { path: '/reminder-rules',    lazy: routeLazy(() => import('@/features/reminders/pages/RemindersPage')) },
      {
        path: '/settings',
        element: <SettingsLayout />,
        children: [
          { path: 'org',     lazy: routeLazy(() => import('@/features/settings/pages/OrgSettingsPage')) },
          { path: 'team',    lazy: routeLazy(() => import('@/features/settings/pages/TeamPage'))        },
          { path: 'billing', lazy: routeLazy(() => import('@/features/settings/pages/BillingPage'))    },
        ],
      },
    ],
  },

  // Public — no shell
  { path: '/confirm/:token', element: <PlaceholderPage title="✅ Payment Confirmation" /> },

  // Catch-all
  { path: '*', element: <Navigate to="/dashboard" replace /> },
])

export default function Router() {
  return (
    <RouterProvider
      router={router}
      future={{ v7_startTransition: true }}
    />
  )
}
