import createdOrderFixture from '../../../assets/fixtures/created_order.json';
import type {
  OrderDetail,
  OrderDestination,
  OrderState,
  PaymentMethod,
  PreparationStation,
} from '@/domain/models';

interface CreatedOrderWire {
  data: {
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
  };
}

export function parseSampleOrder(options?: {
  state?: OrderState;
  paymentMethod?: PaymentMethod;
  destination?: OrderDestination;
  spaceId?: number | null;
}): OrderDetail {
  const wire = (createdOrderFixture as CreatedOrderWire).data;
  const space =
    options?.destination === 'en_espacio' && options.spaceId
      ? {
          id: options.spaceId,
          name: `Mesa ${options.spaceId - 700}`,
          type: 'mesa',
        }
      : wire.espacio
        ? { id: wire.espacio.id, name: wire.espacio.nombre, type: wire.espacio.tipo }
        : null;

  return {
    summary: {
      id: wire.id,
      folio: wire.folio,
      operationalDate: wire.fecha_operativa,
      state: options?.state ?? (wire.estado as OrderState),
      paymentMethod: options?.paymentMethod ?? (wire.metodo_pago as PaymentMethod),
      destination: options?.destination ?? (wire.destino as OrderDestination),
      space,
      subtotal: wire.subtotal,
      combinedSavings: wire.ahorro_combinado,
      cashbackAwarded: wire.cashback_otorgado,
      total: wire.total,
      version: wire.version,
      createdAt: wire.creado_en,
      updatedAt: wire.actualizado_en,
    },
    kitchenNotes: wire.notas_cocina,
    items: wire.items.map((item) => ({
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
    pickupToken: 'v1.fixture-demo-gallery',
  };
}
