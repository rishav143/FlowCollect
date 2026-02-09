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

## Delete (Archive) Organization
`DELETE /api/v1/organizations/{organizationId}`

### Behavior
Soft delete: sets status to `ARCHIVED` and `deletedAt`.

### Responses
- `204 No Content`
- `400` invalid id
- `404` not found
- `409` already archived

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
