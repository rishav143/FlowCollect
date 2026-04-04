import { useRef } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { templateSchema, type TemplateFormValues } from '@/features/templates/schemas/template.schema'
import { useAuthStore } from '@/store/auth.store'

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

// ─── Cursor-aware text insertion for textarea / input ─────────────────────────

function insertAtCursor(
  el: HTMLTextAreaElement | HTMLInputElement,
  text: string,
): string {
  const start = el.selectionStart ?? el.value.length
  const end   = el.selectionEnd   ?? el.value.length
  const newValue = el.value.slice(0, start) + text + el.value.slice(end)
  // Restore focus + move caret after the inserted text once React has updated
  const nextPos = start + text.length
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

  // Refs are needed so we can read selectionStart/End before React state updates
  const bodyRef    = useRef<HTMLTextAreaElement | null>(null)
  const subjectRef = useRef<HTMLInputElement    | null>(null)

  const channel   = watch('channel')
  const body      = watch('body') ?? ''
  const bodyLimit = BODY_LIMIT[channel] ?? 2000
  const bodyOver  = body.length > bodyLimit

  // Separate the RHF ref callback so we can attach our own ref alongside it
  const bodyReg    = register('body')
  const subjectReg = register('subject')

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
        <label className={labelClass}>Message body</label>

        <textarea
          {...bodyReg}
          ref={(el) => {
            bodyReg.ref(el)
            bodyRef.current = el
          }}
          rows={8}
          placeholder="Hi {{customerName}}, this is a friendly reminder…"
          className={[inputClass, 'resize-none leading-relaxed'].join(' ')}
        />

        {/* Counter + validation */}
        <div className="mt-1 flex items-start justify-between gap-2">
          <div>
            {errors.body && (
              <p className="text-xs text-[#EF4444]">{errors.body.message}</p>
            )}
          </div>
          <span
            className={`text-xs tabular-nums shrink-0 ${
              bodyOver ? 'text-[#EF4444] font-semibold' : 'text-c-muted'
            }`}
          >
            {body.length} / {bodyLimit}
          </span>
        </div>

        {/* Variable chip buttons */}
        <div className="mt-2">
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
