# FlowCollect — UI Layout & Design Specification

> Based on: design_ui, home_page_components (1–5), Brand Kit PDF, REST API, README_SERVER
> Philosophy: Functional > Beautiful. Clear > Clever. Action > Analysis.

---

## Brand Tokens (from Brand Kit)

### Colors

| Token          | Hex       | Usage                                  |
|----------------|-----------|----------------------------------------|
| Deep Navy      | `#1B2838` | Page backgrounds, sidebar depth        |
| Ocean Teal     | `#2E7A8E` | Nodes, accents, active nav             |
| Electric Blue  | `#29B6F6` | Primary CTA buttons, highlights        |
| Sky Blue       | `#4FC3F7` | Hover states, gradients                |
| Dark Teal      | `#1E5A6A` | Secondary actions                      |
| Slate Gray     | `#8A9BAE` | Muted text, borders                    |
| Cloud          | `#F4F7F9` | Page background, card backgrounds      |
| Ink            | `#0D1B2A` | Body text, headings                    |
| Brand Gradient | `#29B6F6 → #4FC3F7` | Primary CTA buttons, hero   |

Status colors (outside brand kit — standard):
- Overdue / Risk: `#EF4444` (red)
- Due Today / Slow: `#F59E0B` (amber)
- Paid / Good: `#22C55E` (green)
- Draft / Neutral: `#8A9BAE` (slate gray)

### Typography — Inter / SF Pro Display

| Level    | Weight     | Size  | Tracking | Usage                        |
|----------|------------|-------|----------|------------------------------|
| H1       | Bold       | 32px  | -0.5     | Page titles                  |
| H2       | Bold       | 24px  | 0        | Section headings             |
| H3       | Semi-Bold  | 18px  | 0        | Card titles, table headers   |
| Body     | Regular    | 14px  | +0.1     | Table rows, descriptions     |
| Caption  | Regular    | 11px  | +0.2     | Badges, timestamps, labels   |
| Overline | Bold       | 9px   | +3       | UPPERCASE section labels     |

### Logo Rendering

```
Flow  ← white/dark text, Bold
Collect  ← Electric Blue #29B6F6, Bold
(no space between words)
```

---

## App Route Structure

```
FlowCollect
│
├── Public (no auth required)
│   ├── /login
│   ├── /register
│   ├── /verify-email          (token from email link)
│   ├── /verify-phone          (OTP entry)
│   ├── /forgot-password
│   ├── /reset-password
│   └── /confirm/:token        (customer-facing payment claim page)
│
└── App (JWT required)
    ├── /dashboard
    ├── /invoices
    │   └── /invoices/:id
    ├── /followups
    ├── /clients
    │   └── /clients/:id
    ├── /reminder-rules
    └── /settings
        ├── /settings/org
        ├── /settings/gateways    (ADMIN only)
        └── /settings/team        (ADMIN only)
```

> No separate /templates route. Templates are managed inside the Reminder Rules page
> via the "Edit Message" modal on each rule card.
> No separate /confirmations route. Payment confirmation review lives inside Follow-ups page.

---

## Auth Pages (no sidebar, centered layout)

### /register

```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│              Flow[Collect]   ← logo centered                 │
│                                                              │
│         Create your account                                  │
│                                                              │
│  Business name      [________________________________]       │
│  Currency           [INR ▼]                                  │
│  Timezone           [Asia/Kolkata ▼]                         │
│  Your name          [________________________________]       │
│  Email              [________________________________]       │
│  Password           [________________________________]       │
│  Phone (optional)   [________________________________]       │
│                                                              │
│         [  Create Account  ]  ← gradient #29B6F6→#4FC3F7    │
│                                                              │
│         Already have an account?  Log in                     │
└──────────────────────────────────────────────────────────────┘
```

### /verify-email

```
┌──────────────────────────────────────────────────────────────┐
│              Flow[Collect]                                   │
│                                                              │
│              Check your inbox ✉                              │
│                                                              │
│   We sent a verification link to you@example.com            │
│   Click it to activate your account.                         │
│                                                              │
│              [Resend verification email]                     │
└──────────────────────────────────────────────────────────────┘
```

