# FlowCollect API Documentation

Complete reference for all REST endpoints. Use this document when building the UI — every path, method, request body field, and response schema is current.

## Base URL
`http://localhost:8080` (local dev)

## Authentication
All protected endpoints require a Bearer JWT in the `Authorization` header:
```
Authorization: Bearer <token>
```
Obtain the token from `/api/v1/auth/login` or `/api/v1/auth/oauth/{provider}/callback`.

## Multi-tenancy
All organization-scoped endpoints include `{organizationId}` (UUID) in the path. The server enforces that the authenticated user belongs to that organization — any mismatch returns `403 Forbidden`.

---

## 1. Authentication

### 1.1 Register
**`POST /api/v1/auth/register`** — No auth required.

**Request body:**
| Field | Type | Required | Notes |
|---|---|---|---|
| `organizationName` | String | Yes | max 100 |
| `currency` | String | Yes | 3-letter ISO code, e.g. `INR`, `USD` |
| `timezone` | String | Yes | e.g. `Asia/Kolkata`, `America/New_York` |
| `ownerName` | String | Yes | max 100 |
| `email` | String | Yes | valid email, max 100 |
| `password` | String | Yes | 8–100 chars |
| `phone` | String | No | max 20; triggers SMS OTP verification if provided |

**Response: `201 Created`**
| Field | Type | Notes |
|---|---|---|
| `organizationId` | UUID | |
| `userId` | UUID | |
| `emailVerificationRequired` | boolean | Always `true` — user must verify email before logging in |
| `phoneVerificationRequired` | boolean | `true` if `phone` was supplied at registration |
| `message` | String | Human-readable next-step message |

---

### 1.2 Login
**`POST /api/v1/auth/login`** — No auth required.

**Request body:**
| Field | Type | Required |
|---|---|---|
| `organizationId` | UUID | Yes |
| `email` | String | Yes |
| `password` | String | Yes |

**Response: `200 OK`** — `LoginResponse`
| Field | Type | Notes |
|---|---|---|
| `token` | String | JWT bearer token |
| `type` | String | Always `"Bearer"` |
| `id` | UUID | User ID |
| `organizationId` | UUID | |
| `name` | String | User's full name |
| `email` | String | |
| `role` | String | `ADMIN` or `STAFF` |
| `status` | String | User account status |
| `expiresAt` | Instant | Token expiry timestamp |
| `profileImageUrl` | String | nullable — set when user registered via OAuth |

---

### 1.3 Verify Email
**`GET /api/v1/auth/verify-email?token={token}`** — No auth required.

Validates the email verification token sent after registration. On success, activates the user account and returns a JWT so the user is immediately logged in.

**Query params:** `token` (String, required)

**Response: `200 OK`** — `LoginResponse` (same fields as login)

---

### 1.4 Resend Verification Email
**`POST /api/v1/auth/resend-verification-email`** — No auth required.

**Request body:**
| Field | Type | Required |
|---|---|---|
| `organizationId` | UUID | Yes |

**Response: `204 No Content`**

---

### 1.5 Verify Phone (OTP)
**`POST /api/v1/auth/verify-phone`** — No auth required.

**Request body:**
| Field | Type | Required |
|---|---|---|
| `organizationId` | UUID | Yes |
| `otp` | String | Yes |

**Response: `204 No Content`**

---

### 1.6 Resend Phone OTP
**`POST /api/v1/auth/resend-phone-otp`** — No auth required.

**Request body:**
| Field | Type | Required |
|---|---|---|
| `organizationId` | UUID | Yes |

**Response: `204 No Content`**

---

### 1.7 Forgot Password
**`POST /api/v1/auth/forgot-password`** — No auth required.

Always returns `204` regardless of whether the email exists (prevents user enumeration). Sends a reset link to the address if found.

**Request body:**
| Field | Type | Required |
|---|---|---|
| `organizationId` | UUID | Yes |
| `email` | String | Yes |

**Response: `204 No Content`**

---

### 1.8 Reset Password
**`POST /api/v1/auth/reset-password`** — No auth required.

**Request body:**
| Field | Type | Required | Notes |
|---|---|---|---|
| `token` | String | Yes | Token from the reset email link |
| `newPassword` | String | Yes | 8–100 chars |

**Response: `204 No Content`**

---

### 1.9 OAuth — Get Authorize URL
**`GET /api/v1/auth/oauth/{provider}/authorize-url`** — No auth required.

**Path params:** `provider` — `google` or `microsoft` (case-insensitive)

**Query params:**
| Param | Type | Required | Notes |
|---|---|---|---|
| `mode` | String | Yes | `LOGIN` or `REGISTER` |
| `redirectUri` | String | Yes | Must be registered in the OAuth app settings |
| `organizationId` | UUID | Conditional | Required when `mode=LOGIN`; omit for `REGISTER` |

**Response: `200 OK`**
```json
{ "authorizationUrl": "<provider-consent-page-url>" }
```

---

### 1.10 OAuth — Callback
**`GET /api/v1/auth/oauth/{provider}/callback`** — No auth required.

Exchanges the provider's authorization code for a JWT. Creates the user if this is their first OAuth login.

**Path params:** `provider` — `google` or `microsoft`

**Query params:** `code` (String), `state` (String), `redirectUri` (String) — all required.

**Response: `200 OK`** — `LoginResponse` (same fields as login)

---

## 2. Organization

> Organizations can only be created via `POST /api/v1/auth/register`. The `POST /api/v1/organizations` endpoint is blocked for authenticated users.
> The `activate`, `suspend`, `archive`, and `DELETE` organization endpoints always return `403 Forbidden` — reserved for internal tooling.

### 2.1 Get Organization
**`GET /api/v1/organizations/{organizationId}`** — ADMIN or STAFF

**Response: `200 OK`** — `OrganizationResponse`
| Field | Type | Notes |
|---|---|---|
| `id` | UUID | |
| `name` | String | |
| `email` | String | |
| `phone` | String | nullable |
| `address` | String | nullable |
| `timezone` | String | |
| `currency` | String | ISO 4217 code |
| `logoUrl` | String | nullable |
| `status` | String | `ACTIVE`, `TRIAL`, `SUSPENDED`, `ARCHIVED` |
| `paymentCollectionMode` | String | `PAYMENT_LINK` or `CONFIRMATION_FLOW` |
| `createdAt` | Instant | |
| `updatedAt` | Instant | |

---

### 2.2 List Organizations
**`GET /api/v1/organizations`** — ADMIN or STAFF

Always scoped to the authenticated org — returns at most 1 result.

**Query params (all optional):** `status`, `email`, `name`, `createdFrom` (YYYY-MM-DD), `createdTo` (YYYY-MM-DD), `page`, `size`, `sort`

