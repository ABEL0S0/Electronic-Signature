<script setup>
import { ref, onMounted, watch } from "vue";
import axios from "axios";
import { authService, authState } from "../service/Auth";
import {
  uploadDocument,
  uploadCertificate,
  getDocumentsByUser,
  getCertificatesByUser,
  deleteDocument,
  deleteCertificate,
  downloadDocument,
  downloadCertificate
} from "../utils/api";

// —– Tabs y usuario —–
const activeTab = ref("upload");      // upload, sign, documents
const currentUser = authState.user;
const userName = currentUser
  ? `${currentUser.firstName} ${currentUser.lastName}`
  : "Usuario";
const userEmail = ref(currentUser?.email || "");

// —– Listas globales —–
const documents   = ref([]);
const certificates = ref([]);

// —– Archivos PDF para firmar y su vista —–
const docsToSign  = ref([]);
const pdfBlobUrl  = ref(null);

// —– Subida de archivos —–
const documentFile   = ref(null);
const documentStatus = ref("");
const certificateFile     = ref(null);
const certificatePassword = ref("");
const certificateStatus   = ref("");

// —– Helpers —–
function getToken() {
  return localStorage.getItem("token");
}

// —– Carga inicial —–
async function loadUserFiles() {
  if (!userEmail.value) return;
  try {
    const [docsRes, certsRes] = await Promise.all([
      getDocumentsByUser(),
      getCertificatesByUser(userEmail.value)
    ]);
    documents.value    = docsRes.data;
    certificates.value = certsRes.data;
  } catch (e) {
    console.error("Error cargando archivos:", e);
  }
}

// —– Upload Document —–
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
    await loadUserFiles();
  } catch {
    documentStatus.value = "Error al subir el documento";
  }
}

// —– Upload Certificate —–
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
    await loadUserFiles();
  } catch {
    certificateStatus.value = "Error al subir el certificado";
  }
}

// —– Descargar y eliminar Document —–
async function handleDownloadDocument(doc) {
  try {
    const res = await downloadDocument(doc.id);
    const blob = new Blob([res.data], { type: res.headers["content-type"] });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = doc.fileName;
    link.click();
  } catch {
    alert("Error al descargar documento.");
  }
}
async function handleDeleteDocument(id) {
  if (!confirm("¿Eliminar documento?")) return;
  try {
    await deleteDocument(id);
    await loadUserFiles();
    pdfBlobUrl.value = null;
  } catch {
    alert("Error al eliminar documento.");
  }
}

// —– Descargar y eliminar Certificate —–
async function handleDownloadCertificate(cert) {
  const pwd = prompt("Introduce contraseña:");
  if (!pwd) return alert("Contraseña obligatoria.");
  try {
    const res = await downloadCertificate(cert.id, pwd);
    const blob = new Blob([res.data], { type: "application/octet-stream" });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = cert.filename;
    link.click();
  } catch {
    alert("Error al descargar certificado.");
  }
}
async function handleDeleteCertificate(id) {
  if (!confirm("¿Eliminar certificado?")) return;
  try {
    await deleteCertificate(id);
    await loadUserFiles();
  } catch {
    alert("Error al eliminar certificado.");
  }
}

// —– Lista para firmar —–
async function fetchDocsToSign() {
  try {
    const res = await axios.get("/api/documents/list", {
      headers: { Authorization: `Bearer ${getToken()}` }
    });
    docsToSign.value = res.data;
  } catch (e) {
    console.error("Error cargando docs para firmar:", e);
  }
}

// —– Ver PDF autenticado con Blob —–
async function viewDocument(id) {
  try {
    if (pdfBlobUrl.value) {
      URL.revokeObjectURL(pdfBlobUrl.value);
      pdfBlobUrl.value = null;
    }
    const res = await axios.get(`/api/documents/${id}/view`, {
      headers: { Authorization: `Bearer ${getToken()}` },
      responseType: "blob"
    });
    pdfBlobUrl.value = URL.createObjectURL(res.data);
    activeTab.value = "sign";
  } catch (e) {
    console.error("Error al cargar PDF:", e);
    alert("No se pudo mostrar el documento.");
  }
}

