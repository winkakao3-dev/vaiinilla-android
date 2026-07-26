/**
 * Espaciados y radios portados del demo.
 * Radios: --r-xl:34 --r-lg:28 --r-md:20 --r-sm:14
 */
export const spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 24,
  /** .screen-body { padding: 50px 20px 108px } */
  screen: 20,
  screenTop: 50,
  screenBottom: 108,
  screenBottomNoNav: 28,
} as const;

export const radius = {
  xl: 34, // --r-xl
  lg: 28, // --r-lg
  md: 20, // --r-md
  sm: 14, // --r-sm

  // radios concretos usados por componentes del demo
  phone: 39, // .app
  hero: 34, // .hero -> var(--r-xl)
  card: 28, // .product-card / .role-card
  sheet: 34, // .sheet border-radius 34 34 0 0
  sheetHero: 26,
  nav: 24, // .nav
  navItem: 18, // .nav button
  iconBtn: 16, // .icon-btn
  primary: 18, // .primary
  chip: 13, // .chip
  tab: 14, // .tab
  search: 19, // .search
  lineItem: 22, // .line-item
  lineItemImg: 18,
  miniCard: 22, // .mini-card
  taskCard: 30, // .task-card
  assistantHero: 32, // .assistant-hero
  balanceCard: 32, // .balance-card
  successMark: 42, // .success-mark
  toast: 16, // .toast
  push: 22, // .push
  pill: 999,

  // alias legacy (se mantienen para no romper pantallas aun no migradas)
  button: 18,
} as const;

/** Alto de la nav y su separacion del borde (.nav) */
export const nav = {
  height: 68,
  inset: 18,
  bottom: 15,
  padding: 7,
  itemHeight: 54,
  gap: 4,
} as const;
