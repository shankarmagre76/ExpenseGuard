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

### Recurring Transactions Endpoints (`/api/v1/recurring-transactions`)

#### 1. Create Recurring Transaction
- **Method**: `POST /api/v1/recurring-transactions`
- **Status**: `201 Created`
- **Request Body**:
```json
{
  "accountId": "a1b2c3d4-e5f6-7890-abcd-1234567890ab",
  "categoryId": "f1e2d3c4-b5a6-7890-abcd-0987654321ba",
  "type": "EXPENSE",
  "amount": 15000.00,
  "description": "Monthly House Rent",
  "frequency": "MONTHLY",
  "startDate": "2026-09-01",
  "nextRunDate": "2026-09-01",
  "endDate": "2027-08-31"
}
```

#### 2. Get All Recurring Transactions
- **Method**: `GET /api/v1/recurring-transactions`
- **Status**: `200 OK`

#### 3. Get Recurring Transaction by ID
- **Method**: `GET /api/v1/recurring-transactions/{id}`
- **Status**: `200 OK`

#### 4. Update Recurring Transaction
- **Method**: `PUT /api/v1/recurring-transactions/{id}`
- **Status**: `200 OK`

#### 5. Delete Recurring Transaction
- **Method**: `DELETE /api/v1/recurring-transactions/{id}`
- **Status**: `204 No Content`

#### 6. Toggle Active Status
- **Method**: `PUT /api/v1/recurring-transactions/{id}/status`
- **Status**: `200 OK`
- **Request Body**: `{"active": false}`

#### 7. Manual Trigger Execution (Testing / Portfolio Demo)
- **Method**: `POST /api/v1/recurring-transactions/{id}/execute`
- **Status**: `200 OK`
- **Response Body**: `{"message": "Execution triggered successfully", "executedCount": 1}`

---

### Offline Synchronization Endpoints (`/api/v1/sync`)

#### 1. Single Transaction Synchronization
- **Method**: `POST /api/v1/sync/transactions`
- **Status**: `200 OK` (Processed) or `409 Conflict` (Version Conflict or Payload Mismatch)
- **Request Body**:
```json
{
  "clientOperationId": "mobile-abc-123",
  "operationType": "CREATE",
  "transactionId": null,
  "accountId": "a1b2c3d4-e5f6-7890-abcd-1234567890ab",
  "categoryId": "f1e2d3c4-b5a6-7890-abcd-0987654321ba",
  "type": "EXPENSE",
  "amount": 500.00,
  "transactionDate": "2026-09-07",
  "description": "Lunch"
}
```
- **UPDATE Request (with versioning)**:
```json
{
  "clientOperationId": "mobile-update-456",
  "operationType": "UPDATE",
  "transactionId": "b1c2d3e4-f5a6-7890-abcd-112233445566",
  "version": 2,
  "accountId": "a1b2c3d4-e5f6-7890-abcd-1234567890ab",
  "categoryId": "f1e2d3c4-b5a6-7890-abcd-0987654321ba",
  "type": "EXPENSE",
  "amount": 600.00,
  "transactionDate": "2026-09-07"
}
```
- **PROCESSED Response Body**:
```json
{
  "clientOperationId": "mobile-abc-123",
  "status": "PROCESSED",
  "transactionId": "b1c2d3e4-f5a6-7890-abcd-112233445566",
  "serverVersion": 0,
  "message": "Transaction synchronized successfully"
}
```
- **CONFLICT Response Body (409 Conflict)**:
```json
{
  "clientOperationId": "mobile-update-456",
  "status": "CONFLICT",
  "transactionId": "b1c2d3e4-f5a6-7890-abcd-112233445566",
  "serverVersion": 3,
  "errorCode": "TRANSACTION_CONFLICT",
  "message": "The transaction was modified on the server."
}
```

#### 2. Batch Synchronization
- **Method**: `POST /api/v1/sync/transactions/batch`
- **Status**: `200 OK`
- **Request Body**:
```json
{
  "operations": [
    {
      "clientOperationId": "mobile-op-1",
      "operationType": "CREATE",
      "accountId": "a1b2c3d4-e5f6-7890-abcd-1234567890ab",
      "categoryId": "f1e2d3c4-b5a6-7890-abcd-0987654321ba",
      "type": "EXPENSE",
      "amount": 100.00,
      "transactionDate": "2026-09-07"
    }
  ]
}
```

#### 3. Sync Status Lookup
- **Method**: `GET /api/v1/sync/status/{clientOperationId}`
- **Status**: `200 OK`

---

### Sync Architecture & Design Principles
- **Offline Operations**: Mobile clients generate unique `clientOperationId` UUIDs while offline and send sync operations upon reconnecting.
- **Idempotency & Payload Hashing**: SHA-256 payload hashing ensures retries with identical `clientOperationId` return recorded results without duplicate transactions or balance changes. Mismatched payloads under the same operation ID return `409 Conflict` (`SYNC_OPERATION_PAYLOAD_MISMATCH`).
- **Optimistic Locking**: Transactions use `@Version` fields. UPDATE requests verify `version`. Stale client versions return `409 Conflict` (`TRANSACTION_CONFLICT`).
- **Explicit Conflict Resolution**: Financial data strictly rejects "last write wins". Server state is preserved on conflict, allowing client applications to resolve explicitly.
- **Security & Authorization**: All operations enforce JWT authentication and verify user ownership of accounts, categories, transactions, and sync records.

