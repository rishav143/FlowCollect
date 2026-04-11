import { useEffect, useRef } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/auth.store'
import { listTemplates, createTemplate, updateTemplate } from '@/api/template.api'
import type { CreateTemplateRequest } from '@/types/template.types'

// Bump this number whenever the default template content changes.
// The hook will update any existing default templates (matched by name) for orgs
// that were seeded on an older version.
const SEED_VERSION = 4

// ---------------------------------------------------------------------------
// Default template definitions — mode-aware, one per channel.
// Customers can edit or delete these at any time.
// ---------------------------------------------------------------------------

type PaymentMode = 'PAYMENT_LINK' | 'CONFIRMATION_FLOW' | null | undefined

function buildDefaultTemplates(paymentMode: PaymentMode): CreateTemplateRequest[] {
  const isPaymentLink      = paymentMode === 'PAYMENT_LINK'
  const isConfirmationFlow = paymentMode === 'CONFIRMATION_FLOW'

  // ── EMAIL ─────────────────────────────────────────────────────────────────
  let emailBody: string
  let emailSubject: string

  if (isPaymentLink) {
    emailSubject = 'Payment due: Invoice {{invoiceNumber}}'
    emailBody = `Hi {{customerName}},

This is a reminder that invoice {{invoiceNumber}} for {{remainingAmount}} was due on {{dueDate}}.

You can complete your payment using the link below:
{{paymentLink}}

If you have already paid, please disregard this message. Feel free to reach out if you have any questions.

Regards,
{{organizationName}}`
  } else if (isConfirmationFlow) {
    emailSubject = 'Invoice {{invoiceNumber}} — outstanding balance'
    emailBody = `Hi {{customerName}},

We wanted to follow up on invoice {{invoiceNumber}} for {{remainingAmount}}, which was due on {{dueDate}}.

The link below includes payment details if you haven't yet settled this, and a way to confirm your payment once it's done — this helps us keep our records accurate and avoids any further reminders:
{{confirmationLink}}

If you have any questions, please don't hesitate to reach out.

Regards,
{{organizationName}}`
  } else {
    emailSubject = 'Payment reminder: Invoice {{invoiceNumber}}'
    emailBody = `Hi {{customerName}},

This is a reminder that invoice {{invoiceNumber}} for {{remainingAmount}} was due on {{dueDate}}.

Please arrange your payment at your earliest convenience. If you have already paid, kindly let us know so we can update our records.

Feel free to reach out if you have any questions.

Regards,
{{organizationName}}`
  }

  // ── SMS ───────────────────────────────────────────────────────────────────
  let smsBody: string

  if (isPaymentLink) {
    smsBody = `Hi {{customerName}}, this is a reminder that invoice {{invoiceNumber}} for {{remainingAmount}} was due on {{dueDate}}. Pay here: {{paymentLink}} - {{organizationName}}`
  } else if (isConfirmationFlow) {
    smsBody = `Hi {{customerName}}, invoice {{invoiceNumber}} for {{remainingAmount}} was due on {{dueDate}}. To pay or confirm a payment already made, visit: {{confirmationLink}} - {{organizationName}}`
  } else {
    smsBody = `Hi {{customerName}}, this is a reminder that invoice {{invoiceNumber}} for {{remainingAmount}} was due on {{dueDate}}. Please make your payment at your earliest convenience. - {{organizationName}}`
  }

  // ── WHATSAPP ──────────────────────────────────────────────────────────────
  let whatsappBody: string

  if (isPaymentLink) {
    whatsappBody = `Hi {{customerName}},

This is a reminder that invoice *{{invoiceNumber}}* for *{{remainingAmount}}* was due on {{dueDate}}.

Please complete your payment using the link below:
{{paymentLink}}

If you have already paid, please ignore this message. Reach out if you need any help.

Regards,
{{organizationName}}`
  } else if (isConfirmationFlow) {
    whatsappBody = `Hi {{customerName}},

A quick note regarding invoice *{{invoiceNumber}}* for *{{remainingAmount}}*, which was due on {{dueDate}}.

The link below includes payment details if you have not yet settled this, and a way to confirm once your payment is done — so we can update our records promptly:
{{confirmationLink}}

For any questions, please reach out to us.

Regards,
{{organizationName}}`
  } else {
    whatsappBody = `Hi {{customerName}},

This is a reminder that invoice *{{invoiceNumber}}* for *{{remainingAmount}}* was due on {{dueDate}}.

Please make your payment at your earliest convenience. If you have already paid, do let us know and we will update our records.

Regards,
{{organizationName}}`
  }

  return [
    {
      name:    'Default Email Reminder',
      channel: 'EMAIL',
      tone:    'POLITE',
      subject: emailSubject,
      body:    emailBody,
    },
    {
      name:    'Default SMS Reminder',
      channel: 'SMS',
      tone:    'POLITE',
      body:    smsBody,
    },
    {
      name:    'Default WhatsApp Reminder',
      channel: 'WHATSAPP',
      tone:    'POLITE',
      body:    whatsappBody,
    },
  ]
}

