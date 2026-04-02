import { useState } from 'react'
import {
  Zap, TrendingUp, Send, Clock, Shield,
  Mail, MessageSquare, MessageCircle, Pencil,
  AlertCircle, RefreshCw, Loader2, SkipForward,
} from 'lucide-react'
import { useAuthStore } from '@/store/auth.store'
import { formatCurrency } from '@/lib/format'
import { useRecoverStats, useAutoRules, useToggleAutoRecovery, useQueueActivity, useSkipFollowUp } from '../hooks/useRecover'
import type { QueueItem, ActivityItem } from '@/api/recover.api'
import { useToggleReminderRule } from '@/features/reminders/hooks/useReminders'
import RuleModal from '@/features/reminders/modals/RuleModal'
import type { ReminderRuleResponse, ReminderChannel } from '@/types/reminder.types'
import type { TemplateResponse } from '@/types/template.types'
import EditTemplateModal from '@/features/templates/modals/EditTemplateModal'
import { getTemplate } from '@/api/template.api'

// ---------------------------------------------------------------------------
// Channel meta — shared display config
// ---------------------------------------------------------------------------

const CHANNEL_META: Record<ReminderChannel, { icon: React.ReactNode; color: string; bg: string; label: string }> = {
  EMAIL:    { icon: <Mail          size={14} strokeWidth={1.8} />, color: 'text-blue-500',  bg: 'bg-blue-500/10',  label: 'Email'    },
  SMS:      { icon: <MessageSquare size={14} strokeWidth={1.8} />, color: 'text-amber-500', bg: 'bg-amber-500/10', label: 'SMS'      },
  WHATSAPP: { icon: <MessageCircle size={14} strokeWidth={1.8} />, color: 'text-green-500', bg: 'bg-green-500/10', label: 'WhatsApp' },
}

function timingLabel(daysOffset: number): string {
  const abs = Math.abs(daysOffset)
  return `${abs} day${abs !== 1 ? 's' : ''} after due`
}

// ---------------------------------------------------------------------------
// KPI card
// ---------------------------------------------------------------------------

interface KpiCardProps {
  label:      string
  value:      string
  icon:       React.ReactNode
  iconBg:     string
  iconColor:  string
  valueColor?: string
  sub?:       string
}

function KpiCard({ label, value, icon, iconBg, iconColor, valueColor, sub }: KpiCardProps) {
  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border p-5 flex flex-col gap-4">
      <div className={`w-9 h-9 rounded-lg flex items-center justify-center ${iconBg}`}>
        <span className={iconColor}>{icon}</span>
      </div>
      <div>
        <p className={`text-2xl font-bold tracking-tight ${valueColor ?? 'text-[#0D1B2A] dark:text-white'}`}>
          {value}
        </p>
        <p className="text-sm text-c-muted mt-0.5">{label}</p>
        {sub && <p className="text-xs text-c-muted/70 mt-1">{sub}</p>}
      </div>
    </div>
  )
}

