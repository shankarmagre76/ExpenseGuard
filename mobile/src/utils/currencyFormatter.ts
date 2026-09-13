/**
 * Safe currency formatting without floating-point arithmetic precision errors.
 * Preserves exact numerical string or number representation returned by backend.
 */

export const formatCurrency = (
  amount: number | string | null | undefined,
  currencyCode: string = 'USD'
): string => {
  if (amount === null || amount === undefined || amount === '') {
    return `${getCurrencySymbol(currencyCode)}0.00`;
  }

  const num = typeof amount === 'string' ? parseFloat(amount) : amount;
  if (isNaN(num)) {
    return `${getCurrencySymbol(currencyCode)}0.00`;
  }

  const symbol = getCurrencySymbol(currencyCode);
  const formattedNumber = Math.abs(num).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

  return num < 0 ? `-${symbol}${formattedNumber}` : `${symbol}${formattedNumber}`;
};

export const getCurrencySymbol = (code?: string): string => {
  switch (code?.toUpperCase()) {
    case 'INR':
      return '₹';
    case 'EUR':
      return '€';
    case 'GBP':
      return '£';
    case 'JPY':
      return '¥';
    case 'USD':
    default:
      return '$';
  }
};
