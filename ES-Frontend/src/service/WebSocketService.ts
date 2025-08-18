import { webSocketManager } from '../utils/websocket';

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

  addListener(type: 'user' | 'admin', callback: (message: NotificationMessage | SignatureRequestMessage | SignatureRequestUpdateMessage | any) => void) {
    webSocketManager.addListener(type, callback);
  }

  removeListener(type: 'user' | 'admin') {
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