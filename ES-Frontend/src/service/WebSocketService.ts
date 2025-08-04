import webSocketManager from '../utils/websocket.js';

export interface NotificationMessage {
  title: string;
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

  addListener(type: 'user' | 'admin' | 'auth_success', callback: (message: NotificationMessage | any) => void) {
    webSocketManager.addListener(type, callback);
  }

  removeListener(type: 'user' | 'admin' | 'auth_success') {
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