import { useState, useEffect } from 'react'
import { createPortal } from 'react-dom'
import { X } from 'lucide-react'
import { useCreateReminderRule, useUpdateReminderRule } from '../hooks/useReminders'
import { useTemplates } from '@/features/templates/hooks/useTemplates'
import type { ReminderRuleResponse, ReminderChannel } from '@/types/reminder.types'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type Direction = 'BEFORE' | 'ON' | 'AFTER'

interface FormState {
  days:       string
  direction:  Direction
  channel:    ReminderChannel
  templateId: string
  name:       string
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const CHANNELS: ReminderChannel[] = ['EMAIL', 'SMS', 'WHATSAPP']
const CHANNEL_LABEL: Record<ReminderChannel, string> = {
  EMAIL: 'Email', SMS: 'SMS', WHATSAPP: 'WhatsApp',
}

function offsetToForm(offset: number): Pick<FormState, 'days' | 'direction'> {
  if (offset < 0) return { direction: 'BEFORE', days: String(Math.abs(offset)) }
  if (offset > 0) return { direction: 'AFTER',  days: String(offset) }
  return { direction: 'ON', days: '1' }
}

function formToOffset(form: FormState): number {
  if (form.direction === 'ON') return 0
  const d = Math.max(1, parseInt(form.days) || 1)
  return form.direction === 'BEFORE' ? -d : d
}

const EMPTY: FormState = { days: '3', direction: 'BEFORE', channel: 'EMAIL', templateId: '', name: '' }

// ---------------------------------------------------------------------------
// RuleModal — handles both create and edit
// ---------------------------------------------------------------------------

interface Props {
  rule?:    ReminderRuleResponse   // present → edit mode
  onClose:  () => void
}

export default function RuleModal({ rule, onClose }: Props) {
  const isEdit = !!rule

  const [form, setForm] = useState<FormState>(EMPTY)

  useEffect(() => {
    if (rule) {
      const { days, direction } = offsetToForm(rule.triggerOffset)
      setForm({
        days,
        direction,
        channel:    rule.channel,
        templateId: rule.templateId ?? '',
        name:       rule.name ?? '',
      })
    }
  }, [rule])

  const create = useCreateReminderRule()
  const update = useUpdateReminderRule()
  const isPending = create.isPending || update.isPending

  const { data: templatesData } = useTemplates(
    form.channel !== 'EMAIL' ? { channel: form.channel } : undefined,
  )
  const templates = templatesData?.content ?? []

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  function handleChannelChange(ch: ReminderChannel) {
    setForm((prev) => ({ ...prev, channel: ch, templateId: '' }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const body = {
      triggerOffset: formToOffset(form),
      channel:       form.channel,
      templateId:    form.templateId || null,
      name:          form.name.trim() || undefined,
    }
    if (isEdit) {
      await update.mutateAsync({ id: rule!.id, body })
    } else {
      await create.mutateAsync(body)
    }
    onClose()
  }

  return createPortal(
    <>
      <div className="fixed inset-0 z-[200] bg-black/60" onClick={onClose} aria-hidden="true" />

      <div className="fixed inset-0 z-[200] flex items-center justify-center p-4 pointer-events-none">
        <div
          className="pointer-events-auto w-full max-w-md bg-white dark:bg-[#1B2838] rounded-2xl shadow-2xl"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Header */}
          <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-c-border">
            <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">
              {isEdit ? 'Edit Reminder Rule' : 'New Reminder Rule'}
            </h2>
            <button
              onClick={onClose}
              className="p-1.5 text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] rounded-lg transition-colors"
            >
              <X size={18} />
            </button>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="px-6 py-5 space-y-5">

            {/* Timing */}
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wide text-c-muted mb-2">
                When to send
              </label>
              <div className="flex items-center gap-2">
                <select
                  value={form.direction}
                  onChange={(e) => set('direction', e.target.value as Direction)}
                  className="flex-1 text-sm rounded-lg border border-c-border bg-transparent text-[#0D1B2A] dark:text-white px-3 py-2.5 focus:outline-none focus:border-[#8A9BAE]/40 transition-colors"
                >
                  <option value="BEFORE">Before due date</option>
                  <option value="ON">On due date</option>
                  <option value="AFTER">After due date</option>
                </select>

                {form.direction !== 'ON' && (
                  <div className="flex items-center gap-1.5 shrink-0">
                    <input
                      type="number"
                      min={1}
                      max={90}
                      value={form.days}
                      onChange={(e) => set('days', e.target.value)}
                      className="w-16 text-sm rounded-lg border border-c-border bg-transparent text-[#0D1B2A] dark:text-white px-3 py-2.5 text-center focus:outline-none focus:border-[#8A9BAE]/40 transition-colors tabular-nums"
                    />
                    <span className="text-sm text-c-muted whitespace-nowrap">days</span>
                  </div>
                )}
              </div>
            </div>

            {/* Channel */}
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wide text-c-muted mb-2">
                Channel
              </label>
              <div className="flex gap-2">
                {CHANNELS.map((ch) => (
                  <button
                    key={ch}
                    type="button"
                    onClick={() => handleChannelChange(ch)}
                    className={[
                      'flex-1 py-2 text-sm font-medium rounded-lg border transition-colors',
                      form.channel === ch
                        ? 'border-[#29B6F6] text-[#29B6F6] bg-[#29B6F6]/5'
                        : 'border-c-border text-c-muted hover:text-[#0D1B2A] dark:hover:text-white',
                    ].join(' ')}
                  >
                    {CHANNEL_LABEL[ch]}
                  </button>
                ))}
              </div>
            </div>

            {/* Template */}
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wide text-c-muted mb-2">
                Template <span className="normal-case font-normal">(optional)</span>
              </label>
              <select
                value={form.templateId}
                onChange={(e) => set('templateId', e.target.value)}
                className="w-full text-sm rounded-lg border border-c-border bg-transparent text-[#0D1B2A] dark:text-white px-3 py-2.5 focus:outline-none focus:border-[#8A9BAE]/40 transition-colors"
              >
                <option value="">No template</option>
                {templates.map((t) => (
                  <option key={t.id} value={t.id}>{t.name}</option>
                ))}
              </select>
            </div>

            {/* Name */}
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wide text-c-muted mb-2">
                Label <span className="normal-case font-normal">(optional)</span>
              </label>
              <input
                type="text"
                value={form.name}
                onChange={(e) => set('name', e.target.value)}
                placeholder="e.g. First nudge, Final warning"
                className="w-full text-sm rounded-lg border border-c-border bg-transparent text-[#0D1B2A] dark:text-white placeholder:text-c-muted px-3 py-2.5 focus:outline-none focus:border-[#8A9BAE]/40 transition-colors"
              />
            </div>

            {/* Submit */}
            <button
              type="submit"
              disabled={isPending}
              className="w-full py-2.5 rounded-lg text-sm font-semibold text-white disabled:opacity-50 hover:opacity-90 transition-opacity"
              style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
            >
              {isPending ? 'Saving…' : isEdit ? 'Save Changes' : 'Add Rule'}
            </button>
          </form>
        </div>
      </div>
    </>,
    document.body,
  )
}
