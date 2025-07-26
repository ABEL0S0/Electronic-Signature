<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-bold text-slate-900">Solicitudes de Certificados</h2>
        <p class="text-slate-600 mt-1">Gestiona las solicitudes de certificados digitales</p>
      </div>
      <div class="flex items-center space-x-2">
        <span class="text-sm text-slate-500">Total: {{ requests.length }}</span>
        <span class="text-sm text-amber-600 font-medium">Pendientes: {{ pendingCount }}</span>
      </div>
    </div>

    <!-- Filtros -->
    <div class="bg-white/80 backdrop-blur-sm rounded-lg shadow-sm p-4">
      <div class="flex flex-wrap gap-4">
        <button
          @click="filter = 'all'"
          :class="['px-4 py-2 rounded-lg text-sm font-medium transition-colors', 
            filter === 'all' ? 'bg-emerald-600 text-white' : 'bg-slate-100 text-slate-700 hover:bg-slate-200']"
        >
          Todas ({{ requests.length }})
        </button>
        <button
          @click="filter = 'pending'"
          :class="['px-4 py-2 rounded-lg text-sm font-medium transition-colors', 
            filter === 'pending' ? 'bg-amber-600 text-white' : 'bg-slate-100 text-slate-700 hover:bg-slate-200']"
        >
          Pendientes ({{ pendingCount }})
        </button>
        <button
          @click="filter = 'approved'"
          :class="['px-4 py-2 rounded-lg text-sm font-medium transition-colors', 
            filter === 'approved' ? 'bg-green-600 text-white' : 'bg-slate-100 text-slate-700 hover:bg-slate-200']"
        >
          Aprobadas ({{ approvedCount }})
        </button>
        <button
          @click="filter = 'rejected'"
          :class="['px-4 py-2 rounded-lg text-sm font-medium transition-colors', 
            filter === 'rejected' ? 'bg-red-600 text-white' : 'bg-slate-100 text-slate-700 hover:bg-slate-200']"
        >
          Rechazadas ({{ rejectedCount }})
        </button>
      </div>
    </div>

    <!-- Lista de solicitudes -->
    <div class="bg-white/80 backdrop-blur-sm rounded-lg shadow-sm">
      <div v-if="loading" class="p-8 text-center">
        <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-600 mx-auto"></div>
        <p class="text-slate-600 mt-2">Cargando solicitudes...</p>
      </div>

      <div v-else-if="filteredRequests.length === 0" class="p-8 text-center">
        <div class="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg class="w-8 h-8 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
        </div>
        <h3 class="text-lg font-medium text-slate-900 mb-2">No hay solicitudes</h3>
        <p class="text-slate-600">No se encontraron solicitudes con el filtro seleccionado.</p>
      </div>

      <div v-else class="divide-y divide-slate-200">
        <div
          v-for="request in filteredRequests"
          :key="request.id"
          class="p-6 hover:bg-slate-50 transition-colors"
        >
          <div class="flex items-start justify-between">
            <div class="flex-1">
              <div class="flex items-center space-x-3 mb-2">
                <h3 class="text-lg font-medium text-slate-900">{{ request.userName }}</h3>
                <span
                  :class="[
                    'px-2 py-1 rounded-full text-xs font-medium',
                    request.status === 'PENDING' ? 'bg-amber-100 text-amber-700' :
                    request.status === 'APPROVED' ? 'bg-green-100 text-green-700' :
                    'bg-red-100 text-red-700'
                  ]"
                >
                  {{ getStatusText(request.status) }}
                </span>
              </div>
              
              <div class="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm text-slate-600">
                <div>
                  <span class="font-medium">Email:</span> {{ request.userEmail }}
                </div>
                <div>
                  <span class="font-medium">Organización:</span> {{ request.organization }}
                </div>
                <div>
                  <span class="font-medium">Fecha:</span> {{ formatDate(request.requestedAt) }}
                </div>
              </div>

              <div v-if="request.status === 'REJECTED' && request.rejectionReason" class="mt-3 p-3 bg-red-50 border border-red-200 rounded-lg">
                <p class="text-sm text-red-800">
                  <span class="font-medium">Motivo del rechazo:</span> {{ request.rejectionReason }}
                </p>
              </div>

                             <div v-if="request.status === 'APPROVED' && request.processedBy" class="mt-3 p-3 bg-green-50 border border-green-200 rounded-lg">
                 <p class="text-sm text-green-800">
                   <span class="font-medium">Aprobado por:</span> {{ request.processedBy }}
                   <span class="ml-2" v-if="request.processedAt">el {{ formatDate(request.processedAt) }}</span>
                 </p>
               </div>
            </div>

            <!-- Acciones -->
            <div v-if="request.status === 'PENDING'" class="flex items-center space-x-2 ml-4">
              <button
                @click="approveRequest(request.id)"
                :disabled="processingRequest === request.id"
                class="bg-green-600 hover:bg-green-700 disabled:bg-green-400 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center"
              >
                <svg v-if="processingRequest === request.id" class="animate-spin -ml-1 mr-2 h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <svg v-else class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                </svg>
                Aprobar
              </button>
              
              <button
                @click="showRejectModal = true; selectedRequest = request"
                :disabled="processingRequest === request.id"
                class="bg-red-600 hover:bg-red-700 disabled:bg-red-400 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center"
              >
                <svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                </svg>
                Rechazar
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal de rechazo -->
    <div v-if="showRejectModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 max-w-md w-full mx-4">
        <h3 class="text-lg font-medium text-slate-900 mb-4">Rechazar Solicitud</h3>
        <p class="text-slate-600 mb-4">
          ¿Estás seguro de que quieres rechazar la solicitud de <strong>{{ selectedRequest?.userName }}</strong>?
        </p>
        
        <div class="space-y-4">
          <div>
            <label for="rejectionReason" class="block text-sm font-medium text-slate-700 mb-2">
              Motivo del rechazo
            </label>
            <textarea
              v-model="rejectionReason"
              id="rejectionReason"
              rows="3"
              placeholder="Explica el motivo del rechazo..."
              class="w-full border border-slate-300 rounded-lg px-3 py-2 focus:ring-emerald-400 focus:border-emerald-400"
              required
            ></textarea>
          </div>
          
          <div class="flex justify-end space-x-3">
            <button
              @click="showRejectModal = false; rejectionReason = ''"
              class="px-4 py-2 text-slate-600 hover:text-slate-800 font-medium"
            >
              Cancelar
            </button>
            <button
              @click="rejectRequest(selectedRequest?.id)"
              :disabled="!rejectionReason.trim() || processingRequest === selectedRequest?.id"
              class="bg-red-600 hover:bg-red-700 disabled:bg-red-400 text-white px-4 py-2 rounded-lg font-medium transition-colors"
            >
              {{ processingRequest === selectedRequest?.id ? 'Rechazando...' : 'Rechazar' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { 
  getPendingCertificateRequests, 
  approveCertificateRequest, 
  rejectCertificateRequest 
} from '../utils/api';

interface CertificateRequest {
  id: number;
  userEmail: string;
  userName: string;
  organization: string;
  password: string;
  status: string;
  requestedAt: string;
  processedAt?: string;
  processedBy?: string;
  rejectionReason?: string;
}

const requests = ref<CertificateRequest[]>([]);
const loading = ref(false);
const filter = ref<'all' | 'pending' | 'approved' | 'rejected'>('all');
const processingRequest = ref<number | null>(null);
const showRejectModal = ref(false);
const selectedRequest = ref<CertificateRequest | null>(null);
const rejectionReason = ref('');

const filteredRequests = computed(() => {
  if (filter.value === 'all') return requests.value;
  return requests.value.filter(req => req.status === filter.value.toUpperCase());
});

const pendingCount = computed(() => requests.value.filter(req => req.status === 'PENDING').length);
const approvedCount = computed(() => requests.value.filter(req => req.status === 'APPROVED').length);
const rejectedCount = computed(() => requests.value.filter(req => req.status === 'REJECTED').length);

const getStatusText = (status: string) => {
  switch (status) {
    case 'PENDING': return 'Pendiente';
    case 'APPROVED': return 'Aprobada';
    case 'REJECTED': return 'Rechazada';
    default: return status;
  }
};

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('es-ES', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const fetchRequests = async () => {
  loading.value = true;
  try {
    const response = await getPendingCertificateRequests();
    requests.value = response.data;
  } catch (error) {
    console.error('Error fetching requests:', error);
  } finally {
    loading.value = false;
  }
};

const approveRequest = async (id: number) => {
  processingRequest.value = id;
  try {
    await approveCertificateRequest(id);
    await fetchRequests(); // Recargar la lista
  } catch (error) {
    console.error('Error approving request:', error);
  } finally {
    processingRequest.value = null;
  }
};

const rejectRequest = async (id?: number) => {
  if (!id || !rejectionReason.value.trim()) return;
  
  processingRequest.value = id;
  try {
    await rejectCertificateRequest(id, rejectionReason.value);
    await fetchRequests(); // Recargar la lista
    showRejectModal.value = false;
    rejectionReason.value = '';
  } catch (error) {
    console.error('Error rejecting request:', error);
  } finally {
    processingRequest.value = null;
  }
};

onMounted(() => {
  fetchRequests();
});
</script> 