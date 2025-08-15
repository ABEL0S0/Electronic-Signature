<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { getAllSignatureRequests, getUsersBySignatureRequest, responderSolicitudFirma, getCertificatesByUser } from '../utils/api';
import { authState } from '../service/Auth';
import { webSocketService } from '../service/WebSocketService';
import { validatePassword } from '../utils/password-validation';

const requests = ref([]);
const loading = ref(false);
const selectedRequest = ref(null);
const userEntry = ref(null);
const certificateId = ref('');
const certificatePassword = ref('');
const status = ref('');
const certificates = ref([]);

// Validación de contraseña del certificado
const certificatePasswordValidation = computed(() => validatePassword(certificatePassword.value));
const isCertificatePasswordValid = computed(() => certificatePasswordValidation.value.isValid);
const certificatePasswordStrength = computed(() => certificatePasswordValidation.value.strength);
const certificatePasswordScore = computed(() => certificatePasswordValidation.value.score);
const certificatePasswordColor = computed(() => certificatePasswordValidation.value.color);

// Funciones de conversión para PyHanko (similar a SignDocuments.vue)
// Convertir de 1-based (frontend) a 0-based (backend)
const convertToBackendPage = (page) => page - 1;

// Convertir coordenadas del canvas al sistema PDF estándar
const convertCanvasToPDFCoordinates = (canvasX, canvasY, canvasWidth, canvasHeight, pageWidth, pageHeight) => {
  // Convertir coordenadas del canvas (0,0 en esquina superior izquierda, Y hacia abajo)
  // al sistema PDF (0,0 en esquina inferior izquierda, Y hacia arriba)
  
  // X: se mantiene igual (crece hacia la derecha en ambos sistemas)
  const pdfX = (canvasX / canvasWidth) * pageWidth;
  
  // Y: invertir el eje Y (canvas Y hacia abajo -> PDF Y hacia arriba)
  const pdfY = pageHeight - ((canvasY / canvasHeight) * pageHeight);
  
  return { x: Math.round(pdfX), y: Math.round(pdfY) };
};

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
  await fetchRequests();
  await fetchCertificates();
  setupWebSocket();
});

onUnmounted(() => {
  // Limpiar listeners de WebSocket
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
    // Agregar la nueva solicitud a la lista
    const newRequest = {
      id: message.requestId,
      documentPath: message.documentPath,
      users: [{
        userId: authState.user?.id,
        page: message.page,
        posX: message.posX,
        posY: message.posY,
        status: 'PENDIENTE'
      }]
    };
    
    // Verificar si ya existe la solicitud
    if (!requests.value.some(req => req.id === message.requestId)) {
      requests.value.unshift(newRequest);
    }
  });

  // Escuchar actualizaciones de solicitudes
  webSocketService.addListener('signature_request_update', (message) => {
    console.log('Actualización de solicitud recibida:', message);
    // Actualizar el estado de la solicitud si está en la lista
    const requestIndex = requests.value.findIndex(req => req.id === message.requestId);
    if (requestIndex !== -1) {
      const userIndex = requests.value[requestIndex].users.findIndex(u => u.userId === authState.user?.id);
      if (userIndex !== -1) {
        requests.value[requestIndex].users[userIndex].status = message.status;
      }
    }
  });
}

async function fetchRequests() {
  loading.value = true;
  try {
    const res = await getAllSignatureRequests();
    console.log('SignatureRequests response:', res.data);
    const userId = authState.user?.id;
    // Robustly handle if res.data is not an array
    let data = [];
    if (Array.isArray(res.data)) {
      data = res.data;
    } else if (res.data && Array.isArray(res.data.data)) {
      data = res.data.data;
    }
    requests.value = data.filter(req =>
      req.users && Array.isArray(req.users) && req.users.some(u => u.userId === userId && u.status === 'PENDIENTE')
    );
  } finally {
    loading.value = false;
  }
}

async function fetchCertificates() {
  const user = authState.user;
  if (!user?.email) return;
  try {
    const res = await getCertificatesByUser(user.email);
    certificates.value = res.data;
  } catch {}
}

function openRequest(request) {
  selectedRequest.value = request;
  const userId = authState.user?.id;
  userEntry.value = request.users.find(u => u.userId === userId);
  certificateId.value = '';
  certificatePassword.value = '';
  status.value = '';
}

