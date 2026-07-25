export interface DemoSpace {
  id: number;
  number: number;
  name: string;
}

export const DEMO_SPACES: DemoSpace[] = Array.from({ length: 6 }, (_, index) => {
  const number = index + 1;
  return { id: 700 + number, number, name: `Mesa ${number}` };
});

export const DEFAULT_SPACE = DEMO_SPACES.find((space) => space.number === 4)!;

export const DEMO_SPACE_IDS = new Set(DEMO_SPACES.map((space) => space.id));

export const SPACE_TYPE = 'mesa';

export function spaceForId(id: number): DemoSpace | undefined {
  return DEMO_SPACES.find((space) => space.id === id);
}

export function isInstantDemoPayment(method: 'efectivo' | 'saldo' | 'tarjeta'): boolean {
  return method === 'saldo' || method === 'tarjeta';
}
