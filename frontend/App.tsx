import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { AuthProvider } from './src/domain/auth/hooks/useAuth';
import RootNavigator from './src/navigation/RootNavigator';
import { DialogHost } from './src/shared/dialog';

export default function App() {
  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <AuthProvider>
        <RootNavigator />
        {/* 화면 위에 떠야 하므로 네비게이터 다음에 둔다 */}
        <DialogHost />
      </AuthProvider>
    </GestureHandlerRootView>
  );
}
