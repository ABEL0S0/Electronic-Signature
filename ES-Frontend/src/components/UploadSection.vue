<script setup>
import { ref, onMounted } from 'vue';
import { uploadDocument, uploadCertificate, getDocumentsByUser, getCertificatesByUser } from '../utils/api';
import { authState } from '../service/Auth';

const documentFile = ref(null);
const documentStatus = ref("");
const certificateFile = ref(null);
const certificatePassword = ref("");
const certificateStatus = ref("");
const documents = ref([]);
const certificates = ref([]);
const documentInput = ref(null);
const certificateInput = ref(null);

function handleDocumentFileSelect(e) {
  documentFile.value = e.target.files[0];
  documentStatus.value = "";
}
async function handleUploadDocument() {
  if (!documentFile.value) {
    documentStatus.value = "Selecciona un archivo";
    return;
  }
  documentStatus.value = "Subiendo documento...";
  try {
    await uploadDocument(documentFile.value);
    documentStatus.value = "Documento subido exitosamente";
    documentFile.value = null;
    await fetchDocuments();
  } catch {
    documentStatus.value = "Error al subir el documento";
  }
}
function handleCertificateFileSelect(e) {
  certificateFile.value = e.target.files[0];
  certificateStatus.value = "";
}
async function handleUploadCertificate() {
  if (!certificateFile.value || !certificatePassword.value) {
    certificateStatus.value = "Completa todos los campos";
    return;
  }
  certificateStatus.value = "Subiendo certificado...";
  try {
    await uploadCertificate(certificateFile.value, certificatePassword.value);
    certificateStatus.value = "Certificado subido exitosamente";
    certificateFile.value = null;
    certificatePassword.value = "";
    await fetchCertificates();
  } catch {
    certificateStatus.value = "Error al subir el certificado";
  }
}

async function fetchDocuments() {
  try {
    const res = await getDocumentsByUser();
    documents.value = res.data;
  } catch {}
}
async function fetchCertificates() {
  const user = authState.user;
  if (!user?.email) return;
  try {
    const res = await getCertificatesByUser(user.email);
    certificates.value = res.data;
  } catch {}
}
onMounted(() => {
  fetchDocuments();
  fetchCertificates();
});

function renderIcon(icon, classes = '') {
  switch (icon) {
    case 'file':
      return `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7V3a1 1 0 011-1h8a1 1 0 011 1v4m-2 4h2a2 2 0 012 2v7a2 2 0 01-2 2H7a2 2 0 01-2-2v-7a2 2 0 012-2h2m2-4v4m0 0H7m4 0h4"/></svg>`;
    case 'shield':
      return `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 22s8-4 8-10V5.236A2.236 2.236 0 0017.764 3H6.236A2.236 2.236 0 004 5.236V12c0 6 8 10 8 10z"/></svg>`;
    case 'zap':
      return `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>`;
    default:
      return '';
  }
}

function getSuccessRate() {
  // Simulación: 98% si hay documentos, 0% si no
  if (documents.value.length > 0) return '98%';
  return '0%';
}

function triggerDocumentInput() {
  documentInput.value.click();
}
function triggerCertificateInput() {
  certificateInput.value.click();
}

