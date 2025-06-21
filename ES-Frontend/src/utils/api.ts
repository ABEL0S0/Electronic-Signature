import axios from "axios";
import { authState, authService } from "../service/Auth";

const SERVER_URL = "http://localhost:8080";
const api = axios.create({
  baseURL: SERVER_URL,
});

// Add request interceptor to include token in headers
api.interceptors.request.use((config) => {
  // Don't send Authorization header for authentication endpoints
  if (authState.token && !config.url?.includes('/api/auth/')) {
    config.headers.Authorization = `Bearer ${authState.token}`;
  }
  return config;
});

// Add response interceptor to handle token expiration
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      // Token is invalid or expired, clear auth and redirect to login
      authService.clearAuth();
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

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
