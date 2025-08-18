// Antes
//const WS_URL = import.meta.env.VITE_WS_URL;

// Cambiar por:
const getWebSocketUrl = () => {
    const configuredUrl = import.meta.env.VITE_WS_URL;
    console.log('VITE_WS_URL from env:', configuredUrl);
    console.log('All env variables:', import.meta.env);
    
    if (configuredUrl) {
        return configuredUrl;
    }
    
    // Fallback: construir la URL basada en la ubicación actual
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const fallbackUrl = `${protocol}//${window.location.host}/ws`;
    console.log('Using fallback WebSocket URL:', fallbackUrl);
    return fallbackUrl;
};

const WS_URL = getWebSocketUrl();

interface AuthSuccessMessage {
  userEmail: string;
  userRole: string;
  userId: number;
}

interface NotificationMessage {
  title: string;
  message: string;
  timestamp: string;
}

interface SignatureRequestMessage {
  requestId: number;
  documentPath: string;
  documentName: string;
  page: number;
  posX: number;
  posY: number;
  timestamp: string;
}

interface SignatureRequestUpdateMessage {
  requestId: number;
  status: string;
  message: string;
  timestamp: string;
}

type MessageListener = (message: any) => void;

class WebSocketManager {
  private socket: WebSocket | null;
  private isConnected: boolean;
  private reconnectAttempts: number;
  private readonly maxReconnectAttempts: number;
  private listeners: Map<string, MessageListener>;
  private token: string;
  private manualDisconnect: boolean;

  constructor() {
    this.socket = null;
    this.isConnected = false;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.listeners = new Map();
    this.token = '';
    this.manualDisconnect = false; // Flag to prevent auto-reconnection after manual disconnect
  }

  async connect(token: string): Promise<void> {
    if (this.isConnected) return;
    
    // Reset manual disconnect flag when connecting with a new token
    this.manualDisconnect = false;
    this.reconnectAttempts = 0;
    
    this.token = token;
    console.log('🔌 Intentando conectar WebSocket...');
    
    try {
      if (typeof WebSocket !== 'undefined') {
        await this.connectWithWebSocket(token);
      } else {
        console.error('❌ WebSocket no está disponible en este navegador');
      }
    } catch (error) {
      console.error('Error connecting:', error);
    }
  }

  private async connectWithWebSocket(token: string): Promise<void> {
    return new Promise((resolve, reject) => {
        console.log('Attempting to connect to WebSocket at:', WS_URL);
        console.log('Environment variables:', import.meta.env);
        
        this.socket = new WebSocket(WS_URL);
      
      this.socket.onopen = () => {
        console.log('✅ WebSocket conectado exitosamente');
        this.isConnected = true;
        this.reconnectAttempts = 0;
        this.sendMessage({ type: 'AUTH', token });
        resolve();
      };

      this.socket.onmessage = (event) => {
        this.handleMessage(event.data);
      };

      this.socket.onclose = (event) => {
        console.log('❌ WebSocket desconectado. Código:', event.code, 'Razón:', event.reason);
        this.isConnected = false;
        this.attemptReconnect();
      };

      this.socket.onerror = (error) => {
        console.error('🚨 WebSocket error:', error);
        this.isConnected = false;
        reject(error);
      };
    });
  }

