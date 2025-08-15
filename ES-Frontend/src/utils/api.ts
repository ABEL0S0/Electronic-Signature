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
  password: string,
  role: string = 'USER'
) {
  return api.post("/api/auth/register", {
    firstName,
    lastName,
    email,
    password,
    role,
  });
}
// Email verification
export function verifyAccount(email: string, code: string) {
  return api.post("/api/auth/verify", {
    email: email,
    verificationCode: code,
  });
}
// Password reset APIs
export function requestPasswordReset(email: string) {
  return api.post("/api/auth/password-reset-request", {
    email,
  });
}

export function resetPassword(
  email: string,
  passwordResetCode: string,
  newPassword: string
) {
  return api.post("/api/auth/password-reset", {
    email,
    passwordResetCode,
    newPassword,
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

// Solicitudes de certificados
export function createCertificateRequest(request: {
  nombre: string;
  correo: string;
  organizacion: string;
  password: string;
}) {
  return api.post("/api/certificate-requests/request", request);
}

export function getPendingCertificateRequests() {
  return api.get("/api/certificate-requests/pending");
}

export function getAllCertificateRequests() {
  return api.get("/api/certificate-requests/all");
}

export function getUserCertificateRequests() {
  return api.get("/api/certificate-requests/user");
}

export function approveCertificateRequest(id: number) {
  return api.put(`/api/certificate-requests/${id}/approve`);
}

export function rejectCertificateRequest(id: number, reason: string) {
  return api.put(`/api/certificate-requests/${id}/reject`, { reason });
}

// --- USUARIOS ---
export function getAllUsers() {
  return api.get('/api/users');
}

export function getUserByEmail(email: string) {
  return api.get('/api/users/search', { params: { email } });
}

// --- SOLICITUDES DE FIRMA GRUPAL ---
// NOTA: Las solicitudes de firma ahora se manejan en tiempo real a través de WebSocket
// Los endpoints HTTP se mantienen para compatibilidad y operaciones iniciales
export function createSignatureRequest(request: {
  documentPath: string;
  users: Array<{ userId: number; page: number; posX: number; posY: number }>;
}) {
  return api.post('/api/signature-requests', request);
}

export function getAllSignatureRequests() {
  return api.get('/api/signature-requests');
}

export function getSignatureRequestById(id: number) {
  return api.get(`/api/signature-requests/${id}`);
}

export function responderSolicitudFirma(userResponse: {
  id?: number;
  signatureRequest: { id: number }; // Objeto con ID de la solicitud
  signatureRequestId?: number; // ID directo de la solicitud (opcional, para compatibilidad)
  userId: number;
  page: number;
  posX: number;
  posY: number;
  status: string;
  certificateId?: string;
  certificatePassword?: string;
}) {
  return api.post('/api/signature-requests/user-response', userResponse);
}

export function getUsersBySignatureRequest(signatureRequestId: number) {
  return api.get(`/api/signature-requests/${signatureRequestId}/users`);
}

export { api };
