import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom'
import AppShell    from '@/ui/layout/AppShell/AppShell'
import TemplatesPage from '@/features/templates/pages/TemplatesPage'

// ---------------------------------------------------------------------------
// Placeholder pages — swapped out one-by-one as feature pages are built
// ---------------------------------------------------------------------------

function PlaceholderPage({ title }: { title: string }) {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] text-center gap-2">
      <p className="text-3xl">{title.split(' ')[0]}</p>
      <h1 className="text-xl font-bold text-[#0D1B2A] dark:text-white">{title.split(' ').slice(1).join(' ')}</h1>
      <p className="text-sm text-[#8A9BAE]">Page coming soon</p>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Router
// ---------------------------------------------------------------------------

const router = createBrowserRouter([
  // Root redirect
  { path: '/', element: <Navigate to="/dashboard" replace /> },

  // Authenticated shell — AppShell has zero props, reads from auth.store
  {
    element: <AppShell />,
    children: [
      { path: '/dashboard',          element: <PlaceholderPage title="📊 Dashboard" /> },
      { path: '/invoices',           element: <PlaceholderPage title="📄 Invoices" /> },
      { path: '/invoices/:id',       element: <PlaceholderPage title="📄 Invoice Detail" /> },
      { path: '/followups',          element: <PlaceholderPage title="⏰ Follow-ups" /> },
      { path: '/approvals',          element: <PlaceholderPage title="✅ Approvals" /> },
      { path: '/clients',            element: <PlaceholderPage title="👥 Clients" /> },
      { path: '/clients/:id',        element: <PlaceholderPage title="👥 Client Detail" /> },
      { path: '/reminder-rules',     element: <PlaceholderPage title="🔔 Reminders" /> },
      { path: '/templates',          element: <TemplatesPage /> },
      { path: '/settings/org',       element: <PlaceholderPage title="⚙️ Settings" /> },
      { path: '/settings/gateways',  element: <PlaceholderPage title="⚙️ Gateways" /> },
      { path: '/settings/team',      element: <PlaceholderPage title="⚙️ Team" /> },
    ],
  },

  // Public — no shell
  { path: '/confirm/:token', element: <PlaceholderPage title="✅ Payment Confirmation" /> },

  // Catch-all
  { path: '*', element: <Navigate to="/dashboard" replace /> },
])

export default function Router() {
  return <RouterProvider router={router} />
}
