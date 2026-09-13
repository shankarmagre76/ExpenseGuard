# ExpenseGuard Mobile Frontend

Clean, type-safe React Native TypeScript mobile application for the **ExpenseGuard** personal finance platform.

---

## 1. Project Purpose

ExpenseGuard Mobile provides a modern, intuitive mobile interface for tracking personal expenses, managing account balances, monitoring budgets, analyzing visual spend breakdowns, and inspecting backend server diagnostics.

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

## 8. Core Expense Management (Phase 3)

ExpenseGuard Mobile provides a complete financial tracking experience consuming Spring Boot REST APIs:

### Dashboard
- Real-time **Total Net Balance** calculated across user accounts.
- **Recent Income & Recent Expense** summary breakdown.
- Quick action triggers for **+ Add Expense**, **+ Add Income**, and recent activity feed.

### Accounts Management
- **CRUD Operations**: Create, edit, and delete financial accounts via [`src/api/endpoints/accountApi.ts`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/api/endpoints/accountApi.ts).
- **Supported Account Types**: `CASH`, `BANK`, `SAVINGS`, `CREDIT_CARD`, `WALLET`.
- Delete confirmation alerts to prevent accidental removal.

### Categories Management
- **CRUD Operations**: Create, edit, and delete expense and income categories via [`src/api/endpoints/categoryApi.ts`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/api/endpoints/categoryApi.ts).
- **Tabbed Interface**: Separate management for `EXPENSE` and `INCOME` categories.

### Transactions & Pagination
- **CRUD Operations**: Create, edit, and delete transactions via [`src/api/endpoints/transactionApi.ts`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/api/endpoints/transactionApi.ts).
- **Infinite Scroll Pagination**: Handles Spring Data `Page<TransactionResponse>` with `onEndReached` infinite scrolling and end-of-list detection.
- **Multi-criteria Filtering**: Filter by transaction type (`INCOME`, `EXPENSE`), account, category, `fromDate`, `toDate`.
- **Reusable Forms**: [`TransactionForm.tsx`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/components/TransactionForm.tsx) supports both Expense and Income entry without duplicated logic.

### Money & Date Formatting Safety
- Monetary values avoid JavaScript floating-point arithmetic errors by utilizing [`src/utils/currencyFormatter.ts`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/utils/currencyFormatter.ts).
- Calendar dates maintain ISO format (`YYYY-MM-DD`) without timezone conversion drift.

---

## 9. Authentication Architecture

ExpenseGuard Mobile integrates full JWT authentication with the Spring Boot REST endpoints:

### Endpoints
- **Registration**: `POST /api/v1/auth/register` (`name`, `email`, `password`)
- **Login**: `POST /api/v1/auth/login` (`email`, `password`) → returns JWT `accessToken`
- **Current User Profile**: `GET /api/v1/me` (requires Bearer token)

### Secure Token Storage
- JWT access tokens and user profile payloads are encrypted and stored using **`react-native-keychain`** via [`src/storage/secureStorage.ts`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/storage/secureStorage.ts).
- Passwords are **never** persisted or logged.

---

## 10. Project Structure

```
mobile/
├── __tests__/
│   ├── App.test.tsx            # Navigation render test
│   ├── auth.test.ts            # Auth & token security unit tests
│   └── expense.test.ts         # Core expense management unit tests
│
├── src/
│   ├── api/
│   │   ├── client.ts           # Axios HTTP client with Bearer token & 401 interceptors
│   │   └── endpoints/
│   │       ├── accountApi.ts   # Account endpoints (CRUD)
│   │       ├── authApi.ts      # Auth endpoints (register, login, getCurrentUser)
│   │       ├── categoryApi.ts  # Category endpoints (CRUD)
│   │       ├── health.ts       # Health check API endpoint call
│   │       └── transactionApi.ts # Transaction endpoints (CRUD & paginated filtering)
│   │
│   ├── components/             # Reusable core UI components
│   │   ├── AccountModal.tsx    # Create/Edit Account modal
│   │   ├── CategoryModal.tsx   # Create/Edit Category modal
│   │   ├── ErrorMessage.tsx    # Error display with retry action
│   │   ├── LoadingIndicator.tsx # Activity indicator
│   │   ├── PrimaryButton.tsx   # Styled action button
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
│   │   ├── useAuth.ts          # AuthContext hook
│   │   ├── useCategories.ts    # Custom hook for category CRUD state
│   │   ├── useHealthCheck.ts   # Backend connectivity health hook
│   │   └── useTransactions.ts  # Custom hook for paginated transaction state & filters
│   │
│   ├── navigation/             # React Navigation stack & tab navigators
│   │   ├── AppNavigator.tsx    # Root stack navigator with auth & transaction routes
│   │   ├── AuthNavigator.tsx   # Login & Register stack navigator
│   │   ├── MainNavigator.tsx   # Main bottom tab navigator (Dashboard, Transactions, Accounts, Categories, Profile)
│   │   └── types.ts            # Navigation parameter list types
│   │
│   ├── screens/
│   │   ├── accounts/           # AccountsScreen (Account list & CRUD)
│   │   ├── auth/               # LoginScreen, RegisterScreen, ProfileScreen
│   │   ├── categories/         # CategoriesScreen (Expense & Income categories)
│   │   ├── dashboard/          # DashboardScreen (Net balance, recent activity, quick actions)
│   │   ├── debug/              # HealthCheckScreen
│   │   └── transactions/       # TransactionsScreen, AddExpenseScreen, AddIncomeScreen, EditTransactionScreen
│   │
│   ├── storage/
│   │   └── secureStorage.ts    # Secure token & session storage (react-native-keychain)
│   │
│   ├── theme/                  # Theme tokens (colors, spacing, typography, borderRadius)
│   ├── types/                  # TypeScript interfaces (account, api, auth, category, dashboard, health, navigation, transaction)
│   └── utils/                  # Currency, date, and error helpers
│
├── App.tsx                     # React Native root component
├── .env.example                # Sample environment file template
├── .gitignore                  # Git exclusions for secrets, node_modules, build outputs
├── package.json                # Project dependencies & scripts
├── tsconfig.json               # TypeScript compiler configuration
└── README.md                   # Mobile project documentation
```

---

## Type Checking, Linting & Testing

Run TypeScript compilation check:

```bash
npx tsc --noEmit
```

Run ESLint check:

```bash
npm run lint
```

Run frontend unit tests:

```bash
npm test
```
