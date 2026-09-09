import React from 'react';
import { View, StyleSheet } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { AuthStackParamList } from '../../types/navigation';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { PrimaryButton } from '../../components/PrimaryButton';
import { colors, spacing } from '../../theme';

type Props = NativeStackScreenProps<AuthStackParamList, 'Register'>;

export const RegisterScreen: React.FC<Props> = ({ navigation }) => {
  return (
    <ScreenContainer style={styles.container}>
      <View style={styles.content}>
        <AppText variant="header" style={styles.title}>
          Register Account
        </AppText>
        <AppText variant="body" color={colors.textSecondary} style={styles.placeholderNote}>
          Registration flow will be integrated in Phase 2.
        </AppText>

        <PrimaryButton
          title="Back to Sign In"
          onPress={() => navigation.navigate('Login')}
          variant="secondary"
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
