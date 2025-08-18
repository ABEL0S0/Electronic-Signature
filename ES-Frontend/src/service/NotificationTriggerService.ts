import { authState } from './Auth';
import axios from 'axios';

export class NotificationTriggerService {
  /**
   * Trigger status notifications for the current user
   * This will check for pending documents, missing certificates, etc.
   */
  static async triggerStatusNotifications(): Promise<void> {
    if (!authState.token || !authState.user) {
      return;
    }

    try {
      // Trigger backend to check and send notifications
      await axios.post('/api/notifications/trigger-status', {
        userEmail: authState.user.email
      }, {
        headers: {
          'Authorization': `Bearer ${authState.token}`
        }
      });
    } catch (error) {
      console.error('Error triggering status notifications:', error);
    }
  }

  /**
   * Trigger status notifications after login
   */
  static async triggerAfterLogin(): Promise<void> {
    // Wait a bit for WebSocket to connect
    setTimeout(() => {
      this.triggerStatusNotifications();
    }, 2000);
  }
} 