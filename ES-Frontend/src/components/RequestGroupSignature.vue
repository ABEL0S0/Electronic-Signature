<script setup>
import { ref, computed, onMounted, onUnmounted, shallowRef } from 'vue';
import axios from 'axios';
import { getDocumentsByUser, getAllUsers, createSignatureRequest } from '../utils/api';
import { authState } from '../service/Auth';
import { webSocketService } from '../service/WebSocketService';
import { 
  convertToBackendPage, 
  convertCanvasToPDFCoordinates, 
  calculateSignatureBox 
} from '../utils/pyhanko-conversions';
import firmaImg from '../assets/firma.png';
import * as pdfjsLib from 'pdfjs-dist/legacy/build/pdf';

// Configurar el worker correctamente
pdfjsLib.GlobalWorkerOptions.workerSrc = new URL(
  'pdfjs-dist/legacy/build/pdf.worker.js',
  import.meta.url
).toString();

const documents = ref([]);
const users = ref([]);
const selectedDocument = ref(null);
const searchQuery = ref('');
const userResults = ref([]);
const selectedUsers = ref([]); // [{user, page, posX, posY}]
const position = ref({ page: 1, posX: 100, posY: 100 });
const status = ref('');
const notifications = ref([]);

// Variables para el visualizador de PDF
const canvasRef = ref(null);
const pdfDoc = shallowRef(null);
const currentPage = ref(1);
const scale = ref(1.5);
const loading = ref(false);
const signaturePosition = ref(null);
const pdfBlobUrl = ref(null);

// Variables para arrastre de firma
const isDragging = ref(false);
const dragOffset = ref({ x: 0, y: 0 });

// Asegurar que currentPage siempre sea válido
const getCurrentPage = () => Math.max(1, currentPage.value);

function getToken() {
  return localStorage.getItem('token');
}

// Cleanup function
function cleanupPDFResources() {
  if (pdfBlobUrl.value) {
    URL.revokeObjectURL(pdfBlobUrl.value);
    pdfBlobUrl.value = null;
  }
  pdfDoc.value = null;
  signaturePosition.value = null;
}

// Cargar y renderizar el PDF
async function loadPDF(url) {
  try {
    console.log('Loading PDF from URL:', url);
    
    const task = pdfjsLib.getDocument(url);
    pdfDoc.value = await task.promise;
    
    console.log('PDF cargado:', pdfDoc.value.numPages, 'páginas');
    await renderPage(getCurrentPage());
  } catch (error) {
    console.error('Error al cargar PDF:', error);
    throw error;
  }
}

// Renderizar una página
async function renderPage(pageNum) {
  if (!pdfDoc.value || !canvasRef.value) return;
  
  try {
    // PDF.js usa páginas 1-based (1, 2, 3...)
    const page = await pdfDoc.value.getPage(pageNum);
    const viewport = page.getViewport({ scale: scale.value });
    const canvas = canvasRef.value;
    const context = canvas.getContext('2d');

    if (!context) return;

    canvas.height = viewport.height;
    canvas.width = viewport.width;

    const renderContext = {
      canvasContext: context,
      viewport: viewport,
    };

    await page.render(renderContext).promise;
    
    currentPage.value = pageNum;
    position.value.page = pageNum; // Mantener 1-based para el frontend
    
    // Clear signature position when page changes
    signaturePosition.value = null;
    
    console.log(`Página ${pageNum} renderizada (PDF.js 1-based)`);
  } catch (error) {
    console.error('Error al renderizar página:', error);
  }
}

// Funciones para navegación de páginas
function nextPage() {
  if (pdfDoc.value && currentPage.value < pdfDoc.value.numPages) {
    renderPage(currentPage.value + 1);
  }
}

function prevPage() {
  if (currentPage.value > 1) {
    renderPage(currentPage.value - 1);
  }
}

