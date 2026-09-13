import * as recurringApi from '../src/api/endpoints/recurringTransactionApi';
import { apiClient } from '../src/api/client';
import { RecurringTransactionRequest, RecurrenceFrequency } from '../src/types/recurringTransaction';

jest.mock('../src/api/client', () => ({
  apiClient: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
}));

describe('Recurring Transaction API & Data Contracts', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('fetches list of recurring transactions for current user', async () => {
    const mockData = [
      {
        id: 'r1',
        accountId: 'acc1',
        categoryId: 'cat1',
        categoryName: 'Subscriptions',
        type: 'EXPENSE' as const,
        amount: 15,
        description: 'Netflix',
        frequency: 'MONTHLY' as RecurrenceFrequency,
        startDate: '2026-01-01',
        nextRunDate: '2026-10-01',
        active: true,
      },
    ];
    (apiClient.get as jest.Mock).mockResolvedValueOnce(mockData);

    const res = await recurringApi.getRecurringTransactions();
    expect(apiClient.get).toHaveBeenCalledWith('/api/v1/recurring-transactions');
    expect(res).toEqual(mockData);
  });

  it('creates a new recurring transaction template', async () => {
    const req: RecurringTransactionRequest = {
      accountId: 'acc1',
      categoryId: 'cat1',
      type: 'EXPENSE',
      amount: 15,
      description: 'Netflix',
      frequency: 'MONTHLY',
      startDate: '2026-01-01',
      nextRunDate: '2026-10-01',
    };
    const created = { id: 'r1', ...req, categoryName: 'Subscriptions', active: true };
    (apiClient.post as jest.Mock).mockResolvedValueOnce(created);

    const res = await recurringApi.createRecurringTransaction(req);
    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/recurring-transactions', req);
    expect(res).toEqual(created);
  });

  it('updates an existing recurring transaction template', async () => {
    const req: RecurringTransactionRequest = {
      accountId: 'acc1',
      categoryId: 'cat1',
      type: 'EXPENSE',
      amount: 20,
      description: 'Netflix Premium',
      frequency: 'MONTHLY',
      startDate: '2026-01-01',
      nextRunDate: '2026-10-01',
    };
    const updated = { id: 'r1', ...req, categoryName: 'Subscriptions', active: true };
    (apiClient.put as jest.Mock).mockResolvedValueOnce(updated);

    const res = await recurringApi.updateRecurringTransaction('r1', req);
    expect(apiClient.put).toHaveBeenCalledWith('/api/v1/recurring-transactions/r1', req);
    expect(res).toEqual(updated);
  });

  it('deletes a recurring transaction by ID', async () => {
    (apiClient.delete as jest.Mock).mockResolvedValueOnce(null);

    await recurringApi.deleteRecurringTransaction('r1');
    expect(apiClient.delete).toHaveBeenCalledWith('/api/v1/recurring-transactions/r1');
  });

  it('toggles active status of a recurring transaction', async () => {
    const mockResponse = { id: 'r1', active: false };
    (apiClient.put as jest.Mock).mockResolvedValueOnce(mockResponse);

    const res = await recurringApi.toggleRecurringTransactionStatus('r1', false);
    expect(apiClient.put).toHaveBeenCalledWith('/api/v1/recurring-transactions/r1/status', {
      active: false,
    });
    expect(res).toEqual(mockResponse);
  });

  it('triggers manual execution of due recurring occurrences', async () => {
    const mockExecutionRes = { message: 'Execution triggered successfully', executedCount: 1 };
    (apiClient.post as jest.Mock).mockResolvedValueOnce(mockExecutionRes);

    const res = await recurringApi.triggerManualExecution('r1');
    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/recurring-transactions/r1/execute');
    expect(res).toEqual(mockExecutionRes);
  });
});