function KpiCardSkeleton() {
  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border p-5 flex flex-col gap-4 animate-pulse">
      <div className="w-9 h-9 rounded-lg bg-[#F4F7F9] dark:bg-white/10" />
      <div className="space-y-2">
        <div className="h-7 w-28 rounded bg-[#F4F7F9] dark:bg-white/10" />
        <div className="h-4 w-20 rounded bg-[#F4F7F9] dark:bg-white/10" />
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Stats error state
// ---------------------------------------------------------------------------

function StatsError({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border p-5 flex items-center gap-3">
      <AlertCircle size={16} className="text-red-400 shrink-0" />
      <p className="text-sm text-c-muted flex-1">Could not load recovery stats.</p>
      <button
        onClick={onRetry}
        className="flex items-center gap-1.5 text-xs font-medium text-[#29B6F6] hover:opacity-80 transition-opacity"
      >
        <RefreshCw size={12} />
        Retry
      </button>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function relativeTime(isoString: string): string {
  const diff = Date.now() - new Date(isoString).getTime()
  const mins = Math.floor(diff / 60_000)
  if (mins < 1)  return 'just now'
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs  < 24) return `${hrs}h ago`
  return `${Math.floor(hrs / 24)}d ago`
}

// ---------------------------------------------------------------------------
// Today's Send Queue panel
// ---------------------------------------------------------------------------

function SendQueuePanel({
  queue, totalPending, isLoading, isError, onSkip, skippingId,
}: {
  queue:       QueueItem[]
  totalPending: number
  isLoading:   boolean
  isError:     boolean
  onSkip:      (item: QueueItem) => void
  skippingId:  string | null
}) {
  const extra = totalPending - queue.length

  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
      <div className="px-5 py-3.5 border-b border-c-border flex items-center justify-between">
        <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white">Today's Send Queue</span>
        {!isLoading && !isError && totalPending > 0 && (
          <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-amber-100 dark:bg-amber-500/15 text-amber-700 dark:text-amber-400">
            {totalPending} pending
          </span>
        )}
      </div>

      {isError ? (
        <div className="flex items-center gap-2 px-5 py-8 text-c-muted text-sm">
          <AlertCircle size={14} className="text-red-400 shrink-0" />
          Could not load queue.
        </div>
      ) : isLoading ? (
        <div className="divide-y divide-c-border">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="flex items-center gap-4 px-5 py-3.5 animate-pulse">
              <div className="w-8 h-8 rounded-full bg-[#F4F7F9] dark:bg-white/10 shrink-0" />
              <div className="flex-1 space-y-1.5">
                <div className="h-3.5 w-32 rounded bg-[#F4F7F9] dark:bg-white/10" />
                <div className="h-3 w-20 rounded bg-[#F4F7F9] dark:bg-white/10" />
              </div>
              <div className="w-12 h-3 rounded bg-[#F4F7F9] dark:bg-white/10" />
              <div className="w-14 h-7 rounded-lg bg-[#F4F7F9] dark:bg-white/10" />
            </div>
          ))}
        </div>
      ) : queue.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-10 gap-1.5 text-center px-5">
          <Clock size={18} className="text-c-muted/50" strokeWidth={1.5} />
          <p className="text-sm text-c-muted">No follow-ups queued right now.</p>
        </div>
      ) : (
        <>
          <div className="divide-y divide-c-border">
            {queue.map((item) => {
              const ch = CHANNEL_META[item.channel]
              const isSkipping = skippingId === item.followUpId
              return (
                <div key={item.followUpId} className="flex items-center gap-3 px-5 py-3.5">
                  <div className={`w-8 h-8 rounded-full ${ch.bg} ${ch.color} flex items-center justify-center shrink-0`}>
                    {ch.icon}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-[#0D1B2A] dark:text-white truncate">{item.customerName}</p>
                    <p className="text-xs text-c-muted">{item.invoiceNumber}</p>
                  </div>
                  <span className={`text-xs font-semibold tabular-nums shrink-0 ${
                    item.daysOverdue >= 14 ? 'text-red-500' :
                    item.daysOverdue >= 7  ? 'text-orange-500' :
                    item.daysOverdue >= 1  ? 'text-amber-500' :
                    'text-c-muted'
                  }`}>
                    {item.daysOverdue > 0 ? `${item.daysOverdue}d` : 'Due'}
                  </span>
                  <span className="text-xs text-c-muted shrink-0 hidden sm:block">{ch.label}</span>
                  <button
                    onClick={() => onSkip(item)}
                    disabled={isSkipping}
                    className="shrink-0 flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-medium text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors disabled:opacity-40"
                    aria-label="Skip this follow-up"
                  >
                    {isSkipping
                      ? <Loader2 size={12} className="animate-spin" />
                      : <SkipForward size={12} strokeWidth={2} />
                    }
                    <span className="hidden sm:inline">Skip</span>
                  </button>
                </div>
              )
            })}
          </div>
          {extra > 0 && (
            <p className="px-5 py-2.5 text-xs text-c-muted text-center border-t border-c-border">
              + {extra} more queued for today
            </p>
          )}
        </>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Activity Log panel
// ---------------------------------------------------------------------------

function ActivityLogPanel({
  activity, isLoading, isError,
}: {
  activity:  ActivityItem[]
  isLoading: boolean
  isError:   boolean
}) {
  return (
    <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
      <div className="px-5 py-3.5 border-b border-c-border">
        <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white">Activity Log</span>
      </div>

      {isError ? (
        <div className="flex items-center gap-2 px-5 py-8 text-c-muted text-sm">
          <AlertCircle size={14} className="text-red-400 shrink-0" />
          Could not load activity.
        </div>
      ) : isLoading ? (
        <div className="divide-y divide-c-border">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="px-5 py-3.5 space-y-1.5 animate-pulse">
              <div className="h-3.5 w-40 rounded bg-[#F4F7F9] dark:bg-white/10" />
              <div className="h-3 w-28 rounded bg-[#F4F7F9] dark:bg-white/10" />
            </div>
          ))}
        </div>
      ) : activity.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-10 gap-1.5 text-center px-5">
          <Send size={18} className="text-c-muted/50" strokeWidth={1.5} />
          <p className="text-sm text-c-muted">No activity in the last 7 days.</p>
        </div>
      ) : (
        <div className="divide-y divide-c-border">
          {activity.map((item) => {
            const ch = CHANNEL_META[item.channel]
            const isSent = item.status === 'SENT'
            return (
              <div key={item.followUpId} className="px-5 py-3.5">
                <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">
                  {isSent ? `${ch.label} sent to ${item.customerName}` : `Rule skipped for ${item.customerName}`}
                </p>
                <p className="text-xs text-c-muted mt-0.5">
                  {item.invoiceNumber}
                  {item.ruleName && ` · ${item.ruleName}`}
                  {' · '}{relativeTime(item.eventAt)}
                </p>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Rule row — for Recover page (no delete, name locked for systemDefined rules)
// ---------------------------------------------------------------------------

interface RecoverRuleRowProps {
  rule:    ReminderRuleResponse
  isLast:  boolean
  onEdit:  (r: ReminderRuleResponse) => void
  onToggle:(r: ReminderRuleResponse) => void
}

function RecoverRuleRow({ rule, isLast, onEdit, onToggle }: RecoverRuleRowProps) {
  const ch = CHANNEL_META[rule.channel]

  return (
    <div className={[
      'flex items-center gap-4 px-5 py-4',
      !isLast ? 'border-b border-c-border' : '',
      !rule.active ? 'opacity-50' : '',
    ].join(' ')}>

      {/* Channel icon */}
      <div className={`w-9 h-9 rounded-full ${ch.bg} ${ch.color} flex items-center justify-center shrink-0`}>
        {ch.icon}
      </div>

      {/* Info */}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-[#0D1B2A] dark:text-white">
          {timingLabel(rule.daysOffset)}
          {rule.name && (
            <span className="ml-1.5 text-xs font-normal text-c-muted">· {rule.name}</span>
          )}
        </p>
        <p className="text-xs text-c-muted mt-0.5">
          {ch.label}
          {rule.templateName ? ` · ${rule.templateName}` : ''}
        </p>
      </div>

      {/* AUTO badge */}
      <span className="hidden sm:inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-amber-100 dark:bg-amber-500/15 text-amber-700 dark:text-amber-400 shrink-0">
        AUTO
      </span>

      {/* Toggle */}
      <button
        onClick={() => onToggle(rule)}
        aria-label={rule.active ? 'Deactivate rule' : 'Activate rule'}
        className={[
          'relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent',
          'transition-colors duration-200 focus:outline-none',
          rule.active ? 'bg-[#29B6F6]' : 'bg-[#8A9BAE]/30',
        ].join(' ')}
      >
        <span className={[
          'pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow transition duration-200',
          rule.active ? 'translate-x-4' : 'translate-x-0',
        ].join(' ')} />
      </button>

      {/* Edit */}
      <button
        onClick={() => onEdit(rule)}
        className="p-1.5 text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] rounded-lg transition-colors"
        aria-label="Edit rule"
      >
        <Pencil size={14} strokeWidth={1.8} />
      </button>
    </div>
  )
}

function RuleRowSkeleton({ isLast }: { isLast: boolean }) {
  return (
    <div className={`flex items-center gap-4 px-5 py-4 animate-pulse ${!isLast ? 'border-b border-c-border' : ''}`}>
      <div className="w-9 h-9 rounded-full bg-[#F4F7F9] dark:bg-white/10 shrink-0" />
      <div className="flex-1 space-y-1.5">
        <div className="h-4 w-36 rounded bg-[#F4F7F9] dark:bg-white/10" />
        <div className="h-3 w-24 rounded bg-[#F4F7F9] dark:bg-white/10" />
      </div>
      <div className="w-9 h-5 rounded-full bg-[#F4F7F9] dark:bg-white/10" />
      <div className="w-7 h-7 rounded-lg bg-[#F4F7F9] dark:bg-white/10" />
    </div>
  )
}

// ---------------------------------------------------------------------------
// Template card — preview of a recovery template
// ---------------------------------------------------------------------------

interface TemplateCardProps {
  templateId:   string | null
  templateName: string | null
  channel:      ReminderChannel
  isLoading:    boolean
  onEditById:   (id: string) => void
}

function TemplateCard({ templateId, templateName, channel, isLoading, onEditById }: TemplateCardProps) {
  const ch = CHANNEL_META[channel]
  if (!templateId) return null

  return (
    <div className="flex items-center gap-3 px-5 py-3.5 border-b last:border-0 border-c-border">
      <div className={`w-8 h-8 rounded-full ${ch.bg} ${ch.color} flex items-center justify-center shrink-0`}>
        {ch.icon}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-[#0D1B2A] dark:text-white truncate">
          {templateName ?? 'Unnamed template'}
        </p>
        <p className="text-xs text-c-muted">{ch.label}</p>
      </div>
      <button
        onClick={() => onEditById(templateId)}
        disabled={isLoading}
        className="p-1.5 text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] rounded-lg transition-colors shrink-0 disabled:opacity-40"
        aria-label="Edit template"
      >
        {isLoading
          ? <Loader2 size={14} className="animate-spin" />
          : <Pencil   size={14} strokeWidth={1.8} />
        }
      </button>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Auto-recovery toggle in page header
// ---------------------------------------------------------------------------

interface RecoveryToggleProps {
  enabled:   boolean
  isPending: boolean
  onToggle:  () => void
}

function RecoveryToggle({ enabled, isPending, onToggle }: RecoveryToggleProps) {
  return (
    <div className="flex items-center gap-2.5">
      <span className={[
        'text-xs font-semibold px-2 py-0.5 rounded-full',
        enabled
          ? 'bg-amber-100 dark:bg-amber-500/15 text-amber-700 dark:text-amber-400'
          : 'bg-[#F4F7F9] dark:bg-white/5 text-c-muted',
      ].join(' ')}>
        {enabled ? 'AUTO ON' : 'AUTO OFF'}
      </span>

      <button
        onClick={onToggle}
        disabled={isPending}
        aria-label={enabled ? 'Disable auto recovery' : 'Enable auto recovery'}
        className={[
          'relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent',
          'transition-colors duration-200 focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed',
          enabled ? 'bg-amber-400' : 'bg-[#8A9BAE]/30',
        ].join(' ')}
      >
        <span className={[
          'pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow transition duration-200',
          enabled ? 'translate-x-5' : 'translate-x-0',
        ].join(' ')} />
      </button>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default function RecoverPage() {
  const currency           = useAuthStore((s) => s.org?.currency ?? 'USD')
  const autoRecoveryEnabled = useAuthStore((s) => s.org?.autoRecoveryEnabled ?? false)

  const [editingRule,       setEditingRule]       = useState<ReminderRuleResponse | null>(null)
  const [editingTemplate,   setEditingTemplate]   = useState<TemplateResponse | null>(null)
  const [loadingTemplateId, setLoadingTemplateId] = useState<string | null>(null)

  const { data: stats, isLoading: statsLoading, isError: statsError, refetch: refetchStats } = useRecoverStats()
  const { data: autoRules, isLoading: rulesLoading } = useAutoRules()
  const { data: queueActivity, isLoading: qaLoading, isError: qaError } = useQueueActivity()
  const toggleAutoRecovery = useToggleAutoRecovery()
  const toggleRule         = useToggleReminderRule()
  const skipMutation       = useSkipFollowUp()
  const [skippingId, setSkippingId] = useState<string | null>(null)

  function handleSkip(item: import('@/api/recover.api').QueueItem) {
    setSkippingId(item.followUpId)
    skipMutation.mutate(
      { invoiceId: item.invoiceId, followUpId: item.followUpId },
      { onSettled: () => setSkippingId(null) }
    )
  }

  // De-duplicate templates from rules (multiple rules might share one template, unlikely but safe)
  const uniqueTemplates: { templateId: string; templateName: string | null; channel: ReminderChannel }[] = []
  const seenTemplateIds = new Set<string>()
  for (const rule of autoRules ?? []) {
    if (rule.templateId && !seenTemplateIds.has(rule.templateId)) {
      seenTemplateIds.add(rule.templateId)
      uniqueTemplates.push({ templateId: rule.templateId, templateName: rule.templateName, channel: rule.channel })
    }
  }

  function handleToggleRule(rule: ReminderRuleResponse) {
    toggleRule.mutate({ id: rule.id, active: !rule.active })
  }

  function handleToggleAutoRecovery() {
    toggleAutoRecovery.mutate(!autoRecoveryEnabled)
  }

  async function handleEditTemplate(templateId: string) {
    const orgId = useAuthStore.getState().org?.id
    if (!orgId) return
    setLoadingTemplateId(templateId)
    try {
      const full = await getTemplate(orgId, templateId)
      setEditingTemplate(full)
    } finally {
      setLoadingTemplateId(null)
    }
  }

  return (
    <>
      <div className="space-y-5">

        {/* ── Header ──────────────────────────────────────────────────────── */}
        <div className="flex items-start justify-between gap-3">
          <div>
            <div className="flex items-center gap-2 mb-0.5">
              <Zap size={18} className="text-amber-500" strokeWidth={2} />
              <h1 className="text-xl font-bold text-[#0D1B2A] dark:text-white">Recover</h1>
            </div>
            <p className="text-sm text-c-muted">
              Autonomous follow-ups for overdue invoices — no manual work needed.
            </p>
          </div>
          <RecoveryToggle
            enabled={autoRecoveryEnabled}
            isPending={toggleAutoRecovery.isPending}
            onToggle={handleToggleAutoRecovery}
          />
        </div>

        {/* ── Disabled state banner ─────────────────────────────────────── */}
        {!autoRecoveryEnabled && (
          <div className="flex items-start gap-3 px-4 py-3.5 rounded-xl bg-amber-50 dark:bg-amber-500/10 border border-amber-200 dark:border-amber-500/20">
            <Shield size={16} className="text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" strokeWidth={1.8} />
            <p className="text-sm text-amber-800 dark:text-amber-300">
              Auto Recovery is <strong>off</strong>. Toggle it on to let the engine automatically
              follow up on overdue invoices using the rules below.
            </p>
          </div>
        )}

        {/* ── KPI Strip ────────────────────────────────────────────────── */}
        {statsError ? (
          <StatsError onRetry={refetchStats} />
        ) : statsLoading ? (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            {[...Array(4)].map((_, i) => <KpiCardSkeleton key={i} />)}
          </div>
        ) : (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <KpiCard
              label="Total Recovered"
              value={formatCurrency(stats?.totalRecovered ?? 0, currency, { decimals: false })}
              sub="via auto follow-ups"
              icon={<TrendingUp size={16} strokeWidth={2} />}
              iconBg="bg-green-50 dark:bg-green-500/10"
              iconColor="text-green-500"
              valueColor={(stats?.totalRecovered ?? 0) > 0 ? 'text-green-600 dark:text-green-400' : undefined}
            />
            <KpiCard
              label="Active Rules"
              value={String(stats?.activeRules ?? 0)}
              sub="AUTO rules running"
              icon={<Zap size={16} strokeWidth={2} />}
              iconBg="bg-amber-50 dark:bg-amber-500/10"
              iconColor="text-amber-500"
            />
            <KpiCard
              label="Sent Today"
              value={String(stats?.sentToday ?? 0)}
              sub="follow-ups dispatched"
              icon={<Send size={16} strokeWidth={2} />}
              iconBg="bg-[#29B6F6]/10 dark:bg-[#29B6F6]/15"
              iconColor="text-[#29B6F6]"
            />
            <KpiCard
              label="Pending Today"
              value={String(stats?.pendingToday ?? 0)}
              sub="queued to dispatch"
              icon={<Clock size={16} strokeWidth={2} />}
              iconBg="bg-[#8A9BAE]/10 dark:bg-white/5"
              iconColor="text-c-muted"
            />
          </div>
        )}

        {/* ── Send Queue + Activity Log ─────────────────────────────────── */}
        <div className="grid grid-cols-1 lg:grid-cols-5 gap-4 items-start">
          <div className="lg:col-span-3">
            <SendQueuePanel
              queue={queueActivity?.queue ?? []}
              totalPending={queueActivity?.totalPending ?? 0}
              isLoading={qaLoading}
              isError={qaError}
              onSkip={handleSkip}
              skippingId={skippingId}
            />
          </div>
          <div className="lg:col-span-2">
            <ActivityLogPanel
              activity={queueActivity?.activity ?? []}
              isLoading={qaLoading}
              isError={qaError}
            />
          </div>
        </div>

        {/* ── Rules + Templates row ──────────────────────────────────────── */}
        <div className="grid grid-cols-1 lg:grid-cols-5 gap-4 items-start">

          {/* Automation Rules */}
          <div className="lg:col-span-3 bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
            <div className="px-5 py-3.5 border-b border-c-border flex items-center justify-between">
              <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white">
                Automation Rules
              </span>
              <span className="text-xs text-c-muted">System-managed · Edit to customise</span>
            </div>

            {rulesLoading ? (
              <>
                <RuleRowSkeleton isLast={false} />
                <RuleRowSkeleton isLast={true} />
              </>
            ) : (autoRules ?? []).length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 gap-2 text-center px-5">
                <Zap size={20} className="text-amber-400" strokeWidth={1.5} />
                <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">No AUTO rules found</p>
                <p className="text-xs text-c-muted">System rules will appear here once seeded on the server.</p>
              </div>
            ) : (
              (autoRules ?? []).map((rule, idx) => (
                <RecoverRuleRow
                  key={rule.id}
                  rule={rule}
                  isLast={idx === (autoRules ?? []).length - 1}
                  onEdit={setEditingRule}
                  onToggle={handleToggleRule}
                />
              ))
            )}
          </div>

          {/* Templates */}
          <div className="lg:col-span-2 bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
            <div className="px-5 py-3.5 border-b border-c-border">
              <span className="text-sm font-semibold text-[#0D1B2A] dark:text-white">
                Recovery Templates
              </span>
            </div>

            {rulesLoading ? (
              <div className="px-5 py-10 animate-pulse space-y-3">
                <div className="h-4 w-3/4 rounded bg-[#F4F7F9] dark:bg-white/10" />
                <div className="h-4 w-1/2 rounded bg-[#F4F7F9] dark:bg-white/10" />
              </div>
            ) : uniqueTemplates.length === 0 ? (
              <div className="px-5 py-10 text-center">
                <p className="text-sm text-c-muted">No templates linked yet.</p>
              </div>
            ) : (
              uniqueTemplates.map((t) => (
                <TemplateCard
                  key={t.templateId}
                  templateId={t.templateId}
                  templateName={t.templateName}
                  channel={t.channel}
                  isLoading={loadingTemplateId === t.templateId}
                  onEditById={handleEditTemplate}
                />
              ))
            )}
          </div>
        </div>

        {/* ── Footer note ─────────────────────────────────────────────────── */}
        <p className="text-xs text-c-muted">
          Total Recovered counts only payments attributed to AUTO follow-ups. Edits to rules and templates take effect on the next scheduler run.
        </p>

      </div>

      {/* ── Modals ─────────────────────────────────────────────────────────── */}
      {editingRule && (
        <RuleModal
          rule={editingRule}
          onClose={() => setEditingRule(null)}
        />
      )}

      {editingTemplate && editingTemplate.id && (
        <EditTemplateModal
          template={editingTemplate}
          onClose={() => setEditingTemplate(null)}
        />
      )}
    </>
  )
}
