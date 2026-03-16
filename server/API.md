# FlowCollect API Documentation

This document provides a comprehensive description of the PaidPeace API endpoints, following the design hierarchy:
**Organization -> User, Customer -> Invoice -> (InvoiceItem, Payment, FollowUp), Template, ReminderRule.**

## Base URL
`http://localhost:8080` (Default)

## Authentication
Most endpoints require a Bearer token in the `Authorization` header.
- **Header**: `Authorization: Bearer <token>`
- **Multi-tenancy**: Most organization-scoped endpoints use `{organizationId}` (UUID) in the path. Payment and follow-up endpoints are currently nested under `{invoiceId}`.

---

## 1. Authentication
Endpoints for registration, logging in, and obtaining a JWT.

### Register
- **Endpoint**: `POST /api/v1/auth/register`
- **Body**:
  - `organizationName` (String, Required): Name of the new organization (max 100).
  - `currency` (String, Required): 3-letter ISO code (e.g., USD, INR).
  - `timezone` (String, Required): Timezone name (e.g., America/New_York).
  - `ownerName` (String, Required): Full name of the organization owner (max 100).
  - `email` (String, Required): Owner's email address (max 100).
  - `password` (String, Required): Owner's password (8-100 characters).
- **Description**: Registers a new organization and its first administrative user. Returns a JWT token upon successful registration.
- **Response**: `200 OK` with `LoginResponse` containing the JWT `token`, `expiresAt`, and user details (`id`, `name`, `email`, `role`, `status`).

### Login
- **Endpoint**: `POST /api/v1/auth/login`
- **Body**:
  - `organizationId` (UUID, Required): ID of the organization the user belongs to.
  - `email` (String, Required): User's email address.
  - `password` (String, Required): User's password.
- **Description**: Authenticates a user and returns a JWT token along with user details. Use this token in the `Authorization` header for subsequent requests.
- **Response**: `200 OK` with `LoginResponse` containing the JWT `token`, `expiresAt`, and user details (`id`, `name`, `email`, `role`, `status`).

---

## 2. Organization (Root)
Manage organizations. Admin-only for global management.

### Create Organization (Admin Only)
- **Endpoint**: `POST /api/v1/organizations`
- **Body**:
  - `name` (String, Required): Name of the organization (max 100).
  - `email` (String, Required): Contact email for the organization.
  - `currency` (String, Required): 3-letter ISO code (e.g., USD, INR).
  - `timezone` (String, Required): Timezone name (e.g., America/New_York).
  - `phone` (String, Optional): Contact phone number.
  - `address` (String, Optional): Physical address.
- **Description**: Creates a new organization. Requires `ADMIN` role.
- **Response**: `201 Created` with `OrganizationResponse` (full organization details).

### List Organizations (Admin Only)
- **Endpoint**: `GET /api/v1/organizations`
- **Query Params**: `status`, `email`, `name`, `createdFrom`, `createdTo`, `page`, `size`, `sort`
- **Description**: Returns a paginated list of organizations.
- **Response**: `200 OK` with `Page` of `OrganizationResponse` objects.

### Get Organization (Admin Only)
- **Endpoint**: `GET /api/v1/organizations/{organizationId}`
- **Description**: Retrieves details of a specific organization.
- **Response**: `200 OK` with `OrganizationResponse`.

### Update Organization (Admin Only)
- **Endpoint**: `PATCH /api/v1/organizations/{organizationId}`
- **Body** (All Optional):
  - `name` (String)
  - `email` (String)
  - `phone` (String)
  - `address` (String)
  - `logoUrl` (String)
  - `currency` (String)
  - `timezone` (String)
- **Description**: Partially updates organization details.
- **Response**: `200 OK` with updated `OrganizationResponse`.

### Delete Organization (Admin Only)
- **Endpoint**: `DELETE /api/v1/organizations/{organizationId}`
- **Description**: Permanently removes the organization and all its data.
- **Response**: `204 No Content`.

