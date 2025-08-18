<script setup>
import { ref, onMounted, onUnmounted, computed, shallowRef } from 'vue';
import { getAllSignatureRequests, getUsersBySignatureRequest, responderSolicitudFirma, getCertificatesByUser } from '../utils/api';
import { authState } from '../service/Auth';
import { webSocketService } from '../service/WebSocketService';
import { validatePassword } from '../utils/password-validation';
import firmaImg from '../assets/firma.png';
import * as pdfjsLib from 'pdfjs-dist/legacy/build/pdf';

// Configurar el worker correctamente
pdfjsLib.GlobalWorkerOptions.workerSrc = new URL(
  'pdfjs-dist/legacy/build/pdf.worker.js',
  import.meta.url
).toString();

const requests = ref([]);
const loading = ref(false);
const selectedRequest = ref(null);
const userEntry = ref(null);
const certificateId = ref('');
const certificatePassword = ref('');
const status = ref('');
const certificates = ref([]);

// Variables para el visualizador de PDF
const canvasRef = ref(null);
const pdfDoc = shallowRef(null);
const currentPage = ref(1);
const scale = ref(1.5);
const pdfLoading = ref(false);
const pdfBlobUrl = ref(null);

// Asegurar que currentPage siempre sea válido
const getCurrentPage = () => Math.max(1, currentPage.value);

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

// Función para normalizar rutas de manera compatible con Windows y Ubuntu
function normalizePath(path) {
  // Reemplazar backslashes de Windows con forward slashes
  return path.replace(/\\/g, '/');
}

// Función para extraer el ID del documento de diferentes formatos de ruta
// Compatible con Windows (backslashes) y Ubuntu (forward slashes)
function extractDocumentId(documentPath) {
  const normalizedPath = normalizePath(documentPath);
  
  console.log('=== ANÁLISIS DE RUTA ===');
  console.log('Ruta original:', documentPath);
  console.log('Ruta normalizada:', normalizedPath);
  
  // Dividir la ruta en segmentos para análisis detallado
  const pathSegments = normalizedPath.split('/');
  console.log('Segmentos de la ruta:', pathSegments);
  
  // Buscar patrones específicos en la ruta
  const documentsIndex = pathSegments.findIndex(segment => segment === 'documents');
  const uploadsIndex = pathSegments.findIndex(segment => segment === 'uploads');
  
  console.log('Índice de "documents":', documentsIndex);
  console.log('Índice de "uploads":', uploadsIndex);
  
  // Patrón 1: .../documents/ID/document.pdf
  if (documentsIndex !== -1 && documentsIndex + 2 < pathSegments.length) {
    const id = pathSegments[documentsIndex + 1];
    const fileName = pathSegments[documentsIndex + 2];
    console.log('Patrón 1 detectado:');
    console.log('  - Segmento documents en posición:', documentsIndex);
    console.log('  - ID extraído:', id);
    console.log('  - Nombre del archivo:', fileName);
    
    // Verificar que el ID sea válido (no debe ser el nombre del archivo)
    if (id && id !== fileName && !id.includes('.')) {
      console.log('✅ ID válido extraído del patrón 1:', id);
      return id;
    } else {
      console.log('❌ ID inválido del patrón 1:', id);
    }
  }
  
  // Patrón 2: .../uploads/documents/ID/document.pdf
  if (uploadsIndex !== -1 && uploadsIndex + 3 < pathSegments.length) {
    if (pathSegments[uploadsIndex + 1] === 'documents') {
      const id = pathSegments[uploadsIndex + 2];
      const fileName = pathSegments[uploadsIndex + 3];
      console.log('Patrón 2 detectado:');
      console.log('  - Segmento uploads en posición:', uploadsIndex);
      console.log('  - Segmento documents en posición:', uploadsIndex + 1);
      console.log('  - ID extraído:', id);
      console.log('  - Nombre del archivo:', fileName);
      
      // Verificar que el ID sea válido
      if (id && id !== fileName && !id.includes('.')) {
        console.log('✅ ID válido extraído del patrón 2:', id);
        return id;
      } else {
        console.log('❌ ID inválido del patrón 2:', id);
      }
    }
  }
  
  // Patrón 3: Buscar un segmento que parezca un ID (números, letras, guiones, sin extensión)
  for (let i = 0; i < pathSegments.length; i++) {
    const segment = pathSegments[i];
    // Un ID válido no debe tener extensión y debe ser alfanumérico
    if (segment && !segment.includes('.') && /^[a-zA-Z0-9_-]+$/.test(segment)) {
      // Verificar que no sea un nombre de carpeta común
      const commonFolders = ['documents', 'uploads', 'files', 'tmp', 'temp'];
      if (!commonFolders.includes(segment.toLowerCase())) {
        console.log('Patrón 3 detectado - ID candidato:', segment, 'en posición:', i);
        console.log('✅ ID válido extraído del patrón 3:', segment);
        return segment;
      }
    }
  }
  
  // Patrón 4: Si no se encuentra nada, mostrar error detallado
  console.log('❌ No se pudo extraer ID de ningún patrón conocido');
  console.log('Segmentos analizados:', pathSegments);
  console.log('=== FIN ANÁLISIS ===');
  return null;
}

