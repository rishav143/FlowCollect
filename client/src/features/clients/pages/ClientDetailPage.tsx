import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Pencil, Mail, Phone, MapPin, Building2 } from 'lucide-react'
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
  const org         = useAuthStore((s) => s.org)
  const currency    = org?.currency ?? 'INR'
  const orgId       = org?.id ?? ''
  const orgTimezone = org?.timezone

  const [showEdit, setShowEdit] = useState(false)

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

  const avgDelay = computeAvgDelay(invoices, orgTimezone)
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

          {/* Stats row */}
          <div className="pt-3 border-t border-c-border">
            <div className="grid grid-cols-3 gap-4 sm:flex sm:gap-6 sm:items-start">
              <Stat
                label="Outstanding"
                value={formatCurrency(outstanding, currency, { decimals: false })}
                sub="remaining balance"
                valueColor={outstanding > 0 ? undefined : 'text-green-600 dark:text-green-400'}
              />
              <div className="hidden sm:block w-px bg-c-border self-stretch" />
              <Stat label="Open Invoices"     value={String(openCount)} sub="issued or partial" />
              <div className="hidden sm:block w-px bg-c-border self-stretch" />
              <Stat label="Avg Payment Delay" value={invoices.length > 0 ? `${avgDelay}d` : '—'} sub="days to pay" />
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
