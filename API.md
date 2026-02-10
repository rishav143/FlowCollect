# Cashclarity REST API

Public REST API reference for Cashclarity (Invoice Management + Customer Follow-up).

## Base URL
- Local: `http://localhost:8080`
- Prefix: `/api/v1`

## Authentication
Currently using HTTP Basic (Spring Security default).
```
Username: admin
Password: admin123
```
Example:
```bash
curl -i -u admin:admin123 "http://localhost:8080/api/v1/organizations"
```

## Common Response Shapes

### Success (single resource)
```json
{
  "id": 1,
  "name": "Acme Corporation",
  "email": "contact@acme.com",
  "phone": "+1-555-0100",
  "address": "123 Main Street",
  "timezone": "America/New_York",
  "currency": "USD",
  "logoUrl": null,
  "status": "ACTIVE",
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

### Validation error
```json
{
  "message": "Validation failed.",
  "errors": {
    "email": "Email must be a valid email address."
  }
}
```

### Business error
```json
{
  "message": "Organization not found for id: '123'."
}
```

## Pagination
Pagination uses Spring `Page` format.

Request:
- `page`: 0-based page index
- `size`: page size (1–100)
- `sort`: `field,asc|desc` (repeatable)

Response:
```json
{
  "content": [ ... ],
  "pageable": { ... },
  "totalElements": 42,
  "totalPages": 5,
  "number": 0,
  "size": 10,
  "first": true,
  "last": false
}
```

---

# Organization APIs

## Create Organization
`POST /api/v1/organizations`

### Request body
```json
{
  "name": "Acme Corporation",
  "email": "contact@acme.com",
  "phone": "+1-555-0100",
  "address": "123 Main Street",
  "currency": "USD",
  "timezone": "America/New_York"
}
```

### Responses
- `201 Created` + `Location` header
- `400 Bad Request` for invalid fields
- `409 Conflict` if email already exists

### cURL
```bash
curl -i -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "name":"Acme Corporation",
    "email":"contact@acme.com",
    "currency":"USD",
    "timezone":"America/New_York"
  }' \
  "http://localhost:8080/api/v1/organizations"
```

---

## List Organizations (Filters + Pagination)
`GET /api/v1/organizations`

### Query params
- `status` (e.g., `ACTIVE`, `SUSPENDED`, `ARCHIVED`)
- `email` (substring match)
- `name` (substring match)
- `createdFrom` (ISO-8601 date)
- `createdTo` (ISO-8601 date)
- `page`, `size`, `sort`

### cURL
```bash
curl -i -u admin:admin123 \
  "http://localhost:8080/api/v1/organizations?status=ACTIVE&email=contact&name=Acme&createdFrom=2024-01-01&createdTo=2024-12-31&page=0&size=10&sort=createdAt,desc"
```

### Errors
- `400` invalid status
- `400` invalid date range (`createdFrom > createdTo`)
- `400` invalid pagination (`size` not 1–100, negative `page`)
- `400` invalid sort field

---

## Get Organization by ID
`GET /api/v1/organizations/{organizationId}`

### Responses
- `200 OK`
- `400` invalid id (null/<=0)
- `404` not found

### cURL
```bash
curl -i -u admin:admin123 \
  "http://localhost:8080/api/v1/organizations/2"
```

---

## Update Organization
`PATCH /api/v1/organizations/{organizationId}`

### Request body (partial)
```json
{
  "name": "Updated Org",
  "email": "updated@acme.com",
  "currency": "GBP",
  "timezone": "Europe/London"
}
```

### Responses
- `200 OK`
- `400` invalid id or invalid field
- `404` not found
- `409` duplicate email

---

## Delete Organization (Hard Delete)
`DELETE /api/v1/organizations/{organizationId}`

### Behavior
Permanently removes the organization from the database.

### Responses
- `204 No Content`
- `400` invalid id
- `404` not found

### cURL
```bash
curl -i -u admin:admin123 \
  -X DELETE "http://localhost:8080/api/v1/organizations/2"
