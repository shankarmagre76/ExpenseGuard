import { BudgetResponse } from './budget';

export interface MonthlyAnalyticsResponse {
  month: string;
  totalIncome: number;
  totalExpense: number;
  netSavings: number;
  savingsRate: number;
}

export interface CategoryExpenseItem {
  categoryId: string;
  categoryName: string;
  amount: number;
  percentage: number;
}

export interface CategoryAnalyticsResponse {
  month: string;
  totalExpense: number;
  categories: CategoryExpenseItem[];
}

export interface AccountCashflowItem {
  accountId: string;
  accountName: string;
  totalIncome: number;
  totalExpense: number;
  netChange: number;
}

export interface AccountAnalyticsResponse {
  month: string;
  accounts: AccountCashflowItem[];
}

export interface TopCategoryResponse {
  month: string;
  topCategories: CategoryExpenseItem[];
}

export interface BudgetPerformanceResponse {
  month: string;
  budgets: BudgetResponse[];
  totalBudgeted: number;
  totalSpent: number;
  overallUtilizationPercentage: number;
}

export interface MonthlyTrendItem {
  month: string;
  totalIncome: number;
  totalExpense: number;
  netSavings: number;
}

export interface MonthlyTrendResponse {
  fromMonth: string;
  toMonth: string;
  trends: MonthlyTrendItem[];
}