**Response: `200 OK`** — `Page<OrganizationResponse>`

---

### 2.3 Update Organization
**`PATCH /api/v1/organizations/{organizationId}`** — ADMIN

All fields optional.

**Request body:**
| Field | Type | Notes |
|---|---|---|
| `name` | String | |
| `email` | String | |
| `phone` | String | |
| `address` | String | |
| `logoUrl` | String | |
| `currency` | String | ISO 4217 code |
| `timezone` | String | |
| `paymentCollectionMode` | String | `PAYMENT_LINK` or `CONFIRMATION_FLOW` |

**Response: `200 OK`** — `OrganizationResponse`

---

## 3. Users

### 3.1 Create User
**`POST /api/v1/organizations/{organizationId}/users`** — ADMIN

**Request body:**
| Field | Type | Required | Notes |
|---|---|---|---|
| `name` | String | Yes | max 100 |
| `email` | String | Yes | valid email, max 100 |
| `password` | String | Yes | 8–100 chars |
| `role` | String | Yes | `ADMIN` or `STAFF` |

**Response: `201 Created`** — `UserResponse`
| Field | Type | Notes |
|---|---|---|
| `id` | UUID | |
| `organizationId` | UUID | |
| `name` | String | |
| `email` | String | |
| `role` | String | `ADMIN` or `STAFF` |
| `status` | String | |
| `createdAt` | Instant | |
| `updatedAt` | Instant | |

---

### 3.2 List Users
**`GET /api/v1/organizations/{organizationId}/users`** — ADMIN or STAFF

**Query params (all optional):** `status`, `email`, `name`, `role`, `page`, `size`, `sort`

**Response: `200 OK`** — `Page<UserResponse>`

---

### 3.3 Get User
**`GET /api/v1/organizations/{organizationId}/users/{userId}`** — ADMIN or STAFF

**Response: `200 OK`** — `UserResponse`

---

### 3.4 Update User
**`PATCH /api/v1/organizations/{organizationId}/users/{userId}`** — ADMIN

**Request body (all optional):** `name`, `email`, `role` (`ADMIN` or `STAFF`)

**Response: `200 OK`** — `UserResponse`

---

### 3.5 Change Password
**`POST /api/v1/organizations/{organizationId}/users/{userId}/password`** — ADMIN or STAFF

**Request body:**
| Field | Type | Required |
|---|---|---|
| `oldPassword` | String | No |
| `newPassword` | String | Yes |

**Response: `204 No Content`**

---

### 3.6 Activate / Deactivate User
- **Activate:** `POST /api/v1/organizations/{organizationId}/users/{userId}/activate` — ADMIN
- **Deactivate:** `POST /api/v1/organizations/{organizationId}/users/{userId}/deactivate` — ADMIN

**Response: `200 OK`** — `UserResponse`

---

### 3.7 Delete User
**`DELETE /api/v1/organizations/{organizationId}/users/{userId}`** — ADMIN

**Response: `204 No Content`**

---

## 4. Customers

### 4.1 Create Customer
**`POST /api/v1/organizations/{organizationId}/customers`** — ADMIN or STAFF

**Request body:**
| Field | Type | Required |
|---|---|---|
| `name` | String | Yes |
| `email` | String | No |
| `phone` | String | No |
| `address` | String | No |
| `companyName` | String | No |

**Response: `201 Created`** — `CustomerResponse`
| Field | Type | Notes |
|---|---|---|
| `id` | UUID | |
| `organizationId` | UUID | |
| `name` | String | |
| `email` | String | nullable |
| `phone` | String | nullable |
| `address` | String | nullable |
| `companyName` | String | nullable |
| `active` | boolean | |
| `automationEnabled` | boolean | `true` by default — when `false`, the automated reminder engine skips all invoices belonging to this customer |
| `createdAt` | Instant | |
| `updatedAt` | Instant | |

---

### 4.2 List Customers
**`GET /api/v1/organizations/{organizationId}/customers`** — ADMIN or STAFF

**Query params (all optional):** `name`, `email`, `phone`, `companyName`, `active` (boolean), `page`, `size`, `sort`

**Response: `200 OK`** — `Page<CustomerResponse>`

---

### 4.3 Get Customer
**`GET /api/v1/organizations/{organizationId}/customers/{id}`** — ADMIN or STAFF

**Response: `200 OK`** — `CustomerResponse`

---

### 4.4 Update Customer
**`PATCH /api/v1/organizations/{organizationId}/customers/{id}`** — ADMIN or STAFF

**Request body (all optional):** `name`, `email`, `phone`, `address`, `companyName`

**Response: `200 OK`** — `CustomerResponse`

---

### 4.5 Activate / Deactivate Customer
- **Activate:** `PUT /api/v1/organizations/{organizationId}/customers/{id}/activate`
- **Deactivate:** `PUT /api/v1/organizations/{organizationId}/customers/{id}/deactivate`

**Response: `200 OK`** — `CustomerResponse`

---

### 4.6 Enable / Disable Customer Automation
- **Enable:** `PUT /api/v1/organizations/{organizationId}/customers/{id}/automation/enable`
- **Disable:** `PUT /api/v1/organizations/{organizationId}/customers/{id}/automation/disable`

Controls whether the automated reminder engine fires for this customer's invoices. Defaults to enabled (`true`). Both endpoints are idempotent.

When `automationEnabled = false`:
- No new automated follow-ups are scheduled for this customer's invoices
- Invoices with no customer attached (`customer = null`) are always eligible — this flag only applies to invoices linked to this customer
- Manual follow-ups via `POST /followups` or `POST /followups/dispatch` are unaffected

**Response: `200 OK`** — `CustomerResponse`

---

### 4.7 Delete Customer
**`DELETE /api/v1/organizations/{organizationId}/customers/{id}`** — ADMIN or STAFF

**Response: `204 No Content`**

---

## 5. Invoices

### 5.1 Create Draft Invoice
**`POST /api/v1/organizations/{organizationId}/invoices`** — ADMIN or STAFF

**Request body:**
| Field | Type | Required | Notes |
|---|---|---|---|
| `customerId` | UUID | Yes | |
| `invoiceNumber` | String | Yes | max 100; must be unique within the org |
| `items` | Array | Yes | See InvoiceItem below |
| `issueDate` | String | No | `YYYY-MM-DD` |
| `dueDate` | String | No | `YYYY-MM-DD` |
| `taxPercentage` | Decimal | No | 0–100; defaults to 0 |
| `createdByUserId` | UUID | No | |

**InvoiceItem fields (within `items` array):**
| Field | Type | Required |
|---|---|---|
| `description` | String | Yes |
| `quantity` | Integer | Yes |
| `unitPrice` | Decimal | Yes |

