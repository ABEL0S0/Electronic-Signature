<template>
  <div v-if="notifications.length > 0" class="fixed top-4 right-4 z-50 space-y-2">
    <TransitionGroup name="notification" tag="div" class="space-y-2">
      <div
        v-for="notification in notifications"
        :key="notification.id"
        class="bg-white border-l-4 border-emerald-500 shadow-lg rounded-lg p-4 max-w-sm w-full"
        :class="{
          'border-emerald-500': notification.type === 'success',
          'border-amber-500': notification.type === 'warning',
          'border-red-500': notification.type === 'error'
        }"
      >
        <div class="flex items-start justify-between">
          <div class="flex items-start space-x-3">
            <div class="flex-shrink-0">
              <svg v-if="notification.type === 'success'" class="w-5 h-5 text-emerald-500" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
              </svg>
              <svg v-else-if="notification.type === 'warning'" class="w-5 h-5 text-amber-500" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
              </svg>
              <svg v-else class="w-5 h-5 text-red-500" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
              </svg>
            </div>
            <div class="flex-1">
              <h4 class="text-sm font-medium text-slate-900">{{ notification.title }}</h4>
              <p class="text-sm text-slate-600 mt-1">{{ notification.message }}</p>
              <p class="text-xs text-slate-400 mt-2">{{ formatTime(notification.timestamp) }}</p>
            </div>
          </div>
          <button
            @click="removeNotification(notification.id)"
            class="flex-shrink-0 text-slate-400 hover:text-slate-600 transition-colors"
          >
            <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
            </svg>
          </button>
        </div>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { webSocketService, type NotificationMessage } from '../service/WebSocketService';
import { authState } from '../service/Auth';

interface Notification {
  id: string;
  title: string;
  message: string;
  timestamp: string;
  type: 'success' | 'warning' | 'error';
}

const notifications = ref<Notification[]>([]);

const addNotification = (message: NotificationMessage, type: 'success' | 'warning' | 'error' = 'success') => {
  const notification: Notification = {
    id: Date.now().toString(),
    title: message.title,
    message: message.message,
    timestamp: message.timestamp,
    type
  };
  
  notifications.value.unshift(notification);
  
  // Auto-remove after 5 seconds
  setTimeout(() => {
    removeNotification(notification.id);
  }, 5000);
};

const removeNotification = (id: string) => {
  const index = notifications.value.findIndex(n => n.id === id);
  if (index > -1) {
    notifications.value.splice(index, 1);
  }
};

const formatTime = (timestamp: string) => {
  const date = new Date(timestamp);
  return date.toLocaleTimeString('es-ES', { 
    hour: '2-digit', 
    minute: '2-digit' 
  });
};

onMounted(() => {
  // Si ya hay un token, conectar WebSocket
  if (authState.token && !webSocketService.isConnectedStatus()) {
    webSocketService.connect(authState.token);
  }
  
  // Escuchar notificaciones de usuario
  webSocketService.addListener('user', (message) => {
    addNotification(message, 'success');
  });
  
  // Escuchar notificaciones de admin
  webSocketService.addListener('admin', (message) => {
    addNotification(message, 'warning');
  });
});

onUnmounted(() => {
  webSocketService.removeListener('user');
  webSocketService.removeListener('admin');
});
</script>

<style scoped>
.notification-enter-active,
.notification-leave-active {
  transition: all 0.3s ease;
}

.notification-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.notification-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

.notification-move {
  transition: transform 0.3s ease;
}
</style> 