### Organization State Transitions (Admin Only)
- **Activate**: `POST /api/v1/organizations/{organizationId}/activate`
- **Suspend**: `POST /api/v1/organizations/{organizationId}/suspend`
- **Archive**: `POST /api/v1/organizations/{organizationId}/archive`
- **Description**: Transitions the organization through different lifecycle states. Requires `ADMIN` role.
- **Response**: `200 OK` with updated `OrganizationResponse`.

---

## 2.1 User (Hierarchy: Organization -> User)
Manage users within an organization.

### Create User (Admin Only)
- **Endpoint**: `POST /api/v1/organizations/{organizationId}/users`
- **Body**:
  - `name` (String, Required): Full name of the user (max 100).
  - `email` (String, Required): Valid email address.
  - `password` (String, Required): Minimum 8 characters.
  - `role` (String, Required): Either `ADMIN` or `STAFF`.
- **Description**: Creates a new user within the specified organization.
- **Response**: `201 Created` with `UserResponse`.

### List Users
- **Endpoint**: `GET /api/v1/organizations/{organizationId}/users`
- **Query Params**: `status`, `email`, `name`, `role`, `page`, `size`, `sort`
- **Description**: Returns a paginated list of users in the organization.
- **Response**: `200 OK` with `Page` of `UserResponse` objects.

### Get User
- **Endpoint**: `GET /api/v1/organizations/{organizationId}/users/{userId}`
- **Description**: Retrieves details of a specific user.
- **Response**: `200 OK` with `UserResponse`.

### Update User (Admin Only)
- **Endpoint**: `PATCH /api/v1/organizations/{organizationId}/users/{userId}`
- **Body** (All Optional):
  - `name` (String)
  - `email` (String)
  - `role` (String: `ADMIN` or `STAFF`)
- **Description**: Updates user details.
- **Response**: `200 OK` with updated `UserResponse`.

### User Password Change
- **Endpoint**: `POST /api/v1/organizations/{organizationId}/users/{userId}/password`
- **Body**:
  - `oldPassword` (String, Optional)
  - `newPassword` (String, Required)
- **Description**: Updates user's password. Accessible to `ADMIN` and `STAFF`.
- **Response**: `204 No Content`.

### User State Transitions (Admin Only)
- **Activate**: `POST /api/v1/organizations/{organizationId}/users/{userId}/activate`
- **Deactivate**: `POST /api/v1/organizations/{organizationId}/users/{userId}/deactivate`
- **Description**: Changes the active status of a user.
- **Response**: `200 OK` with updated `UserResponse`.

### Delete User (Admin Only)
- **Endpoint**: `DELETE /api/v1/organizations/{organizationId}/users/{userId}`
- **Description**: Permanently removes a user from the organization.
- **Response**: `204 No Content`.

---

## 2.2 Customer (Hierarchy: Organization -> Customer)
Manage customers belonging to an organization. Requires `ADMIN` or `STAFF` role.

### Create Customer
- **Endpoint**: `POST /api/v1/organizations/{organizationId}/customers`
- **Body**:
  - `name` (String, Required): Full name of the customer.
  - `email` (String, Optional): Contact email.
  - `phone` (String, Optional): Contact phone.
  - `address` (String, Optional): Physical address.
  - `companyName` (String, Optional): Customer's company name.
- **Description**: Creates a new customer record.
- **Response**: `200 OK` with `CustomerResponse`.

### List Customers
- **Endpoint**: `GET /api/v1/organizations/{organizationId}/customers`
- **Query Params**: `name`, `email`, `phone`, `companyName`, `active`, `page`, `size`, `sort`
- **Description**: Lists customers with optional filtering.
- **Response**: `200 OK` with `Page` of `CustomerResponse` objects.

### Get Customer
- **Endpoint**: `GET /api/v1/organizations/{organizationId}/customers/{id}`
- **Description**: Retrieves details of a specific customer.
- **Response**: `200 OK` with `CustomerResponse`.

### Update Customer
- **Endpoint**: `PUT /api/v1/organizations/{organizationId}/customers/{id}`
- **Body** (All Optional):
  - `name` (String)
  - `email` (String)
  - `phone` (String)
  - `address` (String)
  - `companyName` (String)
