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
import { ErrorMessage } from './ErrorMessage';
import { AccountResponse, AccountRequest, AccountType } from '../types/account';
import { colors, spacing, borderRadius } from '../theme';

interface AccountModalProps {
  visible: boolean;
  onClose: () => void;
  onSubmit: (data: AccountRequest) => Promise<void>;
  accountToEdit?: AccountResponse | null;
}

const ACCOUNT_TYPES: { label: string; value: AccountType }[] = [
  { label: 'Cash', value: 'CASH' },
  { label: 'Bank Account', value: 'BANK' },
  { label: 'Savings', value: 'SAVINGS' },
  { label: 'Credit Card', value: 'CREDIT_CARD' },
  { label: 'Digital Wallet', value: 'WALLET' },
];

export const AccountModal: React.FC<AccountModalProps> = ({
  visible,
  onClose,
  onSubmit,
  accountToEdit,
}) => {
  const isEditing = Boolean(accountToEdit);

  const [name, setName] = useState('');
  const [type, setType] = useState<AccountType>('BANK');
  const [openingBalance, setOpeningBalance] = useState('0.00');
  const [currency, setCurrency] = useState('USD');

  const [nameError, setNameError] = useState<string | null>(null);
  const [balanceError, setBalanceError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (accountToEdit) {
      setName(accountToEdit.name);
      setType(accountToEdit.type);
      setOpeningBalance(String(accountToEdit.balance));
      setCurrency(accountToEdit.currency || 'USD');
    } else {
      setName('');
      setType('BANK');
      setOpeningBalance('0.00');
      setCurrency('USD');
    }
    setNameError(null);
    setBalanceError(null);
    setSubmitError(null);
  }, [accountToEdit, visible]);

  const validate = (): boolean => {
    let isValid = true;
    setNameError(null);
    setBalanceError(null);
    setSubmitError(null);

    if (!name.trim()) {
      setNameError('Account name is required');
      isValid = false;
    } else if (name.trim().length > 100) {
      setNameError('Name cannot exceed 100 characters');
      isValid = false;
    }

    if (!isEditing) {
      const parsed = parseFloat(openingBalance);
      if (isNaN(parsed)) {
        setBalanceError('Please enter a valid numeric balance');
        isValid = false;
      }
    }

    return isValid;
  };

  const handleSubmit = async () => {
    if (!validate()) return;

    setIsSubmitting(true);
    setSubmitError(null);

    try {
      await onSubmit({
        name: name.trim(),
        type,
        openingBalance: isEditing ? 0 : parseFloat(openingBalance) || 0,
        currency: currency.trim() || 'USD',
      });
      onClose();
    } catch (err: any) {
      setSubmitError(err?.message || 'Failed to save account.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
      <View style={styles.overlay}>
        <View style={styles.modalCard}>
          <ScrollView showsVerticalScrollIndicator={false}>
            <AppText variant="header" style={styles.title}>
              {isEditing ? 'Edit Account' : 'Create New Account'}
            </AppText>

            {submitError ? (
              <ErrorMessage message={submitError} onRetry={() => setSubmitError(null)} />
            ) : null}

            {/* Name Input */}
            <View style={styles.inputGroup}>
              <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
                ACCOUNT NAME
              </AppText>
              <TextInput
                style={[styles.input, nameError ? styles.inputInvalid : null]}
                placeholder="e.g., Chase Checking"
                placeholderTextColor={colors.textMuted}
                value={name}
                onChangeText={(text) => {
                  setName(text);
                  if (nameError) setNameError(null);
                }}
                editable={!isSubmitting}
              />
              {nameError ? (
                <AppText variant="caption" color={colors.error} style={styles.fieldError}>
                  {nameError}
                </AppText>
              ) : null}
            </View>

            {/* Type Selector */}
            <View style={styles.inputGroup}>
              <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
                ACCOUNT TYPE
              </AppText>
              <View style={styles.chipRow}>
                {ACCOUNT_TYPES.map((item) => {
                  const isSelected = type === item.value;
                  return (
                    <TouchableOpacity
                      key={item.value}
                      style={[styles.chip, isSelected ? styles.chipSelected : null]}
                      onPress={() => setType(item.value)}
                      disabled={isSubmitting}
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

            {/* Opening Balance (Only for Creation) */}
            {!isEditing ? (
              <View style={styles.inputGroup}>
                <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
                  OPENING BALANCE
                </AppText>
                <TextInput
                  style={[styles.input, balanceError ? styles.inputInvalid : null]}
                  placeholder="0.00"
                  placeholderTextColor={colors.textMuted}
                  value={openingBalance}
                  onChangeText={(text) => {
                    setOpeningBalance(text);
                    if (balanceError) setBalanceError(null);
                  }}
                  keyboardType="decimal-pad"
                  editable={!isSubmitting}
                />
                {balanceError ? (
                  <AppText variant="caption" color={colors.error} style={styles.fieldError}>
                    {balanceError}
                  </AppText>
                ) : null}
              </View>
            ) : null}

            {/* Currency Input */}
            <View style={styles.inputGroup}>
              <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
                CURRENCY CODE (USD, EUR, INR)
              </AppText>
              <TextInput
                style={styles.input}
                placeholder="USD"
                placeholderTextColor={colors.textMuted}
                value={currency}
                onChangeText={setCurrency}
                autoCapitalize="characters"
                maxLength={3}
                editable={!isSubmitting}
              />
            </View>

            {/* Action Buttons */}
            <View style={styles.actionRow}>
              <PrimaryButton
                title="Cancel"
                onPress={onClose}
                variant="outline"
                disabled={isSubmitting}
                style={styles.halfBtn}
              />
              <PrimaryButton
                title={isEditing ? 'Save Changes' : 'Create Account'}
                onPress={handleSubmit}
                isLoading={isSubmitting}
                disabled={isSubmitting}
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
