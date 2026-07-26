import * as Crypto from 'expo-crypto';
import { v5 as uuidv5 } from 'uuid';

import catalogFixture from '../../assets/fixtures/catalog.json';
import operationalFixture from '../../assets/fixtures/operational_status.json';
import {
  formatMoney,
  isValidMoney,
  parseMoney,
  productUnitPreview,
  validateCatalog,
  validateCreateOrderRequest,
  validateOperationalStatus,
  validateSelections,
  validateStudentCheckoutRequest,
} from '@/domain/contract-rules';
import { isInstantDemoPayment, spaceForId } from '@/domain/checkout-fixtures';
import type {
  CreateOrderRequest,
  OrderDetail,
  OrderItem,
  OrderItemOption,
  OrderSpace,
  OrderState,
  OrderSummary,
  PreparationStation,
} from '@/domain/models';
import { parseCatalog, parseOperationalStatus } from '@/data/fixtures/parser';
import { parseSampleOrder } from '@/data/fixtures/order-parser';
import { apiRequest } from '@/core/http-client';
import { isMockDataSource } from '@/core/runtime-data-source';

const ORDER_NAMESPACE = '6ba7b810-9dad-11d1-80b4-00c04fd430c8';

export type OperationalRole = 'cliente' | 'cajero' | 'cocina' | 'mesero';

export class OrderRepositoryError extends Error {
  constructor(
    message: string,
    readonly code?: string,
  ) {
    super(message);
    this.name = 'OrderRepositoryError';
  }
}

const ordersById = new Map<string, OrderDetail>();
const createRequestsByKey = new Map<string, { request: CreateOrderRequest; order: OrderDetail }>();
const mutationResultsByKey = new Map<string, OrderDetail>();

function deterministicOrderId(idempotencyKey: string): string {
  return uuidv5(`vaiinilla:${idempotencyKey}`, ORDER_NAMESPACE);
}

function isUuid(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(
    value,
  );
}

function requiresKitchen(order: OrderDetail): boolean {
  return order.items.some((item) => item.preparationStation === 'cocina');
}

function bumpTimestamp(previous: string): string {
  const base = previous.split('.')[0]?.replace('Z', '') ?? previous;
  const millis = Number(base.split('.')[1] ?? '0');
  const nextMillis = Math.min(999, millis + 1);
  const withoutMillis = base.split('.')[0] ?? base;
  return `${withoutMillis}.${String(nextMillis).padStart(3, '0')}Z`;
}

function withState(order: OrderDetail, state: OrderState, version: number, updatedAt: string): OrderDetail {
  return {
    ...order,
    summary: {
      ...order.summary,
      state,
      version,
      updatedAt,
    },
  };
}

function persist(order: OrderDetail): void {
  ordersById.set(order.summary.id, order);
}

function resolveSpace(request: CreateOrderRequest): OrderSpace | null {
  if (request.destination !== 'en_espacio' || !request.spaceId) {
    return null;
  }
  const demoSpace = spaceForId(request.spaceId);
  if (!demoSpace) {
    return null;
  }
  return { id: demoSpace.id, name: demoSpace.name, type: 'mesa' };
}

function buildOrderItems(request: CreateOrderRequest): OrderItem[] {
  const catalog = parseCatalog(catalogFixture);
  validateCatalog(catalog);

  return request.items.map((requestedItem, index) => {
    const product = catalog.products.find((p) => p.id === requestedItem.productId);
    if (!product || !product.available) {
      throw new OrderRepositoryError('El producto no está disponible.', 'PRODUCT_UNAVAILABLE');
    }

    try {
      validateSelections(product, requestedItem.optionIds);
    } catch (error) {
      throw new OrderRepositoryError(
        error instanceof Error ? error.message : 'Las opciones no son válidas.',
        'INVALID_PRODUCT_OPTION',
      );
    }

    const selectedOptions = product.optionGroups
      .flatMap((group) => group.options)
      .filter((option) => requestedItem.optionIds.includes(option.id));
    const unitPrice = productUnitPreview(product, requestedItem.optionIds);
    const itemSubtotal = formatMoney(parseMoney(unitPrice).times(requestedItem.quantity));

    return {
      id: 501 + index,
      productId: product.id,
      productName: product.name,
      preparationStation: product.preparationStation as PreparationStation,
      quantity: requestedItem.quantity,
      unitDigitalPrice: unitPrice,
      subtotal: itemSubtotal,
      options: selectedOptions.map(
        (option): OrderItemOption => ({
          optionId: option.id,
          name: option.name,
          extraPrice: option.extraPrice,
        }),
      ),
    };
  });
}

