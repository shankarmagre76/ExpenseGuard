import React from 'react';
import {
  View,
  StyleSheet,
  ScrollView,
  RefreshControl,
} from 'react-native';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { LoadingIndicator } from '../../components/LoadingIndicator';
import { ErrorMessage } from '../../components/ErrorMessage';
import { MonthSelector } from '../../components/MonthSelector';
import { CategoryBarChart, TrendChart } from '../../components/AnalyticsBarChart';
import { BudgetProgressBar } from '../../components/BudgetProgressBar';
import { useAnalytics } from '../../hooks/useAnalytics';
import { colors, spacing, borderRadius } from '../../theme';
import { formatCurrency } from '../../utils/currencyFormatter';

export const AnalyticsScreen: React.FC = () => {
  const {
    selectedMonth,
    setSelectedMonth,
    monthlyAnalytics,
    categoryAnalytics,
    accountAnalytics,
    budgetPerformance,
    monthlyTrend,
    loading,
    error,
    refreshAnalytics,
  } = useAnalytics();

  const renderFinancialSummaryCards = () => {
    if (!monthlyAnalytics) return null;
    const { totalIncome, totalExpense, netSavings, savingsRate } = monthlyAnalytics;

    return (
      <View style={styles.gridRow}>
        <View style={[styles.card, styles.gridCard]}>
          <AppText variant="caption" color={colors.textSecondary}>
            Total Income
          </AppText>
          <AppText variant="subheader" color={colors.success} style={styles.cardValue}>
            {formatCurrency(totalIncome)}
          </AppText>
        </View>

        <View style={[styles.card, styles.gridCard]}>
          <AppText variant="caption" color={colors.textSecondary}>
            Total Expense
          </AppText>
          <AppText variant="subheader" color={colors.error} style={styles.cardValue}>
            {formatCurrency(totalExpense)}
          </AppText>
        </View>

        <View style={[styles.card, styles.gridCard]}>
          <AppText variant="caption" color={colors.textSecondary}>
            Net Savings
          </AppText>
          <AppText
            variant="subheader"
            color={netSavings < 0 ? colors.error : colors.primary}
            style={styles.cardValue}
          >
            {formatCurrency(netSavings)}
          </AppText>
        </View>

        <View style={[styles.card, styles.gridCard]}>
          <AppText variant="caption" color={colors.textSecondary}>
            Savings Rate
          </AppText>
          <AppText variant="subheader" color={colors.textPrimary} style={styles.cardValue}>
            {savingsRate !== undefined ? `${savingsRate.toFixed(1)}%` : '0%'}
          </AppText>
        </View>
      </View>
    );
  };

  const renderCategoryBreakdown = () => {
    const categories = categoryAnalytics?.categories || [];
    const chartData = categories.map((c) => ({
      id: c.categoryId,
      label: c.categoryName,
      amount: c.amount,
      percentage: c.percentage,
    }));

    return (
      <View style={styles.sectionCard}>
        <AppText variant="subheader" style={styles.sectionTitle}>
          Category Spending Breakdown
        </AppText>

        {chartData.length === 0 ? (
          <AppText variant="body" color={colors.textSecondary} style={styles.emptyText}>
            No expense data available for this month.
          </AppText>
        ) : (
          <CategoryBarChart data={chartData} />
        )}
      </View>
    );
  };

  const renderBudgetPerformance = () => {
    const budgets = budgetPerformance?.budgets || [];

    return (
      <View style={styles.sectionCard}>
        <AppText variant="subheader" style={styles.sectionTitle}>
          Budget vs Actual Performance
        </AppText>

        {budgetPerformance && (
          <View style={styles.summaryBarRow}>
            <View>
              <AppText variant="caption" color={colors.textSecondary}>
                Total Budgeted
              </AppText>
              <AppText variant="body" style={styles.boldText}>
                {formatCurrency(budgetPerformance.totalBudgeted)}
              </AppText>
            </View>
            <View style={styles.alignRight}>
              <AppText variant="caption" color={colors.textSecondary}>
                Total Spent
              </AppText>
              <AppText variant="body" style={styles.boldText}>
                {formatCurrency(budgetPerformance.totalSpent)}
              </AppText>
            </View>
          </View>
        )}

        {budgets.length === 0 ? (
          <AppText variant="body" color={colors.textSecondary} style={styles.emptyText}>
            No budgets configured for this month.
          </AppText>
        ) : (
          budgets.map((b) => (
            <View key={b.id} style={styles.perfItem}>
              <View style={styles.perfHeader}>
                <AppText variant="body" style={styles.perfCategory}>
                  {b.categoryName}
                </AppText>
                <AppText variant="body">
                  {formatCurrency(b.spentAmount)} / {formatCurrency(b.amount ?? b.budgetAmount ?? 0)}
                </AppText>
              </View>
              <BudgetProgressBar utilizationPercentage={b.utilizationPercentage} />
            </View>
          ))
        )}
      </View>
    );
  };

  const renderAccountCashflow = () => {
    const accounts = accountAnalytics?.accounts || [];

    return (
      <View style={styles.sectionCard}>
        <AppText variant="subheader" style={styles.sectionTitle}>
          Account Cash Flow
        </AppText>

        {accounts.length === 0 ? (
          <AppText variant="body" color={colors.textSecondary} style={styles.emptyText}>
            No account activity found for this month.
          </AppText>
        ) : (
          accounts.map((acc) => (
            <View key={acc.accountId} style={styles.accountRow}>
              <AppText variant="body" style={styles.accountName}>
                {acc.accountName}
              </AppText>
              <View style={styles.accountDetails}>
                <AppText variant="caption" color={colors.success}>
                  +{formatCurrency(acc.totalIncome)}
                </AppText>
                <AppText variant="caption" color={colors.error} style={styles.spacingLeft}>
                  -{formatCurrency(acc.totalExpense)}
                </AppText>
                <AppText
                  variant="caption"
                  color={acc.netChange < 0 ? colors.error : colors.primary}
                  style={[styles.spacingLeft, styles.boldText]}
                >
                  Net: {formatCurrency(acc.netChange)}
                </AppText>
              </View>
            </View>
          ))
        )}
      </View>
    );
  };

  const renderSpendingTrend = () => {
    const trends = monthlyTrend?.trends || [];

    return (
      <View style={styles.sectionCard}>
        <AppText variant="subheader" style={styles.sectionTitle}>
          6-Month Spending & Income Trend
        </AppText>

        {trends.length === 0 ? (
          <AppText variant="body" color={colors.textSecondary} style={styles.emptyText}>
            Not enough data to display a trend.
          </AppText>
        ) : (
          <TrendChart trends={trends} />
        )}
      </View>
    );
  };

  return (
    <ScreenContainer scrollable={false}>
      <MonthSelector selectedMonth={selectedMonth} onMonthChange={setSelectedMonth} />

      {error && <ErrorMessage message={error} style={styles.error} />}

      {loading && !monthlyAnalytics ? (
        <LoadingIndicator message="Fetching analytics data..." />
      ) : (
        <ScrollView
          style={styles.scrollContainer}
          contentContainerStyle={styles.scrollContent}
          refreshControl={
            <RefreshControl
              refreshing={loading}
              onRefresh={refreshAnalytics}
              colors={[colors.primary]}
              tintColor={colors.primary}
            />
          }
        >
          {renderFinancialSummaryCards()}
          {renderCategoryBreakdown()}
          {renderBudgetPerformance()}
          {renderAccountCashflow()}
          {renderSpendingTrend()}
        </ScrollView>
      )}
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  error: {
    marginBottom: spacing.md,
  },
  scrollContainer: {
    flex: 1,
  },
  scrollContent: {
    paddingBottom: spacing.xxl,
  },
  gridRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    marginBottom: spacing.md,
  },
  card: {
    backgroundColor: colors.surface,
    padding: spacing.md,
    borderRadius: borderRadius.md,
    borderWidth: 1,
    borderColor: colors.border,
  },
  gridCard: {
    width: '48%',
    marginBottom: spacing.sm,
  },
  cardValue: {
    fontWeight: '700',
    marginTop: spacing.xs,
  },
  sectionCard: {
    backgroundColor: colors.surface,
    padding: spacing.md,
    borderRadius: borderRadius.md,
    borderWidth: 1,
    borderColor: colors.border,
    marginBottom: spacing.md,
  },
  sectionTitle: {
    fontWeight: '700',
    color: colors.textPrimary,
    marginBottom: spacing.sm,
  },
  emptyText: {
    fontStyle: 'italic',
    marginVertical: spacing.sm,
  },
  summaryBarRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    backgroundColor: colors.background,
    padding: spacing.sm,
    borderRadius: borderRadius.sm,
    marginBottom: spacing.md,
  },
  alignRight: {
    alignItems: 'flex-end',
  },
  boldText: {
    fontWeight: '700',
  },
  perfItem: {
    marginBottom: spacing.sm,
    paddingBottom: spacing.xs,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  perfHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 2,
  },
  perfCategory: {
    fontWeight: '600',
  },
  accountRow: {
    paddingVertical: spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  accountName: {
    fontWeight: '600',
    marginBottom: 2,
  },
  accountDetails: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  spacingLeft: {
    marginLeft: spacing.md,
  },
});
