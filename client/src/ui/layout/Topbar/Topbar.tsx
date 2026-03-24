import { useState, useRef, useEffect, useCallback } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { Search, Plus, Bell, ChevronDown, Menu, Sun, Moon } from 'lucide-react'
import Logo from '@/ui/components/Logo/Logo'
import { useAuthStore } from '@/store/auth.store'
import { useUIStore  } from '@/store/ui.store'

const TOPBAR_LEFT_EXPANDED  = 'lg:left-[220px]'
const TOPBAR_LEFT_COLLAPSED = 'lg:left-14'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function getInitials(name: string): string {
  return name
    .split(' ')
    .slice(0, 2)
    .map((n) => n[0])
    .join('')
    .toUpperCase()
}

function useClickOutside(ref: React.RefObject<HTMLElement>, onClose: () => void) {
  useEffect(() => {
    function handle(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) onClose()
    }
    document.addEventListener('mousedown', handle)
    return () => document.removeEventListener('mousedown', handle)
  }, [ref, onClose])
}

// ---------------------------------------------------------------------------
// Notification dropdown
// ---------------------------------------------------------------------------

interface Notification { id: string; message: string }

const DEV_NOTIFICATIONS: Notification[] = [
  { id: '1', message: 'INV-042 is 12 days overdue' },
  { id: '2', message: 'Payment claim received — INV-039' },
  { id: '3', message: 'INV-044 due today' },
]

