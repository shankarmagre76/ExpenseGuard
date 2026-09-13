import React, { useState } from 'react';
import {
  View,
  StyleSheet,
  TouchableOpacity,
  FlatList,
  RefreshControl,
  Switch,
  Alert,
} from 'react-native';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { PrimaryButton } from '../../components/PrimaryButton';
import { LoadingIndicator } from '../../components/LoadingIndicator';
import { ErrorMessage } from '../../components/ErrorMessage';
import { RecurringTransactionModal } from '../../components/RecurringTransactionModal';
import { useRecurringTransactions } from '../../hooks/useRecurringTransactions';
import {
  RecurringTransactionResponse,
  RecurringTransactionRequest,
} from '../../types/recurringTransaction';
import { colors, spacing, borderRadius } from '../../theme';
import { formatCurrency } from '../../utils/currencyFormatter';
import { formatDateDisplay } from '../../utils/dateFormatter';

export const RecurringTransactionsScreen: React.FC = () => {
  const {
    recurringTransactions,
    loading,
    error,
    refresh,
    addRecurringTransaction,
    editRecurringTransaction,
    removeRecurringTransaction,
    toggleStatus,
    triggerExecution,
  } = useRecurringTransactions();

  const [modalVisible, setModalVisible] = useState<boolean>(false);
  const [editingItem, setEditingItem] = useState<RecurringTransactionResponse | null>(null);

  const handleOpenAddModal = () => {
    setEditingItem(null);
    setModalVisible(true);
  };

  const handleOpenEditModal = (item: RecurringTransactionResponse) => {
    setEditingItem(item);
    setModalVisible(true);
  };

  const handleToggleActive = async (item: RecurringTransactionResponse) => {
    try {
      await toggleStatus(item.id, !item.active);
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to update recurring status.');
    }
  };

  const handleManualExecution = async (item: RecurringTransactionResponse) => {
    try {
      const res = await triggerExecution(item.id);
      Alert.alert('Execution Success', `${res.message}. (${res.executedCount} transaction(s) created)`);
    } catch (err: any) {
      Alert.alert('Execution Error', err.message || 'Failed to execute recurring transaction.');
    }
  };

  const handleDeletePress = (item: RecurringTransactionResponse) => {
    Alert.alert(
      'Delete Recurring Transaction',
      `Are you sure you want to delete the recurring template "${item.description || item.categoryName}"?`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            try {
              await removeRecurringTransaction(item.id);
            } catch (err: any) {
              Alert.alert('Error', err.message || 'Failed to delete recurring transaction.');
            }
          },
        },
      ]
    );
  };

  const handleModalSubmit = async (data: RecurringTransactionRequest) => {
    if (editingItem) {
      await editRecurringTransaction(editingItem.id, data);
    } else {
      await addRecurringTransaction(data);
    }
  };

  const renderItem = ({ item }: { item: RecurringTransactionResponse }) => {
    const isIncome = item.type === 'INCOME';
    const amountColor = isIncome ? colors.success : colors.error;
    const prefix = isIncome ? '+' : '-';

    return (
      <View
        style={[styles.card, !item.active && styles.cardInactive]}
        accessibilityLabel={`Recurring transaction ${item.description || item.categoryName}`}
      >
        <View style={styles.cardHeader}>
          <View style={styles.headerLeft}>
            <AppText variant="subheader" style={styles.categoryName} numberOfLines={1}>
              {item.categoryName}
            </AppText>
            {item.description ? (
              <AppText variant="caption" color={colors.textSecondary} numberOfLines={1}>
                {item.description}
              </AppText>
            ) : null}
          </View>
          <View style={styles.switchContainer}>
            <AppText variant="caption" color={colors.textMuted} style={styles.switchLabel}>
              {item.active ? 'Active' : 'Disabled'}
            </AppText>
            <Switch
              value={item.active}
              onValueChange={() => handleToggleActive(item)}
              trackColor={{ false: colors.border, true: `${colors.primary}80` }}
              thumbColor={item.active ? colors.primary : colors.textMuted}
            />
          </View>
        </View>

        <View style={styles.detailsRow}>
          <View>
            <AppText variant="subheader" color={amountColor} style={styles.amountText}>
              {prefix}{formatCurrency(item.amount)}
            </AppText>
            <View style={styles.badgeRow}>
              <View style={styles.freqBadge}>
                <AppText variant="caption" style={styles.freqText}>
                  {item.frequency}
                </AppText>
              </View>
              <View style={[styles.typeBadge, { backgroundColor: isIncome ? `${colors.success}1F` : `${colors.error}1F` }]}>
                <AppText variant="caption" color={amountColor} style={styles.boldText}>
                  {item.type}
                </AppText>
              </View>
            </View>
          </View>

          <View style={styles.dateRight}>
            <AppText variant="caption" color={colors.textSecondary}>
              Next Run:
            </AppText>
            <AppText variant="body" style={styles.boldText}>
              {formatDateDisplay(item.nextRunDate)}
            </AppText>
            {item.endDate && (
              <AppText variant="caption" color={colors.textMuted}>
                Ends: {formatDateDisplay(item.endDate)}
              </AppText>
            )}
          </View>
        </View>

        <View style={styles.actionRow}>
          <TouchableOpacity
            style={styles.runButton}
            onPress={() => handleManualExecution(item)}
            accessibilityLabel={`Run recurring transaction ${item.categoryName} now`}
            accessibilityRole="button"
          >
            <AppText variant="caption" color={colors.primary} style={styles.actionText}>
              ▶ Run Now
            </AppText>
          </TouchableOpacity>
          <View style={styles.rightActions}>
            <TouchableOpacity
              style={styles.actionBtn}
              onPress={() => handleOpenEditModal(item)}
              accessibilityLabel="Edit recurring item"
              accessibilityRole="button"
            >
              <AppText variant="caption" color={colors.primary} style={styles.actionText}>
                Edit
              </AppText>
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.actionBtn}
              onPress={() => handleDeletePress(item)}
              accessibilityLabel="Delete recurring item"
              accessibilityRole="button"
            >
              <AppText variant="caption" color={colors.error} style={styles.actionText}>
                Delete
              </AppText>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    );
  };

  const renderEmptyState = () => {
    if (loading) return null;
    return (
      <View style={styles.emptyContainer}>
        <AppText variant="subheader" style={styles.emptyTitle}>
          No recurring transactions set up
        </AppText>
        <AppText variant="body" color={colors.textSecondary} style={styles.emptySubtitle}>
          Schedule automatic recurring subscriptions, salary deposits, or recurring bills.
        </AppText>
        <View style={styles.emptyButtonWrapper}>
          <PrimaryButton title="+ Schedule Recurring" onPress={handleOpenAddModal} />
        </View>
      </View>
    );
  };

  return (
    <ScreenContainer scrollable={false}>
      <View style={styles.topHeader}>
        <AppText variant="header">Recurring Schedules</AppText>
        <TouchableOpacity
          style={styles.addButton}
          onPress={handleOpenAddModal}
          accessibilityLabel="Add recurring schedule"
          accessibilityRole="button"
        >
          <AppText variant="subheader" color={colors.primary} style={styles.addBtnText}>
            + Schedule Recurring
          </AppText>
        </TouchableOpacity>
      </View>

      {error && <ErrorMessage message={error} style={styles.error} />}

      {loading && recurringTransactions.length === 0 ? (
        <LoadingIndicator message="Loading recurring schedules..." />
      ) : (
        <FlatList
          data={recurringTransactions}
          keyExtractor={(item) => item.id}
          renderItem={renderItem}
          ListEmptyComponent={renderEmptyState}
          contentContainerStyle={styles.listContent}
          refreshControl={
            <RefreshControl
              refreshing={loading}
              onRefresh={refresh}
              colors={[colors.primary]}
              tintColor={colors.primary}
            />
          }
        />
      )}

      <RecurringTransactionModal
        visible={modalVisible}
        item={editingItem}
        onClose={() => setModalVisible(false)}
        onSubmit={handleModalSubmit}
      />
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  topHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  addButton: {
    paddingVertical: spacing.xs,
    paddingHorizontal: spacing.sm,
  },
  addBtnText: {
    fontWeight: '700',
  },
  error: {
    marginBottom: spacing.md,
  },
  listContent: {
    paddingBottom: spacing.xl,
  },
  card: {
    backgroundColor: colors.surface,
    padding: spacing.md,
    borderRadius: borderRadius.md,
    borderWidth: 1,
    borderColor: colors.border,
    marginBottom: spacing.md,
  },
  cardInactive: {
    opacity: 0.7,
    backgroundColor: colors.background,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  headerLeft: {
    flex: 1,
    marginRight: spacing.sm,
  },
  categoryName: {
    fontWeight: '700',
  },
  switchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  switchLabel: {
    marginRight: spacing.xs,
    fontSize: 12,
  },
  detailsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: colors.background,
    padding: spacing.sm,
    borderRadius: borderRadius.sm,
    marginBottom: spacing.sm,
  },
  amountText: {
    fontWeight: '700',
    marginBottom: 4,
  },
  badgeRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  freqBadge: {
    backgroundColor: `${colors.primary}1F`,
    paddingHorizontal: spacing.xs,
    paddingVertical: 2,
    borderRadius: borderRadius.sm,
    marginRight: spacing.xs,
  },
  freqText: {
    color: colors.primary,
    fontWeight: '700',
  },
  typeBadge: {
    paddingHorizontal: spacing.xs,
    paddingVertical: 2,
    borderRadius: borderRadius.sm,
  },
  boldText: {
    fontWeight: '700',
  },
  dateRight: {
    alignItems: 'flex-end',
  },
  actionRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingTop: spacing.xs,
  },
  runButton: {
    paddingVertical: spacing.xs,
    paddingHorizontal: spacing.xs,
  },
  rightActions: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  actionBtn: {
    paddingVertical: spacing.xs,
    paddingHorizontal: spacing.sm,
    marginLeft: spacing.xs,
  },
  actionText: {
    fontWeight: '600',
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
    minWidth: 200,
  },
});
