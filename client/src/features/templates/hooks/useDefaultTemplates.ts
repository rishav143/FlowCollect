import { useEffect, useRef } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { listTemplates, createTemplate } from '@/api/template.api'
import type { CreateTemplateRequest, TemplateChannel } from '@/types/template.types'

// ---------------------------------------------------------------------------
// Default template definitions — one per channel.
// Customers can edit or delete these at any time.
// ---------------------------------------------------------------------------

const DEFAULT_TEMPLATES: CreateTemplateRequest[] = [
  {
    name: 'Default Email Reminder',
    channel: 'EMAIL',
    tone: 'POLITE',
    subject: 'Payment Reminder: Invoice {{invoiceNumber}}',
    body: `Hi {{customerName}},

This is a friendly reminder that invoice {{invoiceNumber}} for {{totalAmount}} is due on {{dueDate}}.

Outstanding balance: {{remainingAmount}}

Please make your payment at the earliest convenience. If you have any questions, feel free to reach out.

Best regards,
{{organizationName}}`,
  },
  {
    name: 'Default SMS Reminder',
    channel: 'SMS',
    tone: 'POLITE',
    body: `Hi {{customerName}}, this is a reminder that invoice {{invoiceNumber}} for {{remainingAmount}} was due on {{dueDate}}. Please make your payment. - {{organizationName}}`,
  },
  {
    name: 'Default WhatsApp Reminder',
    channel: 'WHATSAPP',
    tone: 'POLITE',
    body: `Hi {{customerName}},

We'd like to remind you that invoice *{{invoiceNumber}}* for *{{remainingAmount}}* was due on {{dueDate}}.

Please make your payment at the earliest convenience.

Thank you,
{{organizationName}}`,
  },
]

// ---------------------------------------------------------------------------
// Hook — seeds default templates once if the org has none for a channel.
// Runs only after the org is known; uses a ref so it fires at most once
// per mount even under StrictMode double-invocation.
// ---------------------------------------------------------------------------

export function useDefaultTemplates() {
  const orgId       = useAuthStore((s) => s.org?.id ?? '')
  const queryClient = useQueryClient()
  const seeded      = useRef(false)

  useEffect(() => {
    if (!orgId || seeded.current) return

    async function seed() {
      try {
        const page = await listTemplates(orgId, { size: 200 })
        const existingChannels = new Set<TemplateChannel>(
          page.content.map((t) => t.channel),
        )

        const toCreate = DEFAULT_TEMPLATES.filter(
          (t) => !existingChannels.has(t.channel),
        )

        if (toCreate.length === 0) return

        await Promise.all(toCreate.map((t) => createTemplate(orgId, t)))
        queryClient.invalidateQueries({ queryKey: ['templates', orgId] })
      } catch {
        // Silently swallow — seeding is best-effort and non-blocking
      }
    }

    seeded.current = true
    seed()
  }, [orgId, queryClient])
}