```

---

# Notes
- All timestamps are ISO-8601 UTC.
- `currency` expects ISO-4217 code (e.g., `USD`, `EUR`).
- `timezone` expects IANA Zone ID (e.g., `America/New_York`).

---

# Invoice API Modeling Structure

This section proposes a production-ready REST modeling structure for invoice management.

## Invoice Resource Shape

```json
{
  "id": 12045,
  "organizationId": 1,
  "invoiceNumber": "INV-2026-000345",
  "status": "SENT",
  "issueDate": "2026-02-01",
  "dueDate": "2026-02-15",
  "currency": "USD",
  "paymentTermsDays": 14,
  "customer": {
    "id": 230,
    "name": "Acme Corporation",
    "email": "ap@acme.com",
    "billingAddress": "123 Main Street, NY"
  },
  "lineItems": [
    {
      "id": 1,
      "description": "Website maintenance",
      "quantity": 10,
      "unitPrice": 150.0,
      "taxRate": 0.1,
      "discountRate": 0.0,
      "lineSubtotal": 1500.0,
      "lineTaxAmount": 150.0,
      "lineTotal": 1650.0
    }
  ],
  "summary": {
    "subtotal": 1500.0,
    "taxAmount": 150.0,
    "discountAmount": 0.0,
    "totalAmount": 1650.0,
    "amountPaid": 500.0,
    "amountDue": 1150.0
  },
  "notes": "Net 14 payment terms",
  "metadata": {
    "purchaseOrderNumber": "PO-9852",
    "externalReference": "ERP-22291"
  },
  "sentAt": "2026-02-01T10:30:00Z",
  "lastReminderAt": "2026-02-10T08:00:00Z",
  "createdAt": "2026-02-01T09:15:00Z",
  "updatedAt": "2026-02-10T08:00:00Z"
}
```

## Invoice Lifecycle States

- `DRAFT`: created but not shared with customer.
- `SENT`: issued to customer; awaiting payment.
- `PARTIALLY_PAID`: one or more payments received.
- `PAID`: fully settled.
- `OVERDUE`: due date passed and still unpaid.
- `VOID`: canceled invoice, no longer payable.

> Recommended rule: status is system-driven for `OVERDUE`, `PARTIALLY_PAID`, and `PAID` based on due date and payment ledger.

## Endpoints

### Create Invoice
`POST /api/v1/invoices`

Request body (minimal):
```json
{
  "organizationId": 1,
  "customerId": 230,
  "issueDate": "2026-02-01",
  "dueDate": "2026-02-15",
  "currency": "USD",
  "lineItems": [
    {
      "description": "Website maintenance",
      "quantity": 10,
      "unitPrice": 150.0,
      "taxRate": 0.1
    }
  ],
  "notes": "Net 14 payment terms"
}
```

Response:
- `201 Created` with invoice payload.

### List Invoices
`GET /api/v1/invoices`

Query params:
- `organizationId` (required for tenant isolation)
- `customerId`
- `status`
- `invoiceNumber`
- `issueDateFrom`, `issueDateTo`
- `dueDateFrom`, `dueDateTo`
- `minAmount`, `maxAmount`
- `page`, `size`, `sort`

### Get Invoice by ID
`GET /api/v1/invoices/{invoiceId}`

### Update Invoice
`PATCH /api/v1/invoices/{invoiceId}`

Allowed while in `DRAFT` (recommended constraint):
- `dueDate`, `notes`, `lineItems`, `metadata`, `customerId`

### Delete Invoice
`DELETE /api/v1/invoices/{invoiceId}`

Recommended behavior:
- Hard delete only in `DRAFT`.
- Otherwise return `409 Conflict` and suggest `VOID` action.

### Send Invoice
`POST /api/v1/invoices/{invoiceId}/send`

Transitions:
- `DRAFT -> SENT`

### Void Invoice
`POST /api/v1/invoices/{invoiceId}/void`

Transitions:
- `DRAFT|SENT|OVERDUE -> VOID`
- reject if payments exist (`409 Conflict`) unless refund workflow is completed.

### Record Payment
`POST /api/v1/invoices/{invoiceId}/payments`

Request body:
```json
{
  "amount": 500.0,
  "paymentDate": "2026-02-05",
  "paymentMethod": "BANK_TRANSFER",
  "reference": "TRX-66372",
  "notes": "Partial payment"
}
```

Status updates:
- if `amountPaid == totalAmount` => `PAID`
- if `0 < amountPaid < totalAmount` => `PARTIALLY_PAID`

### List Payments for Invoice
`GET /api/v1/invoices/{invoiceId}/payments`

### Send Payment Reminder
`POST /api/v1/invoices/{invoiceId}/reminders`

Suggested body:
```json
{
  "channel": "EMAIL",
  "template": "OVERDUE_REMINDER_1"
}
```

## Supporting Resources

- `GET /api/v1/invoices/{invoiceId}/pdf` (download/render PDF)
- `GET /api/v1/invoices/{invoiceId}/events` (audit timeline)
- `POST /api/v1/invoices/{invoiceId}/attachments`

## Validation Rules

- `organizationId`, `customerId`, `issueDate`, `dueDate`, `currency`, and at least one `lineItem` are required.
- `dueDate >= issueDate`.
- `lineItems[].quantity > 0`.
- `lineItems[].unitPrice >= 0`.
- `0 <= taxRate <= 1` and `0 <= discountRate <= 1`.
- `currency` must match organization currency policy (single-currency or multi-currency).
- Cannot edit financial fields when invoice status is `PAID` or `VOID`.

## Error Model (Suggested)

```json
{
  "message": "Validation failed.",
  "code": "VALIDATION_ERROR",
  "errors": {
    "dueDate": "dueDate must be greater than or equal to issueDate"
  },
  "traceId": "e36d3fd1124e"
}
```

## Example cURL Sequence

Create invoice:
```bash
curl -i -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "organizationId":1,
    "customerId":230,
    "issueDate":"2026-02-01",
    "dueDate":"2026-02-15",
    "currency":"USD",
    "lineItems":[{"description":"Website maintenance","quantity":10,"unitPrice":150.0,"taxRate":0.1}]
  }' \
  "http://localhost:8080/api/v1/invoices"
```

Record payment:
```bash
curl -i -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "amount":500.0,
    "paymentDate":"2026-02-05",
    "paymentMethod":"BANK_TRANSFER",
    "reference":"TRX-66372"
  }' \
  "http://localhost:8080/api/v1/invoices/12045/payments"
```
