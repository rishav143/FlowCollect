/**
 * devMock.ts
 *
 * Registers an axios request interceptor that completely replaces matched
 * requests with in-memory mock data — no network call is made.
 * Only active in DEV mode when the token is 'dev-token'.
 *
 * Covers: customers, invoices, payments, follow-ups, templates, confirmations.
 * Mutations (POST/PATCH/DELETE) echo back a plausible response so the UI
 * can exercise modals and state transitions without a running server.
 */

import type { AxiosInstance, InternalAxiosRequestConfig } from 'axios'

// ---------------------------------------------------------------------------
// Seed data
// ---------------------------------------------------------------------------

const NOW   = new Date().toISOString()
const TODAY = new Date().toISOString().slice(0, 10)
const PAST7 = new Date(Date.now() - 7  * 86400_000).toISOString().slice(0, 10)
const PAST3 = new Date(Date.now() - 3  * 86400_000).toISOString().slice(0, 10)
const FUT14 = new Date(Date.now() + 14 * 86400_000).toISOString().slice(0, 10)

let ORG_PROFILE: Record<string, unknown> = {
  id: 'o1', name: "Rishav's Agency", email: 'rishav@example.com',
  currency: 'INR', timezone: 'Asia/Kolkata',
  paymentCollectionMode: 'CONFIRMATION_FLOW',
}

const MEMBERS: Record<string, unknown>[] = [
  { id: 'u1', name: 'Rishav Choudhary', email: 'rishav@example.com',  role: 'ADMIN', joinedAt: NOW },
  { id: 'u2', name: 'Priya Sharma',     email: 'priya@example.com',   role: 'STAFF', joinedAt: NOW },
]

let BILLING: Record<string, unknown> = { plan: 'STARTER', smsCredits: 8, waCredits: 6 }

const CUSTOMERS: Record<string, unknown>[] = [
  { id: 'c1', organizationId: 'o1', name: 'Arjun Mehta',  companyName: 'Mehta Designs', email: 'arjun@mehtadesigns.com', phone: '9876543210', address: 'Mumbai, Maharashtra', active: true, automationEnabled: true,  createdAt: NOW, updatedAt: NOW },
  { id: 'c2', organizationId: 'o1', name: 'Priya Sharma', companyName: 'SharmaIT',      email: 'priya@sharmait.in',     phone: '9823456780', address: 'Bengaluru, Karnataka', active: true, automationEnabled: true,  createdAt: NOW, updatedAt: NOW },
  { id: 'c3', organizationId: 'o1', name: 'Rohan Verma',  companyName: null,             email: 'rohan.verma@gmail.com', phone: '9911223344', address: null,                   active: true, automationEnabled: false, createdAt: NOW, updatedAt: NOW },
]

const INVOICES: Record<string, unknown>[] = [
  {
    id: 'inv1', organizationId: 'o1', customerId: 'c1',
    invoiceNumber: 'INV-2025-001',
    lifeCycleStatus: 'ISSUED', timeStatus: 'OVERDUE',
    issueDate: PAST7, dueDate: PAST3,
    subtotal: 45000, taxPercentage: 18, totalAmount: 53100,
    totalPaid: 0, remainingAmount: 53100,
    items: [
      { id: 'li1', description: 'Brand Identity Design', quantity: 1, unitPrice: 30000, amount: 30000 },
      { id: 'li2', description: 'Social Media Kit',      quantity: 3, unitPrice: 5000,  amount: 15000 },
    ],
    createdAt: NOW, updatedAt: NOW,
  },
  {
    id: 'inv2', organizationId: 'o1', customerId: 'c2',
    invoiceNumber: 'INV-2025-002',
    lifeCycleStatus: 'PARTIALLY_PAID', timeStatus: 'DUE_TODAY',
    issueDate: PAST7, dueDate: TODAY,
    subtotal: 80000, taxPercentage: 18, totalAmount: 94400,
    totalPaid: 40000, remainingAmount: 54400,
    items: [
      { id: 'li3', description: 'Web App Development — Phase 1', quantity: 1, unitPrice: 80000, amount: 80000 },
    ],
    createdAt: NOW, updatedAt: NOW,
  },
  {
    id: 'inv3', organizationId: 'o1', customerId: 'c3',
    invoiceNumber: 'INV-2025-003',
    lifeCycleStatus: 'DRAFT', timeStatus: 'NOT_DUE',
    issueDate: null, dueDate: FUT14,
    subtotal: 12000, taxPercentage: 0, totalAmount: 12000,
    totalPaid: 0, remainingAmount: 12000,
    items: [
      { id: 'li4', description: 'Logo Design',       quantity: 1, unitPrice: 8000, amount: 8000 },
      { id: 'li5', description: 'Business Card',     quantity: 1, unitPrice: 2000, amount: 2000 },
      { id: 'li6', description: 'Letterhead Design', quantity: 1, unitPrice: 2000, amount: 2000 },
    ],
    createdAt: NOW, updatedAt: NOW,
  },
]