- **Description**: Performs a full update of customer details.
- **Response**: `200 OK` with updated `CustomerResponse`.

### Customer State Transitions
- **Activate**: `PUT /api/v1/organizations/{organizationId}/customers/{id}/activate`
- **Deactivate**: `PUT /api/v1/organizations/{organizationId}/customers/{id}/deactivate`
- **Description**: Toggles the active status of a customer.
- **Response**: `200 OK` with updated `CustomerResponse`.

### Delete Customer
- **Endpoint**: `DELETE /api/v1/organizations/{organizationId}/customers/{id}`
- **Description**: Permanently removes the customer.
- **Response**: `204 No Content`.

---

## 2.2.1 Invoice (Hierarchy: Customer -> Invoice)
Manage invoices issued to customers. Requires `ADMIN` or `STAFF` role.

### Create Draft Invoice
- **Endpoint**: `POST /api/v1/organizations/{organizationId}/invoices`
- **Body**:
  - `createdByUserId` (UUID, Optional): User who created the invoice.
  - `customerId` (UUID, Required): The customer this invoice belongs to.
  - `invoiceNumber` (String, Required): Unique number for the invoice (max 100).
  - `items` (Array, Required): List of **InvoiceItems** (see below).
  - `issueDate` (String, Optional): YYYY-MM-DD.
  - `dueDate` (String, Optional): YYYY-MM-DD.
  - `taxPercentage` (Decimal, Optional): Default is 0.
- **Description**: Creates a new invoice in `DRAFT` status.
- **Response**: `201 Created` with `InvoiceResponse` (includes `items` with calculated amounts).

#### 2.2.1.1 InvoiceItem (Part of Invoice)
Invoice items are submitted as part of the invoice body.
- **Body structure (within `items` array)**:
  - `description` (String, Required)
  - `quantity` (Integer, Required, Positive)
  - `unitPrice` (Decimal, Required, Non-negative)

### List Invoices
- **Endpoint**: `GET /api/v1/organizations/{organizationId}/invoices`
- **Query Params**: `timeStatus`, `lifeCycleStatus`, `invoiceNumber`, `createdAt`, `updatedAt`, `dueDate`, `page`, `size`, `sort`
- **Description**: Returns a paginated list of invoices with filters.
- **Response**: `200 OK` with `Page` of `InvoiceResponse` objects.

### Get Invoice
- **Endpoint**: `GET /api/v1/organizations/{organizationId}/invoices/{invoiceId}`
- **Description**: Retrieves full details of an invoice, including its items, payments, and follow-ups.
- **Response**: `200 OK` with `InvoiceResponse`.

### Update Draft Invoice
- **Endpoint**: `PATCH /api/v1/organizations/{organizationId}/invoices/{invoiceId}`
- **Body** (All Optional):
  - `createdByUserId` (UUID)
  - `customerId` (UUID)
  - `invoiceNumber` (String)
  - `items` (Array of InvoiceItems)
  - `dueDate` (String)
  - `taxPercentage` (Decimal)
- **Description**: Updates fields of a draft invoice. Only allowed in `DRAFT` status.
- **Response**: `200 OK` with updated `InvoiceResponse`.

### Issue Invoice
- **Endpoint**: `POST /api/v1/organizations/{organizationId}/invoices/{invoiceId}/issue`
- **Body** (Optional):
  - `issueDate` (String, YYYY-MM-DD): Date to be applied as issue date.
- **Description**: Transitions an invoice from `DRAFT` to `ISSUED`. This marks it as ready for follow-up and payment.
- **Response**: `200 OK` with updated `InvoiceResponse` (status changed to `ISSUED`).

### Download PDF
- **Endpoint**: `GET /api/v1/organizations/{organizationId}/invoices/{invoiceId}/pdf`
- **Produces**: `application/pdf`
- **Description**: Generates and downloads the PDF version of the invoice.
- **Response**: `200 OK` with binary data (`application/pdf`).

### Delete Invoice
- **Endpoint**: `DELETE /api/v1/organizations/{organizationId}/invoices/{invoiceId}`
- **Description**: Deletes an invoice. Only allowed in `DRAFT` status.
- **Response**: `204 No Content`.