> Email is globally unique — no orgId needed anywhere.
> User never sees or types a UUID at any point in the product.

### /login

```
┌──────────────────────────────────────────────────────────────┐
│              Flow[Collect]                                   │
│                                                              │
│              Log in to your account                          │
│                                                              │
│  Email       [________________________________]              │
│  Password    [________________________________]              │
│                                                              │
│              [  Log In  ]  ← Electric Blue CTA              │
│                                                              │
│  ───────────────── or ─────────────────                      │
│  [G  Continue with Google]  [⊞  Continue with Microsoft]    │
│                                                              │
│   Forgot password?       Don't have an account?  Sign up    │
└──────────────────────────────────────────────────────────────┘
```

> Email is globally unique across the system.
> Backend resolves the org internally from the email.
> No orgId anywhere in the UI — ever.

---

## Main App Shell

```
┌──────────────────────────────────────────────────────────────────────┐
│ TOPBAR  bg: white  border-bottom: #F4F7F9  height: 56px              │
│                                                                      │
│  Flow[Collect]    [🔍 Search invoices / clients...]   [+ Add Invoice] 🔔 [R▼] │
│                                                                      │
│  ← logo left      ← search center, subtle              ← right zone │
└──────────────────────────────────────────────────────────────────────┘
┌──────────────────┬───────────────────────────────────────────────────┐
│  SIDEBAR         │  MAIN CONTENT AREA                                │
│  bg: #F4F7F9     │  bg: white                                        │
│  width: 220px    │                                                   │
│  fixed on scroll │                                                   │
│                  │                                                   │
│  Flow[Collect]   │                                                   │
│  ─────────────   │                                                   │
│  📊 Dashboard    │                                                   │
│  📄 Invoices     │                                                   │
│  ⏰ Follow-ups 5 │  ← badge count: overdue + due today               │
│  👥 Clients      │                                                   │
│  ─────────────   │                                                   │
│  🔔 Reminders    │                                                   │
│  ⚙️  Settings    │                                                   │
│                  │                                                   │
│  Active item:    │                                                   │
│  rounded pill    │                                                   │
│  bg: #2E7A8E     │                                                   │
│  text: white     │                                                   │
└──────────────────┴───────────────────────────────────────────────────┘
```

### Topbar — Right Zone Detail

```
[+ Add Invoice]  ← gradient button, always visible, slightly larger
    🔔           ← bell, red dot if unread notifications
    [R▼]         ← user avatar / initials circle

Profile dropdown:
┌──────────────────┐
│  Rishav's Agency │
│  ──────────────  │
│  My Account      │
│  Settings        │
│  Logout          │
└──────────────────┘
```

Notification dropdown (no separate page):
```
┌────────────────────────────────────┐
│  INV-042 is 12 days overdue        │
│  Payment claim received — INV-039  │
│  INV-044 due today                 │
└────────────────────────────────────┘
```

---

## 1. Dashboard (`/dashboard`)

> Answers: How much money is stuck? What needs action today? Who is causing the delay?

