<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { getDocumentsByUser, uploadDocument, getCertificatesByUser } from '../utils/api';
import firmaImg from '../assets/firma.png';

const docsToSign = ref([]);
const pdfBlobUrl = ref(null);
const signForm = ref({
  documentId: null,
  certificateId: null,
  certPassword: '',
  page: 0,
  x: 100,
  y: 100,
});
const signStatus = ref("");
const certificates = ref([]);
const allDocuments = ref([]);
const recentlySigned = ref([]);
const fileInput = ref(null);

function getToken() {
  return localStorage.getItem('token');
}
async function fetchDocsToSign() {
  try {
    const res = await axios.get('/api/documents/list', {
      headers: { Authorization: `Bearer ${getToken()}` }
    });
    docsToSign.value = res.data.filter(doc => !doc.signed);
    allDocuments.value = res.data;
    recentlySigned.value = res.data.filter(doc => doc.signed).slice(-5).reverse();
  } catch (e) {
    console.error('Error cargando docs para firmar:', e);
  }
}
async function fetchCertificates() {
  const user = JSON.parse(localStorage.getItem('user'));
  if (!user?.email) return;
  try {
    const res = await getCertificatesByUser(user.email);
    certificates.value = res.data;
  } catch {}
}
onMounted(() => {
  fetchDocsToSign();
  fetchCertificates();
});
async function viewDocument(id) {
  try {
    if (pdfBlobUrl.value) {
      URL.revokeObjectURL(pdfBlobUrl.value);
      pdfBlobUrl.value = null;
    }
    const res = await axios.get(`/api/documents/${id}/view`, {
      headers: { Authorization: `Bearer ${getToken()}` },
      responseType: 'blob'
    });
    pdfBlobUrl.value = URL.createObjectURL(res.data);
    signForm.value.documentId = id;
  } catch (e) {
    alert('No se pudo mostrar el documento.');
  }
}
async function handleSignDocument() {
  signStatus.value = '';
  if (!signForm.value.documentId || !signForm.value.certificateId || !signForm.value.certPassword) {
    signStatus.value = 'Completa todos los campos';
    return;
  }
  try {
    const params = new URLSearchParams({
      page: signForm.value.page,
      x: signForm.value.x,
      y: signForm.value.y,
      certificateId: signForm.value.certificateId,
      certPassword: signForm.value.certPassword,
    });
    const res = await axios.post(`/api/documents/sign/${signForm.value.documentId}?${params.toString()}`, {}, {
      headers: { Authorization: `Bearer ${getToken()}` }
    });
    signStatus.value = res.data || 'Documento firmado correctamente';
    await fetchDocsToSign();
  } catch (e) {
    signStatus.value = e.response?.data || 'Error al firmar documento';
  }
}
function handlePdfClick(e) {
  const rect = e.currentTarget.getBoundingClientRect();
  const x = e.clientX - rect.left;
  const y = e.clientY - rect.top;
  signForm.value.x = Math.round(x);
  signForm.value.y = Math.round(y);
}
function triggerFileInput() {
  fileInput.value.click();
}
async function handleFileChange(e) {
  const file = e.target.files[0];
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
    <div v-else class="space-y-4">
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
      <div v-if="signForm.documentId" class="border-0 shadow-sm bg-white/80 backdrop-blur-sm rounded-xl">
        <div class="p-6">
          <form @submit.prevent="handleSignDocument" class="space-y-4">
            <label class="block font-medium">Selecciona certificado:</label>
            <select v-model="signForm.certificateId" class="block w-full border rounded p-2">
              <option disabled value="">Selecciona un certificado</option>
              <option v-for="cert in certificates" :key="cert.id" :value="cert.id">
                {{ cert.filename }} ({{ new Date(cert.createdAt).toLocaleString() }})
              </option>
            </select>
            <input v-model="signForm.certPassword" type="password" placeholder="Contraseña del certificado" class="block w-full border rounded p-2" required />
            <div class="flex space-x-2">
              <input v-model.number="signForm.page" type="number" min="0" placeholder="Página" class="w-1/3 border rounded p-2" required />
              <input v-model.number="signForm.x" type="number" min="0" placeholder="Posición X" class="w-1/3 border rounded p-2" required />
              <input v-model.number="signForm.y" type="number" min="0" placeholder="Posición Y" class="w-1/3 border rounded p-2" required />
            </div>
            <button type="submit" class="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded">Firmar Documento</button>
            <p v-if="signStatus" :class="signStatus.includes('correctamente') ? 'text-green-600' : 'text-red-600'">{{ signStatus }}</p>
          </form>
          <div v-if="pdfBlobUrl" class="border rounded overflow-hidden p-4 mt-4">
            <div class="relative w-full h-96" @click="handlePdfClick" style="cursor: crosshair;">
              <iframe :src="pdfBlobUrl" class="w-full h-96" frameborder="0" style="pointer-events: none; position: absolute; left: 0; top: 0;"></iframe>
              <div class="absolute top-0 left-0 w-full h-full" style="z-index: 2;"></div>
              <img v-if="signForm.x !== null && signForm.y !== null" :src="firmaImg" :style="{ position: 'absolute', left: signForm.x + 'px', top: signForm.y + 'px', width: '120px', height: '60px', zIndex: 3 }" alt="Firma" />
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- Documentos Firmados Recientemente -->
    <div class="border-0 shadow-sm bg-white/80 backdrop-blur-sm rounded-xl">
      <div class="p-6">
        <div class="flex items-center text-xl font-semibold mb-2">
          <svg class="w-6 h-6 mr-3 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor"><circle cx="12" cy="12" r="10"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4"/></svg>
          Documentos Firmados Recientemente
        </div>
        <div class="text-slate-600 mb-4">Tu historial de firmas más reciente</div>
        <div v-if="recentlySigned.length === 0" class="text-center text-slate-500 py-8">No has firmado documentos aún.</div>
        <div v-else class="space-y-4">
          <div v-for="doc in recentlySigned" :key="doc.id" class="flex items-center justify-between p-4 rounded-lg bg-emerald-50">
            <div class="flex items-center space-x-4">
              <div class="w-10 h-10 bg-emerald-100 rounded-lg flex items-center justify-center">
                <svg class="w-5 h-5 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor"><circle cx="12" cy="12" r="10"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4"/></svg>
              </div>
              <div>
                <p class="font-medium text-slate-900">{{ doc.fileName }}</p>
                <p class="text-sm text-slate-600">Firmado el {{ doc.updatedAt ? new Date(doc.updatedAt).toLocaleDateString() : '-' }}</p>
              </div>
            </div>
            <div class="flex items-center space-x-3">
              <span class="bg-emerald-100 text-emerald-700 px-2 py-1 rounded text-xs font-medium">Completado</span>
              <button class="text-emerald-600 hover:text-emerald-700 px-2 py-1 rounded text-sm font-medium" @click="viewDocument(doc.id)">Ver Documento</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template> 