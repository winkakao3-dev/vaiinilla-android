import { API_BASE_URL } from '@/core/config';
import { getAccessToken } from '@/core/session-store';

export class ApiClientError extends Error {
  constructor(
    message: string,
    readonly code?: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = 'ApiClientError';
  }
}

interface ApiEnvelope<T> {
  data: T;
  meta: {
    page: number | null;
    total_pages: number | null;
    total_items: number | null;
    cursor: string | null;
  };
  error: { code: string; message: string } | null;
}

export async function apiRequest<T>(
  path: string,
  options: {
    method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
    body?: unknown;
    idempotencyKey?: string;
    token?: string | null;
  } = {},
): Promise<T> {
  const token = options.token ?? (await getAccessToken());
  const headers: Record<string, string> = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (options.idempotencyKey) {
    headers['Idempotency-Key'] = options.idempotencyKey;
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path.replace(/^\//, '')}`, {
      method: options.method ?? 'GET',
      headers,
      body: options.body ? JSON.stringify(options.body) : undefined,
    });
  } catch {
    throw new ApiClientError(
      'Sin conexión al servidor. Revisa tu red o EXPO_PUBLIC_API_BASE_URL.',
      'NETWORK',
      0,
    );
  }

  let payload: ApiEnvelope<T>;
  try {
    payload = (await response.json()) as ApiEnvelope<T>;
  } catch {
    throw new ApiClientError(
      'Respuesta inválida del servidor. Intenta de nuevo en unos segundos.',
      'INVALID_JSON',
      response.status,
    );
  }
  if (!response.ok || payload.error) {
    throw new ApiClientError(
      payload.error?.message ?? `HTTP ${response.status}`,
      payload.error?.code,
      response.status,
    );
  }
  return payload.data;
}
