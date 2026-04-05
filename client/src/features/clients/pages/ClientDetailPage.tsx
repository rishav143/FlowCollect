import React, { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Pencil, Mail, Phone, MapPin, Building2, Sparkles, RefreshCw, Loader2 } from 'lucide-react'
import { useAuthStore } from '@/store/auth.store'
import { useClientDetail, useClientInvoices } from '../hooks/useClientDetail'
import { useToggleAutomation } from '../hooks/useClients'
import {
  computeAvgDelay,
  getRisk,
  RISK_META,
} from '../components/ClientMetricsStrip/ClientMetricsStrip'
import ClientInvoiceHistory from '../components/ClientInvoiceHistory/ClientInvoiceHistory'
import EditClientModal from '../modals/EditClientModal'
import { formatCurrency } from '@/lib/format'
import { getClientInsight } from '@/api/ai.api'

// ---------------------------------------------------------------------------
// Insight text helpers (mirrors AiInsightBanner)
// ---------------------------------------------------------------------------

function parseBullets(raw: string): string[] {
  return raw
    .split('\n')
    .map((l) => l.replace(/^-\s*/, '').trim())
    .filter(Boolean)
}

function reformatAmounts(text: string, currency: string): string {
  return text.replace(
    /(₹|\$|€|£|¥|₩|A\$|S\$|AED\s?)([\d,]+(?:\.\d{1,2})?)/g,
    (_match, _symbol, numStr) => {
      const num = parseFloat(numStr.replace(/,/g, ''))
      if (isNaN(num)) return _match
      return formatCurrency(num, currency, { decimals: numStr.includes('.') })
    },
  )
}

function renderBold(text: string): React.ReactNode {
  const parts = text.split(/\*\*(.+?)\*\*/g)
  return parts.map((part, i) =>
    i % 2 === 1 ? <strong key={i}>{part}</strong> : part,
  )
}

// ---------------------------------------------------------------------------
// AI Insight stat (reusable across responsive layouts)
// ---------------------------------------------------------------------------