function NotificationDropdown({ items }: { items: Notification[] }) {
  return (
    <div className="absolute right-0 top-full mt-2 w-72 sm:w-80 bg-white dark:bg-[#1B2838] rounded-xl shadow-lg border border-c-border overflow-hidden z-50">
      <p className="px-4 pt-3 pb-2 text-[9px] font-bold uppercase tracking-[3px] text-c-muted">
        Notifications
      </p>
      {items.length === 0 ? (
        <p className="px-4 pb-3 text-sm text-c-muted">No new notifications</p>
      ) : (
        items.map((n) => (
          <button
            key={n.id}
            className="w-full text-left px-4 py-2.5 text-sm text-[#0D1B2A] dark:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors border-b border-c-border last:border-0"
          >
            {n.message}
          </button>
        ))
      )}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Profile dropdown
// ---------------------------------------------------------------------------

function ProfileDropdown({
  orgName,
  onAccount,
  onSettings,
  onLogout,
}: {
  orgName:    string
  onAccount:  () => void
  onSettings: () => void
  onLogout:   () => void
}) {
  return (
    <div className="absolute right-0 top-full mt-2 w-48 bg-white dark:bg-[#1B2838] rounded-xl shadow-lg border border-c-border overflow-hidden z-50">
      <div className="px-4 py-2.5 border-b border-c-border">
        <p className="text-[11px] font-semibold text-[#0D1B2A] dark:text-white truncate">{orgName}</p>
      </div>
      <button onClick={onAccount}  className="w-full text-left px-4 py-2 text-sm text-[#0D1B2A] dark:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors">My Account</button>
      <button onClick={onSettings} className="w-full text-left px-4 py-2 text-sm text-[#0D1B2A] dark:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors">Settings</button>
      <div className="border-t border-c-border">
        <button onClick={onLogout} className="w-full text-left px-4 py-2 text-sm text-[#EF4444] hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors">Logout</button>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Topbar
// ---------------------------------------------------------------------------

export default function Topbar() {
  const navigate    = useNavigate()
  const user        = useAuthStore((s) => s.user)
  const org         = useAuthStore((s) => s.org)
  const clearAuth   = useAuthStore((s) => s.clearAuth)
  const { toggleSidebar, sidebarCollapsed, theme, toggleTheme } = useUIStore()

  const [profileOpen, setProfileOpen] = useState(false)
  const [notifOpen,   setNotifOpen]   = useState(false)

  const profileRef = useRef<HTMLDivElement>(null!)
  const notifRef   = useRef<HTMLDivElement>(null!)

  const closeProfile = useCallback(() => setProfileOpen(false), [])
  const closeNotif   = useCallback(() => setNotifOpen(false),   [])

  useClickOutside(profileRef, closeProfile)
  useClickOutside(notifRef,   closeNotif)

  function handleLogout() {
    clearAuth()
    setProfileOpen(false)
    navigate('/login')
  }

  const userName  = user?.name ?? 'User'
  const orgName   = org?.name  ?? 'My Organisation'
  const hasUnread = DEV_NOTIFICATIONS.length > 0

  return (
    <header
      className={[
        'fixed top-0 right-0 h-14 z-30',
        'bg-white dark:bg-[#1B2838] border-b border-c-border',
        'flex items-center px-3 sm:px-4 gap-2 sm:gap-3',
        'left-0 transition-[left] duration-300 ease-in-out',
        sidebarCollapsed ? TOPBAR_LEFT_COLLAPSED : TOPBAR_LEFT_EXPANDED,
      ].join(' ')}
      role="banner"
    >

      {/* ── Hamburger — mobile/tablet only ──────────────────────────────── */}
      <button
        onClick={toggleSidebar}
        aria-label="Toggle navigation"
        className="lg:hidden p-2 -ml-1 text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] rounded-lg transition-colors shrink-0"
      >
        <Menu size={20} strokeWidth={1.8} />
      </button>

      {/* ── Logo — mobile/tablet only ───────────────────────────────────── */}
      <NavLink to="/dashboard" className="lg:hidden shrink-0" aria-label="Go to Dashboard">
        <Logo variant="dark" size="sm" />
      </NavLink>

      {/* ── Search — tablet+ ─────────────────────────────────────────────── */}
      <div className="hidden md:flex flex-1 items-center">
        <div className="relative w-full max-w-md">
          <Search
            size={14}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-c-muted pointer-events-none"
          />
          <input
            type="text"
            placeholder="Search invoices / clients..."
            className="w-full pl-8 pr-4 py-1.5 text-sm bg-[#F4F7F9] dark:bg-[#243447] rounded-lg border border-transparent focus:border-[#8A9BAE]/40 focus:outline-none text-[#0D1B2A] dark:text-white placeholder:text-c-muted transition-colors"
          />
        </div>
      </div>

      {/* Spacer — mobile only */}
      <div className="flex-1 md:hidden" />

      {/* ── Right zone ───────────────────────────────────────────────────── */}
      <div className="flex items-center gap-1.5 sm:gap-2 shrink-0">

        {/* Add Invoice */}
        <button
          onClick={() => navigate('/invoices')}
          aria-label="Add invoice"
          className={[
            'flex items-center justify-center text-white font-semibold',
            'hover:opacity-90 transition-opacity',
            'w-8 h-8 rounded-full',
            'sm:w-auto sm:h-auto sm:rounded-lg sm:px-3 sm:py-1.5 sm:gap-1.5 sm:text-sm',
          ].join(' ')}
          style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
        >
          <Plus size={14} strokeWidth={2.5} />
          <span className="hidden sm:inline">Add Invoice</span>
        </button>

        {/* Theme toggle */}
        <button
          onClick={toggleTheme}
          aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
          className="p-2 text-c-muted hover:text-[#0D1B2A] dark:hover:text-white rounded-lg hover:bg-[#F4F7F9] dark:hover:bg-[#243447] hover:shadow-sm transition-all"
        >
          {theme === 'dark'
            ? <Sun  size={18} strokeWidth={1.8} />
            : <Moon size={18} strokeWidth={1.8} />
          }
        </button>

        {/* Bell */}
        <div ref={notifRef} className="relative">
          <button
            onClick={() => { setNotifOpen((o) => !o); setProfileOpen(false) }}
            aria-label="Notifications"
            className="relative p-2 text-c-muted hover:text-[#0D1B2A] dark:hover:text-white rounded-lg hover:bg-[#F4F7F9] dark:hover:bg-[#243447] hover:shadow-sm transition-all"
          >
            <Bell size={18} strokeWidth={1.8} />
            {hasUnread && (
              <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-[#EF4444] rounded-full" />
            )}
          </button>
          {notifOpen && <NotificationDropdown items={DEV_NOTIFICATIONS} />}
        </div>

        {/* Avatar + profile dropdown */}
        <div ref={profileRef} className="relative">
          <button
            onClick={() => { setProfileOpen((o) => !o); setNotifOpen(false) }}
            aria-label="Profile menu"
            className="flex items-center gap-1 px-1 py-1 rounded-lg hover:bg-[#F4F7F9] dark:hover:bg-[#243447] hover:shadow-sm transition-all"
          >
            <span className="w-8 h-8 rounded-full bg-[#2E7A8E] text-white text-xs font-bold flex items-center justify-center select-none">
              {getInitials(userName)}
            </span>
            <ChevronDown size={12} className="hidden sm:block text-c-muted" />
          </button>

          {profileOpen && (
            <ProfileDropdown
              orgName={orgName}
              onAccount={() => { navigate('/settings/account'); setProfileOpen(false) }}
              onSettings={() => { navigate('/settings/org');    setProfileOpen(false) }}
              onLogout={handleLogout}
            />
          )}
        </div>

      </div>
    </header>
  )
}
