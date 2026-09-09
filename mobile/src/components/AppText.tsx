import React from 'react';
import { Text, TextStyle, StyleSheet, TextProps } from 'react-native';
import { colors, typography } from '../theme';

export interface AppTextProps extends TextProps {
  variant?: 'title' | 'header' | 'subheader' | 'body' | 'caption' | 'button';
  color?: string;
  bold?: boolean;
  semibold?: boolean;
  align?: 'auto' | 'left' | 'right' | 'center' | 'justify';
  style?: TextStyle | TextStyle[];
}

export const AppText: React.FC<AppTextProps> = ({
  children,
  variant = 'body',
  color,
  bold,
  semibold,
  align,
  style,
  ...rest
}) => {
  const getVariantStyle = (): TextStyle => {
    switch (variant) {
      case 'title':
        return styles.title;
      case 'header':
        return styles.header;
      case 'subheader':
        return styles.subheader;
      case 'button':
        return styles.button;
      case 'caption':
        return styles.caption;
      case 'body':
      default:
        return styles.body;
    }
  };

  const textStyles: TextStyle[] = [
    getVariantStyle(),
    color ? { color } : undefined,
    align ? { textAlign: align } : undefined,
    bold ? { fontWeight: typography.fontWeight.bold } : undefined,
    semibold ? { fontWeight: typography.fontWeight.semibold } : undefined,
    ...(Array.isArray(style) ? style : [style]),
  ].filter(Boolean) as TextStyle[];

  return (
    <Text style={textStyles} {...rest}>
      {children}
    </Text>
  );
};

const styles = StyleSheet.create({
  title: {
    fontSize: typography.fontSize.title,
    fontWeight: typography.fontWeight.bold,
    color: colors.textPrimary,
  },
  header: {
    fontSize: typography.fontSize.xl,
    fontWeight: typography.fontWeight.semibold,
    color: colors.textPrimary,
  },
  subheader: {
    fontSize: typography.fontSize.lg,
    fontWeight: typography.fontWeight.medium,
    color: colors.textSecondary,
  },
  body: {
    fontSize: typography.fontSize.md,
    fontWeight: typography.fontWeight.regular,
    color: colors.textPrimary,
  },
  caption: {
    fontSize: typography.fontSize.xs,
    fontWeight: typography.fontWeight.regular,
    color: colors.textMuted,
  },
  button: {
    fontSize: typography.fontSize.md,
    fontWeight: typography.fontWeight.semibold,
    color: colors.surface,
  },
});
