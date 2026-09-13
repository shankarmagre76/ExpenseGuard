import React, { useState, useEffect } from 'react';
import {
  Modal,
  View,
  StyleSheet,
  TextInput,
  TouchableOpacity,
} from 'react-native';
import { AppText } from './AppText';
import { PrimaryButton } from './PrimaryButton';
import { ErrorMessage } from './ErrorMessage';
import { CategoryResponse, CategoryRequest, CategoryType } from '../types/category';
import { colors, spacing, borderRadius } from '../theme';

interface CategoryModalProps {
  visible: boolean;
  onClose: () => void;
  onSubmit: (data: CategoryRequest) => Promise<void>;
  categoryToEdit?: CategoryResponse | null;
  defaultType?: CategoryType;
}

export const CategoryModal: React.FC<CategoryModalProps> = ({
  visible,
  onClose,
  onSubmit,
  categoryToEdit,
  defaultType = 'EXPENSE',
}) => {
  const isEditing = Boolean(categoryToEdit);

  const [name, setName] = useState('');
  const [type, setType] = useState<CategoryType>(defaultType);

  const [nameError, setNameError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (categoryToEdit) {
      setName(categoryToEdit.name);
      setType(categoryToEdit.type);
    } else {
      setName('');
      setType(defaultType);
    }
    setNameError(null);
    setSubmitError(null);
  }, [categoryToEdit, defaultType, visible]);

  const validate = (): boolean => {
    setNameError(null);
    setSubmitError(null);

    if (!name.trim()) {
      setNameError('Category name is required');
      return false;
    } else if (name.trim().length > 100) {
      setNameError('Category name cannot exceed 100 characters');
      return false;
    }
    return true;
  };

  const handleSubmit = async () => {
    if (!validate()) return;

    setIsSubmitting(true);
    setSubmitError(null);

    try {
      await onSubmit({
        name: name.trim(),
        type,
      });
      onClose();
    } catch (err: any) {
      setSubmitError(err?.message || 'Failed to save category.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
      <View style={styles.overlay}>
        <View style={styles.modalCard}>
          <AppText variant="header" style={styles.title}>
            {isEditing ? 'Edit Category' : 'Create Category'}
          </AppText>

          {submitError ? (
            <ErrorMessage message={submitError} onRetry={() => setSubmitError(null)} />
          ) : null}

          {/* Type Toggle */}
          <View style={styles.inputGroup}>
            <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
              CATEGORY TYPE
            </AppText>
            <View style={styles.tabContainer}>
              <TouchableOpacity
                style={[styles.tab, type === 'EXPENSE' ? styles.tabActiveExpense : null]}
                onPress={() => setType('EXPENSE')}
                disabled={isSubmitting || isEditing}
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
                style={[styles.tab, type === 'INCOME' ? styles.tabActiveIncome : null]}
                onPress={() => setType('INCOME')}
                disabled={isSubmitting || isEditing}
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
          </View>

          {/* Name Input */}
          <View style={styles.inputGroup}>
            <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
              CATEGORY NAME
            </AppText>
            <TextInput
              style={[styles.input, nameError ? styles.inputInvalid : null]}
              placeholder="e.g. Groceries, Salary, Utilities"
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
              title={isEditing ? 'Save' : 'Create'}
              onPress={handleSubmit}
              isLoading={isSubmitting}
              disabled={isSubmitting}
              style={styles.halfBtn}
            />
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
  tabContainer: {
    flexDirection: 'row',
    borderRadius: borderRadius.md,
    backgroundColor: colors.background,
    padding: 2,
    borderWidth: 1,
    borderColor: colors.border,
  },
  tab: {
    flex: 1,
    paddingVertical: spacing.sm,
    alignItems: 'center',
    borderRadius: borderRadius.sm,
  },
  tabActiveExpense: {
    backgroundColor: colors.error,
  },
  tabActiveIncome: {
    backgroundColor: colors.success,
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
