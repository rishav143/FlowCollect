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

function StatCard({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <div className="bg-[#F4F7F9] dark:bg-[#243447] rounded-lg p-4">
      <p className="text-xs font-semibold uppercase tracking-wide text-[#8A9BAE] mb-1">{label}</p>
      <p className="text-xl font-bold text-[#0D1B2A] dark:text-white tabular-nums">{value}</p>
      {sub && <p className="text-xs text-[#8A9BAE] mt-0.5">{sub}</p>}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default function ClientDetailPage() {
  const { id }   = useParams<{ id: string }>()
  const navigate = useNavigate()
  const currency = useAuthStore((s) => s.org?.currency ?? 'INR')

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
        <p className="text-sm text-[#8A9BAE]">Client not found.</p>
        <button onClick={() => navigate('/clients')} className="text-sm text-[#29B6F6] hover:underline">
          ← Back to Clients
        </button>
      </div>
    )
  }

  // ── Stats ──────────────────────────────────────────────────────────────────
  function fmt(n: number) {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 0 }).format(n)
  }

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
          className="flex items-center gap-1.5 text-sm text-[#8A9BAE] hover:text-[#0D1B2A] dark:hover:text-white transition-colors"
        >
          <ArrowLeft size={15} strokeWidth={2} />
          Clients
        </button>

        {/* Header card */}
        <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-[#F4F7F9] dark:border-white/10 p-5 space-y-4">
          {/* Name + risk + edit */}
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div className="min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <h1 className="text-2xl font-bold text-[#0D1B2A] dark:text-white">{customer.name}</h1>
                <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${riskMeta.chip}`}>
                  {riskMeta.dot} {riskMeta.label}
                </span>
              </div>
              {customer.companyName && (
                <p className="flex items-center gap-1.5 text-sm text-[#8A9BAE] mt-1">
                  <Building2 size={13} /> {customer.companyName}
                </p>
              )}
            </div>
            <button
              onClick={() => setShowEdit(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium text-[#8A9BAE] border border-[#F4F7F9] dark:border-white/10 hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors"
            >
              <Pencil size={13} />
              Edit
            </button>
          </div>

          {/* Contact info */}
          <div className="flex flex-wrap gap-x-5 gap-y-1.5">
            {customer.email && (
              <a href={`mailto:${customer.email}`} className="flex items-center gap-1.5 text-sm text-[#8A9BAE] hover:text-[#0D1B2A] dark:hover:text-white transition-colors">
                <Mail size={13} /> {customer.email}
              </a>
            )}
            {customer.phone && (
              <a href={`tel:${customer.phone}`} className="flex items-center gap-1.5 text-sm text-[#8A9BAE] hover:text-[#0D1B2A] dark:hover:text-white transition-colors">
                <Phone size={13} /> {customer.phone}
              </a>
            )}
            {customer.address && (
              <span className="flex items-center gap-1.5 text-sm text-[#8A9BAE]">
                <MapPin size={13} /> {customer.address}
              </span>
            )}
          </div>

          {/* Stats */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
            <StatCard label="Outstanding"      value={fmt(outstanding)}  sub="remaining balance" />
            <StatCard label="Open Invoices"    value={String(openCount)} sub="issued / partial"  />
            <StatCard label="Avg Payment Delay" value={invoices.length > 0 ? `${avgDelay}d` : '—'} sub="days past due" />
          </div>

          {/* Automation toggle */}
          <div className="flex items-center justify-between pt-2 border-t border-[#F4F7F9] dark:border-white/10">
            <div>
              <p className="text-sm font-medium text-[#0D1B2A] dark:text-white">Auto-reminders</p>
              <p className="text-xs text-[#8A9BAE] mt-0.5">
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
        <div className="bg-white dark:bg-[#1B2838] rounded-xl border border-[#F4F7F9] dark:border-white/10 p-5">
          <p className="text-xs font-semibold uppercase tracking-wide text-[#8A9BAE] mb-4">
            Invoices ({invoices.length})
          </p>
          <ClientInvoiceHistory
            invoices={invoices}
            currency={currency}
            isLoading={loadingInvoices}
          />
        </div>
      </div>

      {showEdit && customer && (
        <EditClientModal customer={customer} onClose={() => setShowEdit(false)} />
      )}
    </>
  )
}