**Response: `201 Created`** — `InvoiceResponse`
| Field | Type | Notes |
|---|---|---|
| `id` | UUID | |
| `organizationId` | UUID | |
| `customerId` | UUID | |
| `createdByUserId` | UUID | nullable |
| `invoiceNumber` | String | |
| `timeStatus` | String | `UPCOMING`, `DUE_TODAY`, `OVERDUE`, `PAID` |
| `lifeCycleStatus` | String | `DRAFT`, `ISSUED`, `CANCELLED` |
| `issueDate` | String | `YYYY-MM-DD`, nullable |
| `dueDate` | String | `YYYY-MM-DD`, nullable |
| `subtotal` | Decimal | |
| `taxPercentage` | Decimal | |
| `totalAmount` | Decimal | |
| `totalPaid` | Decimal | Sum of all approved payments |
| `remainingAmount` | Decimal | `totalAmount - totalPaid` |
| `items` | Array | `InvoiceItemResponse` objects |
| `createdAt` | Instant | |
| `updatedAt` | Instant | |

---

### 5.2 List Invoices
**`GET /api/v1/organizations/{organizationId}/invoices`** — ADMIN or STAFF

**Query params (all optional):** `timeStatus`, `lifeCycleStatus`, `invoiceNumber`, `createdAt`, `updatedAt`, `dueDate`, `page`, `size`, `sort`

**Response: `200 OK`** — `Page<InvoiceResponse>`

---

### 5.3 Get Invoice
**`GET /api/v1/organizations/{organizationId}/invoices/{invoiceId}`** — ADMIN or STAFF

**Response: `200 OK`** — `InvoiceResponse`

---

### 5.4 Update Draft Invoice
**`PATCH /api/v1/organizations/{organizationId}/invoices/{invoiceId}`** — ADMIN or STAFF

Only allowed when `lifeCycleStatus = DRAFT`.

**Request body (all optional):** `customerId`, `invoiceNumber`, `items`, `dueDate`, `taxPercentage`, `createdByUserId`

**Response: `200 OK`** — `InvoiceResponse`

---

### 5.5 Issue Invoice
**`POST /api/v1/organizations/{organizationId}/invoices/{invoiceId}/issue`** — ADMIN or STAFF

Transitions invoice from `DRAFT` → `ISSUED`. After this, payments and follow-ups can be recorded.

**Request body (optional):**
| Field | Type | Notes |
|---|---|---|
| `issueDate` | String | `YYYY-MM-DD`; defaults to today if omitted |

**Response: `200 OK`** — `InvoiceResponse`

---

### 5.6 Download Invoice PDF
**`GET /api/v1/organizations/{organizationId}/invoices/{invoiceId}/pdf`** — ADMIN or STAFF

**Response: `200 OK`** — binary PDF (`Content-Type: application/pdf`)

---

### 5.7 Delete Invoice
**`DELETE /api/v1/organizations/{organizationId}/invoices/{invoiceId}`** — ADMIN or STAFF

Only allowed when `lifeCycleStatus = DRAFT`.

**Response: `204 No Content`**

---

## 6. Payments

Payments are manual records of money received against an invoice. Each payment reduces the remaining balance.

### 6.1 Record Payment
**`POST /api/v1/organizations/{organizationId}/invoices/{invoiceId}/payments`** — ADMIN or STAFF

**Request body:**
| Field | Type | Required | Notes |
|---|---|---|---|
| `amount` | Decimal | Yes | Must be > 0 |
| `mode` | String | Yes | `CASH`, `BANK_TRANSFER`, `CHEQUE`, `ONLINE`, `CRYPTO`, `OTHER` |
| `referenceId` | String | No | max 100 |
| `notes` | String | No | max 255 |

**Response: `201 Created`** — `PaymentResponse`
| Field | Type | Notes |
|---|---|---|
| `id` | UUID | |
| `invoiceId` | UUID | |
| `amount` | Decimal | |
| `mode` | String | |
| `referenceId` | String | nullable |
| `notes` | String | nullable |
| `paidAt` | Instant | |
| `createdAt` | Instant | |

---

### 6.2 List Payments
**`GET /api/v1/organizations/{organizationId}/invoices/{invoiceId}/payments`** — ADMIN or STAFF

**Query params (all optional):** `mode`, `paidOn` (YYYY-MM-DD), `page`, `size`, `sort`

**Response: `200 OK`** — `Page<PaymentResponse>`

---

### 6.3 Get Payment
**`GET /api/v1/organizations/{organizationId}/invoices/{invoiceId}/payments/{paymentId}`** — ADMIN or STAFF

**Response: `200 OK`** — `PaymentResponse`

---

### 6.4 Update Payment
**`PATCH /api/v1/organizations/{organizationId}/invoices/{invoiceId}/payments/{paymentId}`** — ADMIN or STAFF

**Request body (all optional):** same fields as Record Payment.

**Response: `200 OK`** — `PaymentResponse`

---

### 6.5 Delete Payment
**`DELETE /api/v1/organizations/{organizationId}/invoices/{invoiceId}/payments/{paymentId}`** — ADMIN or STAFF

**Response: `204 No Content`**

---

## 7. Follow-ups

Follow-ups are outbound messages (email, SMS) sent to customers about an invoice. They can be manual or triggered by reminder rules.

### 7.1 Create Follow-up
**`POST /api/v1/organizations/{organizationId}/invoices/{invoiceId}/followups`** — ADMIN or STAFF

**Request body:**
| Field | Type | Required | Notes |
|---|---|---|---|
| `channel` | String | Yes | `EMAIL` or `SMS` |
| `triggerType` | String | No | `MANUAL` (default) or `AUTOMATED` |
| `templateId` | UUID | No | Template to use for the message |
| `scheduledForDate` | String | No | `YYYY-MM-DD` |
| `attachPdf` | Boolean | No | Attach invoice PDF (email only) |

**Response: `201 Created`** — `FollowUpResponse`
| Field | Type | Notes |
|---|---|---|
| `id` | UUID | |
| `invoiceId` | UUID | |
| `channel` | String | `EMAIL` or `SMS` |
| `triggerType` | String | `MANUAL` or `AUTOMATED` |
| `status` | String | `PENDING`, `SENT`, `FAILED` |
| `templateId` | UUID | nullable |
| `reminderRuleId` | UUID | nullable — set when triggered by a reminder rule |
| `scheduledForDate` | String | `YYYY-MM-DD`, nullable |
| `sentAt` | Instant | nullable |
| `createdAt` | Instant | |
| `paymentLinkId` | UUID | nullable |
| `paymentLinkUrl` | String | nullable — short URL sent to the customer |
| `paymentLinkGateway` | String | nullable — `STRIPE` or `RAZORPAY` |
| `paymentLinkStatus` | String | nullable — `ACTIVE`, `PAID`, `EXPIRED` |

