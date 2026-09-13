# ExpenseGuard Mobile Frontend

Clean, type-safe React Native TypeScript mobile application for the **ExpenseGuard** personal finance platform.

---

## 1. Project Purpose

ExpenseGuard Mobile provides a modern, intuitive mobile interface for tracking personal expenses, managing account balances, setting monthly category budgets, scheduling recurring transactions, analyzing financial analytics and spending trends, and inspecting backend server diagnostics.

---

## 2. Requirements

- **Node.js**: `>= 22.11.0`
- **npm** or **yarn**
- **Android Development**: Android Studio, Android SDK (API 34+ recommended), JDK 17 or Java 21, configured `ANDROID_HOME` environment variable.
- **iOS Development (macOS only)**: Xcode 15+, CocoaPods.
- **ExpenseGuard Backend**: Spring Boot backend running locally or on port `8081`.

---

## 3. Installation

Navigate to the `mobile` directory and install project dependencies:

```bash
cd mobile
npm install
```

---

## 4. Running Metro Bundler

To start the Metro development server:

```bash
npm start
```

---

## 5. Running Android Application

To launch the application on an active Android emulator or connected device:

```bash
npm run android
```

*(For iOS on macOS: `npm run ios`)*

---

## 6. Backend Dependency

ExpenseGuard Mobile consumes the REST APIs exposed by the **ExpenseGuard Spring Boot Backend** running on port `8081`.

---

## 7. API URL Configuration

The API base URL is configured centrally in [`src/config/env.ts`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/config/env.ts).

Environment variable support:
- `API_BASE_URL` (default dynamically selected based on platform)
- `API_TIMEOUT` (default `15000` ms)

Example `.env` configuration (refer to `.env.example`):
```env
API_BASE_URL=http://10.0.2.2:8081
API_TIMEOUT=15000
ENV_NAME=development
```

---

## 8. Core Features & Architecture

### Dashboard & Navigation
- Real-time **Total Net Balance** calculated across user accounts.
- **Recent Income & Recent Expense** summary breakdown.
- **Budget Status Overview Widget** displaying monthly budget utilization warnings.
- Quick action triggers for **+ Add Expense**, **+ Add Income**, and recent activity feed.

### Accounts Management (Phase 3)
- **CRUD Operations**: Create, edit, and delete financial accounts via [`src/api/endpoints/accountApi.ts`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/api/endpoints/accountApi.ts).
- **Supported Account Types**: `CASH`, `BANK`, `SAVINGS`, `CREDIT_CARD`, `WALLET`.

### Categories Management (Phase 3)
- **CRUD Operations**: Create, edit, and delete expense and income categories via [`src/api/endpoints/categoryApi.ts`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/api/endpoints/categoryApi.ts).

### Transactions & Pagination (Phase 3)
- **CRUD Operations**: Create, edit, and delete transactions via [`src/api/endpoints/transactionApi.ts`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/api/endpoints/transactionApi.ts).
- **Infinite Scroll Pagination**: Handles Spring Data `Page<TransactionResponse>` with `onEndReached` infinite scrolling.

---

## 9. Budgets & Financial Analytics (Phase 4)

ExpenseGuard Mobile implements complete monthly budget control and visual financial analytics:

### Budget Management & Utilization
- **Budgets CRUD Operations**: Create, edit, and delete category budgets via [`src/api/endpoints/budgetApi.ts`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/api/endpoints/budgetApi.ts).
- **Category Filter Restriction**: Budget creation strictly restricts category selection to `EXPENSE` type categories.
- **Backend Threshold Colors & Badges**:
  - `< 80%`: Green (`On track`)
  - `80% - < 100%`: Warning Yellow/Orange (`Warning`)
  - `100%`: Dark Alert (`Budget reached`)
  - `> 100%`: Red (`Over budget`)
- **Month Selector Component**: [`MonthSelector.tsx`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/components/MonthSelector.tsx) allows seamlessly navigating months (`YYYY-MM` ISO format) with human-readable display (`September 2026`).

