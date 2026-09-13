import React from 'react';
import { View, StyleSheet } from 'react-native';
import { AppText } from './AppText';
import { colors, spacing, borderRadius } from '../theme';
import { formatCurrency } from '../utils/currencyFormatter';
import { formatMonthShort } from '../utils/dateFormatter';
import { MonthlyTrendItem } from '../types/analytics';

interface CategoryBarChartProps {
  data: Array<{
    id: string;
    label: string;
    amount: number;
    percentage: number;
  }>;
}

export const CategoryBarChart: React.FC<CategoryBarChartProps> = ({ data }) => {
  if (!data || data.length === 0) {
    return null;
  }

  const barColors = [
    colors.primary,
    '#36A2EB',
    '#FFCE56',
    '#4BC0C0',
    '#9966FF',
    '#FF9F40',
    '#C9CBCF',
  ];

  return (
    <View style={styles.container} accessibilityLabel="Category expense chart">
      {data.map((item, index) => {
        const barColor = barColors[index % barColors.length];
        const clampedPct = Math.min(Math.max(item.percentage, 0), 100);

        return (
          <View key={item.id || index} style={styles.categoryItem}>
            <View style={styles.itemHeader}>
              <View style={styles.labelGroup}>
                <View style={[styles.colorDot, { backgroundColor: barColor }]} />
                <AppText variant="body" style={styles.itemLabel} numberOfLines={1}>
                  {item.label}
                </AppText>
              </View>
              <View style={styles.valueGroup}>
                <AppText variant="body" style={styles.itemAmount}>
                  {formatCurrency(item.amount)}
                </AppText>
                <AppText variant="caption" color={colors.textSecondary} style={styles.itemPct}>
                  ({item.percentage.toFixed(1)}%)
                </AppText>
              </View>
            </View>

            <View style={styles.track}>
              <View
                style={[
                  styles.fill,
                  { width: `${clampedPct}%`, backgroundColor: barColor },
                ]}
              />
            </View>
          </View>
        );
      })}
    </View>
  );
};

interface TrendChartProps {
  trends: MonthlyTrendItem[];
}

export const TrendChart: React.FC<TrendChartProps> = ({ trends }) => {
  if (!trends || trends.length === 0) {
    return null;
  }

  // Find max value across income and expense for scaling vertical bars
  const maxVal = Math.max(
    ...trends.flatMap((t) => [t.totalIncome, t.totalExpense]),
    1
  );

  return (
    <View style={styles.container} accessibilityLabel="Multi-month spending trend chart">
      <View style={styles.legendRow}>
        <View style={styles.legendItem}>
          <View style={[styles.legendBox, { backgroundColor: colors.success }]} />
          <AppText variant="caption">Income</AppText>
        </View>
        <View style={styles.legendItem}>
          <View style={[styles.legendBox, { backgroundColor: colors.error }]} />
          <AppText variant="caption">Expense</AppText>
        </View>
      </View>

      <View style={styles.trendBarsRow}>
        {trends.map((item) => {
          const incomeHeightPct = Math.min((item.totalIncome / maxVal) * 100, 100);
          const expenseHeightPct = Math.min((item.totalExpense / maxVal) * 100, 100);

          return (
            <View key={item.month} style={styles.trendColumn}>
              <View style={styles.barPairContainer}>
                <View style={styles.singleBarSlot}>
                  <View
                    style={[
                      styles.verticalBarFill,
                      {
                        height: `${Math.max(incomeHeightPct, 4)}%`,
                        backgroundColor: colors.success,
                      },
                    ]}
                  />
                </View>
                <View style={styles.singleBarSlot}>
                  <View
                    style={[
                      styles.verticalBarFill,
                      {
                        height: `${Math.max(expenseHeightPct, 4)}%`,
                        backgroundColor: colors.error,
                      },
                    ]}
                  />
                </View>
              </View>
              <AppText variant="caption" color={colors.textSecondary} style={styles.monthLabel}>
                {formatMonthShort(item.month)}
              </AppText>
            </View>
          );
        })}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    width: '100%',
    marginVertical: spacing.sm,
  },
  categoryItem: {
    marginBottom: spacing.sm,
  },
  itemHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  labelGroup: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  colorDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    marginRight: spacing.xs,
  },
  itemLabel: {
    fontWeight: '600',
    flex: 1,
  },
  valueGroup: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  itemAmount: {
    fontWeight: '600',
  },
  itemPct: {
    marginLeft: spacing.xs,
    fontSize: 12,
  },
  track: {
    height: 8,
    backgroundColor: colors.background,
    borderRadius: borderRadius.sm,
    overflow: 'hidden',
  },
  fill: {
    height: '100%',
    borderRadius: borderRadius.sm,
  },
  legendRow: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    marginBottom: spacing.md,
  },
  legendItem: {
    flexDirection: 'row',
    alignItems: 'center',
    marginLeft: spacing.md,
  },
  legendBox: {
    width: 12,
    height: 12,
    borderRadius: 2,
    marginRight: spacing.xs,
  },
  trendBarsRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'flex-end',
    height: 140,
    paddingTop: spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  trendColumn: {
    alignItems: 'center',
    flex: 1,
    height: '100%',
    justifyContent: 'flex-end',
  },
  barPairContainer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    height: 100,
    width: 24,
    justifyContent: 'space-between',
  },
  singleBarSlot: {
    width: 10,
    height: '100%',
    justifyContent: 'flex-end',
  },
  verticalBarFill: {
    width: '100%',
    borderTopLeftRadius: 2,
    borderTopRightRadius: 2,
  },
  monthLabel: {
    marginTop: spacing.xs,
    fontSize: 11,
  },
});
