# ExpenseGuard

ExpenseGuard is a full-stack personal expense and financial management application built using Java, Spring Boot, Spring Security, Hibernate/JPA, PostgreSQL, React Native, and TypeScript. It enables users to securely manage accounts, income and expenses, categories, monthly budgets, recurring transactions, financial analytics, and receipt OCR. The application also supports offline transaction management with synchronization and conflict resolution, ensuring reliable data handling even with limited connectivity. JWT-based authentication, resource ownership validation, secure token storage, idempotent synchronization, Docker, automated testing, and CI/CD are implemented to provide a secure and production-oriented architecture.

## Features

- User registration and login
- JWT-based authentication
- Secure token storage
- User profile management
- Account management
- Income and expense tracking
- Expense and income categories
- Transaction filtering and pagination
- Monthly budgets
- Budget utilization tracking
- Financial analytics
- Recurring transactions
- Receipt upload and OCR
- Offline transaction creation and synchronization
- Conflict detection and resolution
- Notification integration
- Docker support
- CI/CD pipeline
- Automated backend and mobile testing

## Technology Stack

### Backend

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- PostgreSQL
- JWT
- Maven
- Docker
- Docker Compose
- Spring Boot Actuator

### Mobile

- React Native
- TypeScript
- React Navigation
- Axios
- React Native Keychain
- AsyncStorage
- NetInfo
- React Native Image Picker
- Jest
- ESLint

## Architecture

```text
React Native + TypeScript
          |
          | REST API / JSON
          v
Spring Boot + Java 21
          |
          | JPA / Hibernate
          v
PostgreSQL