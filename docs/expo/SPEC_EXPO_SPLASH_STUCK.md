# MASTER SPEC — Fix APK stuck on splash logo

**Branch:** `cursor/expo-vaiinilla-5c6b`  
**Lead already wrote this SPEC — execute it verbatim.**  
**Env:** Mac local (`/Users/kakao/Projects/vaiinilla-android`). Not cloud.

## Teardown verdict

**Fix first** — cold start is Broken on release APK.

### Broken — Native splash never dismisses if fonts stall
**Location:** `expo/src/app/_layout.tsx`  
**What:** `SplashScreen.preventAutoHideAsync()` then `hideAsync()` only when `useFonts` → `loaded === true`. On error/hang, `loaded` stays false → user stares at `splash-icon.png` forever.  
**Seen:** User installed release APK; app does not leave logo. Code path is unconditional.  
**Consequence:** App unusable after install.  
**Fix:** Hide native splash when fonts load **or** error **or** after **2500ms** timeout. Render the tree even if fonts failed (system font fallback already via `Platform.select` in typography).  
**Trade-off:** First paint may use fallback fonts briefly.

### Fragile — Animated splash `onFinished` cancelled by remounts
**Location:** `expo/src/app/index.tsx` + `expo/src/screens/splash-screen.tsx`  
**What:** `onFinished={() => setDone(true)}` is a new function each render; effect deps include `onFinished`. Provider/`Stack` re-renders cancel the animation (`cancelled = true`) before `onFinished`.  
**Fix:** Call `onFinished` via `useRef` (stable); add **hard timeout 3500ms** that always finishes; ignore late cancel for the timeout path.  
**Trade-off:** Slightly less “perfect” animation if interrupted.

## Scope (touch only)

- `expo/src/app/_layout.tsx`
- `expo/src/app/index.tsx`
- `expo/src/screens/splash-screen.tsx`
- Optional one-liner in `expo/README.md` or `expo/COMO_CORRER.md` if useful

Do **not** touch Android Kotlin, Firebase keys, admin screens, theme tokens.

## Implementation details

### A. `_layout.tsx`
```ts
const [loaded, error] = useFonts({ ... });

useEffect(() => {
  if (loaded || error) {
    SplashScreen.hideAsync().catch(() => undefined);
  }
}, [loaded, error]);

useEffect(() => {
  const t = setTimeout(() => {
    SplashScreen.hideAsync().catch(() => undefined);
  }, 2500);
  return () => clearTimeout(t);
}, []);

// Proceed when loaded OR error OR after timeout gate:
const [forceReady, setForceReady] = useState(false);
useEffect(() => {
  const t = setTimeout(() => setForceReady(true), 2500);
  return () => clearTimeout(t);
}, []);

if (!loaded && !error && !forceReady) {
  return <View style={{ flex: 1, backgroundColor: colors.paper }} />;
}
// then providers + Stack as today
```

### B. `splash-screen.tsx`
- Store latest `onFinished` in a ref; effect depends on `[]` or animation values only
- Always schedule `setTimeout(..., 3500)` that calls `onFinishedRef.current()` once (guard with `finished` flag)
- Keep reduced-motion fast path
- Keep existing animation if it completes earlier (same `finished` guard)

### C. `index.tsx`
- Prefer `useCallback` for `onFinished` **or** rely on ref inside SplashScreen (B is enough)
- Optional: skip long splash in `__DEV__` — not required

## Acceptance

- [ ] Cold start always leaves native splash within ≤3s even if fonts fail
- [ ] Animated splash always navigates to `/roles` within ≤4s
- [ ] `npm run typecheck` green in `expo/`
- [ ] No Firebase/env changes
- [ ] Do not commit/push (lead will)

## Footguns

1. Do not remove `preventAutoHideAsync` without also ensuring hide on all paths  
2. Do not block the whole app on font download forever  
3. Do not rewrite navigation architecture  

## Closing

Execute verbatim. Prefer minimal surgical fix.
