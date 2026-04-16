import { createBrowserRouter, RouterProvider, Navigate, Outlet, useLocation, useRouteError } from 'react-router-dom'
import { useEffect } from 'react'
import { RotateCcw, AlertTriangle } from 'lucide-react'
import AppShell      from '@/ui/layout/AppShell/AppShell'
import SettingsLayout from '@/features/settings/layout/SettingsLayout'
import { useAuthStore } from '@/store/auth.store'

// ---------------------------------------------------------------------------
// Domain guard
//  MARKETING_HOST  → marketing only  (app paths → APP_HOST)
//  APP_HOST        → app only        (/ → MARKETING_HOST)
//  localhost       → no redirect (dev)
// ---------------------------------------------------------------------------

const APP_HOST       = new URL(import.meta.env.VITE_APP_URL       ?? 'http://localhost:3000').hostname
const MARKETING_HOST = new URL(import.meta.env.VITE_MARKETING_URL ?? 'http://localhost:3000').hostname

function isAppPath(pathname: string) {
  const marketingOnly = ['/', '/confirm', '/accept-invite']
  return !marketingOnly.some(p => pathname === p || pathname.startsWith(p + '/'))
}

;(function applyDomainRedirect() {
  const { hostname, pathname, search } = window.location
  if (hostname === 'localhost' || hostname === '127.0.0.1') return

  // Also match the apex domain (flowcollect.io without www) as a marketing host
  const isMarketingHost = hostname === MARKETING_HOST || hostname === MARKETING_HOST.replace(/^www\./, '')
  if (isMarketingHost && isAppPath(pathname))
    window.location.replace(`https://${APP_HOST}${pathname}${search}`)

  if (hostname === APP_HOST && pathname === '/')
    window.location.replace(`https://${MARKETING_HOST}`)
})()

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function routeLazy(fn: () => Promise<{ default: React.ComponentType }>) {
  return async () => {
    try {
      const { default: Component } = await fn()
      return { Component }
    } catch (err) {
      // Chunk failed to load — stale deployment, new hashes on CDN.
      // Reload once so the browser picks up the new build.
      const key = 'chunk_reload_attempted'
      if (!sessionStorage.getItem(key)) {
        sessionStorage.setItem(key, '1')
        window.location.reload()
        // Return nothing while reload is in flight — avoids React Router
        // flashing "Unexpected Application Error!" before the page refreshes.
        return { Component: () => null }
      }
      throw err
    }
  }
}

// Watches every client-side navigation and redirects to the correct domain.
// The IIFE above handles the initial hard load; this handles SPA route changes.
function DomainGuard() {
  const { pathname, search } = useLocation()

  useEffect(() => {
    const { hostname } = window.location
    if (hostname === 'localhost' || hostname === '127.0.0.1') return

    const isMarketingHost = hostname === MARKETING_HOST || hostname === MARKETING_HOST.replace(/^www\./, '')
    if (isMarketingHost && isAppPath(pathname))
      window.location.replace(`https://${APP_HOST}${pathname}${search}`)

    if (hostname === APP_HOST && pathname === '/')
      window.location.replace(`https://${MARKETING_HOST}`)
  }, [pathname, search])

  return null
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
// Route-level error fallback
// Rendered by React Router when a lazy route fails to load (e.g. stale chunk
// after a new deployment where the first reload attempt already ran).
// ---------------------------------------------------------------------------

function RouteErrorPage() {
  useRouteError() // must call to satisfy React Router's hook requirement
  return (
    <div className="min-h-screen flex items-center justify-center bg-[#F4F7F9] dark:bg-[#0D1B2A] px-4">
      <div className="text-center max-w-xs">
        <div className="w-14 h-14 rounded-full bg-red-100 dark:bg-red-500/10 flex items-center justify-center mx-auto mb-5">
          <AlertTriangle size={24} className="text-red-500" strokeWidth={1.5} />
        </div>
        <h1 className="text-lg font-semibold text-[#0D1B2A] dark:text-white mb-2">
          Page failed to load
        </h1>
        <p className="text-sm text-c-muted mb-6">
          Try reloading the page. If the problem persists, come back in a few minutes.
        </p>
        <button
          onClick={() => window.location.reload()}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold text-white hover:opacity-90 transition-opacity"
          style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
        >
          <RotateCcw size={14} strokeWidth={2.5} />
          Reload page
        </button>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Router
// ---------------------------------------------------------------------------

function RootLayout() {
  return (
    <>
      <DomainGuard />
      <Outlet />
    </>
  )
}

const router = createBrowserRouter([
  {
    element:      <RootLayout />,
    errorElement: <RouteErrorPage />,
    children: [

  { path: '/', lazy: routeLazy(() => import('@/features/landing/pages/LandingPage')) },

  {
    element: <GuestRoute />,
    children: [
      { path: '/login',            lazy: routeLazy(() => import('@/features/auth/pages/LoginPage')) },
      { path: '/register',         lazy: routeLazy(() => import('@/features/auth/pages/RegisterPage')) },
      { path: '/verify-email',     lazy: routeLazy(() => import('@/features/auth/pages/VerifyEmailPage')) },
      { path: '/forgot-password',  lazy: routeLazy(() => import('@/features/auth/pages/ForgotPasswordPage')) },
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

  { path: '/reset-password',        lazy: routeLazy(() => import('@/features/auth/pages/ResetPasswordPage')) },
  { path: '/confirm/:token',        lazy: routeLazy(() => import('@/features/confirm/pages/ConfirmPaymentPage')) },
  { path: '/accept-invite/:token',  lazy: routeLazy(() => import('@/features/invite/pages/AcceptInvitePage')) },

  { path: '*', element: <Navigate to="/dashboard" replace /> },

    ],
  },
])

export default function Router() {
  return <RouterProvider router={router} future={{ v7_startTransition: true }} />
}