---

### 7.2 Dispatch Multi-channel Follow-up
**`POST /api/v1/organizations/{organizationId}/invoices/{invoiceId}/followups/dispatch`** — ADMIN or STAFF

Creates and immediately dispatches follow-ups across multiple channels in one call.

**Request body:**
| Field | Type | Required | Notes |
|---|---|---|---|
| `channels` | Array of String | Yes | e.g. `["EMAIL", "SMS"]` |
| `templateId` | UUID | No | |
| `scheduledForDate` | String | No | `YYYY-MM-DD` |
| `attachPdf` | Boolean | No | Email only |
| `includePaymentLink` | Boolean | No | Default `false`. When `true`, generates a payment link and injects it via `{{paymentLink}}` in the template |
| `paymentGateway` | String | Conditional | `STRIPE` or `RAZORPAY`. Required when `includePaymentLink=true`. Org must have an active connection for the chosen gateway |

**Response: `201 Created`** — `List<FollowUpResponse>` (one entry per channel)

---

### 7.3 List Follow-ups
**`GET /api/v1/organizations/{organizationId}/invoices/{invoiceId}/followups`** — ADMIN or STAFF

**Query params (all optional):** `status`, `triggerType`, `channel`, `page`, `size`, `sort`

**Response: `200 OK`** — `Page<FollowUpResponse>`

---

### 7.4 Get Follow-up
**`GET /api/v1/organizations/{organizationId}/invoices/{invoiceId}/followups/{followUpId}`** — ADMIN or STAFF

**Response: `200 OK`** — `FollowUpResponse`

---

### 7.5 Update Follow-up
**`PATCH /api/v1/organizations/{organizationId}/invoices/{invoiceId}/followups/{followUpId}`** — ADMIN or STAFF

**Request body (all optional):** same fields as Create Follow-up.

**Response: `200 OK`** — `FollowUpResponse`

---

### 7.6 Send / Fail Follow-up
- **Send:** `PATCH /api/v1/organizations/{organizationId}/invoices/{invoiceId}/followups/{followUpId}/send`
- **Fail:** `PATCH /api/v1/organizations/{organizationId}/invoices/{invoiceId}/followups/{followUpId}/fail`

Manually marks a follow-up as `SENT` or `FAILED`.

**Response: `200 OK`** — `FollowUpResponse`

---

### 7.7 Delete Follow-up
**`DELETE /api/v1/organizations/{organizationId}/invoices/{invoiceId}/followups/{followUpId}`** — ADMIN or STAFF

Only `PENDING` or `FAILED` follow-ups can be deleted.

**Response: `204 No Content`**

---

## 8. Templates

Templates contain the message body for follow-ups. The body supports the following placeholders:

| Placeholder | Replaced with |
|---|---|
| `{{customerName}}` | Customer's full name |
| `{{invoiceNumber}}` | Invoice number |
| `{{amount}}` | Total invoice amount |
| `{{dueDate}}` | Due date (YYYY-MM-DD) |
| `{{confirmationLink}}` | Customer-facing URL to submit a payment claim (Confirmation Flow only) |
| `{{paymentLink}}` | Customer-facing URL to pay via gateway (Payment Link Flow only) |

### 8.1 Create Template
**`POST /api/v1/organizations/{organizationId}/templates`** — ADMIN or STAFF

**Request body:**
| Field | Type | Required | Notes |
|---|---|---|---|
| `name` | String | Yes | Unique within the org; max 100 |
| `channel` | String | Yes | `EMAIL` or `SMS` |
| `subject` | String | No | Email subject line; max 200 |
| `body` | String | Yes | Message content with optional placeholders |
| `tone` | String | Yes | `POLITE`, `NEUTRAL`, or `FIRM` |

**Response: `201 Created`** — `TemplateResponse`
| Field | Type | Notes |
|---|---|---|
| `id` | UUID | |
| `name` | String | |
| `channel` | String | |
| `subject` | String | nullable |
| `body` | String | |
| `tone` | String | |
| `active` | boolean | |
| `createdAt` | Instant | |
| `updatedAt` | Instant | |

---

### 8.2 List Templates
**`GET /api/v1/organizations/{organizationId}/templates`** — ADMIN or STAFF

**Query params (all optional):** `name`, `channel`, `tone`, `page`, `size`, `sort`

**Response: `200 OK`** — `Page<TemplateResponse>`

---

### 8.3 Get Template
**`GET /api/v1/organizations/{organizationId}/templates/{id}`** — ADMIN or STAFF

**Response: `200 OK`** — `TemplateResponse`

---

### 8.4 Update Template
**`PATCH /api/v1/organizations/{organizationId}/templates/{id}`** — ADMIN or STAFF

**Request body (all optional):** same fields as Create Template.

**Response: `200 OK`** — `TemplateResponse`

---

### 8.5 Activate / Deactivate Template
- **Activate:** `PATCH /api/v1/organizations/{organizationId}/templates/{id}/activate`
- **Deactivate:** `PATCH /api/v1/organizations/{organizationId}/templates/{id}/deactivate`

**Response: `200 OK`** — `TemplateResponse`

---

### 8.6 Delete Template
**`DELETE /api/v1/organizations/{organizationId}/templates/{id}`** — ADMIN or STAFF

**Response: `204 No Content`**

---

## 9. Reminder Rules

Reminder rules automate follow-up dispatch. The scheduler runs every 15 minutes, checks all active rules, and fires follow-ups for matching invoices.

### 9.1 Create Reminder Rule
**`POST /api/v1/organizations/{organizationId}/reminder-rules`** — ADMIN or STAFF

**Request body:**
| Field | Type | Required | Notes |
|---|---|---|---|
| `name` | String | Yes | |
| `daysOffset` | Integer | Yes | Days relative to due date. Negative = before due, positive = after due, 0 = on due date |
| `triggerType` | String | Yes | `BEFORE_DUE_DATE`, `ON_DUE_DATE`, or `AFTER_DUE_DATE` |
| `channel` | String | Yes | `EMAIL` or `SMS` |
| `templateId` | UUID | Yes | Template to use for the automated message |
| `active` | Boolean | No | Defaults to `false` — activate explicitly when ready |
| `maxOccurrences` | Integer | No | Max times this rule fires per invoice; `null` = unlimited |
| `cycleIntervalDays` | Integer | No | Days between repeat firings (for recurring rules) |
| `startDate` | String | No | `YYYY-MM-DD`; invoices before this date are skipped |

