import catalogFixture from '../../assets/fixtures/catalog.json';
import { apiRequest } from '@/core/http-client';
import { isMockDataSource } from '@/core/runtime-data-source';
import { validateCatalog } from '@/domain/contract-rules';
import type { Catalog } from '@/domain/models';
import { parseCatalog } from '@/data/fixtures/parser';

export class CatalogRepositoryError extends Error {
  constructor(
    message: string,
    readonly code?: string,
  ) {
    super(message);
    this.name = 'CatalogRepositoryError';
  }
}

export async function getCatalog(): Promise<Catalog> {
  if (isMockDataSource()) {
    const catalog = parseCatalog(catalogFixture);
    validateCatalog(catalog);
    return catalog;
  }

  try {
    const data = await apiRequest<{
      categorias: unknown[];
      productos: unknown[];
    }>('catalogo');
    const catalog = parseCatalog({
      data,
      meta: { cursor: null },
      error: null,
    });
    validateCatalog(catalog);
    return catalog;
  } catch (error) {
    throw new CatalogRepositoryError(
      error instanceof Error ? error.message : 'No pudimos cargar el catálogo.',
    );
  }
}
