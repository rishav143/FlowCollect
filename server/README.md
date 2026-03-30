# FlowCollect — Backend

FlowCollect is a B2B invoice management and payment collection platform. Businesses use it to issue invoices, send automated follow-up reminders, and collect payments — either via gateway payment links (Stripe, Razorpay) or a manual confirmation flow where customers self-report their payment.

For the complete API reference, see [API.md](API.md).

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Project Structure](#project-structure)
3. [Prerequisites](#prerequisites)
4. [Local Setup](#local-setup)
5. [Environment Variables](#environment-variables)
6. [Running the App](#running-the-app)
7. [Running Tests](#running-tests)
8. [Core Concepts](#core-concepts)
9. [Feature Flows](#feature-flows)
10. [Background Jobs](#background-jobs)
11. [Integrations](#integrations)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.2 |
| Database | MySQL (production), H2 in-memory (tests) |
| ORM | Spring Data JPA / Hibernate |
| Auth | JWT (JJWT 0.12.6) + Spring Security |
| Email | Resend via SMTP |
| SMS | Twilio |
| PDF | Apache PDFBox 3.0.6 |
| Payment gateways | Stripe Connect, Razorpay |
| OAuth | Google, Microsoft |
| AI | OpenAI Chat Completions API (gpt-4o-mini) |
| Build | Maven (`mvnw`) |

---

## Project Structure

```
server/
├── src/main/java/com/flowcollect/
│   ├── api/v1/              # REST controllers and DTOs (request/response objects)
│   │   ├── auth/            # Login, register, OAuth, email/phone verification
│   │   ├── confirmation/    # Payment confirmation review (approve/reject)
│   │   ├── customer/        # Customer CRUD
│   │   ├── diagnostics/     # Dev-only test endpoints (email, SMS)
│   │   ├── invoice/         # Invoice, payment, follow-up controllers
│   │   ├── organization/    # Org management
│   │   ├── paymentlink/     # Payment gateway link redirect
│   │   ├── publicapi/       # Customer-facing confirmation endpoints (no auth)
│   │   ├── ai/              # AI template generation/enhancement, insight endpoints
│   │   ├── reminderrule/    # Automated reminder rule CRUD
│   │   ├── template/        # Message template CRUD
│   │   ├── user/            # User management
│   │   └── webhook/         # Stripe & Razorpay webhooks
│   │
│   ├── application/         # Business logic (services)
│   │   ├── auth/            # AuthService, VerificationService
│   │   ├── confirmation/    # PaymentConfirmationService, BusinessNotificationService
│   │   ├── customer/        # CustomerService
│   │   ├── invoice/         # InvoiceService, PaymentService, FollowUpService
│   │   ├── oauth/           # OAuthService (Google/Microsoft)
│   │   ├── organization/    # OrganizationService
│   │   ├── paymentlink/     # PaymentLinkService, ConfirmationLinkService
│   │   ├── ai/              # AiTemplateService, AiInsightService
│   │   ├── reminder/        # ReminderRuleService, ReminderScheduler
│   │   ├── template/        # TemplateService
│   │   └── user/            # UserService
│   │
│   ├── domain/              # JPA entities and domain objects
│   │   ├── confirmation/    # PaymentConfirmation, ConfirmationLink
│   │   ├── customer/        # Customer
│   │   ├── invoice/         # Invoice, InvoiceItem, Payment, FollowUp, PaymentLink
│   │   ├── organization/    # Organization
│   │   ├── reminder/        # ReminderRule
│   │   ├── template/        # Template
│   │   ├── user/            # User
│   │   └── verification/    # EmailVerificationToken, PasswordResetToken, PhoneOtp
│   │
│   ├── infrastructure/      # External integrations and config
│   │   ├── ai/              # OpenAiClient, OpenAiProperties, request/response DTOs
│   │   ├── config/          # @ConfigurationProperties classes
│   │   ├── email/           # Email sender
│   │   ├── pdf/             # PDF generation
│   │   ├── persistence/     # JPA repositories
│   │   ├── sms/             # Twilio SMS sender
│   │   └── whatsapp/        # WhatsApp Cloud API sender
│   │
│   ├── security/            # JWT filter, SecurityConfig
│   ├── scheduler/           # Scheduled jobs (invoice status, reminders, link expiry)
│   ├── exception/           # Global exception handler and custom exceptions
│   └── common/              # Shared utilities
│
├── src/test/                # Unit tests (JUnit 5 + Mockito, H2 in-memory DB)
├── API.md                   # Complete REST API reference
├── pom.xml
└── README.md
```

---

## Prerequisites

- **Java 21** — [Download](https://adoptium.net/)
- **MySQL 8+** — running locally on port 3306
- **Maven** — included via `mvnw` wrapper; no separate install needed

Optional (can be skipped for basic local dev):
- Resend account (for real emails)
- Twilio account (for SMS)
- Stripe account (for payment links)
- Razorpay account (for payment links)
- Google / Microsoft OAuth app (for social login)

---

## Local Setup

### 1. Clone the repo

```bash
git clone <repo-url>
cd "dream Project/PaidPeace/server"
```

### 2. Create the database

```sql
CREATE DATABASE cashclarity;
```

The app also auto-creates it on startup if it doesn't exist (`createDatabaseIfNotExist=true` in the JDBC URL).

### 3. Configure environment variables

Copy the example below into a `.env` file or export them in your shell. Only the database credentials and JWT secret are strictly required to start the app locally.

```bash
# Database
DB_USERNAME=root
DB_PASSWORD=

# JWT — change this in production (min 32 chars)
JWT_SECRET=local-dev-secret-change-me-in-production-minimum-32

# App base URL (used in email verification links, payment links)
APP_BASE_URL=http://localhost:8080

# Gateway credential encryption key (AES-256, 32-char min)
GATEWAY_ENCRYPTION_KEY=local-dev-encryption-key-change-me

# Email (Resend) — optional for local dev
RESEND_API_KEY=
RESEND_FROM=billing@yourdomain.com
RESEND_AUTH_FROM=noreply@yourdomain.com

# SMS (Twilio) — optional
TWILIO_ENABLED=false
TWILIO_ACCOUNT_SID=
TWILIO_AUTH_TOKEN=
TWILIO_SMS_FROM=

# Payment gateways — optional
STRIPE_ENABLED=false
STRIPE_API_KEY=
STRIPE_CONNECT_CLIENT_ID=
STRIPE_WEBHOOK_SECRET=

RAZORPAY_ENABLED=false
RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=
RAZORPAY_WEBHOOK_SECRET=

# OAuth — optional
GOOGLE_OAUTH_CLIENT_ID=
GOOGLE_OAUTH_CLIENT_SECRET=
MICROSOFT_OAUTH_CLIENT_ID=
MICROSOFT_OAUTH_CLIENT_SECRET=
MICROSOFT_OAUTH_TENANT=common

# OpenAI (AI features) — optional
OPENAI_ENABLED=false
OPENAI_API_KEY=
```

### 4. Start the app

```bash
./mvnw spring-boot:run
```

The app starts on **http://localhost:8080**.

Hibernate runs `ddl-auto=update` on startup — it creates or updates tables automatically. You never need to write migration SQL manually for schema changes.

---

## Environment Variables

Full reference of every environment variable the app reads:

| Variable | Required | Default | Description |
|---|---|---|---|
| `DB_USERNAME` | Yes | `root` | MySQL username |
| `DB_PASSWORD` | Yes | _(empty)_ | MySQL password |
| `JWT_SECRET` | Yes | _(insecure default)_ | JWT signing key — 32+ chars; **change in production** |
| `APP_BASE_URL` | No | `http://localhost:8080` | Used in email links and payment redirect URLs |
| `GATEWAY_ENCRYPTION_KEY` | No | _(insecure default)_ | AES key for encrypting gateway credentials — **change in production** |
| `RESEND_API_KEY` | No | _(empty)_ | Resend SMTP password — emails silently skipped if missing |
| `RESEND_FROM` | No | `billing@flowcollect.io` | From address for invoice/payment emails |
| `RESEND_AUTH_FROM` | No | `noreply@flowcollect.io` | From address for account emails (verification, password reset) |
| `NOTIFICATION_EMAIL_FROM_NAME` | No | `FlowCollect` | Display name in email From header |
| `TWILIO_ENABLED` | No | `false` | Set `true` to enable SMS |
| `TWILIO_ACCOUNT_SID` | No | _(empty)_ | |
| `TWILIO_AUTH_TOKEN` | No | _(empty)_ | |
| `TWILIO_SMS_FROM` | No | _(empty)_ | Twilio phone number in E.164 format |
| `STRIPE_ENABLED` | No | `false` | Set `true` to enable Stripe payment links |
| `STRIPE_API_KEY` | No | _(empty)_ | Stripe secret key |
| `STRIPE_CONNECT_CLIENT_ID` | No | _(empty)_ | Stripe Connect OAuth client ID |
| `STRIPE_WEBHOOK_SECRET` | No | _(empty)_ | For verifying Stripe webhook signatures |
| `RAZORPAY_ENABLED` | No | `false` | Set `true` to enable Razorpay |
| `RAZORPAY_KEY_ID` | No | _(empty)_ | |
| `RAZORPAY_KEY_SECRET` | No | _(empty)_ | |
| `RAZORPAY_WEBHOOK_SECRET` | No | _(empty)_ | For verifying Razorpay webhook signatures |
| `GOOGLE_OAUTH_CLIENT_ID` | No | _(empty)_ | Google OAuth app client ID |
| `GOOGLE_OAUTH_CLIENT_SECRET` | No | _(empty)_ | |
| `MICROSOFT_OAUTH_CLIENT_ID` | No | _(empty)_ | Microsoft OAuth app client ID |
| `MICROSOFT_OAUTH_CLIENT_SECRET` | No | _(empty)_ | |
| `MICROSOFT_OAUTH_TENANT` | No | `common` | Azure AD tenant; `common` allows any Microsoft account |
| `SCHEDULER_PAYMENT_LINK_EXPIRY_CRON` | No | `0 0 0 * * *` | Cron for payment link expiry job |
| `OPENAI_ENABLED` | No | `false` | Set `true` to enable AI features |
| `OPENAI_API_KEY` | No | _(empty)_ | OpenAI secret key (`sk-...`); required when `OPENAI_ENABLED=true` |
| `OPENAI_MODEL` | No | `gpt-4o-mini` | Override the OpenAI model (e.g. `gpt-4o`) |
| `OPENAI_MAX_TOKENS` | No | `1024` | Max tokens per AI response |

> **Graceful degradation:** Email, SMS, and gateway features are disabled by default. If a credential is missing, the relevant feature logs a warning and skips silently — the rest of the app works normally. This means you can run fully functional local dev with only the database and JWT secret configured.

---

## Running the App

```bash
# Standard run
./mvnw spring-boot:run

# With explicit env vars (alternative to export)
DB_PASSWORD=secret JWT_SECRET=my-secret-key ./mvnw spring-boot:run

# Kill if port 8080 is already in use
lsof -ti :8080 | xargs kill -9
```

**Verify it's running:**
```bash
curl http://localhost:8080/api/v1/diagnostics/test-email?to=you@example.com
```

---

## Running Tests

Tests use an H2 in-memory database — no MySQL needed.

```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=BusinessNotificationServiceTest

# Run a specific test method
./mvnw test -Dtest=BusinessNotificationServiceTest#notifyPaymentSubmitted_sendsEmail_toOrganization
```

---

## Core Concepts

### Multi-tenancy

Every organization is isolated. All API calls include `{organizationId}` in the path, and the server verifies that the authenticated user belongs to that organization. There is no cross-org data leakage.

### Authentication flow

1. User registers → `RegisterResponse` with `emailVerificationRequired: true`
2. User clicks email link → `GET /api/v1/auth/verify-email?token=...` → returns JWT
3. All subsequent requests carry `Authorization: Bearer <token>`

### User roles

| Role | What they can do |
|---|---|
| `ADMIN` | Full access — manage users, org settings, gateway connections, all data |
| `STAFF` | Everything except user management and gateway configuration |

### Organization `paymentCollectionMode`

Controls how the org collects payment:

| Mode | How it works |
|---|---|
| `PAYMENT_LINK` | Follow-ups include a gateway checkout link. Customer pays directly online. Payment recorded automatically via webhook. |
| `CONFIRMATION_FLOW` | Follow-ups include a confirmation link. Customer self-reports payment. Business user reviews and approves or rejects the claim. |

---

## Feature Flows

### Onboarding a new customer and issuing an invoice

```
POST /auth/register                          → creates org + owner, sends verification email
GET  /auth/verify-email?token=...            → activates account, returns JWT

POST /organizations/{orgId}/customers        → create customer record
POST /organizations/{orgId}/invoices         → create draft invoice with line items
POST /organizations/{orgId}/invoices/{id}/issue  → transitions DRAFT → ISSUED
```

Once issued, the invoice is ready for follow-ups and payment collection.

---

### Sending a payment reminder (manual)

```
POST /organizations/{orgId}/templates        → create a template with {{customerName}}, {{amount}}, etc.

POST /organizations/{orgId}/invoices/{invoiceId}/followups/dispatch
  body: { "channels": ["EMAIL"], "templateId": "...", "includePaymentLink": true, "paymentGateway": "RAZORPAY" }
```

This sends the email immediately and creates a payment link embedded via `{{paymentLink}}` in the template body.

---

### Automated reminders (reminder rules)

```
POST /organizations/{orgId}/reminder-rules
  body: {
    "name": "3 days before due",
    "daysOffset": 3,
    "triggerType": "BEFORE_DUE_DATE",
    "channel": "EMAIL",
    "templateId": "...",
    "active": true
  }
```

The scheduler runs every 15 minutes. For every active rule, it checks all `ISSUED` and `PARTIALLY_PAID` invoices. If an invoice's due date adjusted by `daysOffset` equals today (and the rule hasn't already fired for that invoice), it dispatches a follow-up automatically.

Invoices whose customer has `automationEnabled = false` are silently skipped. Use `PUT /customers/{id}/automation/disable` to opt a customer out of all automated reminders.

Recurring rules: set `maxOccurrences` and `cycleIntervalDays` to fire the rule multiple times per invoice (e.g., every 3 days after due, up to 5 times).

---

### Payment collection — Payment Link flow

```
# Customer pays via gateway
GET /pay/{token}                  → redirects customer to Stripe/Razorpay checkout

# After payment, gateway calls the webhook
POST /api/v1/webhooks/stripe      → signature verified → payment recorded on invoice automatically
```

---

### Payment collection — Confirmation Flow

Enable on org: `PATCH /organizations/{orgId}` with `{ "paymentCollectionMode": "CONFIRMATION_FLOW" }`

```
# Template must include {{confirmationLink}} placeholder
POST /followups/dispatch          → sends email; link resolves to GET /api/v1/public/confirmations/{token}

# Customer side (no auth)
GET  /api/v1/public/confirmations/{token}    → shows invoice summary (amount due, etc.)
POST /api/v1/public/confirmations/{token}
  body: { "amountClaimed": 5000, "customerNote": "Paid via NEFT ref TXN123" }
  → 201 Created; org receives "Payment Claim Received" email

# Business side (authenticated)
GET  /organizations/{orgId}/payment-confirmations?status=PENDING_APPROVAL
POST /organizations/{orgId}/payment-confirmations/{id}/approve
  body: { "note": "Verified on bank statement", "notifyCustomer": true }
  → payment recorded on invoice; customer receives "Payment Confirmed" email

# Or reject
POST /organizations/{orgId}/payment-confirmations/{id}/reject
  body: { "note": "Amount mismatch — expected 6000", "notifyCustomer": true }
  → no payment recorded; customer receives "Payment Not Verified" email; link stays open
```

---

### Recording a payment manually (no customer involvement)

```
POST /organizations/{orgId}/invoices/{invoiceId}/payments
  body: { "amount": 5000, "mode": "BANK_TRANSFER", "referenceId": "TXN123" }
```

This directly reduces the remaining balance. Use for cash, cheque, or bank transfers that you've already confirmed.

---

### Password reset

```
POST /auth/forgot-password   body: { "organizationId": "...", "email": "..." }
# → sends reset email; always returns 204 regardless of whether email exists

POST /auth/reset-password    body: { "token": "...", "newPassword": "..." }
# → token is from the email link
```

---

## Background Jobs

| Job | Schedule | What it does |
|---|---|---|
| Invoice status sync | Every hour | Marks `ISSUED` invoices as `OVERDUE` when past their due date |
| Automated reminder engine | Every 15 minutes | Fires follow-ups for invoices matching active reminder rules; skips invoices whose customer has `automationEnabled = false` |
| Payment link expiry | Daily at midnight | Marks `ACTIVE`/`PARTIALLY_PAID` payment links as `EXPIRED` |

Schedulers can be disabled for testing: set `scheduler.enabled=false` in application.properties.

---

## Integrations

### Email (Resend)

FlowCollect uses [Resend](https://resend.com) via SMTP. All transactional emails are HTML.

| Email type | From address | Trigger |
|---|---|---|
| Email verification | `RESEND_AUTH_FROM` | Registration |
| Password reset | `RESEND_AUTH_FROM` | Forgot password request |
| Invoice follow-up | `RESEND_FROM` | Manual dispatch or reminder rule |
| Payment claim received | `RESEND_FROM` | Customer submits confirmation |
| Payment confirmed | `RESEND_FROM` | Business approves claim |
| Payment not verified | `RESEND_FROM` | Business rejects claim |

If `RESEND_API_KEY` is not set, email calls are silently skipped (logged as WARN). This lets you run locally without an email account.

---

### SMS (Twilio)

Used for phone OTP verification during registration and for SMS follow-up channels.

Set `TWILIO_ENABLED=true` and provide `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_SMS_FROM` to activate.

Test the connection:
```bash
curl -X POST "http://localhost:8080/api/v1/diagnostics/test-sms?to=+917876596480"
```

---

### Stripe

Stripe Connect is used so each organization connects their own Stripe account. The platform generates checkout sessions on their behalf.

Setup:
1. Create a Stripe platform account and enable Connect
2. Set `STRIPE_ENABLED=true`, `STRIPE_API_KEY`, `STRIPE_CONNECT_CLIENT_ID`, `STRIPE_WEBHOOK_SECRET`
3. Organizations connect via `GET /organizations/{orgId}/gateways/stripe/authorize-url` → redirect → `POST /gateways/stripe/connect?code=...`
4. Register the webhook endpoint `POST /api/v1/webhooks/stripe` in Stripe dashboard

---

### Razorpay

Organizations enter their own Razorpay key pair (no OAuth — manual credentials).

Setup:
1. Set `RAZORPAY_ENABLED=true`
2. Organizations connect via `POST /organizations/{orgId}/gateways/razorpay/connect`
3. Register the webhook endpoint `POST /api/v1/webhooks/razorpay` in Razorpay dashboard

Credentials are encrypted with AES-256 before storage using `GATEWAY_ENCRYPTION_KEY`.

---

### OpenAI (AI features)

FlowCollect uses the OpenAI Chat Completions API to power three AI capabilities:

| Feature | Endpoint | What it does |
|---|---|---|
| Template generation | `POST .../ai/templates/generate` | Writes a new payment reminder template from scratch given channel and tone |
| Template enhancement | `POST .../ai/templates/enhance` | Rewrites an existing template to better match a target tone while keeping all `{{placeholders}}` intact |
| Org overview | `GET .../ai/insights/overview` | Summarises overdue balances, top customers to chase, and cash flow signals |
| Customer intelligence | `GET .../ai/insights/customers/{id}` | Profiles a customer's payment behavior and recommends next actions |
| Flexible insights | `POST .../ai/insights/ask` | Answers any natural-language question scoped by optional filters (status, date range, channel, customer) |

**To activate:**
1. Obtain an OpenAI API key from [platform.openai.com](https://platform.openai.com)
2. Add to `.env`:
   ```
   OPENAI_ENABLED=true
   OPENAI_API_KEY=sk-...
   ```
3. The default model is `gpt-4o-mini` (fast, cost-effective). Override with `OPENAI_MODEL=gpt-4o` for higher quality if needed.

AI endpoints are **read-only** and **opt-in per deployment** — they never create, update, or delete data. If `OPENAI_ENABLED=false` or the key is missing, all AI endpoints return `503 Service Unavailable` gracefully.

---

### OAuth (Google / Microsoft)

Used for social login/registration. The flow:
1. Frontend calls `GET /auth/oauth/{provider}/authorize-url` → gets consent URL
2. User is redirected to Google/Microsoft consent page
3. Provider redirects back with `code` and `state`
4. Frontend calls `GET /auth/oauth/{provider}/callback?code=...&state=...&redirectUri=...` → returns JWT

Set the callback URL in your Google/Microsoft OAuth app to match what you pass as `redirectUri`.

---

## Common Mistakes

**"Data truncated for column 'body'"** — Hibernate's `ddl-auto=update` tried to shrink a column. This was fixed by adding `columnDefinition = "TEXT"` to `Template.body`. If you see similar truncation errors on other columns, the same fix applies.

**Port 8080 already in use** — A previous Spring process didn't stop cleanly. Kill it:
```bash
lsof -ti :8080 | xargs kill -9
```

**Approve/reject body field** — The request field for the reviewer's note is `note`, not `businessNote`. The response field in `PaymentConfirmationResponse` is `businessNote` (that's what gets stored and returned).

**Emails not sending locally** — Expected behavior when `RESEND_API_KEY` is not set. The app logs a WARN and continues. To test emails locally, either set a real Resend key or use the diagnostics endpoint.

**`emailVerificationRequired: true` after register** — New users must verify their email before they can log in. Call `GET /auth/verify-email?token=...` with the token from the verification email.
