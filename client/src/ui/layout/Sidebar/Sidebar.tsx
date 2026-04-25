import { useEffect } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import {
  LayoutDashboard,
  FileText,
  Clock,
  CheckCircle2,
  Users,
  Settings,
  LayoutTemplate,
  Zap,
  X,
  ChevronLeft,
  ChevronRight,
  type LucideIcon,
} from 'lucide-react'
import Logo from '@/ui/components/Logo/Logo'
import { useAuthStore } from '@/store/auth.store'
import { useUIStore  } from '@/store/ui.store'
import { useNavBadges } from '@/hooks/useNavBadges'
import { useIsDesktop } from '@/hooks/useIsDesktop'


// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface NavItemDef {
  to:        string
  icon:      LucideIcon
  label:     string
  count?:    number
  highlight?: boolean
  pulseDot?: boolean  // animated status dot (e.g. auto-recovery active)
}

// ---------------------------------------------------------------------------
// Nav item — single structure, text animates out on collapse
// ---------------------------------------------------------------------------

function NavItem({
  to,
  icon: Icon,
  label,
  count,
  highlight,
  pulseDot,
  collapsed,
  onNavigate,
}: NavItemDef & { collapsed: boolean; onNavigate: () => void }) {

  const activeClass   = 'bg-[#2E7A8E] text-white shadow-sm'
  const inactiveClass = highlight
    ? 'text-amber-600 dark:text-[#F59E0B] hover:bg-amber-50 dark:hover:bg-[#F59E0B]/10 hover:shadow-sm'
    : 'text-[#374151] dark:text-white/60 hover:bg-[#8A9BAE]/10 dark:hover:bg-[#243447] hover:text-[#0D1B2A] dark:hover:text-white hover:shadow-sm'

  return (
    <div className="relative group">
      <NavLink
        to={to}
        onClick={onNavigate}
        className={({ isActive }) =>
          [
            'relative flex items-center px-3 py-2 rounded-lg text-sm font-medium',
            'transition-colors duration-100',
            collapsed ? 'justify-center' : '',
            isActive ? activeClass : inactiveClass,
          ].join(' ')
        }
      >
        {/* Icon — always same size, never moves */}
        <Icon size={18} strokeWidth={1.8} className="shrink-0" />

        {/* Label + count — collapse together */}
        <span
          className="flex items-center flex-1 overflow-hidden whitespace-nowrap transition-[max-width,opacity,margin] duration-300 ease-in-out"
          style={{
            maxWidth:   collapsed ? 0 : 160,
            opacity:    collapsed ? 0 : 1,
            marginLeft: collapsed ? 0 : 12,
          }}
        >
          <span className="flex-1">{label}</span>
          {count != null && count > 0 && (
            <span className="text-[11px] font-semibold tabular-nums opacity-70">
              {count > 99 ? '99+' : count}
            </span>
          )}
        </span>

      </NavLink>

      {/* Tooltip — only visible in collapsed mode on hover */}
      {collapsed && (
        <div
          className={[
            'pointer-events-none absolute left-full top-1/2 -translate-y-1/2 ml-3 z-50',
            'px-2.5 py-1.5 bg-[#0D1B2A] text-white text-xs font-medium rounded-lg',
            'whitespace-nowrap shadow-lg',
            'opacity-0 group-hover:opacity-100 transition-opacity duration-150',
          ].join(' ')}
          role="tooltip"
        >
          {label}
          <span className="absolute right-full top-1/2 -translate-y-1/2 border-4 border-transparent border-r-[#0D1B2A]" />
        </div>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Sidebar
// ---------------------------------------------------------------------------

export default function Sidebar() {
  const org               = useAuthStore((s) => s.org)
  const autoRecoveryEnabled = useAuthStore((s) => s.org?.autoRecoveryEnabled ?? false)
  const badges = useNavBadges()
  const location = useLocation()
  const isDesktop = useIsDesktop()

  const { sidebarOpen, closeSidebar, sidebarCollapsed, toggleCollapsed } = useUIStore()

  const showApprovals = org?.paymentCollectionMode === 'CONFIRMATION_FLOW'

  // Collapse only applies on desktop — mobile drawer always shows full labels
  const c = sidebarCollapsed && isDesktop

  // Auto-close drawer on mobile when navigating
  useEffect(() => {
    closeSidebar()
  }, [location.pathname, closeSidebar])

  return (
    <aside
      className={[
        'fixed left-0 bottom-0 flex flex-col z-30',
        'bg-[#F4F7F9] dark:bg-[#1B2838] border-r border-[#8A9BAE]/20 dark:border-white/10',
        // Mobile/tablet: drawer from top-14, always 220px wide, slide in/out
        'top-14 w-[220px]',
        sidebarOpen ? 'translate-x-0' : '-translate-x-full',
        // Desktop: locked visible, full height, animated width
        'lg:top-0 lg:translate-x-0',
        'lg:transition-[width] lg:duration-300 lg:ease-in-out',
        c ? 'lg:w-14' : 'lg:w-[220px]',
      ].join(' ')}
      aria-label="Main navigation"
    >

      {/* ── Desktop header (logo + collapse toggle) ─────────────────────── */}
      {/* group enables hover-reveal of toggle button when collapsed */}
      <div className="group hidden lg:flex h-14 items-center px-3 gap-2 border-b border-[#8A9BAE]/20 dark:border-white/10 shrink-0 overflow-hidden">

        {/* Logo / monogram area */}
        <div className="flex-1 min-w-0 relative h-7">
          {/* Full logo — visible when expanded */}
          <span className={`absolute inset-0 flex items-center transition-opacity duration-200 ${c ? 'opacity-0 pointer-events-none' : 'opacity-100'}`}>
            <NavLink to="/dashboard" aria-label="Go to Dashboard">
              <Logo variant="dark" size="md" />
            </NavLink>
          </span>

          {/* FC monogram — visible when collapsed, fades to toggle on hover */}
          <span className={`absolute inset-0 flex items-center justify-center transition-opacity duration-200 ${c ? 'opacity-100' : 'opacity-0 pointer-events-none'}`}>
            <span className="relative flex items-center justify-center w-8 h-8">
              {/* FC text — fades out on hover */}
              <NavLink
                to="/dashboard"
                aria-label="Go to Dashboard"
                className="absolute inset-0 flex items-center justify-center rounded-lg group-hover:opacity-0 transition-opacity duration-150"
              >
                <span className="text-sm font-bold leading-none">
                  <span className="text-[#0D1B2A] dark:text-white">F</span>
                  <span className="text-[#29B6F6]">C</span>
                </span>
              </NavLink>
              {/* Toggle — appears on hover in place of FC */}
              <button
                onClick={toggleCollapsed}
                aria-label="Expand sidebar"
                className="absolute inset-0 flex items-center justify-center rounded-lg opacity-0 group-hover:opacity-100 transition-opacity duration-150 text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#8A9BAE]/10 dark:hover:bg-[#243447]"
              >
                <ChevronRight size={16} strokeWidth={2} />
              </button>
            </span>
          </span>
        </div>

        {/* Collapse toggle — only shown in expanded mode */}
        {!c && (
          <button
            onClick={toggleCollapsed}
            aria-label="Collapse sidebar"
            className="shrink-0 flex items-center justify-center w-7 h-7 rounded-lg text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#8A9BAE]/10 dark:hover:bg-[#243447] hover:shadow-sm transition-all duration-150"
          >
            <ChevronLeft size={16} strokeWidth={2} />
          </button>
        )}
      </div>

      {/* ── Mobile drawer header (close button) ─────────────────────────── */}
      <div className="flex lg:hidden items-center justify-between px-4 py-3 border-b border-[#8A9BAE]/20 dark:border-white/10 shrink-0">
        <span className="text-xs font-semibold uppercase tracking-widest text-c-muted">
          Menu
        </span>
        <button
          onClick={closeSidebar}
          aria-label="Close menu"
          className="p-1 rounded-lg text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#8A9BAE]/10 dark:hover:bg-[#243447] transition-colors"
        >
          <X size={18} />
        </button>
      </div>

      {/* ── Primary nav ─────────────────────────────────────────────────── */}
      <nav
        className={[
          'flex-1 pt-3 pb-2 overflow-y-auto',
          c ? 'lg:px-2 lg:overflow-visible px-3' : 'px-3',
        ].join(' ')}
      >
        {/* Main */}
        {!c && <p className="px-3 mb-1 text-[10px] font-semibold uppercase tracking-widest text-c-muted">Main</p>}
        <div className="space-y-0.5">
          <NavItem to="/dashboard" icon={LayoutDashboard} label="Dashboard"  collapsed={c} onNavigate={closeSidebar} />
          {autoRecoveryEnabled && (
            <NavItem to="/recover" icon={Zap} label="Recover" highlight pulseDot collapsed={c} onNavigate={closeSidebar} />
          )}
          <NavItem to="/invoices"  icon={FileText}        label="Invoices"   collapsed={c} onNavigate={closeSidebar} />
          <NavItem to="/followups" icon={Clock}           label="Follow-ups" count={badges.followups} collapsed={c} onNavigate={closeSidebar} />
        </div>

        {/* Manage */}
        {!c && <p className="px-3 mt-4 mb-1 text-[10px] font-semibold uppercase tracking-widest text-c-muted">Manage</p>}
        {c && <hr className="my-3 border-[#8A9BAE]/20 dark:border-white/10" />}
        <div className="space-y-0.5">
          <NavItem to="/clients"   icon={Users}          label="Clients"   collapsed={c} onNavigate={closeSidebar} />
          {showApprovals && (
            <NavItem to="/approvals" icon={CheckCircle2} label="Approvals" count={badges.approvals} collapsed={c} onNavigate={closeSidebar} />
          )}
          <NavItem to="/templates" icon={LayoutTemplate} label="Templates" collapsed={c} onNavigate={closeSidebar} />
        </div>
      </nav>

      {/* ── Secondary nav ───────────────────────────────────────────────── */}
      <div
        className={[
          'pb-4 shrink-0',
          c ? 'lg:px-2 px-3' : 'px-3',
        ].join(' ')}
      >
        <hr className="mb-3 border-[#8A9BAE]/20 dark:border-white/10" />
        <div className="space-y-0.5">
          <NavItem to="/settings/org" icon={Settings} label="Settings" collapsed={c} onNavigate={closeSidebar} />
        </div>
      </div>

    </aside>
  )
}
