import React from 'react';
import { View, StyleSheet } from 'react-native';
import { AppText } from './AppText';
import { colors, spacing, borderRadius } from '../theme';
import { getBudgetStatus, getBudgetStatusLabel, BudgetStatus } from '../types/budget';

interface BudgetProgressBarProps {
  utilizationPercentage: number;
  showDetails?: boolean;
}

export const BudgetProgressBar: React.FC<BudgetProgressBarProps> = ({
  utilizationPercentage,
  showDetails = true,
}) => {
  const status: BudgetStatus = getBudgetStatus(utilizationPercentage);
  const statusLabel = getBudgetStatusLabel(status);

  const getStatusColor = (): string => {
    switch (status) {
      case 'EXCEEDED':
        return colors.error;
      case 'REACHED':
        return '#E65100'; // Dark Warning/Alert
      case 'WARNING':
        return colors.warning;
      case 'OK':
      default:
        return colors.success;
    }
  };

  const barColor = getStatusColor();
  const clampedWidthPercentage = Math.min(Math.max(utilizationPercentage, 0), 100);

  return (
    <View
      style={styles.container}
      accessibilityLabel={`Budget utilization ${utilizationPercentage.toFixed(1)} percent, status ${statusLabel}`}
    >
      <View style={styles.track}>
        <View
          style={[
            styles.fill,
            { width: `${clampedWidthPercentage}%`, backgroundColor: barColor },
          ]}
        />
      </View>

      {showDetails && (
        <View style={styles.detailsRow}>
          <AppText variant="caption" color={colors.textSecondary}>
            {utilizationPercentage.toFixed(1)}% used
          </AppText>
          <View style={[styles.statusBadge, { backgroundColor: `${barColor}1F` }]}>
            <AppText variant="caption" style={[styles.statusBadgeText, { color: barColor }]}>
              {statusLabel}
            </AppText>
          </View>
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    width: '100%',
    marginVertical: spacing.xs,
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
  detailsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: spacing.xs,
  },
  statusBadge: {
    paddingHorizontal: spacing.xs,
    paddingVertical: 2,
    borderRadius: borderRadius.sm,
  },
  statusBadgeText: {
    fontWeight: '700',
  },
});
