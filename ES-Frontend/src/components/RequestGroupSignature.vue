<script setup>
import { ref, computed, onMounted } from 'vue';
import { getDocumentsByUser, getAllUsers, createSignatureRequest } from '../utils/api';
import { authState } from '../service/Auth';

const documents = ref([]);
const users = ref([]);
const selectedDocument = ref(null);
const searchQuery = ref('');
const userResults = ref([]);
const selectedUsers = ref([]); // [{user, page, posX, posY}]
const position = ref({ page: 1, posX: 100, posY: 100 });
const status = ref('');

onMounted(async () => {
  await fetchDocuments();
  await fetchUsers();
});

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
  selectedUsers.value.push({
    user,
    page: position.value.page,
    posX: position.value.posX,
    posY: position.value.posY
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
        page: u.page,
        posX: u.posX,
        posY: u.posY
      }))
    };
    await createSignatureRequest(payload);
    status.value = 'Solicitud enviada correctamente.';
    selectedUsers.value = [];
    selectedDocument.value = null;
  } catch {
    status.value = 'Error al enviar la solicitud.';
  }
}
</script>

<template>
  <div class="bg-white/90 rounded-xl shadow p-8 max-w-2xl mx-auto">
    <h2 class="text-xl font-bold mb-4">Solicitar Firma Grupal</h2>
    <div class="mb-4">
      <label class="block font-medium mb-1">Selecciona un documento</label>
      <select v-model="selectedDocument" class="w-full border rounded p-2">
        <option :value="null">-- Selecciona --</option>
        <option v-for="doc in documents" :key="doc.id" :value="doc">{{ doc.fileName }}</option>
      </select>
    </div>
    <div class="mb-4">
      <label class="block font-medium mb-1">Buscar usuario</label>
      <input v-model="searchQuery" @input="searchUsers" placeholder="Nombre, apellido o email" class="w-full border rounded p-2 mb-2" />
      <div v-if="userResults.length" class="bg-slate-50 border rounded p-2 max-h-32 overflow-y-auto">
        <div v-for="user in userResults" :key="user.id" class="flex justify-between items-center py-1">
          <span>{{ user.firstName }} {{ user.lastName }} ({{ user.email }})</span>
          <button @click="addUser(user)" class="text-emerald-600 hover:underline">Agregar</button>
        </div>
      </div>
    </div>
    <div class="mb-4">
      <label class="block font-medium mb-1">Posición de la firma (para el usuario a agregar)</label>
      <div class="flex space-x-2">
        <input v-model.number="position.page" type="number" min="1" placeholder="Página" class="w-20 border rounded p-2" />
        <input v-model.number="position.posX" type="number" min="0" placeholder="X" class="w-20 border rounded p-2" />
        <input v-model.number="position.posY" type="number" min="0" placeholder="Y" class="w-20 border rounded p-2" />
      </div>
    </div>
    <div class="mb-4">
      <h4 class="font-medium mb-2">Usuarios seleccionados</h4>
      <div v-if="selectedUsers.length === 0" class="text-slate-500">No hay usuarios agregados.</div>
      <ul>
        <li v-for="u in selectedUsers" :key="u.user.id" class="flex items-center justify-between py-1">
          <span>{{ u.user.firstName }} {{ u.user.lastName }} ({{ u.user.email }}) - Página: {{ u.page }}, X: {{ u.posX }}, Y: {{ u.posY }}</span>
          <button @click="removeUser(u.user.id)" class="text-red-500 hover:underline">Quitar</button>
        </li>
      </ul>
    </div>
    <button @click="submitRequest" class="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg py-3">Solicitar Firma</button>
    <p v-if="status" class="mt-2 text-center" :class="status.includes('correctamente') ? 'text-green-600' : 'text-red-600'">{{ status }}</p>
  </div>
</template>