const PAYMENTS: Record<string, Record<string, unknown>[]> = {
  inv2: [
    { id: 'pay1', invoiceId: 'inv2', amount: 40000, mode: 'UPI', referenceId: 'UPI20250320', notes: 'First instalment', paidAt: PAST3, createdAt: NOW },
  ],
}

const FOLLOWUPS: Record<string, Record<string, unknown>[]> = {
  inv1: [
    { id: 'fu1', invoiceId: 'inv1', channel: 'EMAIL',    triggerType: 'MANUAL',    status: 'SENT',   templateId: 't1', reminderRuleId: null, scheduledForDate: null, attachPdf: true,  sentAt: PAST7, createdAt: NOW, updatedAt: NOW, paymentLinkId: null, paymentLinkUrl: null, paymentLinkGateway: null, paymentLinkStatus: null },
    { id: 'fu2', invoiceId: 'inv1', channel: 'WHATSAPP', triggerType: 'MANUAL',    status: 'SENT',   templateId: null, reminderRuleId: null, scheduledForDate: null, attachPdf: false, sentAt: PAST3, createdAt: NOW, updatedAt: NOW, paymentLinkId: null, paymentLinkUrl: null, paymentLinkGateway: null, paymentLinkStatus: null },
    { id: 'fu3', invoiceId: 'inv1', channel: 'SMS',      triggerType: 'AUTOMATED', status: 'FAILED', templateId: 't2', reminderRuleId: 'r1', scheduledForDate: null, attachPdf: false, sentAt: null,  createdAt: NOW, updatedAt: NOW, paymentLinkId: null, paymentLinkUrl: null, paymentLinkGateway: null, paymentLinkStatus: null },
  ],
  inv2: [
    { id: 'fu4', invoiceId: 'inv2', channel: 'EMAIL', triggerType: 'AUTOMATED', status: 'SENT', templateId: 't1', reminderRuleId: 'r1', scheduledForDate: null, attachPdf: true, sentAt: PAST7, createdAt: NOW, updatedAt: NOW, paymentLinkId: 'pl1', paymentLinkUrl: 'https://pay.example.com/pl1', paymentLinkGateway: 'RAZORPAY', paymentLinkStatus: 'ACTIVE' },
  ],
}

const TEMPLATES: Record<string, unknown>[] = [
  { id: 't1', name: 'Friendly Reminder',    channel: 'EMAIL',    subject: 'Friendly reminder — Invoice {{invoiceNumber}} is due', body: 'Hi {{customerName}},\n\nJust a friendly reminder that Invoice {{invoiceNumber}} for ₹{{amount}} is due on {{dueDate}}.\n\nPlease let us know if you have any questions.\n\nThanks,\n{{orgName}}', tone: 'POLITE',  active: true,  createdAt: NOW, updatedAt: NOW },
  { id: 't2', name: 'Overdue Notice',       channel: 'EMAIL',    subject: 'Overdue — Invoice {{invoiceNumber}}',                  body: 'Dear {{customerName}},\n\nInvoice {{invoiceNumber}} for ₹{{amount}} was due on {{dueDate}} and remains unpaid.\n\nPlease arrange payment at the earliest.\n\nRegards,\n{{orgName}}',                 tone: 'FIRM',    active: true,  createdAt: NOW, updatedAt: NOW },
  { id: 't3', name: 'WhatsApp Quick Nudge', channel: 'WHATSAPP', subject: null,                                                   body: 'Hi {{customerName}}! Reminder — Invoice {{invoiceNumber}} (₹{{amount}}) is due {{dueDate}}. Pay here: {{paymentLink}}',                                                                                         tone: 'NEUTRAL', active: false, createdAt: NOW, updatedAt: NOW },
]

