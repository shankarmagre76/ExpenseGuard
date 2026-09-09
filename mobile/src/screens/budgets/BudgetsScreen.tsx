import React from 'react';
import { View, StyleSheet } from 'react-native';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { colors, spacing, borderRadius } from '../../theme';

export const BudgetsScreen: React.FC = () => {
  return (
    <ScreenContainer scrollable>
      <View style={styles.card}>
        <AppText variant="header" style={styles.title}>
          Budgets & Limits
        </AppText>
        <AppText variant="body" color={colors.textSecondary}>
          Monthly budget creation, alert thresholds, and spending velocity tracking will be added in Phase 3.
        </AppText>
      </View>
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.surface,
    padding: spacing.lg,
    borderRadius: borderRadius.md,
    borderWidth: 1,
    borderColor: colors.border,
  },
  title: {
    marginBottom: spacing.sm,
  },
});
