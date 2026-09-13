import React from 'react';
import { View, StyleSheet, TouchableOpacity } from 'react-native';
import { AppText } from './AppText';
import { colors, spacing, borderRadius } from '../theme';
import {
  formatMonthDisplay,
  getPreviousMonth,
  getNextMonth,
  getCurrentMonth,
} from '../utils/dateFormatter';

interface MonthSelectorProps {
  selectedMonth: string;
  onMonthChange: (newMonth: string) => void;
}

export const MonthSelector: React.FC<MonthSelectorProps> = ({
  selectedMonth,
  onMonthChange,
}) => {
  const isCurrentMonth = selectedMonth === getCurrentMonth();

  const handlePrev = () => {
    onMonthChange(getPreviousMonth(selectedMonth));
  };

  const handleNext = () => {
    onMonthChange(getNextMonth(selectedMonth));
  };

  const handleResetCurrent = () => {
    onMonthChange(getCurrentMonth());
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity
        style={styles.navButton}
        onPress={handlePrev}
        accessibilityLabel="Previous month"
        accessibilityRole="button"
      >
        <AppText variant="subheader" color={colors.primary}>
          ‹ Prev
        </AppText>
      </TouchableOpacity>

      <TouchableOpacity
        style={styles.monthDisplay}
        onPress={handleResetCurrent}
        accessibilityLabel={`Selected month ${formatMonthDisplay(selectedMonth)}. Tap to reset to current month`}
        accessibilityRole="button"
      >
        <AppText variant="subheader" style={styles.monthText}>
          {formatMonthDisplay(selectedMonth)}
        </AppText>
        {!isCurrentMonth && (
          <AppText variant="caption" color={colors.primary} style={styles.todayBadge}>
            Go to Current
          </AppText>
        )}
      </TouchableOpacity>

      <TouchableOpacity
        style={styles.navButton}
        onPress={handleNext}
        accessibilityLabel="Next month"
        accessibilityRole="button"
      >
        <AppText variant="subheader" color={colors.primary}>
          Next ›
        </AppText>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: colors.surface,
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.md,
    borderRadius: borderRadius.md,
    borderWidth: 1,
    borderColor: colors.border,
    marginBottom: spacing.md,
  },
  navButton: {
    paddingVertical: spacing.xs,
    paddingHorizontal: spacing.sm,
  },
  monthDisplay: {
    alignItems: 'center',
  },
  monthText: {
    fontWeight: '700',
    color: colors.textPrimary,
  },
  todayBadge: {
    marginTop: 2,
    fontWeight: '600',
  },
});
