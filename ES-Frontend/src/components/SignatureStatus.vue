<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { authState } from '../service/Auth';
import { webSocketService } from '../service/WebSocketService';

const signatureRequests = ref([]);
const activeRequests = ref([]);

// Funciones de conversión para mostrar coordenadas legibles
// Convertir de 0-based (backend) a 1-based (frontend) para mostrar
const convertToFrontendPage = (page) => page + 1;

// Función para mostrar coordenadas en formato legible
const formatCoordinates = (posX, posY) => {
  return `(${posX}, ${posY})`;
};

// Función para mostrar información de conversión
const getConversionInfo = (page, posX, posY) => {
  const frontendPage = convertToFrontendPage(page);
  return {
    frontendPage,
    backendPage: page,
    coordinates: formatCoordinates(posX, posY),
    info: `Backend: Página ${page} | Frontend: Página ${frontendPage}`
  };
};

onMounted(() => {
  setupWebSocket();
});

onUnmounted(() => {
  webSocketService.removeListener('signature_request');
  webSocketService.removeListener('signature_request_update');
});

function setupWebSocket() {
  if (authState.token && !webSocketService.isConnectedStatus()) {
    webSocketService.connect(authState.token);
  }

  // Escuchar nuevas solicitudes de firma
  webSocketService.addListener('signature_request', (message) => {
    console.log('Nueva solicitud de firma recibida:', message);
    const newRequest = {
      id: message.requestId,
      documentName: message.documentName,
      page: message.page,
      posX: message.posX,
      posY: message.posY,
      timestamp: message.timestamp,
      status: 'PENDIENTE'
    };
    
    // Agregar a la lista de solicitudes activas
    activeRequests.value.unshift(newRequest);
    
    // También agregar a la lista general
    if (!signatureRequests.value.some(req => req.id === message.requestId)) {
      signatureRequests.value.unshift(newRequest);
    }
  });

  // Escuchar actualizaciones de solicitudes
  webSocketService.addListener('signature_request_update', (message) => {
    console.log('Actualización de solicitud recibida:', message);
    
    // Actualizar en ambas listas
    const updateRequest = (list) => {
      const index = list.findIndex(req => req.id === message.requestId);
      if (index !== -1) {
        list[index].status = message.status;
        list[index].lastUpdate = message.message;
      }
    };
    
    updateRequest(signatureRequests.value);
    updateRequest(activeRequests.value);
  });
}

function getStatusColor(status) {
  switch (status) {
    case 'PENDIENTE': return 'text-yellow-600 bg-yellow-100';
    case 'PERMITIDO': return 'text-green-600 bg-green-100';
    case 'DENEGADO': return 'text-red-600 bg-red-100';
    case 'COMPLETADO': return 'text-blue-600 bg-blue-100';
    default: return 'text-gray-600 bg-gray-100';
  }
}

function getStatusIcon(status) {
  switch (status) {
    case 'PENDIENTE': return '⏳';
    case 'PERMITIDO': return '✅';
    case 'DENEGADO': return '❌';
    case 'COMPLETADO': return '🎉';
    default: return '❓';
  }
}
</script>

<template>
  <div class="bg-white/90 rounded-xl shadow p-8 max-w-4xl mx-auto">
    <h2 class="text-xl font-bold mb-6">Estado de Solicitudes de Firma</h2>
    
    <!-- Solicitudes Activas -->
    <div class="mb-8">
      <h3 class="text-lg font-semibold mb-4 text-emerald-700">Solicitudes Activas</h3>
      <div v-if="activeRequests.length === 0" class="text-center text-slate-500 py-8">
        No hay solicitudes activas en este momento
      </div>
      <div v-else class="grid gap-4">
        <div v-for="request in activeRequests" :key="request.id" 
             class="border border-emerald-200 rounded-lg p-4 bg-emerald-50">
          <div class="flex items-center justify-between">
            <div>
              <h4 class="font-medium text-emerald-900">{{ request.documentName }}</h4>
              <div class="text-sm text-emerald-700">
                <div>Página Backend: {{ request.page }} | Página Frontend: {{ convertToFrontendPage(request.page) }}</div>
                <div>Posición PyHanko: {{ formatCoordinates(request.posX, request.posY) }}</div>
                <div class="text-xs text-emerald-600 mt-1">
                  {{ getConversionInfo(request.page, request.posX, request.posY).info }}
                </div>
              </div>
              <p class="text-xs text-emerald-600">
                Recibido: {{ new Date(request.timestamp).toLocaleString() }}
              </p>
            </div>
            <div class="flex items-center space-x-2">
              <span class="text-2xl">{{ getStatusIcon(request.status) }}</span>
              <span :class="['px-3 py-1 rounded-full text-xs font-medium', getStatusColor(request.status)]">
                {{ request.status }}
              </span>
            </div>
          </div>
          <div v-if="request.lastUpdate" class="mt-2 p-2 bg-white rounded border-l-4 border-emerald-400">
            <p class="text-sm text-emerald-800">{{ request.lastUpdate }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Historial de Solicitudes -->
    <div>
      <h3 class="text-lg font-semibold mb-4 text-slate-700">Historial de Solicitudes</h3>
      <div v-if="signatureRequests.length === 0" class="text-center text-slate-500 py-8">
        No hay solicitudes en el historial
      </div>
      <div v-else class="space-y-3">
        <div v-for="request in signatureRequests" :key="request.id" 
             class="border rounded-lg p-3 hover:bg-slate-50 transition-colors">
          <div class="flex items-center justify-between">
            <div class="flex-1">
              <h4 class="font-medium text-slate-900">{{ request.documentName }}</h4>
              <div class="text-sm text-slate-600">
                <div>Página Backend: {{ request.page }} | Página Frontend: {{ convertToFrontendPage(request.page) }}</div>
                <div>Posición PyHanko: {{ formatCoordinates(request.posX, request.posY) }}</div>
              </div>
            </div>
            <div class="flex items-center space-x-2">
              <span class="text-lg">{{ getStatusIcon(request.status) }}</span>
              <span :class="['px-2 py-1 rounded text-xs font-medium', getStatusColor(request.status)]">
                {{ request.status }}
              </span>
            </div>
          </div>
          <div v-if="request.lastUpdate" class="mt-2 text-xs text-slate-500">
            {{ request.lastUpdate }}
          </div>
        </div>
      </div>
    </div>

    <!-- Indicador de conexión WebSocket -->
    <div class="fixed bottom-4 right-4">
      <div class="flex items-center space-x-2 bg-white rounded-lg shadow-lg px-3 py-2">
        <div :class="['w-3 h-3 rounded-full', webSocketService.isConnectedStatus() ? 'bg-green-500' : 'bg-red-500']"></div>
        <span class="text-sm text-slate-600">
          {{ webSocketService.isConnectedStatus() ? 'Conectado' : 'Desconectado' }}
        </span>
      </div>
    </div>
  </div>
</template>
