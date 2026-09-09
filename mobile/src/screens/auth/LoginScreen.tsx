import React from 'react';
import { View, StyleSheet } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { AuthStackParamList } from '../../types/navigation';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { PrimaryButton } from '../../components/PrimaryButton';
import { colors, spacing } from '../../theme';

type Props = NativeStackScreenProps<AuthStackParamList, 'Login'>;

export const LoginScreen: React.FC<Props> = ({ navigation }) => {
  return (
    <ScreenContainer style={styles.container}>
      <View style={styles.content}>
        <AppText variant="title" color={colors.primary} style={styles.title}>
          ExpenseGuard
        </AppText>
        <AppText variant="subheader" style={styles.subtitle}>
          Sign in to your account
        </AppText>
        <AppText variant="body" color={colors.textSecondary} style={styles.placeholderNote}>
          Authentication features will be implemented in Phase 2.
        </AppText>

        <PrimaryButton
          title="Create Account"
          onPress={() => navigation.navigate('Register')}
          variant="outline"
          style={styles.button}
        />
      </View>
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  container: {
    justifyContent: 'center',
  },
  content: {
    padding: spacing.lg,
    alignItems: 'center',
  },
  title: {
    marginBottom: spacing.xs,
  },
  subtitle: {
    marginBottom: spacing.md,
  },
  placeholderNote: {
    textAlign: 'center',
    marginBottom: spacing.xl,
  },
  button: {
    width: '100%',
  },
});