---

## 2.2.1.2 Payment (Hierarchy: Invoice -> Payment)
Record and manage payments against an invoice. Requires `ADMIN` or `STAFF` role.

### Record Payment
- **Endpoint**: `POST /api/v1/invoices/{invoiceId}/payments`
- **Body**:
  - `amount` (Decimal, Required): Positive amount paid.
  - `mode` (String, Required): One of `CASH`, `UPI`, `BANK_TRANSFER`, `CARD`, `CHEQUE`.
  - `referenceId` (String, Optional): Transaction reference ID.
  - `notes` (String, Optional): Internal notes about the payment.
- **Description**: Records a payment against an issued invoice. Automatically updates the invoice payment status.
- **Response**: `200 OK` with `PaymentResponse`.

### List Payments
- **Endpoint**: `GET /api/v1/invoices/{invoiceId}/payments`
- **Query Params**: `mode`, `page`, `size`, `sort`
- **Description**: Lists all payments recorded for a specific invoice.
- **Response**: `200 OK` with `Page` of `PaymentResponse` objects.

### Get Payment
- **Endpoint**: `GET /api/v1/invoices/{invoiceId}/payments/{paymentId}`
- **Description**: Retrieves details of a specific payment.
- **Response**: `200 OK` with `PaymentResponse`.

### Update Payment
- **Endpoint**: `PATCH /api/v1/invoices/{invoiceId}/payments/{paymentId}`
- **Body** (All Optional): Same fields as Record Payment.
- **Description**: Updates details of a recorded payment.
- **Response**: `200 OK` with updated `PaymentResponse`.

---

## 2.2.1.3 FollowUp (Hierarchy: Invoice -> FollowUp)
Manual and automated follow-ups for an invoice. Requires `ADMIN` or `STAFF` role.

### Create Follow-up
- **Endpoint**: `POST /api/v1/invoices/{invoiceId}/followups`
- **Body**:
  - `channel` (String, Required): `EMAIL`, `SMS`, or `WHATSAPP`.
  - `triggerType` (String, Optional): `MANUAL` or `AUTOMATED`. Defaults to `MANUAL` when omitted.
  - `templateId` (UUID, Optional): The template to use for the message.
  - `scheduledForDate` (String, Optional): Scheduled date in `YYYY-MM-DD` format.
  - `attachPdf` (Boolean, Optional): Whether to attach the invoice PDF.
- **Description**: Creates a follow-up record for an invoice.
- **Response**: `200 OK` with `FollowUpResponse` (see field list below).

#### FollowUpResponse fields
| Field | Type | Notes |
|---|---|---|
| `id` | UUID | |
| `invoiceId` | UUID | |
| `channel` | String | `EMAIL`, `SMS`, or `WHATSAPP` |
| `triggerType` | String | `MANUAL` or `AUTOMATED` |
| `status` | String | Current delivery status |
| `templateId` | UUID | nullable |
| `reminderRuleId` | UUID | nullable — set when triggered by a reminder rule |
| `scheduledForDate` | String | `YYYY-MM-DD`, nullable |
| `sentAt` | Instant | nullable |
| `createdAt` | Instant | |
| `paymentLinkId` | UUID | nullable — present when a payment link was attached |
| `paymentLinkUrl` | String | nullable — public short URL for the customer |
| `paymentLinkGateway` | String | nullable — `STRIPE` or `RAZORPAY` |
| `paymentLinkStatus` | String | nullable — `ACTIVE`, `PAID`, `EXPIRED`, etc. |

### Dispatch Multi-channel Follow-up
- **Endpoint**: `POST /api/v1/invoices/{invoiceId}/followups/dispatch`
- **Body**:
  - `channels` (Array of String, Required): List of channels (e.g., `["EMAIL", "SMS"]`).
  - `templateId` (UUID, Optional): Template to use.
  - `scheduledForDate` (String, Optional): Scheduled date in `YYYY-MM-DD` format.
  - `attachPdf` (Boolean, Optional): Whether to attach the invoice PDF (Email only).
  - `includePaymentLink` (Boolean, Optional, default: `false`): When `true`, generates a payment link and embeds it via the `{{paymentLink}}` placeholder in the template body.
  - `paymentGateway` (String, Optional): `STRIPE` or `RAZORPAY`. Required when `includePaymentLink` is `true`. The organization must have an active connection for the chosen gateway.