```
┌──────────────────────────────────────────────────────────────────────┐
│  📊 Dashboard                                                        │
│  Quick overview of outstanding payments                              │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────── KPI STRIP ───────────────────────────┐
│                                                                      │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐   │
│  │ 💰 Outstanding   │  │ 🔴 Overdue       │  │ 📅 Due This Week │   │
│  │ ₹12,45,000       │  │ ₹4,80,000        │  │ ₹2,10,000        │   │
│  │ Total unpaid     │  │ Past due         │  │ Next 7 days      │   │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘   │
│                                                                      │
│  ┌──────────────────┐                                                │
│  │ ⏳ Avg Delay     │                                                │
│  │ 9 days           │                                                │
│  │ Payment delay    │                                                │
│  └──────────────────┘                                                │
│                                                                      │
│  Cards: white bg, soft shadow, Slate Gray border                     │
│  Numbers: H2 Bold, Ink #0D1B2A                                       │
│  Labels: Caption, Slate Gray #8A9BAE                                 │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────── MAIN CONTENT ────────────────────────────┐
│                                                                      │
│  ┌────────────────────────── ACTION TABLE ────────────────────────┐  │
│  │ Needs Follow-up Today                                          │  │
│  │ Invoices requiring immediate attention                         │  │
│  │                                                                │  │
│  │  Client        Invoice #   Amount    Due Date  Days OD  Act   │  │
│  │  ──────────────────────────────────────────────────────────   │  │
│  │  ABC Pvt Ltd   INV-1023    ₹45,000   12 Mar    7 days   [▶]   │  │
│  │  XYZ Agency    INV-1041    ₹18,500   10 Mar    9 days   [▶]   │  │
│  │  Nova Tech     INV-1102    ₹92,000   05 Mar    14 days  [▶]   │  │
│  │  Pixel Studio  INV-1088    ₹26,000   08 Mar    11 days  [▶]   │  │
│  │                                                                │  │
│  │  Overdue rows: very light red bg tint                          │  │
│  │  Due today rows: very light amber bg tint                      │  │
│  │  Max 8–10 rows shown, sticky header                            │  │
│  │                                                                │  │
│  │                   [ View All Follow-ups ]                      │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌────────────────────────── INSIGHTS PANEL ──────────────────────┐  │
│  │ Payment Insights                                               │  │
│  │                                                                │  │
│  │ 🔴 Top Delayed Clients                                         │  │
│  │    1. Nova Tech      ₹1,20,000 overdue                         │  │
│  │    2. ABC Pvt Ltd    ₹78,000 overdue                           │  │
│  │    3. Pixel Studio   ₹54,000 overdue                           │  │
│  │                                                                │  │
│  │ ⏳ Oldest Unpaid Invoice                                        │  │
│  │    INV-0931 · ₹64,000 · 32 days overdue                       │  │
│  │                                                                │  │
│  │ 💸 Amount Stuck > 30 Days                                       │  │
│  │    ₹2,40,000                                                   │  │
│  └────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────── EMPTY STATE (no invoices) ───────────────────────┐
│                                                                      │
│  📭 No invoices yet                                                  │
│  Add your first invoice to start tracking payments                   │
│                                                                      │
│                    [➕ Add Invoice]                                   │
└──────────────────────────────────────────────────────────────────────┘
```

> No charts, no graphs, no month selectors in v1.
> Dashboard = action, not analysis.

---

## 2. Invoices (`/invoices`)

> Answers: What is the status of every invoice?

```
┌──────────────────────────────────────────────────────────────────────┐
│  📄 Invoices                                          [➕ Add Invoice] │
│  Track and follow up on customer payments                            │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────── TABS ────────────────────────────────┐
│  [ All (24) ]   [ Due (8) ]   [ Overdue (10) 🔴 ]   [ Paid (6) ]    │
│                                                                      │
│  Default: All. Overdue tab has red badge count.                      │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────── FILTER BAR ──────────────────────────────┐
│  Client ▾   Due Date ▾   Amount ▾   Status ▾                        │
│  Max 3 filters. No advanced filtering in v1.                         │
└──────────────────────────────────────────────────────────────────────┘

┌────────────────────────── INVOICES TABLE ────────────────────────────┐
│                                                                      │
│  Invoice #   Client Name     Amount    Due Date   Status     Action  │
│  ────────────────────────────────────────────────────────────────── │
│  INV-1023    ABC Pvt Ltd     ₹45,000   12 Mar     🔴 Overdue 7d  [▶] │
│  INV-1041    XYZ Agency      ₹18,500   15 Mar     🟡 Due Today   [▶] │
│  INV-1102    Nova Tech       ₹92,000   05 Mar     🔴 Overdue 14d [▶] │
│  INV-1088    Pixel Studio    ₹26,000   20 Mar     🟢 Paid        [▶] │
│  INV-1115    Orion Systems   ₹54,000   18 Mar     ⚪ Due          [▶] │
│  INV-1120    Freelance Co    ₹12,000   —          🔵 Draft        [▶] │
│                                                                      │
│  Whole row clickable → Invoice Detail                                │
│  Hover → subtle highlight                                            │
│  Overdue rows → very light red bg tint                               │
│                                                                      │
│                          [ Load More ]                               │
└──────────────────────────────────────────────────────────────────────┘

Status Legend:
  🟢 Paid     → green pill
  🟡 Due      → amber pill  (DUE_TODAY from API)
  🔴 Overdue  → red pill + days count  (OVERDUE from API)
  ⚪ Upcoming → slate gray  (UPCOMING from API)
  🔵 Draft    → blue outline pill

┌──────────────────── EMPTY STATE ─────────────────────────────────────┐
│  📭 No invoices found                                                │
│  Add your first invoice to start tracking payments                   │
│  [➕ Add Invoice]                                                     │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 3. Invoice Detail (`/invoices/:id`)

```
  ← Back to Invoices

  INV-1023                                    🔴 Overdue · 7 days
  ABC Pvt Ltd