async function respond(permitir) {
  if (permitir && (!certificateId.value || !certificatePassword.value)) {
    status.value = 'Selecciona tu certificado y escribe la contraseña.';
    return;
  }
  
  if (permitir && !isCertificatePasswordValid.value) {
    status.value = 'La contraseña del certificado no cumple con los requisitos de seguridad.';
    return;
  }
  
  status.value = 'Enviando respuesta...';
  try {
    // Convertir coordenadas para PyHanko
    const backendPage = convertToBackendPage(userEntry.value.page);
    
    // Calcular la caja de firma para PyHanko usando las coordenadas originales
    const signatureBox = calculateSignatureBox(userEntry.value.posX, userEntry.value.posY);
    
    const payload = {
      id: userEntry.value.id,
      signatureRequest: { id: selectedRequest.value.id },
      signatureRequestId: selectedRequest.value.id, // Agregar también este campo para compatibilidad
      userId: userEntry.value.userId,
      page: backendPage, // Página convertida a 0-based para el backend
      posX: signatureBox.x1, // x1 de la caja de firma para PyHanko
      posY: signatureBox.y1, // y1 de la caja de firma para PyHanko
      status: permitir ? 'PERMITIDO' : 'DENEGADO',
      certificateId: permitir ? certificateId.value : undefined,
      certificatePassword: permitir ? certificatePassword.value : undefined
    };
    
    console.log('Enviando payload para PyHanko:', {
      original: {
        page: userEntry.value.page,
        posX: userEntry.value.posX,
        posY: userEntry.value.posY
      },
      converted: {
        page: backendPage,
        posX: signatureBox.x1,
        posY: signatureBox.y1,
        signatureBox: signatureBox
      },
      payload: payload
    });
    
    await responderSolicitudFirma(payload);
    status.value = 'Respuesta enviada correctamente.';
    
    // Actualizar el estado local inmediatamente
    userEntry.value.status = permitir ? 'PERMITIDO' : 'DENEGADO';
    
    // Remover la solicitud de la lista si ya no está pendiente
    if (userEntry.value.status !== 'PENDIENTE') {
      const requestIndex = requests.value.findIndex(req => req.id === selectedRequest.value.id);
      if (requestIndex !== -1) {
        requests.value.splice(requestIndex, 1);
      }
    }
    
    selectedRequest.value = null;
  } catch (error) {
    console.error('Error al responder:', error);
    status.value = 'Error al enviar la respuesta: ' + (error.response?.data?.message || error.message);
  }
}
</script>