---

### Error Responses
- `401 Unauthorized`: Missing or invalid JWT.
- `403 Forbidden`: Accessing or referencing another user's resource.
- `404 Not Found`: Nonexistent resource ID or sync operation.
- `400 Bad Request`: Validation error, invalid month, missing date/version/operationId, or category/type mismatch.
- `409 Conflict`: Unique constraint violation, payload hash mismatch, or optimistic lock version conflict.

---

## Testing & Quality Assurance

ExpenseGuard incorporates a multi-tiered test suite ensuring security isolation, financial accuracy, input validation, and offline idempotency.

### Running Tests
Execute the complete test suite:
```powershell
.\mvnw.cmd clean test
```

### Test Suite Architecture
- **Integration Testing**: End-to-end REST API verification using MockMvc and Spring Boot test context.
- **Unit Testing**: Focused unit tests for deterministic business rules (`SyncServiceImpl`, `BudgetServiceImpl`).
- **Security Testing**: Verification of missing, malformed, expired, and tampered JWT tokens, invalid auth schemes, IDOR cross-user protection, and password hash concealment.
- **Validation & Boundary Testing**: Testing boundary amounts (`0`, negative, `0.01`, `999999.99`), long descriptions (`255` vs `256`), and pagination bounds (`size > 100` capped).
- **Financial Consistency & Invariants**: Enforces `Account.balance == Initial + sum(INCOME) - sum(EXPENSE)` with exact `BigDecimal` decimal math.
- **Idempotency & Optimistic Locking**: Ensures zero duplicate transactions/balance changes on retries, SHA-256 payload consistency, and `@Version` conflict rejection (`409 Conflict`).
- **Receipt OCR & Storage Security**: Path traversal prevention (`../../etc/passwd`), file size bounds (>10MB), and physical file deletion cleanup.

---

## Docker Setup

ExpenseGuard provides containerized deployment for the Spring Boot backend and PostgreSQL database.

### Prerequisites
- [Docker Engine](https://docs.docker.com/get-docker/) (v20.10+)
- [Docker Compose](https://docs.docker.com/compose/install/) (v2.0+)

### Environment Variable Setup
Copy `.env.example` to `.env` to customize production parameters:
```bash
cp .env.example .env
```

### Build & Run Services
Build and start the complete application stack (PostgreSQL + Spring Boot Backend):
```bash
docker compose up --build
```

Start services in detached background mode:
```bash
docker compose up -d
```

### View Application Logs
Stream live logs from the backend service:
```bash
docker compose logs -f backend
```

### Stopping Services
Stop and remove running containers:
```bash
docker compose down
```

### Database & File Persistence
- **PostgreSQL Data**: Saved in the named Docker volume `postgres_data`, preserving all tables, records, and schema migrations across container recreations.
- **Uploaded Receipts**: Stored in the named Docker volume `receipt_data`, guaranteeing that uploaded receipt images persist independently of backend container restarts.

### Health Checks
- **Public Health Endpoint**: `http://localhost:8081/api/v1/health`
- **Actuator Health Endpoint**: `http://localhost:8081/actuator/health`

---

## CI/CD & Deployment

ExpenseGuard utilizes GitHub Actions for continuous integration and automated quality assurance.

### Continuous Integration Pipeline (`.github/workflows/ci.yml`)

The CI workflow automatically triggers on every `push` and `pull_request` to the `main` branch.

#### Pipeline Steps:
1. **Source Code Checkout**: Pulls the target commit repository state (`actions/checkout@v4`).
2. **Java 21 JDK Setup**: Configures Eclipse Temurin JDK 21 with automatic Maven dependency caching (`actions/setup-java@v4`).
3. **Execution Permissions**: Grants executable flags (`chmod +x`) to `mvnw` and helper scripts.
4. **Automated Testing**: Runs the complete unit and integration test suite (`./mvnw clean test`).
5. **Artifact Packaging**: Compiles and packages the executable Spring Boot JAR (`./mvnw package -DskipTests`).
6. **Artifact Upload**: Uploads the packaged JAR file as a GitHub Actions workflow artifact (`actions/upload-artifact@v4`).
7. **Containerization**: Builds production-grade Docker image using Docker Buildx (`docker/setup-buildx-action@v3`) tagged with `github.sha` and `ci`.
8. **Container Stack Verification**: Launches PostgreSQL and Spring Boot backend services via `docker compose up -d`.
9. **Automated Readiness Check**: Executes `scripts/wait-for-health.sh` polling `http://localhost:8081/api/v1/health` until HTTP 200 OK.
10. **Teardown & Cleanup**: Gracefully shuts down and removes Docker containers (`docker compose down -v`).

### Deployment & Environment Security
- **Zero Hardcoded Secrets**: All JWT secrets, database credentials, and service configuration parameters are loaded dynamically via environment variables (`JWT_SECRET`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `SPRING_PROFILES_ACTIVE`).
- **Production Readiness**: Docker multi-stage build creates a lightweight, unprivileged JRE container for production deployment.