┌──────────────────────────────────────────────────────────────────────┐
│  Total Amount   ₹45,000                                              │
│  Amount Paid    ₹0                                                   │
│  Remaining      ₹45,000                                              │
│  Due Date       12 Mar 2026                                          │
│  Issued         1 Mar 2026                                           │
└──────────────────────────────────────────────────────────────────────┘

  Action Bar (contextual by status):
  ┌─────────────────────────────────────────────────────────────────┐
  │  ISSUED / OVERDUE:                                              │
  │  [Follow up now]  [Record Payment]  [Download PDF]  [Cancel ▼] │
  │                                                                 │
  │  DRAFT only:                                                    │
  │  [Issue Invoice]  [Edit]  [Delete]                              │
  └─────────────────────────────────────────────────────────────────┘

  Line Items:
  ─────────────────────────────────────────────────────────────────
  Web Design – Homepage      1 × ₹35,000        ₹35,000
  Domain Setup               1 × ₹10,000        ₹10,000
                                      Subtotal   ₹45,000
                                      Tax  (0%)  ₹0
                                      Total      ₹45,000

  Tabs:
  [ Payments ]   [ Follow-ups ]

  ── Payments Tab ──────────────────────────────────────────────────
  No payments recorded yet.
  [+ Record Payment]

  ── Follow-ups Tab ────────────────────────────────────────────────
  Mar 10   EMAIL      Sent    "Polite follow-up"
  Mar 14   WhatsApp   Sent    "Overdue firm notice"
  ──────────────────────────────────────────────────────────────────
```

**Follow up now — Modal:**
```
┌──────────────────────────────────────────────────────────────────┐
│  Follow-up for INV-1023                                          │
│                                                                  │
│  Choose Action:                                                  │
│   ● Send Email Reminder                                          │
│   ○ Send WhatsApp Reminder                                       │
│   ○ Mark as Paid                                                 │
│   ○ Add Note                                                     │
│                                                                  │
│  Template   [Polite follow-up ▼]                                 │
│  Attach PDF [  ] Yes                                             │
│                                                                  │
│  Add Internal Note:                                              │
│  [ Client promised to pay on Friday...               ]           │
│                                                                  │
│                  [ Cancel ]   [ Send Follow-up ]                 │
└──────────────────────────────────────────────────────────────────┘
```

**Record Payment — Modal:**
```
┌──────────────────────────────────────────────────────────────────┐
│  Record Payment                                                  │
│                                                                  │
│  Amount       [_______________________]                          │
│  Mode         [Bank Transfer ▼]                                  │
│               CASH / BANK_TRANSFER / CHEQUE / ONLINE / OTHER     │
│  Reference    [_______________________]  optional               │
│  Notes        [_______________________]  optional               │
│                                                                  │
│              [ Cancel ]   [ Record Payment ]                     │
└──────────────────────────────────────────────────────────────────┘
```

---

## 4. Follow-ups (`/followups`)

> Answers: Who do I need to follow up with right now?
> This is a to-do list for money — not an invoice table.

```
┌──────────────────────────────────────────────────────────────────────┐
│  ⏰ Follow-ups                                                        │
│  Invoices that need your immediate attention                         │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────── FILTER TABS ─────────────────────────────┐
│  [ Today (5) ]   [ Overdue (12) 🔴 ]   [ Upcoming (7) ]   [ All ]   │
│  Default: Today                                                      │
└──────────────────────────────────────────────────────────────────────┘

