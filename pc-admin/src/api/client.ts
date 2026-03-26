import type { ApiResponse } from "../types";

const API_BASE = "/api/v2";
const V2_API_BASE = "/api/v2";

async function request<T>(base: string, path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${base}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {})
    },
    ...init
  });

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }

  const payload = (await response.json()) as ApiResponse<T> | T;

  if (typeof payload === "object" && payload !== null && "code" in payload) {
    const wrapped = payload as ApiResponse<T>;
    if (wrapped.code !== 0) {
      throw new Error(wrapped.message || "Request failed");
    }
    return wrapped.data;
  }

  return payload as T;
}

export function apiGet<T>(path: string): Promise<T> {
  return request<T>(API_BASE, path);
}

export function apiPost<T>(path: string, body: unknown): Promise<T> {
  return request<T>(API_BASE, path, {
    method: "POST",
    body: JSON.stringify(body)
  });
}

export function apiPut<T>(path: string, body: unknown): Promise<T> {
  return request<T>(API_BASE, path, {
    method: "PUT",
    body: JSON.stringify(body)
  });
}

export function apiGetV2<T>(path: string): Promise<T> {
  return request<T>(V2_API_BASE, path);
}

export function apiPostV2<T>(path: string, body: unknown): Promise<T> {
  return request<T>(V2_API_BASE, path, {
    method: "POST",
    body: JSON.stringify(body)
  });
}

export function apiPutV2<T>(path: string, body: unknown): Promise<T> {
  return request<T>(V2_API_BASE, path, {
    method: "PUT",
    body: JSON.stringify(body)
  });
}
