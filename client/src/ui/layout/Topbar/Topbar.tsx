import { useState, useRef, useEffect, useCallback } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import GlobalSearch from '@/ui/components/GlobalSearch/GlobalSearch'
import { Plus, Bell, ChevronDown, Menu, Sun, Moon, CheckCheck, Circle } from 'lucide-react'
import Logo from '@/ui/components/Logo/Logo'
import { useAuthStore } from '@/store/auth.store'
import { useUIStore  } from '@/store/ui.store'
import { useNotifications, type AppNotification } from '@/hooks/useNotifications'
import { useNotificationStore } from '@/store/notification.store'
import { usePlan } from '@/hooks/usePlan'

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

function timeAgo(isoDate: string): string {
  const diff = Date.now() - new Date(isoDate).getTime()
  const mins  = Math.floor(diff / 60_000)
  const hours = Math.floor(diff / 3_600_000)
  const days  = Math.floor(diff / 86_400_000)
  if (mins  < 1)  return 'Just now'
  if (mins  < 60) return `${mins}m ago`
  if (hours < 24) return `${hours}h ago`
  if (days  < 7)  return `${days}d ago`
  return new Date(isoDate).toLocaleDateString('en', { day: 'numeric', month: 'short' })
}

function notifIcon(type: AppNotification['type']): React.ReactNode {
  if (type === 'overdue')   return <span className="w-2 h-2 rounded-full bg-red-500 shrink-0 mt-1.5" />
  if (type === 'due-today') return <span className="w-2 h-2 rounded-full bg-amber-400 shrink-0 mt-1.5" />
  return                           <span className="w-2 h-2 rounded-full bg-violet-500 shrink-0 mt-1.5" />
}

// ---------------------------------------------------------------------------
// Notification dropdown
// ---------------------------------------------------------------------------

function NotificationDropdown({ onClose }: { onClose: () => void }) {
  const navigate     = useNavigate()
  const { notifications, isLoading } = useNotifications()
  const { readIds, markRead, markUnread, markAllRead } = useNotificationStore()

  function handleItemClick(n: AppNotification) {
    markRead(n.id)
    onClose()
    navigate(n.actionUrl)
  }

  function handleToggleRead(e: React.MouseEvent, n: AppNotification) {
    e.stopPropagation()
    if (readIds.includes(n.id)) {
      markUnread(n.id)
    } else {
      markRead(n.id)
    }
  }

  const allIds     = notifications.map((n) => n.id)
  const unreadCount = notifications.filter((n) => !readIds.includes(n.id)).length

  return (
    <div className="absolute right-0 top-full mt-2 w-80 sm:w-96 bg-white dark:bg-[#1B2838] rounded-xl shadow-xl border border-c-border overflow-hidden z-50">

      {/* Header */}
      <div className="flex items-center justify-between px-4 pt-3.5 pb-2.5 border-b border-c-border">
        <div className="flex items-center gap-2">
          <p className="text-[11px] font-bold uppercase tracking-[2.5px] text-c-muted">Notifications</p>
          {unreadCount > 0 && (
            <span className="text-[10px] font-semibold bg-[#EF4444] text-white rounded-full px-1.5 py-0.5 leading-none">
              {unreadCount}
            </span>
          )}
        </div>
        {unreadCount > 0 && (
          <button
            onClick={() => markAllRead(allIds)}
            className="flex items-center gap-1 text-xs text-[#29B6F6] hover:text-[#0288D1] font-medium transition-colors"
            title="Mark all as read"
          >
            <CheckCheck size={13} strokeWidth={2} />
            Mark all read
          </button>
        )}
      </div>

      {/* Body */}
      <div className="max-h-[420px] overflow-y-auto">
        {isLoading ? (
          <div className="space-y-px py-1">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="flex items-start gap-3 px-4 py-3 animate-pulse">
                <div className="w-2 h-2 rounded-full bg-[#F4F7F9] dark:bg-white/10 mt-1.5 shrink-0" />
                <div className="flex-1 space-y-1.5">
                  <div className="h-3.5 w-48 rounded bg-[#F4F7F9] dark:bg-white/10" />
                  <div className="h-3 w-36 rounded bg-[#F4F7F9] dark:bg-white/10" />
                </div>
              </div>
            ))}
          </div>
        ) : notifications.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-10 gap-2">
            <Bell size={22} className="text-c-muted/50" strokeWidth={1.5} />
            <p className="text-sm text-c-muted">You're all caught up</p>
            <p className="text-xs text-c-muted/60">No pending notifications</p>
          </div>
        ) : (
          notifications.map((n) => {
            const isRead = readIds.includes(n.id)
            return (
              <button
                key={n.id}
                onClick={() => handleItemClick(n)}
                className={[
                  'w-full text-left flex items-start gap-3 px-4 py-3',
                  'border-b border-c-border last:border-0',
                  'hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors group',
                  isRead ? 'opacity-60' : '',
                ].join(' ')}
              >
                {notifIcon(n.type)}
                <div className="flex-1 min-w-0">
                  <p className={`text-sm leading-snug text-[#0D1B2A] dark:text-white ${isRead ? 'font-normal' : 'font-semibold'}`}>
                    {n.title}
                  </p>
                  <p className="text-xs text-c-muted mt-0.5 truncate">{n.body}</p>
                  <p className="text-[10px] text-c-muted/60 mt-1">{timeAgo(n.sortKey)}</p>
                </div>
                {/* Toggle read/unread */}
                <button
                  onClick={(e) => handleToggleRead(e, n)}
                  title={isRead ? 'Mark as unread' : 'Mark as read'}
                  className="shrink-0 mt-0.5 opacity-0 group-hover:opacity-100 p-1 rounded hover:bg-[#E5EAEF] dark:hover:bg-white/10 transition-all"
                >
                  <Circle
                    size={10}
                    strokeWidth={2.5}
                    className={isRead ? 'text-c-muted/40' : 'text-[#29B6F6] fill-[#29B6F6]'}
                  />
                </button>
              </button>
            )
          })
        )}
      </div>

      {/* Footer — only when there are items */}
      {!isLoading && notifications.length > 0 && (
        <div className="px-4 py-2.5 border-t border-c-border">
          <p className="text-[10px] text-c-muted/60 text-center">
            Showing overdue, due today, and pending approvals
          </p>
        </div>
      )}

    </div>
  )
}