Priority:
  🔴 Overdue  → left border red + light red bg tint
  🟡 Due Today → left border amber
  ⚪ Upcoming  → neutral

Automatic sort (not user-controlled):
  1. Oldest overdue first
  2. Highest amount next
  3. Least recently followed-up last

┌────────────────────────── ACTION CARDS ──────────────────────────────┐
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │ 🔴  CLIENT: Nova Tech                                        │   │
│  │     Invoice #INV-1102 · ₹92,000                              │   │
│  │     Due: 14 days ago                                         │   │
│  │     Last Follow-up: WhatsApp · 4 days ago                    │   │
│  │                          [ Follow up now ▶ ]                 │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │ 🔴  CLIENT: ABC Pvt Ltd                                      │   │
│  │     Invoice #INV-1023 · ₹45,000                              │   │
│  │     Due: 7 days ago                                          │   │
│  │     Last Follow-up: Email · 3 days ago                       │   │
│  │                          [ Follow up now ▶ ]                 │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │ 🟡  CLIENT: XYZ Agency                                       │   │
│  │     Invoice #INV-1041 · ₹18,500                              │   │
│  │     Due: Today                                               │   │
│  │     Last Follow-up: None                                     │   │
│  │                          [ Follow up now ▶ ]                 │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │ ⚪   CLIENT: Pixel Studio                                     │   │
│  │     Invoice #INV-1088 · ₹26,000                              │   │
│  │     Due: In 3 days                                           │   │
│  │     Last Follow-up: Email · 1 day ago                        │   │
│  │                          [ Follow up now ▶ ]                 │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
│                          [ Load More ]                               │
└──────────────────────────────────────────────────────────────────────┘

"Follow up now" → opens Quick Action Modal (same as Invoice Detail modal):
┌──────────────────────────────────────────────────────────────────┐
│  Follow-up for INV-1102                                          │
│                                                                  │
│  Choose Action:                                                  │
│   ● Send Email Reminder                                          │
│   ○ Send WhatsApp Reminder                                       │
│   ○ Mark as Paid                                                 │
│   ○ Add Note                                                     │
│                                                                  │
│  Add Internal Note:                                              │
│  [ Client promised to pay on Friday...               ]           │
│                                                                  │
│                  [ Cancel ]   [ Send Follow-up ]                 │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────── EMPTY STATE (all clear) ─────────────────────────┐
│  🎉 All caught up!                                                   │
│  No invoices need follow-up today.                                   │
│  ✔ You're up to date                                                 │
└──────────────────────────────────────────────────────────────────────┘
```

> Also shows payment confirmation claims (CONFIRMATION_FLOW mode) as cards here,
> inline with the follow-up list. No separate nav item needed for confirmations.

---

## 5. Clients (`/clients`)

> Answers: Which clients are good for my business — and which are risky?
> Not a CRM. Payment behavior insight only.

```
┌──────────────────────────────────────────────────────────────────────┐
│  👥 Clients                                                          │
│  View customer payment behavior and outstanding balances             │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────── CLIENT METRICS ──────────────────────────┐
│                                                                      │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐   │
│  │ Total Clients    │  │ High Risk        │  │ Good Payers      │   │
│  │ 28               │  │ 6                │  │ 14               │   │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘   │
│                                                                      │
│  ┌──────────────────┐                                                │
│  │ Avg Payment Delay│                                                │
│  │ 11 days          │                                                │
│  └──────────────────┘                                                │
└──────────────────────────────────────────────────────────────────────┘

