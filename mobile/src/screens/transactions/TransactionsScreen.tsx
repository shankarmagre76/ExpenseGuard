import React from 'react';
import { View, StyleSheet } from 'react-native';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { colors, spacing, borderRadius } from '../../theme';

export const TransactionsScreen: React.FC = () => {
  return (
    <ScreenContainer scrollable>
      <View style={styles.card}>
        <AppText variant="header" style={styles.title}>
          Transactions Management
        </AppText>
        <AppText variant="body" color={colors.textSecondary}>
          Full transaction history, category filtering, search, and manual transaction logging will be introduced in Phase 2.
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
