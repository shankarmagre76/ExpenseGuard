export type AuthStackParamList = {
  Login: undefined;
  Register: undefined;
};

export type MainTabParamList = {
  Dashboard: undefined;
  Transactions: undefined;
  Accounts: undefined;
  Categories: undefined;
  Budgets: undefined;
  Analytics: undefined;
  Recurring: undefined;
  Profile: undefined;
};

export type RootStackParamList = {
  Auth: undefined;
  Main: undefined;
  HealthCheck: undefined;
  AddExpense: undefined;
  AddIncome: undefined;
  EditTransaction: { transactionId: string };
};
