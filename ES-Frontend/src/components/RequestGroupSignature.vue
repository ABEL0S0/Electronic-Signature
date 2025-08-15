<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { getDocumentsByUser, getAllUsers, createSignatureRequest } from '../utils/api';
import { authState } from '../service/Auth';
import { webSocketService } from '../service/WebSocketService';

const documents = ref([]);
const users = ref([]);
const selectedDocument = ref(null);
const searchQuery = ref('');
const userResults = ref([]);
const selectedUsers = ref([]); // [{user, page, posX, posY}]
const position = ref({ page: 1, posX: 100, posY: 100 });
const status = ref('');
const notifications = ref([]);

// Funciones de conversión para PyHanko (similar a SignDocuments.vue)
// Convertir de 1-based (frontend) a 0-based (backend)
const convertToBackendPage = (page) => page - 1;

// Calcular las coordenadas de la caja de firma para PyHanko
const calculateSignatureBox = (centerX, centerY, signatureWidth = 120, signatureHeight = 60) => {
  // PyHanko usa box = (x1, y1, x2, y2) donde:
  // (x1, y1) = esquina inferior izquierda
  // (x2, y2) = esquina superior derecha
  
  const halfWidth = signatureWidth / 2;
  const halfHeight = signatureHeight / 2;
  
  const x1 = centerX - halfWidth; // esquina inferior izquierda X
  const y1 = centerY - halfHeight; // esquina inferior izquierda Y
  const x2 = centerX + halfWidth; // esquina superior derecha X
  const y2 = centerY + halfHeight; // esquina superior derecha Y
  
  return { x1, y1, x2, y2 };
};

onMounted(async () => {
  await fetchDocuments();
  await fetchUsers();
  setupWebSocket();
});

onUnmounted(() => {
  webSocketService.removeListener('signature_request_update');
});

function setupWebSocket() {
  if (authState.token && !webSocketService.isConnectedStatus()) {
    webSocketService.connect(authState.token);
  }

  // Escuchar actualizaciones de solicitudes de firma
  webSocketService.addListener('signature_request_update', (message) => {
    console.log('Actualización de solicitud de firma:', message);
    addNotification('Actualización de Firma', message.message, 'info');
  });
}

function addNotification(title, message, type = 'info') {
  const notification = {
    id: Date.now(),
    title,
    message,
    type,
    timestamp: new Date().toISOString()
  };
  notifications.value.unshift(notification);
  
  // Remover notificación después de 5 segundos
  setTimeout(() => {
    const index = notifications.value.findIndex(n => n.id === notification.id);
    if (index !== -1) {
      notifications.value.splice(index, 1);
    }
  }, 5000);
}

async function fetchDocuments() {
  try {
    const res = await getDocumentsByUser();
    documents.value = res.data;
  } catch {}
}

async function fetchUsers() {
  try {
    const res = await getAllUsers();
    users.value = res.data;
  } catch {}
}

function searchUsers() {
  if (!searchQuery.value) {
    userResults.value = users.value;
    return;
  }
  const q = searchQuery.value.toLowerCase();
  userResults.value = users.value.filter(u =>
    u.email.toLowerCase().includes(q) ||
    u.firstName.toLowerCase().includes(q) ||
    u.lastName.toLowerCase().includes(q)
  );
}

function addUser(user) {
  if (selectedUsers.value.some(u => u.user.id === user.id)) return;
  
  // Calcular la caja de firma para PyHanko usando las coordenadas del usuario
  const signatureBox = calculateSignatureBox(position.value.posX, position.value.posY);
  
  selectedUsers.value.push({
    user,
    page: position.value.page,
    posX: signatureBox.x1, // x1 de la caja de firma para PyHanko
    posY: signatureBox.y1, // y1 de la caja de firma para PyHanko
    // Guardar también las coordenadas originales para mostrar en la UI
    originalPosX: position.value.posX,
    originalPosY: position.value.posY
  });
  searchQuery.value = '';
  userResults.value = [];
}

function removeUser(userId) {
  selectedUsers.value = selectedUsers.value.filter(u => u.user.id !== userId);
}