const REMINDER_RULES: Record<string, unknown>[] = [
  { id: 'rr1', name: 'Early nudge',    triggerOffset: -3,  channel: 'EMAIL',    templateId: 't1', templateName: 'Friendly Reminder', active: true,  createdAt: NOW, updatedAt: NOW },
  { id: 'rr2', name: null,             triggerOffset:  0,  channel: 'WHATSAPP', templateId: null, templateName: null,                active: true,  createdAt: NOW, updatedAt: NOW },
  { id: 'rr3', name: 'Final warning',  triggerOffset:  5,  channel: 'EMAIL',    templateId: 't2', templateName: 'Overdue Notice',    active: true,  createdAt: NOW, updatedAt: NOW },
  { id: 'rr4', name: 'SMS escalation', triggerOffset:  10, channel: 'SMS',      templateId: null, templateName: null,                active: false, createdAt: NOW, updatedAt: NOW },
]

const CONFIRMATIONS: Record<string, unknown>[] = [
  // Full-payment claim on INV-2025-002 (amountClaimed == invoiceRemainingAmount → Approve / Reject)
  {
    id: 'conf1', invoiceId: 'inv2', invoiceNumber: 'INV-2025-002', confirmationLinkId: 'cl1',
    amountClaimed: 54400, invoiceTotalAmount: 94400, invoiceRemainingAmount: 54400,
    customerNote: 'Paid via NEFT. Please find UTR: NEFT20250320-001',
    status: 'PENDING_APPROVAL', businessNote: null, reviewedAt: null, createdAt: NOW,
  },
  // Partial-payment claim on INV-2025-001 (amountClaimed < invoiceRemainingAmount → Approve Partial / Request Remaining / Reject)
  {
    id: 'conf2', invoiceId: 'inv1', invoiceNumber: 'INV-2025-001', confirmationLinkId: 'cl2',
    amountClaimed: 25000, invoiceTotalAmount: 53100, invoiceRemainingAmount: 53100,
    customerNote: 'Sending 25k now, will pay the rest by end of month.',
    status: 'PENDING_APPROVAL', businessNote: null, reviewedAt: null, createdAt: PAST3,
  },
  // Already approved (historical record)
  {
    id: 'conf3', invoiceId: 'inv2', invoiceNumber: 'INV-2025-002', confirmationLinkId: 'cl1',
    amountClaimed: 40000, invoiceTotalAmount: 94400, invoiceRemainingAmount: 94400,
    customerNote: 'First instalment via UPI',
    status: 'APPROVED', businessNote: 'Payment verified in account.', reviewedAt: PAST7, createdAt: PAST7,
  },
]

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function makeId() { return Math.random().toString(36).slice(2, 10) }

function paginate<T>(list: T[], qs: Record<string, string>) {
  const size = parseInt(qs.size ?? '20')
  const num  = parseInt(qs.page ?? '0')
  return { content: list, totalElements: list.length, totalPages: Math.max(1, Math.ceil(list.length / size)), number: num, size }
}

function path(url: string) {
  try { return new URL(url, 'http://x').pathname } catch { return url }
}

// ---------------------------------------------------------------------------
// Mock router
// ---------------------------------------------------------------------------

const ORG  = 'o1'
const BASE = `/api/v1/organizations/${ORG}`

type Handler = (pathname: string, qs: Record<string, string>, body: unknown) => unknown

function route(method: string, pattern: RegExp, handle: Handler) {
  return { method: method.toUpperCase(), pattern, handle }
}

