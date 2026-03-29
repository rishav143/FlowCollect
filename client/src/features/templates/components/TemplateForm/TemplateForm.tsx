import { useRef, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { templateSchema, type TemplateFormValues } from '@/features/templates/schemas/template.schema'
import { useAuthStore } from '@/store/auth.store'

const BASE_PLACEHOLDERS = [
  { label: 'Customer Name',     value: '{{customerName}}'    },
  { label: 'Organization Name', value: '{{organizationName}}'},
  { label: 'Invoice #',         value: '{{invoiceNumber}}'   },
  { label: 'Amount Due', value: '{{remainingAmount}}' },
  { label: 'Due Date',          value: '{{dueDate}}'         },
]

const BODY_LIMIT: Record<string, number> = {
  SMS:      320,
  WHATSAPP: 1000,
  EMAIL:    2000,
}

const isVarToken = (s: string) => /^\{\{[^}]+\}\}$/.test(s)

// ─── DOM helpers ─────────────────────────────────────────────────────────────

function makeVarSpan(value: string): HTMLSpanElement {
  const span = document.createElement('span')
  span.className = 'body-var-chip'
  span.dataset.var = value
  span.setAttribute('contenteditable', 'false')
  span.textContent = value
  return span
}

function buildEditorDOM(el: HTMLDivElement | HTMLSpanElement, text: string, multiline = true) {
  el.innerHTML = ''
  if (!text) return // leave empty so CSS :empty placeholder shows

  const parts = text.split(/({{[^}]+}})/g)
  for (const part of parts) {
    if (!part) continue
    if (isVarToken(part)) {
      el.appendChild(makeVarSpan(part))
    } else if (multiline) {
      const lines = part.split('\n')
      lines.forEach((line, i) => {
        if (line) el.appendChild(document.createTextNode(line))
        if (i < lines.length - 1) el.appendChild(document.createElement('br'))
      })
    } else {
      // Single-line: strip any newlines
      el.appendChild(document.createTextNode(part.replace(/\n/g, ' ')))
    }
  }

  if (multiline) {
    // Sentinel <br>: browsers won't render a trailing new line in a
    // contenteditable unless something follows the last <br>. Keeping one
    // invisible sentinel node here fixes that without affecting the value.
    const sentinel = document.createElement('br')
    sentinel.dataset.sentinel = 'true'
    el.appendChild(sentinel)
  }
}

function readEditorValue(el: HTMLDivElement | HTMLSpanElement): string {
  const nodes  = Array.from(el.childNodes)
  const lastEl = nodes[nodes.length - 1]
  // Skip the trailing sentinel <br>
  const limit  = (lastEl instanceof HTMLElement && lastEl.dataset.sentinel === 'true')
    ? nodes.length - 1
    : nodes.length

  let result = ''
  for (let i = 0; i < limit; i++) {
    const node = nodes[i]
    if (node.nodeType === Node.TEXT_NODE) {
      result += node.textContent ?? ''
    } else {
      const child = node as HTMLElement
      if (child.dataset.var) {
        result += child.dataset.var
      } else if (child.tagName === 'BR') {
        result += '\n'
      } else {
        result += child.textContent ?? ''
      }
    }
  }
  return result
}

/** Keep a sentinel <br data-sentinel> as the last child so trailing newlines render. */
function ensureSentinel(el: HTMLDivElement) {
  if (!el.childNodes.length) return
  const last = el.lastChild as HTMLElement | null
  if (!last || last.dataset?.sentinel !== 'true') {
    const sentinel = document.createElement('br')
    sentinel.dataset.sentinel = 'true'
    el.appendChild(sentinel)
  }
}

