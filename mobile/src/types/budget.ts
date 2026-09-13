export interface BudgetRequest {
  categoryId: string;
  amount: number;
  month: string; // YYYY-MM format
}

export interface BudgetResponse {
  id: string;
  categoryId: string;
  categoryName: string;
  month: string;
  amount: number;
  budgetAmount?: number;
  spentAmount: number;
  remainingAmount: number;
  utilizationPercentage: number;
  createdAt?: string;
  updatedAt?: string;
}

export type BudgetStatus = 'OK' | 'WARNING' | 'REACHED' | 'EXCEEDED';

export const getBudgetStatus = (utilizationPercentage: number): BudgetStatus => {
  if (utilizationPercentage > 100) return 'EXCEEDED';
  if (utilizationPercentage === 100) return 'REACHED';
  if (utilizationPercentage >= 80) return 'WARNING';
  return 'OK';
};

export const getBudgetStatusLabel = (status: BudgetStatus): string => {
  switch (status) {
    case 'EXCEEDED':
      return 'Over budget';
    case 'REACHED':
      return 'Budget reached';
    case 'WARNING':
      return 'Warning';
    case 'OK':
    default:
      return 'On track';
  }
};