**Response: `201 Created`** — `ReminderRuleResponse`
| Field | Type | Notes |
|---|---|---|
| `id` | UUID | |
| `name` | String | |
| `daysOffset` | Integer | |
| `triggerType` | String | |
| `channel` | String | |
| `templateId` | UUID | |
| `templateName` | String | |
| `attachPdf` | boolean | |
| `active` | boolean | |
| `maxOccurrences` | Integer | nullable |
| `cycleIntervalDays` | Integer | nullable |
| `startDate` | String | nullable |
| `createdAt` | Instant | |
| `updatedAt` | Instant | |

---

### 9.2 List Reminder Rules
**`GET /api/v1/organizations/{organizationId}/reminder-rules`** — ADMIN or STAFF

**Query params (all optional):** `name`, `page`, `size`, `sort`

**Response: `200 OK`** — `Page<ReminderRuleResponse>`

---

### 9.3 Get Reminder Rule
**`GET /api/v1/organizations/{organizationId}/reminder-rules/{reminderRuleId}`** — ADMIN or STAFF

**Response: `200 OK`** — `ReminderRuleResponse`

---

### 9.4 Update Reminder Rule
**`PATCH /api/v1/organizations/{organizationId}/reminder-rules/{reminderRuleId}`** — ADMIN or STAFF

**Request body (all optional):** same fields as Create Reminder Rule.

**Response: `200 OK`** — `ReminderRuleResponse`

---

### 9.5 Activate / Deactivate Reminder Rule
- **Activate:** `PATCH /api/v1/organizations/{organizationId}/reminder-rules/{reminderRuleId}/activate`
- **Deactivate:** `PATCH /api/v1/organizations/{organizationId}/reminder-rules/{reminderRuleId}/deactivate`

Inactive rules are skipped entirely by the scheduler.

**Response: `200 OK`** — `ReminderRuleResponse`

---

### 9.6 Delete Reminder Rule
**`DELETE /api/v1/organizations/{organizationId}/reminder-rules/{reminderRuleId}`** — ADMIN or STAFF

**Response: `204 No Content`**

---

## 10. Payment Gateway Connections

All gateway endpoints require **ADMIN** role.

### 10.1 List Gateway Connections
**`GET /api/v1/organizations/{organizationId}/gateways`** — ADMIN

Returns all configured gateway connections. Raw credentials are never exposed — only masked hints.

**Response: `200 OK`** — `List<GatewayConnectionResponse>`
| Field | Type | Notes |
|---|---|---|
| `gateway` | String | `STRIPE` or `RAZORPAY` |
| `status` | String | e.g. `ACTIVE`, `DISCONNECTED` |
| `displayName` | String | Masked account identifier |
| `createdAt` | Instant | |
| `updatedAt` | Instant | |

---

### 10.2 Stripe — Get Authorize URL
**`GET /api/v1/organizations/{organizationId}/gateways/stripe/authorize-url`** — ADMIN

Returns the Stripe OAuth consent page URL. Redirect the user there to begin the Stripe Connect flow.

**Response: `200 OK`**
```json
{ "authorizeUrl": "<stripe-oauth-url>" }
```

---

### 10.3 Stripe — Complete Connection
**`POST /api/v1/organizations/{organizationId}/gateways/stripe/connect`** — ADMIN

**Query params:** `code` (String, required) — authorization code returned by Stripe after user consent.

**Response: `200 OK`** — `GatewayConnectionResponse`

---

### 10.4 Stripe — Disconnect
**`DELETE /api/v1/organizations/{organizationId}/gateways/stripe`** — ADMIN

Existing active payment links will stop working after disconnection.

**Response: `200 OK`** — `GatewayConnectionResponse`

---

### 10.5 Razorpay — Connect
**`POST /api/v1/organizations/{organizationId}/gateways/razorpay/connect`** — ADMIN

Saves encrypted Razorpay credentials. Keys are validated against the Razorpay API before storage. Idempotent — calling again updates the existing connection.

**Request body:**
| Field | Type | Required |
|---|---|---|
| `keyId` | String | Yes |
| `keySecret` | String | Yes |
| `webhookSecret` | String | Yes |

**Response: `200 OK`** — `GatewayConnectionResponse`

---

### 10.6 Razorpay — Disconnect
**`DELETE /api/v1/organizations/{organizationId}/gateways/razorpay`** — ADMIN

**Response: `200 OK`** — `GatewayConnectionResponse`

---

## 11. Payment Confirmation Flow

An alternative to payment links. Instead of redirecting customers to a gateway checkout, the organization sends a `{{confirmationLink}}` in the follow-up. The customer self-reports their payment, and a business user approves or rejects the claim.

**Enable:** Set `paymentCollectionMode = CONFIRMATION_FLOW` on the organization (see `PATCH /api/v1/organizations/{organizationId}`).

**Email notifications (automatic):**
- When a customer submits a claim → org receives "Payment Claim Received" email
- When org approves → customer receives "Payment Confirmed" email
- When org rejects → customer receives "Payment Not Verified" email with reason and outstanding balance

---

### 11.1 Public — Get Confirmation View (Customer-Facing)
**`GET /api/v1/public/confirmations/{token}`** — No auth required

`token` is an unguessable 32-char hex value. Returns a read-only invoice summary the customer sees before submitting a claim.

**Response: `200 OK`** — `CustomerConfirmationView`
| Field | Type | Notes |
|---|---|---|
| `invoiceNumber` | String | |
| `organizationName` | String | |
| `customerName` | String | |
| `totalAmount` | Decimal | |
| `totalPaid` | Decimal | Amount already approved |
| `remainingAmount` | Decimal | |
| `dueDate` | String | `YYYY-MM-DD`, nullable |
| `currency` | String | ISO 4217 code |
| `linkStatus` | String | `OPEN` or `CLOSED` |

---

### 11.2 Public — Submit Payment Claim (Customer-Facing)
**`POST /api/v1/public/confirmations/{token}`** — No auth required

Returns `409 Conflict` if a claim is already pending review or the invoice is no longer collectible.

**Request body:**
| Field | Type | Required | Notes |
|---|---|---|---|
| `amountClaimed` | Decimal | Yes | Must be > 0 |
| `customerNote` | String | No | e.g. "Paid via NEFT, ref: TXN123456"; max 500 chars |

**Response: `201 Created`**
| Field | Type |
|---|---|
| `confirmationId` | UUID |
| `amountClaimed` | Decimal |
| `status` | String |
| `createdAt` | Instant |

---

### 11.3 List Payment Confirmations
**`GET /api/v1/organizations/{organizationId}/payment-confirmations`** — ADMIN or STAFF

**Query params (all optional):** `status` (`PENDING_APPROVAL`, `APPROVED`, `REJECTED`), `page`, `size`, `sort`