/** Handle Backspace/Delete for whole-variable deletion in any contenteditable el. */
function handleVarKeyDown(
  e: React.KeyboardEvent<HTMLDivElement | HTMLSpanElement>,
  el: HTMLDivElement | HTMLSpanElement,
  onCommit: () => void,
) {
  if (e.key === 'Backspace') {
    const sel = window.getSelection()
    if (!sel?.rangeCount) return
    const range = sel.getRangeAt(0)
    if (!range.collapsed) return

    const { startContainer, startOffset } = range

    if (startContainer.nodeType === Node.TEXT_NODE && startOffset === 0) {
      const prev = startContainer.previousSibling as HTMLElement | null
      if (prev?.dataset?.var) {
        e.preventDefault()
        prev.remove()
        onCommit()
        return
      }
    }

    if (startContainer === el && startOffset > 0) {
      const prevNode = el.childNodes[startOffset - 1] as HTMLElement
      if (prevNode?.dataset?.var) {
        e.preventDefault()
        prevNode.remove()
        const newRange = document.createRange()
        const newSel   = window.getSelection()
        const refNode  = el.childNodes[startOffset - 2]
        if (refNode) newRange.setStartAfter(refNode)
        else         newRange.setStart(el, 0)
        newRange.collapse(true)
        newSel?.removeAllRanges()
        newSel?.addRange(newRange)
        onCommit()
        return
      }
    }
  }

  if (e.key === 'Delete') {
    const sel = window.getSelection()
    if (!sel?.rangeCount) return
    const range = sel.getRangeAt(0)
    if (!range.collapsed) return

    const { startContainer, startOffset } = range

    if (startContainer.nodeType === Node.TEXT_NODE) {
      const textLen = (startContainer.textContent ?? '').length
      if (startOffset === textLen) {
        const next = startContainer.nextSibling as HTMLElement | null
        if (next?.dataset?.var) {
          e.preventDefault()
          next.remove()
          onCommit()
          return
        }
      }
    }

    if (startContainer === el && startOffset < el.childNodes.length) {
      const nextNode = el.childNodes[startOffset] as HTMLElement
      if (nextNode?.dataset?.var) {
        e.preventDefault()
        nextNode.remove()
        onCommit()
        return
      }
    }
  }
}

// ─── Shared Tailwind classes ──────────────────────────────────────────────────

const inputClass =
  'w-full px-3 py-2 text-sm rounded-lg border border-transparent ' +
  'bg-[#F4F7F9] dark:bg-[#243447] ' +
  'text-[#0D1B2A] dark:text-white ' +
  'placeholder:text-c-muted ' +
  'focus:border-[#8A9BAE]/40 focus:outline-none transition-colors'

const labelClass = 'block text-xs font-medium text-[#0D1B2A] dark:text-white/80 mb-1'

// ─── Component ────────────────────────────────────────────────────────────────

interface Props {
  defaultValues?: Partial<TemplateFormValues>
  onSubmit:       (values: TemplateFormValues) => void
  isPending:      boolean
  submitLabel?:   string
}

