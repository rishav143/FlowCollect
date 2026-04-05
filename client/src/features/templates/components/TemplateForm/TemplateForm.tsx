import { useRef, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Sparkles, RefreshCw, Loader2, Check, X, ChevronDown } from 'lucide-react'
import { templateSchema, type TemplateFormValues } from '@/features/templates/schemas/template.schema'
import { useAuthStore } from '@/store/auth.store'
import { generateTemplate, enhanceTemplate } from '@/api/ai.api'

// ─── Constants ────────────────────────────────────────────────────────────────

const BASE_PLACEHOLDERS = [
  { label: 'Customer Name',     value: '{{customerName}}'    },
  { label: 'Organization Name', value: '{{organizationName}}'},
  { label: 'Invoice #',         value: '{{invoiceNumber}}'   },
  { label: 'Amount Due',        value: '{{remainingAmount}}' },
  { label: 'Due Date',          value: '{{dueDate}}'         },
]

const BODY_LIMIT: Record<string, number> = {
  SMS:      320,
  WHATSAPP: 1000,
  EMAIL:    2000,
}

const TONES = ['POLITE', 'NEUTRAL', 'FIRM'] as const
type Tone = typeof TONES[number]

const TONE_LABEL: Record<Tone, string> = {
  POLITE:  'Polite',
  NEUTRAL: 'Neutral',
  FIRM:    'Firm',
}

// ─── Cursor-aware text insertion ──────────────────────────────────────────────

function insertAtCursor(
  el: HTMLTextAreaElement | HTMLInputElement,
  text: string,
): string {
  const start    = el.selectionStart ?? el.value.length
  const end      = el.selectionEnd   ?? el.value.length
  const newValue = el.value.slice(0, start) + text + el.value.slice(end)
  const nextPos  = start + text.length
  requestAnimationFrame(() => {
    el.focus()
    el.setSelectionRange(nextPos, nextPos)
  })
  return newValue
}

// ─── Shared Tailwind classes ──────────────────────────────────────────────────

const inputClass =
  'w-full px-3 py-2 text-sm rounded-lg border border-transparent ' +
  'bg-[#F4F7F9] dark:bg-[#243447] ' +
  'text-[#0D1B2A] dark:text-white ' +
  'placeholder:text-c-muted ' +
  'focus:border-[#8A9BAE]/40 focus:outline-none transition-colors'

const labelClass = 'block text-xs font-medium text-[#0D1B2A] dark:text-white/80 mb-1'

const varBtnClass =
  'px-2 py-1 text-[11px] font-medium rounded transition-colors ' +
  'text-[#2E7A8E] dark:text-[#4FC3F7] ' +
  'bg-[#2E7A8E]/10 dark:bg-[#4FC3F7]/10 ' +
  'hover:bg-[#2E7A8E]/20 dark:hover:bg-[#4FC3F7]/20'

// ─── AI suggestion preview ────────────────────────────────────────────────────

interface AiPreview {
  subject: string | null
  body:    string
  label:   string          // what action generated this
}

// ─── AI Toolbar ───────────────────────────────────────────────────────────────

