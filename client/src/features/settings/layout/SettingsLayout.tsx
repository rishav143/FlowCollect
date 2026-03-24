import { NavLink, Outlet, Navigate, useLocation } from 'react-router-dom'

const TABS = [
  { to: '/settings/org',     label: 'Organisation' },
  { to: '/settings/team',    label: 'Team'         },
  { to: '/settings/billing', label: 'Billing'      },
]

export default function SettingsLayout() {
  const { pathname } = useLocation()

  // Redirect bare /settings to /settings/org
  if (pathname === '/settings') return <Navigate to="/settings/org" replace />

  return (
    <div className="space-y-5">

      {/* Header */}
      <div>
        <h1 className="text-xl font-bold text-[#0D1B2A] dark:text-white">Settings</h1>
        <p className="text-sm text-c-muted mt-0.5">Manage your organisation, team, and billing.</p>
      </div>

      {/* Tab nav */}
      <div className="flex gap-1 p-1 bg-[#F4F7F9] dark:bg-[#1B2838] rounded-lg w-fit">
        {TABS.map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            className={({ isActive }) => [
              'px-4 py-1.5 text-sm font-medium rounded-md transition-colors',
              isActive
                ? 'bg-white dark:bg-[#243447] text-[#0D1B2A] dark:text-white shadow-sm'
                : 'text-c-muted hover:text-[#0D1B2A] dark:hover:text-white',
            ].join(' ')}
          >
            {tab.label}
          </NavLink>
        ))}
      </div>

      {/* Active section */}
      <Outlet />
    </div>
  )
}
