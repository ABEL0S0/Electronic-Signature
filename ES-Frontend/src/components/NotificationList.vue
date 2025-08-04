<template>
  <div class="relative notification-dropdown">
    <!-- Notification Icon with Badge -->
    <button 
      @click="toggleNotifications" 
      class="relative bg-transparent p-2 rounded-full hover:bg-emerald-50 transition-colors"
    >
      <span v-html="renderIcon('bell', 'w-5 h-5 text-slate-600')"></span>
      <span 
        v-if="unreadCount > 0" 
        class="absolute -top-1 -right-1 w-5 h-5 p-0 bg-emerald-500 text-white text-xs flex items-center justify-center rounded-full"
      >
        {{ unreadCount > 99 ? '99+' : unreadCount }}
      </span>
    </button>

    <!-- Notification Dropdown -->
    <div 
      v-if="isOpen" 
      class="notification-dropdown absolute right-0 mt-2 w-80 bg-white rounded-lg shadow-lg border border-emerald-100 z-50 max-h-96 overflow-y-auto"
    >
      <div class="p-4 border-b border-emerald-100">
        <div class="flex items-center justify-between">
          <h3 class="text-lg font-semibold text-slate-900">Notificaciones</h3>
          <button 
            @click="markAllAsRead" 
            class="text-sm text-emerald-600 hover:text-emerald-700"
            v-if="unreadCount > 0"
          >
            Marcar como leídas
          </button>
        </div>
      </div>
      
      <div class="p-2">
        <div v-if="notifications.length === 0" class="text-center py-8">
          <div class="w-12 h-12 bg-emerald-100 rounded-full flex items-center justify-center mx-auto mb-3">
            <span v-html="renderIcon('bell', 'w-6 h-6 text-emerald-600')"></span>
          </div>
          <p class="text-slate-600">No hay notificaciones</p>
        </div>
        
        <div v-else class="space-y-2">
          <div 
            v-for="notification in notifications" 
            :key="notification.id"
            :class="[
              'p-3 rounded-lg border transition-colors cursor-pointer',
              notification.read ? 'bg-slate-50 border-slate-200' : 'bg-emerald-50 border-emerald-200'
            ]"
            @click="markAsRead(notification.id)"
          >
            <div class="flex items-start space-x-3">
              <div class="flex-shrink-0">
                <div 
                  :class="[
                    'w-8 h-8 rounded-full flex items-center justify-center',
                    notification.read ? 'bg-slate-200' : 'bg-emerald-200'
                  ]"
                >
                  <span v-html="getNotificationIcon(notification.type)" class="w-4 h-4"></span>
                </div>
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium text-slate-900 mb-1">
                  {{ notification.title }}
                </p>
                <p class="text-sm text-slate-600 mb-2">
                  {{ notification.message }}
                </p>
                <p class="text-xs text-slate-500">
                  {{ formatTimestamp(notification.timestamp) }}
                </p>
              </div>
              <div v-if="!notification.read" class="flex-shrink-0">
                <div class="w-2 h-2 bg-emerald-500 rounded-full"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { webSocketService, type NotificationMessage } from '../service/WebSocketService';
import { authState } from '../service/Auth';

interface Notification {
  id: string;
  title: string;
  message: string;
  timestamp: string;
  type: 'success' | 'warning' | 'info' | 'error';
  read: boolean;
}

const notifications = ref<Notification[]>([]);
const isOpen = ref(false);

const unreadCount = computed(() => {
  return notifications.value.filter(n => !n.read).length;
});

const renderIcon = (icon: string, classes: string) => {
  const iconMap: { [key: string]: string } = {
    bell: `<svg class="${classes}" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-5 5v-5zM10.5 3.75a6 6 0 0 1 6 6v3.75l1.5 1.5H3l1.5-1.5V9.75a6 6 0 0 1 6-6z"></path></svg>`,
    check: `<svg class="${classes}" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path></svg>`,
    x: `<svg class="${classes}" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>`,
    file: `<svg class="${classes}" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path></svg>`,
    shield: `<svg class="${classes}" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"></path></svg>`,
    info: `<svg class="${classes}" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>`
  };
  return iconMap[icon] || iconMap.info;
};

