<script setup>
import { ref, onMounted } from "vue";
import { authService, authState } from "../service/Auth";
import { uploadDocument, uploadCertificate } from "../utils/api";

// Reactive data
const selectedFile = ref(null);
const uploadStatus = ref("");
const documents = ref([]);
const certificates = ref([]);
const activeTab = ref("upload"); // upload, sign, documents

// Get user info
const user = authState.user;
const userName = user ? `${user.firstName} ${user.lastName}` : "Usuario";

// Documento
const documentFile = ref(null);
const documentStatus = ref("");
const userUuid = ref(authState.user?.uuid || "");
const signature = ref("");
const hashAlgorithm = ref("SHA256");

// Certificado
const certificateFile = ref(null);
const certificatePassword = ref("");
const certificateStatus = ref("");

function handleSignOut() {
  // Clear authentication data
  authService.clearAuth();

  // Redirect to login page
  window.location.hash = "/";
}

// File upload handling
function handleFileSelect(event) {
  selectedFile.value = event.target.files[0];
  uploadStatus.value = "";
}

async function uploadFile() {
  if (!selectedFile.value) {
    uploadStatus.value = "Por favor selecciona un archivo";
    return;
  }

  const formData = new FormData();
  formData.append("file", selectedFile.value);

  try {
    uploadStatus.value = "Subiendo archivo...";
    
    const response = await fetch("http://localhost:8080/api/documents/upload", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${authState.token}`,
      },
      body: formData,
    });

    if (response.ok) {
      uploadStatus.value = "Archivo subido exitosamente";
      selectedFile.value = null;
      // Reset file input
      const fileInput = document.getElementById("fileInput");
      if (fileInput) fileInput.value = "";
      // Refresh documents list
      loadDocuments();
    } else {
      uploadStatus.value = "Error al subir el archivo";
    }
  } catch (error) {
    uploadStatus.value = "Error de conexión";
    console.error("Error:", error);
  }
}

async function loadDocuments() {
  try {
    const response = await fetch("http://localhost:8080/api/documents", {
      headers: {
        "Authorization": `Bearer ${authState.token}`,
      },
    });

    if (response.ok) {
      const data = await response.json();
      documents.value = data.filter(doc => doc.type === "PDF");
      certificates.value = data.filter(doc => doc.type === "CERTIFICATE");
    }
  } catch (error) {
    console.error("Error loading documents:", error);
  }
}

function handleDocumentFileSelect(event) {
  documentFile.value = event.target.files[0];
  documentStatus.value = "";
}

async function handleUploadDocument() {
  if (!documentFile.value || !userUuid.value || !signature.value || !hashAlgorithm.value) {
    documentStatus.value = "Completa todos los campos";
    return;
  }
  documentStatus.value = "Subiendo documento...";
  try {
    await uploadDocument(documentFile.value, userUuid.value, signature.value, hashAlgorithm.value);
    documentStatus.value = "Documento subido exitosamente";
    documentFile.value = null;
    signature.value = "";
    // Recargar documentos si es necesario
    loadDocuments();
  } catch (e) {
    documentStatus.value = "Error al subir el documento";
  }
}

function handleCertificateFileSelect(event) {
  certificateFile.value = event.target.files[0];
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
    // Recargar certificados si es necesario
    loadDocuments();
  } catch (e) {
    certificateStatus.value = "Error al subir el certificado";
  }
}

onMounted(() => {
  loadDocuments();
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
              <input type="text" v-model="userUuid" placeholder="UUID de usuario" class="mt-2 block" />
              <input type="text" v-model="signature" placeholder="Firma" class="mt-2 block" />
              <select v-model="hashAlgorithm" class="mt-2 block">
                <option value="SHA256">SHA256</option>
                <option value="SHA512">SHA512</option>
              </select>
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
          <div class="text-center py-12">
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
            <h3 class="mt-2 text-sm font-medium text-gray-900">Funcionalidad en desarrollo</h3>
            <p class="mt-1 text-sm text-gray-500">
              La funcionalidad de firma de documentos estará disponible próximamente.
            </p>
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
                    <p class="text-sm font-medium text-gray-900 truncate">{{ doc.name }}</p>
                    <p class="text-sm text-gray-500">{{ doc.uploadDate }}</p>
                  </div>
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
                    <p class="text-sm font-medium text-gray-900 truncate">{{ cert.name }}</p>
                    <p class="text-sm text-gray-500">{{ cert.uploadDate }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>