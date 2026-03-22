import { apiPost } from "../client";
import { USE_MOCK_API } from "../config";
import * as mockApi from "../mockApi";
import type { AuthUser } from "../../types";

interface LoginRequest {
  username: string;
  password: string;
  clientType: "PC";
}

export async function login(username: string, password: string): Promise<AuthUser> {
  if (USE_MOCK_API) {
    return mockApi.login(username, password);
  }

  return apiPost<AuthUser>("/auth/login", {
    username,
    password,
    clientType: "PC"
  } satisfies LoginRequest);
}