// Función para cargar documento seleccionado
async function loadSelectedDocument() {
  if (!selectedDocument.value) {
    cleanupPDFResources();
    return;
  }

  try {
    loading.value = true;
    
    // Clean up previous resources
    cleanupPDFResources();
    
    const res = await axios.get(`/api/documents/${selectedDocument.value.id}/view`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
      responseType: 'blob'
    });
    
    const blobUrl = URL.createObjectURL(res.data);
    pdfBlobUrl.value = blobUrl;
    
    // Load PDF with PDF.js
    await loadPDF(blobUrl);
  } catch (e) {
    console.error('Error viewing document:', e);
    alert('No se pudo mostrar el documento.');
  } finally {
    loading.value = false;
  }
}

// Funciones para manejo de arrastre de firma
function handleCanvasClick(event) {
  if (!canvasRef.value) return;
  
  const canvas = canvasRef.value;
  const rect = canvas.getBoundingClientRect();
  const x = event.clientX - rect.left;
  const y = event.clientY - rect.top;
  
  // Convertir coordenadas del canvas al sistema PDF estándar
  const pdfCoords = convertCanvasToPDFCoordinates(
    x, y, 
    canvas.width, canvas.height, 
    canvas.width / scale.value, canvas.height / scale.value
  );
  
  // Calcular la caja de firma para PyHanko
  const signatureBox = calculateSignatureBox(pdfCoords.x, pdfCoords.y);
  
  // Update position values con las coordenadas convertidas
  position.value.posX = signatureBox.x1;
  position.value.posY = signatureBox.y1;
  
  // Update signature position for visual feedback
  signaturePosition.value = {
    x: x,
    y: y,
    page: currentPage.value
  };
  
  console.log('Coordenadas convertidas:', {
    canvas: { x, y },
    pdf: pdfCoords,
    signatureBox: signatureBox
  });
}

function handleSignatureMouseDown(event) {
  if (!signaturePosition.value) return;
  
  isDragging.value = true;
  const rect = event.target.getBoundingClientRect();
  dragOffset.value = {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top
  };
  
  event.preventDefault();
}

function handleSignatureMouseMove(event) {
  if (!isDragging.value || !canvasRef.value || !signaturePosition.value) return;
  
  const canvas = canvasRef.value;
  const rect = canvas.getBoundingClientRect();
  const x = event.clientX - rect.left - dragOffset.value.x;
  const y = event.clientY - rect.top - dragOffset.value.y;
  
  // Mantener la firma dentro de los límites del canvas
  const maxX = rect.width - 120; // 120px es el ancho de la firma
  const maxY = rect.height - 60; // 60px es el alto de la firma
  
  const clampedX = Math.max(0, Math.min(x, maxX));
  const clampedY = Math.max(0, Math.min(y, maxY));
  
  // Update signature position (coordenadas del canvas para la UI)
  signaturePosition.value.x = clampedX;
  signaturePosition.value.y = clampedY;
  
  // Convertir coordenadas del canvas al sistema PDF estándar
  const pdfCoords = convertCanvasToPDFCoordinates(
    clampedX, clampedY, 
    canvas.width, canvas.height, 
    canvas.width / scale.value, canvas.height / scale.value
  );
  
  // Calcular la caja de firma para PyHanko
  const signatureBox = calculateSignatureBox(pdfCoords.x, pdfCoords.y);
  
  // Update position values con las coordenadas de la caja de firma
  position.value.posX = signatureBox.x1;
  position.value.posY = signatureBox.y1;
}

function handleSignatureMouseUp() {
  isDragging.value = false;
}

function clearSignaturePosition() {
  signaturePosition.value = null;
  position.value.posX = 0;
  position.value.posY = 0;
}

onMounted(async () => {
  await fetchDocuments();
  await fetchUsers();
  setupWebSocket();
  
  // Event listeners para arrastre global
  document.addEventListener('mousemove', handleSignatureMouseMove);
  document.addEventListener('mouseup', handleSignatureMouseUp);
});

onUnmounted(() => {
  webSocketService.removeListener('signature_request_update');
  document.removeEventListener('mousemove', handleSignatureMouseMove);
  document.removeEventListener('mouseup', handleSignatureMouseUp);
  cleanupPDFResources();
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
    cleanupPDFResources();
  } catch {
    status.value = 'Error al enviar la solicitud.';
    addNotification('Error', 'No se pudo enviar la solicitud de firma', 'error');
  }
}
</script>