</script>
<template>
  <div class="grid lg:grid-cols-2 gap-8">
    <!-- Subir Documento PDF -->
    <div class="bg-white/90 rounded-xl shadow p-8 flex flex-col">
      <h3 class="flex items-center text-xl font-semibold mb-2">
        <svg class="w-6 h-6 mr-2 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7V3a1 1 0 011-1h8a1 1 0 011 1v4m-2 4h2a2 2 0 012 2v7a2 2 0 01-2 2H7a2 2 0 01-2-2v-7a2 2 0 012-2h2m2-4v4m0 0H7m4 0h4"/></svg>
        Subir Documento PDF
      </h3>
      <p class="text-slate-600 mb-4">Arrastra y suelta tu archivo PDF o selecciónalo desde tu dispositivo</p>
      <label class="flex flex-col items-center justify-center border-2 border-dashed border-emerald-200 rounded-xl py-10 mb-4 cursor-pointer hover:border-emerald-400 transition-all">
        <svg class="w-14 h-14 text-emerald-400 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
        <span class="font-medium text-slate-700 mb-1">Arrastra tu archivo aquí</span>
        <span class="text-slate-500 text-sm mb-2">o haz clic para seleccionar desde tu dispositivo</span>
        <input ref="documentInput" type="file" accept=".pdf" @change="handleDocumentFileSelect" class="hidden" />
        <button type="button" class="mt-2 px-4 py-2 bg-white border border-emerald-200 rounded text-emerald-700 font-semibold hover:bg-emerald-50" @click.prevent="triggerDocumentInput">Seleccionar Archivo PDF</button>
        <span class="text-xs text-slate-400 mt-2">Máximo 10MB · Solo archivos PDF</span>
      </label>
      <button @click="handleUploadDocument" class="w-full h-12 bg-emerald-400 hover:bg-emerald-500 text-white font-bold rounded-lg mt-2">Subir Documento</button>
      <p v-if="documentStatus" class="mt-2 text-center" :class="documentStatus.includes('exitosamente') ? 'text-green-600' : 'text-red-600'">{{ documentStatus }}</p>
    </div>
    <!-- Subir Certificado Digital -->
    <div class="bg-white/90 rounded-xl shadow p-8 flex flex-col">
      <h3 class="flex items-center text-xl font-semibold mb-2">
        <svg class="w-6 h-6 mr-2 text-teal-600" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 22s8-4 8-10V5.236A2.236 2.236 0 0017.764 3H6.236A2.236 2.236 0 004 5.236V12c0 6 8 10 8 10z"/></svg>
        Subir Certificado Digital
      </h3>
      <p class="text-slate-600 mb-4">Carga tu certificado digital para firmar documentos de forma segura</p>
      <label class="flex flex-col items-center justify-center border-2 border-dashed border-teal-200 rounded-xl py-10 mb-4 cursor-pointer hover:border-teal-400 transition-all">
        <svg class="w-14 h-14 text-teal-400 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 22s8-4 8-10V5.236A2.236 2.236 0 0017.764 3H6.236A2.236 2.236 0 004 5.236V12c0 6 8 10 8 10z"/></svg>
        <span class="font-medium text-slate-700 mb-1">Certificado Digital</span>
        <span class="text-slate-500 text-sm mb-2">Sube tu archivo .p12 o .pfx</span>
        <input ref="certificateInput" type="file" accept=".p12,.pfx" @change="handleCertificateFileSelect" class="hidden" />
        <button type="button" class="mt-2 px-4 py-2 bg-white border border-teal-200 rounded text-teal-700 font-semibold hover:bg-teal-50" @click.prevent="triggerCertificateInput">Seleccionar Certificado</button>
        <span class="text-xs text-slate-400 mt-2">Formatos: .p12, .pfx</span>
      </label>
      <input type="password" v-model="certificatePassword" placeholder="Ingresa la contraseña de tu certificado" class="mb-4 block w-full border border-teal-200 rounded-md py-2 px-3" />
      <button @click="handleUploadCertificate" class="w-full h-12 bg-teal-400 hover:bg-teal-500 text-white font-bold rounded-lg">Subir Certificado</button>
      <p v-if="certificateStatus" class="mt-2 text-center" :class="certificateStatus.includes('exitosamente') ? 'text-green-600' : 'text-red-600'">{{ certificateStatus }}</p>
    </div>
  </div>
  <!-- Archivos Recientes -->
  <div class="border-0 shadow-sm bg-white/80 backdrop-blur-sm rounded-xl mt-8">
    <div class="p-6">
      <h3 class="text-xl font-bold mb-2">Archivos Recientes</h3>
      <p class="text-slate-600 mb-4">Historial de tus últimas cargas de archivos</p>
      <div v-if="documents.length === 0" class="text-center text-slate-500 py-8">No has subido documentos aún.</div>
      <div v-else class="space-y-4">
        <div v-for="(doc, index) in documents.slice(-5).reverse()" :key="doc.id" class="flex items-center justify-between p-4 rounded-lg bg-slate-50 hover:bg-slate-100 transition-colors">
          <div class="flex items-center space-x-4">
            <div class="w-10 h-10 bg-emerald-100 rounded-lg flex items-center justify-center">
              <svg class="w-5 h-5 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
            </div>
            <div>
              <p class="font-medium text-slate-900">{{ doc.fileName }}</p>
              <p class="text-sm text-slate-600">PDF • {{ new Date(doc.uploadedAt).toLocaleDateString() }}</p>
            </div>
          </div>
          <div class="flex items-center space-x-3">
            <span class="bg-emerald-100 text-emerald-700 px-2 py-1 rounded text-xs font-medium">Subido</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template> 