- **Description**: Immediately creates and sends follow-ups across multiple specified channels.
- **Response**: `200 OK` with `List` of `FollowUpResponse` objects (one per channel).

### List Follow-ups
- **Endpoint**: `GET /api/v1/invoices/{invoiceId}/followups`
- **Query Params**: `status`, `triggerType`, `channel`, `page`, `size`, `sort`
- **Description**: Returns a paginated list of follow-ups for the invoice.
- **Response**: `200 OK` with `Page` of `FollowUpResponse` objects.

### Get Follow-up
- **Endpoint**: `GET /api/v1/invoices/{invoiceId}/followups/{followUpId}`
- **Description**: Retrieves details of a specific follow-up.
- **Response**: `200 OK` with `FollowUpResponse`.

### Update Follow-up
- **Endpoint**: `PATCH /api/v1/invoices/{invoiceId}/followups/{followUpId}`
- **Body** (All Optional): Same fields as Create Follow-up.
- **Description**: Updates a follow-up record.
- **Response**: `200 OK` with updated `FollowUpResponse`.

### Follow-up Actions
- **Send**: `PATCH /api/v1/invoices/{invoiceId}/followups/{followUpId}/send`
- **Fail**: `PATCH /api/v1/invoices/{invoiceId}/followups/{followUpId}/fail`
- **Description**: Manually marks a follow-up as sent or failed.
- **Response**: `200 OK` with updated `FollowUpResponse`.

---

## 2.3 Template (Hierarchy: Organization -> Template)
Messaging templates for follow-ups. Requires `ADMIN` or `STAFF` role.

### Create Template
- **Endpoint**: `POST /api/v1/organizations/{organizationId}/templates`
- **Body**:
  - `name` (String, Required): Unique name for the template.
  - `channel` (String, Optional): `EMAIL`, `SMS`, or `WHATSAPP`.
  - `subject` (String, Optional): Subject line.
  - `body` (String, Optional): The message content with placeholders (e.g., `{{customerName}}`).
  - `tone` (String, Optional): `POLITE`, `NEUTRAL`, or `FIRM`.
- **Description**: Creates a new messaging template for the organization.
- **Response**: `200 OK` with `TemplateResponse`.

### List Templates
- **Endpoint**: `GET /api/v1/organizations/{organizationId}/templates`
- **Query Params**: `name`, `channel`, `tone`, `page`, `size`, `sort`
- **Description**: Lists templates with filtering options.
- **Response**: `200 OK` with `Page` of `TemplateResponse` objects.

### Get Template
- **Endpoint**: `GET /api/v1/organizations/{organizationId}/templates/{id}`
- **Description**: Retrieves template details.
- **Response**: `200 OK` with `TemplateResponse`.

### Update Template
- **Endpoint**: `PATCH /api/v1/organizations/{organizationId}/templates/{id}`
- **Body** (All Optional): Same fields as Create Template.
- **Description**: Updates template details.
- **Response**: `200 OK` with updated `TemplateResponse`.

### Template State Transitions
- **Activate**: `PATCH /api/v1/organizations/{organizationId}/templates/{id}/activate`
- **Deactivate**: `PATCH /api/v1/organizations/{organizationId}/templates/{id}/deactivate`
- **Description**: Toggles whether a template is available for use.
- **Response**: `200 OK` with updated `TemplateResponse`.

### Delete Template
- **Endpoint**: `DELETE /api/v1/organizations/{organizationId}/templates/{id}`
- **Description**: Permanently removes the template.
- **Response**: `204 No Content`.

---

## 2.4 Reminder Rule (Hierarchy: Organization -> ReminderRule)
Rules for automated payment reminders. Requires `ADMIN` or `STAFF` role.

