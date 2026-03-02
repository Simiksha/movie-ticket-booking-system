import { createContext, useContext, useMemo, useState } from "react";
import { jwtDecode } from "jwt-decode";

const AuthContext = createContext(null);

function decode(token) {
  try { return jwtDecode(token); } catch { return null; }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem("token") || "");
  const [payload, setPayload] = useState(() => (token ? decode(token) : null));

  const role = payload?.role || null; // "USER" / "ADMIN"
  const email = payload?.sub || null;

  function login(tokenValue) {
    localStorage.setItem("token", tokenValue);
    setToken(tokenValue);
    setPayload(decode(tokenValue));
  }

  function logout() {
    localStorage.removeItem("token");
    setToken("");
    setPayload(null);
  }

  const value = useMemo(
    () => ({ token, role, email, isAuthed: !!token, login, logout }),
    [token, role, email]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}