// —– Logout —–
function handleSignOut() {
  authService.clearAuth();
  window.location.hash = "/";
}

// —– Ciclo de vida —–
onMounted(() => {
  loadUserFiles();
  if (activeTab.value === "sign") fetchDocsToSign();
});
watch(activeTab, (tab) => {
  if (tab === "sign") fetchDocsToSign();
});
</script>

<template>
  <div class="min-h-screen bg-gradient-to-b from-blue-50 to-blue-100">
    <header class="bg-white shadow">
      <div
        class="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 flex justify-between items-center"
      >
        <div>
          <h1 class="text-3xl font-medium tracking-tight text-gray-900">
            Firmas Electrónicas
          </h1>
          <p class="text-gray-500">Sistema de gestión de documentos y firmas</p>
        </div>
        <div class="flex items-center">
          <span class="mr-4 text-gray-700">{{ userName }}</span>
          <a
            href="#/"
            @click.prevent="handleSignOut"
            class="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-md text-sm font-medium"
            >Cerrar Sesión</a
          >
        </div>
      </div>
    </header>

    <main>
      <div class="mx-auto max-w-7xl py-6 sm:px-6 lg:px-8">
        <!-- Navigation Tabs -->
        <div class="mb-6">
          <nav class="flex space-x-8">
            <button
              @click="activeTab = 'upload'"
              :class="[
                'px-3 py-2 rounded-md text-sm font-medium',
                activeTab === 'upload'
                  ? 'bg-blue-100 text-blue-700'
                  : 'text-gray-500 hover:text-gray-700'
              ]"
            >
              Subir Archivos
            </button>
            <button
              @click="activeTab = 'sign'"
              :class="[
                'px-3 py-2 rounded-md text-sm font-medium',
                activeTab === 'sign'
                  ? 'bg-blue-100 text-blue-700'
                  : 'text-gray-500 hover:text-gray-700'
              ]"
            >
              Firmar Documentos
            </button>
            <button
              @click="activeTab = 'documents'"
              :class="[
                'px-3 py-2 rounded-md text-sm font-medium',
                activeTab === 'documents'
                  ? 'bg-blue-100 text-blue-700'
                  : 'text-gray-500 hover:text-gray-700'
              ]"
            >
              Mis Documentos
            </button>
          </nav>
        </div>

        <!-- Upload Files Tab -->
        <div v-if="activeTab === 'upload'" class="bg-white shadow rounded-lg p-6">
          <h2 class="text-xl font-semibold text-gray-900 mb-4">Subir Archivos</h2>
          <p class="text-gray-600 mb-6">
            Sube archivos PDF y certificados para firmar electrónicamente.
          </p>

          <div class="space-y-8">
            <!-- Subir Documento PDF -->
            <div>
              <h3 class="text-lg font-semibold mb-2">Subir Documento PDF</h3>
              <input type="file" accept=".pdf" @change="handleDocumentFileSelect" class="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100" />
              <button @click="handleUploadDocument" class="mt-2 bg-blue-600 text-white px-4 py-2 rounded">Subir Documento</button>
              <p v-if="documentStatus" class="mt-2" :class="documentStatus.includes('exitosamente') ? 'text-green-600' : 'text-red-600'">{{ documentStatus }}</p>
            </div>

            <!-- Subir Certificado -->
            <div>
              <h3 class="text-lg font-semibold mb-2">Subir Certificado</h3>
              <input type="file" accept=".p12,.pfx,.cer,.crt" @change="handleCertificateFileSelect" class="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"/>
              <input type="password" v-model="certificatePassword" placeholder="Contraseña del certificado" class="mt-2 block" />
              <button @click="handleUploadCertificate" class="mt-2 bg-green-600 text-white px-4 py-2 rounded">Subir Certificado</button>
              <p v-if="certificateStatus" class="mt-2" :class="certificateStatus.includes('exitosamente') ? 'text-green-600' : 'text-red-600'">{{ certificateStatus }}</p>
            </div>
          </div>
        </div>

            <!-- Sign Documents Tab -->
