/**
 * Tokens portados 1:1 desde el demo (`:root` de Vaiinilla_Demo_Web_IA_CHAT.html).
 * No inventar valores: si el demo cambia, se cambian aqui.
 */
export const colors = {
  // --- superficies / texto (light) ---
  paper: '#f4f1e7', // --paper
  paper2: '#e9e6da', // --paper-2
  ink: '#171817', // --ink
  ink2: '#2a2b29', // --ink-2
  muted: '#77796f', // --muted
  line: 'rgba(23,24,23,.12)', // --line

  // --- acentos ---
  accent: '#b9d86d', // --accent
  accent2: '#d7ef8b', // --accent-2
  accentSoft: '#d7ef8b', // alias legacy
  accentInk: '#1d250c', // --accent-ink
  coral: '#f15b55', // --coral
  yolk: '#ffd15b', // --yolk
  yolkInk: '#28200b', // role-card:nth-child(2)
  slate: '#262724', // role-card:nth-child(3)
  slateInk: '#f7f4e9',

  white: '#ffffff',

  // --- nav (.nav del demo: negro solido, no glass) ---
  navBg: '#171817',
  navPill: 'rgba(255,255,255,.10)',
  navTextActive: '#ffffff',
  navTextIdle: '#a8aaa3',

  // --- toast / push ---
  toastBg: '#171817',
  toastInk: '#ffffff',
  pushBg: '#ffffff',
  pushInk: '#171817',

  // --- sheet ---
  sheetBackdrop: 'rgba(0,0,0,.58)',
} as const;

/** Variante oscura (.app.dark del demo). */
export const darkColors = {
  ...colors,
  paper: '#1d1e1c',
  paper2: '#292a27',
  ink: '#f5f1e5',
  ink2: '#dedbd0',
  muted: '#aaa99f',
  line: 'rgba(255,255,255,.12)',
  accentInk: '#182008',
} as const;

/** Sombra --shadow: 0 18px 46px rgba(19,22,18,.18) */
export const shadow = {
  shadowColor: 'rgb(19,22,18)',
  shadowOpacity: 0.18,
  shadowRadius: 46 / 2,
  shadowOffset: { width: 0, height: 18 },
  elevation: 14,
} as const;

/** Sombra de la nav: 0 15px 40px rgba(0,0,0,.26) */
export const navShadow = {
  shadowColor: '#000',
  shadowOpacity: 0.26,
  shadowRadius: 20,
  shadowOffset: { width: 0, height: 15 },
  elevation: 18,
} as const;

export type AppColors = typeof colors;
