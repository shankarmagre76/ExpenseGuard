import React, { useCallback } from 'react';
import {
  View,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  RefreshControl,
} from 'react-native';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { PrimaryButton } from '../../components/PrimaryButton';
import { ErrorMessage } from '../../components/ErrorMessage';
import { useAccounts } from '../../hooks/useAccounts';
import { useTransactions } from '../../hooks/useTransactions';
import { useBudgets } from '../../hooks/useBudgets';
import { useHealthCheck } from '../../hooks/useHealthCheck';
import { useNotifications } from '../../hooks/useNotifications';
import { NotificationBadge } from '../../components/NotificationBadge';
import { BudgetProgressBar } from '../../components/BudgetProgressBar';
import { colors, spacing, borderRadius } from '../../theme';
import { formatCurrency } from '../../utils/currencyFormatter';
import { formatDateDisplay } from '../../utils/dateFormatter';

export const DashboardScreen: React.FC<any> = ({ navigation }) => {
  const { accounts, loading: accountsLoading, error: accountsError, refresh: refreshAccounts } = useAccounts();
  const { transactions, loading: txLoading, error: txError, refresh: refreshTransactions } = useTransactions({ size: 5 });
  const { budgets, refreshBudgets } = useBudgets();
  const { status: healthStatus } = useHealthCheck();
  const { unreadCount, refreshNotifications } = useNotifications();

  const handleRefresh = useCallback(async () => {
    await Promise.all([refreshAccounts(), refreshTransactions(), refreshBudgets(), refreshNotifications()]);
  }, [refreshAccounts, refreshTransactions, refreshBudgets, refreshNotifications]);

  // Compute aggregated total net balance from backend accounts response
  const totalBalance = accounts.reduce((acc, account) => {
    const val = typeof account.balance === 'string' ? parseFloat(account.balance) : account.balance;
    return acc + (isNaN(val) ? 0 : val);
  }, 0);

  // Filter recent income vs expense totals
  const totalIncome = transactions
    .filter((t) => t.type === 'INCOME')
    .reduce((acc, t) => acc + (typeof t.amount === 'string' ? parseFloat(t.amount) : t.amount), 0);

  const totalExpense = transactions
    .filter((t) => t.type === 'EXPENSE')
    .reduce((acc, t) => acc + (typeof t.amount === 'string' ? parseFloat(t.amount) : t.amount), 0);

  const primaryCurrency = accounts.length > 0 ? accounts[0].currency : 'USD';
  const isLoading = accountsLoading || txLoading;

  return (
    <ScreenContainer scrollable>
      <ScrollView
        refreshControl={<RefreshControl refreshing={isLoading} onRefresh={handleRefresh} />}
        showsVerticalScrollIndicator={false}
      >
        {/* Header Bar with Notification Icon */}
        <View style={styles.headerBar}>
          <AppText variant="header" bold>Dashboard</AppText>
          <NotificationBadge
            unreadCount={unreadCount}
            onPress={() => navigation.navigate('Notifications')}
          />
        </View>

        {/* Total Net Balance Card */}
        <View style={styles.heroCard}>
          <AppText variant="caption" color={colors.primaryLight} style={styles.heroLabel}>
            TOTAL NET BALANCE
          </AppText>
          <AppText variant="title" color={colors.surface} style={styles.heroBalance}>
            {formatCurrency(totalBalance, primaryCurrency)}
          </AppText>
          <AppText variant="caption" color={colors.primaryLight}>
            Across {accounts.length} linked financial account{accounts.length === 1 ? '' : 's'}
          </AppText>
        </View>

        {/* Quick Summary Grid */}
        <View style={styles.summaryGrid}>
          <View style={[styles.summaryCard, { borderColor: colors.success }]}>
            <AppText variant="caption" color={colors.textMuted}>
              RECENT INCOME
            </AppText>
            <AppText variant="header" color={colors.success} style={styles.summaryValue}>
              +{formatCurrency(totalIncome, primaryCurrency)}
            </AppText>
          </View>
          <View style={[styles.summaryCard, { borderColor: colors.error }]}>
            <AppText variant="caption" color={colors.textMuted}>
              RECENT EXPENSE
            </AppText>
            <AppText variant="header" color={colors.error} style={styles.summaryValue}>
              -{formatCurrency(totalExpense, primaryCurrency)}
            </AppText>
          </View>
        </View>

        {/* Quick Action Buttons */}
        <View style={styles.actionRow}>
          <PrimaryButton
            title="+ Add Expense"
            onPress={() => navigation.navigate('AddExpense')}
            style={[styles.halfActionBtn, { backgroundColor: colors.error }]}
          />
          <PrimaryButton
            title="+ Add Income"
            onPress={() => navigation.navigate('AddIncome')}
            style={[styles.halfActionBtn, { backgroundColor: colors.success }]}
          />
        </View>
        <PrimaryButton
          title="📷 Scan Receipt with OCR"
          onPress={() => navigation.navigate('ReceiptUpload')}
          variant="outline"
          style={{ marginBottom: spacing.lg }}
        />

        {/* Budget Status Summary */}
        {budgets.length > 0 && (
          <View style={styles.emptyCard}>
            <View style={styles.sectionHeaderRow}>
              <AppText variant="header">Budget Status</AppText>
              <TouchableOpacity onPress={() => navigation.navigate('Budgets')}>
                <AppText variant="caption" color={colors.primary} bold>
                  Manage Budgets →
                </AppText>
              </TouchableOpacity>
            </View>
            {budgets.slice(0, 2).map((b) => (
              <View key={b.id} style={styles.budgetWidgetItem}>
                <View style={styles.budgetWidgetHeader}>
                  <AppText variant="body" bold>{b.categoryName}</AppText>
                  <AppText variant="caption">{formatCurrency(b.spentAmount)} / {formatCurrency(b.amount ?? b.budgetAmount ?? 0)}</AppText>
                </View>
                <BudgetProgressBar utilizationPercentage={b.utilizationPercentage} />
              </View>
            ))}
          </View>
        )}

        {/* Accounts Overview Section */}
        <View style={styles.sectionHeaderRow}>
          <AppText variant="header">Your Accounts</AppText>
          <TouchableOpacity onPress={() => navigation.navigate('Accounts')}>
            <AppText variant="caption" color={colors.primary} bold>
              Manage All →
            </AppText>
          </TouchableOpacity>
        </View>

        {accountsError ? <ErrorMessage message={accountsError} onRetry={refreshAccounts} /> : null}

        {accounts.length === 0 && !accountsLoading ? (
          <View style={styles.emptyCard}>
            <AppText variant="body" color={colors.textSecondary}>
              No accounts created yet. Add an account to begin tracking transactions.
            </AppText>
          </View>
        ) : (
          <View style={styles.accountsScroll}>
            {accounts.slice(0, 3).map((acc) => (
              <View key={acc.id} style={styles.accountChip}>
                <View style={styles.accountChipHeader}>
                  <AppText variant="body" bold style={styles.accountName}>
                    {acc.name}
                  </AppText>
                  <AppText variant="caption" color={colors.primary}>
                    {acc.type}
                  </AppText>
                </View>
                <AppText variant="subheader" bold color={colors.textPrimary}>
                  {formatCurrency(acc.balance, acc.currency)}
                </AppText>
              </View>
            ))}
          </View>
        )}

        {/* Recent Activity Section */}
        <View style={styles.sectionHeaderRow}>
          <AppText variant="header">Recent Activity</AppText>
          <TouchableOpacity onPress={() => navigation.navigate('Transactions')}>
            <AppText variant="caption" color={colors.primary} bold>
              View All →
            </AppText>
          </TouchableOpacity>
        </View>

        {txError ? <ErrorMessage message={txError} onRetry={refreshTransactions} /> : null}

        {transactions.length === 0 && !txLoading ? (
          <View style={styles.emptyCard}>
            <AppText variant="body" color={colors.textSecondary}>
              No transactions recorded yet. Tap "+ Add Expense" or "+ Add Income" above.
            </AppText>
          </View>
        ) : (
          <View style={styles.txList}>
            {transactions.slice(0, 5).map((tx) => {
              const isIncome = tx.type === 'INCOME';
              const color = isIncome ? colors.success : colors.error;
              const prefix = isIncome ? '+' : '-';
              return (
                <View key={tx.id} style={styles.txItem}>
                  <View style={styles.txLeft}>
                    <AppText variant="body" bold>
                      {tx.categoryName || 'Transaction'}
                    </AppText>
                    <AppText variant="caption" color={colors.textMuted}>
                      {formatDateDisplay(tx.transactionDate)}
                    </AppText>
                  </View>
                  <AppText variant="body" bold color={color}>
                    {prefix}{formatCurrency(tx.amount)}
                  </AppText>
                </View>
              );
            })}
          </View>
        )}

        {/* Backend Status Footer Badge */}
        <TouchableOpacity
          style={styles.healthFooter}
          onPress={() => navigation.navigate('HealthCheck')}
        >
          <AppText variant="caption" color={colors.textMuted}>
            Backend Server: {healthStatus.toUpperCase()} (Port 8081) • Tap for diagnostics →
          </AppText>
        </TouchableOpacity>
      </ScrollView>
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  headerBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.md,
    marginTop: spacing.xs,
  },
  heroCard: {
    backgroundColor: colors.primary,
    borderRadius: borderRadius.lg,
    padding: spacing.lg,
    marginBottom: spacing.md,
  },
  heroLabel: {
    letterSpacing: 1,
    marginBottom: spacing.xs,
  },
  heroBalance: {
    fontSize: 32,
    fontWeight: '700',
    marginBottom: spacing.xs,
  },
  summaryGrid: {
    flexDirection: 'row',
    gap: spacing.sm,
    marginBottom: spacing.md,
  },
  summaryCard: {
    flex: 1,
    backgroundColor: colors.surface,
    padding: spacing.md,
    borderRadius: borderRadius.md,
    borderWidth: 1,
    borderColor: colors.border,
  },
  summaryValue: {
    marginTop: spacing.xs,
    fontSize: 18,
  },
  actionRow: {
    flexDirection: 'row',
    gap: spacing.sm,
    marginBottom: spacing.lg,
  },
  halfActionBtn: {
    flex: 1,
  },
  sectionHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.sm,
    marginTop: spacing.sm,
  },
  emptyCard: {
    backgroundColor: colors.surface,
    padding: spacing.md,
    borderRadius: borderRadius.md,
    borderWidth: 1,
    borderColor: colors.border,
    marginBottom: spacing.md,
  },
  accountsScroll: {
    gap: spacing.sm,
    marginBottom: spacing.md,
  },
  accountChip: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.md,
    padding: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
  },
  accountChipHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.xs,
  },
  accountName: {
    fontSize: 16,
  },
  txList: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    borderWidth: 1,
    borderColor: colors.border,
    paddingHorizontal: spacing.md,
    marginBottom: spacing.lg,
  },
  txItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  txLeft: {
    flex: 1,
  },
  healthFooter: {
    alignItems: 'center',
    paddingVertical: spacing.md,
  },
  budgetWidgetItem: {
    marginTop: spacing.xs,
  },
  budgetWidgetHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
});
