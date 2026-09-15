import React from 'react';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../../types/navigation';
import { ScreenContainer } from '../../components/ScreenContainer';
import { TransactionForm } from '../../components/TransactionForm';
import { useAccounts } from '../../hooks/useAccounts';
import { useCategories } from '../../hooks/useCategories';
import { useTransactions } from '../../hooks/useTransactions';
import { TransactionRequest } from '../../types/transaction';

type Props = NativeStackScreenProps<RootStackParamList, 'AddIncome'>;

export const AddIncomeScreen: React.FC<Props> = ({ navigation }) => {
  const { accounts } = useAccounts();
  const { categories } = useCategories('INCOME');
  const { addTransaction } = useTransactions();

  const handleSubmit = async (data: TransactionRequest) => {
    await addTransaction(data);
    navigation.goBack();
  };

  return (
    <ScreenContainer scrollable>
      <TransactionForm
        type="INCOME"
        accounts={accounts}
        categories={categories}
        onSubmit={handleSubmit}
        onCancel={() => navigation.goBack()}
      />
    </ScreenContainer>
  );
};
