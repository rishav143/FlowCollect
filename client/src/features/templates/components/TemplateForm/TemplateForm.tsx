import { useRef } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { templateSchema, type TemplateFormValues } from '@/features/templates/schemas/template.schema'

const PLACEHOLDERS = [
  '{{customerName}}',
  '{{invoiceNumber}}',
  '{{amount}}',
  '{{dueDate}}',
  '{{paymentLink}}',
  '{{confirmationLink}}',
]

// Shared class strings — keep as literals so Tailwind scanner sees them
const inputClass =
  'w-full px-3 py-2 text-sm rounded-lg border border-transparent ' +
  'bg-[#F4F7F9] dark:bg-[#243447] ' +
  'text-[#0D1B2A] dark:text-white ' +
  'placeholder:text-c-muted ' +
  'focus:border-[#8A9BAE]/40 focus:outline-none transition-colors'

const labelClass = 'block text-xs font-medium text-[#0D1B2A] dark:text-white/80 mb-1'

interface Props {
  defaultValues?: Partial<TemplateFormValues>
  onSubmit:       (values: TemplateFormValues) => void
  isPending:      boolean
  submitLabel?:   string
}

export default function TemplateForm({ defaultValues, onSubmit, isPending, submitLabel = 'Save' }: Props) {
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

  const textareaRef = useRef<HTMLTextAreaElement | null>(null)
  const channel     = watch('channel')
  const body        = watch('body') ?? ''

  function insertPlaceholder(ph: string) {
    const el    = textareaRef.current
    const start = el?.selectionStart ?? body.length
    const end   = el?.selectionEnd   ?? body.length
    const next  = body.slice(0, start) + ph + body.slice(end)
    setValue('body', next, { shouldValidate: true })
    // Restore cursor position after the inserted variable
    requestAnimationFrame(() => {
      el?.focus()
      el?.setSelectionRange(start + ph.length, start + ph.length)
    })
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
        {errors.name && <p className="mt-1 text-xs text-[#EF4444]">{errors.name.message}</p>}
      </div>

      {/* Channel + Tone */}
      <div className="grid grid-cols-2 gap-3">
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
            Subject line <span className="text-c-muted font-normal">(optional)</span>
          </label>
          <input
            {...register('subject')}
            className={inputClass}
            placeholder="e.g. Friendly reminder — Invoice {{invoiceNumber}}"
          />
          {errors.subject && <p className="mt-1 text-xs text-[#EF4444]">{errors.subject.message}</p>}
        </div>
      )}

      {/* Body */}
      <div>
        <label className={labelClass}>Message body</label>
        <textarea
          {...register('body')}
          ref={(el) => {
            register('body').ref(el)
            textareaRef.current = el
          }}
          rows={5}
          className={`${inputClass} resize-y`}
          placeholder="Hi {{customerName}}, this is a friendly reminder…"
        />
        {errors.body && <p className="mt-1 text-xs text-[#EF4444]">{errors.body.message}</p>}

        {/* Placeholder chips */}
        <div className="mt-2">
          <p className="text-[10px] font-medium text-c-muted uppercase tracking-wide mb-1.5">
            Insert variable
          </p>
          <div className="flex flex-wrap gap-1.5">
            {PLACEHOLDERS.map((ph) => (
              <button
                key={ph}
                type="button"
                onClick={() => insertPlaceholder(ph)}
                className="px-2 py-1 text-[11px] font-mono text-[#2E7A8E] dark:text-[#4FC3F7] bg-[#2E7A8E]/10 dark:bg-[#4FC3F7]/10 hover:bg-[#2E7A8E]/20 dark:hover:bg-[#4FC3F7]/20 rounded transition-colors"
              >
                {ph}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Submit */}
      <div className="flex justify-end pt-1">
        <button
          type="submit"
          disabled={isPending}
          className="px-4 py-2 text-sm font-semibold text-white rounded-lg hover:opacity-90 transition-opacity disabled:opacity-50"
          style={{ background: 'linear-gradient(90deg, #29B6F6 0%, #4FC3F7 100%)' }}
        >
          {isPending ? 'Saving…' : submitLabel}
        </button>
      </div>
    </form>
  )
}
