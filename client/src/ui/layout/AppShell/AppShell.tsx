import { Outlet } from 'react-router-dom'
import Topbar           from '@/ui/layout/Topbar/Topbar'
import Sidebar          from '@/ui/layout/Sidebar/Sidebar'
import PageTransition   from '@/ui/components/PageTransition'
import TopLoadingBar    from '@/ui/loading/TopLoadingBar'
import { useNavProgress } from '@/ui/loading/useNavProgress'
import { useUIStore } from '@/store/ui.store'
// Both values must appear as complete literals so Tailwind includes them in the build:
const MAIN_ML_EXPANDED  = 'lg:ml-[220px]'
const MAIN_ML_COLLAPSED = 'lg:ml-14'

/**
 * AppShell — authenticated route chrome.
 *
 * Responsive layout:
 *
 *  Mobile / Tablet  (< lg)
 *  ┌──────────────────────────────────────────────────┐
 *  │  ☰  FlowCollect        [+] [🔔] [RC▼]  Topbar   │  full-width, z-30
 *  ├──────────────────────────────────────────────────┤
 *  │                                                  │
 *  │           <Outlet />  (full-width)               │
 *  │                                                  │
 *  └──────────────────────────────────────────────────┘
 *  ◀──────── Sidebar slides in as overlay drawer ──────▶
 *
 *  Laptop / PC  (≥ lg)
 *  ┌────────────┬─────────────────────────────────────┐
 *  │            │ 🔍 Search    [+ Add Invoice] [🔔][RC▼] │  left-[220px], z-30
 *  │  Sidebar   ├─────────────────────────────────────┤
 *  │  220px     │                                     │
 *  │  (fixed)   │   <Outlet />                        │
 *  │            │                                     │
 *  └────────────┴─────────────────────────────────────┘
 *
 * Design decisions:
 *  • Zero props — both Topbar and Sidebar read from stores directly.
 *  • Backdrop only mounts when sidebarOpen — no unnecessary DOM node.
 *  • Content padding is responsive: tighter on mobile, comfortable on desktop.
 */
export default function AppShell() {
  const { sidebarOpen, closeSidebar, sidebarCollapsed, mutationDepth } = useUIStore()
  const navActive = useNavProgress()

  return (
    <div className="min-h-screen bg-white dark:bg-[#0D1B2A]">

      {/* Route navigation progress bar — above everything */}
      <TopLoadingBar active={navActive || mutationDepth > 0} />

      {/* Sidebar */}
      <Sidebar />

      {/*
        Backdrop — mobile/tablet only.
        Sits between sidebar (z-30) and content (z-0).
        Click to dismiss drawer.
      */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black/40 z-20 lg:hidden"
          onClick={closeSidebar}
          aria-hidden="true"
        />
      )}

      {/* Topbar */}
      <Topbar />

      {/* Main content */}
      <main className={`pt-14 bg-white dark:bg-[#0D1B2A] min-h-[calc(100vh-3.5rem)] transition-[margin] duration-300 ease-in-out ${sidebarCollapsed ? MAIN_ML_COLLAPSED : MAIN_ML_EXPANDED}`}>
        <PageTransition>
          <div className="p-4 sm:p-5 lg:p-6">
            <Outlet />
          </div>
        </PageTransition>
      </main>

    </div>
  )
}
