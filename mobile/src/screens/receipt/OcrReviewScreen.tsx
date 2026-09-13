import React, { useState } from 'react';
import {
  View,
  StyleSheet,
  ScrollView,
  TextInput,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { PrimaryButton } from '../../components/PrimaryButton';
import { ErrorMessage } from '../../components/ErrorMessage';
import { LoadingIndicator } from '../../components/LoadingIndicator';
import { useAccounts } from '../../hooks/useAccounts';
import { useCategories } from '../../hooks/useCategories';
import { useReceipts } from '../../hooks/useReceipts';
import { createTransaction } from '../../api/endpoints/transactionApi';
import { ReceiptResponse } from '../../types/receipt';
import { colors, spacing, borderRadius } from '../../theme';
import { formatCurrency } from '../../utils/currencyFormatter';
import { getTodayISODate } from '../../utils/dateFormatter';

export const OcrReviewScreen: React.FC<any> = ({ route, navigation }) => {
  const receipt: ReceiptResponse = route.params?.receipt;

  const { confirmReceiptData } = useReceipts();
  const { accounts, loading: loadingAccounts } = useAccounts();
  const { categories, loading: loadingCategories } = useCategories();

  const expenseCategories = categories.filter((c) => c.type === 'EXPENSE');

  // Pre-populate fields from backend OCR extraction
  const [merchantName, setMerchantName] = useState<string>(receipt?.merchantName || '');
  const [transactionDate, setTransactionDate] = useState<string>(
    receipt?.extractedDate || getTodayISODate()
  );
  const [amount, setAmount] = useState<string>(
    receipt?.extractedAmount !== undefined && receipt?.extractedAmount !== null
      ? receipt.extractedAmount.toString()
      : ''
  );
  const [accountId, setAccountId] = useState<string>(accounts.length > 0 ? accounts[0].id : '');
  const [categoryId, setCategoryId] = useState<string>(
    expenseCategories.length > 0 ? expenseCategories[0].id : ''
  );
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const confidencePct =
    receipt?.confidence !== undefined && receipt?.confidence !== null
      ? Math.round(receipt.confidence * 100)
      : null;

  const handleConfirmAndCreateTransaction = async () => {
    setError(null);

    if (!accountId) {
      setError('Please select an account for this transaction.');
      return;
    }

    if (!categoryId) {
      setError('Please select an expense category.');
      return;
    }

    const numericAmount = parseFloat(amount);
    if (isNaN(numericAmount) || numericAmount <= 0) {
      setError('Amount must be a positive number.');
      return;
    }

    if (!transactionDate.match(/^\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])$/)) {
      setError('Transaction date must be in YYYY-MM-DD format.');
      return;
    }

    try {
      setSubmitting(true);

      // 1. Confirm OCR metadata on backend receipt entity
      await confirmReceiptData(receipt.id, {
        merchantName: merchantName.trim(),
        transactionDate,
        amount: numericAmount,
      });

      // 2. Explicit user confirmation: Create actual transaction on backend
      await createTransaction({
        accountId,
        categoryId,
        type: 'EXPENSE',
        amount: numericAmount,
        transactionDate,
        description: merchantName.trim() ? `Receipt: ${merchantName.trim()}` : 'Receipt Expense',
      });

      Alert.alert(
        'Transaction Created',
        'Receipt OCR verified and expense transaction created successfully!',
        [
          {
            text: 'OK',
            onPress: () => navigation.navigate('Main'),
          },
        ]
      );
    } catch (err: any) {
      setError(err.message || 'Failed to create transaction from receipt.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <ScreenContainer scrollable>
      <ScrollView contentContainerStyle={styles.container}>
        <AppText variant="header" style={styles.title}>
          Review OCR Data
        </AppText>
        <AppText variant="body" color={colors.textSecondary} style={styles.subtitle}>
          Verify extracted values below. Edit any details before confirming transaction creation.
        </AppText>

        {error && <ErrorMessage message={error} style={styles.errorBanner} />}

        {/* OCR Raw Extraction Card */}
        <View style={styles.extractedCard}>
          <AppText variant="caption" color={colors.primary} style={styles.badgeLabel}>
            OCR EXTRACTION RESULT
          </AppText>

          <View style={styles.infoRow}>
            <AppText variant="body" color={colors.textSecondary}>
              Detected Merchant:
            </AppText>
            <AppText variant="body" bold>
              {receipt?.merchantName || 'Not detected'}
            </AppText>
          </View>

          <View style={styles.infoRow}>
            <AppText variant="body" color={colors.textSecondary}>
              Extracted Date:
            </AppText>
            <AppText variant="body" bold>
              {receipt?.extractedDate || 'Not detected'}
            </AppText>
          </View>

          <View style={styles.infoRow}>
            <AppText variant="body" color={colors.textSecondary}>
              Extracted Amount:
            </AppText>
            <AppText variant="body" bold color={colors.error}>
              {receipt?.extractedAmount !== null && receipt?.extractedAmount !== undefined
                ? formatCurrency(receipt.extractedAmount)
                : 'Not detected'}
            </AppText>
          </View>

          {confidencePct !== null && (
            <View style={styles.infoRow}>
              <AppText variant="body" color={colors.textSecondary}>
                Extraction Confidence:
              </AppText>
              <AppText
                variant="body"
                bold
                color={confidencePct >= 80 ? colors.success : colors.warning}
              >
                {confidencePct}%
              </AppText>
            </View>
          )}
        </View>

        {/* Transaction Parameters Form */}
        <View style={styles.formCard}>
          <AppText variant="subheader" style={styles.formTitle}>
            Transaction Parameters
          </AppText>

          {/* Merchant / Description */}
          <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
            MERCHANT / DESCRIPTION
          </AppText>
          <TextInput
            style={styles.input}
            value={merchantName}
            onChangeText={setMerchantName}
            placeholder="e.g. Walmart, Target"
            placeholderTextColor={colors.textMuted}
            editable={!submitting}
          />

          {/* Amount */}
          <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
            AMOUNT
          </AppText>
          <TextInput
            style={styles.input}
            value={amount}
            onChangeText={setAmount}
            placeholder="e.g. 45.99"
            placeholderTextColor={colors.textMuted}
            keyboardType="decimal-pad"
            editable={!submitting}
          />

          {/* Transaction Date */}
          <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
            TRANSACTION DATE (YYYY-MM-DD)
          </AppText>
          <TextInput
            style={styles.input}
            value={transactionDate}
            onChangeText={setTransactionDate}
            placeholder="YYYY-MM-DD"
            placeholderTextColor={colors.textMuted}
            editable={!submitting}
          />

          {/* Account Picker */}
          <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
            SELECT ACCOUNT
          </AppText>
          {loadingAccounts ? (
            <AppText variant="caption">Loading accounts...</AppText>
          ) : (
            <View style={styles.chipRow}>
              {accounts.map((acc) => {
                const isSel = accountId === acc.id || (!accountId && accounts[0].id === acc.id);
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

          {/* Category Picker */}
          <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
            SELECT EXPENSE CATEGORY
          </AppText>
          {loadingCategories ? (
            <AppText variant="caption">Loading categories...</AppText>
          ) : (
            <View style={styles.chipRow}>
              {expenseCategories.map((cat) => {
                const isSel = categoryId === cat.id || (!categoryId && expenseCategories[0]?.id === cat.id);
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
        </View>

        {/* Action Buttons */}
        <View style={styles.actionRow}>
          <PrimaryButton
            title="Cancel"
            onPress={() => navigation.goBack()}
            variant="outline"
            disabled={submitting}
            style={styles.halfBtn}
          />
          <PrimaryButton
            title="Confirm & Create Transaction"
            onPress={handleConfirmAndCreateTransaction}
            isLoading={submitting}
            disabled={submitting}
            style={styles.halfBtn}
          />
        </View>

        {submitting && <LoadingIndicator message="Verifying OCR & creating transaction..." />}
      </ScrollView>
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  container: {
    paddingBottom: spacing.xl,
  },
  title: {
    marginBottom: spacing.xs,
  },
  subtitle: {
    marginBottom: spacing.lg,
  },
  errorBanner: {
    marginBottom: spacing.md,
  },
  extractedCard: {
    backgroundColor: colors.surface,
    padding: spacing.md,
    borderRadius: borderRadius.lg,
    borderWidth: 1,
    borderColor: colors.border,
    marginBottom: spacing.lg,
  },
  badgeLabel: {
    fontWeight: '700',
    letterSpacing: 1,
    marginBottom: spacing.sm,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: spacing.xs,
    borderBottomWidth: 1,
    borderBottomColor: colors.background,
  },
  formCard: {
    backgroundColor: colors.surface,
    padding: spacing.md,
    borderRadius: borderRadius.lg,
    borderWidth: 1,
    borderColor: colors.border,
    marginBottom: spacing.lg,
  },
  formTitle: {
    fontWeight: '700',
    marginBottom: spacing.md,
  },
  label: {
    fontWeight: '600',
    marginTop: spacing.sm,
    marginBottom: spacing.xs,
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
  chipRow: {
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
  actionRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: spacing.md,
  },
  halfBtn: {
    flex: 1,
  },
});
