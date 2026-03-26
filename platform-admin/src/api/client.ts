export type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

const V2_API_BASE = "/api/v2";

function getToken(): string | null {
  return localStorage.getItem("platform-admin-token");
}

export function saveToken(token: string) {
  localStorage.setItem("platform-admin-token", token);
}

export function clearToken() {
  localStorage.removeItem("platform-admin-token");
  localStorage.removeItem("platform-admin-user");
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(init?.headers as Record<string, string> ?? {})
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(`${V2_API_BASE}${path}`, {
    ...init,
    headers
  });

  if (response.status === 401 || response.status === 403) {
    clearToken();
    window.location.href = "/platform/login";
    throw new Error("Session expired");
  }

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

export function apiPostV2<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, {
    method: "POST",
    body: JSON.stringify(body)
  });
}
