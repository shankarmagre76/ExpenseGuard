import React, { useState } from 'react';
import {
  View,
  StyleSheet,
  TouchableOpacity,
  FlatList,
  RefreshControl,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../../types/navigation';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { PrimaryButton } from '../../components/PrimaryButton';
import { LoadingIndicator } from '../../components/LoadingIndicator';
import { ErrorMessage } from '../../components/ErrorMessage';
import { TransactionFilterModal } from '../../components/TransactionFilterModal';
import { useTransactions } from '../../hooks/useTransactions';
import { useAccounts } from '../../hooks/useAccounts';
import { useCategories } from '../../hooks/useCategories';
import { TransactionResponse } from '../../types/transaction';
import { colors, spacing, borderRadius } from '../../theme';
import { formatCurrency } from '../../utils/currencyFormatter';
import { formatDateDisplay } from '../../utils/dateFormatter';

type Props = NativeStackScreenProps<RootStackParamList, any>;

export const TransactionsScreen: React.FC<Props> = ({ navigation }) => {
  const {
    transactions,
    loading,
    loadingMore,
    error,
    filters,
    refresh,
    applyFilters,
    clearFilters,
    loadMore,
    deleteTransaction,
  } = useTransactions();

  const { accounts } = useAccounts();
  const { allCategories: categories } = useCategories();

  const [filterModalVisible, setFilterModalVisible] = useState(false);

  const hasActiveFilters = Boolean(
    filters.type || filters.accountId || filters.categoryId || filters.fromDate || filters.toDate
  );

  const handleDelete = (tx: TransactionResponse) => {
    Alert.alert(
      'Delete Transaction',
      `Are you sure you want to delete this ${tx.type.toLowerCase()} of ${formatCurrency(tx.amount)}? Your account balance will automatically revert.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: () => deleteTransaction(tx.id),
        },
      ]
    );
  };

  const getAccountName = (accId: string) => {
    const acc = accounts.find((a) => a.id === accId);
    return acc ? acc.name : 'Account';
  };

  const renderTransactionItem = ({ item }: { item: TransactionResponse }) => {
    const isIncome = item.type === 'INCOME';
    const amountColor = isIncome ? colors.success : colors.error;
    const prefix = isIncome ? '+' : '-';

    return (
      <View style={styles.card}>
        <View style={styles.cardMain}>
          <View style={styles.leftCol}>
            <AppText variant="subheader" bold style={styles.catName}>
              {item.categoryName || 'Transaction'}
            </AppText>
            {item.description ? (
              <AppText variant="body" color={colors.textSecondary} numberOfLines={1}>
                {item.description}
              </AppText>
            ) : null}
            <View style={styles.metaRow}>
              <AppText variant="caption" color={colors.textMuted}>
                {getAccountName(item.accountId)} • {formatDateDisplay(item.transactionDate)}
              </AppText>
            </View>
          </View>

          <View style={styles.rightCol}>
            <AppText variant="subheader" bold color={amountColor} style={styles.amountText}>
              {prefix}{formatCurrency(item.amount)}
            </AppText>
          </View>
        </View>

        <View style={styles.cardFooter}>
          <TouchableOpacity
            style={styles.actionBtn}
            onPress={() => navigation.navigate('EditTransaction', { transactionId: item.id })}
          >
            <AppText variant="caption" color={colors.primary} bold>
              Edit
            </AppText>
          </TouchableOpacity>
          <TouchableOpacity style={styles.actionBtn} onPress={() => handleDelete(item)}>
            <AppText variant="caption" color={colors.error} bold>
              Delete
            </AppText>
          </TouchableOpacity>
        </View>
      </View>
    );
  };

  return (
    <ScreenContainer style={styles.container}>
      {/* Header */}
      <View style={styles.headerRow}>
        <View style={styles.headerTextCol}>
          <AppText variant="header">Transactions</AppText>
          <AppText variant="caption" color={colors.textSecondary}>
            Income & expense history
          </AppText>
        </View>

        <View style={styles.headerBtnRow}>
          <PrimaryButton
            title="+ Expense"
            onPress={() => navigation.navigate('AddExpense')}
            style={[styles.smallBtn, { backgroundColor: colors.error }]}
          />
          <PrimaryButton
            title="+ Income"
            onPress={() => navigation.navigate('AddIncome')}
            style={[styles.smallBtn, { backgroundColor: colors.success }]}
          />
        </View>
      </View>

      {/* Filter Bar */}
      <View style={styles.filterBar}>
        <TouchableOpacity
          style={[styles.filterTrigger, hasActiveFilters ? styles.filterActiveTrigger : null]}
          onPress={() => setFilterModalVisible(true)}
        >
          <AppText
            variant="body"
            bold={hasActiveFilters}
            color={hasActiveFilters ? colors.primary : colors.textPrimary}
          >
            🔍 {hasActiveFilters ? 'Filters Applied' : 'Filter Transactions'}
          </AppText>
        </TouchableOpacity>

        {hasActiveFilters ? (
          <TouchableOpacity style={styles.clearFilterBtn} onPress={clearFilters}>
            <AppText variant="caption" color={colors.error} bold>
              Clear
            </AppText>
          </TouchableOpacity>
        ) : null}
      </View>

      {error ? <ErrorMessage message={error} onRetry={refresh} /> : null}

      {loading && transactions.length === 0 ? (
        <LoadingIndicator message="Loading transaction history..." />
      ) : (
        <FlatList
          data={transactions}
          keyExtractor={(item) => item.id}
          renderItem={renderTransactionItem}
          refreshControl={<RefreshControl refreshing={loading} onRefresh={refresh} />}
          onEndReached={loadMore}
          onEndReachedThreshold={0.3}
          showsVerticalScrollIndicator={false}
          contentContainerStyle={styles.listContent}
          ListFooterComponent={
            loadingMore ? (
              <View style={styles.loadMoreContainer}>
                <ActivityIndicator size="small" color={colors.primary} />
                <AppText variant="caption" color={colors.textMuted} style={styles.loadMoreText}>
                  Loading more transactions...
                </AppText>
              </View>
            ) : undefined
          }
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <AppText variant="subheader" style={styles.emptyTitle}>
                No Transactions Found
              </AppText>
              <AppText variant="body" color={colors.textSecondary} style={styles.emptyText}>
                {hasActiveFilters
                  ? 'No transactions match the selected filters. Try clearing or adjusting filters.'
                  : 'You have not recorded any transactions yet. Use "+ Expense" or "+ Income" to record one.'}
              </AppText>
              {hasActiveFilters ? (
                <PrimaryButton title="Clear Filters" onPress={clearFilters} style={styles.emptyBtn} />
              ) : (
                <View style={styles.emptyBtnRow}>
                  <PrimaryButton
                    title="Add Expense"
                    onPress={() => navigation.navigate('AddExpense')}
                    style={[styles.halfBtn, { backgroundColor: colors.error }]}
                  />
                  <PrimaryButton
                    title="Add Income"
                    onPress={() => navigation.navigate('AddIncome')}
                    style={[styles.halfBtn, { backgroundColor: colors.success }]}
                  />
                </View>
              )}
            </View>
          }
        />
      )}

      {/* Filter Modal */}
      <TransactionFilterModal
        visible={filterModalVisible}
        onClose={() => setFilterModalVisible(false)}
        onApply={applyFilters}
        onReset={clearFilters}
        activeFilters={filters}
        accounts={accounts}
        categories={categories}
      />
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  headerTextCol: {
    flex: 1,
  },
  headerBtnRow: {
    flexDirection: 'row',
    gap: spacing.xs,
  },
  smallBtn: {
    height: 36,
    paddingHorizontal: spacing.sm,
  },
  filterBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: colors.surface,
    borderRadius: borderRadius.md,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    marginBottom: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
  },
  filterTrigger: {
    flex: 1,
  },
  filterActiveTrigger: {
    borderColor: colors.primary,
  },
  clearFilterBtn: {
    paddingHorizontal: spacing.sm,
  },
  listContent: {
    paddingBottom: spacing.xl,
  },
  card: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.md,
    marginBottom: spacing.sm,
    borderWidth: 1,
    borderColor: colors.border,
  },
  cardMain: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: spacing.xs,
  },
  leftCol: {
    flex: 1,
    paddingRight: spacing.sm,
  },
  rightCol: {
    alignItems: 'flex-end',
  },
  catName: {
    fontSize: 16,
    marginBottom: 2,
  },
  metaRow: {
    marginTop: 4,
  },
  amountText: {
    fontSize: 17,
  },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: spacing.md,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    paddingTop: spacing.xs,
    marginTop: spacing.xs,
  },
  actionBtn: {
    paddingHorizontal: spacing.xs,
    paddingVertical: 2,
  },
  loadMoreContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: spacing.md,
    gap: spacing.sm,
  },
  loadMoreText: {
    textAlign: 'center',
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: spacing.xxl,
    paddingHorizontal: spacing.lg,
  },
  emptyTitle: {
    marginBottom: spacing.xs,
  },
  emptyText: {
    textAlign: 'center',
    marginBottom: spacing.lg,
  },
  emptyBtn: {
    width: '100%',
  },
  emptyBtnRow: {
    flexDirection: 'row',
    gap: spacing.md,
    width: '100%',
  },
  halfBtn: {
    flex: 1,
  },
});
