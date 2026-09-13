import { useState, useCallback, useEffect } from 'react';
import { AccountResponse, AccountRequest } from '../types/account';
import {
  getAccounts,
  createAccount,
  updateAccount,
  deleteAccount,
} from '../api/endpoints/accountApi';
import { AppError } from '../types/api';

export const useAccounts = () => {
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getAccounts();
      setAccounts(data);
    } catch (err: any) {
      const appErr = err as AppError;
      setError(appErr.message || 'Failed to load accounts.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const addAccount = async (request: AccountRequest): Promise<AccountResponse> => {
    const newAccount = await createAccount(request);
    await refresh();
    return newAccount;
  };

  const editAccount = async (id: string, request: AccountRequest): Promise<AccountResponse> => {
    const updated = await updateAccount(id, request);
    await refresh();
    return updated;
  };

  const removeAccount = async (id: string): Promise<void> => {
    await deleteAccount(id);
    await refresh();
  };

  return {
    accounts,
    loading,
    error,
    refresh,
    addAccount,
    editAccount,
    removeAccount,
  };
};
