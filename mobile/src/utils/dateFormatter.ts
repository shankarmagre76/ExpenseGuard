/**
 * Date formatting helpers handling ISO calendar dates (YYYY-MM-DD) without timezone shifts.
 */

export const formatDateDisplay = (dateString?: string): string => {
  if (!dateString) return '';
  // If dateString is YYYY-MM-DD
  const parts = dateString.split('T')[0].split('-');
  if (parts.length === 3) {
    const year = parseInt(parts[0], 10);
    const month = parseInt(parts[1], 10) - 1;
    const day = parseInt(parts[2], 10);
    const date = new Date(year, month, day);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  }
  return dateString;
};

export const getTodayISODate = (): string => {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, '0');
  const day = String(today.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

export const getCurrentMonth = (): string => {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, '0');
  return `${year}-${month}`;
};

export const formatMonthDisplay = (monthStr?: string): string => {
  if (!monthStr || !monthStr.match(/^\d{4}-(0[1-9]|1[0-2])$/)) {
    return monthStr || '';
  }
  const [yearStr, monthNumStr] = monthStr.split('-');
  const year = parseInt(yearStr, 10);
  const monthIndex = parseInt(monthNumStr, 10) - 1;
  const date = new Date(year, monthIndex, 1);
  return date.toLocaleDateString('en-US', {
    month: 'long',
    year: 'numeric',
  });
};

export const formatMonthShort = (monthStr?: string): string => {
  if (!monthStr || !monthStr.match(/^\d{4}-(0[1-9]|1[0-2])$/)) {
    return monthStr || '';
  }
  const [yearStr, monthNumStr] = monthStr.split('-');
  const year = parseInt(yearStr, 10);
  const monthIndex = parseInt(monthNumStr, 10) - 1;
  const date = new Date(year, monthIndex, 1);
  return date.toLocaleDateString('en-US', {
    month: 'short',
  });
};

export const getPreviousMonth = (monthStr: string): string => {
  const [yearStr, monthNumStr] = monthStr.split('-');
  let year = parseInt(yearStr, 10);
  let monthIndex = parseInt(monthNumStr, 10) - 1;
  if (monthIndex === 0) {
    monthIndex = 11;
    year -= 1;
  } else {
    monthIndex -= 1;
  }
  const month = String(monthIndex + 1).padStart(2, '0');
  return `${year}-${month}`;
};

export const getNextMonth = (monthStr: string): string => {
  const [yearStr, monthNumStr] = monthStr.split('-');
  let year = parseInt(yearStr, 10);
  let monthIndex = parseInt(monthNumStr, 10) - 1;
  if (monthIndex === 11) {
    monthIndex = 0;
    year += 1;
  } else {
    monthIndex += 1;
  }
  const month = String(monthIndex + 1).padStart(2, '0');
  return `${year}-${month}`;
};

export const getTrendRange = (targetMonth: string, count: number = 6): { from: string; to: string } => {
  let curr = targetMonth;
  for (let i = 1; i < count; i++) {
    curr = getPreviousMonth(curr);
  }
  return { from: curr, to: targetMonth };
};

