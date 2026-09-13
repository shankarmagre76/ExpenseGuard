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

## 8. Authentication Architecture

ExpenseGuard Mobile integrates full JWT authentication with the Spring Boot REST endpoints:

### Endpoints
- **Registration**: `POST /api/v1/auth/register` (`name`, `email`, `password`)
- **Login**: `POST /api/v1/auth/login` (`email`, `password`) → returns JWT `accessToken`
- **Current User Profile**: `GET /api/v1/me` (requires Bearer token)

### Secure Token Storage
- JWT access tokens and user profile payloads are encrypted and stored using **`react-native-keychain`** via [`src/storage/secureStorage.ts`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/storage/secureStorage.ts).
- Passwords are **never** persisted or logged.

### Session Restoration & Auto-Login
- On application startup, [`AuthContext`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/context/AuthContext.tsx) retrieves the stored JWT token, attaches it to the HTTP client, and validates the session via `GET /api/v1/me`.
- If valid, the user transitions directly to the `MainNavigator`.
- If invalid or expired, the session is securely cleared and the user is routed to `LoginScreen`.

### API Authentication Headers & 401 Handling
- Centralized Axios client ([`src/api/client.ts`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/api/client.ts)) automatically attaches `Authorization: Bearer <accessToken>` to every outbound request.
- A response interceptor monitors `401 Unauthorized` responses, triggering automatic session cleanup and routing back to `AuthNavigator`.

---

## 9. Android Emulator Networking

> [!IMPORTANT]
> Inside the standard Android Emulator VM, `localhost` refers to the emulator's virtual device itself, **not** your development host computer.

To allow the Android emulator to reach the Spring Boot backend running on your host machine:
- **API Base URL**: `http://10.0.2.2:8081`
- **Health Endpoint**: `http://10.0.2.2:8081/api/v1/health`

---

## 10. Physical Device Networking

When testing on a physical mobile device:
1. Connect both your development machine and your mobile phone to the **same local Wi-Fi network**.
2. Find your development machine's local IP address (e.g., `192.168.1.50`).
3. Configure `API_BASE_URL` to point to your machine's LAN IP:
   ```env
   API_BASE_URL=http://192.168.1.50:8081
   ```
4. Ensure port `8081` is not blocked by your host computer's firewall.

---

## 11. Health Check

The mobile application includes a built-in health diagnostic feature to verify frontend-to-backend REST connectivity:

- **Endpoint**: `GET /api/v1/health`
- **Hook**: [`src/hooks/useHealthCheck.ts`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/hooks/useHealthCheck.ts)
- **Screen**: [`src/screens/debug/HealthCheckScreen.tsx`](file:///d:/JAVA/Projects/ExpenseGuard/mobile/src/screens/debug/HealthCheckScreen.tsx)
- **Dashboard Banner**: Displays real-time connection status (`CONNECTED`, `LOADING`, `FAILED`) and timestamp directly on the main dashboard.

---

## 12. Project Structure

```
mobile/
├── __tests__/
│   ├── App.test.tsx            # Navigation render test
│   └── auth.test.ts            # Authentication, token storage & security tests
│
├── src/
│   ├── api/
│   │   ├── client.ts           # Axios HTTP client with Bearer token & 401 interceptors
│   │   └── endpoints/
│   │       ├── authApi.ts      # Auth endpoints (register, login, getCurrentUser)
│   │       └── health.ts       # Health check API endpoint call
│   │
│   ├── components/             # Reusable core UI components
│   │   ├── AppText.tsx         # Typography wrapper
│   │   ├── ErrorMessage.tsx    # Error display with retry action
│   │   ├── LoadingIndicator.tsx # Spinner & loading message
│   │   ├── PrimaryButton.tsx   # Styled action button
│   │   └── ScreenContainer.tsx # SafeArea & ScrollView screen container
│   │
│   ├── config/
│   │   └── env.ts              # Environment & API host resolution
│   │
│   ├── context/
│   │   └── AuthContext.tsx     # Centralized auth state & session restoration provider
│   │
│   ├── hooks/
│   │   ├── useAuth.ts          # AuthContext hook
│   │   └── useHealthCheck.ts   # Custom hook for backend connectivity health check
│   │
│   ├── navigation/             # React Navigation stack & tab navigators
│   │   ├── AppNavigator.tsx    # Root stack navigator with auth state guard
│   │   ├── AuthNavigator.tsx   # Login & Register stack navigator
│   │   ├── MainNavigator.tsx   # Main bottom tab navigator
│   │   └── types.ts            # Navigation parameter list types
│   │
│   ├── screens/
│   │   ├── auth/               # Auth screens (LoginScreen, RegisterScreen, ProfileScreen)
│   │   ├── dashboard/          # Main dashboard screen
│   │   ├── transactions/       # Transactions screen placeholder
│   │   ├── accounts/           # Accounts screen placeholder
│   │   ├── categories/         # Categories screen placeholder
│   │   ├── budgets/            # Budgets screen placeholder
│   │   ├── analytics/          # Analytics screen placeholder
│   │   ├── receipts/           # Receipts screen placeholder
│   │   ├── notifications/      # Notifications screen placeholder
│   │   └── debug/              # Backend health check diagnostics screen
│   │
│   ├── storage/
│   │   └── secureStorage.ts    # Secure token & session storage (react-native-keychain)
│   │
│   ├── theme/                  # Theme tokens (colors, spacing, typography, borderRadius)
│   ├── types/                  # TypeScript interfaces (api, auth, health, navigation)
│   └── utils/                  # Centralized error parser
│
├── App.tsx                     # React Native root component
├── .env.example                # Sample environment file template
├── .gitignore                  # Git exclusions for secrets, node_modules, build outputs
├── package.json                # Project dependencies & scripts
├── tsconfig.json               # TypeScript compiler configuration
└── README.md                   # Mobile project documentation
```

---

## Type Checking & Testing

Run TypeScript compilation check:

```bash
npx tsc --noEmit
```

Run frontend unit tests:

```bash
npm test
```
