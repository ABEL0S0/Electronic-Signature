import { reactive } from "vue";
import { webSocketService } from "./WebSocketService";
import { NotificationTriggerService } from "./NotificationTriggerService";

interface AuthState {
  token: string | null;
  user: any | null;
  isAuthenticated: boolean;
}

// Create a reactive state object for authentication
export const authState = reactive<AuthState>({
  token: localStorage.getItem("token"),
  user: JSON.parse(localStorage.getItem("user") || "null"),
  isAuthenticated: !!localStorage.getItem("token"),
});

// Auth methods
export const authService = {
  // Save authentication data
  setAuth(token: string, user: any): void {
    // Save to reactive state
    authState.token = token;
    authState.user = user;
    authState.isAuthenticated = true;

    // Save to localStorage for persistence
    localStorage.setItem("token", token);
    localStorage.setItem("user", JSON.stringify(user));

    // Connect to WebSocket for notifications
    webSocketService.connect(token);
    
    // Trigger status notifications after login
    NotificationTriggerService.triggerAfterLogin();
  },

  // Clear authentication data
  clearAuth(): void {
    // Clear reactive state
    authState.token = null;
    authState.user = null;
    authState.isAuthenticated = false;

    // Clear localStorage
    localStorage.removeItem("token");
    localStorage.removeItem("user");

    // Disconnect WebSocket and prevent auto-reconnection
    webSocketService.disconnect();
    
    // Clear any other stored data
    sessionStorage.clear();
    
    // Clear any cookies if they exist
    document.cookie.split(";").forEach(function(c) { 
      document.cookie = c.replace(/^ +/, "").replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/"); 
    });
  },

  // Check if user is authenticated
  isAuthenticated(): boolean {
    return authState.isAuthenticated;
  },
};