// Cleanup function para recursos del PDF
function cleanupPDFResources() {
  if (pdfBlobUrl.value) {
    URL.revokeObjectURL(pdfBlobUrl.value);
    pdfBlobUrl.value = null;
  }
  pdfDoc.value = null;
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

// Función para cargar el documento de la solicitud
// Esta función extrae el ID del documento desde diferentes formatos de ruta:
// - Windows: C:\Users\...\uploads\documents\ID\document.pdf
// - Ubuntu: /home/user/.../uploads/documents/ID/document.pdf
// - Relativa: documents/ID/document.pdf
async function loadRequestDocument() {
  if (!selectedRequest.value) {
    cleanupPDFResources();
    return;
  }

  try {
    pdfLoading.value = true;
    
    // Clean up previous resources
    cleanupPDFResources();
    
    // Obtener la ruta del documento
    const documentPath = selectedRequest.value.documentPath;
    console.log('Ruta del documento:', documentPath);
    
    // Intentar cargar el documento con diferentes estrategias
    let res = null;
    let success = false;
    let documentId = null;
    
    console.log('Ruta original:', documentPath);
    console.log('Ruta normalizada:', normalizePath(documentPath));
    
    // Estrategia 1: Intentar buscar por ruta del archivo directamente
    try {
      console.log(`🔄 Estrategia 1: Buscando documento por ruta`);
      res = await fetch(`/api/documents/search?filepath=${encodeURIComponent(documentPath)}`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      
      if (res.ok) {
        const searchResult = await res.json();
        if (searchResult.id) {
          console.log('✅ Estrategia 1 exitosa, documento encontrado:', searchResult.id);
          documentId = searchResult.id;
          
          // Ahora intentar cargar con el ID real
          res = await fetch(`/api/documents/${documentId}/view`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
          });
          success = res.ok;
        }
      } else {
        console.log(`❌ Estrategia 1 falló: ${res.status} ${res.statusText}`);
      }
    } catch (fetchError) {
      console.error('❌ Error en estrategia 1:', fetchError);
    }
    
    // Estrategia 2: Si falló, intentar buscar por nombre del archivo como fallback
    if (!success) {
      try {
        const fileName = documentPath.split(/[\\/]/).pop();
        console.log(`🔄 Estrategia 2: Intentando buscar por nombre de archivo "${fileName}"`);
        
        // Aquí podrías implementar una búsqueda por nombre si es necesario
        console.log('⚠️ Estrategia 2 no implementada completamente');
      } catch (searchError) {
        console.error('❌ Error en estrategia 2:', searchError);
      }
    }
    
    // Si ambas estrategias fallaron, mostrar error detallado
    if (!success) {
      const errorText = res ? await res.text() : 'Sin respuesta del servidor';
      console.error('Respuesta del servidor:', res?.status, res?.statusText, errorText);
      
      if (res?.status === 403) {
        throw new Error(`Acceso denegado (403). No tienes permisos para acceder al documento. Verifica que el documento esté registrado en el sistema.`);
      } else if (res?.status === 404) {
        throw new Error(`Documento no encontrado (404). La ruta del documento "${documentPath}" no existe en el sistema. Verifica que el documento haya sido subido correctamente.`);
      } else {
        throw new Error(`Error del servidor (${res?.status || 'N/A'}): ${res?.statusText || 'Sin respuesta'}. Detalles: ${errorText}`);
      }
    }
    
    const blob = await res.blob();
    const blobUrl = URL.createObjectURL(blob);
    pdfBlobUrl.value = blobUrl;
    
    // Load PDF with PDF.js
    await loadPDF(blobUrl);
    
    // Ir a la página donde está la firma
    if (userEntry.value && userEntry.value.page) {
      renderPage(userEntry.value.page);
    }
  } catch (e) {
    console.error('Error viewing document:', e);
    alert('No se pudo mostrar el documento: ' + e.message);
  } finally {
    pdfLoading.value = false;
  }
}

onMounted(async () => {
  await fetchRequests();
  await fetchCertificates();
  setupWebSocket();
});

onUnmounted(() => {
  // Limpiar listeners de WebSocket
  webSocketService.removeListener('signature_request');
  webSocketService.removeListener('signature_request_update');
  
  // Limpiar recursos del PDF
  cleanupPDFResources();
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
  
  // Debug: Mostrar información de la solicitud
  console.log('=== SOLICITUD ABIERTA ===');
  console.log('Solicitud completa:', request);
  console.log('documentPath:', request.documentPath);
  console.log('userEntry:', userEntry.value);
  console.log('=== FIN SOLICITUD ===');
  
  // Cargar el documento automáticamente
  loadRequestDocument();
}

function closeModal() {
  selectedRequest.value = null;
  cleanupPDFResources();
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
    
    closeModal();
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
      <div class="bg-white rounded-xl shadow-lg p-8 w-full max-w-6xl relative max-h-[90vh] overflow-y-auto">
        <button class="absolute top-2 right-2 text-slate-400 hover:text-slate-600" @click="closeModal">&times;</button>
        <h3 class="text-lg font-bold mb-2">Responder Solicitud</h3>
        <div class="mb-2"><span class="font-medium">Documento:</span> {{ selectedRequest.documentPath.split('/').pop() }}</div>
        <div class="mb-2"><span class="font-medium">Página:</span> {{ userEntry.page }}, <span class="font-medium">X:</span> {{ userEntry.posX }}, <span class="font-medium">Y:</span> {{ userEntry.posY }}</div>
        
        <!-- Visualizador de PDF -->
        <div class="mb-6">
          <h4 class="font-medium mb-3">Vista previa del documento</h4>
          <p class="text-sm text-slate-600 mb-3">La posición de la firma se muestra en la página {{ userEntry.page }}. Puedes navegar por el documento usando los controles de abajo.</p>
          <div v-if="!pdfBlobUrl" class="flex flex-col items-center justify-center py-8 border-2 border-dashed border-slate-300 rounded-lg">
            <div class="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center mb-4">
              <svg class="w-8 h-8 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
            </div>
            <p class="text-slate-600 text-center">Cargando documento...</p>
          </div>
          
          <!-- Visualizador de PDF -->
          <div v-if="pdfBlobUrl" class="border rounded overflow-hidden">
            <!-- PDF Viewer Container -->
            <div class="flex justify-center">
              <div class="relative">
                <!-- Loading indicator -->
                <div v-if="pdfLoading" class="absolute inset-0 flex items-center justify-center bg-white bg-opacity-75 z-10">
                  <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600"></div>
                </div>
                
                <!-- PDF Canvas -->
                <canvas 
                  ref="canvasRef" 
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
                    <button 
                      v-if="userEntry && userEntry.page !== currentPage"
                      @click="renderPage(userEntry.page)" 
                      class="px-3 py-1 bg-emerald-600 text-white rounded text-sm hover:bg-emerald-700"
                    >
                      Ir a firma
                    </button>
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
                
                <!-- Signature Preview - Solo visualización -->
                <img 
                  v-if="userEntry && userEntry.page === currentPage" 
                  :src="firmaImg" 
                  :style="{ 
                    position: 'absolute', 
                    left: (userEntry.posX * scale - 60) + 'px', 
                    top: (userEntry.posY * scale - 30) + 'px', 
                    width: '120px', 
                    height: '60px', 
                    zIndex: 10,
                    pointerEvents: 'none'
                  }" 
                  alt="Posición de la firma" 
                  draggable="false"
                />
              </div>
            </div>
          </div>
        </div>
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
