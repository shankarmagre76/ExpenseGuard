import React, { useState, useEffect } from 'react';
import {
  Modal,
  View,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  ScrollView,
} from 'react-native';
import { AppText } from './AppText';
import { PrimaryButton } from './PrimaryButton';
import { TransactionFilters, TransactionType } from '../types/transaction';
import { AccountResponse } from '../types/account';
import { CategoryResponse } from '../types/category';
import { colors, spacing, borderRadius } from '../theme';

interface TransactionFilterModalProps {
  visible: boolean;
  onClose: () => void;
  onApply: (filters: TransactionFilters) => void;
  onReset: () => void;
  activeFilters: TransactionFilters;
  accounts: AccountResponse[];
  categories: CategoryResponse[];
}

export const TransactionFilterModal: React.FC<TransactionFilterModalProps> = ({
  visible,
  onClose,
  onApply,
  onReset,
  activeFilters,
  accounts,
  categories,
}) => {
  const [type, setType] = useState<TransactionType | undefined>(activeFilters.type);
  const [accountId, setAccountId] = useState<string | undefined>(activeFilters.accountId);
  const [categoryId, setCategoryId] = useState<string | undefined>(activeFilters.categoryId);
  const [fromDate, setFromDate] = useState<string>(activeFilters.fromDate || '');
  const [toDate, setToDate] = useState<string>(activeFilters.toDate || '');

  useEffect(() => {
    setType(activeFilters.type);
    setAccountId(activeFilters.accountId);
    setCategoryId(activeFilters.categoryId);
    setFromDate(activeFilters.fromDate || '');
    setToDate(activeFilters.toDate || '');
  }, [activeFilters, visible]);

  const handleApply = () => {
    onApply({
      type,
      accountId,
      categoryId,
      fromDate: fromDate.trim() || undefined,
      toDate: toDate.trim() || undefined,
    });
    onClose();
  };

  const handleReset = () => {
    setType(undefined);
    setAccountId(undefined);
    setCategoryId(undefined);
    setFromDate('');
    setToDate('');
    onReset();
    onClose();
  };

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
      <View style={styles.overlay}>
        <View style={styles.modalCard}>
          <ScrollView showsVerticalScrollIndicator={false}>
            <AppText variant="header" style={styles.title}>
              Filter Transactions
            </AppText>

            {/* Type Filter */}
            <View style={styles.inputGroup}>
              <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
                TRANSACTION TYPE
              </AppText>
              <View style={styles.chipRow}>
                {[
                  { label: 'All', value: undefined },
                  { label: 'Expense', value: 'EXPENSE' },
                  { label: 'Income', value: 'INCOME' },
                ].map((item) => {
                  const isSelected = type === item.value;
                  return (
                    <TouchableOpacity
                      key={item.label}
                      style={[styles.chip, isSelected ? styles.chipSelected : null]}
                      onPress={() => setType(item.value as TransactionType | undefined)}
                    >
                      <AppText
                        variant="caption"
                        bold={isSelected}
                        color={isSelected ? colors.surface : colors.textPrimary}
                      >
                        {item.label}
                      </AppText>
                    </TouchableOpacity>
                  );
                })}
              </View>
            </View>

            {/* Account Filter */}
            <View style={styles.inputGroup}>
              <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
                ACCOUNT
              </AppText>
              <View style={styles.chipRow}>
                <TouchableOpacity
                  style={[styles.chip, !accountId ? styles.chipSelected : null]}
                  onPress={() => setAccountId(undefined)}
                >
                  <AppText variant="caption" bold={!accountId} color={!accountId ? colors.surface : colors.textPrimary}>
                    All Accounts
                  </AppText>
                </TouchableOpacity>
                {accounts.map((acc) => {
                  const isSelected = accountId === acc.id;
                  return (
                    <TouchableOpacity
                      key={acc.id}
                      style={[styles.chip, isSelected ? styles.chipSelected : null]}
                      onPress={() => setAccountId(acc.id)}
                    >
                      <AppText
                        variant="caption"
                        bold={isSelected}
                        color={isSelected ? colors.surface : colors.textPrimary}
                      >
                        {acc.name}
                      </AppText>
                    </TouchableOpacity>
                  );
                })}
              </View>
            </View>

            {/* Category Filter */}
            <View style={styles.inputGroup}>
              <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
                CATEGORY
              </AppText>
              <View style={styles.chipRow}>
                <TouchableOpacity
                  style={[styles.chip, !categoryId ? styles.chipSelected : null]}
                  onPress={() => setCategoryId(undefined)}
                >
                  <AppText variant="caption" bold={!categoryId} color={!categoryId ? colors.surface : colors.textPrimary}>
                    All Categories
                  </AppText>
                </TouchableOpacity>
                {categories.map((cat) => {
                  const isSelected = categoryId === cat.id;
                  return (
                    <TouchableOpacity
                      key={cat.id}
                      style={[styles.chip, isSelected ? styles.chipSelected : null]}
                      onPress={() => setCategoryId(cat.id)}
                    >
                      <AppText
                        variant="caption"
                        bold={isSelected}
                        color={isSelected ? colors.surface : colors.textPrimary}
                      >
                        {cat.name}
                      </AppText>
                    </TouchableOpacity>
                  );
                })}
              </View>
            </View>

            {/* Date Range Filters */}
            <View style={styles.inputGroup}>
              <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
                FROM DATE (YYYY-MM-DD)
              </AppText>
              <TextInput
                style={styles.input}
                placeholder="YYYY-MM-DD"
                placeholderTextColor={colors.textMuted}
                value={fromDate}
                onChangeText={setFromDate}
              />
            </View>

            <View style={styles.inputGroup}>
              <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
                TO DATE (YYYY-MM-DD)
              </AppText>
              <TextInput
                style={styles.input}
                placeholder="YYYY-MM-DD"
                placeholderTextColor={colors.textMuted}
                value={toDate}
                onChangeText={setToDate}
              />
            </View>

            {/* Buttons */}
            <View style={styles.actionRow}>
              <PrimaryButton
                title="Reset Filters"
                onPress={handleReset}
                variant="outline"
                style={styles.halfBtn}
              />
              <PrimaryButton
                title="Apply Filters"
                onPress={handleApply}
                style={styles.halfBtn}
              />
            </View>
          </ScrollView>
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
    maxHeight: '90%',
  },
  title: {
    marginBottom: spacing.md,
    textAlign: 'center',
  },
  inputGroup: {
    marginBottom: spacing.md,
  },
  label: {
    marginBottom: spacing.xs,
    fontWeight: '600',
  },
  chipRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.xs,
  },
  chip: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs,
    borderRadius: borderRadius.full,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.background,
  },
  chipSelected: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
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
  actionRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: spacing.sm,
    marginTop: spacing.md,
  },
  halfBtn: {
    flex: 1,
  },
});
