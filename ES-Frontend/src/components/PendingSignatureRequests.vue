<script setup>
import { ref, onMounted } from 'vue';
import { getAllSignatureRequests, getUsersBySignatureRequest, responderSolicitudFirma, getCertificatesByUser } from '../utils/api';
import { authState } from '../service/Auth';

const requests = ref([]);
const loading = ref(false);
const selectedRequest = ref(null);
const userEntry = ref(null);
const certificateId = ref('');
const certificatePassword = ref('');
const status = ref('');
const certificates = ref([]);

onMounted(async () => {
  await fetchRequests();
  await fetchCertificates();
});

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
  status.value = 'Enviando respuesta...';
  try {
    await responderSolicitudFirma({
      id: userEntry.value.id,
      signatureRequest: { id: selectedRequest.value.id },
      userId: userEntry.value.userId,
      page: userEntry.value.page,
      posX: userEntry.value.posX,
      posY: userEntry.value.posY,
      status: permitir ? 'PERMITIDO' : 'DENEGADO',
      certificateId: permitir ? certificateId.value : undefined,
      certificatePassword: permitir ? certificatePassword.value : undefined
    });
    status.value = 'Respuesta enviada correctamente.';
    await fetchRequests();
    selectedRequest.value = null;
  } catch {
    status.value = 'Error al enviar la respuesta.';
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
            <input v-model="certificatePassword" type="password" class="w-full border rounded p-2" />
          </div>
        </div>
        <p v-if="status" class="mt-2 text-center" :class="status.includes('correctamente') ? 'text-green-600' : 'text-red-600'">{{ status }}</p>
      </div>
    </div>
  </div>
</template>
