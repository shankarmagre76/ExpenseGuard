import React from 'react';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../../types/navigation';
import { ScreenContainer } from '../../components/ScreenContainer';
import { PrimaryButton } from '../../components/PrimaryButton';
import { TransactionForm } from '../../components/TransactionForm';
import { useAccounts } from '../../hooks/useAccounts';
import { useCategories } from '../../hooks/useCategories';
import { createTransaction } from '../../api/endpoints/transactionApi';
import { TransactionRequest } from '../../types/transaction';
import { spacing } from '../../theme';

type Props = NativeStackScreenProps<RootStackParamList, 'AddExpense'>;

export const AddExpenseScreen: React.FC<Props> = ({ navigation }) => {
  const { accounts } = useAccounts();
  const { categories } = useCategories('EXPENSE');

  const handleSubmit = async (data: TransactionRequest) => {
    await createTransaction(data);
    navigation.goBack();
  };

  return (
    <ScreenContainer scrollable>
      <PrimaryButton
        title="📷 Scan Receipt with OCR"
        onPress={() => navigation.navigate('ReceiptUpload')}
        variant="outline"
        style={{ marginBottom: spacing.md }}
      />
      <TransactionForm
        type="EXPENSE"
        accounts={accounts}
        categories={categories}
        onSubmit={handleSubmit}
        onCancel={() => navigation.goBack()}
      />
    </ScreenContainer>
  );
};