function createOrderInternal(
  request: CreateOrderRequest,
  idempotencyKey: string,
  validate: (req: CreateOrderRequest) => void,
  initialState: OrderState,
  space: OrderSpace | null,
): OrderDetail {
  if (!isUuid(idempotencyKey)) {
    throw new OrderRepositoryError('Idempotency-Key debe ser UUID.', 'VALIDATION_ERROR');
  }

  const stored = createRequestsByKey.get(idempotencyKey);
  if (stored) {
    if (JSON.stringify(stored.request) !== JSON.stringify(request)) {
      throw new OrderRepositoryError(
        'La llave de idempotencia ya fue usada con otro request.',
        'IDEMPOTENCY_KEY_REUSED',
      );
    }
    return stored.order;
  }

  validate(request);

  const status = parseOperationalStatus(operationalFixture);
  validateOperationalStatus(status);
  if (!status.acceptingOrders || !status.cashSessionOpen) {
    throw new OrderRepositoryError(
      'El establecimiento no está recibiendo pedidos.',
      'ESTABLISHMENT_NOT_RECEIVING',
    );
  }

  const orderItems = buildOrderItems(request);
  const total = formatMoney(
    orderItems.reduce((sum, item) => sum.plus(parseMoney(item.subtotal)), parseMoney('0.00')),
  );

  const consultedAt = status.consultedAt;
  const sequence = ordersById.size;
  const summary: OrderSummary = {
    id: deterministicOrderId(idempotencyKey),
    folio: 3472 + sequence,
    operationalDate: consultedAt.substring(0, 10),
    state: initialState,
    paymentMethod: request.paymentMethod,
    destination: request.destination,
    space,
    subtotal: total,
    combinedSavings: '0.00',
    cashbackAwarded: '0.00',
    total,
    version: 1,
    createdAt: consultedAt,
    updatedAt: consultedAt,
  };

  const order: OrderDetail = {
    summary,
    kitchenNotes: request.kitchenNotes,
    items: orderItems,
    pickupToken: `v1.fixture-${idempotencyKey.replace(/-/g, '').slice(0, 32)}`,
  };

  persist(order);
  createRequestsByKey.set(idempotencyKey, { request, order });
  return order;
}

function createOrderMock(request: CreateOrderRequest, idempotencyKey: string): OrderDetail {
  return createOrderInternal(request, idempotencyKey, validateCreateOrderRequest, 'por_cobrar', null);
}

function createStudentCheckoutMock(request: CreateOrderRequest, idempotencyKey: string): OrderDetail {
  const initialState: OrderState = isInstantDemoPayment(request.paymentMethod) ? 'cobrado' : 'por_cobrar';
  return createOrderInternal(
    request,
    idempotencyKey,
    validateStudentCheckoutRequest,
    initialState,
    resolveSpace(request),
  );
}

function matchesRole(role: OperationalRole, order: OrderDetail): boolean {
  const state = order.summary.state;
  switch (role) {
    case 'cliente':
      return true;
    case 'cajero':
      return (
        state === 'por_cobrar' ||
        (state === 'listo' && order.summary.destination === 'para_llevar')
      );
    case 'cocina':
      return (
        requiresKitchen(order) && ['cobrado', 'preparando', 'listo'].includes(state)
      );
    case 'mesero':
      return state === 'listo' && order.summary.destination === 'en_espacio';
    default:
      return false;
  }
}