function AiToolbar({
  loading,
  onPolish,
  onGenerate,
  onToneShift,
  currentTone,
}: {
  loading:     boolean
  onPolish:    () => void
  onGenerate:  () => void
  onToneShift: (tone: Tone) => void
  currentTone: Tone
}) {
  const [toneOpen, setToneOpen] = useState(false)

  const otherTones = TONES.filter((t) => t !== currentTone)

  return (
    <div className="flex items-center gap-1.5 flex-wrap">
      {/* Polish */}
      <button
        type="button"
        onClick={onPolish}
        disabled={loading}
        title="AI-polish the message while preserving its tone and all placeholders"
        className={[
          'inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs font-semibold border transition-all',
          loading
            ? 'opacity-50 cursor-not-allowed border-c-border text-c-muted'
            : 'border-violet-200 dark:border-violet-500/30 text-violet-600 dark:text-violet-400 bg-violet-50 dark:bg-violet-500/10 hover:bg-violet-100 dark:hover:bg-violet-500/20',
        ].join(' ')}
      >
        {loading
          ? <Loader2 size={11} className="animate-spin" />
          : <Sparkles size={11} strokeWidth={2} />
        }
        Polish
      </button>

      {/* Rewrite in tone */}
      <div className="relative">
        <button
          type="button"
          onClick={() => !loading && setToneOpen((v) => !v)}
          disabled={loading}
          title="Rewrite the message in a different tone"
          className={[
            'inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs font-semibold border transition-all',
            loading
              ? 'opacity-50 cursor-not-allowed border-c-border text-c-muted'
              : 'border-blue-200 dark:border-blue-500/30 text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-500/10 hover:bg-blue-100 dark:hover:bg-blue-500/20',
          ].join(' ')}
        >
          <RefreshCw size={11} strokeWidth={2} />
          Rewrite tone
          <ChevronDown size={10} strokeWidth={2.5} className={`transition-transform ${toneOpen ? 'rotate-180' : ''}`} />
        </button>

        {toneOpen && (
          <>
            <div className="fixed inset-0 z-10" onClick={() => setToneOpen(false)} />
            <div className="absolute left-0 top-full mt-1.5 z-20 bg-white dark:bg-[#243447] border border-c-border rounded-xl shadow-lg overflow-hidden min-w-[130px]">
              {otherTones.map((tone) => (
                <button
                  key={tone}
                  type="button"
                  onClick={() => { setToneOpen(false); onToneShift(tone) }}
                  className="w-full text-left px-3.5 py-2.5 text-xs font-medium text-[#0D1B2A] dark:text-white hover:bg-[#F4F7F9] dark:hover:bg-white/5 transition-colors"
                >
                  {TONE_LABEL[tone]}
                </button>
              ))}
            </div>
          </>
        )}
      </div>

      {/* Generate fresh */}
      <button
        type="button"
        onClick={onGenerate}
        disabled={loading}
        title="Generate a brand-new message from scratch using the selected channel and tone"
        className={[
          'inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg text-xs font-semibold border transition-all',
          loading
            ? 'opacity-50 cursor-not-allowed border-c-border text-c-muted'
            : 'border-amber-200 dark:border-amber-500/30 text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-500/10 hover:bg-amber-100 dark:hover:bg-amber-500/20',
        ].join(' ')}
      >
        <Sparkles size={11} strokeWidth={2} />
        Generate fresh
      </button>
    </div>
  )
}

// ─── Diff renderer — highlight added/changed lines ───────────────────────────