┌────────────────────────── CLIENTS TABLE ─────────────────────────────┐
│                                                                      │
│  Client Name      Outstanding  Overdue    Avg Delay  Open  Risk      │
│  ─────────────────────────────────────────────────────────────────── │
│  ABC Pvt Ltd      ₹78,000      ₹35,000    12 days    3     🔴 Risk ▶ │
│  Nova Tech        ₹1,20,000    ₹82,000    18 days    5     🔴 Risk ▶ │
│  XYZ Agency       ₹26,500      ₹0         4 days     2     🟢 Good ▶ │
│  Pixel Studio     ₹54,000      ₹12,000    9 days     3     🟡 Slow ▶ │
│  Orion Systems    ₹18,000      ₹0         2 days     1     🟢 Good ▶ │
│                                                                      │
│  Click row → Client Detail page                                      │
│  Hover → subtle highlight                                            │
└──────────────────────────────────────────────────────────────────────┘

Risk Legend:
  🟢 Good  → usually pays on time     (avg delay < 5 days)
  🟡 Slow  → occasional delays        (avg delay 5–14 days)
  🔴 Risk  → frequently late          (avg delay > 14 days)
  (computed on frontend from payment history — not stored on backend)

┌──────────────────── EMPTY STATE ─────────────────────────────────────┐
│  👤 No clients yet                                                   │
│  Clients will appear here once you add invoices                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 6. Client Detail (`/clients/:id`)

```
  ← Back to Clients

  Nova Tech                                             🔴 High Risk
  billing@novatech.com  ·  +91 98765 00000

┌──────────────────────────────────────────────────────────────────────┐
│  Total Outstanding  ₹1,20,000      Avg Payment Delay  18 days       │
│  Open Invoices      5                                                │
└──────────────────────────────────────────────────────────────────────┘

  Auto-reminders   [ON ●──]   ← disables automated follow-ups for client

  [Edit Client Details]

┌────────────────────── INVOICES FOR THIS CLIENT ──────────────────────┐
│  Invoice #   Amount    Due Date    Status              Action        │
│  ────────────────────────────────────────────────────────────────── │
│  INV-1102    ₹92,000   05 Mar      🔴 Overdue (14d)    [▶]           │
│  INV-1089    ₹28,000   12 Mar      🟡 Due Today        [▶]           │
│  INV-1044    ₹14,000   22 Mar      ⚪ Upcoming         [▶]           │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────── CLIENT NOTES ────────────────────────────────┐
│  Notes                                            [+ Add Note]      │
│                                                                      │
│  • Always delays payments by ~15 days                                │
│  • Prefers WhatsApp reminders                                        │
│  • Asked for extended credit period last quarter                     │
└──────────────────────────────────────────────────────────────────────┘
```

> "Client Notes" is frontend-stored in v1 (localStorage or a new API endpoint).
> Not currently in the REST API — note this for backend v2.

---

## 7. Reminder Rules (`/reminder-rules`)

> "Set once, forget forever."
> Designed to feel safe and calm — not like an automation engine.