function mapOrderFromApi(data: {
  id: string;
  folio: number;
  fecha_operativa: string;
  estado: string;
  metodo_pago: string;
  destino: string;
  espacio: null | { id: number; nombre: string; tipo: string };
  subtotal: string;
  ahorro_combinado: string;
  cashback_otorgado: string;
  total: string;
  version: number;
  creado_en: string;
  actualizado_en: string;
  notas_cocina: string;
  items: Array<{
    id: number;
    producto_id: number;
    nombre_producto: string;
    estacion_preparacion: PreparationStation;
    cantidad: number;
    precio_digital_unitario: string;
    subtotal: string;
    opciones: Array<{ opcion_id: number; nombre: string; precio_extra: string }>;
  }>;
}): OrderDetail {
  return {
    summary: {
      id: data.id,
      folio: data.folio,
      operationalDate: data.fecha_operativa,
      state: data.estado as OrderSummary['state'],
      paymentMethod: data.metodo_pago as OrderSummary['paymentMethod'],
      destination: data.destino as OrderSummary['destination'],
      space: data.espacio
        ? { id: data.espacio.id, name: data.espacio.nombre, type: data.espacio.tipo }
        : null,
      subtotal: data.subtotal,
      combinedSavings: data.ahorro_combinado,
      cashbackAwarded: data.cashback_otorgado,
      total: data.total,
      version: data.version,
      createdAt: data.creado_en,
      updatedAt: data.actualizado_en,
    },
    kitchenNotes: data.notas_cocina,
    items: data.items.map((item) => ({
      id: item.id,
      productId: item.producto_id,
      productName: item.nombre_producto,
      preparationStation: item.estacion_preparacion,
      quantity: item.cantidad,
      unitDigitalPrice: item.precio_digital_unitario,
      subtotal: item.subtotal,
      options: item.opciones.map((option) => ({
        optionId: option.opcion_id,
        name: option.nombre,
        extraPrice: option.precio_extra,
      })),
    })),
  };
}

export function getSampleOrder(options?: Parameters<typeof parseSampleOrder>[0]): OrderDetail {
  return parseSampleOrder(options);
}

export function seedMockOrder(order: OrderDetail): void {
  persist(order);
}

export function clearMockOrders(): void {
  ordersById.clear();
  createRequestsByKey.clear();
  mutationResultsByKey.clear();
}

export async function createOrder(
  request: CreateOrderRequest,
  idempotencyKey?: string,
): Promise<OrderDetail> {
  const key = idempotencyKey ?? Crypto.randomUUID();

  if (isMockDataSource()) {
    return createOrderMock(request, key);
  }

  try {
    const data = await apiRequest<Parameters<typeof mapOrderFromApi>[0]>('pedidos', {
      method: 'POST',
      idempotencyKey: key,
      body: {
        metodo_pago: request.paymentMethod,
        destino: request.destination,
        espacio_id: request.spaceId,
        notas_cocina: request.kitchenNotes,
        items: request.items.map((item) => ({
          producto_id: item.productId,
          cantidad: item.quantity,
          opcion_ids: item.optionIds,
        })),
      },
    });
    return mapOrderFromApi(data);
  } catch (error) {
    throw new OrderRepositoryError(
      error instanceof Error ? error.message : 'No pudimos crear el pedido.',
    );
  }
}

export async function createStudentCheckout(
  request: CreateOrderRequest,
  idempotencyKey?: string,
): Promise<OrderDetail> {
  const key = idempotencyKey ?? Crypto.randomUUID();

  if (isMockDataSource()) {
    return createStudentCheckoutMock(request, key);
  }

  if (request.paymentMethod !== 'efectivo' || request.destination !== 'para_llevar') {
    throw new OrderRepositoryError(
      'Checkout alumno con saldo, tarjeta o mesa solo está disponible en modo MOCK (Solo pruebas).',
      'REMOTE_CHECKOUT_UNSUPPORTED',
    );
  }

  return createOrder(request, key);
}

export async function getOrder(orderId: string): Promise<OrderDetail> {
  if (isMockDataSource()) {
    const order = ordersById.get(orderId);
    if (!order) {
      throw new OrderRepositoryError('El pedido no existe.', 'ORDER_NOT_FOUND');
    }
    return order;
  }

  try {
    const data = await apiRequest<Parameters<typeof mapOrderFromApi>[0]>(`pedidos/${orderId}`);
    return mapOrderFromApi(data);
  } catch (error) {
    throw new OrderRepositoryError(
      error instanceof Error ? error.message : 'No pudimos cargar el pedido.',
    );
  }
}

export async function listOrders(role: OperationalRole = 'cliente'): Promise<OrderDetail[]> {
  if (isMockDataSource()) {
    return [...ordersById.values()]
      .filter((order) => matchesRole(role, order))
      .sort((a, b) => b.summary.updatedAt.localeCompare(a.summary.updatedAt));
  }

  try {
    const data = await apiRequest<Parameters<typeof mapOrderFromApi>[0][]>('pedidos');
    return data.map(mapOrderFromApi).filter((order) => matchesRole(role, order));
  } catch (error) {
    throw new OrderRepositoryError(
      error instanceof Error ? error.message : 'No pudimos listar pedidos.',
    );
  }
}

