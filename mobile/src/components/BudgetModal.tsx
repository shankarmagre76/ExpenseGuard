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
import { useCategories } from '../hooks/useCategories';
import { BudgetResponse, BudgetRequest } from '../types/budget';
import { colors, spacing, borderRadius } from '../theme';
import { getCurrentMonth, formatMonthDisplay } from '../utils/dateFormatter';

interface BudgetModalProps {
  visible: boolean;
  budget?: BudgetResponse | null;
  selectedMonth: string;
  onClose: () => void;
  onSubmit: (data: BudgetRequest) => Promise<void>;
}

export const BudgetModal: React.FC<BudgetModalProps> = ({
  visible,
  budget,
  selectedMonth,
  onClose,
  onSubmit,
}) => {
  const { categories, loading: loadingCategories } = useCategories();
  // Filter strictly to EXPENSE categories per Step 8 requirement
  const expenseCategories = categories.filter((c) => c.type === 'EXPENSE');

  const [categoryId, setCategoryId] = useState<string>('');
  const [amount, setAmount] = useState<string>('');
  const [month, setMonth] = useState<string>(selectedMonth);
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (visible) {
      if (budget) {
        setCategoryId(budget.categoryId);
        setAmount(budget.amount.toString());
        setMonth(budget.month);
      } else {
        setCategoryId(expenseCategories.length > 0 ? expenseCategories[0].id : '');
        setAmount('');
        setMonth(selectedMonth || getCurrentMonth());
      }
      setError(null);
    }
  }, [visible, budget, selectedMonth, expenseCategories]);

  const handleSubmit = async () => {
    setError(null);
    if (!categoryId) {
      setError('Please select an expense category.');
      return;
    }

    const numericAmount = parseFloat(amount);
    if (isNaN(numericAmount) || numericAmount <= 0) {
      setError('Budget amount must be a positive number.');
      return;
    }

    if (!month.match(/^\d{4}-(0[1-9]|1[0-2])$/)) {
      setError('Invalid month format. Expected YYYY-MM.');
      return;
    }

    try {
      setSubmitting(true);
      await onSubmit({
        categoryId,
        amount: numericAmount,
        month,
      });
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to save budget');
    } finally {
      setSubmitting(false);
    }
  };

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
            {budget ? 'Edit Budget' : 'Set New Budget'}
          </AppText>

          {error && <ErrorMessage message={error} style={styles.error} />}

          <ScrollView style={styles.formScroll} keyboardShouldPersistTaps="handled">
            <AppText variant="subheader" style={styles.label}>
              Month
            </AppText>
            <View style={styles.readOnlyContainer}>
              <AppText variant="body" color={colors.textPrimary}>
                {formatMonthDisplay(month)} ({month})
              </AppText>
            </View>

            <AppText variant="subheader" style={styles.label}>
              Expense Category
            </AppText>
            {loadingCategories ? (
              <AppText variant="caption" color={colors.textSecondary}>
                Loading categories...
              </AppText>
            ) : expenseCategories.length === 0 ? (
              <AppText variant="caption" color={colors.error}>
                No expense categories available. Please create an expense category first.
              </AppText>
            ) : (
              <View style={styles.categoryPicker}>
                {expenseCategories.map((cat) => {
                  const isSelected = categoryId === cat.id;
                  return (
                    <TouchableOpacity
                      key={cat.id}
                      style={[
                        styles.categoryOption,
                        isSelected && styles.categoryOptionSelected,
                      ]}
                      onPress={() => setCategoryId(cat.id)}
                      disabled={!!budget} // Cannot change category when editing existing budget
                      accessibilityLabel={`Select category ${cat.name}`}
                      accessibilityRole="button"
                    >
                      <AppText
                        variant="body"
                        color={isSelected ? colors.primary : colors.textPrimary}
                        style={isSelected ? styles.selectedText : undefined}
                      >
                        {cat.name}
                      </AppText>
                    </TouchableOpacity>
                  );
                })}
              </View>
            )}

            <AppText variant="subheader" style={styles.label}>
              Monthly Budget Amount
            </AppText>
            <TextInput
              style={styles.input}
              value={amount}
              onChangeText={setAmount}
              placeholder="e.g. 500.00"
              placeholderTextColor={colors.textMuted}
              keyboardType="decimal-pad"
              editable={!submitting}
            />
          </ScrollView>

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
                title={budget ? 'Update Budget' : 'Create Budget'}
                onPress={handleSubmit}
                isLoading={submitting}
                disabled={submitting || expenseCategories.length === 0}
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
  readOnlyContainer: {
    backgroundColor: colors.background,
    padding: spacing.sm,
    borderRadius: borderRadius.sm,
    borderWidth: 1,
    borderColor: colors.border,
    marginBottom: spacing.sm,
  },
  categoryPicker: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: spacing.md,
  },
  categoryOption: {
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
    borderRadius: borderRadius.sm,
    borderWidth: 1,
    borderColor: colors.border,
    marginRight: spacing.xs,
    marginBottom: spacing.xs,
    backgroundColor: colors.background,
  },
  categoryOptionSelected: {
    borderColor: colors.primary,
    backgroundColor: `${colors.primary}1A`,
  },
  selectedText: {
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
