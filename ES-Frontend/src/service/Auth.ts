import { reactive } from "vue";

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
  },

  // Check if user is authenticated
  isAuthenticated(): boolean {
    return authState.isAuthenticated;
  },
};
