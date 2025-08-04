<script setup lang="ts">
import { ref, shallowRef, onMounted } from 'vue';
import axios from 'axios';
import { getDocumentsByUser, uploadDocument, getCertificatesByUser } from '../utils/api';
import firmaImg from '../assets/firma.png';
import * as pdfjsLib from 'pdfjs-dist/legacy/build/pdf';

// Configurar el worker correctamente
pdfjsLib.GlobalWorkerOptions.workerSrc = new URL(
  'pdfjs-dist/legacy/build/pdf.worker.js',
  import.meta.url
).toString();

// Tipos para TypeScript
interface Document {
  id: string;
  fileName: string;
  senderName?: string;
  receivedAt?: string;
  dueDate?: string;
  signed: boolean;
  updatedAt?: string;
}

interface Certificate {
  id: string;
  filename: string;
  createdAt: string;
}

interface SignaturePosition {
  x: number;
  y: number;
  page: number;
}

const docsToSign = ref<Document[]>([]);
const pdfBlobUrl = ref<string | null>(null);
const signForm = ref({
  documentId: null as string | null,
  certificateId: null as string | null,
  certPassword: '',
  page: 1,
  x: 100,
  y: 100,
});
const signStatus = ref("");
const certificates = ref<Certificate[]>([]);
const allDocuments = ref<Document[]>([]);
const recentlySigned = ref<Document[]>([]);
const fileInput = ref<HTMLInputElement | null>(null);

// PDF.js variables - usando shallowRef para el documento PDF
const canvasRef = ref<HTMLCanvasElement | null>(null);
const pdfDoc = shallowRef<pdfjsLib.PDFDocumentProxy | null>(null);
const currentPage = ref(1);
const scale = ref(1.5);
const loading = ref(false);
const signaturePosition = ref<SignaturePosition | null>(null);

// Asegurar que currentPage siempre sea válido
const getCurrentPage = () => Math.max(1, currentPage.value);

// Convertir de 1-based (frontend) a 0-based (backend)
const convertToBackendPage = (page: number) => page - 1;

// Convertir coordenadas del canvas al sistema PDF estándar
const convertCanvasToPDFCoordinates = (canvasX: number, canvasY: number, canvasWidth: number, canvasHeight: number, pageWidth: number, pageHeight: number) => {
  // Convertir coordenadas del canvas (0,0 en esquina superior izquierda, Y hacia abajo)
  // al sistema PDF (0,0 en esquina inferior izquierda, Y hacia arriba)
  
  // X: se mantiene igual (crece hacia la derecha en ambos sistemas)
  const pdfX = (canvasX / canvasWidth) * pageWidth;
  
  // Y: invertir el eje Y (canvas Y hacia abajo -> PDF Y hacia arriba)
  const pdfY = pageHeight - ((canvasY / canvasHeight) * pageHeight);
  
  return { x: Math.round(pdfX), y: Math.round(pdfY) };
};

