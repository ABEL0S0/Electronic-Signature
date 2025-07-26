declare module '../utils/websocket.js' {
  export interface NotificationMessage {
    title: string;
    message: string;
    timestamp: string;
  }

  export interface ConnectionInfo {
    isConnected: boolean;
    connectionMethod: 'websocket';
    reconnectAttempts: number;
    maxReconnectAttempts: number;
  }

  class WebSocketManager {
    connect(token: string): Promise<void>;
    disconnect(): void;
    addListener(type: 'user' | 'admin', callback: (message: NotificationMessage) => void): void;
    removeListener(type: 'user' | 'admin'): void;
    isConnectedStatus(): boolean;
    getConnectionInfo(): ConnectionInfo;
  }

  const webSocketManager: WebSocketManager;
  export default webSocketManager;
} 