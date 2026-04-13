import { createBrowserRouter, RouterProvider, Navigate, Outlet, useLocation } from 'react-router-dom'
import { useEffect } from 'react'
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
    const { default: Component } = await fn()
    return { Component }
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
    element: <RootLayout />,
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
