import * as Crypto from 'expo-crypto';
import { v5 as uuidv5 } from 'uuid';

import catalogFixture from '../../assets/fixtures/catalog.json';
import operationalFixture from '../../assets/fixtures/operational_status.json';
import {
  formatMoney,
  parseMoney,
  productUnitPreview,
  validateCatalog,
  validateCreateOrderRequest,
  validateOperationalStatus,
  validateSelections,
} from '@/domain/contract-rules';
import type {
  CreateOrderRequest,
  OrderDetail,
  OrderItem,
  OrderItemOption,
  OrderSummary,
  PreparationStation,
} from '@/domain/models';
import { parseCatalog, parseOperationalStatus } from '@/data/fixtures/parser';
import { apiRequest } from '@/core/http-client';
import { DATA_SOURCE } from '@/core/config';

const ORDER_NAMESPACE = '6ba7b810-9dad-11d1-80b4-00c04fd430c8';

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

function deterministicOrderId(idempotencyKey: string): string {
  return uuidv5(`vaiinilla:${idempotencyKey}`, ORDER_NAMESPACE);
}

function isUuid(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(
    value,
  );
}

function createOrderMock(request: CreateOrderRequest, idempotencyKey: string): OrderDetail {
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

  validateCreateOrderRequest(request);

  const status = parseOperationalStatus(operationalFixture);
  validateOperationalStatus(status);
  if (!status.acceptingOrders || !status.cashSessionOpen) {
    throw new OrderRepositoryError(
      'El establecimiento no está recibiendo pedidos.',
      'ESTABLISHMENT_NOT_RECEIVING',
    );
  }

  const catalog = parseCatalog(catalogFixture);
  validateCatalog(catalog);

  const orderItems: OrderItem[] = request.items.map((requestedItem, index) => {
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
    const itemSubtotal = formatMoney(
      parseMoney(unitPrice).times(requestedItem.quantity),
    );

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

  const total = formatMoney(
    orderItems.reduce((sum, item) => sum.plus(parseMoney(item.subtotal)), parseMoney('0.00')),
  );

  const consultedAt = status.consultedAt;
  const sequence = ordersById.size;
  const summary: OrderSummary = {
    id: deterministicOrderId(idempotencyKey),
    folio: 3472 + sequence,
    operationalDate: consultedAt.substring(0, 10),
    state: 'por_cobrar',
    paymentMethod: request.paymentMethod,
    destination: request.destination,
    space: null,
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

  ordersById.set(order.summary.id, order);
  createRequestsByKey.set(idempotencyKey, { request, order });
  return order;
}

export async function createOrder(
  request: CreateOrderRequest,
  idempotencyKey?: string,
): Promise<OrderDetail> {
  const key = idempotencyKey ?? Crypto.randomUUID();

  if (DATA_SOURCE === 'MOCK') {
    return createOrderMock(request, key);
  }

  try {
    const data = await apiRequest<{
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
    }>('pedidos', {
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
  } catch (error) {
    throw new OrderRepositoryError(
      error instanceof Error ? error.message : 'No pudimos crear el pedido.',
    );
  }
}

export async function generateIdempotencyKey(): Promise<string> {
  return Crypto.randomUUID();
}
