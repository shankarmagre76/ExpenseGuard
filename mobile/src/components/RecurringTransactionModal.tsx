import React, { useState, useEffect } from 'react';
import {
  Modal,
  View,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  TextInput,
} from 'react-native';
import { AppText } from './AppText';
import { PrimaryButton } from './PrimaryButton';
import { ErrorMessage } from './ErrorMessage';
import { useAccounts } from '../hooks/useAccounts';
import { useCategories } from '../hooks/useCategories';
import {
  RecurringTransactionResponse,
  RecurringTransactionRequest,
  RecurrenceFrequency,
} from '../types/recurringTransaction';
import { TransactionType } from '../types/transaction';
import { colors, spacing, borderRadius } from '../theme';
import { getTodayISODate } from '../utils/dateFormatter';

interface RecurringTransactionModalProps {
  visible: boolean;
  item?: RecurringTransactionResponse | null;
  onClose: () => void;
  onSubmit: (data: RecurringTransactionRequest) => Promise<void>;
}

export const RecurringTransactionModal: React.FC<RecurringTransactionModalProps> = ({
  visible,
  item,
  onClose,
  onSubmit,
}) => {
  const { accounts, loading: loadingAccounts } = useAccounts();
  const { categories, loading: loadingCategories } = useCategories();

  const [type, setType] = useState<TransactionType>('EXPENSE');
  const [accountId, setAccountId] = useState<string>('');
  const [categoryId, setCategoryId] = useState<string>('');
  const [amount, setAmount] = useState<string>('');
  const [description, setDescription] = useState<string>('');
  const [frequency, setFrequency] = useState<RecurrenceFrequency>('MONTHLY');
  const [startDate, setStartDate] = useState<string>(getTodayISODate());
  const [nextRunDate, setNextRunDate] = useState<string>(getTodayISODate());
  const [endDate, setEndDate] = useState<string>('');

  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const filteredCategories = categories.filter((c) => c.type === type);

  useEffect(() => {
    if (visible) {
      if (item) {
        setType(item.type);
        setAccountId(item.accountId);
        setCategoryId(item.categoryId);
        setAmount(item.amount.toString());
        setDescription(item.description || '');
        setFrequency(item.frequency);
        setStartDate(item.startDate);
        setNextRunDate(item.nextRunDate);
        setEndDate(item.endDate || '');
      } else {
        const today = getTodayISODate();
        setType('EXPENSE');
        setAccountId(accounts.length > 0 ? accounts[0].id : '');
        setCategoryId('');
        setAmount('');
        setDescription('');
        setFrequency('MONTHLY');
        setStartDate(today);
        setNextRunDate(today);
        setEndDate('');
      }
      setError(null);
    }
  }, [visible, item, accounts]);

  // Update default selected category when type changes
  useEffect(() => {
    if (filteredCategories.length > 0 && (!categoryId || !filteredCategories.some((c) => c.id === categoryId))) {
      setCategoryId(filteredCategories[0].id);
    }
  }, [type, filteredCategories, categoryId]);

  const handleSubmit = async () => {
    setError(null);

    if (!accountId) {
      setError('Please select an account.');
      return;
    }

    if (!categoryId) {
      setError('Please select a category.');
      return;
    }

    const numericAmount = parseFloat(amount);
    if (isNaN(numericAmount) || numericAmount <= 0) {
      setError('Amount must be a positive number.');
      return;
    }

    const isoDateRegex = /^\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])$/;
    if (!startDate.match(isoDateRegex)) {
      setError('Start date must be in YYYY-MM-DD format.');
      return;
    }

    if (!nextRunDate.match(isoDateRegex)) {
      setError('Next run date must be in YYYY-MM-DD format.');
      return;
    }

    if (endDate && !endDate.match(isoDateRegex)) {
      setError('End date must be in YYYY-MM-DD format.');
      return;
    }

    try {
      setSubmitting(true);
      await onSubmit({
        accountId,
        categoryId,
        type,
        amount: numericAmount,
        description: description.trim() || undefined,
        frequency,
        startDate,
        nextRunDate,
        endDate: endDate.trim() || undefined,
      });
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to save recurring transaction.');
    } finally {
      setSubmitting(false);
    }
  };

  const frequencies: RecurrenceFrequency[] = ['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY'];

  return (
    <Modal
      visible={visible}
      animationType="slide"
      transparent
      onRequestClose={onClose}
    >
      <View style={styles.overlay}>
        <View style={styles.modalCard}>
          <AppText variant="header" style={styles.title}>
            {item ? 'Edit Recurring Schedule' : 'New Recurring Transaction'}
          </AppText>

          {error && <ErrorMessage message={error} style={styles.error} />}

          <ScrollView style={styles.formScroll} keyboardShouldPersistTaps="handled">
            {/* Type Selector */}
            <AppText variant="subheader" style={styles.label}>
              Transaction Type
            </AppText>
            <View style={styles.typeContainer}>
              <TouchableOpacity
                style={[
                  styles.typeTab,
                  type === 'EXPENSE' && styles.typeExpenseActive,
                ]}
                onPress={() => setType('EXPENSE')}
                accessibilityLabel="Select Expense type"
                accessibilityRole="button"
              >
                <AppText
                  variant="body"
                  bold={type === 'EXPENSE'}
                  color={type === 'EXPENSE' ? colors.surface : colors.textPrimary}
                >
                  Expense
                </AppText>
              </TouchableOpacity>
              <TouchableOpacity
                style={[
                  styles.typeTab,
                  type === 'INCOME' && styles.typeIncomeActive,
                ]}
                onPress={() => setType('INCOME')}
                accessibilityLabel="Select Income type"
                accessibilityRole="button"
              >
                <AppText
                  variant="body"
                  bold={type === 'INCOME'}
                  color={type === 'INCOME' ? colors.surface : colors.textPrimary}
                >
                  Income
                </AppText>
              </TouchableOpacity>
            </View>

            {/* Account Selector */}
            <AppText variant="subheader" style={styles.label}>
              Account
            </AppText>
            {loadingAccounts ? (
              <AppText variant="caption">Loading accounts...</AppText>
            ) : (
              <View style={styles.pickerGrid}>
                {accounts.map((acc) => {
                  const isSel = accountId === acc.id;
                  return (
                    <TouchableOpacity
                      key={acc.id}
                      style={[styles.chip, isSel && styles.chipSelected]}
                      onPress={() => setAccountId(acc.id)}
                      accessibilityLabel={`Select account ${acc.name}`}
                      accessibilityRole="button"
                    >
                      <AppText
                        variant="body"
                        color={isSel ? colors.primary : colors.textPrimary}
                        style={isSel ? styles.boldText : undefined}
                      >
                        {acc.name}
                      </AppText>
                    </TouchableOpacity>
                  );
                })}
              </View>
            )}

            {/* Category Selector */}
            <AppText variant="subheader" style={styles.label}>
              Category
            </AppText>
            {loadingCategories ? (
              <AppText variant="caption">Loading categories...</AppText>
            ) : filteredCategories.length === 0 ? (
              <AppText variant="caption" color={colors.error}>
                No {type.toLowerCase()} categories found.
              </AppText>
            ) : (
              <View style={styles.pickerGrid}>
                {filteredCategories.map((cat) => {
                  const isSel = categoryId === cat.id;
                  return (
                    <TouchableOpacity
                      key={cat.id}
                      style={[styles.chip, isSel && styles.chipSelected]}
                      onPress={() => setCategoryId(cat.id)}
                      accessibilityLabel={`Select category ${cat.name}`}
                      accessibilityRole="button"
                    >
                      <AppText
                        variant="body"
                        color={isSel ? colors.primary : colors.textPrimary}
                        style={isSel ? styles.boldText : undefined}
                      >
                        {cat.name}
                      </AppText>
                    </TouchableOpacity>
                  );
                })}
              </View>
            )}

            {/* Frequency Selector */}
            <AppText variant="subheader" style={styles.label}>
              Recurrence Frequency
            </AppText>
            <View style={styles.pickerGrid}>
              {frequencies.map((freq) => {
                const isSel = frequency === freq;
                return (
                  <TouchableOpacity
                    key={freq}
                    style={[styles.chip, isSel && styles.chipSelected]}
                    onPress={() => setFrequency(freq)}
                    accessibilityLabel={`Select frequency ${freq}`}
                    accessibilityRole="button"
                  >
                    <AppText
                      variant="body"
                      color={isSel ? colors.primary : colors.textPrimary}
                      style={isSel ? styles.boldText : undefined}
                    >
                      {freq}
                    </AppText>
                  </TouchableOpacity>
                );
              })}
            </View>

            {/* Amount Input */}
            <AppText variant="subheader" style={styles.label}>
              Amount
            </AppText>
            <TextInput
              style={styles.input}
              value={amount}
              onChangeText={setAmount}
              placeholder="e.g. 120.00"
              placeholderTextColor={colors.textMuted}
              keyboardType="decimal-pad"
              editable={!submitting}
            />

            {/* Description Input */}
            <AppText variant="subheader" style={styles.label}>
              Description (Optional)
            </AppText>
            <TextInput
              style={styles.input}
              value={description}
              onChangeText={setDescription}
              placeholder="e.g. Monthly Netflix Subscription"
              placeholderTextColor={colors.textMuted}
              editable={!submitting}
            />

            {/* Start Date */}
            <AppText variant="subheader" style={styles.label}>
              Start Date (YYYY-MM-DD)
            </AppText>
            <TextInput
              style={styles.input}
              value={startDate}
              onChangeText={setStartDate}
              placeholder="YYYY-MM-DD"
              placeholderTextColor={colors.textMuted}
              editable={!submitting}
            />

            {/* Next Run Date */}
            <AppText variant="subheader" style={styles.label}>
              Next Run Date (YYYY-MM-DD)
            </AppText>
            <TextInput
              style={styles.input}
              value={nextRunDate}
              onChangeText={setNextRunDate}
              placeholder="YYYY-MM-DD"
              placeholderTextColor={colors.textMuted}
              editable={!submitting}
            />

            {/* End Date */}
            <AppText variant="subheader" style={styles.label}>
              End Date (Optional YYYY-MM-DD)
            </AppText>
            <TextInput
              style={styles.input}
              value={endDate}
              onChangeText={setEndDate}
              placeholder="YYYY-MM-DD"
              placeholderTextColor={colors.textMuted}
              editable={!submitting}
            />
          </ScrollView>

          {/* Action Buttons */}
          <View style={styles.actionButtons}>
            <TouchableOpacity
              style={styles.cancelButton}
              onPress={onClose}
              disabled={submitting}
            >
              <AppText variant="body" color={colors.textSecondary}>
                Cancel
              </AppText>
            </TouchableOpacity>
            <View style={styles.submitContainer}>
              <PrimaryButton
                title={item ? 'Update' : 'Create'}
                onPress={handleSubmit}
                isLoading={submitting}
                disabled={submitting}
              />
            </View>
          </View>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    padding: spacing.md,
  },
  modalCard: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.lg,
    maxHeight: '85%',
  },
  title: {
    marginBottom: spacing.md,
  },
  error: {
    marginBottom: spacing.md,
  },
  formScroll: {
    marginBottom: spacing.md,
  },
  label: {
    marginBottom: spacing.xs,
    marginTop: spacing.sm,
  },
  typeContainer: {
    flexDirection: 'row',
    backgroundColor: colors.background,
    borderRadius: borderRadius.md,
    padding: 2,
    borderWidth: 1,
    borderColor: colors.border,
    marginBottom: spacing.sm,
  },
  typeTab: {
    flex: 1,
    paddingVertical: spacing.sm,
    alignItems: 'center',
    borderRadius: borderRadius.sm,
  },
  typeExpenseActive: {
    backgroundColor: colors.error,
  },
  typeIncomeActive: {
    backgroundColor: colors.success,
  },
  pickerGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: spacing.sm,
  },
  chip: {
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
    borderRadius: borderRadius.sm,
    borderWidth: 1,
    borderColor: colors.border,
    marginRight: spacing.xs,
    marginBottom: spacing.xs,
    backgroundColor: colors.background,
  },
  chipSelected: {
    borderColor: colors.primary,
    backgroundColor: `${colors.primary}1A`,
  },
  boldText: {
    fontWeight: '700',
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
    marginBottom: spacing.sm,
  },
  actionButtons: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-end',
    marginTop: spacing.sm,
  },
  cancelButton: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    marginRight: spacing.sm,
  },
  submitContainer: {
    minWidth: 140,
  },
});
