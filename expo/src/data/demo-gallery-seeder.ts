import { DEFAULT_SPACE } from '@/domain/checkout-fixtures';
import type {
  CartLine,
  OrderDetail,
  OrderDestination,
  OrderState,
  PaymentMethod,
  Product,
} from '@/domain/models';
import { getSampleOrder, seedMockOrder } from '@/data/order-repository';

export interface GallerySeedApi {
  setSearchQuery: (value: string) => void;
  setSelectedCategoryId: (value: number | null) => void;
  openProduct: (productId: number) => void;
  closeProduct: () => void;
  clearCart: () => void;
  setCartLines: (lines: CartLine[]) => void;
  setCheckoutDestination: (destination: OrderDestination) => void;
  setCheckoutPayment: (payment: PaymentMethod) => void;
  setSelectedSpaceId: (spaceId: number) => void;
  setCreatedOrder: (order: OrderDetail | null) => void;
  setClientOrders: (orders: OrderDetail[], selectedId: string | null) => void;
}

function firstCartLine(products: Product[]): CartLine | null {
  const firstProduct = products[0];
  if (!firstProduct) {
    return null;
  }
  const defaultOptionId = firstProduct.optionGroups[0]?.options[0]?.id;
  return {
    product: firstProduct,
    quantity: 1,
    selectedOptionIds: defaultOptionId ? [defaultOptionId] : [],
  };
}

export function seedCatalogCleared(api: GallerySeedApi): void {
  api.setSearchQuery('');
  api.setSelectedCategoryId(null);
  api.closeProduct();
  api.clearCart();
}

export function seedCatalogEmptySearch(api: GallerySeedApi): void {
  api.setSearchQuery('zzzsinresultados');
  api.setSelectedCategoryId(null);
  api.closeProduct();
}

export function seedCatalogProductSheet(api: GallerySeedApi, products: Product[]): void {
  api.setSearchQuery('');
  api.setSelectedCategoryId(null);
  const first = products[0];
  if (first) {
    api.openProduct(first.id);
  }
}

export function seedCartEmpty(api: GallerySeedApi): void {
  api.setSearchQuery('');
  api.setSelectedCategoryId(null);
  api.closeProduct();
  api.clearCart();
}

export function seedCartWithFirstProduct(api: GallerySeedApi, products: Product[]): void {
  api.setSearchQuery('');
  api.setSelectedCategoryId(null);
  api.closeProduct();
  const line = firstCartLine(products);
  api.setCartLines(line ? [line] : []);
}

export function seedCheckout(
  api: GallerySeedApi,
  products: Product[],
  destination: OrderDestination,
  payment: PaymentMethod,
  spaceId: number = DEFAULT_SPACE.id,
): void {
  seedCartWithFirstProduct(api, products);
  api.setCheckoutDestination(destination);
  api.setCheckoutPayment(payment);
  if (destination === 'en_espacio') {
    api.setSelectedSpaceId(spaceId);
  }
}

export function seedConfirmation(api: GallerySeedApi, payment: PaymentMethod): void {
  const order = getSampleOrder({
    state: payment === 'efectivo' ? 'por_cobrar' : 'cobrado',
    paymentMethod: payment,
  });
  api.setCreatedOrder(order);
  seedMockOrder(order);
}

export function seedTrackingEmpty(api: GallerySeedApi): void {
  api.setClientOrders([], null);
}

export function seedTrackingOrder(
  api: GallerySeedApi,
  state: OrderState,
  payment: PaymentMethod = 'efectivo',
): void {
  const order = getSampleOrder({ state, paymentMethod: payment });
  seedMockOrder(order);
  api.setClientOrders([order], order.summary.id);
}

export function seedCatalogActiveOrder(api: GallerySeedApi, products: Product[]): void {
  const order = getSampleOrder({ state: 'preparando', paymentMethod: 'efectivo' });
  seedMockOrder(order);
  api.setSearchQuery('');
  api.setSelectedCategoryId(null);
  api.closeProduct();
  api.setClientOrders([order], order.summary.id);
  seedCartWithFirstProduct(api, products);
}
