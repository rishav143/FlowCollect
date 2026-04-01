import { useState, useEffect } from 'react'
import { createPortal } from 'react-dom'
import { X, ChevronDown, ChevronUp } from 'lucide-react'
import { useCreateReminderRule, useUpdateReminderRule } from '../hooks/useReminders'
import { useTemplates } from '@/features/templates/hooks/useTemplates'
import type {
  ReminderRuleResponse,
  ReminderChannel,
  ReminderTriggerType,
  ReminderRuleRequest,
} from '@/types/reminder.types'

// ---------------------------------------------------------------------------
// Types & constants
// ---------------------------------------------------------------------------

type Direction = 'BEFORE' | 'ON' | 'AFTER'

interface FormState {
  name:                  string
  days:                  string
  direction:             Direction
  channel:               ReminderChannel
  templateId:            string
  active:                boolean
  // Advanced
  advancedEnabled:       boolean
  maxOccurrences:        string
  cycleIntervalDays:     string
  // Per-occurrence templates (only used when cyclic; index = occurrence - 1)
  occurrenceTemplateIds: string[]
}

const DIRECTION_TO_TRIGGER: Record<Direction, ReminderTriggerType> = {
  BEFORE: 'BEFORE_DUE_DATE',
  ON:     'ON_DUE_DATE',
  AFTER:  'AFTER_DUE_DATE',
}

const TRIGGER_TO_DIRECTION: Record<ReminderTriggerType, Direction> = {
  BEFORE_DUE_DATE: 'BEFORE',
  ON_DUE_DATE:     'ON',
  AFTER_DUE_DATE:  'AFTER',
}

const CHANNELS: ReminderChannel[] = ['EMAIL', 'SMS', 'WHATSAPP']
const CHANNEL_LABEL: Record<ReminderChannel, string> = {
  EMAIL: 'Email', SMS: 'SMS', WHATSAPP: 'WhatsApp',
}

const OCCURRENCE_OPTIONS = Array.from({ length: 10 }, (_, i) => i + 1)  // 1–10

const CYCLE_OPTIONS = [
  { value: '7',  label: '7 days'  },
  { value: '14', label: '14 days' },
  { value: '28', label: '28 days' },
  { value: '60', label: '60 days' },
]

