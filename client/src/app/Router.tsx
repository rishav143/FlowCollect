import { createBrowserRouter, RouterProvider, Navigate, Outlet } from 'react-router-dom'
import AppShell      from '@/ui/layout/AppShell/AppShell'
import SettingsLayout from '@/features/settings/layout/SettingsLayout'
import { useAuthStore } from '@/store/auth.store'

// ---------------------------------------------------------------------------
// Domain guard
//  www.flowcollect.io  → marketing only  (app paths → app.flowcollect.io)
//  app.flowcollect.io  → app only        (/ → www.flowcollect.io)
//  localhost           → no redirect (dev)
// ---------------------------------------------------------------------------

const APP_HOST       = 'app.flowcollect.io'
const MARKETING_HOST = 'www.flowcollect.io'

function isAppPath(pathname: string) {
  const marketingOnly = ['/', '/confirm', '/accept-invite']
  return !marketingOnly.some(p => pathname === p || pathname.startsWith(p + '/'))
}

;(function applyDomainRedirect() {
  const { hostname, pathname, search } = window.location
  if (hostname === 'localhost' || hostname === '127.0.0.1') return

  if ((hostname === MARKETING_HOST || hostname === 'flowcollect.io') && isAppPath(pathname))
    window.location.replace(`https://${APP_HOST}${pathname}${search}`)

  if (hostname === APP_HOST && pathname === '/')
    window.location.replace(`https://${MARKETING_HOST}`)
})()

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function routeLazy(fn: () => Promise<{ default: React.ComponentType }>) {
  return async () => {
    const { default: Component } = await fn()
    return { Component }
  }
}

function ProtectedRoute() {
  const token = useAuthStore((s) => s.token)
  if (!token) return <Navigate to="/login" replace />
  return <Outlet />
}

function GuestRoute() {
  const token = useAuthStore((s) => s.token)
  if (token) return <Navigate to="/dashboard" replace />
  return <Outlet />
}

// ---------------------------------------------------------------------------
// Router
// ---------------------------------------------------------------------------

const router = createBrowserRouter([
  { path: '/', lazy: routeLazy(() => import('@/features/landing/pages/LandingPage')) },

  {
    element: <GuestRoute />,
    children: [
      { path: '/login',        lazy: routeLazy(() => import('@/features/auth/pages/LoginPage')) },
      { path: '/register',     lazy: routeLazy(() => import('@/features/auth/pages/RegisterPage')) },
      { path: '/verify-email', lazy: routeLazy(() => import('@/features/auth/pages/VerifyEmailPage')) },
    ],
  },

  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppShell />,
        children: [
          { path: '/dashboard',      lazy: routeLazy(() => import('@/features/dashboard/pages/DashboardPage')) },
          { path: '/recover',        lazy: routeLazy(() => import('@/features/recover/pages/RecoverPage')) },
          { path: '/invoices',       lazy: routeLazy(() => import('@/features/invoices/pages/InvoiceListPage')) },
          { path: '/invoices/:id',   lazy: routeLazy(() => import('@/features/invoices/pages/InvoiceDetailPage')) },
          { path: '/templates',      lazy: routeLazy(() => import('@/features/templates/pages/TemplatesPage')) },
          { path: '/followups',      lazy: routeLazy(() => import('@/features/followups/pages/FollowupsPage')) },
          { path: '/approvals',      lazy: routeLazy(() => import('@/features/approvals/pages/ApprovalsPage')) },
          { path: '/clients',        lazy: routeLazy(() => import('@/features/clients/pages/ClientListPage')) },
          { path: '/clients/:id',    lazy: routeLazy(() => import('@/features/clients/pages/ClientDetailPage')) },
          { path: '/reminder-rules', lazy: routeLazy(() => import('@/features/reminders/pages/RemindersPage')) },
          { path: '/settings',       element: <SettingsLayout /> },
          { path: '/settings/:tab',  element: <SettingsLayout /> },
        ],
      },
    ],
  },

  { path: '/confirm/:token',       lazy: routeLazy(() => import('@/features/confirm/pages/ConfirmPaymentPage')) },
  { path: '/accept-invite/:token', lazy: routeLazy(() => import('@/features/invite/pages/AcceptInvitePage')) },

  { path: '*', element: <Navigate to="/dashboard" replace /> },
])

export default function Router() {
  return <RouterProvider router={router} future={{ v7_startTransition: true }} />
}
