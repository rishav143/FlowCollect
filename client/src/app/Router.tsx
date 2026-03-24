import { createBrowserRouter, RouterProvider, Navigate, Outlet } from 'react-router-dom'
import AppShell from '@/ui/layout/AppShell/AppShell'
import SettingsLayout from '@/features/settings/layout/SettingsLayout'
import { useAuthStore } from '@/store/auth.store'

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
// Route guards
// ---------------------------------------------------------------------------

/** Redirects unauthenticated users to /login */
function ProtectedRoute() {
  const token = useAuthStore((s) => s.token)
  if (!token) return <Navigate to="/login" replace />
  return <Outlet />
}

/** Redirects already-authenticated users to /dashboard (keep /login clean) */
function GuestRoute() {
  const token = useAuthStore((s) => s.token)
  if (token) return <Navigate to="/dashboard" replace />
  return <Outlet />
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

  // ── Guest-only routes (redirect to /dashboard if already authenticated) ──
  {
    element: <GuestRoute />,
    children: [
      { path: '/login',    lazy: routeLazy(() => import('@/features/auth/pages/LoginPage')) },
      { path: '/register', lazy: routeLazy(() => import('@/features/auth/pages/RegisterPage')) },
    ],
  },

  // ── Protected routes (redirect to /login if unauthenticated) ─────────────
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppShell />,
        children: [
          { path: '/dashboard',    lazy: routeLazy(() => import('@/features/dashboard/pages/DashboardPage')) },
          { path: '/invoices',     lazy: routeLazy(() => import('@/features/invoices/pages/InvoiceListPage')) },
          { path: '/invoices/:id', lazy: routeLazy(() => import('@/features/invoices/pages/InvoiceDetailPage')) },
          { path: '/templates',    lazy: routeLazy(() => import('@/features/templates/pages/TemplatesPage')) },
          { path: '/followups',    lazy: routeLazy(() => import('@/features/followups/pages/FollowupsPage')) },
          { path: '/approvals',    lazy: routeLazy(() => import('@/features/approvals/pages/ApprovalsPage')) },
          { path: '/clients',      lazy: routeLazy(() => import('@/features/clients/pages/ClientListPage')) },
          { path: '/clients/:id',  lazy: routeLazy(() => import('@/features/clients/pages/ClientDetailPage')) },
          { path: '/reminder-rules', lazy: routeLazy(() => import('@/features/reminders/pages/RemindersPage')) },
          // Settings — tabs are rendered eagerly inside SettingsLayout (no sub-routes)
          { path: '/settings',      element: <SettingsLayout /> },
          { path: '/settings/:tab', element: <SettingsLayout /> },
        ],
      },
    ],
  },

  // ── Public — no shell, no auth required ───────────────────────────────────
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