export async function collectCash(
  orderId: string,
  amountReceived: string,
  expectedVersion: number,
  idempotencyKey: string,
): Promise<OrderDetail> {
  if (isMockDataSource()) {
    const cached = mutationResultsByKey.get(idempotencyKey);
    if (cached) {
      return cached;
    }
    if (!isUuid(idempotencyKey)) {
      throw new OrderRepositoryError('Idempotency-Key debe ser UUID.', 'VALIDATION_ERROR');
    }
    if (!isValidMoney(amountReceived)) {
      throw new OrderRepositoryError('El monto recibido no es válido.', 'VALIDATION_ERROR');
    }

    const order = ordersById.get(orderId);
    if (!order) {
      throw new OrderRepositoryError('El pedido no existe.', 'ORDER_NOT_FOUND');
    }
    if (order.summary.version !== expectedVersion) {
      throw new OrderRepositoryError('El pedido cambió en otro dispositivo.', 'VERSION_CONFLICT');
    }
    if (order.summary.state !== 'por_cobrar') {
      throw new OrderRepositoryError('El pedido no está por cobrar.', 'INVALID_ORDER_STATE');
    }
    if (parseMoney(amountReceived).lt(parseMoney(order.summary.total))) {
      throw new OrderRepositoryError('El monto recibido es menor al total.', 'INSUFFICIENT_CASH');
    }

    const timestamp = bumpTimestamp(order.summary.updatedAt);
    const paid = withState(order, 'cobrado', order.summary.version + 1, timestamp);
    const next = requiresKitchen(paid)
      ? paid
      : withState(paid, 'listo', paid.summary.version + 1, timestamp);
    persist(next);
    mutationResultsByKey.set(idempotencyKey, next);
    return next;
  }

  try {
    const data = await apiRequest<Parameters<typeof mapOrderFromApi>[0]>(
      `pedidos/${orderId}/cobrar-efectivo`,
      {
        method: 'POST',
        idempotencyKey,
        body: { monto_recibido: amountReceived, version_esperada: expectedVersion },
      },
    );
    return mapOrderFromApi(data);
  } catch (error) {
    throw new OrderRepositoryError(
      error instanceof Error ? error.message : 'No pudimos cobrar el pedido.',
    );
  }
}

export async function transitionOrder(
  orderId: string,
  targetState: OrderState,
  expectedVersion: number,
  idempotencyKey: string,
): Promise<OrderDetail> {
  if (isMockDataSource()) {
    const cached = mutationResultsByKey.get(idempotencyKey);
    if (cached) {
      return cached;
    }
    if (!isUuid(idempotencyKey)) {
      throw new OrderRepositoryError('Idempotency-Key debe ser UUID.', 'VALIDATION_ERROR');
    }

    const order = ordersById.get(orderId);
    if (!order) {
      throw new OrderRepositoryError('El pedido no existe.', 'ORDER_NOT_FOUND');
    }
    if (order.summary.version !== expectedVersion) {
      throw new OrderRepositoryError('El pedido cambió en otro dispositivo.', 'VERSION_CONFLICT');
    }

    const current = order.summary.state;
    const allowed =
      (targetState === 'preparando' && current === 'cobrado' && requiresKitchen(order)) ||
      (targetState === 'listo' && current === 'preparando') ||
      (targetState === 'entregado' && current === 'listo');

    if (!allowed) {
      throw new OrderRepositoryError('La transición solicitada no es válida.', 'INVALID_TRANSITION');
    }

    const timestamp = bumpTimestamp(order.summary.updatedAt);
    const next = withState(order, targetState, order.summary.version + 1, timestamp);
    persist(next);
    mutationResultsByKey.set(idempotencyKey, next);
    return next;
  }

  try {
    const data = await apiRequest<Parameters<typeof mapOrderFromApi>[0]>(
      `pedidos/${orderId}/transicion`,
      {
        method: 'POST',
        idempotencyKey,
        body: { estado_destino: targetState, version_esperada: expectedVersion },
      },
    );
    return mapOrderFromApi(data);
  } catch (error) {
    throw new OrderRepositoryError(
      error instanceof Error ? error.message : 'No pudimos actualizar el pedido.',
    );
  }
}

export async function generateIdempotencyKey(): Promise<string> {
  return Crypto.randomUUID();
}