### Create Reminder Rule
- **Endpoint**: `POST /api/v1/organizations/{organizationId}/reminder-rules`
- **Body**:
  - `name` (String, Required): Rule name.
  - `daysOffset` (Integer, Required): Days relative to due date (e.g., -2 means 2 days before due).
  - `triggerType` (String, Required): `BEFORE_DUE_DATE`, `ON_DUE_DATE`, or `AFTER_DUE_DATE`.
  - `channel` (String, Required): `EMAIL`, `SMS`, or `WHATSAPP`.
  - `template` (Template, Required): Template object used for the automated reminder.
  - `active` (Boolean, Optional): Whether the rule starts active. Defaults to `false`.
  - `maxOccurrences` (Integer, Optional): Maximum number of times this rule fires per invoice. `null` means unlimited.
  - `cycleIntervalDays` (Integer, Optional): Days between repeat firings when recurring. Used together with `maxOccurrences` to configure repeating reminders.
  - `startDate` (String, Optional): Date from which the rule begins applying (`YYYY-MM-DD`). Invoices before this date are ignored.
- **Description**: Configures an automated reminder rule. Supports one-shot and recurring (cycled) reminders.
- **Response**: `200 OK` with `ReminderRuleResponse` (`id`, `name`, `daysOffset`, `triggerType`, `channel`, `template`, `active`, `maxOccurrences`, `cycleIntervalDays`, `startDate`, `createdAt`, `updatedAt`).

### List Reminder Rules
- **Endpoint**: `GET /api/v1/organizations/{organizationId}/reminder-rules`
- **Query Params**: `name`, `page`, `size`, `sort`
- **Description**: Lists all reminder rules in the organization.
- **Response**: `200 OK` with `Page` of `ReminderRuleResponse` objects.

### Get Reminder Rule
- **Endpoint**: `GET /api/v1/organizations/{organizationId}/reminder-rules/{reminderRuleId}`
- **Description**: Retrieves rule details.
- **Response**: `200 OK` with `ReminderRuleResponse`.

### Update Reminder Rule
- **Endpoint**: `PATCH /api/v1/organizations/{organizationId}/reminder-rules/{reminderRuleId}`
- **Body** (All Optional): Same fields as Create Reminder Rule, including `maxOccurrences`, `cycleIntervalDays`, and `startDate`. Omitted cycle fields retain their existing values.
- **Description**: Updates rule configuration.
- **Response**: `200 OK` with updated `ReminderRuleResponse`.

### Reminder Rule State Transitions
- **Activate**: `POST /api/v1/organizations/{organizationId}/reminder-rules/{reminderRuleId}/activate`
- **Deactivate**: `POST /api/v1/organizations/{organizationId}/reminder-rules/{reminderRuleId}/deactivate`
- **Description**: Toggles whether the rule is processed by the scheduler. Inactive rules are skipped entirely during the automated reminder job.
- **Response**: `204 No Content`.

### Delete Reminder Rule
- **Endpoint**: `DELETE /api/v1/organizations/{organizationId}/reminder-rules/{reminderRuleId}`
- **Description**: Permanently removes the reminder rule.
- **Response**: `204 No Content`.

---

## 2.5 Gateway Connections (Hierarchy: Organization -> Gateway)
Manage payment gateway integrations (Stripe, Razorpay) for an organization. All endpoints require `ADMIN` role.

### List Gateway Connections
- **Endpoint**: `GET /api/v1/organizations/{organizationId}/gateways`
- **Description**: Lists all configured gateway connections. Never exposes raw credentials — returns masked hints only.
- **Response**: `200 OK` with `List` of `GatewayConnectionResponse` (`id`, `gateway`, `status`, `accountHint`, `connectedAt`, `disconnectedAt`).

### Stripe Connect — Get Authorize URL
- **Endpoint**: `GET /api/v1/organizations/{organizationId}/gateways/stripe/authorize-url`
- **Description**: Returns the Stripe OAuth URL. The frontend should open or redirect to this URL to begin the Stripe Connect authorization flow.
- **Response**: `200 OK` with `{ "authorizeUrl": "<url>" }`.

