import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { RootStackParamList } from '../types/navigation';
import { AuthNavigator } from './AuthNavigator';
import { MainNavigator } from './MainNavigator';
import { HealthCheckScreen } from '../screens/debug/HealthCheckScreen';
import { useAuth } from '../hooks/useAuth';
import { ScreenContainer } from '../components/ScreenContainer';
import { LoadingIndicator } from '../components/LoadingIndicator';

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
          <RootStack.Screen name="Main" component={MainNavigator} />
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
