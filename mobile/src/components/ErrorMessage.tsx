import React from 'react';
import { View, StyleSheet } from 'react-native';
import { AppText } from './AppText';
import { PrimaryButton } from './PrimaryButton';
import { colors, spacing, borderRadius } from '../theme';

export interface ErrorMessageProps {
  title?: string;
  message: string;
  onRetry?: () => void;
}

export const ErrorMessage: React.FC<ErrorMessageProps> = ({
  title = 'Connection Error',
  message,
  onRetry,
}) => {
  return (
    <View style={styles.card}>
      <AppText variant="subheader" color={colors.error} bold style={styles.title}>
        {title}
      </AppText>
      <AppText variant="body" color={colors.textSecondary} style={styles.message}>
        {message}
      </AppText>
      {onRetry ? (
        <PrimaryButton
          title="Retry Connection"
          onPress={onRetry}
          variant="outline"
          style={styles.button}
        />
      ) : null}
    </View>
  );
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.errorLight,
    borderColor: colors.error,
    borderWidth: 1,
    borderRadius: borderRadius.md,
    padding: spacing.md,
    marginVertical: spacing.md,
  },
  title: {
    marginBottom: spacing.xs,
  },
  message: {
    marginBottom: spacing.md,
  },
  button: {
    height: 40,
  },
});