const getNotificationIcon = (type: string) => {
  const iconMap: { [key: string]: string } = {
    success: renderIcon('check', 'w-4 h-4 text-emerald-600'),
    warning: renderIcon('info', 'w-4 h-4 text-amber-600'),
    error: renderIcon('x', 'w-4 h-4 text-red-600'),
    info: renderIcon('info', 'w-4 h-4 text-blue-600')
  };
  return iconMap[type] || iconMap.info;
};

const toggleNotifications = () => {
  isOpen.value = !isOpen.value;
};

const markAsRead = (id: string) => {
  const notification = notifications.value.find(n => n.id === id);
  if (notification) {
    notification.read = true;
  }
};

const markAllAsRead = () => {
  notifications.value.forEach(n => n.read = true);
};

const addNotification = (message: NotificationMessage, type: 'success' | 'warning' | 'info' | 'error' = 'info', target: 'user' | 'admin' = 'user') => {
  // Filtrar notificaciones de admin - solo mostrar a usuarios ADMIN
  if (target === 'admin' && authState.user?.role !== 'ADMIN') {
    return;
  }
  
  const notification: Notification = {
    id: Date.now().toString() + Math.random().toString(36).substr(2, 9),
    title: message.title,
    message: message.message,
    timestamp: message.timestamp || new Date().toISOString(),
    type,
    read: false
  };
  
  notifications.value.unshift(notification);
  
  // Mantener solo las últimas 50 notificaciones
  if (notifications.value.length > 50) {
    notifications.value = notifications.value.slice(0, 50);
  }
};

const formatTimestamp = (timestamp: string) => {
  const date = new Date(timestamp);
  const now = new Date();
  const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60));
  
  if (diffInMinutes < 1) return 'Ahora';
  if (diffInMinutes < 60) return `Hace ${diffInMinutes} min`;
  if (diffInMinutes < 1440) return `Hace ${Math.floor(diffInMinutes / 60)}h`;
  return date.toLocaleDateString('es-ES');
};



// Cerrar dropdown cuando se hace click fuera
const handleClickOutside = (event: Event) => {
  const target = event.target as Element;
  const notificationContainer = target.closest('.notification-dropdown');
  if (!notificationContainer) {
    isOpen.value = false;
  }
};

onMounted(() => {
  // Función para configurar listeners
  const setupListeners = () => {
    // Limpiar listeners existentes antes de agregar nuevos
    webSocketService.removeListener('user');
    webSocketService.removeListener('admin');
    webSocketService.removeListener('auth_success');
    
    // Escuchar notificaciones de usuario
    webSocketService.addListener('user', (message) => {
      addNotification(message, 'success', 'user');
    });
    
    // Escuchar notificaciones de admin
    webSocketService.addListener('admin', (message) => {
      addNotification(message, 'warning', 'admin');
    });
    
    // Escuchar autenticación exitosa
    webSocketService.addListener('auth_success', (data) => {
      // Reconfigurar listeners después de autenticación exitosa
      setTimeout(() => {
        setupListeners();
      }, 100);
    });
  };
  
  // Función para limpiar notificaciones
  const clearNotifications = () => {
    notifications.value = [];
    isOpen.value = false;
  };
  
  // Configurar listeners inmediatamente
  setupListeners();
  
  // Conectar WebSocket si no está conectado
  if (authState.token && !webSocketService.isConnectedStatus()) {
    webSocketService.connect(authState.token);
  }
  
  // Escuchar cambios en el estado de autenticación
  const checkAuthState = () => {
    if (!authState.isAuthenticated) {
      clearNotifications();
    }
  };
  
  // Verificar estado de autenticación cada segundo
  const authCheckInterval = setInterval(checkAuthState, 1000);
  
  document.addEventListener('click', handleClickOutside);
  
  // Limpiar intervalo al desmontar
  onUnmounted(() => {
    clearInterval(authCheckInterval);
  });
});

onUnmounted(() => {
  webSocketService.removeListener('user');
  webSocketService.removeListener('admin');
  webSocketService.removeListener('auth_success');
  document.removeEventListener('click', handleClickOutside);
});
</script> 