const EMPTY: FormState = {
  name:                  '',
  days:                  '3',
  direction:             'BEFORE',
  channel:               'EMAIL',
  templateId:            '',
  active:                true,
  advancedEnabled:       false,
  maxOccurrences:        '1',
  cycleIntervalDays:     '7',
  occurrenceTemplateIds: [],
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const VALID_CYCLE_VALUES = CYCLE_OPTIONS.map((o) => o.value)

function snapCycleInterval(val: number): string {
  const s = String(val)
  if (VALID_CYCLE_VALUES.includes(s)) return s
  // Snap to closest valid option
  const closest = CYCLE_OPTIONS.reduce((prev, curr) =>
    Math.abs(parseInt(curr.value) - val) < Math.abs(parseInt(prev.value) - val) ? curr : prev,
  )
  return closest.value
}

function formToDaysOffset(form: Pick<FormState, 'direction' | 'days'>): number {
  if (form.direction === 'ON') return 0
  const d = Math.max(1, parseInt(form.days) || 1)
  return form.direction === 'BEFORE' ? -d : d
}

// ---------------------------------------------------------------------------
// Toggle — shared visual
// ---------------------------------------------------------------------------

function Toggle({ on, onToggle }: { on: boolean; onToggle: () => void }) {
  return (
    <button
      type="button"
      onClick={onToggle}
      className={[
        'relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent',
        'transition-colors duration-200 focus:outline-none',
        on ? 'bg-[#29B6F6]' : 'bg-[#8A9BAE]/30',
      ].join(' ')}
    >
      <span className={[
        'pointer-events-none inline-block h-4 w-4 rounded-full bg-white shadow transition duration-200',
        on ? 'translate-x-4' : 'translate-x-0',
      ].join(' ')} />
    </button>
  )
}

// ---------------------------------------------------------------------------
// Timeline diagram
// ---------------------------------------------------------------------------

type DiagramItem =
  | { kind: 'send'; n: number; offset: number }
  | { kind: 'due' }
  | { kind: 'more'; count: number }

function Timeline({
  direction,
  days,
  count,
  interval,
}: {
  direction: Direction
  days:      number
  count:     number
  interval:  number
}) {
  const MAX_SHOW  = 2
  const showCount = Math.min(count, MAX_SHOW)
  const hidden    = count - showCount

  const entityOffset = direction === 'BEFORE' ? -days : direction === 'ON' ? 0 : days
  const offsets = Array.from({ length: showCount }, (_, i) => entityOffset + interval * i)
  offsets.sort((a, b) => a - b)

  const sends: DiagramItem[] = offsets.map((offset, idx) => ({ kind: 'send', n: idx + 1, offset }))

  // Build ordered flat item list — pill sits just before DUE for BEFORE, at end for AFTER
  const items: DiagramItem[] =
    direction === 'BEFORE'
      ? [...sends, ...(hidden > 0 ? [{ kind: 'more' as const, count: hidden }] : []), { kind: 'due' }]
      : [{ kind: 'due' }, ...sends, ...(hidden > 0 ? [{ kind: 'more' as const, count: hidden }] : [])]

  function sublabel(item: DiagramItem): string {
    if (item.kind === 'due')  return 'Due date'
    if (item.kind === 'more') return ''
    const abs = Math.abs(item.offset)
    if (item.offset < 0)  return `${abs}d before`
    if (item.offset === 0) return 'on due date'
    return `${abs}d after`
  }

  return (
    <div className="pt-3 mt-1 border-t border-c-border">
      <p className="text-[10px] font-semibold uppercase tracking-wider text-c-muted mb-2">
        Schedule preview
      </p>
      {/* Flat flex row: nodes are shrink-0, connectors are flex-1 — guaranteed no overflow */}
      <div className="flex items-start w-full select-none">
        {items.map((item, idx) => (
          <div key={idx} className="contents">
            {/* flex-1 connector between items */}
            {idx > 0 && (
              <div className="flex-1 min-w-[8px] pt-[18px]">
                <div className="w-full h-px bg-c-border" />
              </div>
            )}

            {item.kind === 'more' ? (
              /* +N more badge */
              <div className="flex flex-col items-center shrink-0 gap-1 pt-1">
                <span className="text-[9px] font-semibold text-c-muted bg-[#F4F7F9] dark:bg-[#243447] px-2 py-1 rounded-full whitespace-nowrap">
                  +{item.count} more
                </span>
              </div>
            ) : (
              /* Send / Due node */
              <div className="flex flex-col items-center shrink-0 gap-1">
                <div className={[
                  'w-8 h-8 rounded-full flex items-center justify-center border-2',
                  item.kind === 'due'
                    ? 'bg-[#2E7A8E]/10 border-[#2E7A8E]'
                    : 'bg-[#29B6F6]/10 border-[#29B6F6]',
                ].join(' ')}>
                  {item.kind === 'due'
                    ? <span className="text-[7px] font-black text-[#2E7A8E] leading-none">DUE</span>
                    : <span className="text-[10px] font-bold text-[#29B6F6] leading-none">{item.n}</span>
                  }
                </div>
                <span className="text-[9px] text-center text-c-muted leading-tight whitespace-nowrap">
                  {sublabel(item)}
                </span>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// RuleModal — create & edit
// ---------------------------------------------------------------------------

interface Props {
  rule?:   ReminderRuleResponse
  onClose: () => void
}

export default function RuleModal({ rule, onClose }: Props) {
  const isEdit = !!rule

  const [form,        setForm]        = useState<FormState>(EMPTY)
  const [submitError, setSubmitError] = useState<string | null>(null)

  useEffect(() => {
    if (rule) {
      const clampedMax = Math.min(10, Math.max(1, rule.maxOccurrences))
      const isCyclicRule = clampedMax > 1
      // Populate per-occurrence slots: use existing overrides if present, otherwise fill with the single templateId
      const occSlots: string[] = isCyclicRule
        ? Array.from({ length: clampedMax }, (_, i) =>
            rule.occurrenceTemplateIds?.[i] ?? rule.templateId ?? ''
          )
        : []
      setForm({
        name:                  rule.name ?? '',
        days:                  String(Math.abs(rule.daysOffset) || 3),
        direction:             TRIGGER_TO_DIRECTION[rule.triggerType],
        channel:               rule.channel,
        templateId:            rule.templateId ?? '',
        active:                rule.active,
        advancedEnabled:       isCyclicRule,
        maxOccurrences:        String(clampedMax),
        cycleIntervalDays:     snapCycleInterval(rule.cycleIntervalDays),
        occurrenceTemplateIds: occSlots,
      })
    }
  }, [rule])

  const create    = useCreateReminderRule()
  const update    = useUpdateReminderRule()
  const isPending = create.isPending || update.isPending

  const { data: templatesData } = useTemplates({ channel: form.channel })
  const templates = templatesData?.content ?? []

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  function handleChannelChange(ch: ReminderChannel) {
    setForm((prev) => ({
      ...prev,
      channel:               ch,
      templateId:            '',
      occurrenceTemplateIds: prev.occurrenceTemplateIds.map(() => ''),
    }))
  }

  function handleDirectionChange(dir: Direction) {
    setForm((prev) => ({
      ...prev,
      direction:             dir,
      // Reset cycle state whenever direction changes to avoid stale/invalid combos
      maxOccurrences:        '1',
      cycleIntervalDays:     '7',
      occurrenceTemplateIds: [],
    }))
  }

  function handleMaxOccurrencesChange(value: string) {
    const next = Math.max(1, parseInt(value) || 1)
    setForm((prev) => {
      const current = prev.occurrenceTemplateIds
      let nextSlots: string[]
      if (next <= 1) {
        nextSlots = []
      } else if (next > current.length) {
        // Grow: fill new slots with global templateId as a helpful default
        nextSlots = [
          ...current,
          ...Array(next - current.length).fill(prev.templateId || ''),
        ]
      } else {
        // Shrink: truncate
        nextSlots = current.slice(0, next)
      }
      return { ...prev, maxOccurrences: value, occurrenceTemplateIds: nextSlots }
    })
  }

  // Derived computed values
  const days        = Math.max(1, parseInt(form.days) || 1)
  const interval    = Math.max(1, parseInt(form.cycleIntervalDays) || 1)
  const rawMaxOcc   = form.advancedEnabled ? Math.max(1, parseInt(form.maxOccurrences) || 1) : 1
  // Clamp to the highest occurrence option still valid given current days & interval,
  // so that stale form state doesn't make isCyclic true when the select shows "1 time".
  const maxOcc      = form.direction === 'BEFORE'
    ? Math.min(rawMaxOcc, Math.max(1, ...OCCURRENCE_OPTIONS.filter(n => interval * (n - 1) < days)))
    : rawMaxOcc
  const isCyclic    = form.direction !== 'ON' && maxOcc > 1

  // For BEFORE rules: an occurrence count n is valid if interval * (n-1) < days
  function occIsValid(n: number) {
    if (form.direction !== 'BEFORE') return true
    return interval * (n - 1) < days
  }
  // For BEFORE rules: a cycle interval val is valid if val * (maxOcc-1) < days
  function cycleIsValid(val: number) {
    if (form.direction !== 'BEFORE') return true
    return val * (maxOcc - 1) < days
  }

  function validationError(): string | null {
    if (isCyclic) {
      for (let i = 0; i < maxOcc; i++) {
        if (!form.occurrenceTemplateIds[i]) {
          return `Please select a template for send ${i + 1}.`
        }
      }
    } else {
      if (!form.templateId) return 'Please select a template.'
    }
    return null
  }

  const error = validationError()

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (error) return
    setSubmitError(null)

    const body: ReminderRuleRequest = {
      name:                  form.name.trim() || undefined,
      daysOffset:            formToDaysOffset(form),
      triggerType:           DIRECTION_TO_TRIGGER[form.direction],
      channel:               form.channel,
      templateId:            isCyclic ? form.occurrenceTemplateIds[0] : (form.templateId || null),
      active:                form.active,
      maxOccurrences:        maxOcc,
      cycleIntervalDays:     isCyclic ? interval : 0,
      occurrenceTemplateIds: isCyclic ? form.occurrenceTemplateIds.slice(0, maxOcc) : [],
    }

    try {
      if (isEdit) {
        await update.mutateAsync({ id: rule!.id, body })
      } else {
        await create.mutateAsync(body)
      }
      onClose()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setSubmitError(msg ?? 'Something went wrong. Please try again.')
    }
  }

  const showTimeline = form.advancedEnabled && form.direction !== 'ON'

  return createPortal(
    <>
      <div className="fixed inset-0 z-[200] bg-black/60 backdrop-blur-sm" onClick={onClose} aria-hidden="true" />

      <div className="fixed inset-0 z-[200] flex items-center justify-center p-4 pointer-events-none">
        <div
          className="pointer-events-auto w-full max-w-md bg-white dark:bg-[#1B2838] rounded-2xl shadow-2xl flex flex-col max-h-[90vh]"
          onClick={(e) => e.stopPropagation()}
        >
          {/* ── Header ─────────────────────────────────────────────── */}
          <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-c-border shrink-0">
            <h2 className="text-base font-semibold text-[#0D1B2A] dark:text-white">
              {isEdit ? 'Edit Reminder Rule' : 'New Reminder Rule'}
            </h2>
            <div className="flex items-center gap-3">
              {/* Active toggle — only shown when editing an existing rule */}
              {isEdit && (
                <div className="flex items-center gap-2">
                  <span className="text-xs text-c-muted select-none">
                    {form.active ? 'Active' : 'Inactive'}
                  </span>
                  <Toggle on={form.active} onToggle={() => set('active', !form.active)} />
                </div>
              )}
              <button
                type="button"
                onClick={onClose}
                className="p-1.5 text-c-muted hover:text-[#0D1B2A] dark:hover:text-white hover:bg-[#F4F7F9] dark:hover:bg-[#243447] rounded-lg transition-colors"
              >
                <X size={18} />
              </button>
            </div>
          </div>

          {/* ── Scrollable form ──────────────────────────────────── */}
          <form onSubmit={handleSubmit} className="px-6 py-5 space-y-5 overflow-y-auto flex-1">

            {/* Timing */}
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wide text-c-muted mb-2">
                When to send
              </label>
              <div className="flex items-center gap-2">
                <select
                  value={form.direction}
                  onChange={(e) => handleDirectionChange(e.target.value as Direction)}
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

            {/* Template — hidden when cyclic (per-occurrence selectors replace it) */}
            {!isCyclic && (
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wide text-c-muted mb-2">
                  Template
                </label>
                <select
                  value={form.templateId}
                  onChange={(e) => set('templateId', e.target.value)}
                  className="w-full text-sm rounded-lg border border-c-border bg-transparent text-[#0D1B2A] dark:text-white px-3 py-2.5 focus:outline-none focus:border-[#8A9BAE]/40 transition-colors"
                >
                  <option value="">Select a template…</option>
                  {templates.map((t) => (
                    <option key={t.id} value={t.id}>{t.name}</option>
                  ))}
                </select>
              </div>
            )}

            {/* Name */}
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wide text-c-muted mb-2">
                Name
              </label>
              <input
                type="text"
                value={form.name}
                onChange={(e) => set('name', e.target.value)}
                placeholder="e.g. First nudge, Final warning"
                readOnly={!!rule?.systemDefined}
                title={rule?.systemDefined ? 'System rule names cannot be changed' : undefined}
                className={[
                  'w-full text-sm rounded-lg border border-c-border bg-transparent text-[#0D1B2A] dark:text-white placeholder:text-c-muted px-3 py-2.5 focus:outline-none focus:border-[#8A9BAE]/40 transition-colors',
                  rule?.systemDefined ? 'opacity-50 cursor-not-allowed' : '',
                ].join(' ')}
              />
            </div>

            {/* ── Advanced options ──────────────────────────────── */}
            <div className="rounded-xl border border-c-border overflow-hidden">

              {/* Toggle header — full-row click area */}
              <div
                role="button"
                tabIndex={0}
                onClick={() => set('advancedEnabled', !form.advancedEnabled)}
                onKeyDown={(e) => e.key === 'Enter' && set('advancedEnabled', !form.advancedEnabled)}
                className="flex items-center justify-between px-4 py-3 cursor-pointer hover:bg-[#F4F7F9] dark:hover:bg-[#243447] transition-colors select-none"
              >
                <div className="flex items-center gap-2">
                  {form.advancedEnabled
                    ? <ChevronUp   size={14} className="text-[#29B6F6] shrink-0" />
                    : <ChevronDown size={14} className="text-c-muted shrink-0" />
                  }
                  <span className={`text-sm font-medium ${form.advancedEnabled ? 'text-[#29B6F6]' : 'text-c-muted'}`}>
                    Advanced options
                  </span>
                </div>
                {/* Visual-only toggle — click handled by parent div */}
                <div className={[
                  'relative inline-flex h-5 w-9 rounded-full border-2 border-transparent transition-colors duration-200',
                  form.advancedEnabled ? 'bg-[#29B6F6]' : 'bg-[#8A9BAE]/30',
                ].join(' ')}>
                  <span className={[
                    'inline-block h-4 w-4 rounded-full bg-white shadow transition duration-200',
                    form.advancedEnabled ? 'translate-x-4' : 'translate-x-0',
                  ].join(' ')} />
                </div>
              </div>

              {/* Advanced fields */}
              {form.advancedEnabled && (
                <div className="px-4 pb-4 pt-3 space-y-4 border-t border-c-border bg-[#F4F7F9]/60 dark:bg-[#243447]/30">

                  {/* Occurrences + Repeat every — inline */}
                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wide text-c-muted mb-2">
                      Occurrences
                    </label>
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-sm text-c-muted whitespace-nowrap">Send</span>
                      <select
                        value={form.maxOccurrences}
                        onChange={(e) => handleMaxOccurrencesChange(e.target.value)}
                        disabled={form.direction === 'ON'}
                        className="text-sm rounded-lg border border-c-border bg-white dark:bg-[#1B2838] text-[#0D1B2A] dark:text-white px-3 py-2 focus:outline-none focus:border-[#8A9BAE]/40 transition-colors disabled:opacity-40"
                      >
                        {OCCURRENCE_OPTIONS.filter(occIsValid).map((n) => (
                          <option key={n} value={String(n)}>{n} {n === 1 ? 'time' : 'times'}</option>
                        ))}
                      </select>
                      {isCyclic && (
                        <>
                          <span className="text-sm text-c-muted whitespace-nowrap">repeat every</span>
                          <select
                            value={form.cycleIntervalDays}
                            onChange={(e) => set('cycleIntervalDays', e.target.value)}
                            className="text-sm rounded-lg border border-c-border bg-white dark:bg-[#1B2838] text-[#0D1B2A] dark:text-white px-3 py-2 focus:outline-none focus:border-[#8A9BAE]/40 transition-colors"
                          >
                            {CYCLE_OPTIONS.filter((opt) => cycleIsValid(parseInt(opt.value))).map((opt) => (
                              <option key={opt.value} value={opt.value}>{opt.label}</option>
                            ))}
                          </select>
                        </>
                      )}
                    </div>
                  </div>


                  {/* Per-occurrence template selectors — shown only when cyclic */}
                  {isCyclic && (
                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wide text-c-muted mb-2">
                        Template per send
                      </label>
                      <div className="space-y-2">
                        {Array.from({ length: maxOcc }, (_, i) => (
                          <div key={i} className="flex items-center gap-2">
                            <span className="text-xs text-c-muted whitespace-nowrap w-12 shrink-0">
                              Send {i + 1}
                            </span>
                            <select
                              value={form.occurrenceTemplateIds[i] ?? ''}
                              onChange={(e) => {
                                const next = [...form.occurrenceTemplateIds]
                                next[i] = e.target.value
                                set('occurrenceTemplateIds', next)
                              }}
                              className="flex-1 text-sm rounded-lg border border-c-border bg-white dark:bg-[#1B2838] text-[#0D1B2A] dark:text-white px-3 py-2 focus:outline-none focus:border-[#8A9BAE]/40 transition-colors"
                            >
                              <option value="">Select a template…</option>
                              {templates.map((t) => (
                                <option key={t.id} value={t.id}>{t.name}</option>
                              ))}
                            </select>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Timeline diagram */}
                  {showTimeline && (
                    <Timeline
                      direction={form.direction}
                      days={days}
                      count={maxOcc}
                      interval={interval}
                    />
                  )}
                </div>
              )}
            </div>

            {/* Validation / submit error */}
            {(error || submitError) && (
              <p className="text-xs text-red-500 -mt-2">{error ?? submitError}</p>
            )}

            {/* Submit */}
            <button
              type="submit"
              disabled={isPending || !!error}
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
