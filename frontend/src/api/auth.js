import api from "./axios";

export async function loginApi(email, password) {
  const res = await api.post("/auth/login", { email, password });

  if (res.status < 200 || res.status >= 300) {
    const msg = res.data?.message || "Login failed";
    const err = new Error(msg);
    err.response = res; 
    throw err;
  }

  const token = res.data?.token || res.data?.accessToken || res.data;
  if (!token || typeof token !== "string") {
    throw new Error("Token missing in /auth/login response");
  }

  return token;
}

export async function registerApi(name, email, password) {
  return api.post("/auth/register", { name, email, password });
}