// Default template names — used to match and update existing defaults.
const DEFAULT_NAMES = new Set([
  'Default Email Reminder',
  'Default SMS Reminder',
  'Default WhatsApp Reminder',
])

function seedVersionKey(orgId: string) {
  return `pp_tpl_seed_v_${orgId}`
}

// ---------------------------------------------------------------------------
// Hook — seeds default templates on first run and re-applies updated content
// whenever SEED_VERSION is bumped, matching by the well-known default names.
// Runs only after the org is known; uses a ref so it fires at most once
// per mount even under StrictMode double-invocation.
// ---------------------------------------------------------------------------

export function useDefaultTemplates() {
  const orgId       = useAuthStore((s) => s.org?.id ?? '')
  const paymentMode = useAuthStore((s) => s.org?.paymentCollectionMode as PaymentMode)
  const queryClient = useQueryClient()
  const seeded      = useRef(false)

  useEffect(() => {
    if (!orgId || seeded.current) return

    async function seed() {
      try {
        const storedVersion = parseInt(localStorage.getItem(seedVersionKey(orgId)) ?? '0', 10)
        const needsUpdate   = storedVersion < SEED_VERSION

        const page     = await listTemplates(orgId, { size: 100 })
        const defaults = buildDefaultTemplates(paymentMode)

        // Map channel → existing template for quick lookup — exclude system templates
        // so system-defined AUTO templates (e.g. Day 3 Recovery Nudge) don't block
        // creation of org-owned default templates on the same channel.
        const existingByChannel = new Map(
          page.content.filter((t) => !t.systemDefined).map((t) => [t.channel, t]),
        )

        const toCreate: CreateTemplateRequest[] = []
        const updatePromises: Promise<unknown>[] = []

        // If the org has been seeded before (storedVersion > 0), never re-create
        // deleted defaults — the user intentionally removed them. Only create on
        // the very first seed run (storedVersion === 0).
        const firstRun = storedVersion === 0

        for (const def of defaults) {
          const existing = existingByChannel.get(def.channel)
          if (!existing) {
            if (firstRun) toCreate.push(def)
          } else if (needsUpdate && DEFAULT_NAMES.has(existing.name)) {
            // Only overwrite templates that still carry a default name —
            // if the user renamed it, treat it as customised and leave it alone.
            updatePromises.push(
              updateTemplate(orgId, existing.id, {
                subject: def.subject,
                body:    def.body,
              }),
            )
          }
        }

        const created = await Promise.all(toCreate.map((t) => createTemplate(orgId, t)))
        await Promise.all(updatePromises)

        if (toCreate.length === 0 && updatePromises.length === 0) {
          localStorage.setItem(seedVersionKey(orgId), String(SEED_VERSION))
          return
        }

        // Merge updates back into the page content for the cache
        const updatedIds = new Set(
          defaults
            .map((d) => existingByChannel.get(d.channel))
            .filter((t) => t && DEFAULT_NAMES.has(t.name))
            .map((t) => t!.id),
        )
        const mergedDefaults = new Map(defaults.map((d) => [d.channel, d]))
        const updatedExisting = page.content.map((t) =>
          updatedIds.has(t.id)
            ? { ...t, ...mergedDefaults.get(t.channel) }
            : t,
        )
        const allTemplates = [...updatedExisting, ...created]

        queryClient.setQueryData(
          ['templates', orgId, { size: 100 }],
          {
            content:       allTemplates,
            totalElements: allTemplates.length,
            totalPages:    1,
            number:        0,
            size:          200,
          },
        )
        queryClient.invalidateQueries({ queryKey: ['templates', orgId] })
        localStorage.setItem(seedVersionKey(orgId), String(SEED_VERSION))
      } catch {
        // Silently swallow — seeding is best-effort and non-blocking
      }
    }

    seeded.current = true
    seed()
  }, [orgId, paymentMode, queryClient])
}
