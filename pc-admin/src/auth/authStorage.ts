import type { AuthUser } from "../types";

const AUTH_KEY = "pc-admin-auth-user";

export function saveAuthUser(user: AuthUser) {
  localStorage.setItem(AUTH_KEY, JSON.stringify(user));
}

export function loadAuthUser(): AuthUser | null {
  const raw = localStorage.getItem(AUTH_KEY);
  return raw ? (JSON.parse(raw) as AuthUser) : null;
}

export function clearAuthUser() {
  localStorage.removeItem(AUTH_KEY);
}