```
┌──────────────────────────────────────────────────────────────────────┐
│  🔔 Reminder Rules                              [ Enable All  ON ⏻ ] │
│  Automatically remind clients about unpaid invoices                  │
└──────────────────────────────────────────────────────────────────────┘

Timeline layout: Before Due → Due Date → After Due

┌──────────────────────────── RULE CARDS ──────────────────────────────┐
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  Before Due                                  [ Enabled ⏻ ]  │   │
│  │                                                              │   │
│  │  Timing:   [ 3 days before due ▼ ]                           │   │
│  │  Channel:  [ Email ☑ ]  [ WhatsApp ☐ ]  [ Manual ☐ ]        │   │
│  │                                                              │   │
│  │  Message Preview:                                            │   │
│  │  "Hi {{customerName}}, this is a reminder that invoice       │   │
│  │  {{invoiceNumber}} of ₹{{amount}} is due on {{dueDate}}."    │   │
│  │                                                              │   │
│  │                  [ Edit Message ]   [ Delete ]               │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  Due Date                                    [ Enabled ⏻ ]  │   │
│  │                                                              │   │
│  │  Timing:   [ On due date ▼ ]                                 │   │
│  │  Channel:  [ Email ☑ ]  [ WhatsApp ☑ ]  [ Manual ☐ ]        │   │
│  │                                                              │   │
│  │  Message Preview:                                            │   │
│  │  "Invoice {{invoiceNumber}} is due today. Please arrange     │   │
│  │  payment."                                                   │   │
│  │                                                              │   │
│  │                  [ Edit Message ]   [ Delete ]               │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  Overdue                                     [ Enabled ⏻ ]  │   │
│  │                                                              │   │
│  │  Timing:   [ 7 days overdue ▼ ]                              │   │
│  │  Channel:  [ Email ☐ ]  [ WhatsApp ☑ ]  [ Manual ☐ ]        │   │
│  │                                                              │   │
│  │  Message Preview:                                            │   │
│  │  "Invoice {{invoiceNumber}} is overdue. Please confirm       │   │
│  │  payment date."                                              │   │
│  │                                                              │   │
│  │                  [ Edit Message ]   [ Delete ]               │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
│                      [ ➕ Add New Reminder Rule ]                    │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────── GLOBAL SETTINGS ─────────────────────────────────────┐
│  Sender Name:    [ ABC Consulting Pvt Ltd         ]                  │
│  Sender Email:   [ billing@abc.com                ]                  │
│  WhatsApp:       [ Enable ⏻ ]                                        │
│  WhatsApp No:    [ +91 98765 43210                ]                  │
│                                                                      │
│  Signature Preview:                                                  │
│  — ABC Consulting Pvt Ltd                                            │
└──────────────────────────────────────────────────────────────────────┘
```

**Edit Message — Modal:**
```
┌──────────────────────────────────────────────────────────────────┐
│  Edit Reminder Message                                           │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Hi {{customerName}}, this is a reminder that invoice    │   │
│  │  {{invoiceNumber}} of ₹{{amount}} is due on {{dueDate}}. │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  Insert variable:                                                │
│  [{{customerName}}] [{{invoiceNumber}}] [{{amount}}] [{{dueDate}}] │
│  [{{paymentLink}}]  [{{confirmationLink}}]                       │
│                                                                  │
│  Live Preview:                                                   │
│  "Hi ABC Pvt Ltd, invoice INV-123 of ₹45,000 is due 12 Mar."   │
│                                                                  │
│                   [ Cancel ]   [ Save Message ]                  │
└──────────────────────────────────────────────────────────────────┘
```

**First-time empty state:**
```
  We've added default reminder rules for you.
  You can customize them anytime.
```

---

## 8. Settings

### Org (`/settings/org`)
```
  Business name     [Rishav's Agency               ]
  Email             [rishav@agency.com              ]
  Phone             [+91 98765 43210                ]
  Address           [Mumbai, India                  ]
  Currency          [INR ▼]
  Timezone          [Asia/Kolkata ▼]

  Payment Collection Mode
  (●) Payment Link     — customer pays via Stripe / Razorpay directly
  ( ) Manual Confirm   — customer self-reports, you approve

  [Save Changes]
```

### Gateways (`/settings/gateways`) — ADMIN only
```
  STRIPE
  ─────────────────────────────────────────────────────────────────
  Status: Not connected
  [Connect Stripe Account]  ← OAuth redirect flow

  RAZORPAY
  ─────────────────────────────────────────────────────────────────
  Status: Connected  (rzp_live_••••ABCD)          [Disconnect]

  Key ID          [rzp_live_••••••ABCD     ]
  Key Secret      [•••••••••••••••••••••••• ]
  Webhook Secret  [•••••••••••••••••••••••• ]
  [Update Credentials]
```

### Team (`/settings/team`) — ADMIN only
```
  [+ Add Team Member]

  Name              Email                 Role    Status   Actions
  ──────────────────────────────────────────────────────────────────
  Rishav Choudhary  rishav@agency.com     Admin   Active   [...]
  Priya Sharma      priya@agency.com      Staff   Active   [...]
  ──────────────────────────────────────────────────────────────────
```

---

## 9. Customer-Facing Page (public, no login)

### /confirm/:token — Payment Confirmation Claim

