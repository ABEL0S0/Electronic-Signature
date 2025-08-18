<script setup>
import { ref, onMounted, computed } from 'vue';
import { getDocumentsByUser, getCertificatesByUser, downloadDocument, deleteDocument, downloadCertificate, deleteCertificate, uploadDocument, uploadCertificate } from '../utils/api';

const documents = ref([]);
const certificates = ref([]);
const search = ref('');

const documentInput = ref(null);
const certificateInput = ref(null);
const documentStatus = ref("");
const certificateStatus = ref("");
const certificatePassword = ref("");

function getToken() {
  return localStorage.getItem('token');
}
async function fetchDocuments() {
  try {
    const res = await getDocumentsByUser();
    documents.value = res.data;
  } catch {}
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
  fetchDocuments();
  fetchCertificates();
});
async function handleDownloadDocument(doc) {
  try {
    const res = await downloadDocument(doc.id);
    const blob = new Blob([res.data], { type: res.headers['content-type'] });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = doc.fileName;
    link.click();
  } catch {
    alert('Error al descargar documento.');
  }
}
async function handleDeleteDocument(id) {
  if (!confirm('¿Eliminar documento?')) return;
  try {
    await deleteDocument(id);
    await fetchDocuments();
  } catch {
    alert('Error al eliminar documento.');
  }
}
async function handleDownloadCertificate(cert) {
  const pwd = prompt('Introduce contraseña:');
  if (!pwd) return alert('Contraseña obligatoria.');
  try {
    const res = await downloadCertificate(cert.id, pwd);
    const blob = new Blob([res.data], { type: 'application/octet-stream' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = cert.filename;
    link.click();
  } catch {
    alert('Error al descargar certificado.');
  }
}
async function handleDeleteCertificate(id) {
  if (!confirm('¿Eliminar certificado?')) return;
  try {
    await deleteCertificate(id);
    await fetchCertificates();
  } catch {
    alert('Error al eliminar certificado.');
  }
}

function triggerDocumentInput() {
  documentInput.value.click();
}
function triggerCertificateInput() {
  certificateInput.value.click();
}
async function handleDocumentFileSelect(e) {
  const file = e.target.files[0];
  if (!file) return;
  documentStatus.value = "Subiendo documento...";
  try {
    await uploadDocument(file);
    documentStatus.value = "Documento subido exitosamente";
    await fetchDocuments();
  } catch {
    documentStatus.value = "Error al subir el documento";
  }
}
async function handleCertificateFileSelect(e) {
  const file = e.target.files[0];
  if (!file || !certificatePassword.value) {
    certificateStatus.value = "Selecciona archivo y escribe la contraseña";
    return;
  }
  certificateStatus.value = "Subiendo certificado...";
  try {
    await uploadCertificate(file, certificatePassword.value);
    certificateStatus.value = "Certificado subido exitosamente";
    await fetchCertificates();
  } catch {
    certificateStatus.value = "Error al subir el certificado";
  }
}

const filteredDocuments = computed(() => {
  if (!search.value) return documents.value;
  return documents.value.filter(doc =>
    doc.fileName.toLowerCase().includes(search.value.toLowerCase())
  );
});
</script>
<template>
  <div class="space-y-8">
    <!-- Encabezado y botón nuevo documento -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-3xl font-bold text-slate-900">Mis Documentos</h2>
        <p class="text-slate-600 mt-2">Administra todos tus documentos y certificados</p>
      </div>
      <button class="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded flex items-center font-medium shadow" @click.prevent="triggerDocumentInput">
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-3-3v6m8 4a2 2 0 01-2 2H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414A1 1 0 0120 9.414V19z"/></svg>
        Nuevo Documento
      </button>
      <input ref="documentInput" type="file" accept=".pdf" @change="handleDocumentFileSelect" class="hidden" />
      <input ref="certificateInput" type="file" accept=".p12,.pfx" @change="handleCertificateFileSelect" class="hidden" />
    </div>

    <!-- Barra de búsqueda y filtros -->
    <div class="bg-white/80 backdrop-blur-sm rounded shadow-sm p-6">
      <div class="flex flex-col sm:flex-row gap-4">
        <div class="relative flex-1">
          <svg class="absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400 w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
          <input v-model="search" placeholder="Buscar documentos por nombre..." class="pl-12 border border-emerald-200 focus:border-emerald-400 focus:ring-emerald-400 rounded w-full py-2" />
        </div>
        <button class="border border-emerald-200 hover:bg-emerald-50 bg-transparent text-emerald-700 px-4 py-2 rounded flex items-center font-medium">
          <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1V4zm0 6a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1v-2zm0 6a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-1 1H4a1 1 0 01-1-1v-2z"/></svg>
          Filtros
        </button>
      </div>
    </div>

    <!-- Documentos PDF -->
    <div class="bg-white/80 backdrop-blur-sm rounded shadow-sm">
      <div class="p-6 border-b">
        <div class="flex items-center text-xl font-semibold">
          <svg class="w-6 h-6 mr-3 text-emerald-600" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-3-3v6m8 4a2 2 0 01-2 2H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414A1 1 0 0120 9.414V19z"/></svg>
          Documentos PDF
        </div>
        <div class="text-slate-600 text-sm">Gestiona tus documentos PDF subidos y firmados</div>
      </div>
      <div class="p-6">
        <div v-if="filteredDocuments.length === 0" class="text-center py-16">
          <div class="w-20 h-20 bg-emerald-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <svg class="w-10 h-10 text-emerald-600" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-3-3v6m8 4a2 2 0 01-2 2H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414A1 1 0 0120 9.414V19z"/></svg>
          </div>
          <h3 class="text-xl font-semibold text-slate-900 mb-3">No hay documentos PDF</h3>
          <p class="text-slate-600 mb-6 max-w-md mx-auto">
            Comienza subiendo tu primer documento PDF para gestionar y firmar digitalmente.
          </p>
          <button class="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded flex items-center font-medium mx-auto" @click.prevent="triggerDocumentInput">
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-3-3v6m8 4a2 2 0 01-2 2H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414A1 1 0 0120 9.414V19z"/></svg>
            Subir Primer Documento
          </button>
        </div>
        <div v-else class="overflow-x-auto">
          <table class="min-w-full divide-y divide-emerald-100">
            <thead>
              <tr>
                <th class="px-4 py-2 text-left font-semibold">Documento</th>
                <th class="px-4 py-2 text-left font-semibold">Estado</th>
                <th class="px-4 py-2 text-left font-semibold">Fecha</th>
                <th class="px-4 py-2 text-left font-semibold">Tamaño</th>
                <th class="px-4 py-2 text-right font-semibold">Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="doc in filteredDocuments" :key="doc.id" class="border-emerald-50 hover:bg-emerald-50/50">
                <td class="px-4 py-2">
                  <div class="flex items-center space-x-3">
                    <div class="w-10 h-10 bg-emerald-100 rounded-lg flex items-center justify-center">
                      <svg class="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-3-3v6m8 4a2 2 0 01-2 2H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414A1 1 0 0120 9.414V19z"/></svg>
                    </div>
                    <div>
                      <p class="font-medium text-slate-900">{{ doc.fileName }}</p>
                      <p class="text-sm text-slate-500">PDF</p>
                    </div>
                  </div>
                </td>
                <td class="px-4 py-2">
                  <span :class="doc.status === 'Firmado' ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'" class="px-2 py-1 rounded text-xs font-semibold">
                    {{ doc.status || (doc.signed ? 'Firmado' : 'Pendiente') }}
                  </span>
                </td>
                <td class="px-4 py-2 text-slate-600">{{ new Date(doc.uploadedAt).toLocaleDateString() }}</td>
                <td class="px-4 py-2 text-slate-600">{{ (doc.size / 1024 / 1024).toFixed(2) }} MB</td>
                <td class="px-4 py-2 text-right">
                  <div class="inline-flex space-x-2">
                    <button @click="handleDownloadDocument(doc)" class="text-emerald-600 hover:bg-emerald-50 rounded p-1" title="Descargar">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/></svg>
                    </button>
                    <button @click="handleDeleteDocument(doc.id)" class="text-red-600 hover:bg-red-50 rounded p-1" title="Eliminar">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Certificados -->
    <div class="bg-white/80 backdrop-blur-sm rounded shadow-sm">
      <div class="p-6 border-b">
        <div class="flex items-center text-xl font-semibold">
          <svg class="w-6 h-6 mr-3 text-teal-600" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/></svg>
          Certificados Digitales
        </div>
        <div class="text-slate-600 text-sm">Administra tus certificados para firmar documentos</div>
      </div>
      <div class="p-6">
        <div v-if="certificates.length === 0" class="text-center py-16">
          <div class="w-20 h-20 bg-teal-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <svg class="w-10 h-10 text-teal-600" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/></svg>
          </div>
          <h3 class="text-xl font-semibold text-slate-900 mb-3">No hay certificados</h3>
          <p class="text-slate-600 mb-6 max-w-md mx-auto">
            Sube tu certificado digital para comenzar a firmar documentos de forma segura.
          </p>
          <button class="bg-teal-600 hover:bg-teal-700 text-white px-4 py-2 rounded flex items-center font-medium mx-auto" @click.prevent="triggerCertificateInput">
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/></svg>
            Subir Certificado
          </button>
        </div>
        <div v-else class="overflow-x-auto">
          <table class="min-w-full divide-y divide-teal-100">
            <thead>
              <tr>
                <th class="px-4 py-2 text-left font-semibold">Certificado</th>
                <th class="px-4 py-2 text-left font-semibold">Estado</th>
                <th class="px-4 py-2 text-left font-semibold">Válido hasta</th>
                <th class="px-4 py-2 text-left font-semibold">Emisor</th>
                <th class="px-4 py-2 text-right font-semibold">Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="cert in certificates" :key="cert.id" class="border-teal-50 hover:bg-teal-50/50">
                <td class="px-4 py-2">
                  <div class="flex items-center space-x-3">
                    <div class="w-10 h-10 bg-teal-100 rounded-lg flex items-center justify-center">
                      <svg class="w-5 h-5 text-teal-600" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/></svg>
                    </div>
                    <div>
                      <p class="font-medium text-slate-900">{{ cert.filename }}</p>
                      <p class="text-sm text-slate-500">Certificado Digital</p>
                    </div>
                  </div>
                </td>
                <td class="px-4 py-2">
                  <span class="bg-teal-100 text-teal-700 px-2 py-1 rounded text-xs font-semibold">Activo</span>
                </td>
                <td class="px-4 py-2 text-slate-600">{{ cert.validUntil ? new Date(cert.validUntil).toLocaleDateString() : '-' }}</td>
                <td class="px-4 py-2 text-slate-600">{{ cert.issuer || '-' }}</td>
                <td class="px-4 py-2 text-right">
                  <div class="inline-flex space-x-2">
                    <button @click="handleDownloadCertificate(cert)" class="text-teal-600 hover:bg-teal-50 rounded p-1" title="Descargar">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/></svg>
                    </button>
                    <button @click="handleDeleteCertificate(cert.id)" class="text-red-600 hover:bg-red-50 rounded p-1" title="Eliminar">
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template> 