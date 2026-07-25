import { router } from 'expo-router';

import { RoleSelectorScreen } from '@/screens/role-selector-screen';
import { useOrderFlow } from '@/hooks/use-order-flow';

export default function RolesRoute() {
  const flow = useOrderFlow();

  return (
    <RoleSelectorScreen
      testOnlyMode={flow.testOnlyMode}
      onTestOnlyModeChange={flow.setTestOnlyMode}
      onStudentSelected={() => router.push('/(student)/menu')}
    />
  );
}
