import React, { useState, useEffect, useCallback } from 'react';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../../types/navigation';
import { ScreenContainer } from '../../components/ScreenContainer';
import { LoadingIndicator } from '../../components/LoadingIndicator';
import { ErrorMessage } from '../../components/ErrorMessage';
import { TransactionForm } from '../../components/TransactionForm';
import { useAccounts } from '../../hooks/useAccounts';
import { useCategories } from '../../hooks/useCategories';
import { getTransactionById, updateTransaction } from '../../api/endpoints/transactionApi';
import { TransactionResponse, TransactionRequest } from '../../types/transaction';

type Props = NativeStackScreenProps<RootStackParamList, 'EditTransaction'>;

export const EditTransactionScreen: React.FC<Props> = ({ route, navigation }) => {
  const { transactionId } = route.params;

  const [transaction, setTransaction] = useState<TransactionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const { accounts } = useAccounts();
  const { allCategories: categories } = useCategories();

  const loadTransaction = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getTransactionById(transactionId);
      setTransaction(data);
    } catch (err: any) {
      setError(err?.message || 'Failed to load transaction details.');
    } finally {
      setLoading(false);
    }
  }, [transactionId]);

  useEffect(() => {
    loadTransaction();
  }, [loadTransaction]);

  const handleSubmit = async (data: TransactionRequest) => {
    await updateTransaction(transactionId, data);
    navigation.goBack();
  };

  if (loading) {
    return (
      <ScreenContainer>
        <LoadingIndicator message="Loading transaction details..." />
      </ScreenContainer>
    );
  }

  if (error || !transaction) {
    return (
      <ScreenContainer>
        <ErrorMessage message={error || 'Transaction not found'} onRetry={loadTransaction} />
      </ScreenContainer>
    );
  }

  return (
    <ScreenContainer scrollable>
      <TransactionForm
        type={transaction.type}
        initialValues={transaction}
        accounts={accounts}
        categories={categories}
        onSubmit={handleSubmit}
        onCancel={() => navigation.goBack()}
        title={`Edit ${transaction.type}`}
      />
    </ScreenContainer>
  );
};
