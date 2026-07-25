import Decimal from 'decimal.js';

import type { Catalog, CreateOrderRequest, Product } from '@/domain/models';

const moneyPattern = /^(0|[1-9]\d*)\.\d{2}$/;
const utcTimestampPattern =
  /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{3})?Z$/;

export function isValidMoney(value: string): boolean {
  return moneyPattern.test(value);
}

export function validateCatalog(catalog: Catalog): void {
  if (catalog.categories.length === 0) {
    throw new Error('El catálogo debe incluir categorías.');
  }
  const categoryIds = new Set(catalog.categories.map((c) => c.id));
  if (categoryIds.size !== catalog.categories.length) {
    throw new Error('Los IDs de categoría deben ser únicos.');
  }
  const productIds = new Set(catalog.products.map((p) => p.id));
  if (productIds.size !== catalog.products.length) {
    throw new Error('Los IDs de producto deben ser únicos.');
  }

  catalog.products.forEach((product) => {
    if (!categoryIds.has(product.categoryId)) {
      throw new Error(`El producto ${product.id} referencia una categoría inexistente.`);
    }
    if (product.estimatedTimeMinutes < 0) {
      throw new Error(`tiempo_estimado_min inválido para ${product.id}`);
    }
    if (!isValidMoney(product.counterPrice)) {
      throw new Error(`precio_mostrador inválido para ${product.id}`);
    }
    if (!isValidMoney(product.digitalPrice)) {
      throw new Error(`precio_digital inválido para ${product.id}`);
    }
    if (!product.imageUrl.trim()) {
      throw new Error(`imagen_url vacía para ${product.id}`);
    }
    const groupIds = new Set(product.optionGroups.map((g) => g.id));
    if (groupIds.size !== product.optionGroups.length) {
      throw new Error(`Los IDs de grupos de opción deben ser únicos dentro del producto ${product.id}.`);
    }
    product.optionGroups.forEach((group) => {
      if (group.minimumSelections < 0 || group.maximumSelections < group.minimumSelections) {
        throw new Error(`Grupo ${group.id} tiene límites inválidos.`);
      }
      if (group.maximumSelections > group.options.length) {
        throw new Error(`Grupo ${group.id} excede opciones disponibles.`);
      }
      const optionIds = new Set(group.options.map((o) => o.id));
      if (optionIds.size !== group.options.length) {
        throw new Error(`Los IDs de opción deben ser únicos dentro del grupo ${group.id}.`);
      }
      group.options.forEach((option) => {
        if (!isValidMoney(option.extraPrice)) {
          throw new Error(`precio_extra inválido para ${option.id}`);
        }
      });
    });
  });
}

export function validateOperationalStatus(status: {
  estimatedTimeMinutes: number;
  consultedAt: string;
}): void {
  if (status.estimatedTimeMinutes < 0) {
    throw new Error('tiempo_estimado_min no puede ser negativo.');
  }
  if (!utcTimestampPattern.test(status.consultedAt)) {
    throw new Error('consultado_en debe usar ISO 8601 UTC.');
  }
}

export function validateSelections(product: Product, selectedOptionIds: number[]): void {
  const allOptionIds = new Set(
    product.optionGroups.flatMap((group) => group.options.map((o) => o.id)),
  );
  if (!selectedOptionIds.every((id) => allOptionIds.has(id))) {
    throw new Error(`Una opción seleccionada no pertenece al producto ${product.id}.`);
  }

  product.optionGroups.forEach((group) => {
    const count = group.options.filter((o) => selectedOptionIds.includes(o.id)).length;
    if (count < group.minimumSelections || count > group.maximumSelections) {
      throw new Error(
        `El grupo ${group.id} requiere entre ${group.minimumSelections} y ${group.maximumSelections} selecciones.`,
      );
    }
  });
}

export function validateCreateOrderRequest(request: CreateOrderRequest): void {
  if (request.paymentMethod !== 'efectivo') {
    throw new Error('VAI-10 solo acepta efectivo.');
  }
  if (request.destination !== 'para_llevar') {
    throw new Error('VAI-10 solo acepta destino para_llevar.');
  }
  if (request.spaceId !== null) {
    throw new Error('para_llevar exige espacio_id null.');
  }
  validateOrderItems(request);
}

function validateOrderItems(request: CreateOrderRequest): void {
  if (request.items.length < 1 || request.items.length > 50) {
    throw new Error('El pedido debe contener entre 1 y 50 líneas.');
  }
  request.items.forEach((item) => {
    if (item.quantity < 1 || item.quantity > 20) {
      throw new Error('cantidad debe estar entre 1 y 20.');
    }
    const unique = new Set(item.optionIds);
    if (unique.size !== item.optionIds.length) {
      throw new Error('opcion_ids no puede contener duplicados.');
    }
  });
}

export function parseMoney(value: string): Decimal {
  if (!isValidMoney(value)) {
    throw new Error(`Importe contractual inválido: ${value}`);
  }
  return new Decimal(value);
}

export function formatMoney(value: Decimal): string {
  return value.toFixed(2, Decimal.ROUND_HALF_UP);
}

export function productUnitPreview(product: Product, selectedOptionIds: number[]): string {
  const optionTotal = product.optionGroups
    .flatMap((g) => g.options)
    .filter((o) => selectedOptionIds.includes(o.id))
    .reduce((sum, o) => sum.plus(parseMoney(o.extraPrice)), new Decimal(0));
  return formatMoney(parseMoney(product.digitalPrice).plus(optionTotal));
}

export function cartLinePreview(line: { product: Product; quantity: number; selectedOptionIds: number[] }): string {
  const unit = parseMoney(productUnitPreview(line.product, line.selectedOptionIds));
  return formatMoney(unit.times(line.quantity));
}

export function cartPreview(
  lines: { product: Product; quantity: number; selectedOptionIds: number[] }[],
): string {
  const total = lines.reduce(
    (sum, line) => sum.plus(parseMoney(cartLinePreview(line))),
    new Decimal(0),
  );
  return formatMoney(total);
}
