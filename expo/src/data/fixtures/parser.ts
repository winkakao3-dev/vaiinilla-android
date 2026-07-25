import type {
  Catalog,
  OperationalStatus,
  OptionGroup,
  PreparationStation,
  Product,
} from '@/domain/models';

interface CatalogEnvelopeDto {
  data: {
    categorias: Array<{ id: number; nombre: string; orden: number }>;
    productos: Array<{
      id: number;
      categoria_id: number;
      estacion_preparacion: PreparationStation;
      nombre: string;
      descripcion?: string | null;
      ingredientes?: string | null;
      alergenos?: string | null;
      tiempo_estimado_min: number;
      precio_mostrador: string;
      precio_digital: string;
      disponible: boolean;
      imagen_url?: string | null;
      grupos_opcion: Array<{
        id: number;
        nombre: string;
        min_selecciones: number;
        max_selecciones: number;
        opciones: Array<{ id: number; nombre: string; precio_extra: string }>;
      }>;
    }>;
  };
  meta: { cursor: string | null };
  error: unknown;
}

interface OperationalStatusEnvelopeDto {
  data: {
    recibiendo_pedidos: boolean;
    sesion_caja_abierta: boolean;
    caja_en_linea: boolean;
    cocina_en_linea: boolean;
    tiempo_estimado_min: number;
    consultado_en: string;
  };
  error: unknown;
}

export function parseCatalog(raw: unknown): Catalog {
  const envelope = raw as CatalogEnvelopeDto;
  if (envelope.error) {
    throw new Error('El fixture de catálogo contiene error.');
  }

  return {
    categories: envelope.data.categorias.map((dto) => ({
      id: dto.id,
      name: dto.nombre,
      order: dto.orden,
    })),
    products: envelope.data.productos.map((dto): Product => ({
      id: dto.id,
      categoryId: dto.categoria_id,
      preparationStation: dto.estacion_preparacion,
      name: dto.nombre,
      description: dto.descripcion ?? '',
      ingredients: dto.ingredientes ?? '',
      allergens: dto.alergenos ?? '',
      estimatedTimeMinutes: dto.tiempo_estimado_min,
      counterPrice: dto.precio_mostrador,
      digitalPrice: dto.precio_digital,
      available: dto.disponible,
      imageUrl: dto.imagen_url ?? '',
      optionGroups: dto.grupos_opcion.map(
        (group): OptionGroup => ({
          id: group.id,
          name: group.nombre,
          minimumSelections: group.min_selecciones,
          maximumSelections: group.max_selecciones,
          options: group.opciones.map((option) => ({
            id: option.id,
            name: option.nombre,
            extraPrice: option.precio_extra,
          })),
        }),
      ),
    })),
    cursor: envelope.meta.cursor,
  };
}

export function parseOperationalStatus(raw: unknown): OperationalStatus {
  const envelope = raw as OperationalStatusEnvelopeDto;
  if (envelope.error) {
    throw new Error('El fixture de estado operativo contiene error.');
  }

  return {
    acceptingOrders: envelope.data.recibiendo_pedidos,
    cashSessionOpen: envelope.data.sesion_caja_abierta,
    cashierOnline: envelope.data.caja_en_linea,
    kitchenOnline: envelope.data.cocina_en_linea,
    estimatedTimeMinutes: envelope.data.tiempo_estimado_min,
    consultedAt: envelope.data.consultado_en,
  };
}
