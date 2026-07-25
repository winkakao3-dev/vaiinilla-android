export interface SeedAccount {
  email: string;
  membresiaId: string;
  role: string;
  label: string;
}

export const SEED_PASSWORD = 'saul1234';

export const SEED_ACCOUNTS: SeedAccount[] = [
  {
    email: 'cliente@vaiinilla.test',
    membresiaId: '9c3334d4-1ed6-4cc2-a4a8-785a6afd03f3',
    role: 'cliente',
    label: 'Cliente (alumno)',
  },
  {
    email: 'cajero@vaiinilla.test',
    membresiaId: 'a1111111-0000-4000-8000-0000000000a1',
    role: 'cajero',
    label: 'Cajero',
  },
  {
    email: 'cocina@vaiinilla.test',
    membresiaId: 'a1111111-0000-4000-8000-0000000000a2',
    role: 'cocina',
    label: 'Cocina',
  },
  {
    email: 'mesero@vaiinilla.test',
    membresiaId: 'a1111111-0000-4000-8000-0000000000a3',
    role: 'mesero',
    label: 'Mesero',
  },
];

export function findSeedAccount(email: string): SeedAccount | undefined {
  return SEED_ACCOUNTS.find((account) => account.email === email.trim().toLowerCase());
}