function PreviewPane({
  preview,
  channel,
  onAccept,
  onDiscard,
}: {
  preview:   AiPreview
  channel:   string
  onAccept:  () => void
  onDiscard: () => void
}) {
  return (
    <div className="rounded-xl border border-violet-200 dark:border-violet-500/30 overflow-hidden bg-violet-50/40 dark:bg-violet-500/5">
      {/* Header */}
      <div className="flex items-center justify-between px-3.5 py-2.5 border-b border-violet-200 dark:border-violet-500/20 bg-violet-50 dark:bg-violet-500/10">
        <div className="flex items-center gap-2">
          <Sparkles size={12} strokeWidth={2} className="text-violet-500" />
          <span className="text-xs font-semibold text-violet-700 dark:text-violet-300">
            AI suggestion · {preview.label}
          </span>
        </div>
        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={onDiscard}
            className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold border border-c-border text-c-muted hover:bg-white dark:hover:bg-[#1B2838] transition-colors"
          >
            <X size={11} strokeWidth={2.5} /> Discard
          </button>
          <button
            type="button"
            onClick={onAccept}
            className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold text-white transition-opacity hover:opacity-90"
            style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
          >
            <Check size={11} strokeWidth={2.5} /> Apply
          </button>
        </div>
      </div>

      {/* Preview content */}
      <div className="px-3.5 py-3 space-y-2.5">
        {channel === 'EMAIL' && preview.subject && (
          <div>
            <p className="text-[10px] font-semibold uppercase tracking-wider text-violet-500/70 dark:text-violet-400/60 mb-1">
              Subject
            </p>
            <p className="text-xs text-[#0D1B2A] dark:text-white leading-relaxed font-medium">
              {preview.subject}
            </p>
          </div>
        )}
        <div>
          {channel === 'EMAIL' && preview.subject && (
            <p className="text-[10px] font-semibold uppercase tracking-wider text-violet-500/70 dark:text-violet-400/60 mb-1">
              Body
            </p>
          )}
          <pre className="text-xs text-[#0D1B2A] dark:text-white leading-relaxed whitespace-pre-wrap font-sans">
            {preview.body}
          </pre>
        </div>
      </div>
    </div>
  )
}

// ─── Component ────────────────────────────────────────────────────────────────

interface Props {
  defaultValues?: Partial<TemplateFormValues>
  onSubmit:       (values: TemplateFormValues) => void
  isPending:      boolean
  submitLabel?:   string
}

export default function TemplateForm({
  defaultValues,
  onSubmit,
  isPending,
  submitLabel = 'Save',
}: Props) {
  const orgId       = useAuthStore((s) => s.org?.id ?? '')
  const paymentMode = useAuthStore((s) => s.org?.paymentCollectionMode)

  const PLACEHOLDERS = [
    ...BASE_PLACEHOLDERS,
    ...(paymentMode === 'PAYMENT_LINK'
      ? [{ label: 'Payment Link',      value: '{{paymentLink}}'      }]
      : []),
    ...(paymentMode === 'CONFIRMATION_FLOW'
      ? [{ label: 'Confirmation Link', value: '{{confirmationLink}}' }]
      : []),
  ]

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<TemplateFormValues>({
    resolver: zodResolver(templateSchema),
    defaultValues: {
      channel: 'EMAIL',
      tone:    'POLITE',
      ...defaultValues,
    },
  })

  const bodyRef    = useRef<HTMLTextAreaElement | null>(null)
  const subjectRef = useRef<HTMLInputElement    | null>(null)

  const channel   = watch('channel')
  const tone      = watch('tone') as Tone
  const body      = watch('body') ?? ''
  const subject   = watch('subject') ?? ''
  const bodyLimit = BODY_LIMIT[channel] ?? 2000
  const bodyOver  = body.length > bodyLimit

  const bodyReg    = register('body')
  const subjectReg = register('subject')

  // AI state
  const [aiLoading,  setAiLoading]  = useState(false)
  const [aiError,    setAiError]    = useState<string | null>(null)
  const [aiPreview,  setAiPreview]  = useState<AiPreview | null>(null)

  function insertIntoBody(variable: string) {
    if (!bodyRef.current) return
    const newValue = insertAtCursor(bodyRef.current, variable)
    setValue('body', newValue, { shouldValidate: true, shouldDirty: true })
  }

  function insertIntoSubject(variable: string) {
    if (!subjectRef.current) return
    const newValue = insertAtCursor(subjectRef.current, variable)
    setValue('subject', newValue || undefined, { shouldValidate: true, shouldDirty: true })
  }

  // Accept AI suggestion — apply to form fields
  function acceptPreview() {
    if (!aiPreview) return
    setValue('body', aiPreview.body, { shouldValidate: true, shouldDirty: true })
    if (channel === 'EMAIL' && aiPreview.subject) {
      setValue('subject', aiPreview.subject, { shouldValidate: true, shouldDirty: true })
    }
    setAiPreview(null)
  }

  async function runAi(action: 'polish' | 'generate' | 'tone', targetTone?: Tone) {
    if (!orgId || aiLoading) return
    setAiError(null)
    setAiPreview(null)
    setAiLoading(true)
    try {
      let result: { subject: string | null; body: string }
      let label: string

      if (action === 'generate') {
        result = await generateTemplate(orgId, channel, tone)
        label  = `Generated · ${TONE_LABEL[tone]}`
      } else if (action === 'tone' && targetTone) {
        result = await enhanceTemplate(orgId, channel, targetTone, body, subject || undefined)
        label  = `Rewritten as ${TONE_LABEL[targetTone]}`
        // Also shift the tone selector to match
        setValue('tone', targetTone)
      } else {
        // polish — enhance with current tone
        result = await enhanceTemplate(orgId, channel, tone, body, subject || undefined)
        label  = `Polished · ${TONE_LABEL[tone]}`
      }

      setAiPreview({ subject: result.subject, body: result.body, label })
    } catch {
      setAiError('AI request failed. Please try again.')
    } finally {
      setAiLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">

      {/* Name */}
      <div>
        <label className={labelClass}>Template name</label>
        <input
          {...register('name')}
          className={inputClass}
          placeholder="e.g. Polite 3-day reminder"
        />
        {errors.name && (
          <p className="mt-1 text-xs text-[#EF4444]">{errors.name.message}</p>
        )}
      </div>

      {/* Channel + Tone */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <div>
          <label className={labelClass}>Channel</label>
          <select {...register('channel')} className={inputClass}>
            <option value="EMAIL">Email</option>
            <option value="SMS">SMS</option>
            <option value="WHATSAPP">WhatsApp</option>
          </select>
        </div>
        <div>
          <label className={labelClass}>Tone</label>
          <select {...register('tone')} className={inputClass}>
            <option value="POLITE">Polite</option>
            <option value="NEUTRAL">Neutral</option>
            <option value="FIRM">Firm</option>
          </select>
        </div>
      </div>

      {/* Subject — email only */}
      {channel === 'EMAIL' && (
        <div>
          <label className={labelClass}>
            Subject line{' '}
            <span className="text-c-muted font-normal">(optional)</span>
          </label>
          <input
            {...subjectReg}
            ref={(el) => {
              subjectReg.ref(el)
              subjectRef.current = el
            }}
            className={inputClass}
            placeholder="e.g. Friendly reminder — Invoice {{invoiceNumber}}"
          />
          {errors.subject && (
            <p className="mt-1 text-xs text-[#EF4444]">{errors.subject.message}</p>
          )}
          <div className="mt-2 flex flex-wrap gap-1.5">
            {PLACEHOLDERS.map((ph) => (
              <button
                key={ph.value}
                type="button"
                onClick={() => insertIntoSubject(ph.value)}
                className={varBtnClass}
              >
                {ph.label}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Body */}
      <div>
        {/* Label + AI toolbar on same row */}
        <div className="flex items-center justify-between gap-2 mb-1.5 flex-wrap">
          <label className="text-xs font-medium text-[#0D1B2A] dark:text-white/80">
            Message body
          </label>
          <AiToolbar
            loading={aiLoading}
            currentTone={tone}
            onPolish={() => runAi('polish')}
            onGenerate={() => runAi('generate')}
            onToneShift={(t) => runAi('tone', t)}
          />
        </div>

        <div className="relative">
          <textarea
            {...bodyReg}
            ref={(el) => {
              bodyReg.ref(el)
              bodyRef.current = el
            }}
            rows={8}
            placeholder="Hi {{customerName}}, this is a friendly reminder…"
            className={[
              inputClass,
              'resize-none leading-relaxed',
              aiLoading ? 'opacity-50' : '',
            ].join(' ')}
          />
          {/* Loading overlay */}
          {aiLoading && (
            <div className="absolute inset-0 flex items-center justify-center rounded-lg bg-white/40 dark:bg-[#1B2838]/40 backdrop-blur-[1px]">
              <div className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-white dark:bg-[#243447] border border-c-border shadow-md">
                <Loader2 size={14} className="animate-spin text-violet-500" />
                <span className="text-xs font-semibold text-[#0D1B2A] dark:text-white">
                  AI thinking…
                </span>
              </div>
            </div>
          )}
        </div>

        {/* Counter + validation */}
        <div className="mt-1 flex items-start justify-between gap-2">
          <div>
            {errors.body && (
              <p className="text-xs text-[#EF4444]">{errors.body.message}</p>
            )}
            {aiError && (
              <p className="text-xs text-red-500">{aiError}</p>
            )}
          </div>
          <span className={`text-xs tabular-nums shrink-0 ${bodyOver ? 'text-[#EF4444] font-semibold' : 'text-c-muted'}`}>
            {body.length} / {bodyLimit}
          </span>
        </div>

        {/* AI preview */}
        {aiPreview && (
          <div className="mt-3">
            <PreviewPane
              preview={aiPreview}
              channel={channel}
              onAccept={acceptPreview}
              onDiscard={() => setAiPreview(null)}
            />
          </div>
        )}

        {/* Variable chips */}
        <div className="mt-3">
          <p className="text-[10px] font-medium text-c-muted uppercase tracking-wide mb-1.5">
            Insert variable
          </p>
          <div className="flex flex-wrap gap-1.5">
            {PLACEHOLDERS.map((ph) => (
              <button
                key={ph.value}
                type="button"
                onClick={() => insertIntoBody(ph.value)}
                className={varBtnClass}
              >
                {ph.label}
              </button>
            ))}
          </div>
        </div>

        {/* Payment info hint */}
        <div className="mt-3 flex items-start gap-2 px-3 py-2 rounded-lg bg-[#2E7A8E]/8 dark:bg-[#4FC3F7]/8 border border-[#2E7A8E]/20 dark:border-[#4FC3F7]/20">
          <span className="text-[#2E7A8E] dark:text-[#4FC3F7] text-xs mt-px shrink-0">ℹ</span>
          <p className="text-xs text-[#2E7A8E] dark:text-[#4FC3F7]">
            You can include your bank details, UPI ID, or any payment instructions directly in the message body.
          </p>
        </div>
      </div>

      {/* Submit */}
      <div className="flex justify-end pt-1">
        <button
          type="submit"
          disabled={isPending || bodyOver}
          className="px-4 py-2 text-sm font-semibold text-white rounded-lg hover:opacity-90 transition-opacity disabled:opacity-50"
          style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
        >
          {isPending ? 'Saving…' : submitLabel}
        </button>
      </div>
    </form>
  )
}