  private handleMessage(data: string): void {
    try {
      console.log('📨 Mensaje recibido:', data);
      
      const parsedData = JSON.parse(data);
      
      if (parsedData.type === 'CONNECTION') {
        console.log('✅ Conexión WebSocket confirmada:', parsedData.message);
      } else if (parsedData.type === 'AUTH_SUCCESS') {
        console.log('🔐 Autenticación exitosa:', parsedData.message, 'Usuario:', parsedData.userEmail);
        // Notificar que la autenticación fue exitosa
        this.notifyListeners('auth_success', {
          userEmail: parsedData.userEmail,
          userRole: parsedData.userRole,
          userId: parsedData.userId
        } as AuthSuccessMessage);
      } else if (parsedData.type === 'NOTIFICATION') {
        console.log('🔔 Notificación recibida:', parsedData);
        const notification: NotificationMessage = {
          title: parsedData.title,
          message: parsedData.message,
          timestamp: parsedData.timestamp || new Date().toISOString()
        };
        
        if (parsedData.target === 'USER') {
          this.notifyListeners('user', notification);
        } else if (parsedData.target === 'ADMIN') {
          this.notifyListeners('admin', notification);
        }
      } else if (parsedData.type === 'SIGNATURE_REQUEST') {
        console.log('📝 Nueva solicitud de firma recibida:', parsedData);
        const signatureRequest: SignatureRequestMessage = {
          requestId: parsedData.requestId,
          documentPath: parsedData.documentPath,
          documentName: parsedData.documentName,
          page: parsedData.page,
          posX: parsedData.posX,
          posY: parsedData.posY,
          timestamp: parsedData.timestamp || new Date().toISOString()
        };
        this.notifyListeners('signature_request', signatureRequest);
      } else if (parsedData.type === 'SIGNATURE_REQUEST_UPDATE') {
        console.log('🔄 Actualización de solicitud de firma:', parsedData);
        const update: SignatureRequestUpdateMessage = {
          requestId: parsedData.requestId,
          status: parsedData.status,
          message: parsedData.message,
          timestamp: parsedData.timestamp || new Date().toISOString()
        };
        this.notifyListeners('signature_request_update', update);
      } else {
        console.log('📝 Mensaje de otro tipo:', parsedData);
      }
    } catch (error) {
      console.error('❌ Error parsing message:', error);
      console.log('Mensaje raw:', data);
    }
  }

  sendMessage(message: any): void {
    if (this.socket && this.socket.readyState === 1) { // WebSocket.OPEN = 1
      this.socket.send(JSON.stringify(message));
      console.log('📤 Mensaje enviado:', message);
    } else {
      console.warn('⚠️ No se pudo enviar mensaje. Estado:', this.socket?.readyState);
    }
  }

  private attemptReconnect(): void {
    // Don't attempt to reconnect if manually disconnected
    if (this.manualDisconnect) {
      console.log('🔌 Reconexión cancelada - desconexión manual activa');
      return;
    }
    
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`🔄 Intentando reconectar WebSocket... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      
      setTimeout(() => {
        this.connect(this.token);
      }, 3000);
    } else {
      console.log('❌ Máximo de intentos de reconexión alcanzado. WebSocket deshabilitado.');
    }
  }

  disconnect(): void {
    this.manualDisconnect = true; // Set flag to prevent auto-reconnection
    this.reconnectAttempts = 0; // Reset reconnect attempts
    
    if (this.socket) {
      this.socket.close();
      this.socket = null;
      this.isConnected = false;
      console.log('🔌 WebSocket desconectado manualmente');
    }
    
    // Clear all listeners
    this.listeners.clear();
  }

  addListener(type: string, callback: MessageListener): void {
    // Si ya existe un listener para este tipo, lo removemos primero
    this.listeners.delete(type);
    this.listeners.set(type, callback);
  }

  removeListener(type: string): void {
    this.listeners.delete(type);
  }

  private notifyListeners(type: string, message: any): void {
    const listener = this.listeners.get(type);
    if (listener) {
      listener(message);
    }
  }

  isConnectedStatus(): boolean {
    return this.isConnected;
  }

  getConnectionInfo(): {
    isConnected: boolean;
    connectionMethod: string;
    reconnectAttempts: number;
    maxReconnectAttempts: number;
  } {
    return {
      isConnected: this.isConnected,
      connectionMethod: 'websocket',
      reconnectAttempts: this.reconnectAttempts,
      maxReconnectAttempts: this.maxReconnectAttempts
    };
  }
}

// Crear instancia singleton
export const webSocketManager = new WebSocketManager();
