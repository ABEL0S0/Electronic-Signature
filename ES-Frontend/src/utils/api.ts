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

// Certificados
export function uploadCertificate(file: File, password: string) {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("password", password);

  return api.post("/api/certificates", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
}

export function downloadCertificate(id: string, password: string) {
  return api.get(`/api/certificates/${id}`, {
    params: { password },
    responseType: 'arraybuffer',
  });
}

export function getCertificatesByUser(user: string) {
  return api.get(`/api/certificates/user/${user}`);
}

export function deleteCertificate(id: string) {
  return api.delete(`/api/certificates/${id}`);
}

export function deleteCertificatesByUser(user: string) {
  return api.delete(`/api/certificates/user/${user}`);
}

// Documentos
export function uploadDocument(file: File) {
  const formData = new FormData();
  formData.append("file", file);

  return api.post("/api/documents/upload", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
}

export function downloadDocument(id: string) {
  return api.get(`/api/documents/download/${id}`, {
    responseType: 'blob',
  });
}

export function getDocumentsByUser() {
  return api.get(`/api/documents/list`);
}

export function deleteDocument(id: string) {
  return api.delete(`/api/documents/${id}`);
}

export function deleteDocumentsByUser(user: string) {
  return api.delete(`/api/documents/user/${user}`);
}

export { api };
