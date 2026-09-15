import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { RootStackParamList } from '../types/navigation';
import { AuthNavigator } from './AuthNavigator';
import { MainNavigator } from './MainNavigator';
import { HealthCheckScreen } from '../screens/debug/HealthCheckScreen';
import { AddExpenseScreen } from '../screens/transactions/AddExpenseScreen';
import { AddIncomeScreen } from '../screens/transactions/AddIncomeScreen';
import { EditTransactionScreen } from '../screens/transactions/EditTransactionScreen';
import { ReceiptUploadScreen } from '../screens/receipt/ReceiptUploadScreen';
import { OcrReviewScreen } from '../screens/receipt/OcrReviewScreen';
import { NotificationsScreen } from '../screens/notifications/NotificationsScreen';
import { ConflictListScreen } from '../screens/conflicts/ConflictListScreen';
import { ConflictResolutionScreen } from '../screens/conflicts/ConflictResolutionScreen';
import { useAuth } from '../hooks/useAuth';
import { ScreenContainer } from '../components/ScreenContainer';
import { LoadingIndicator } from '../components/LoadingIndicator';
import { colors } from '../theme';

const RootStack = createNativeStackNavigator<RootStackParamList>();

export const AppNavigator: React.FC = () => {
  const { status } = useAuth();

  if (status === 'restoring') {
    return (
      <ScreenContainer>
        <LoadingIndicator message="Restoring ExpenseGuard session..." />
      </ScreenContainer>
    );
  }

  return (
    <NavigationContainer>
      <RootStack.Navigator screenOptions={{ headerShown: false }}>
        {status === 'authenticated' ? (
          <>
            <RootStack.Screen name="Main" component={MainNavigator} />
            <RootStack.Screen
              name="AddExpense"
              component={AddExpenseScreen}
              options={{
                headerShown: true,
                title: 'Add Expense',
                headerStyle: { backgroundColor: colors.surface },
                headerTintColor: colors.textPrimary,
              }}
            />
            <RootStack.Screen
              name="AddIncome"
              component={AddIncomeScreen}
              options={{
                headerShown: true,
                title: 'Add Income',
                headerStyle: { backgroundColor: colors.surface },
                headerTintColor: colors.textPrimary,
              }}
            />
            <RootStack.Screen
              name="EditTransaction"
              component={EditTransactionScreen}
              options={{
                headerShown: true,
                title: 'Edit Transaction',
                headerStyle: { backgroundColor: colors.surface },
                headerTintColor: colors.textPrimary,
              }}
            />
            <RootStack.Screen
              name="ReceiptUpload"
              component={ReceiptUploadScreen}
              options={{
                headerShown: true,
                title: 'Scan Receipt',
                headerStyle: { backgroundColor: colors.surface },
                headerTintColor: colors.textPrimary,
              }}
            />
            <RootStack.Screen
              name="OcrReview"
              component={OcrReviewScreen}
              options={{
                headerShown: true,
                title: 'Review OCR Data',
                headerStyle: { backgroundColor: colors.surface },
                headerTintColor: colors.textPrimary,
              }}
            />
            <RootStack.Screen
              name="Notifications"
              component={NotificationsScreen}
              options={{
                headerShown: true,
                title: 'Notifications',
                headerStyle: { backgroundColor: colors.surface },
                headerTintColor: colors.textPrimary,
              }}
            />
            <RootStack.Screen
              name="ConflictList"
              component={ConflictListScreen}
              options={{
                headerShown: true,
                title: 'Conflicts',
                headerStyle: { backgroundColor: colors.surface },
                headerTintColor: colors.textPrimary,
              }}
            />
            <RootStack.Screen
              name="ConflictResolution"
              component={ConflictResolutionScreen}
              options={{
                headerShown: true,
                title: 'Resolve Conflict',
                headerStyle: { backgroundColor: colors.surface },
                headerTintColor: colors.textPrimary,
              }}
            />
          </>
        ) : (
          <RootStack.Screen name="Auth" component={AuthNavigator} />
        )}
        <RootStack.Screen
          name="HealthCheck"
          component={HealthCheckScreen}
          options={{
            headerShown: true,
            title: 'Backend Health Check',
          }}
        />
      </RootStack.Navigator>
    </NavigationContainer>
  );
};
