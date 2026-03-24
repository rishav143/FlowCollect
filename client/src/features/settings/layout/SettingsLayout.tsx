import { useState } from 'react'
import { useLocation } from 'react-router-dom'
import OrgSettingsPage from '../pages/OrgSettingsPage'
import TeamPage        from '../pages/TeamPage'
import BillingPage     from '../pages/BillingPage'

const TABS = [
  { path: '/settings/org',     label: 'Organisation', Component: OrgSettingsPage },
  { path: '/settings/team',    label: 'Team',         Component: TeamPage        },
  { path: '/settings/billing', label: 'Billing',      Component: BillingPage     },
] as const

type TabPath = typeof TABS[number]['path']

function resolveInitialTab(pathname: string): TabPath {
  const match = TABS.find((t) => pathname.startsWith(t.path))
  return match ? match.path : TABS[0].path
}

export default function SettingsLayout() {
  const { pathname } = useLocation()
  const [activeTab, setActiveTab] = useState<TabPath>(() => resolveInitialTab(pathname))

  function switchTab(path: TabPath) {
    setActiveTab(path)
    // Update URL for bookmarkability without triggering React Router navigation
    window.history.replaceState(null, '', path)
  }

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
          <button
            key={tab.path}
            onClick={() => switchTab(tab.path)}
            className={[
              'px-4 py-1.5 text-sm font-medium rounded-md transition-colors',
              activeTab === tab.path
                ? 'bg-white dark:bg-[#243447] text-[#0D1B2A] dark:text-white shadow-sm'
                : 'text-c-muted hover:text-[#0D1B2A] dark:hover:text-white',
            ].join(' ')}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* All tabs rendered simultaneously — CSS-only toggle keeps components
          mounted so they never re-fetch or show skeleton loaders on switch */}
      <div className={activeTab === '/settings/org'     ? undefined : 'hidden'}><OrgSettingsPage /></div>
      <div className={activeTab === '/settings/team'    ? undefined : 'hidden'}><TeamPage /></div>
      <div className={activeTab === '/settings/billing' ? undefined : 'hidden'}><BillingPage /></div>

    </div>
  )
}