### Stripe Connect — Complete Connection
- **Endpoint**: `POST /api/v1/organizations/{organizationId}/gateways/stripe/connect`
- **Query Params**: `code` (String, Required) — the authorization code returned by Stripe after user consent.
- **Description**: Exchanges the authorization code for a Stripe connected account ID and persists the connection. Called by the frontend after Stripe redirects back.
- **Response**: `200 OK` with `GatewayConnectionResponse`.

### Stripe Connect — Disconnect
- **Endpoint**: `DELETE /api/v1/organizations/{organizationId}/gateways/stripe`
- **Description**: Disconnects the Stripe account. Existing active payment links will stop working.
- **Response**: `200 OK` with `GatewayConnectionResponse`.

### Razorpay — Connect (Manual Key Entry)
- **Endpoint**: `POST /api/v1/organizations/{organizationId}/gateways/razorpay/connect`
- **Body**:
  - `keyId` (String, Required): Razorpay Key ID.
  - `keySecret` (String, Required): Razorpay Key Secret.
  - `webhookSecret` (String, Required): Razorpay webhook signing secret.
- **Description**: Saves encrypted Razorpay credentials. Keys are validated against the Razorpay API before storage. Idempotent — calling again with new keys updates the existing connection.
- **Response**: `200 OK` with `GatewayConnectionResponse`.

### Razorpay — Disconnect
- **Endpoint**: `DELETE /api/v1/organizations/{organizationId}/gateways/razorpay`
- **Description**: Disconnects Razorpay and clears all stored credentials.
- **Response**: `200 OK` with `GatewayConnectionResponse`.

---

## 3. Public & Webhook Endpoints
These endpoints are excluded from JWT authentication.

### Payment Link Redirect (Customer-Facing)
- **Endpoint**: `GET /pay/{token}`
- **Auth**: None — `token` is an unguessable UUID.
- **Description**: Looks up the payment link by token and performs a `302` redirect to the gateway checkout page. Returns `410 Gone` if the link has already been paid or has expired.
- **Response**: `302 Found` (redirect to gateway) or `410 Gone`.

### Stripe Webhook
- **Endpoint**: `POST /api/v1/webhooks/stripe`
- **Auth**: Verified via `Stripe-Signature` header (HMAC-SHA256). No JWT required.
- **Headers**: `Stripe-Signature` (String, Required).
- **Body**: Raw Stripe event payload.
- **Description**: Receives `checkout.session.completed` events from Stripe. Marks the associated payment link as paid and records the payment automatically.
- **Response**: `200 OK`.

### Razorpay Webhook
- **Endpoint**: `POST /api/v1/webhooks/razorpay`
- **Auth**: Verified via `X-Razorpay-Signature` header (HMAC-SHA256). No JWT required.
- **Headers**: `X-Razorpay-Signature` (String, Required).
- **Body**: Raw Razorpay event payload.
- **Description**: Receives `payment_link.paid` events from Razorpay. Marks the associated payment link as paid and records the payment automatically.
- **Response**: `200 OK`.

---

## 4. Background Schedulers
FlowCollect uses background workers to automate status management and reminders.

### Overdue Invoice Sync
- **Job Name**: `runInvoiceStatusSyncJob`
- **Schedule**: Hourly (`0 0 * * * *`)
- **Description**: Automatically checks all `ISSUED` invoices. If the current date is past the `dueDate`, the invoice's time status is updated to `OVERDUE`.

### Automated Reminder Engine
- **Job Name**: `runAutomatedReminderJob`
- **Schedule**: Every 15 minutes (`0 */15 * * * *`)
- **Description**: Scans all active `ReminderRules` across all organizations. If an invoice matches a rule's criteria (e.g., 2 days before due date), it automatically generates and dispatches a `FollowUp` using the associated `Template`. Respects `startDate`, `maxOccurrences`, and `cycleIntervalDays` when configured.

### Payment Link Expiry
- **Job Name**: `expireOverduePaymentLinks`
- **Schedule**: Daily at midnight (`0 0 0 * * *`)
- **Description**: Marks `ACTIVE` and `PARTIALLY_PAID` payment links as `EXPIRED` once their `expiresAt` timestamp has passed.