export default function TemplateForm({ defaultValues, onSubmit, isPending, submitLabel = 'Save' }: Props) {
  const paymentMode = useAuthStore((s) => s.org?.paymentCollectionMode)

  const PLACEHOLDERS = [
    ...BASE_PLACEHOLDERS,
    ...(paymentMode === 'PAYMENT_LINK'      ? [{ label: 'Payment Link',      value: '{{paymentLink}}'      }] : []),
    ...(paymentMode === 'CONFIRMATION_FLOW' ? [{ label: 'Confirmation Link', value: '{{confirmationLink}}' }] : []),
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

  // ── Body editor ─────────────────────────────────────────────────────────────
  const bodyRef      = useRef<HTMLDivElement | null>(null)
  const bodySkipSync = useRef(false)

  const channel   = watch('channel')
  const body      = watch('body') ?? ''
  const bodyLimit = BODY_LIMIT[channel] ?? 2000
  const bodyOver  = body.length > bodyLimit

  useEffect(() => {
    if (bodySkipSync.current) { bodySkipSync.current = false; return }
    if (bodyRef.current) buildEditorDOM(bodyRef.current, body, true)
  }, [body]) // eslint-disable-line react-hooks/exhaustive-deps

  function commitBodyValue() {
    const el = bodyRef.current
    if (!el) return
    bodySkipSync.current = true
    const text = readEditorValue(el)
    setValue('body', text, { shouldValidate: true, shouldDirty: true })
    // If editor is now empty, clear all nodes so :empty placeholder shows
    if (!text) { el.innerHTML = ''; return }
    ensureSentinel(el)
  }

  function handleBodyKeyDown(e: React.KeyboardEvent<HTMLDivElement>) {
    const el = bodyRef.current
    if (!el) return

    // ── Enter → clean <br> insertion ─────────────────────────────────────────
    if (e.key === 'Enter') {
      e.preventDefault()
      const sel = window.getSelection()
      if (!sel?.rangeCount) return
      const range = sel.getRangeAt(0)
      range.deleteContents()

      const br   = document.createElement('br')
      range.insertNode(br)

      // If the next sibling is a plain text node put cursor at its start;
      // otherwise insert an empty text node. The sentinel br at the end then
      // forces the new line to visually render immediately.
      const next = br.nextSibling
      let cursorNode: Node
      if (next?.nodeType === Node.TEXT_NODE) {
        cursorNode = next
      } else {
        cursorNode = document.createTextNode('')
        br.parentNode!.insertBefore(cursorNode, next ?? null)
      }
      range.setStart(cursorNode, 0)
      range.collapse(true)
      sel.removeAllRanges()
      sel.addRange(range)
      commitBodyValue()
      return
    }

    handleVarKeyDown(e as React.KeyboardEvent<HTMLDivElement>, el, commitBodyValue)
  }

  function insertIntoBody(ph: string) {
    const el = bodyRef.current
    if (!el) return
    el.focus()
    insertVarAtCursor(el, ph)
    bodySkipSync.current = true
    setValue('body', readEditorValue(el), { shouldValidate: true, shouldDirty: true })
    ensureSentinel(el)
  }

  // ── Subject editor (email only) ──────────────────────────────────────────────
  const subjectRef      = useRef<HTMLDivElement | null>(null)
  const subjectSkipSync = useRef(false)
  const subject         = watch('subject') ?? ''

  useEffect(() => {
    if (subjectSkipSync.current) { subjectSkipSync.current = false; return }
    if (subjectRef.current) buildEditorDOM(subjectRef.current, subject, false)
  }, [subject]) // eslint-disable-line react-hooks/exhaustive-deps

  function commitSubjectValue() {
    const el = subjectRef.current
    if (!el) return
    subjectSkipSync.current = true
    const text = readEditorValue(el)
    setValue('subject', text || undefined, { shouldValidate: true, shouldDirty: true })
    if (!text) el.innerHTML = ''
  }

  function handleSubjectKeyDown(e: React.KeyboardEvent<HTMLDivElement>) {
    const el = subjectRef.current
    if (!el) return
    // No Enter in subject (single-line)
    if (e.key === 'Enter') { e.preventDefault(); return }
    handleVarKeyDown(e as React.KeyboardEvent<HTMLDivElement>, el, commitSubjectValue)
  }

  function insertIntoSubject(ph: string) {
    const el = subjectRef.current
    if (!el) return
    el.focus()
    insertVarAtCursor(el, ph)
    subjectSkipSync.current = true
    setValue('subject', readEditorValue(el) || undefined, { shouldValidate: true, shouldDirty: true })
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
            Subject line <span className="text-c-muted font-normal">(optional)</span>
          </label>
          <div
            ref={subjectRef}
            contentEditable
            suppressContentEditableWarning
            onInput={commitSubjectValue}
            onKeyDown={handleSubjectKeyDown}
            data-placeholder="e.g. Friendly reminder — Invoice {{invoiceNumber}}"
            className={[
              'subject-editor',
              'w-full px-3 py-2 text-sm rounded-lg',
              'border border-transparent',
              'bg-[#F4F7F9] dark:bg-[#243447]',
              'text-[#0D1B2A] dark:text-white',
              'focus:border-[#8A9BAE]/40 focus:outline-none transition-colors',
              'whitespace-nowrap overflow-x-auto',
            ].join(' ')}
          />
          {errors.subject && <p className="mt-1 text-xs text-[#EF4444]">{errors.subject.message}</p>}

          {/* Subject variable chips */}
          <div className="mt-2 flex flex-wrap gap-1.5">
            {PLACEHOLDERS.map((ph) => (
              <button
                key={ph.value}
                type="button"
                onClick={() => insertIntoSubject(ph.value)}
                className="px-2 py-1 text-[11px] font-medium text-[#2E7A8E] dark:text-[#4FC3F7] bg-[#2E7A8E]/10 dark:bg-[#4FC3F7]/10 hover:bg-[#2E7A8E]/20 dark:hover:bg-[#4FC3F7]/20 rounded transition-colors"
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

        <div
          ref={bodyRef}
          contentEditable
          suppressContentEditableWarning
          onInput={commitBodyValue}
          onKeyDown={handleBodyKeyDown}
          data-placeholder="Hi {{customerName}}, this is a friendly reminder…"
          className={[
            'body-editor',
            'min-h-[10rem] w-full px-3 py-2 rounded-lg',
            'border border-transparent',
            'bg-[#F4F7F9] dark:bg-[#243447]',
            'text-[#0D1B2A] dark:text-white',
            'text-base leading-relaxed',
            'focus:border-[#8A9BAE]/40 focus:outline-none transition-colors',
            'whitespace-pre-wrap break-words',
          ].join(' ')}
        />

        {/* Counter + validation */}
        <div className="mt-1 flex items-start justify-between gap-2">
          <div>
            {errors.body && <p className="text-xs text-[#EF4444]">{errors.body.message}</p>}
          </div>
          <span className={`text-xs tabular-nums shrink-0 ${bodyOver ? 'text-[#EF4444] font-semibold' : 'text-c-muted'}`}>
            {body.length} / {bodyLimit}
          </span>
        </div>

        {/* Body variable chips */}
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
                className="px-2 py-1 text-[11px] font-medium text-[#2E7A8E] dark:text-[#4FC3F7] bg-[#2E7A8E]/10 dark:bg-[#4FC3F7]/10 hover:bg-[#2E7A8E]/20 dark:hover:bg-[#4FC3F7]/20 rounded transition-colors"
              >
                {ph.label}
              </button>
            ))}
          </div>
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

// ─── Shared cursor insertion utility ─────────────────────────────────────────

function insertVarAtCursor(el: HTMLElement, ph: string) {
  const sel  = window.getSelection()
  const span = (() => {
    const s = document.createElement('span')
    s.className = 'body-var-chip'
    s.dataset.var = ph
    s.setAttribute('contenteditable', 'false')
    s.textContent = ph
    return s
  })()

  if (sel?.rangeCount) {
    const range = sel.getRangeAt(0)
    range.deleteContents()
    range.insertNode(span)
    let after = span.nextSibling
    if (!after || after.nodeType !== Node.TEXT_NODE) {
      after = document.createTextNode('')
      span.parentNode!.insertBefore(after, span.nextSibling)
    }
    range.setStart(after, 0)
    range.collapse(true)
    sel.removeAllRanges()
    sel.addRange(range)
  } else {
    el.appendChild(span)
    const after = document.createTextNode('')
    el.appendChild(after)
    const range = document.createRange()
    range.setStart(after, 0)
    range.collapse(true)
    window.getSelection()?.removeAllRanges()
    window.getSelection()?.addRange(range)
  }
}
