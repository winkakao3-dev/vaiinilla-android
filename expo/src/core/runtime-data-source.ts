import { DATA_SOURCE, type DataSource } from '@/core/config';

/** Solo pruebas forces fixtures even when env is REMOTE. */
let forceMock = false;

export function setForceMock(enabled: boolean): void {
  forceMock = enabled;
}

export function effectiveDataSource(): DataSource {
  if (forceMock) {
    return 'MOCK';
  }
  return DATA_SOURCE;
}

export function isMockDataSource(): boolean {
  return effectiveDataSource() === 'MOCK';
}
