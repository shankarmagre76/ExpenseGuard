import React from 'react';
import { View, StyleSheet } from 'react-native';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { colors, spacing, borderRadius } from '../../theme';

export const AnalyticsScreen: React.FC = () => {
  return (
    <ScreenContainer scrollable>
      <View style={styles.card}>
        <AppText variant="header" style={styles.title}>
          Financial Analytics
        </AppText>
        <AppText variant="body" color={colors.textSecondary}>
          Spending breakdown, monthly trends, and charts will be implemented in Phase 3.
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
