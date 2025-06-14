import axios from "axios";
import { authState } from "../service/Auth";

const SERVER_URL = "http://localhost:3000";
const api = axios.create({
  baseURL: SERVER_URL,
});

// Add request interceptor to include token in headers
api.interceptors.request.use((config) => {
  if (authState.token) {
    config.headers.Authorization = `Bearer ${authState.token}`;
  }
  return config;
});

export function login(email: string, password: string) {
  return api.post("/api/auth/login", { email, password });
}

export function register(
  firstName: string,
  lastName: string,
  email: string,
  password: string
) {
  return api.post("/api/auth/register", {
    firstName,
    lastName,
    email,
    password,
  });
}

export { api };