### Financial Analytics Dashboard
- **Monthly Financial Summary**: Overview cards for Total Income, Total Expense, Net Savings, and Savings Rate (`GET /api/v1/analytics/monthly`).
- **Category Expense Breakdown**: Visual bar chart breakdown of spending by category with percentages (`GET /api/v1/analytics/categories`).
- **Budget Performance**: Budget vs actual spending progress bars and status indicators (`GET /api/v1/analytics/budget-performance`).
- **Account Cash Flow**: Account-level income, expense, and net change metrics (`GET /api/v1/analytics/accounts`).
- **6-Month Spending Trend**: Multi-month comparative bar chart visualization for historical income vs expense trends (`GET /api/v1/analytics/trend`).

---

## 10. Recurring Transactions (Phase 5.1)

ExpenseGuard Mobile supports automated recurring transaction management:

- **Recurring Templates CRUD**: Create, edit, and delete recurring schedules via [`src/api/endpoints/recurringTransactionApi.ts`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/api/endpoints/recurringTransactionApi.ts).
- **Frequencies**: `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`.
- **Active / Disabled Toggle**: Enable or disable recurring schedules (`PUT /api/v1/recurring-transactions/{id}/status`).
- **Manual Trigger ("Run Now")**: Immediately trigger execution of due recurring occurrences (`POST /api/v1/recurring-transactions/{id}/execute`).

---

## 11. Receipt Upload & OCR Scanner (Phase 5.2)

ExpenseGuard Mobile integrates receipt scanning and OCR parsing:

- **Image Picker & Camera Integration**: Launch camera or photo gallery to capture receipt images (`react-native-image-picker`).
- **Multipart Receipt Upload**: Upload receipt images with multipart form-data to `POST /api/v1/receipts/upload`.
- **OCR Status & Extraction**: View extracted merchant name, transaction date, total amount, and confidence score.
- **Review & Convert**: Pre-fill transaction form with extracted receipt data for instant expense logging.

---

## 12. In-App Notifications Engine (Phase 5.3)

ExpenseGuard Mobile derives in-app notification alerts dynamically from backend states:

- **Derived Notification Types**:
  - `BUDGET_EXCEEDED`: Over-budget alert when spending exceeds allocated budget (utilization >= 100%).
  - `BUDGET_WARNING`: Early warning alert when spending reaches 80% to 99.9% of budget.
  - `RECURRING_DUE`: Due alert when active recurring payment schedule `nextRunDate` is today or past due.
  - `SYSTEM_INFO`: Connectivity alert verifying backend server status on port 8081.
- **Notification Badge Component**: [`NotificationBadge.tsx`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/components/NotificationBadge.tsx) bell icon with real-time unread badge count on the dashboard header.
- **Notifications Screen**: Screen allowing users to view, mark read, mark all read, and delete notifications.

---

## 13. Authentication Architecture

ExpenseGuard Mobile integrates full JWT authentication with the Spring Boot REST endpoints:

### Endpoints
- **Registration**: `POST /api/v1/auth/register` (`name`, `email`, `password`)
- **Login**: `POST /api/v1/auth/login` (`email`, `password`) → returns JWT `accessToken`
- **Current User Profile**: `GET /api/v1/me` (requires Bearer token)

### Secure Token Storage
- JWT access tokens and user profile payloads are encrypted and stored using **`react-native-keychain`** via [`src/storage/secureStorage.ts`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/storage/secureStorage.ts).

---

## 12. Project Structure