**Response: `200 OK`** — `Page<PaymentConfirmationResponse>`
| Field | Type | Notes |
|---|---|---|
| `id` | UUID | |
| `invoiceId` | UUID | |
| `invoiceNumber` | String | |
| `confirmationLinkId` | UUID | |
| `amountClaimed` | Decimal | Amount self-reported by customer |
| `customerNote` | String | nullable |
| `status` | String | `PENDING_APPROVAL`, `APPROVED`, `REJECTED` |
| `businessNote` | String | nullable — set by reviewer on approve/reject |
| `reviewedAt` | Instant | nullable |
| `createdAt` | Instant | |

---

### 11.4 Get Payment Confirmation
**`GET /api/v1/organizations/{organizationId}/payment-confirmations/{confirmationId}`** — ADMIN or STAFF

**Response: `200 OK`** — `PaymentConfirmationResponse`

---

### 11.5 Approve Payment Confirmation
**`POST /api/v1/organizations/{organizationId}/payment-confirmations/{confirmationId}/approve`** — ADMIN or STAFF

Records the claimed amount as a payment on the invoice. If the invoice becomes fully paid, the confirmation link is automatically closed.

**Request body (optional):**
| Field | Type | Notes |
|---|---|---|
| `note` | String | Internal reviewer note; max 500 chars |
| `notifyCustomer` | boolean | Default `true` — sends approval email to customer |

**Response: `200 OK`** — `PaymentConfirmationResponse`

---

### 11.6 Reject Payment Confirmation
**`POST /api/v1/organizations/{organizationId}/payment-confirmations/{confirmationId}/reject`** — ADMIN or STAFF

No payment is recorded. The confirmation link stays open so the customer may resubmit.

**Request body (optional):**
| Field | Type | Notes |
|---|---|---|
| `note` | String | Reason for rejection; shown in the customer's rejection email; max 500 chars |
| `notifyCustomer` | boolean | Default `true` — sends rejection email to customer |

**Response: `200 OK`** — `PaymentConfirmationResponse`

---

## 12. Public & Webhook Endpoints

### 12.1 Payment Link Redirect (Customer-Facing)
**`GET /pay/{token}`** — No auth required

`token` is an unguessable UUID. Redirects the customer to the gateway checkout page. Returns `410 Gone` if the link has already been paid or has expired.

**Response: `302 Found`** (redirect) or **`410 Gone`**

---

### 12.2 Stripe Webhook
**`POST /api/v1/webhooks/stripe`** — No JWT required; verified via `Stripe-Signature` header (HMAC-SHA256)

Receives `checkout.session.completed` events. Marks the associated payment link as paid and records the payment automatically.

**Required header:** `Stripe-Signature`

**Response: `200 OK`**

---

### 12.3 Razorpay Webhook
**`POST /api/v1/webhooks/razorpay`** — No JWT required; verified via `X-Razorpay-Signature` header (HMAC-SHA256)

Receives `payment_link.paid` events. Marks the associated payment link as paid and records the payment automatically.

**Required header:** `X-Razorpay-Signature`

**Response: `200 OK`**

---

## 13. Background Schedulers

| Job | Schedule | Description |
|---|---|---|
| Invoice overdue sync | Every hour | Checks all `ISSUED` invoices; marks as `OVERDUE` when past `dueDate` |
| Automated reminder engine | Every 15 minutes | Scans active reminder rules across all orgs; dispatches follow-ups for matching invoices. Respects `startDate`, `maxOccurrences`, `cycleIntervalDays`, and customer `automationEnabled` flag |
| Payment link expiry | Daily at midnight | Marks `ACTIVE` and `PARTIALLY_PAID` payment links as `EXPIRED` when past `expiresAt` |

---

## 14. AI Features

AI endpoints use OpenAI under the hood. They are **disabled by default** — set `OPENAI_ENABLED=true` and `OPENAI_API_KEY` in your environment to activate. All endpoints require **ADMIN or STAFF** role.

If OpenAI is disabled or the API key is missing, all AI endpoints return `503 Service Unavailable`.

> **Not destructive:** AI endpoints never create, update, or delete any data. Generate and enhance endpoints return suggestions for the caller to review — saving is done separately via the standard template CRUD endpoints. Insight endpoints are read-only.

---

### 14.1 AI — Generate Template
**`POST /api/v1/organizations/{organizationId}/ai/templates/generate`** — ADMIN or STAFF

Generates a brand-new payment reminder template from scratch based on channel and tone. Returns a suggestion — call `POST /templates` separately to save it.

**Request body:**
| Field | Type | Required | Values |
|---|---|---|---|
| `channel` | String | Yes | `EMAIL`, `SMS`, `WHATSAPP` |
| `tone` | String | Yes | `POLITE`, `NEUTRAL`, `FIRM` |

**Response: `200 OK`**
| Field | Type | Notes |
|---|---|---|
| `subject` | String | Email subject line. `null` for SMS and WHATSAPP |
| `body` | String | Message body with `{{placeholder}}` variables pre-populated |

**Example:**
```json
POST /api/v1/organizations/{orgId}/ai/templates/generate
{
  "channel": "EMAIL",
  "tone": "POLITE"
}

→ 200 OK
{
  "subject": "Friendly reminder: Invoice #INV-042 is due soon",
  "body": "Hi {{customerName}},\n\nJust a quick reminder that invoice #{{invoiceNumber}} for {{totalAmount}} is due on {{dueDate}}.\n\nPlease pay at your earliest convenience:\n{{paymentLink}}\n\nThank you,\n{{organizationName}}"
}
```

---

### 14.2 AI — Enhance Template
**`POST /api/v1/organizations/{organizationId}/ai/templates/enhance`** — ADMIN or STAFF

Rewrites an existing template body (and subject) to better match a target tone, while preserving all `{{placeholder}}` variables exactly.

**Request body:**
| Field | Type | Required | Notes |
|---|---|---|---|
| `channel` | String | Yes | `EMAIL`, `SMS`, `WHATSAPP` |
| `tone` | String | Yes | `POLITE`, `NEUTRAL`, `FIRM` |
| `subject` | String | No | Existing subject (EMAIL only); omit for SMS/WHATSAPP |
| `body` | String | Yes | Existing template body |

**Response: `200 OK`**
| Field | Type | Notes |
|---|---|---|
| `subject` | String | Rewritten subject. `null` for SMS and WHATSAPP |
| `body` | String | Rewritten body — all original placeholders preserved |

---

### 14.3 AI — Organization Payment Health Overview
**`GET /api/v1/organizations/{organizationId}/ai/insights/overview`** — ADMIN or STAFF

Analyzes the organization's full invoice portfolio and returns actionable AI-generated insights. Covers: overdue priorities, top customers to chase, recent payment receipts, and cash flow signals.

**Response: `200 OK`**
| Field | Type | Notes |
|---|---|---|
| `insights` | String | Bullet-point list separated by `\n`. Each bullet starts with `- ` |