<div v-if="activeTab === 'sign'" class="bg-white shadow rounded-lg p-6">
  <h2 class="text-xl font-semibold text-gray-900 mb-4">Firmar Documentos</h2>

  <!-- Mensaje si no hay documentos -->
  <div v-if="docsToSign.length === 0" class="text-center py-12">
    <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0
               012-2h5.586a1 1 0 01.707.293l5.414
               5.414a1 1 0 01.293.707V19a2 2 0
               01-2 2z"/>
    </svg>
    <h3 class="mt-2 text-sm font-medium text-gray-900">No tienes documentos para firmar.</h3>
  </div>

  <!-- Lista y visor -->
  <div v-else class="grid md:grid-cols-2 gap-6">
    <!-- Lista de documentos -->
    <ul class="space-y-2">
      <li v-for="doc in docsToSign" :key="doc.id">
        <!-- cambiamos button por un enlace que llama a viewDocument -->
        <a href="#"
           @click.prevent="viewDocument(doc.id)"
           class="text-blue-600 hover:underline">
          {{ doc.fileName }}
        </a>
      </li>
    </ul>

    <!-- Visor PDF vía Blob-URL -->
    <div v-if="pdfBlobUrl" class="border rounded overflow-hidden">
      <iframe
        :src="pdfBlobUrl"
        class="w-full h-96"
        frameborder="0"
      ></iframe>
    </div>
  </div>
</div>

        <!-- My Documents Tab -->
        <div v-if="activeTab === 'documents'" class="space-y-6">
          <!-- PDF Documents -->
          <div class="bg-white shadow rounded-lg p-6">
            <h2 class="text-xl font-semibold text-gray-900 mb-4">Documentos PDF</h2>
            <div v-if="documents.length === 0" class="text-center py-8">
              <svg
                class="mx-auto h-12 w-12 text-gray-400"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                />
              </svg>
              <h3 class="mt-2 text-sm font-medium text-gray-900">No hay documentos PDF</h3>
              <p class="mt-1 text-sm text-gray-500">
                Sube tu primer documento PDF para comenzar.
              </p>
            </div>
            <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div
                v-for="doc in documents"
                :key="doc.id"
                class="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow"
              >
                <div class="flex items-center">
                  <div class="flex-shrink-0 bg-red-100 rounded-full p-2">
                    <svg class="h-5 w-5 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                  </div>
                  <div class="ml-3 flex-1">
                    <p class="text-sm font-medium text-gray-900 truncate">{{ doc.fileName }}</p>
                    <p class="text-sm text-gray-500">{{ new Date(doc.uploadedAt).toLocaleString() }}</p>
                  </div>
                  <button @click="handleDownloadDocument(doc)" class="ml-4 text-gray-400 hover:text-blue-600" title="Descargar">
                    <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                    </svg>
                  </button>
                  <button @click="handleDeleteDocument(doc.id)" class="ml-2 text-gray-400 hover:text-red-600" title="Eliminar">
                    <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Certificates -->
          <div class="bg-white shadow rounded-lg p-6">
            <h2 class="text-xl font-semibold text-gray-900 mb-4">Certificados</h2>
            <div v-if="certificates.length === 0" class="text-center py-8">
              <svg
                class="mx-auto h-12 w-12 text-gray-400"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"
                />
              </svg>
              <h3 class="mt-2 text-sm font-medium text-gray-900">No hay certificados</h3>
              <p class="mt-1 text-sm text-gray-500">
                Sube tu primer certificado para comenzar.
              </p>
            </div>
            <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div
                v-for="cert in certificates"
                :key="cert.id"
                class="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow"
              >
                <div class="flex items-center">
                  <div class="flex-shrink-0 bg-green-100 rounded-full p-2">
                    <svg class="h-5 w-5 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                    </svg>
                  </div>
                  <div class="ml-3 flex-1">
                    <p class="text-sm font-medium text-gray-900 truncate">{{ cert.filename }}</p>
                    <p class="text-sm text-gray-500">{{ new Date(cert.createdAt).toLocaleString() }}</p>
                  </div>
                  <button @click="handleDownloadCertificate(cert)" class="ml-4 text-gray-400 hover:text-blue-600" title="Descargar">
                    <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                    </svg>
                  </button>
                  <button @click="handleDeleteCertificate(cert.id)" class="ml-2 text-gray-400 hover:text-red-600" title="Eliminar">
                    <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>