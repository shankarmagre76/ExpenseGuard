import { colors } from './colors';
import { spacing } from './spacing';
import { borderRadius } from './borderRadius';
import { typography } from './typography';

export const theme = {
  colors,
  spacing,
  borderRadius,
  typography,
};

export type Theme = typeof theme;
export { colors, spacing, borderRadius, typography };