```
mobile/
├── __tests__/
│   ├── App.test.tsx            # Navigation render test
│   ├── auth.test.ts            # Auth & token security unit tests
│   ├── budget_analytics.test.ts # Budgets & analytics unit tests
│   ├── expense.test.ts         # Core expense management unit tests
│   └── recurring_transaction.test.ts # Recurring transaction unit tests
│
├── src/
│   ├── api/
│   │   ├── client.ts           # Axios HTTP client with Bearer token & 401 interceptors
│   │   └── endpoints/
│   │       ├── accountApi.ts   # Account endpoints (CRUD)
│   │       ├── analyticsApi.ts # Financial analytics endpoints
│   │       ├── authApi.ts      # Auth endpoints
│   │       ├── budgetApi.ts    # Budget endpoints (CRUD & summary)
│   │       ├── categoryApi.ts  # Category endpoints (CRUD)
│   │       ├── health.ts       # Health check endpoint call
│   │       ├── recurringTransactionApi.ts # Recurring transaction endpoints (CRUD, status, execute)
│   │       └── transactionApi.ts # Transaction endpoints (CRUD & paginated filtering)
│   │
│   ├── components/             # Reusable core UI components
│   │   ├── AccountModal.tsx    # Create/Edit Account modal
│   │   ├── AnalyticsBarChart.tsx # Category & Trend visualizers
│   │   ├── BudgetModal.tsx     # Create/Edit Budget modal
│   │   ├── BudgetProgressBar.tsx # Utilization progress bar & status badge
│   │   ├── CategoryModal.tsx   # Create/Edit Category modal
│   │   ├── ErrorMessage.tsx    # Error display with retry action
│   │   ├── LoadingIndicator.tsx # Activity indicator
│   │   ├── MonthSelector.tsx   # Month navigation header
│   │   ├── PrimaryButton.tsx   # Styled action button
│   │   ├── RecurringTransactionModal.tsx # Create/Edit Recurring modal
│   │   ├── ScreenContainer.tsx # SafeArea & ScrollView wrapper
│   │   ├── TransactionFilterModal.tsx # Multi-criteria transaction filter sheet
│   │   └── TransactionForm.tsx # Reusable expense & income form
│   │
│   ├── config/
│   │   └── env.ts              # Environment & API host resolution
│   │
│   ├── context/
│   │   └── AuthContext.tsx     # Centralized auth state & session restoration provider
│   │
│   ├── hooks/
│   │   ├── useAccounts.ts      # Custom hook for account CRUD state
│   │   ├── useAnalytics.ts     # Custom hook for analytics state
│   │   ├── useAuth.ts          # AuthContext hook
│   │   ├── useBudgets.ts        # Custom hook for budget CRUD state
│   │   ├── useCategories.ts    # Custom hook for category CRUD state
│   │   ├── useHealthCheck.ts   # Backend connectivity health hook
│   │   ├── useRecurringTransactions.ts # Custom hook for recurring transactions
│   │   └── useTransactions.ts  # Custom hook for paginated transaction state
│   │
│   ├── navigation/             # React Navigation stack & tab navigators
│   ├── screens/
│   │   ├── accounts/           # AccountsScreen
│   │   ├── analytics/          # AnalyticsScreen
│   │   ├── auth/               # LoginScreen, RegisterScreen, ProfileScreen
│   │   ├── budgets/            # BudgetsScreen
│   │   ├── categories/         # CategoriesScreen
│   │   ├── dashboard/          # DashboardScreen
│   │   ├── debug/              # HealthCheckScreen
│   │   ├── recurring/          # RecurringTransactionsScreen
│   │   └── transactions/       # TransactionsScreen & Form screens
│   │
│   ├── storage/
│   │   └── secureStorage.ts    # Secure token & session storage
│   ├── theme/                  # Theme tokens
│   ├── types/                  # TypeScript interfaces (account, analytics, api, auth, budget, category, recurringTransaction, etc.)
│   └── utils/                  # Currency, date, and error helpers
│
├── App.tsx                     # React Native root component
├── .env.example                # Sample environment file template
├── package.json                # Project dependencies & scripts
├── tsconfig.json               # TypeScript compiler configuration
└── README.md                   # Mobile project documentation
```

---

## 13. Type Checking, Linting & Testing

Run TypeScript compilation check:

```bash
cd mobile
npx tsc --noEmit
```

Run ESLint check:

```bash
cd mobile
npm run lint
```

Run frontend unit tests:

```bash
cd mobile
npm test -- --runInBand
```
