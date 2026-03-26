export type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

const V2_API_BASE = "/api/v2";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${V2_API_BASE}${path}`, {
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

export function apiGetV2<T>(path: string): Promise<T> {
  return request<T>(path);
}
