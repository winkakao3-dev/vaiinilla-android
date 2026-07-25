import { isInstantDemoPayment } from '@/domain/checkout-fixtures';
import type { OrderDestination, OrderState, PaymentMethod } from '@/domain/models';

export interface TimelineStep {
  state: OrderState;
  title: (payment: PaymentMethod) => string;
  description: (destination: OrderDestination, payment: PaymentMethod) => string;
}

export const TRACKING_TIMELINE: TimelineStep[] = [
  {
    state: 'por_cobrar',
    title: (payment) => (isInstantDemoPayment(payment) ? 'PAGO CONFIRMADO' : 'POR COBRAR'),
    description: (_destination, payment) =>
      isInstantDemoPayment(payment)
        ? 'Saldo descontado y pedido enviado.'
        : 'Caja espera el pago en efectivo.',
  },
  {
    state: 'cobrado',
    title: () => 'COBRADO',
    description: () => 'Cocina recibió la comanda.',
  },
  {
    state: 'preparando',
    title: () => 'PREPARANDO',
    description: () => 'Tu comida se está preparando.',
  },
  {
    state: 'listo',
    title: () => 'LISTO',
    description: (destination) =>
      destination === 'en_espacio' ? 'El mesero lo llevará a tu mesa.' : 'Recógelo en la barra.',
  },
  {
    state: 'entregado',
    title: () => 'ENTREGADO',
    description: () => 'Pedido completado.',
  },
];

const STATE_INDEX: Record<OrderState, number> = {
  por_cobrar: 0,
  cobrado: 1,
  preparando: 2,
  listo: 3,
  entregado: 4,
};

export function trackingIndex(state: OrderState): number {
  return STATE_INDEX[state];
}

export function destinationDisplayLabel(
  destination: OrderDestination,
  spaceName: string | null,
): string {
  return spaceName ?? (destination === 'para_llevar' ? 'Para llevar' : 'En espacio');
}