**Example response:**
```json
{
  "insights": "- 3 invoices are overdue totalling $4,200 — prioritise Acme Corp ($2,500) immediately as it is 45 days past due.\n- Collect $1,800 from two invoices due today before end of day.\n- You collected $6,100 in the last 30 days — strong momentum; follow up with the 3 partially-paid invoices to convert them.\n- Enable automated reminders for customers without automation to reduce manual chasing."
}
```

---

### 14.4 AI — Customer Payment Intelligence
**`GET /api/v1/organizations/{organizationId}/ai/insights/customers/{customerId}`** — ADMIN or STAFF

Analyzes a specific customer's invoice history and returns a payment behavior profile with recommended next actions.

**Response: `200 OK`**
| Field | Type | Notes |
|---|---|---|
| `insights` | String | Bullet-point list separated by `\n` |

---

### 14.5 AI — Ask Anything (Flexible Insight)
**`POST /api/v1/organizations/{organizationId}/ai/insights/ask`** — ADMIN or STAFF

Ask any payment-related question in natural language with optional data filters. The backend resolves the filters to real invoice and follow-up data, then asks OpenAI the question against that scoped context.

**Request body:**
| Field | Type | Required | Notes |
|---|---|---|---|
| `question` | String | Yes | Natural-language question; max 500 chars |
| `customerId` | UUID | No | Scope data to one customer |
| `lifeCycleStatus` | String | No | `DRAFT`, `ISSUED`, `PARTIALLY_PAID`, `PAID`, `CANCELLED` |
| `timeStatus` | String | No | `NOT_DUE`, `DUE_TODAY`, `OVERDUE` |
| `channel` | String | No | `EMAIL`, `SMS`, `WHATSAPP` — scopes follow-up data |
| `fromDate` | String | No | `YYYY-MM-DD` — invoice issue date lower bound |
| `toDate` | String | No | `YYYY-MM-DD` — invoice issue date upper bound |

**Response: `200 OK`**
| Field | Type | Notes |
|---|---|---|
| `insights` | String | Bullet-point list separated by `\n` |

**Example requests:**
```json
// Who should I follow up with this week?
{ "question": "Which customers should I chase this week?" }

// Is SMS working?
{ "question": "How effective are my SMS reminders?", "channel": "SMS" }

// Q1 collection summary
{ "question": "How much did I collect in Q1?", "fromDate": "2026-01-01", "toDate": "2026-03-31" }

// Overdue situation
{ "question": "Summarise my overdue situation", "timeStatus": "OVERDUE" }

// One customer deep-dive
{ "question": "Is this customer a reliable payer?", "customerId": "uuid-here" }
```

---

## 15. Diagnostics (Development Only)

> Remove or guard these before production. No JWT required.

### 14.1 Test Email
**`POST /api/v1/diagnostics/test-email?to={email}`**

**Response: `200 OK`**
```json
{ "status": "sent", "to": "...", "from": "...", "message": "..." }
```

---

### 14.2 Test SMS
**`POST /api/v1/diagnostics/test-sms?to={phone}`**

`phone` must be in E.164 format (e.g. `+917876596480`). Returns `503` if Twilio is disabled.

**Response: `200 OK`**
```json
{ "status": "sent", "to": "...", "from": "...", "message": "..." }
```

---