<template>
  <div class="space-y-8">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-3xl font-bold text-slate-900">Solicitar Firma Grupal</h2>
        <p class="text-slate-600 mt-2">Envía solicitudes de firma a múltiples usuarios</p>
      </div>
    </div>

    <div class="grid lg:grid-cols-2 gap-8">
      <!-- Panel izquierdo: Formulario -->
      <div class="space-y-6">
        <div class="border-0 shadow-sm bg-white/80 backdrop-blur-sm rounded-xl">
          <div class="p-6">
            <h3 class="text-xl font-semibold mb-4">Configuración de Solicitud</h3>
            
            <!-- Selección de documento -->
            <div class="mb-6">
              <label class="block font-medium mb-2">Selecciona un documento</label>
              <select 
                v-model="selectedDocument" 
                @change="loadSelectedDocument"
                class="w-full border border-emerald-200 focus:border-emerald-400 focus:ring-emerald-400 rounded px-3 py-2"
              >
                <option :value="null">-- Selecciona un documento --</option>
                <option v-for="doc in documents" :key="doc.id" :value="doc">{{ doc.fileName }}</option>
              </select>
            </div>

            <!-- Búsqueda de usuarios -->
            <div class="mb-6">
              <label class="block font-medium mb-2">Buscar usuario</label>
              <input 
                v-model="searchQuery" 
                @input="searchUsers" 
                placeholder="Nombre, apellido o email" 
                class="w-full border border-emerald-200 focus:border-emerald-400 focus:ring-emerald-400 rounded px-3 py-2 mb-2" 
              />
              <div v-if="userResults.length" class="bg-slate-50 border rounded p-2 max-h-32 overflow-y-auto">
                <div v-for="user in userResults" :key="user.id" class="flex justify-between items-center py-1">
                  <span>{{ user.firstName }} {{ user.lastName }} ({{ user.email }})</span>
                  <button @click="addUser(user)" class="text-emerald-600 hover:underline">Agregar</button>
                </div>
              </div>
            </div>

            <!-- Posición de firma (ahora visual) -->
            <div class="mb-6">
              <label class="block font-medium mb-2">Posición de la firma</label>
              <div class="p-3 bg-blue-50 border border-blue-200 rounded-lg">
                <p class="text-sm text-blue-800 mb-2">
                  <strong>💡 Instrucciones:</strong> Haz clic en el documento para colocar la firma, luego arrastra la imagen para ajustar la posición.
                </p>
                <div class="flex space-x-2">
                  <button 
                    @click="clearSignaturePosition" 
                    class="px-3 py-1 bg-slate-500 text-white rounded hover:bg-slate-600 text-sm"
                  >
                    Limpiar Posición
                  </button>
                </div>
              </div>
            </div>

            <!-- Usuarios seleccionados -->
            <div class="mb-6">
              <h4 class="font-medium mb-2">Usuarios seleccionados</h4>
              <div v-if="selectedUsers.length === 0" class="text-slate-500 text-sm">No hay usuarios agregados.</div>
              <div v-else class="space-y-2">
                <div v-for="u in selectedUsers" :key="u.user.id" class="flex items-center justify-between p-2 bg-slate-50 rounded">
                  <div>
                    <span class="font-medium">{{ u.user.firstName }} {{ u.user.lastName }}</span>
                    <div class="text-xs text-slate-600">{{ u.user.email }}</div>
                    <div class="text-xs text-slate-500">
                      Página: {{ u.page }} | Posición: ({{ u.originalPosX }}, {{ u.originalPosY }})
                    </div>
                  </div>
                  <button @click="removeUser(u.user.id)" class="text-red-500 hover:underline text-sm">Quitar</button>
                </div>
              </div>
            </div>

            <!-- Botón de envío -->
            <button 
              @click="submitRequest" 
              class="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg py-3"
            >
              Solicitar Firma
            </button>
            
            <p v-if="status" class="mt-2 text-center" :class="status.includes('correctamente') ? 'text-green-600' : 'text-red-600'">{{ status }}</p>
          </div>
        </div>
      </div>

      <!-- Panel derecho: Visualizador de documento -->
      <div class="space-y-6">
        <div class="border-0 shadow-sm bg-white/80 backdrop-blur-sm rounded-xl">
          <div class="p-6">
            <h3 class="text-xl font-semibold mb-4">Previsualización del Documento</h3>
            
            <div v-if="!selectedDocument" class="flex flex-col items-center justify-center py-20">
              <div class="w-24 h-24 bg-slate-100 rounded-full flex items-center justify-center mb-6">
                <svg class="w-12 h-12 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                </svg>
              </div>
              <h4 class="text-lg font-semibold text-slate-900 mb-2">Selecciona un documento</h4>
              <p class="text-slate-600 text-center">Elige un documento del panel izquierdo para previsualizarlo y seleccionar la posición de la firma.</p>
            </div>

                         <!-- Visualizador de PDF -->
             <div v-if="pdfBlobUrl" class="border rounded overflow-hidden p-4 mt-4">
               <!-- PDF Viewer Container -->
               <div class="flex justify-center">
                 <div class="relative" style="cursor: crosshair;">
                   <!-- Loading indicator -->
                   <div v-if="loading" class="absolute inset-0 flex items-center justify-center bg-white bg-opacity-75 z-10">
                     <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600"></div>
                   </div>
                   
                   <!-- PDF Canvas -->
                   <canvas 
                     ref="canvasRef" 
                     @click="handleCanvasClick"
                     class="border border-slate-200 shadow-sm"
                   ></canvas>
                   
                   <!-- Información de páginas -->
                   <div v-if="pdfDoc" class="mt-2 text-center text-sm text-slate-600">
                     Página {{ currentPage }} de {{ pdfDoc.numPages }}
                   </div>
                   
                   <!-- Controles de navegación -->
                   <div v-if="pdfDoc && pdfDoc.numPages > 1" class="mt-4 flex justify-center items-center space-x-4">
                     <button 
                       @click="prevPage" 
                       :disabled="currentPage <= 1"
                       class="px-4 py-2 bg-slate-600 text-white rounded hover:bg-slate-700 disabled:bg-slate-400 disabled:cursor-not-allowed flex items-center"
                     >
                       <svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                         <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
                       </svg>
                       Anterior
                     </button>
                     
                     <!-- Selector de página -->
                     <div class="flex items-center space-x-2">
                       <span class="text-sm text-slate-600">Ir a:</span>
                       <select 
                         :value="currentPage" 
                         @change="(e) => renderPage(Number(e.target.value))"
                         class="px-3 py-1 border border-slate-300 rounded text-sm"
                       >
                         <option v-for="page in pdfDoc.numPages" :key="page" :value="page">
                           Página {{ page }}
                         </option>
                       </select>
                     </div>
                     
                     <button 
                       @click="nextPage" 
                       :disabled="currentPage >= pdfDoc.numPages"
                       class="px-4 py-2 bg-slate-600 text-white rounded hover:bg-slate-700 disabled:bg-slate-400 disabled:cursor-not-allowed flex items-center"
                     >
                       Siguiente
                       <svg class="w-4 h-4 ml-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                         <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                       </svg>
                     </button>
                   </div>
                   
                   <!-- Signature Preview - Arrastrable -->
                   <img 
                     v-if="signaturePosition" 
                     :src="firmaImg" 
                     :style="{ 
                       position: 'absolute', 
                       left: (signaturePosition.x - 60) + 'px', 
                       top: (signaturePosition.y - 30) + 'px', 
                       width: '120px', 
                       height: '60px', 
                       zIndex: 10,
                       cursor: isDragging ? 'grabbing' : 'grab',
                       userSelect: 'none',
                       pointerEvents: 'auto'
                     }" 
                     @mousedown="handleSignatureMouseDown"
                     alt="Firma" 
                     draggable="false"
                   />
                 </div>
               </div>
             </div>
          </div>
        </div>
      </div>
    </div>
    
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