// Calcular las coordenadas de la caja de firma para PyHanko
const calculateSignatureBox = (centerX: number, centerY: number, signatureWidth: number = 120, signatureHeight: number = 60) => {
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

// Variables para arrastre de firma
const isDragging = ref(false);
const dragOffset = ref({ x: 0, y: 0 });

console.log('PDF.js configured with local worker');

function getToken() {
  return localStorage.getItem('token');
}
async function fetchDocsToSign() {
  try {
    const res = await axios.get('/api/documents/list', {
      headers: { Authorization: `Bearer ${getToken()}` }
    });
    docsToSign.value = res.data.filter((doc: Document) => !doc.signed);
    allDocuments.value = res.data;
    recentlySigned.value = res.data.filter((doc: Document) => doc.signed).slice(-5).reverse();
  } catch (e) {
    console.error('Error cargando docs para firmar:', e);
  }
}
async function fetchCertificates() {
  const userStr = localStorage.getItem('user');
  if (!userStr) return;
  const user = JSON.parse(userStr);
  if (!user?.email) return;
  try {
    const res = await getCertificatesByUser(user.email);
    certificates.value = res.data;
  } catch {}
}
// Limpiar event listeners al desmontar
import { onUnmounted } from 'vue';

onUnmounted(() => {
  document.removeEventListener('mousemove', handleSignatureMouseMove);
  document.removeEventListener('mouseup', handleSignatureMouseUp);
});



// Cleanup function
function cleanupPDFResources() {
  if (pdfBlobUrl.value) {
    URL.revokeObjectURL(pdfBlobUrl.value);
    pdfBlobUrl.value = null;
  }
  pdfDoc.value = null;
  signaturePosition.value = null;
}
async function viewDocument(id: string) {
  try {
    loading.value = true;
    
    // Clean up previous resources
    cleanupPDFResources();
    
    const res = await axios.get(`/api/documents/${id}/view`, {
      headers: { Authorization: `Bearer ${getToken()}` },
      responseType: 'blob'
    });
    
    const blobUrl = URL.createObjectURL(res.data);
    pdfBlobUrl.value = blobUrl;
    signForm.value.documentId = id;
    
    // Load PDF with PDF.js
    await loadPDF(blobUrl);
  } catch (e) {
    console.error('Error viewing document:', e);
    alert('No se pudo mostrar el documento.');
  } finally {
    loading.value = false;
  }
}

// Cargar y renderizar el PDF
async function loadPDF(url: string) {
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
async function renderPage(pageNum: number) {
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
    signForm.value.page = pageNum; // Mantener 1-based para el frontend
    
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

// Funciones para manejo de arrastre de firma
function handleCanvasClick(event: MouseEvent) {
  if (!canvasRef.value) return;
  
  const canvas = canvasRef.value;
  const rect = canvas.getBoundingClientRect();
  const x = event.clientX - rect.left;
  const y = event.clientY - rect.top;
  
  // Convert canvas coordinates to PDF coordinates (simplificado)
  const pdfX = x / scale.value;
  const pdfY = y / scale.value;
  
  // Convertir coordenadas del canvas al sistema PDF estándar
  const pdfCoords = convertCanvasToPDFCoordinates(
    x, y, 
    canvas.width, canvas.height, 
    canvas.width / scale.value, canvas.height / scale.value
  );
  
  // Calcular la caja de firma para PyHanko
  const signatureBox = calculateSignatureBox(pdfCoords.x, pdfCoords.y);
  
  // Update form values con las coordenadas convertidas
  signForm.value.x = signatureBox.x1;
  signForm.value.y = signatureBox.y1;
  
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

function handleSignatureMouseDown(event: MouseEvent) {
  if (!signaturePosition.value) return;
  
  isDragging.value = true;
  const rect = (event.target as HTMLElement).getBoundingClientRect();
  dragOffset.value = {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top
  };
  
  event.preventDefault();
}

function handleSignatureMouseMove(event: MouseEvent) {
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
  
  // Update form values con las coordenadas de la caja de firma
  signForm.value.x = signatureBox.x1;
  signForm.value.y = signatureBox.y1;
}

function handleSignatureMouseUp() {
  isDragging.value = false;
}

// Agregar event listeners globales para el arrastre
onMounted(() => {
  fetchDocsToSign();
  fetchCertificates();
  
  // Event listeners para arrastre global
  document.addEventListener('mousemove', handleSignatureMouseMove);
  document.addEventListener('mouseup', handleSignatureMouseUp);
});

function clearSignaturePosition() {
  signaturePosition.value = null;
  signForm.value.x = 0;
  signForm.value.y = 0;
}


async function handleSignDocument() {
  signStatus.value = '';
  if (!signForm.value.documentId || !signForm.value.certificateId || !signForm.value.certPassword) {
    signStatus.value = 'Completa todos los campos';
    return;
  }
  
  if (!signaturePosition.value) {
    signStatus.value = 'Debes seleccionar una posición para la firma';
    return;
  }
  
  try {
    // Convertir página de 1-based (frontend) a 0-based (backend)
    const backendPage = convertToBackendPage(signForm.value.page);
    
    // Las coordenadas ya están en el formato correcto para PyHanko (x1, y1 de la caja de firma)
    const params = new URLSearchParams({
      page: backendPage.toString(),
      x: signForm.value.x.toString(),
      y: signForm.value.y.toString(),
      certificateId: signForm.value.certificateId,
      certPassword: signForm.value.certPassword,
    });
    
    console.log('Enviando coordenadas al backend:', {
      page: backendPage,
      x: signForm.value.x,
      y: signForm.value.y,
      params: params.toString()
    });
    
    const res = await axios.post(`/api/documents/sign/${signForm.value.documentId}?${params.toString()}`, {}, {
      headers: { Authorization: `Bearer ${getToken()}` }
    });
    signStatus.value = res.data || 'Documento firmado correctamente';
    await fetchDocsToSign();
  } catch (e: any) {
    signStatus.value = e.response?.data || 'Error al firmar documento';
  }
}
// Remove the old handlePdfClick function as it's replaced by handleCanvasClick
function triggerFileInput() {
  fileInput.value?.click();
}
async function handleFileChange(e: Event) {
  const target = e.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;
  try {
    await uploadDocument(file);
    await fetchDocsToSign();
    alert('Documento subido exitosamente');
  } catch {
    alert('Error al subir el documento');
  }
}
function goToRequestSignature() {
  // Emitir evento para cambiar sección o usar router si aplica
  // Aquí emitimos un evento para el Dashboard
  window.dispatchEvent(new CustomEvent('go-to-request-signature'));
}

// Función para verificar si un documento está firmado
function isDocumentSigned(documentId: string): boolean {
  const doc = allDocuments.value.find((d: Document) => d.id === documentId);
  return doc ? doc.signed : false;
}
</script>
<template>
  <div class="space-y-8">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-3xl font-bold text-slate-900">Firmar Documentos</h2>
        <p class="text-slate-600 mt-2">Gestiona y firma tus documentos pendientes</p>
      </div>
      <div class="flex space-x-2">
        <button class="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded flex items-center font-medium shadow" @click="goToRequestSignature">
          <svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 8v6M8 16h8M8 8h8"/></svg>
          Nueva Firma
        </button>
        <button class="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded flex items-center font-medium shadow" @click="triggerFileInput">
          <svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
          Subir Documento
        </button>
        <input ref="fileInput" type="file" accept=".pdf" class="hidden" @change="handleFileChange" />
      </div>
    </div>
         <!-- Main Content -->
     <div v-if="docsToSign.length === 0">
       <div class="border-0 shadow-sm bg-white/80 backdrop-blur-sm rounded-xl">
         <div class="flex flex-col items-center justify-center py-20">
           <div class="w-24 h-24 bg-emerald-100 rounded-full flex items-center justify-center mb-6">
             <svg class="w-12 h-12 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 8v6M8 16h8M8 8h8"/></svg>
           </div>
           <h3 class="text-2xl font-semibold text-slate-900 mb-3">¡Excelente trabajo!</h3>
           <p class="text-slate-600 text-center max-w-md mb-8">
             No tienes documentos pendientes de firma. Todos tus documentos están al día.
           </p>
         </div>
       </div>
     </div>
     
     <!-- Mostrar documentos pendientes -->
     <div v-if="docsToSign.length > 0" class="space-y-4">
      <div v-for="doc in docsToSign" :key="doc.id" class="border-0 shadow-sm bg-white/80 backdrop-blur-sm rounded-xl hover:shadow-md transition-all duration-200">
        <div class="p-6">
          <div class="flex items-start justify-between">
            <div class="flex items-start space-x-4">
              <div class="w-12 h-12 bg-orange-100 rounded-xl flex items-center justify-center">
                <svg class="w-6 h-6 text-orange-600" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
              </div>
              <div>
                <h4 class="text-lg font-semibold text-slate-900">{{ doc.fileName }}</h4>
                <p class="text-slate-600">Enviado por {{ doc.senderName || 'Desconocido' }}</p>
                <div class="flex items-center space-x-4 mt-2 text-sm text-slate-500">
                  <span>Recibido: {{ doc.receivedAt ? new Date(doc.receivedAt).toLocaleDateString() : '-' }}</span>
                  <span>•</span>
                  <span>Vence: {{ doc.dueDate ? new Date(doc.dueDate).toLocaleDateString() : '-' }}</span>
                </div>
              </div>
            </div>
            <div class="flex items-center space-x-3">
              <span class="bg-orange-100 text-orange-700 px-2 py-1 rounded text-xs font-medium flex items-center">
                <svg class="w-3 h-3 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor"><circle cx="12" cy="12" r="10"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3"/></svg>
                Urgente
              </span>
              <div class="flex space-x-2">
                <button @click="viewDocument(doc.id)" class="border border-emerald-200 hover:bg-emerald-50 bg-transparent text-emerald-700 px-3 py-1 rounded text-sm font-medium">Previsualizar</button>
                <button @click="() => { signForm.documentId = doc.id; }" class="bg-emerald-600 hover:bg-emerald-700 text-white px-3 py-1 rounded text-sm font-medium flex items-center">
                  <svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 8v6M8 16h8M8 8h8"/></svg>
                  Firmar Ahora
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <!-- Formulario de firma y previsualización -->
      <div v-if="signForm.documentId || pdfBlobUrl" class="border-0 shadow-sm bg-white/80 backdrop-blur-sm rounded-xl">
        <div class="p-6">
                     <!-- Formulario de firma solo si el documento no está firmado -->
           <form v-if="signForm.documentId && !isDocumentSigned(signForm.documentId)" @submit.prevent="handleSignDocument" class="space-y-4">
            <label class="block font-medium">Selecciona certificado:</label>
            <select v-model="signForm.certificateId" class="block w-full border rounded p-2">
              <option disabled value="">Selecciona un certificado</option>
              <option v-for="cert in certificates" :key="cert.id" :value="cert.id">
                {{ cert.filename }} ({{ new Date(cert.createdAt).toLocaleString() }})
              </option>
            </select>
                         <input v-model="signForm.certPassword" type="password" placeholder="Contraseña del certificado" class="block w-full border rounded p-2" required />
                          <div class="flex space-x-2">
               <button 
                 type="button" 
                 @click="clearSignaturePosition" 
                 class="w-full px-3 py-2 bg-slate-500 text-white rounded hover:bg-slate-600"
               >
                 Limpiar Posición
               </button>
             </div>
                                                  <button type="submit" class="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded">Firmar Documento</button>
           </form>
           
                       <!-- Mensaje de estado y instrucciones fuera del formulario -->
            <p v-if="signStatus" :class="signStatus.includes('correctamente') ? 'text-green-600' : 'text-red-600'">{{ signStatus }}</p>
            

           
                       <!-- Instrucciones para el usuario -->
            <div v-if="signaturePosition && !isDocumentSigned(signForm.documentId || '')" class="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
              <p class="text-sm text-blue-800">
                <strong>💡 Instrucciones:</strong> Haz clic en el PDF para colocar la firma, luego arrastra la imagen para ajustar la posición.
              </p>
            </div>
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
                        @change="(e) => renderPage(Number((e.target as HTMLSelectElement).value))"
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
     
     <!-- Mostrar documentos firmados siempre que existan -->
     <div v-if="recentlySigned.length > 0" class="space-y-4">
       <div v-for="doc in recentlySigned" :key="doc.id" class="border-0 shadow-sm bg-white/80 backdrop-blur-sm rounded-xl hover:shadow-md transition-all duration-200">
         <div class="p-6">
           <div class="flex items-start justify-between">
             <div class="flex items-start space-x-4">
               <div class="w-12 h-12 bg-emerald-100 rounded-xl flex items-center justify-center">
                 <svg class="w-6 h-6 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor"><circle cx="12" cy="12" r="10"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4"/></svg>
               </div>
               <div>
                 <h4 class="text-lg font-semibold text-slate-900">{{ doc.fileName }}</h4>
                 <p class="text-slate-600">Enviado por {{ doc.senderName || 'Desconocido' }}</p>
                 <div class="flex items-center space-x-4 mt-2 text-sm text-slate-500">
                   <span>Recibido: {{ doc.receivedAt ? new Date(doc.receivedAt).toLocaleDateString() : '-' }}</span>
                   <span>•</span>
                   <span>Firmado: {{ doc.updatedAt ? new Date(doc.updatedAt).toLocaleDateString() : '-' }}</span>
                 </div>
               </div>
             </div>
             <div class="flex items-center space-x-3">
               <span class="bg-emerald-100 text-emerald-700 px-2 py-1 rounded text-xs font-medium flex items-center">
                 <svg class="w-3 h-3 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor"><circle cx="12" cy="12" r="10"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4"/></svg>
                 Firmado
               </span>
               <div class="flex space-x-2">
                 <button @click="viewDocument(doc.id)" class="border border-emerald-200 hover:bg-emerald-50 bg-transparent text-emerald-700 px-3 py-1 rounded text-sm font-medium">Ver Documento</button>
               </div>
             </div>
           </div>
         </div>
       </div>
     </div>
     
   </div>
</template> 