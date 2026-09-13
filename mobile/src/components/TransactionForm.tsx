import React, { useState, useEffect } from 'react';
import {
  View,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  ScrollView,
} from 'react-native';
import { AppText } from './AppText';
import { PrimaryButton } from './PrimaryButton';
import { ErrorMessage } from './ErrorMessage';
import { TransactionResponse, TransactionRequest, TransactionType } from '../types/transaction';
import { AccountResponse } from '../types/account';
import { CategoryResponse } from '../types/category';
import { colors, spacing, borderRadius } from '../theme';
import { getTodayISODate } from '../utils/dateFormatter';

interface TransactionFormProps {
  type: TransactionType;
  initialValues?: TransactionResponse | null;
  accounts: AccountResponse[];
  categories: CategoryResponse[];
  onSubmit: (data: TransactionRequest) => Promise<void>;
  onCancel: () => void;
  title?: string;
}

export const TransactionForm: React.FC<TransactionFormProps> = ({
  type,
  initialValues,
  accounts,
  categories,
  onSubmit,
  onCancel,
  title,
}) => {
  const isEditing = Boolean(initialValues);
  const filteredCategories = categories.filter(
    (cat) => cat.type === (type === 'INCOME' ? 'INCOME' : 'EXPENSE')
  );

  const [amount, setAmount] = useState('');
  const [accountId, setAccountId] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [transactionDate, setTransactionDate] = useState(getTodayISODate());
  const [description, setDescription] = useState('');

  const [amountError, setAmountError] = useState<string | null>(null);
  const [accountError, setAccountError] = useState<string | null>(null);
  const [categoryError, setCategoryError] = useState<string | null>(null);
  const [dateError, setDateError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (initialValues) {
      setAmount(String(initialValues.amount));
      setAccountId(initialValues.accountId);
      setCategoryId(initialValues.categoryId);
      setTransactionDate(initialValues.transactionDate || getTodayISODate());
      setDescription(initialValues.description || '');
    } else {
      setAmount('');
      setAccountId(accounts.length > 0 ? accounts[0].id : '');
      setCategoryId(filteredCategories.length > 0 ? filteredCategories[0].id : '');
      setTransactionDate(getTodayISODate());
      setDescription('');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialValues, accounts, categories, type]);

  const validate = (): boolean => {
    let isValid = true;
    setAmountError(null);
    setAccountError(null);
    setCategoryError(null);
    setDateError(null);
    setSubmitError(null);

    const parsedAmount = parseFloat(amount);
    if (!amount || isNaN(parsedAmount) || parsedAmount <= 0) {
      setAmountError('Please enter a valid amount greater than zero');
      isValid = false;
    }

    if (!accountId) {
      setAccountError('Please select a financial account');
      isValid = false;
    }

    if (!categoryId) {
      setCategoryError(`Please select a ${type.toLowerCase()} category`);
      isValid = false;
    }

    if (!transactionDate.trim() || !/^\d{4}-\d{2}-\d{2}$/.test(transactionDate.trim())) {
      setDateError('Date must be in YYYY-MM-DD format');
      isValid = false;
    }

    if (description.length > 255) {
      setSubmitError('Description cannot exceed 255 characters');
      isValid = false;
    }

    return isValid;
  };

  const handleSubmit = async () => {
    if (!validate()) return;

    setIsSubmitting(true);
    setSubmitError(null);

    try {
      await onSubmit({
        accountId,
        categoryId,
        type,
        amount: parseFloat(amount),
        transactionDate: transactionDate.trim(),
        description: description.trim() || undefined,
      });
    } catch (err: any) {
      setSubmitError(err?.message || 'Failed to save transaction.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const themeColor = type === 'INCOME' ? colors.success : colors.error;

  return (
    <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>
      {/* Form Header */}
      <View style={styles.header}>
        <AppText variant="title" color={themeColor} style={styles.headerTitle}>
          {title || (isEditing ? `Edit ${type}` : `Add ${type}`)}
        </AppText>
        <AppText variant="body" color={colors.textSecondary}>
          {type === 'INCOME' ? 'Record earnings or incoming funds' : 'Record an expense or payment'}
        </AppText>
      </View>

      {submitError ? (
        <ErrorMessage message={submitError} onRetry={() => setSubmitError(null)} />
      ) : null}

      <View style={styles.formCard}>
        {/* Amount Input */}
        <View style={styles.inputGroup}>
          <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
            AMOUNT ($)
          </AppText>
          <TextInput
            style={[styles.amountInput, amountError ? styles.inputInvalid : null]}
            placeholder="0.00"
            placeholderTextColor={colors.textMuted}
            value={amount}
            onChangeText={(text) => {
              setAmount(text);
              if (amountError) setAmountError(null);
            }}
            keyboardType="decimal-pad"
            editable={!isSubmitting}
          />
          {amountError ? (
            <AppText variant="caption" color={colors.error} style={styles.fieldError}>
              {amountError}
            </AppText>
          ) : null}
        </View>

        {/* Account Selector */}
        <View style={styles.inputGroup}>
          <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
            SELECT ACCOUNT
          </AppText>
          {accounts.length === 0 ? (
            <AppText variant="body" color={colors.error}>
              No accounts available. Please create an account first.
            </AppText>
          ) : (
            <View style={styles.chipRow}>
              {accounts.map((acc) => {
                const isSelected = accountId === acc.id;
                return (
                  <TouchableOpacity
                    key={acc.id}
                    style={[styles.chip, isSelected ? styles.chipSelected : null]}
                    onPress={() => {
                      setAccountId(acc.id);
                      if (accountError) setAccountError(null);
                    }}
                    disabled={isSubmitting}
                  >
                    <AppText
                      variant="body"
                      bold={isSelected}
                      color={isSelected ? colors.surface : colors.textPrimary}
                    >
                      {acc.name}
                    </AppText>
                  </TouchableOpacity>
                );
              })}
            </View>
          )}
          {accountError ? (
            <AppText variant="caption" color={colors.error} style={styles.fieldError}>
              {accountError}
            </AppText>
          ) : null}
        </View>

        {/* Category Selector */}
        <View style={styles.inputGroup}>
          <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
            SELECT CATEGORY ({type})
          </AppText>
          {filteredCategories.length === 0 ? (
            <AppText variant="body" color={colors.warning}>
              No {type.toLowerCase()} categories found. Please create one in Categories tab.
            </AppText>
          ) : (
            <View style={styles.chipRow}>
              {filteredCategories.map((cat) => {
                const isSelected = categoryId === cat.id;
                return (
                  <TouchableOpacity
                    key={cat.id}
                    style={[
                      styles.chip,
                      isSelected ? { backgroundColor: themeColor, borderColor: themeColor } : null,
                    ]}
                    onPress={() => {
                      setCategoryId(cat.id);
                      if (categoryError) setCategoryError(null);
                    }}
                    disabled={isSubmitting}
                  >
                    <AppText
                      variant="body"
                      bold={isSelected}
                      color={isSelected ? colors.surface : colors.textPrimary}
                    >
                      {cat.name}
                    </AppText>
                  </TouchableOpacity>
                );
              })}
            </View>
          )}
          {categoryError ? (
            <AppText variant="caption" color={colors.error} style={styles.fieldError}>
              {categoryError}
            </AppText>
          ) : null}
        </View>

        {/* Date Input */}
        <View style={styles.inputGroup}>
          <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
            TRANSACTION DATE (YYYY-MM-DD)
          </AppText>
          <TextInput
            style={[styles.input, dateError ? styles.inputInvalid : null]}
            placeholder="YYYY-MM-DD"
            placeholderTextColor={colors.textMuted}
            value={transactionDate}
            onChangeText={(text) => {
              setTransactionDate(text);
              if (dateError) setDateError(null);
            }}
            editable={!isSubmitting}
          />
          {dateError ? (
            <AppText variant="caption" color={colors.error} style={styles.fieldError}>
              {dateError}
            </AppText>
          ) : null}
        </View>

        {/* Description Input */}
        <View style={styles.inputGroup}>
          <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
            DESCRIPTION (OPTIONAL)
          </AppText>
          <TextInput
            style={[styles.input, styles.textArea]}
            placeholder="What was this transaction for?"
            placeholderTextColor={colors.textMuted}
            value={description}
            onChangeText={setDescription}
            multiline
            numberOfLines={3}
            editable={!isSubmitting}
          />
        </View>

        {/* Action Buttons */}
        <View style={styles.buttonRow}>
          <PrimaryButton
            title="Cancel"
            onPress={onCancel}
            variant="outline"
            disabled={isSubmitting}
            style={styles.halfBtn}
          />
          <PrimaryButton
            title={isEditing ? 'Update Transaction' : `Save ${type}`}
            onPress={handleSubmit}
            isLoading={isSubmitting}
            disabled={isSubmitting || accounts.length === 0 || filteredCategories.length === 0}
            style={styles.halfBtn}
          />
        </View>
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    paddingVertical: spacing.md,
  },
  header: {
    marginBottom: spacing.md,
  },
  headerTitle: {
    marginBottom: spacing.xs,
  },
  formCard: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.border,
  },
  inputGroup: {
    marginBottom: spacing.md,
  },
  label: {
    marginBottom: spacing.xs,
    fontWeight: '600',
  },
  amountInput: {
    backgroundColor: colors.background,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: borderRadius.md,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.md,
    fontSize: 24,
    fontWeight: '700',
    color: colors.textPrimary,
  },
  input: {
    backgroundColor: colors.background,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: borderRadius.md,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    fontSize: 16,
    color: colors.textPrimary,
  },
  textArea: {
    height: 80,
    textAlignVertical: 'top',
  },
  inputInvalid: {
    borderColor: colors.error,
  },
  fieldError: {
    marginTop: 4,
  },
  chipRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.xs,
  },
  chip: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: borderRadius.md,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.background,
  },
  chipSelected: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: spacing.sm,
    marginTop: spacing.md,
  },
  halfBtn: {
    flex: 1,
  },
});
