import { apiPost } from "../client";
import { USE_MOCK_API } from "../config";
import * as mockApi from "../mockApi";
import type { AuthUser } from "../../types";

interface LoginRequest {
  username: string;
  password: string;
  clientType: "PC";
}

interface LoginResponse {
  token: string;
  user: AuthUser;
}

export async function login(username: string, password: string): Promise<{ token: string; user: AuthUser }> {
  if (USE_MOCK_API) {
    return { token: "mock-token", user: await mockApi.login(username, password) };
  }

  const response = await apiPost<LoginResponse>("/auth/login", {
    username,
    password,
    clientType: "PC"
  } satisfies LoginRequest);

  return { token: response.token, user: response.user };
}