function AiInsightStat({
  insightText, insightLoading, insightError, currency, onGenerate,
}: {
  insightText:    string | null
  insightLoading: boolean
  insightError:   string | null
  currency:       string
  onGenerate:     () => void
}) {
  return (
    <div className="flex-1 min-w-0">
      <div className="flex items-center gap-2 mb-1">
        <div className="flex items-center gap-1">
          <Sparkles size={11} className="text-[#29B6F6]" strokeWidth={2} />
          <span className="text-sm text-c-muted">AI Insight</span>
        </div>
        <button
          onClick={onGenerate}
          disabled={insightLoading}
          className={[
            'flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-medium transition-colors disabled:opacity-60',
            insightText
              ? 'text-c-muted border border-c-border hover:bg-[#F4F7F9] dark:hover:bg-[#243447]'
              : 'text-white',
          ].join(' ')}
          style={!insightText ? { background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' } : undefined}
        >
          {insightLoading ? (
            <><Loader2 size={10} className="animate-spin" /> Analyzing…</>
          ) : insightText ? (
            <><RefreshCw size={10} /> Refresh</>
          ) : (
            <><Sparkles size={10} /> Generate</>
          )}
        </button>
      </div>

      {!insightText && !insightLoading && !insightError && (
        <p className="text-xs text-c-muted/70">On-demand payment behavior profile.</p>
      )}
      {insightLoading && (
        <p className="text-xs text-c-muted">Analyzing payment history…</p>
      )}
      {insightError && !insightLoading && (
        <p className="text-xs text-red-500">{insightError}</p>
      )}
      {insightText && !insightLoading && (
        <ul className="space-y-1 mt-0.5">
          {parseBullets(insightText).map((bullet, i) => (
            <li key={i} className="flex items-start gap-1.5">
              <span className="mt-1.5 h-1.5 w-1.5 rounded-full bg-[#29B6F6] shrink-0" />
              <span className="text-xs text-[#0D1B2A] dark:text-[#C9D9E8] leading-snug">
                {renderBold(reformatAmounts(bullet, currency))}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Automation toggle
// ---------------------------------------------------------------------------

function Toggle({ enabled, onChange, isLoading }: { enabled: boolean; onChange: (v: boolean) => void; isLoading: boolean }) {
  return (
    <button
      role="switch"
      aria-checked={enabled}
      onClick={() => !isLoading && onChange(!enabled)}
      disabled={isLoading}
      className={[
        'relative w-10 h-5 rounded-full transition-colors disabled:opacity-60',
        enabled ? 'bg-[#2E7A8E]' : 'bg-[#E2E8F0] dark:bg-white/20',
      ].join(' ')}
    >
      <span
        className={[
          'absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white shadow transition-transform',
          enabled ? 'translate-x-5' : 'translate-x-0',
        ].join(' ')}
      />
    </button>
  )
}

// ---------------------------------------------------------------------------
// Stat card
// ---------------------------------------------------------------------------

function Stat({ label, value, sub, valueColor }: { label: string; value: string; sub?: string; valueColor?: string }) {
  return (
    <div>
      <p className={`text-xl font-bold tabular-nums ${valueColor ?? 'text-[#0D1B2A] dark:text-white'}`}>{value}</p>
      <p className="text-sm text-c-muted mt-0.5">{label}</p>
      {sub && <p className="text-xs text-c-muted/70 mt-0.5">{sub}</p>}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default function ClientDetailPage() {
  const { id }   = useParams<{ id: string }>()
  const navigate = useNavigate()
  const org      = useAuthStore((s) => s.org)
  const currency = org?.currency ?? 'INR'
  const orgId    = org?.id ?? ''

  const [showEdit, setShowEdit] = useState(false)

  const [insightText,    setInsightText]    = useState<string | null>(null)
  const [insightLoading, setInsightLoading] = useState(false)
  const [insightError,   setInsightError]   = useState<string | null>(null)

  async function generateInsight() {
    if (!id) return
    setInsightLoading(true)
    setInsightError(null)
    try {
      const result = await getClientInsight(orgId, id)
      setInsightText(result.insights)
    } catch {
      setInsightError('Could not generate insight. Please try again.')
    } finally {
      setInsightLoading(false)
    }
  }

  const { data: customer, isLoading: loadingCustomer } = useClientDetail(id!)
  const { data: invoices = [], isLoading: loadingInvoices } = useClientInvoices(id!)
  const toggleAuto = useToggleAutomation()

  // ── Loading ────────────────────────────────────────────────────────────────
  if (loadingCustomer) {
    return (
      <div className="space-y-5 animate-pulse">
        <div className="h-6 w-32 rounded bg-[#F4F7F9] dark:bg-white/10" />
        <div className="h-28 rounded-xl bg-[#F4F7F9] dark:bg-white/10" />
        <div className="h-40 rounded-xl bg-[#F4F7F9] dark:bg-white/10" />
      </div>
    )
  }

  if (!customer) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[40vh] gap-3">
        <p className="text-sm text-c-muted">Client not found.</p>
        <button onClick={() => navigate('/clients')} className="text-sm text-[#29B6F6] hover:underline">
          ← Back to Clients
        </button>
      </div>
    )
  }

  // ── Stats ──────────────────────────────────────────────────────────────────
  const outstanding = invoices
    .filter((i) => i.lifeCycleStatus !== 'PAID' && i.lifeCycleStatus !== 'CANCELLED')
    .reduce((s, i) => s + i.remainingAmount, 0)

  const openCount = invoices.filter(
    (i) => i.lifeCycleStatus === 'ISSUED' || i.lifeCycleStatus === 'PARTIALLY_PAID',
  ).length

  const avgDelay = computeAvgDelay(invoices)
  const risk     = getRisk(avgDelay, invoices.length > 0)
  const riskMeta = RISK_META[risk]

  return (
    <>
      <div className="space-y-5">

        {/* Back */}
        <button
          onClick={() => navigate('/clients')}
          className="flex items-center gap-1.5 text-sm text-c-muted hover:text-[#0D1B2A] dark:hover:text-white transition-colors"
        >
          <ArrowLeft size={15} strokeWidth={2} />
          Clients
        </button>

        {/* Header card */}
        <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border p-5 space-y-4">
          {/* Name + risk + edit */}
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div className="min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <h1 className="text-2xl font-bold text-[#0D1B2A] dark:text-white">{customer.name}</h1>
                <span className={`text-sm font-medium ${riskMeta.text}`}>{riskMeta.label}</span>
              </div>
              {customer.companyName && (
                <p className="flex items-center gap-1.5 text-sm text-c-muted mt-1">
                  <Building2 size={13} /> {customer.companyName}
                </p>
              )}
            </div>
            <button
              onClick={() => setShowEdit(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium text-c-muted border border-c-border hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors"
            >
              <Pencil size={13} />
              Edit
            </button>
          </div>

          {/* Contact info */}
          <div className="flex flex-wrap gap-x-5 gap-y-1.5">
            {customer.email && (
              <a href={`mailto:${customer.email}`} className="flex items-center gap-1.5 text-sm text-c-muted hover:text-[#0D1B2A] dark:hover:text-white transition-colors">
                <Mail size={13} /> {customer.email}
              </a>
            )}
            {customer.phone && (
              <a href={`tel:${customer.phone}`} className="flex items-center gap-1.5 text-sm text-c-muted hover:text-[#0D1B2A] dark:hover:text-white transition-colors">
                <Phone size={13} /> {customer.phone}
              </a>
            )}
            {customer.address && (
              <span className="flex items-center gap-1.5 text-sm text-c-muted">
                <MapPin size={13} /> {customer.address}
              </span>
            )}
          </div>

          {/* Stats + AI Insight row */}
          <div className="pt-3 border-t border-c-border">
            {/* On md+ screens: all in one horizontal row with dividers */}
            <div className="hidden md:flex gap-6 items-start">
              <Stat
                label="Outstanding"
                value={formatCurrency(outstanding, currency, { decimals: false })}
                sub="remaining balance"
                valueColor={outstanding > 0 ? undefined : 'text-green-600 dark:text-green-400'}
              />
              <div className="w-px bg-c-border self-stretch" />
              <Stat label="Open Invoices"     value={String(openCount)} sub="issued or partial" />
              <div className="w-px bg-c-border self-stretch" />
              <Stat label="Avg Payment Delay" value={invoices.length > 0 ? `${avgDelay}d` : '—'} sub="days to pay" />
              <div className="w-px bg-c-border self-stretch" />
              <AiInsightStat
                insightText={insightText}
                insightLoading={insightLoading}
                insightError={insightError}
                currency={currency}
                onGenerate={generateInsight}
              />
            </div>

            {/* On sm and below: 2-column grid for stats, AI insight full-width below */}
            <div className="md:hidden space-y-4">
              <div className="grid grid-cols-2 gap-x-6 gap-y-4">
                <Stat
                  label="Outstanding"
                  value={formatCurrency(outstanding, currency, { decimals: false })}
                  sub="remaining balance"
                  valueColor={outstanding > 0 ? undefined : 'text-green-600 dark:text-green-400'}
                />
                <Stat label="Open Invoices"     value={String(openCount)} sub="issued or partial" />
                <Stat label="Avg Payment Delay" value={invoices.length > 0 ? `${avgDelay}d` : '—'} sub="days to pay" />
              </div>
              <div className="border-t border-c-border pt-3">
                <AiInsightStat
                  insightText={insightText}
                  insightLoading={insightLoading}
                  insightError={insightError}
                  currency={currency}
                  onGenerate={generateInsight}
                />
              </div>
            </div>
          </div>

          {/* Automation toggle */}
          <div className="flex items-center justify-between pt-2 border-t border-c-border">
            <div>
              <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">Auto-reminders</p>
              <p className="text-xs text-c-muted mt-0.5">
                {customer.automationEnabled
                  ? 'Automated follow-ups are active for this client'
                  : 'Automated follow-ups are paused for this client'}
              </p>
            </div>
            <Toggle
              enabled={customer.automationEnabled}
              isLoading={toggleAuto.isPending}
              onChange={(enabled) => toggleAuto.mutate({ id: customer.id, enabled })}
            />
          </div>
        </div>

        {/* Invoice history */}
        <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-c-border overflow-hidden">
          <div className="flex items-center justify-between px-5 py-4 border-b border-c-border">
            <h2 className="text-sm font-semibold text-[#0D1B2A] dark:text-white">Invoice History</h2>
            <span className="text-xs text-c-muted">{invoices.length} invoice{invoices.length !== 1 ? 's' : ''}</span>
          </div>
          <div className="p-5">
          <ClientInvoiceHistory
            invoices={invoices}
            currency={currency}
            isLoading={loadingInvoices}
          />
          </div>
        </div>
      </div>

      {showEdit && customer && (
        <EditClientModal customer={customer} onClose={() => setShowEdit(false)} />
      )}
    </>
  )
}