const ROUTES = [
  // Org profile
  route('GET',   /^$/, () => ORG_PROFILE),
  route('PATCH', /^$/, (_, __, b) => { ORG_PROFILE = { ...ORG_PROFILE, ...(b as object) }; return ORG_PROFILE }),

  // Team members
  route('GET', /\/members$/, () => MEMBERS),
  route('POST', /\/members\/invite$/, (_, __, b) => {
    const m = { id: makeId(), joinedAt: NOW, ...(b as object) }
    MEMBERS.push(m)
    return m
  }),
  route('DELETE', /\/members\/([^/]+)$/, (p) => {
    const idx = MEMBERS.findIndex((m) => m.id === p.match(/\/members\/([^/]+)$/)?.[1])
    if (idx !== -1) MEMBERS.splice(idx, 1)
    return null
  }),

  // Billing
  route('GET',  /\/billing$/, () => BILLING),
  route('POST', /\/billing\/credits\/purchase$/, (_, __, b) => {
    const { channel, pack } = b as { channel: string; pack: number }
    if (channel === 'SMS')      BILLING = { ...BILLING, smsCredits: (BILLING.smsCredits as number) + pack }
    if (channel === 'WHATSAPP') BILLING = { ...BILLING, waCredits:  (BILLING.waCredits  as number) + pack }
    return BILLING
  }),

  // Customers
  route('GET', /\/customers$/, (_, qs) => {
    let list = [...CUSTOMERS]
    if (qs.name) list = list.filter((c) => (c.name as string).toLowerCase().includes(qs.name.toLowerCase()))
    if (qs.active !== undefined) list = list.filter((c) => String(c.active) === qs.active)
    return paginate(list, qs)
  }),
  route('GET',    /\/customers\/([^/]+)$/, (p) => CUSTOMERS.find((c) => c.id === p.match(/\/customers\/([^/]+)$/)?.[1]) ?? null),
  route('POST',   /\/customers$/, (_, __, b) => {
    const c = { id: makeId(), organizationId: ORG, active: true, automationEnabled: true, address: null, createdAt: NOW, updatedAt: NOW, ...(b as object) }
    CUSTOMERS.push(c)
    return c
  }),
  route('PATCH',  /\/customers\/([^/]+)$/, (p, __, b) => {
    const c = CUSTOMERS.find((x) => x.id === p.match(/\/customers\/([^/]+)$/)?.[1])
    if (c) Object.assign(c, b)
    return c
  }),
  route('DELETE', /\/customers\/([^/]+)$/, (p) => {
    const idx = CUSTOMERS.findIndex((c) => c.id === p.match(/\/customers\/([^/]+)$/)?.[1])
    if (idx !== -1) CUSTOMERS.splice(idx, 1)
    return null
  }),
  route('PUT', /\/customers\/([^/]+)\/activate$/,           (p) => { const idx = CUSTOMERS.findIndex((x) => x.id === p.match(/\/customers\/([^/]+)/)?.[1]); if (idx !== -1) CUSTOMERS[idx] = { ...CUSTOMERS[idx], active: true };             return CUSTOMERS[idx] ?? null }),
  route('PUT', /\/customers\/([^/]+)\/deactivate$/,         (p) => { const idx = CUSTOMERS.findIndex((x) => x.id === p.match(/\/customers\/([^/]+)/)?.[1]); if (idx !== -1) CUSTOMERS[idx] = { ...CUSTOMERS[idx], active: false };            return CUSTOMERS[idx] ?? null }),
  route('PUT', /\/customers\/([^/]+)\/automation\/enable$/, (p) => { const idx = CUSTOMERS.findIndex((x) => x.id === p.match(/\/customers\/([^/]+)/)?.[1]); if (idx !== -1) CUSTOMERS[idx] = { ...CUSTOMERS[idx], automationEnabled: true };  return CUSTOMERS[idx] ?? null }),
  route('PUT', /\/customers\/([^/]+)\/automation\/disable$/,(p) => { const idx = CUSTOMERS.findIndex((x) => x.id === p.match(/\/customers\/([^/]+)/)?.[1]); if (idx !== -1) CUSTOMERS[idx] = { ...CUSTOMERS[idx], automationEnabled: false }; return CUSTOMERS[idx] ?? null }),

  // Invoices list & detail
  route('GET', /\/invoices$/, (_, qs) => {
    let list = [...INVOICES]
    if (qs.lifeCycleStatus) list = list.filter((i) => i.lifeCycleStatus === qs.lifeCycleStatus)
    if (qs.timeStatus)      list = list.filter((i) => i.timeStatus      === qs.timeStatus)
    if (qs.invoiceNumber)   list = list.filter((i) => (i.invoiceNumber as string).includes(qs.invoiceNumber))
    return paginate(list, qs)
  }),
  route('GET',    /\/invoices\/([^/]+)$/,       (p) => INVOICES.find((i) => i.id === p.match(/\/invoices\/([^/]+)$/)?.[1]) ?? null),
  route('POST',   /\/invoices$/,                (_, __, b) => {
    const inv = { id: makeId(), organizationId: ORG, lifeCycleStatus: 'DRAFT', timeStatus: 'NOT_DUE', issueDate: null, subtotal: 0, taxPercentage: 0, totalAmount: 0, totalPaid: 0, remainingAmount: 0, createdAt: NOW, updatedAt: NOW, items: [], ...(b as object) }
    INVOICES.push(inv)
    return inv
  }),
  route('POST', /\/invoices\/([^/]+)\/issue$/, (p) => {
    const id  = p.match(/\/invoices\/([^/]+)/)?.[1]
    const inv = INVOICES.find((i) => i.id === id)
    if (inv) { inv.lifeCycleStatus = 'ISSUED'; inv.issueDate = TODAY }
    return inv
  }),
  route('DELETE', /\/invoices\/([^/]+)$/, (p) => {
    const id  = p.match(/\/invoices\/([^/]+)$/)?.[1]
    const idx = INVOICES.findIndex((i) => i.id === id)
    if (idx !== -1) INVOICES.splice(idx, 1)
    return null
  }),
  route('GET', /\/invoices\/([^/]+)\/pdf$/, () => new Blob(['%PDF-1.4 mock'], { type: 'application/pdf' })),

  // Payments
  route('GET',    /\/invoices\/([^/]+)\/payments$/, (p, qs) => {
    const id = p.match(/\/invoices\/([^/]+)/)?.[1]!
    return paginate(PAYMENTS[id] ?? [], qs)
  }),
  route('POST',   /\/invoices\/([^/]+)\/payments$/, (p, _, b) => {
    const id  = p.match(/\/invoices\/([^/]+)/)?.[1]!
    const pay = { id: makeId(), invoiceId: id, paidAt: NOW, createdAt: NOW, ...(b as object) }
    if (!PAYMENTS[id]) PAYMENTS[id] = []
    PAYMENTS[id].push(pay)
    return pay
  }),
  route('DELETE', /\/invoices\/([^/]+)\/payments\/[^/]+$/, () => null),

  // Follow-ups
  route('GET',  /\/invoices\/([^/]+)\/followups$/, (p) => {
    const id = p.match(/\/invoices\/([^/]+)/)?.[1]!
    return FOLLOWUPS[id] ?? []
  }),
  route('POST', /\/invoices\/([^/]+)\/followups\/dispatch$/, (p, _, b) => {
    const id  = p.match(/\/invoices\/([^/]+)/)?.[1]!
    const chs = ((b as Record<string, unknown>).channels ?? []) as string[]
    const dispatched = chs.map((ch) => ({
      id: makeId(), invoiceId: id, channel: ch, triggerType: 'MANUAL', status: 'SENT',
      templateId: null, reminderRuleId: null, scheduledForDate: null, attachPdf: false,
      sentAt: NOW, createdAt: NOW, updatedAt: NOW,
      paymentLinkId: null, paymentLinkUrl: null, paymentLinkGateway: null, paymentLinkStatus: null,
    }))
    if (!FOLLOWUPS[id]) FOLLOWUPS[id] = []
    FOLLOWUPS[id].push(...dispatched)
    return dispatched
  }),

  // Templates
  route('GET', /\/templates$/, (_, qs) => {
    let list = [...TEMPLATES]
    if (qs.channel) list = list.filter((t) => t.channel === qs.channel)
    if (qs.tone)    list = list.filter((t) => t.tone    === qs.tone)
    if (qs.name)    list = list.filter((t) => (t.name as string).toLowerCase().includes(qs.name.toLowerCase()))
    return paginate(list, qs)
  }),
  route('GET',    /\/templates\/([^/]+)$/, (p) => TEMPLATES.find((t) => t.id === p.match(/\/templates\/([^/]+)$/)?.[1]) ?? null),
  route('POST',   /\/templates$/, (_, __, b) => {
    const t = { id: makeId(), active: true, createdAt: NOW, updatedAt: NOW, ...(b as object) }
    TEMPLATES.push(t)
    return t
  }),
  route('PATCH',  /\/templates\/([^/]+)$/, (p, __, b) => {
    const idx = TEMPLATES.findIndex((x) => x.id === p.match(/\/templates\/([^/]+)$/)?.[1])
    if (idx !== -1) TEMPLATES[idx] = { ...TEMPLATES[idx], ...(b as object) }
    return TEMPLATES[idx] ?? null
  }),
  route('PATCH',  /\/templates\/([^/]+)\/activate$/,   (p) => {
    const idx = TEMPLATES.findIndex((x) => x.id === p.match(/\/templates\/([^/]+)/)?.[1])
    if (idx !== -1) TEMPLATES[idx] = { ...TEMPLATES[idx], active: true }
    return TEMPLATES[idx] ?? null
  }),
  route('PATCH',  /\/templates\/([^/]+)\/deactivate$/, (p) => {
    const idx = TEMPLATES.findIndex((x) => x.id === p.match(/\/templates\/([^/]+)/)?.[1])
    if (idx !== -1) TEMPLATES[idx] = { ...TEMPLATES[idx], active: false }
    return TEMPLATES[idx] ?? null
  }),
  route('DELETE', /\/templates\/([^/]+)$/, (p) => {
    const idx = TEMPLATES.findIndex((t) => t.id === p.match(/\/templates\/([^/]+)$/)?.[1])
    if (idx !== -1) TEMPLATES.splice(idx, 1)
    return null
  }),

  // Reminder Rules
  route('GET', /\/reminder-rules$/, (_, qs) => paginate([...REMINDER_RULES], qs)),
  route('POST', /\/reminder-rules$/, (_, __, b) => {
    const r = { id: makeId(), templateName: null, active: true, createdAt: NOW, updatedAt: NOW, ...(b as object) }
    REMINDER_RULES.push(r)
    return r
  }),
  route('PATCH', /\/reminder-rules\/([^/]+)$/, (p, __, b) => {
    const idx = REMINDER_RULES.findIndex((x) => x.id === p.match(/\/reminder-rules\/([^/]+)$/)?.[1])
    if (idx !== -1) REMINDER_RULES[idx] = { ...REMINDER_RULES[idx], ...(b as object), updatedAt: NOW }
    return REMINDER_RULES[idx] ?? null
  }),
  route('DELETE', /\/reminder-rules\/([^/]+)$/, (p) => {
    const idx = REMINDER_RULES.findIndex((x) => x.id === p.match(/\/reminder-rules\/([^/]+)$/)?.[1])
    if (idx !== -1) REMINDER_RULES.splice(idx, 1)
    return null
  }),
  route('PUT', /\/reminder-rules\/([^/]+)\/activate$/, (p) => {
    const idx = REMINDER_RULES.findIndex((x) => x.id === p.match(/\/reminder-rules\/([^/]+)/)?.[1])
    if (idx !== -1) REMINDER_RULES[idx] = { ...REMINDER_RULES[idx], active: true }
    return REMINDER_RULES[idx] ?? null
  }),
  route('PUT', /\/reminder-rules\/([^/]+)\/deactivate$/, (p) => {
    const idx = REMINDER_RULES.findIndex((x) => x.id === p.match(/\/reminder-rules\/([^/]+)/)?.[1])
    if (idx !== -1) REMINDER_RULES[idx] = { ...REMINDER_RULES[idx], active: false }
    return REMINDER_RULES[idx] ?? null
  }),

  // Payment Confirmations
  route('GET', /\/payment-confirmations$/, (_, qs) => {
    let list = [...CONFIRMATIONS]
    if (qs.status) list = list.filter((c) => c.status === qs.status)
    return paginate(list, qs)
  }),
  route('GET', /\/payment-confirmations\/([^/]+)$/, (p) =>
    CONFIRMATIONS.find((c) => c.id === p.match(/\/payment-confirmations\/([^/]+)$/)?.[1]) ?? null,
  ),
  route('POST', /\/payment-confirmations\/([^/]+)\/approve$/, (p, _, b) => {
    const idx = CONFIRMATIONS.findIndex((x) => x.id === p.match(/\/payment-confirmations\/([^/]+)/)?.[1])
    if (idx !== -1) CONFIRMATIONS[idx] = { ...CONFIRMATIONS[idx], status: 'APPROVED', reviewedAt: NOW, businessNote: (b as Record<string,unknown>)?.businessNote ?? null }
    return CONFIRMATIONS[idx] ?? null
  }),
  route('POST', /\/payment-confirmations\/([^/]+)\/request-remaining$/, (p, _, b) => {
    const idx = CONFIRMATIONS.findIndex((x) => x.id === p.match(/\/payment-confirmations\/([^/]+)/)?.[1])
    if (idx !== -1) CONFIRMATIONS[idx] = { ...CONFIRMATIONS[idx], status: 'REMAINING_REQUESTED', reviewedAt: NOW, businessNote: (b as Record<string,unknown>)?.businessNote ?? null }
    return CONFIRMATIONS[idx] ?? null
  }),
  route('POST', /\/payment-confirmations\/([^/]+)\/reject$/, (p, _, b) => {
    const idx = CONFIRMATIONS.findIndex((x) => x.id === p.match(/\/payment-confirmations\/([^/]+)/)?.[1])
    if (idx !== -1) CONFIRMATIONS[idx] = { ...CONFIRMATIONS[idx], status: 'REJECTED', reviewedAt: NOW, businessNote: (b as Record<string,unknown>)?.businessNote ?? null }
    return CONFIRMATIONS[idx] ?? null
  }),
]

