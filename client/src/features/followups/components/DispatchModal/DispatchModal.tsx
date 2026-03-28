import { useState } from 'react'
import { Send, X, AlertCircle } from 'lucide-react'
import { useDispatchFollowup } from '../../hooks/useFollowups'
import { useToast } from '@/store/toast.store'
import { useTemplates } from '@/features/templates/hooks/useTemplates'
import type { InvoiceResponse } from '@/types/invoice.types'
import type { FollowUpChannel } from '@/types/followup.types'
import type { TemplateResponse } from '@/types/template.types'

// ---------------------------------------------------------------------------
// Channel config
// ---------------------------------------------------------------------------

export const CHANNELS: { id: FollowUpChannel; label: string }[] = [
  { id: 'EMAIL',    label: 'Email'    },
  { id: 'SMS',      label: 'SMS'      },
  { id: 'WHATSAPP', label: 'WhatsApp' },
]

// ---------------------------------------------------------------------------
// Per-channel template selector
// ---------------------------------------------------------------------------

function ChannelTemplateSelect({
  label,
  templates,
  value,
  loading,
  onChange,
}: {
  label:     string
  templates: TemplateResponse[]
  value:     string
  loading:   boolean
  onChange:  (id: string) => void
}) {
  if (loading) {
    return (
      <div className="space-y-1">
        <p className="text-xs text-c-muted">{label} template</p>
        <div className="h-9 rounded-lg bg-[#F4F7F9] dark:bg-white/10 animate-pulse" />
      </div>
    )
  }

  const active = templates.filter((t) => t.active)

  if (active.length === 0) {
    return (
      <div className="flex items-start gap-2 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700/40 px-3 py-2">
        <AlertCircle size={14} className="text-amber-500 mt-0.5 shrink-0" />
        <p className="text-xs text-amber-700 dark:text-amber-400">
          No {label} template found.{' '}
          <a href="/templates" className="underline font-medium">Create one in Templates.</a>
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-1">
      <p className="text-xs text-c-muted">{label} template</p>
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-lg border border-c-border bg-white dark:bg-[#243447] text-sm text-[#0D1B2A] dark:text-white px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#29B6F6]/40"
      >
        <option value="">— Select template —</option>
        {active.map((t) => (
          <option key={t.id} value={t.id}>{t.name}</option>
        ))}
      </select>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Dispatch modal
// ---------------------------------------------------------------------------

interface Props {
  invoice: InvoiceResponse
  onClose: () => void
}

export default function DispatchModal({ invoice, onClose }: Props) {
  const [channels,         setChannels]         = useState<FollowUpChannel[]>(['EMAIL'])
  const [attachPdf,        setAttachPdf]        = useState(false)
  const [channelTemplates, setChannelTemplates] = useState<Partial<Record<FollowUpChannel, string>>>({})
  const [sendError,        setSendError]        = useState<string | null>(null)

  const hasNoClient = !invoice.customerId
  const toast       = useToast()
  const dispatch    = useDispatchFollowup(invoice.id)

  const { data: templatesData, isLoading, isFetching } = useTemplates({ size: 100 })
  const templatesLoading = isLoading || isFetching
  const allTemplates = templatesData?.content ?? []

  function templatesFor(ch: FollowUpChannel) {
    return allTemplates.filter((t) => t.channel === ch)
  }

  function toggleChannel(ch: FollowUpChannel) {
    setChannels((prev) =>
      prev.includes(ch) ? prev.filter((c) => c !== ch) : [...prev, ch],
    )
  }

  function setTemplate(ch: FollowUpChannel, id: string) {
    setChannelTemplates((prev) => ({ ...prev, [ch]: id }))
  }

  const channelsWithNoTemplates = channels.filter(
    (ch) => templatesFor(ch).filter((t) => t.active).length === 0,
  )
  const channelsWithoutSelection = channels.filter(
    (ch) => templatesFor(ch).filter((t) => t.active).length > 0 && !channelTemplates[ch],
  )

  const canSend =
    channels.length > 0 &&
    channelsWithNoTemplates.length === 0 &&
    channelsWithoutSelection.length === 0

  async function handleSend() {
    if (hasNoClient) return
    if (!canSend) {
      if (channelsWithoutSelection.length > 0) {
        setSendError(
          `Please select a template for: ${channelsWithoutSelection.map((c) => CHANNELS.find((ch) => ch.id === c)?.label).join(', ')}`,
        )
      }
      return
    }
    setSendError(null)
    try {
      for (const ch of channels) {
        const result = await dispatch.mutateAsync({
          channels: [ch],
          templateId: channelTemplates[ch],
          attachPdf,
        })
        const anyFailed = result.some((f) => f.status === 'FAILED')
        if (anyFailed) {
          setSendError(
            `Follow-up via ${CHANNELS.find((c) => c.id === ch)?.label} could not be delivered. Please check the client's contact details.`,
          )
          return
        }
      }
      const channelLabels = channels
        .map((ch) => CHANNELS.find((c) => c.id === ch)?.label)
        .filter(Boolean)
        .join(', ')
      toast.success(`Follow-up sent via ${channelLabels}`)
      onClose()
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        ?? 'Something went wrong. Please try again.'
      setSendError(msg)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-full max-w-sm bg-white dark:bg-[#1B2838] rounded-2xl shadow-xl p-6 space-y-5 max-h-[90vh] overflow-y-auto">

        {/* Header */}
        <div className="flex items-start justify-between">
          <div>
            <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">Send Follow-up</h2>
            <p className="text-xs text-c-muted mt-0.5">{invoice.invoiceNumber}</p>
          </div>
          <button onClick={onClose} className="text-c-muted hover:text-[#0D1B2A] dark:hover:text-white transition-colors">
            <X size={18} />
          </button>
        </div>

        {/* No-client warning — blocks sending */}
        {hasNoClient && (
          <div className="flex items-start gap-2 rounded-lg bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700/40 px-3 py-2">
            <AlertCircle size={14} className="text-amber-500 mt-0.5 shrink-0" />
            <p className="text-xs text-amber-700 dark:text-amber-400">
              No client is assigned to this invoice. Please assign a client before sending a follow-up.
            </p>
          </div>
        )}

        {/* Channel toggles */}
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-c-muted mb-2">Send via</p>
          <div className="flex flex-wrap gap-2">
            {CHANNELS.map((ch) => {
              const active = channels.includes(ch.id)
              return (
                <button
                  key={ch.id}
                  onClick={() => toggleChannel(ch.id)}
                  className={[
                    'px-3 py-1.5 rounded-lg text-sm font-medium border transition-colors',
                    active
                      ? 'border-[#2E7A8E] bg-[#2E7A8E]/10 text-[#2E7A8E] dark:border-[#29B6F6] dark:bg-[#29B6F6]/10 dark:text-[#29B6F6]'
                      : 'border-c-border text-c-muted hover:border-[#8A9BAE]/40',
                  ].join(' ')}
                >
                  {ch.label}
                </button>
              )
            })}
          </div>
          {channels.length === 0 && (
            <p className="text-xs text-red-500 mt-1.5">Select at least one channel</p>
          )}
        </div>

        {/* Per-channel template selectors */}
        {channels.length > 0 && (
          <div className="space-y-3">
            <p className="text-xs font-semibold uppercase tracking-wide text-c-muted">Templates</p>
            {channels.map((ch) => {
              const meta = CHANNELS.find((c) => c.id === ch)!
              return (
                <ChannelTemplateSelect
                  key={ch}
                  label={meta.label}
                  templates={templatesFor(ch)}
                  value={channelTemplates[ch] ?? ''}
                  loading={templatesLoading}
                  onChange={(id) => setTemplate(ch, id)}
                />
              )
            })}
          </div>
        )}

        {/* Attach PDF toggle */}
        <label className="flex items-center justify-between gap-3 cursor-pointer">
          <span className="text-sm text-[#0D1B2A] dark:text-white">Attach invoice PDF</span>
          <button
            role="switch"
            aria-checked={attachPdf}
            onClick={() => setAttachPdf(!attachPdf)}
            className={[
              'relative w-9 h-5 rounded-full transition-colors',
              attachPdf ? 'bg-[#2E7A8E]' : 'bg-[#E2E8F0] dark:bg-white/20',
            ].join(' ')}
          >
            <span className={[
              'absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white shadow transition-transform',
              attachPdf ? 'translate-x-4' : 'translate-x-0',
            ].join(' ')} />
          </button>
        </label>

        {/* Inline error */}
        {sendError && (
          <div className="flex items-start gap-2 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-700/40 px-3 py-2">
            <AlertCircle size={14} className="text-red-500 mt-0.5 shrink-0" />
            <p className="text-xs text-red-600 dark:text-red-400">{sendError}</p>
          </div>
        )}

        {/* Actions */}
        <div className="flex gap-3 pt-1">
          <button
            onClick={onClose}
            className="flex-1 py-2 rounded-lg text-sm font-medium text-c-muted hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleSend}
            disabled={hasNoClient || channels.length === 0 || dispatch.isPending}
            className="flex-1 flex items-center justify-center gap-1.5 py-2 rounded-lg text-sm font-semibold text-white disabled:opacity-50 transition-opacity hover:opacity-90"
            style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
          >
            <Send size={13} strokeWidth={2.5} />
            {dispatch.isPending ? 'Sending…' : 'Send'}
          </button>
        </div>
      </div>
    </div>
  )
}
