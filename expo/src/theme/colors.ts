export const colors = {
  paper: '#f4f1e7',
  paper2: '#e9e6da',
  ink: '#171817',
  ink2: '#2a2b29',
  muted: '#77796f',
  accent: '#b9d86d',
  accentSoft: '#d7ef8b',
  accentInk: '#1d250c',
  coral: '#f15b55',
  yolk: '#ffd15b',
  white: '#ffffff',
  line: 'rgba(23, 24, 23, 0.12)',
  navGlass: 'rgba(17, 17, 17, 0.97)',
  navBorder: 'rgba(255, 255, 255, 0.10)',
  navPill: '#292929',
  navTextActive: '#f2f2f2',
  navTextIdle: '#b7b7b7',
  navInsetHighlight: 'rgba(255, 255, 255, 0.025)',
  navShadow: 'rgba(0, 0, 0, 0.5)',
} as const;

export type AppColors = typeof colors;