// ---------------------------------------------------------------------------
// Profile dropdown
// ---------------------------------------------------------------------------

function ProfileDropdown({
  orgName,
  onSettings,
  onLogout,
}: {
  orgName:    string
  onSettings: () => void
  onLogout:   () => void
}) {
  return (
    <div className="absolute right-0 top-full mt-2 w-48 bg-white dark:bg-[#1B2838] rounded-xl shadow-lg border border-c-border overflow-hidden z-50">
      <div className="px-4 py-2.5 border-b border-c-border">
        <p className="text-[11px] font-semibold text-[#0D1B2A] dark:text-white truncate">{orgName}</p>
      </div>
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
  const { unreadCount } = useNotifications()

  const { atInvoiceLimit, activeInvoiceCount, invoiceLimit } = usePlan()

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
    navigate('/login', { replace: true })
  }

  const userName = user?.name ?? 'User'
  const orgName  = org?.name  ?? 'My Organisation'

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
        <GlobalSearch />
      </div>

      {/* Spacer — mobile only */}
      <div className="flex-1 md:hidden" />

      {/* ── Right zone ───────────────────────────────────────────────────── */}
      <div className="flex items-center gap-1.5 sm:gap-2 shrink-0">

        {/* Add Invoice */}
        <div className="relative group">
          <button
            onClick={() => atInvoiceLimit ? navigate('/settings/billing') : navigate('/invoices?create=true')}
            aria-label={atInvoiceLimit ? 'Upgrade to add more invoices' : 'Add invoice'}
            className={[
              'flex items-center justify-center font-semibold',
              'transition-opacity',
              'w-8 h-8 rounded-full',
              'sm:w-auto sm:h-auto sm:rounded-lg sm:px-3 sm:py-1.5 sm:gap-1.5 sm:text-sm',
              atInvoiceLimit
                ? 'text-c-muted border border-c-border bg-white dark:bg-[#1B2838] hover:border-[#29B6F6] hover:text-[#29B6F6]'
                : 'text-white hover:opacity-90',
            ].join(' ')}
            style={!atInvoiceLimit ? { background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' } : undefined}
          >
            <Plus size={14} strokeWidth={2.5} />
            <span className="hidden sm:inline">
              {atInvoiceLimit ? 'Upgrade' : 'Add Invoice'}
            </span>
          </button>
          {atInvoiceLimit && (
            <div className="absolute right-0 top-full mt-2 w-52 bg-[#0D1B2A] text-white text-xs rounded-lg px-3 py-2 shadow-lg z-50 hidden group-hover:block pointer-events-none">
              {activeInvoiceCount}/{invoiceLimit} active invoices used. Upgrade to PRO for unlimited.
            </div>
          )}
        </div>

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
            {unreadCount > 0 && (
              <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-[#EF4444] rounded-full" />
            )}
          </button>
          {notifOpen && <NotificationDropdown onClose={() => setNotifOpen(false)} />}
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
              onSettings={() => { navigate('/settings/org'); setProfileOpen(false) }}
              onLogout={handleLogout}
            />
          )}
        </div>

      </div>
    </header>
  )
}
