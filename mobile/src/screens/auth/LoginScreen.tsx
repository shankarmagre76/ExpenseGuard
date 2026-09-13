import React, { useState } from 'react';
import { View, StyleSheet, TextInput, TouchableOpacity, KeyboardAvoidingView, Platform } from 'react-native';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { AuthStackParamList } from '../../types/navigation';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { PrimaryButton } from '../../components/PrimaryButton';
import { ErrorMessage } from '../../components/ErrorMessage';
import { useAuth } from '../../hooks/useAuth';
import { colors, spacing, borderRadius } from '../../theme';

type Props = NativeStackScreenProps<AuthStackParamList, 'Login'>;

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const LoginScreen: React.FC<Props> = ({ navigation }) => {
  const { login, status, error, clearError } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const [emailError, setEmailError] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);

  const validateForm = (): boolean => {
    let isValid = true;
    setEmailError(null);
    setPasswordError(null);

    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      setEmailError('Email is required');
      isValid = false;
    } else if (!EMAIL_REGEX.test(trimmedEmail)) {
      setEmailError('Please enter a valid email address');
      isValid = false;
    }

    if (!password) {
      setPasswordError('Password is required');
      isValid = false;
    }

    return isValid;
  };

  const handleLogin = async () => {
    clearError();
    if (!validateForm()) {
      return;
    }

    try {
      await login({
        email: email.trim(),
        password,
      });
    } catch {
      // API error handled via AuthContext state error
    }
  };

  const isLoading = status === 'authenticating';

  return (
    <ScreenContainer scrollable>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.container}
      >
        <View style={styles.header}>
          <AppText variant="title" color={colors.primary} style={styles.brand}>
            ExpenseGuard
          </AppText>
          <AppText variant="subheader" style={styles.subtitle}>
            Welcome Back
          </AppText>
          <AppText variant="body" color={colors.textSecondary}>
            Sign in to track your personal finances
          </AppText>
        </View>

        {error ? (
          <ErrorMessage
            title="Authentication Error"
            message={error}
            onRetry={clearError}
            style={styles.errorBanner}
          />
        ) : null}

        <View style={styles.form}>
          {/* Email Field */}
          <View style={styles.inputGroup}>
            <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
              EMAIL ADDRESS
            </AppText>
            <TextInput
              style={[styles.input, emailError ? styles.inputInvalid : null]}
              placeholder="name@example.com"
              placeholderTextColor={colors.textMuted}
              value={email}
              onChangeText={(text) => {
                setEmail(text);
                if (emailError) setEmailError(null);
              }}
              keyboardType="email-address"
              autoCapitalize="none"
              autoCorrect={false}
              editable={!isLoading}
            />
            {emailError ? (
              <AppText variant="caption" color={colors.error} style={styles.fieldError}>
                {emailError}
              </AppText>
            ) : null}
          </View>

          {/* Password Field */}
          <View style={styles.inputGroup}>
            <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
              PASSWORD
            </AppText>
            <View style={[styles.passwordWrapper, passwordError ? styles.inputInvalid : null]}>
              <TextInput
                style={styles.passwordInput}
                placeholder="Enter your password"
                placeholderTextColor={colors.textMuted}
                value={password}
                onChangeText={(text) => {
                  setPassword(text);
                  if (passwordError) setPasswordError(null);
                }}
                secureTextEntry={!showPassword}
                autoCapitalize="none"
                editable={!isLoading}
              />
              <TouchableOpacity
                onPress={() => setShowPassword(!showPassword)}
                style={styles.eyeToggle}
                disabled={isLoading}
              >
                <AppText variant="caption" color={colors.primary} bold>
                  {showPassword ? 'HIDE' : 'SHOW'}
                </AppText>
              </TouchableOpacity>
            </View>
            {passwordError ? (
              <AppText variant="caption" color={colors.error} style={styles.fieldError}>
                {passwordError}
              </AppText>
            ) : null}
          </View>

          {/* Submit Button */}
          <PrimaryButton
            title="Sign In"
            onPress={handleLogin}
            isLoading={isLoading}
            disabled={isLoading}
            style={styles.submitButton}
          />

          {/* Register Link */}
          <View style={styles.footerRow}>
            <AppText variant="body" color={colors.textSecondary}>
              Don't have an account?{' '}
            </AppText>
            <TouchableOpacity onPress={() => navigation.navigate('Register')} disabled={isLoading}>
              <AppText variant="body" color={colors.primary} bold>
                Sign Up
              </AppText>
            </TouchableOpacity>
          </View>
        </View>
      </KeyboardAvoidingView>
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    paddingVertical: spacing.xl,
  },
  header: {
    alignItems: 'center',
    marginBottom: spacing.xl,
  },
  brand: {
    fontSize: 32,
    fontWeight: '700',
    marginBottom: spacing.xs,
  },
  subtitle: {
    marginBottom: spacing.xs,
  },
  errorBanner: {
    marginBottom: spacing.md,
  },
  form: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.lg,
    borderWidth: 1,
    borderColor: colors.border,
  },
  inputGroup: {
    marginBottom: spacing.md,
  },
  label: {
    marginBottom: spacing.xs,
    fontWeight: '600',
  },
  input: {
    backgroundColor: colors.background,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: borderRadius.md,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    fontSize: 16,
    color: colors.textPrimary,
  },
  inputInvalid: {
    borderColor: colors.error,
  },
  passwordWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.background,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: borderRadius.md,
  },
  passwordInput: {
    flex: 1,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    fontSize: 16,
    color: colors.textPrimary,
  },
  eyeToggle: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  fieldError: {
    marginTop: 4,
  },
  submitButton: {
    marginTop: spacing.sm,
    marginBottom: spacing.md,
  },
  footerRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
  },
});
