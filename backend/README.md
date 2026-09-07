# ExpenseGuard Backend

ExpenseGuard is an enterprise-grade, portfolio-quality RESTful backend service built with Spring Boot 3, Java 21, PostgreSQL, and Spring Security with JWT authentication.

## API Specification

### Authentication
All protected endpoints under `/api/v1/accounts`, `/api/v1/categories`, `/api/v1/transactions`, and `/api/v1/budgets` require a valid JWT bearer token in the `Authorization` header:

```http
Authorization: Bearer <JWT_TOKEN>
```

---

### Budgets Endpoints (`/api/v1/budgets`)

#### 1. Create Budget
- **Method**: `POST /api/v1/budgets`
- **Status**: `201 Created`
- **Request Body**:
```json
{
  "categoryId": "f1e2d3c4-b5a6-7890-abcd-0987654321ba",
  "amount": 10000.00,
  "month": "2026-09"
}
```
- **Validation Rules**:
  - `categoryId`: Required (UUID). Must belong to the authenticated user and be an `EXPENSE` category.
  - `amount`: Required (`BigDecimal > 0`).
  - `month`: Required (`YYYY-MM`). Must be a valid year-month string.
  - **Uniqueness**: Only 1 budget allocation allowed per user + category + month (`409 Conflict` on duplicate).

- **Response Body**:
```json
{
  "id": "b1c2d3e4-f5a6-7890-abcd-112233445566",
  "categoryId": "f1e2d3c4-b5a6-7890-abcd-0987654321ba",
  "categoryName": "Food",
  "month": "2026-09",
  "amount": 10000.00,
  "budgetAmount": 10000.00,
  "spentAmount": 7500.00,
  "remainingAmount": 2500.00,
  "utilizationPercentage": 75.00,
  "createdAt": "2026-09-07T00:00:00Z",
  "updatedAt": "2026-09-07T00:00:00Z"
}
```

#### 2. Get All Budgets (Filtered)
- **Method**: `GET /api/v1/budgets`
- **Status**: `200 OK`
- **Query Parameters**:
  - `month` (Optional): `YYYY-MM` (e.g. `?month=2026-09`)

#### 3. Get Budget by ID
- **Method**: `GET /api/v1/budgets/{id}`
- **Status**: `200 OK`

#### 4. Get Budget Summary
- **Method**: `GET /api/v1/budgets/{id}/summary`
- **Status**: `200 OK`

#### 5. Update Budget
- **Method**: `PUT /api/v1/budgets/{id}`
- **Status**: `200 OK`

#### 6. Delete Budget
- **Method**: `DELETE /api/v1/budgets/{id}`
- **Status**: `204 No Content`

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

---

### Analytics Endpoints (`/api/v1/analytics`)

#### 1. Monthly Financial Summary
- **Method**: `GET /api/v1/analytics/monthly`
- **Status**: `200 OK`
- **Query Parameters**: `month` (Optional, default current month `YYYY-MM`)
- **Response Body**:
```json
{
  "month": "2026-09",
  "totalIncome": 50000.00,
  "totalExpense": 15000.00,
  "netSavings": 35000.00,
  "savingsRate": 70.00
}
```

#### 2. Category Expense Breakdown
- **Method**: `GET /api/v1/analytics/categories`
- **Status**: `200 OK`
- **Query Parameters**: `month` (Optional, default current month `YYYY-MM`)
- **Response Body**:
```json
{
  "month": "2026-09",
  "totalExpense": 15000.00,
  "categories": [
    {
      "categoryId": "f1e2d3c4-b5a6-7890-abcd-0987654321ba",
      "categoryName": "Food & Dining",
      "amount": 10000.00,
      "percentage": 66.67
    }
  ]
}
```

#### 3. Account Cashflow Breakdown
- **Method**: `GET /api/v1/analytics/accounts`
- **Status**: `200 OK`
- **Query Parameters**: `month` (Optional, default current month `YYYY-MM`)

#### 4. Top Spending Categories
- **Method**: `GET /api/v1/analytics/top-categories`
- **Status**: `200 OK`
- **Query Parameters**:
  - `month` (Optional, `YYYY-MM`)
  - `limit` (Optional, Integer `1` to `20`, default `5`)

#### 5. Budget vs Actual Performance
- **Method**: `GET /api/v1/analytics/budget-performance`
- **Status**: `200 OK`
- **Query Parameters**: `month` (Optional, default current month `YYYY-MM`)

#### 6. Multi-Month Financial Trend
- **Method**: `GET /api/v1/analytics/trend`
- **Status**: `200 OK`
- **Query Parameters**:
  - `from` (Optional, `YYYY-MM`)
  - `to` (Optional, `YYYY-MM`)

---

### Error Responses
- `401 Unauthorized`: Missing or invalid JWT.
- `403 Forbidden`: Accessing or referencing another user's resource.
- `404 Not Found`: Nonexistent resource ID.
- `400 Bad Request`: Validation error, invalid month, or category/type mismatch.
- `409 Conflict`: Unique constraint violation (duplicate category, budget, or operation ID).
