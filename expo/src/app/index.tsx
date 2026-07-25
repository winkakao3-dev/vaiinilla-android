import { Redirect } from 'expo-router';
import { useState } from 'react';

import { SplashScreen } from '@/screens/splash-screen';

export default function IndexRoute() {
  const [done, setDone] = useState(false);

  if (!done) {
    return <SplashScreen onFinished={() => setDone(true)} />;
  }

  return <Redirect href="/roles" />;
}
