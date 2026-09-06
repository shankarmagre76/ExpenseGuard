# ExpenseGuard Backend

ExpenseGuard is a enterprise-grade, portfolio-quality RESTful backend service built with Spring Boot 3, Java 21, PostgreSQL, and Spring Security with JWT authentication.

## Expense Management API Specification

### Authentication
All endpoints under `/api/v1/accounts`, `/api/v1/categories`, and `/api/v1/transactions` require a valid JWT bearer token in the `Authorization` header:

```http
Authorization: Bearer <JWT_TOKEN>
```

---

### Transactions Endpoints (`/api/v1/transactions`)

#### 1. Create Transaction
- **Method**: `POST /api/v1/transactions`
- **Status**: `201 Created`
- **Request Body**:
```json
{
  "accountId": "a1b2c3d4-e5f6-7890-abcd-1234567890ab",
  "categoryId": "f1e2d3c4-b5a6-7890-abcd-0987654321ba",
  "type": "EXPENSE",
  "amount": 450.50,
  "transactionDate": "2026-09-06",
  "description": "Groceries",
  "clientOperationId": "op-123456789"
}
```
- **Validation Rules**:
  - `accountId`: Required (UUID). Must belong to the authenticated user.
  - `categoryId`: Required (UUID). Must belong to the authenticated user.
  - `type`: Required (`EXPENSE` or `INCOME`). Must be compatible with the category type.
  - `amount`: Required (`BigDecimal > 0`).
  - `transactionDate`: Required (`YYYY-MM-DD`).
  - `description`: Optional (max 255 characters).
  - `clientOperationId`: Optional (max 100 characters). Enforces retry idempotency per user.

#### 2. Get Transactions (Paginated & Filtered)
- **Method**: `GET /api/v1/transactions`
- **Status**: `200 OK`
- **Query Parameters** (Optional):
  - `type`: `EXPENSE` | `INCOME`
  - `accountId`: `UUID`
  - `categoryId`: `UUID`
  - `fromDate`: `YYYY-MM-DD`
  - `toDate`: `YYYY-MM-DD`
  - `page`: Integer (default `0`)
  - `size`: Integer (default `20`, capped server-side at `100`)
- **Response**:
```json
{
  "content": [
    {
      "id": "11223344-5566-7788-9900-aabbccddeeff",
      "accountId": "a1b2c3d4-e5f6-7890-abcd-1234567890ab",
      "categoryId": "f1e2d3c4-b5a6-7890-abcd-0987654321ba",
      "categoryName": "Food",
      "type": "EXPENSE",
      "amount": 450.50,
      "transactionDate": "2026-09-06",
      "description": "Groceries",
      "clientOperationId": "op-123456789",
      "createdAt": "2026-09-06T23:30:00Z",
      "updatedAt": "2026-09-06T23:30:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

#### 3. Get Transaction by ID
- **Method**: `GET /api/v1/transactions/{id}`
- **Status**: `200 OK`

#### 4. Update Transaction
- **Method**: `PUT /api/v1/transactions/{id}`
- **Status**: `200 OK`

#### 5. Delete Transaction
- **Method**: `DELETE /api/v1/transactions/{id}`
- **Status**: `204 No Content`

---

### Error Responses
- `401 Unauthorized`: Missing or invalid JWT.
- `403 Forbidden`: Accessing or referencing another user's resource.
- `404 Not Found`: Nonexistent resource ID.
- `400 Bad Request`: Validation error or category/type mismatch.
- `409 Conflict`: Unique constraint violation.