```
┌──────────────────────────────────────────────────────────────────┐
│                    Flow[Collect]                                 │
│                                                                  │
│  Payment due to     Rishav's Agency                              │
│  Invoice            INV-1023                                     │
│  Amount Due         ₹45,000                                      │
│  Due Date           12 Mar 2026                                  │
│                                                                  │
│  ──────────────────────────────────────────────────────────     │
│  Confirm your payment                                            │
│                                                                  │
│  Amount paid        [____________________________]               │
│  Reference / note   [____________________________]  optional    │
│                                                                  │
│              [ I've Made This Payment ]                          │
│                                                                  │
│  (shows 409 message if claim already pending)                    │
└──────────────────────────────────────────────────────────────────┘
```

---

## 10. API → UI Term Mapping

| API term              | UI label          | Reason                         |
|-----------------------|-------------------|--------------------------------|
| `customers`           | Clients           | Feels less transactional       |
| `lifeCycleStatus`     | hidden            | Users don't need this term     |
| `timeStatus`          | Status badge      | Shown as colored pill          |
| `UPCOMING`            | Due (date shown)  | Clear meaning                  |
| `DUE_TODAY`           | Due Today         | Urgent language                |
| `OVERDUE`             | Overdue · Xd      | Days count adds urgency        |
| `ISSUED`              | Active / Sent     | Matches user mental model      |
| `DRAFT`               | Draft             | Fine as-is                     |
| `automationEnabled`   | Auto-reminders    | Toggle label on client detail  |
| `CONFIRMATION_FLOW`   | Manual Confirm    | Plain English                  |
| `PAYMENT_LINK`        | Payment Link      | Fine as-is                     |
| `POLITE/NEUTRAL/FIRM` | Tone              | Used inside template editor    |
| `triggerType`         | Timing            | In reminder rule card          |

---

## 11. API Gaps (features in UI not yet in REST API)

| UI Feature                | API Status         | Resolution                                          |
|---------------------------|--------------------|-----------------------------------------------------|
| Login without orgId       | Breaking change    | Enforce globally unique email on User table. Remove `organizationId` from login, forgot-password, resend-verification, verify-phone, resend-phone-otp, and OAuth login. Backend resolves org from email internally |
| Client Notes              | Not in API         | Frontend localStorage in v1; add `POST /customers/:id/notes` in v2 |
| WhatsApp channel          | Infrastructure exists, not in follow-up API | Add `WHATSAPP` to channel enum when ready |
| Client Risk score         | Not stored         | Computed frontend from `payments` + `invoices` data |
| Avg payment delay         | Not stored         | Computed frontend from payment history              |
| Notification bell items   | No notifications API | Frontend-derived from polling invoices in v1     |

---

## 12. Navigation Rules

| Role  | Can access                                             |
|-------|--------------------------------------------------------|
| ADMIN | Everything                                             |
| STAFF | Dashboard, Invoices, Follow-ups, Clients, Reminders — no Gateways, Team |

| Org Mode          | Follow-ups page confirmation cards |
|-------------------|------------------------------------|
| `PAYMENT_LINK`    | Not shown                          |
| `CONFIRMATION_FLOW` | Shown as cards with Approve / Reject |

---

## 13. Visual & Component Decisions

- **Component library:** shadcn/ui + Tailwind CSS
- **Sidebar:** fixed, 220px, bg `#F4F7F9`, active item pill `#2E7A8E`
- **Topbar:** white, 56px height, bottom border `#F4F7F9`
- **Cards:** white bg, `shadow-sm`, `rounded-lg`, border `#F4F7F9`
- **Primary CTA:** gradient `#29B6F6 → #4FC3F7`, rounded
- **Tables:** clean rows, no heavy borders, overdue rows get `bg-red-50` tint
- **Status pills:** rounded-full, small text, color-coded
- **Red used ONLY for:** overdue status, risk badge, notification dot
- **Green used ONLY for:** paid status, good payer badge
- **No dark mode in v1**
- **No animation libraries in v1**
- **Mobile:** sidebar collapses to hamburger, tables become cards