// ---------------------------------------------------------------------------
// Register on axios instance
// ---------------------------------------------------------------------------

export function registerDevMock(axiosInstance: AxiosInstance) {
  if (!import.meta.env.DEV) return

  axiosInstance.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
    const token = config.headers?.Authorization as string | undefined
    if (!token?.includes('dev-token')) return config

    const method   = (config.method ?? 'GET').toUpperCase()
    const fullPath = (config.baseURL ?? '') + (config.url ?? '')
    const pathname = path(fullPath)

    // Only mock our org's endpoints
    if (!pathname.startsWith(BASE)) return config

    const relPath = pathname.slice(BASE.length)
    const qs      = config.params
      ? Object.fromEntries(
          Object.entries(config.params as Record<string, unknown>)
            .filter(([, v]) => v !== undefined)
            .map(([k, v]) => [k, String(v)]),
        )
      : {}

    const body = config.data
      ? (typeof config.data === 'string' ? JSON.parse(config.data) : config.data)
      : undefined

    for (const r of ROUTES) {
      if (r.method !== method) continue
      if (!r.pattern.test(relPath)) continue

      // Small simulated latency
      await new Promise((res) => setTimeout(res, 80))

      const data = r.handle(relPath, qs, body)

      // Replace the transport adapter so no HTTP request is sent
      config.adapter = async () => ({
        data,
        status:     data === null ? 204 : 200,
        statusText: 'OK',
        headers:    {},
        config,
      })
      return config
    }

    return config
  })
}
