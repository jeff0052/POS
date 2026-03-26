import { createContext, useContext, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { clearAuthUser, loadAuthUser, saveAuthUser, saveToken } from "./authStorage";
import { login as loginRequest } from "../api/services/authService";
import type { AuthUser } from "../types";

interface AuthContextValue {
  user: AuthUser | null;
  signIn: (username: string, password: string) => Promise<void>;
  signOut: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => loadAuthUser());

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      async signIn(username: string, password: string) {
        const { token, user: nextUser } = await loginRequest(username, password);
        saveToken(token);
        saveAuthUser(nextUser);
        setUser(nextUser);
      },
      signOut() {
        clearAuthUser();
        setUser(null);
      }
    }),
    [user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
