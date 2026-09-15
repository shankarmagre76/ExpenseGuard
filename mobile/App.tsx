import React from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { AuthProvider } from './src/context/AuthContext';
import { SyncProvider } from './src/context/SyncContext';
import { AppNavigator } from './src/navigation/AppNavigator';

const App: React.FC = () => {
  return (
    <SafeAreaProvider>
      <AuthProvider>
        <SyncProvider>
          <AppNavigator />
        </SyncProvider>
      </AuthProvider>
    </SafeAreaProvider>
  );
};

export default App;