async function submitRequest() {
  if (!selectedDocument.value || selectedUsers.value.length === 0) {
    status.value = 'Selecciona un documento y al menos un usuario.';
    return;
  }
  status.value = 'Enviando solicitud...';
  try {
    const payload = {
      documentPath: selectedDocument.value.filePath,
      users: selectedUsers.value.map(u => ({
        userId: u.user.id,
        page: convertToBackendPage(u.page), // Convertir a 0-based para el backend
        posX: u.posX, // Ya está en formato PyHanko (x1)
        posY: u.posY  // Ya está en formato PyHanko (y1)
      }))
    };
    
    console.log('Enviando solicitud de firma para PyHanko:', {
      original: selectedUsers.value.map(u => ({
        user: u.user.email,
        page: u.page,
        posX: u.originalPosX,
        posY: u.originalPosY
      })),
      converted: payload.users.map(u => ({
        userId: u.userId,
        page: u.page,
        posX: u.posX,
        posY: u.posY
      }))
    });
    
    await createSignatureRequest(payload);
    status.value = 'Solicitud enviada correctamente.';
    
    // Agregar notificación de éxito
    addNotification('Solicitud Enviada', `Se ha enviado la solicitud de firma a ${selectedUsers.value.length} usuario(s)`, 'success');
    
    selectedUsers.value = [];
    selectedDocument.value = null;
  } catch {
    status.value = 'Error al enviar la solicitud.';
    addNotification('Error', 'No se pudo enviar la solicitud de firma', 'error');
  }
}
</script>

<template>
  <div class="bg-white/90 rounded-xl shadow p-8 max-w-2xl mx-auto">
    <h2 class="text-xl font-bold mb-4">Solicitar Firma Grupal</h2>
    <div class="mb-4">
      <label class="block font-medium mb-1">Selecciona un documento</label>
      <select v-model="selectedDocument" class="w-full border rounded p-2">
        <option :value="null">-- Selecciona --</option>
        <option v-for="doc in documents" :key="doc.id" :value="doc">{{ doc.fileName }}</option>
      </select>
    </div>
    <div class="mb-4">
      <label class="block font-medium mb-1">Buscar usuario</label>
      <input v-model="searchQuery" @input="searchUsers" placeholder="Nombre, apellido o email" class="w-full border rounded p-2 mb-2" />
      <div v-if="userResults.length" class="bg-slate-50 border rounded p-2 max-h-32 overflow-y-auto">
        <div v-for="user in userResults" :key="user.id" class="flex justify-between items-center py-1">
          <span>{{ user.firstName }} {{ user.lastName }} ({{ user.email }})</span>
          <button @click="addUser(user)" class="text-emerald-600 hover:underline">Agregar</button>
        </div>
      </div>
    </div>
    <div class="mb-4">
      <label class="block font-medium mb-1">Posición de la firma (para el usuario a agregar)</label>
      <div class="flex space-x-2">
        <input v-model.number="position.page" type="number" min="1" placeholder="Página" class="w-20 border rounded p-2" />
        <input v-model.number="position.posX" type="number" min="0" placeholder="X" class="w-20 border rounded p-2" />
        <input v-model.number="position.posY" type="number" min="0" placeholder="Y" class="w-20 border rounded p-2" />
      </div>
    </div>
    <div class="mb-4">
      <h4 class="font-medium mb-2">Usuarios seleccionados</h4>
      <div v-if="selectedUsers.length === 0" class="text-slate-500">No hay usuarios agregados.</div>
      <ul>
        <li v-for="u in selectedUsers" :key="u.user.id" class="flex items-center justify-between py-1">
          <div>
            <span>{{ u.user.firstName }} {{ u.user.lastName }} ({{ u.user.email }})</span>
            <div class="text-sm text-slate-600">
              Página: {{ u.page }} | 
              Posición original: ({{ u.originalPosX }}, {{ u.originalPosY }}) | 
              Posición PyHanko: ({{ u.posX }}, {{ u.posY }})
            </div>
          </div>
          <button @click="removeUser(u.user.id)" class="text-red-500 hover:underline">Quitar</button>
        </li>
      </ul>
    </div>
    <button @click="submitRequest" class="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg py-3">Solicitar Firma</button>
    <p v-if="status" class="mt-2 text-center" :class="status.includes('correctamente') ? 'text-green-600' : 'text-red-600'">{{ status }}</p>
    
    <!-- Sistema de notificaciones -->
    <div class="fixed top-4 right-4 z-50 space-y-2">
      <div v-for="notification in notifications" :key="notification.id" 
           class="bg-white border-l-4 shadow-lg rounded-lg p-4 max-w-sm transform transition-all duration-300"
           :class="{
             'border-green-500': notification.type === 'success',
             'border-red-500': notification.type === 'error',
             'border-blue-500': notification.type === 'info'
           }">
        <div class="flex items-start">
          <div class="flex-shrink-0">
            <svg v-if="notification.type === 'success'" class="h-5 w-5 text-green-400" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
            </svg>
            <svg v-else-if="notification.type === 'error'" class="h-5 w-5 text-red-400" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
            </svg>
            <svg v-else class="h-5 w-5 text-blue-400" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
            </svg>
          </div>
          <div class="ml-3 w-0 flex-1">
            <p class="text-sm font-medium text-gray-900">{{ notification.title }}</p>
            <p class="mt-1 text-sm text-gray-500">{{ notification.message }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
