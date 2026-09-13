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

type Props = NativeStackScreenProps<AuthStackParamList, 'Register'>;

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const RegisterScreen: React.FC<Props> = ({ navigation }) => {
  const { register, status, error, clearError } = useAuth();

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const [nameError, setNameError] = useState<string | null>(null);
  const [emailError, setEmailError] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);

  const validateForm = (): boolean => {
    let isValid = true;
    setNameError(null);
    setEmailError(null);
    setPasswordError(null);

    const trimmedName = name.trim();
    if (!trimmedName) {
      setNameError('Full name is required');
      isValid = false;
    } else if (trimmedName.length > 100) {
      setNameError('Name must not exceed 100 characters');
      isValid = false;
    }

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
    } else if (password.length < 8) {
      setPasswordError('Password must be at least 8 characters long');
      isValid = false;
    }

    return isValid;
  };

  const handleRegister = async () => {
    clearError();
    if (!validateForm()) {
      return;
    }

    try {
      await register({
        name: name.trim(),
        email: email.trim(),
        password,
      });
    } catch {
      // API error set in AuthContext
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
            Create Account
          </AppText>
          <AppText variant="body" color={colors.textSecondary}>
            Start tracking your income and expenses today
          </AppText>
        </View>

        {error ? (
          <ErrorMessage
            title="Registration Failed"
            message={error}
            onRetry={clearError}
            style={styles.errorBanner}
          />
        ) : null}

        <View style={styles.form}>
          {/* Full Name Field */}
          <View style={styles.inputGroup}>
            <AppText variant="caption" color={colors.textSecondary} style={styles.label}>
              FULL NAME
            </AppText>
            <TextInput
              style={[styles.input, nameError ? styles.inputInvalid : null]}
              placeholder="John Doe"
              placeholderTextColor={colors.textMuted}
              value={name}
              onChangeText={(text) => {
                setName(text);
                if (nameError) setNameError(null);
              }}
              autoCapitalize="words"
              editable={!isLoading}
            />
            {nameError ? (
              <AppText variant="caption" color={colors.error} style={styles.fieldError}>
                {nameError}
              </AppText>
            ) : null}
          </View>

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
              PASSWORD (MIN 8 CHARACTERS)
            </AppText>
            <View style={[styles.passwordWrapper, passwordError ? styles.inputInvalid : null]}>
              <TextInput
                style={styles.passwordInput}
                placeholder="At least 8 characters"
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
            title="Create Account"
            onPress={handleRegister}
            isLoading={isLoading}
            disabled={isLoading}
            style={styles.submitButton}
          />

          {/* Login Link */}
          <View style={styles.footerRow}>
            <AppText variant="body" color={colors.textSecondary}>
              Already have an account?{' '}
            </AppText>
            <TouchableOpacity onPress={() => navigation.navigate('Login')} disabled={isLoading}>
              <AppText variant="body" color={colors.primary} bold>
                Sign In
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
