export type PreparationStation = 'cocina' | 'caja';

export interface Category {
  id: number;
  name: string;
  order: number;
}

export interface ProductOption {
  id: number;
  name: string;
  extraPrice: string;
}

export interface OptionGroup {
  id: number;
  name: string;
  minimumSelections: number;
  maximumSelections: number;
  options: ProductOption[];
}

export interface Product {
  id: number;
  categoryId: number;
  preparationStation: PreparationStation;
  name: string;
  description: string;
  ingredients: string;
  allergens: string;
  estimatedTimeMinutes: number;
  counterPrice: string;
  digitalPrice: string;
  available: boolean;
  imageUrl: string;
  optionGroups: OptionGroup[];
}

export interface Catalog {
  categories: Category[];
  products: Product[];
  cursor: string | null;
}

export interface OperationalStatus {
  acceptingOrders: boolean;
  cashSessionOpen: boolean;
  cashierOnline: boolean;
  kitchenOnline: boolean;
  estimatedTimeMinutes: number;
  consultedAt: string;
}

export type PaymentMethod = 'efectivo' | 'saldo' | 'tarjeta';
export type OrderDestination = 'para_llevar' | 'en_espacio';
export type OrderState =
  | 'por_cobrar'
  | 'cobrado'
  | 'preparando'
  | 'listo'
  | 'entregado';

export interface CartLine {
  product: Product;
  quantity: number;
  selectedOptionIds: number[];
}

export function cartLineKey(line: Pick<CartLine, 'product' | 'selectedOptionIds'>): string {
  const sorted = [...line.selectedOptionIds].sort((a, b) => a - b).join(',');
  return `${line.product.id}:${sorted}`;
}

export interface CreateOrderItem {
  productId: number;
  quantity: number;
  optionIds: number[];
}

export interface CreateOrderRequest {
  paymentMethod: PaymentMethod;
  destination: OrderDestination;
  spaceId: number | null;
  kitchenNotes: string;
  items: CreateOrderItem[];
}

export interface OrderSpace {
  id: number;
  name: string;
  type: string;
}

export interface OrderSummary {
  id: string;
  folio: number;
  operationalDate: string;
  state: OrderState;
  paymentMethod: PaymentMethod;
  destination: OrderDestination;
  space: OrderSpace | null;
  subtotal: string;
  combinedSavings: string;
  cashbackAwarded: string;
  total: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface OrderItemOption {
  optionId: number;
  name: string;
  extraPrice: string;
}

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  preparationStation: PreparationStation;
  quantity: number;
  unitDigitalPrice: string;
  subtotal: string;
  options: OrderItemOption[];
}

export interface OrderDetail {
  summary: OrderSummary;
  kitchenNotes: string;
  items: OrderItem[];
  pickupToken?: string | null;
}

export const PAYMENT_LABELS: Record<PaymentMethod, string> = {
  efectivo: 'Efectivo',
  saldo: 'Saldo',
  tarjeta: 'Tarjeta',
};

export const DESTINATION_LABELS: Record<OrderDestination, string> = {
  para_llevar: 'Para llevar',
  en_espacio: 'En espacio',
};

export function moneyLabel(amount: string): string {
  return `$${amount}`;
}
