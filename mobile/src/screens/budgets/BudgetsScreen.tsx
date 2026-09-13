import React, { useState } from 'react';
import {
  View,
  StyleSheet,
  TouchableOpacity,
  FlatList,
  RefreshControl,
  Alert,
} from 'react-native';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { PrimaryButton } from '../../components/PrimaryButton';
import { LoadingIndicator } from '../../components/LoadingIndicator';
import { ErrorMessage } from '../../components/ErrorMessage';
import { MonthSelector } from '../../components/MonthSelector';
import { BudgetProgressBar } from '../../components/BudgetProgressBar';
import { BudgetModal } from '../../components/BudgetModal';
import { useBudgets } from '../../hooks/useBudgets';
import { BudgetResponse, BudgetRequest } from '../../types/budget';
import { colors, spacing, borderRadius } from '../../theme';
import { formatCurrency } from '../../utils/currencyFormatter';

export const BudgetsScreen: React.FC = () => {
  const {
    selectedMonth,
    setSelectedMonth,
    budgets,
    loading,
    error,
    refreshBudgets,
    addBudget,
    editBudget,
    removeBudget,
  } = useBudgets();

  const [modalVisible, setModalVisible] = useState<boolean>(false);
  const [editingBudget, setEditingBudget] = useState<BudgetResponse | null>(null);

  const handleOpenAddModal = () => {
    setEditingBudget(null);
    setModalVisible(true);
  };

  const handleOpenEditModal = (budget: BudgetResponse) => {
    setEditingBudget(budget);
    setModalVisible(true);
  };

  const handleDeletePress = (budget: BudgetResponse) => {
    Alert.alert(
      'Delete Budget',
      `Are you sure you want to delete the budget for "${budget.categoryName}"?`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            try {
              await removeBudget(budget.id);
            } catch (err: any) {
              Alert.alert('Error', err.message || 'Failed to delete budget');
            }
          },
        },
      ]
    );
  };

  const handleModalSubmit = async (data: BudgetRequest) => {
    if (editingBudget) {
      await editBudget(editingBudget.id, data);
    } else {
      await addBudget(data);
    }
  };

  const renderBudgetItem = ({ item }: { item: BudgetResponse }) => {
    const budgetAmount = item.amount ?? item.budgetAmount ?? 0;

    return (
      <View style={styles.budgetCard} accessibilityLabel={`Budget item for ${item.categoryName}`}>
        <View style={styles.cardHeader}>
          <AppText variant="subheader" style={styles.categoryTitle}>
            {item.categoryName}
          </AppText>
          <View style={styles.actionButtons}>
            <TouchableOpacity
              style={styles.actionButton}
              onPress={() => handleOpenEditModal(item)}
              accessibilityLabel={`Edit budget for ${item.categoryName}`}
              accessibilityRole="button"
            >
              <AppText variant="caption" color={colors.primary} style={styles.actionText}>
                Edit
              </AppText>
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.actionButton}
              onPress={() => handleDeletePress(item)}
              accessibilityLabel={`Delete budget for ${item.categoryName}`}
              accessibilityRole="button"
            >
              <AppText variant="caption" color={colors.error} style={styles.actionText}>
                Delete
              </AppText>
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.amountRow}>
          <View>
            <AppText variant="caption" color={colors.textSecondary}>
              Budgeted
            </AppText>
            <AppText variant="body" style={styles.amountText}>
              {formatCurrency(budgetAmount)}
            </AppText>
          </View>
          <View style={styles.amountRight}>
            <AppText variant="caption" color={colors.textSecondary}>
              Spent
            </AppText>
            <AppText variant="body" style={styles.amountText}>
              {formatCurrency(item.spentAmount)}
            </AppText>
          </View>
          <View style={styles.amountRight}>
            <AppText variant="caption" color={colors.textSecondary}>
              Remaining
            </AppText>
            <AppText
              variant="body"
              color={item.remainingAmount < 0 ? colors.error : colors.textPrimary}
              style={styles.amountText}
            >
              {formatCurrency(item.remainingAmount)}
            </AppText>
          </View>
        </View>

        <BudgetProgressBar utilizationPercentage={item.utilizationPercentage} />
      </View>
    );
  };

  const renderEmptyState = () => {
    if (loading) return null;
    return (
      <View style={styles.emptyContainer}>
        <AppText variant="subheader" style={styles.emptyTitle}>
          No budgets set for this month
        </AppText>
        <AppText variant="body" color={colors.textSecondary} style={styles.emptySubtitle}>
          Set a category budget to track and control your monthly spending velocity.
        </AppText>
        <View style={styles.emptyButtonWrapper}>
          <PrimaryButton title="+ Set Budget" onPress={handleOpenAddModal} />
        </View>
      </View>
    );
  };

  return (
    <ScreenContainer scrollable={false}>
      <View style={styles.headerRow}>
        <MonthSelector selectedMonth={selectedMonth} onMonthChange={setSelectedMonth} />
        <TouchableOpacity
          style={styles.addButton}
          onPress={handleOpenAddModal}
          accessibilityLabel="Add budget"
          accessibilityRole="button"
        >
          <AppText variant="subheader" color={colors.primary} style={styles.addButtonText}>
            + Set Budget
          </AppText>
        </TouchableOpacity>
      </View>

      {error && <ErrorMessage message={error} style={styles.error} />}

      {loading && budgets.length === 0 ? (
        <LoadingIndicator message="Loading budgets..." />
      ) : (
        <FlatList
          data={budgets}
          keyExtractor={(item) => item.id}
          renderItem={renderBudgetItem}
          ListEmptyComponent={renderEmptyState}
          contentContainerStyle={styles.listContent}
          refreshControl={
            <RefreshControl
              refreshing={loading}
              onRefresh={refreshBudgets}
              colors={[colors.primary]}
              tintColor={colors.primary}
            />
          }
        />
      )}

      <BudgetModal
        visible={modalVisible}
        budget={editingBudget}
        selectedMonth={selectedMonth}
        onClose={() => setModalVisible(false)}
        onSubmit={handleModalSubmit}
      />
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  headerRow: {
    marginBottom: spacing.xs,
  },
  addButton: {
    alignSelf: 'flex-end',
    paddingVertical: spacing.xs,
    paddingHorizontal: spacing.sm,
    marginBottom: spacing.sm,
  },
  addButtonText: {
    fontWeight: '700',
  },
  error: {
    marginBottom: spacing.md,
  },
  listContent: {
    paddingBottom: spacing.xl,
  },
  budgetCard: {
    backgroundColor: colors.surface,
    padding: spacing.md,
    borderRadius: borderRadius.md,
    borderWidth: 1,
    borderColor: colors.border,
    marginBottom: spacing.md,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  categoryTitle: {
    fontWeight: '700',
    color: colors.textPrimary,
    flex: 1,
  },
  actionButtons: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  actionButton: {
    paddingHorizontal: spacing.xs,
    paddingVertical: 2,
    marginLeft: spacing.xs,
  },
  actionText: {
    fontWeight: '600',
  },
  amountRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.sm,
    backgroundColor: colors.background,
    padding: spacing.sm,
    borderRadius: borderRadius.sm,
  },
  amountRight: {
    alignItems: 'flex-end',
  },
  amountText: {
    fontWeight: '700',
    marginTop: 2,
  },
  emptyContainer: {
    backgroundColor: colors.surface,
    padding: spacing.xl,
    borderRadius: borderRadius.md,
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
    marginVertical: spacing.md,
  },
  emptyTitle: {
    fontWeight: '700',
    marginBottom: spacing.xs,
  },
  emptySubtitle: {
    textAlign: 'center',
    marginBottom: spacing.lg,
  },
  emptyButtonWrapper: {
    width: 180,
  },
});