<template>
  <div class="bg-white/90 rounded-xl shadow p-8 max-w-3xl mx-auto">
    <h2 class="text-xl font-bold mb-4">Solicitudes de Firma Pendientes</h2>
    <div v-if="loading" class="text-center text-slate-500">Cargando...</div>
    <div v-else>
      <div v-if="requests.length === 0" class="text-center text-slate-500">No tienes solicitudes pendientes.</div>
      <ul v-else class="space-y-4">
        <li v-for="req in requests" :key="req.id" class="border rounded p-4 flex flex-col">
          <div class="flex justify-between items-center mb-2">
            <div>
              <span class="font-medium">Documento:</span> {{ req.documentPath.split('/').pop() }}
            </div>
            <button class="text-emerald-600 hover:underline" @click="openRequest(req)">Responder</button>
          </div>
          <div class="text-sm text-slate-600">Usuarios: {{ req.users.length }}</div>
        </li>
      </ul>
    </div>
    <!-- Modal para responder -->
    <div v-if="selectedRequest" class="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
      <div class="bg-white rounded-xl shadow-lg p-8 w-full max-w-md relative">
        <button class="absolute top-2 right-2 text-slate-400 hover:text-slate-600" @click="selectedRequest = null">&times;</button>
        <h3 class="text-lg font-bold mb-2">Responder Solicitud</h3>
        <div class="mb-2"><span class="font-medium">Documento:</span> {{ selectedRequest.documentPath.split('/').pop() }}</div>
        <div class="mb-2"><span class="font-medium">Página:</span> {{ userEntry.page }}, <span class="font-medium">X:</span> {{ userEntry.posX }}, <span class="font-medium">Y:</span> {{ userEntry.posY }}</div>
        <div class="mb-4">
          <label class="block font-medium mb-1">¿Permitir firma?</label>
          <div class="flex space-x-2">
            <button @click="respond(true)" class="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded">Permitir</button>
            <button @click="respond(false)" class="bg-red-500 hover:bg-red-600 text-white px-4 py-2 rounded">Denegar</button>
          </div>
        </div>
        <div v-if="userEntry && userEntry.status === 'PENDIENTE'">
          <div class="mb-2">
            <label class="block font-medium mb-1">Certificado</label>
            <select v-model="certificateId" class="w-full border rounded p-2">
              <option :value="''">-- Selecciona tu certificado --</option>
              <option v-for="cert in certificates" :key="cert.id" :value="cert.id">{{ cert.filename }}</option>
            </select>
          </div>
          <div class="mb-2">
            <label class="block font-medium mb-1">Contraseña del certificado</label>
            <input v-model="certificatePassword" type="password" class="w-full border rounded p-2" placeholder="Ingresa la contraseña del certificado" />
            
            <!-- Indicador de fortaleza de contraseña del certificado -->
            <div v-if="certificatePassword" class="mt-3 p-3 bg-gray-50 rounded-lg border">
              <div class="flex items-center justify-between mb-2">
                <span class="text-sm font-medium text-gray-700">Fortaleza de la contraseña:</span>
                <span :class="['text-sm font-semibold', certificatePasswordColor]">{{ certificatePasswordStrength }}</span>
              </div>
              
              <!-- Barra de progreso -->
              <div class="w-full bg-gray-200 rounded-full h-2 mb-3">
                <div 
                  :class="['h-2 rounded-full transition-all duration-300', 
                    certificatePasswordScore < 40 ? 'bg-red-500' : 
                    certificatePasswordScore < 60 ? 'bg-orange-500' : 
                    certificatePasswordScore < 80 ? 'bg-yellow-500' : 
                    certificatePasswordScore < 90 ? 'bg-blue-500' : 'bg-green-500']"
                  :style="{ width: certificatePasswordScore + '%' }"
                ></div>
              </div>
              
              <!-- Puntuación -->
              <div class="text-xs text-gray-500 mb-3">
                Puntuación: {{ certificatePasswordScore }}/100
              </div>
              
              <!-- Requisitos -->
              <div class="space-y-1">
                <div class="flex items-center text-sm">
                  <span :class="['w-4 h-4 mr-2 rounded-full', certificatePasswordValidation.requirements.minLength ? 'bg-green-500' : 'bg-red-500']">
                    <svg v-if="certificatePasswordValidation.requirements.minLength" class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                    </svg>
                    <svg v-else class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
                    </svg>
                  </span>
                  Mínimo 8 caracteres
                </div>
                <div class="flex items-center text-sm">
                  <span :class="['w-4 h-4 mr-2 rounded-full', certificatePasswordValidation.requirements.hasUppercase ? 'bg-green-500' : 'bg-red-500']">
                    <svg v-if="certificatePasswordValidation.requirements.hasUppercase" class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                    </svg>
                    <svg v-else class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
                    </svg>
                  </span>
                  Al menos 1 letra mayúscula
                </div>
                <div class="flex items-center text-sm">
                  <span :class="['w-4 h-4 mr-2 rounded-full', certificatePasswordValidation.requirements.hasLowercase ? 'bg-green-500' : 'bg-red-500']">
                    <svg v-if="certificatePasswordValidation.requirements.hasLowercase" class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                    </svg>
                    <svg v-else class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
                    </svg>
                  </span>
                  Al menos 1 letra minúscula
                </div>
                <div class="flex items-center text-sm">
                  <span :class="['w-4 h-4 mr-2 rounded-full', certificatePasswordValidation.requirements.hasNumber ? 'bg-green-500' : 'bg-red-500']">
                    <svg v-if="certificatePasswordValidation.requirements.hasNumber" class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                    </svg>
                    <svg v-else class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
                    </svg>
                  </span>
                  Al menos 1 número
                </div>
                <div class="flex items-center text-sm">
                  <span :class="['w-4 h-4 mr-2 rounded-full', certificatePasswordValidation.requirements.hasSpecialChar ? 'bg-green-500' : 'bg-red-500']">
                    <svg v-if="certificatePasswordValidation.requirements.hasSpecialChar" class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                    </svg>
                    <svg v-else class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
                    </svg>
                  </span>
                  Al menos 1 símbolo especial (!@#$%^&*)
                </div>
              </div>
              
              <!-- Sugerencias -->
              <div v-if="certificatePasswordValidation.feedback.length > 0" class="mt-3 p-2 bg-yellow-50 border border-yellow-200 rounded">
                <p class="text-xs text-yellow-800 font-medium mb-1">Sugerencias para mejorar:</p>
                <ul class="text-xs text-yellow-700 space-y-1">
                  <li v-for="suggestion in certificatePasswordValidation.feedback" :key="suggestion" class="flex items-center">
                    <span class="w-1 h-1 bg-yellow-500 rounded-full mr-2"></span>
                    {{ suggestion }}
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
        <p v-if="status" class="mt-2 text-center" :class="status.includes('correctamente') ? 'text-green-600' : 'text-red-600'">{{ status }}</p>
      </div>
    </div>
  </div>
</template>