## Quick Reference — All Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | None | Register org + owner |
| POST | `/api/v1/auth/login` | None | Login |
| GET | `/api/v1/auth/verify-email` | None | Verify email token |
| POST | `/api/v1/auth/resend-verification-email` | None | Resend verification email |
| POST | `/api/v1/auth/verify-phone` | None | Submit phone OTP |
| POST | `/api/v1/auth/resend-phone-otp` | None | Resend phone OTP |
| POST | `/api/v1/auth/forgot-password` | None | Request password reset |
| POST | `/api/v1/auth/reset-password` | None | Set new password |
| GET | `/api/v1/auth/oauth/{provider}/authorize-url` | None | Get OAuth consent URL |
| GET | `/api/v1/auth/oauth/{provider}/callback` | None | OAuth code exchange |
| GET | `/api/v1/organizations/{orgId}` | ADMIN/STAFF | Get org |
| GET | `/api/v1/organizations` | ADMIN/STAFF | List orgs |
| PATCH | `/api/v1/organizations/{orgId}` | ADMIN | Update org |
| POST | `/api/v1/organizations/{orgId}/users` | ADMIN | Create user |
| GET | `/api/v1/organizations/{orgId}/users` | ADMIN/STAFF | List users |
| GET | `/api/v1/organizations/{orgId}/users/{userId}` | ADMIN/STAFF | Get user |
| PATCH | `/api/v1/organizations/{orgId}/users/{userId}` | ADMIN | Update user |
| POST | `/api/v1/organizations/{orgId}/users/{userId}/activate` | ADMIN | Activate user |
| POST | `/api/v1/organizations/{orgId}/users/{userId}/deactivate` | ADMIN | Deactivate user |
| POST | `/api/v1/organizations/{orgId}/users/{userId}/password` | ADMIN/STAFF | Change password |
| DELETE | `/api/v1/organizations/{orgId}/users/{userId}` | ADMIN | Delete user |
| POST | `/api/v1/organizations/{orgId}/customers` | ADMIN/STAFF | Create customer |
| GET | `/api/v1/organizations/{orgId}/customers` | ADMIN/STAFF | List customers |
| GET | `/api/v1/organizations/{orgId}/customers/{id}` | ADMIN/STAFF | Get customer |
| PATCH | `/api/v1/organizations/{orgId}/customers/{id}` | ADMIN/STAFF | Update customer |
| PUT | `/api/v1/organizations/{orgId}/customers/{id}/activate` | ADMIN/STAFF | Activate customer |
| PUT | `/api/v1/organizations/{orgId}/customers/{id}/deactivate` | ADMIN/STAFF | Deactivate customer |
| PUT | `/api/v1/organizations/{orgId}/customers/{id}/automation/enable` | ADMIN/STAFF | Enable automated reminders for customer |
| PUT | `/api/v1/organizations/{orgId}/customers/{id}/automation/disable` | ADMIN/STAFF | Disable automated reminders for customer |
| DELETE | `/api/v1/organizations/{orgId}/customers/{id}` | ADMIN/STAFF | Delete customer |
| POST | `/api/v1/organizations/{orgId}/invoices` | ADMIN/STAFF | Create draft invoice |
| GET | `/api/v1/organizations/{orgId}/invoices` | ADMIN/STAFF | List invoices |
| GET | `/api/v1/organizations/{orgId}/invoices/{invoiceId}` | ADMIN/STAFF | Get invoice |
| PATCH | `/api/v1/organizations/{orgId}/invoices/{invoiceId}` | ADMIN/STAFF | Update draft invoice |
| DELETE | `/api/v1/organizations/{orgId}/invoices/{invoiceId}` | ADMIN/STAFF | Delete draft invoice |
| POST | `/api/v1/organizations/{orgId}/invoices/{invoiceId}/issue` | ADMIN/STAFF | Issue invoice |
| GET | `/api/v1/organizations/{orgId}/invoices/{invoiceId}/pdf` | ADMIN/STAFF | Download PDF |
| POST | `/api/v1/organizations/{orgId}/invoices/{invoiceId}/payments` | ADMIN/STAFF | Record payment |
| GET | `/api/v1/organizations/{orgId}/invoices/{invoiceId}/payments` | ADMIN/STAFF | List payments |
| GET | `/api/v1/organizations/{orgId}/invoices/{invoiceId}/payments/{paymentId}` | ADMIN/STAFF | Get payment |
| PATCH | `/api/v1/organizations/{orgId}/invoices/{invoiceId}/payments/{paymentId}` | ADMIN/STAFF | Update payment |
| DELETE | `/api/v1/organizations/{orgId}/invoices/{invoiceId}/payments/{paymentId}` | ADMIN/STAFF | Delete payment |
| POST | `/api/v1/organizations/{orgId}/invoices/{invoiceId}/followups` | ADMIN/STAFF | Create follow-up |
| POST | `/api/v1/organizations/{orgId}/invoices/{invoiceId}/followups/dispatch` | ADMIN/STAFF | Dispatch multi-channel |
| GET | `/api/v1/organizations/{orgId}/invoices/{invoiceId}/followups` | ADMIN/STAFF | List follow-ups |
| GET | `/api/v1/organizations/{orgId}/invoices/{invoiceId}/followups/{followUpId}` | ADMIN/STAFF | Get follow-up |
| PATCH | `/api/v1/organizations/{orgId}/invoices/{invoiceId}/followups/{followUpId}` | ADMIN/STAFF | Update follow-up |
| PATCH | `/api/v1/organizations/{orgId}/invoices/{invoiceId}/followups/{followUpId}/send` | ADMIN/STAFF | Mark sent |
| PATCH | `/api/v1/organizations/{orgId}/invoices/{invoiceId}/followups/{followUpId}/fail` | ADMIN/STAFF | Mark failed |
| DELETE | `/api/v1/organizations/{orgId}/invoices/{invoiceId}/followups/{followUpId}` | ADMIN/STAFF | Delete follow-up |
| POST | `/api/v1/organizations/{orgId}/templates` | ADMIN/STAFF | Create template |
| GET | `/api/v1/organizations/{orgId}/templates` | ADMIN/STAFF | List templates |
| GET | `/api/v1/organizations/{orgId}/templates/{id}` | ADMIN/STAFF | Get template |
| PATCH | `/api/v1/organizations/{orgId}/templates/{id}` | ADMIN/STAFF | Update template |
| PATCH | `/api/v1/organizations/{orgId}/templates/{id}/activate` | ADMIN/STAFF | Activate template |
| PATCH | `/api/v1/organizations/{orgId}/templates/{id}/deactivate` | ADMIN/STAFF | Deactivate template |
| DELETE | `/api/v1/organizations/{orgId}/templates/{id}` | ADMIN/STAFF | Delete template |
| POST | `/api/v1/organizations/{orgId}/reminder-rules` | ADMIN/STAFF | Create reminder rule |
| GET | `/api/v1/organizations/{orgId}/reminder-rules` | ADMIN/STAFF | List reminder rules |
| GET | `/api/v1/organizations/{orgId}/reminder-rules/{ruleId}` | ADMIN/STAFF | Get reminder rule |
| PATCH | `/api/v1/organizations/{orgId}/reminder-rules/{ruleId}` | ADMIN/STAFF | Update reminder rule |
| PATCH | `/api/v1/organizations/{orgId}/reminder-rules/{ruleId}/activate` | ADMIN/STAFF | Activate rule |
| PATCH | `/api/v1/organizations/{orgId}/reminder-rules/{ruleId}/deactivate` | ADMIN/STAFF | Deactivate rule |
| DELETE | `/api/v1/organizations/{orgId}/reminder-rules/{ruleId}` | ADMIN/STAFF | Delete reminder rule |
| GET | `/api/v1/organizations/{orgId}/gateways` | ADMIN | List gateway connections |
| GET | `/api/v1/organizations/{orgId}/gateways/stripe/authorize-url` | ADMIN | Stripe OAuth URL |
| POST | `/api/v1/organizations/{orgId}/gateways/stripe/connect` | ADMIN | Complete Stripe connect |
| DELETE | `/api/v1/organizations/{orgId}/gateways/stripe` | ADMIN | Disconnect Stripe |
| POST | `/api/v1/organizations/{orgId}/gateways/razorpay/connect` | ADMIN | Connect Razorpay |
| DELETE | `/api/v1/organizations/{orgId}/gateways/razorpay` | ADMIN | Disconnect Razorpay |
| GET | `/api/v1/public/confirmations/{token}` | None | Get confirmation view |
| POST | `/api/v1/public/confirmations/{token}` | None | Submit payment claim |
| GET | `/api/v1/organizations/{orgId}/payment-confirmations` | ADMIN/STAFF | List confirmations |
| GET | `/api/v1/organizations/{orgId}/payment-confirmations/{id}` | ADMIN/STAFF | Get confirmation |
| POST | `/api/v1/organizations/{orgId}/payment-confirmations/{id}/approve` | ADMIN/STAFF | Approve claim |
| POST | `/api/v1/organizations/{orgId}/payment-confirmations/{id}/reject` | ADMIN/STAFF | Reject claim |
| GET | `/pay/{token}` | None | Payment link redirect |
| POST | `/api/v1/webhooks/stripe` | None (HMAC) | Stripe webhook |
| POST | `/api/v1/webhooks/razorpay` | None (HMAC) | Razorpay webhook |
| POST | `/api/v1/organizations/{orgId}/ai/templates/generate` | ADMIN/STAFF | AI: generate template from scratch |
| POST | `/api/v1/organizations/{orgId}/ai/templates/enhance` | ADMIN/STAFF | AI: rewrite template to target tone |
| GET | `/api/v1/organizations/{orgId}/ai/insights/overview` | ADMIN/STAFF | AI: org payment health overview |
| GET | `/api/v1/organizations/{orgId}/ai/insights/customers/{customerId}` | ADMIN/STAFF | AI: customer payment intelligence |
| POST | `/api/v1/organizations/{orgId}/ai/insights/ask` | ADMIN/STAFF | AI: ask any question with optional filters |
| POST | `/api/v1/diagnostics/test-email` | None | Send test email |
| POST | `/api/v1/diagnostics/test-sms` | None | Send test SMS |
