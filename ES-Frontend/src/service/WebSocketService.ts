import webSocketManager from '../utils/websocket.js';

export interface NotificationMessage {
  title: string;
  message: string;
  timestamp: string;
}

export interface SignatureRequestMessage {
  requestId: number;
  documentPath: string;
  documentName: string;
  page: number;
  posX: number;
  posY: number;
  timestamp: string;
}

export interface SignatureRequestUpdateMessage {
  requestId: number;
  status: string;
  message: string;
  timestamp: string;
}

// Wrapper TypeScript para el WebSocketManager JavaScript
class WebSocketService {
  connect(token: string) {
    return webSocketManager.connect(token);
  }

  disconnect() {
    webSocketManager.disconnect();
  }

  addListener(type: 'user' | 'admin' | 'auth_success' | 'signature_request' | 'signature_request_update', callback: (message: NotificationMessage | SignatureRequestMessage | SignatureRequestUpdateMessage | any) => void) {
    webSocketManager.addListener(type, callback);
  }

  removeListener(type: 'user' | 'admin' | 'auth_success' | 'signature_request' | 'signature_request_update') {
    webSocketManager.removeListener(type);
  }

  isConnectedStatus() {
    return webSocketManager.isConnectedStatus();
  }

  getConnectionInfo() {
    return webSocketManager.getConnectionInfo();
  }
}

export const webSocketService = new WebSocketService(); 