import React from 'react';
import { View, StyleSheet } from 'react-native';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { colors, spacing, borderRadius } from '../../theme';

export const AccountsScreen: React.FC = () => {
  return (
    <ScreenContainer scrollable>
      <View style={styles.card}>
        <AppText variant="header" style={styles.title}>
          Financial Accounts
        </AppText>
        <AppText variant="body" color={colors.textSecondary}>
          Bank accounts, credit cards, wallet management, and balances integration will be added in Phase 2.
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
