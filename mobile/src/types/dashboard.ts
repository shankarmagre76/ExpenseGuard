import { TransactionResponse } from './transaction';
import { AccountResponse } from './account';

export interface DashboardSummary {
  totalBalance: number;
  totalIncome: number;
  totalExpenses: number;
  currency: string;
  accounts: AccountResponse[];
  recentTransactions: TransactionResponse[];
}
