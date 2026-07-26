import { Redirect } from 'expo-router';
import { useCallback, useState } from 'react';

import { SplashScreen } from '@/screens/splash-screen';

export default function IndexRoute() {
  const [done, setDone] = useState(false);
  const onFinished = useCallback(() => setDone(true), []);

  if (!done) {
    return <SplashScreen onFinished={onFinished} />;
  }

  return <Redirect href="/roles" />;
}
