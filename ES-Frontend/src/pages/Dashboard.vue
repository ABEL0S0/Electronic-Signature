<script setup>
import { ref, onMounted, computed } from 'vue';
import UploadSection from '../components/UploadSection.vue';
import SignDocuments from '../components/SignDocuments.vue';
import MyDocuments from '../components/MyDocuments.vue';
import RequestSignature from '../components/RequestSignature.vue';
import CertificateRequestForm from '../components/CertificateRequestForm.vue';
import AdminCertificateRequests from '../components/AdminCertificateRequests.vue';
import NotificationToast from '../components/NotificationToast.vue';
import NotificationList from '../components/NotificationList.vue';
import RequestGroupSignature from '../components/RequestGroupSignature.vue';
import PendingSignatureRequests from '../components/PendingSignatureRequests.vue';
import MobileNavigation from '../components/MobileNavigation.vue';

import { authState, authService } from '../service/Auth';
import { getDocumentsByUser } from '../utils/api';

const activeSection = ref('dashboard');
const documents = ref([]);

const stats = [
  { label: 'Documentos Firmados', value: '24', change: '+12%' },
  { label: 'Pendientes', value: '3', change: '+2' },
  { label: 'Certificados', value: '2', change: 'Activos' },
  { label: 'Este Mes', value: '18', change: '+8%' },
];

const quickActions = computed(() => {
  const baseActions = [
    { icon: 'upload', label: 'Subir Documento', action: () => (activeSection.value = 'upload') },
    { icon: 'pen', label: 'Firmar Ahora', action: () => (activeSection.value = 'sign') },
    { icon: 'file', label: 'Ver Documentos', action: () => (activeSection.value = 'documents') },
    { icon: 'users', label: 'Solicitar Firma Grupal', action: () => (activeSection.value = 'group-signature') },
    { icon: 'zap', label: 'Solicitudes de Firma', action: () => (activeSection.value = 'pending-signature') },
  ];

  // Agregar acción de solicitar certificado para usuarios normales
  if (!isAdmin.value) {
    baseActions.push({ icon: 'shield', label: 'Solicitar Certificado', action: () => (activeSection.value = 'certificate-request') });
  }

  // Agregar acción de gestionar solicitudes para admins
  if (isAdmin.value) {
    baseActions.push({ icon: 'users', label: 'Gestionar Solicitudes', action: () => (activeSection.value = 'admin-requests') });
  }

  return baseActions;
});

const isAdmin = computed(() => {
  return authState.user?.role === 'ADMIN';
});

function renderIcon(icon, classes = '') {
  // SVGs inline para Vue (puedes reemplazar por componentes si tienes una librería de iconos)
  switch (icon) {
    case 'upload':
      return `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1M4 12l8-8m0 0l8 8m-8-8v12"/></svg>`;
    case 'pen':
      return `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16.862 3.487a2.6 2.6 0 113.677 3.677L7.5 20.205l-4 1 1-4 12.362-13.718z"/></svg>`;
    case 'users':
      return `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a4 4 0 00-3-3.87M9 20H4v-2a4 4 0 013-3.87m9-7a4 4 0 11-8 0 4 4 0 018 0zm6 8a4 4 0 00-3-3.87M6 10a4 4 0 100-8 4 4 0 000 8z"/></svg>`;
    case 'file':
      return `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7V3a1 1 0 011-1h8a1 1 0 011 1v4m-2 4h2a2 2 0 012 2v7a2 2 0 01-2 2H7a2 2 0 01-2-2v-7a2 2 0 012-2h2m2-4v4m0 0H7m4 0h4"/></svg>`;
    case 'leaf':
      return `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 21c0-4.418 7-8 7-8s7 3.582 7 8"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v8"/></svg>`;
    case 'bell':
      return `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/></svg>`;
    case 'search':
      return `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-4.35-4.35M17 11A6 6 0 105 11a6 6 0 0012 0z"/></svg>`;
    case 'plus':
      return `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>`;
    case 'shield':
      return `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/></svg>`;
    case 'zap':
      return `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>`;
    default:
      return '';
  }
}

const getUserInitials = () => {
  if (!authState.user) return '';
  const { firstName, lastName } = authState.user;
  return `${(firstName?.[0] || '').toUpperCase()}${(lastName?.[0] || '').toUpperCase()}`;
};

const getUserFullName = () => {
  if (!authState.user) return '';
  const { firstName, lastName } = authState.user;
  return `${firstName || ''} ${lastName || ''}`.trim();
};

// —– Logout —–
function handleSignOut() {
  // Limpiar todos los datos de autenticación
  authService.clearAuth();
  
  // Limpiar cualquier estado local del componente
  documents.value = [];
  activeSection.value = 'dashboard';
  
  // Redirigir al login
  window.location.hash = "/";
}

// —– Section Change —–
function handleSectionChange(section) {
  activeSection.value = section;
}

// —– Load Documents —–
onMounted(async () => {
  try {
    const response = await getDocumentsByUser();
    if (response.status === 200) {
      documents.value = response.data;
    }
  } catch (error) {
    console.error('Error loading documents:', error);
  }
});
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-emerald-50 via-white to-teal-50">
    <!-- Mobile Navigation -->
    <MobileNavigation 
      :active-section="activeSection" 
      @section-change="handleSectionChange"
      @sign-out="handleSignOut"
    />

    <!-- Top Navigation -->
    <nav class="bg-white/90 backdrop-blur-md border-b border-emerald-100 sticky top-0 z-50 lg:block hidden">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-16">
          <!-- Logo y nombre -->
          <div class="flex items-center space-x-4">
            <img src="../assets/E.png" alt="E-Signature Logo" class="w-10 h-10 rounded-xl shadow-lg" style="object-fit: contain;" />
            <div>
              <h1 class="text-xl font-bold text-slate-900">E-Signature</h1>
              <p class="text-xs text-slate-500">Firmas Electrónicas</p>
            </div>
          </div>
          <!-- Search -->
          <div class="hidden md:flex flex-1 max-w-md mx-8">
            <div class="relative w-full">
              <span class="absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400 w-4 h-4" v-html="renderIcon('search', 'w-4 h-4')"></span>
              <input placeholder="Buscar documentos..." class="pl-10 border-emerald-200 focus:border-emerald-400 focus:ring-emerald-400 rounded-md w-full py-2" />
            </div>
          </div>
          <!-- User Actions -->
          <div class="flex items-center space-x-4">
            <NotificationList />
            <button class="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded hidden sm:flex items-center" @click="activeSection = 'upload'">
              <span v-html="renderIcon('plus', 'w-4 h-4 mr-2')"></span>
              Nuevo
            </button>
            <div class="flex items-center space-x-3">
              <div class="w-8 h-8 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-700 font-bold">{{ getUserInitials() }}</div>
              <div class="hidden sm:block">
                <p class="text-sm font-medium text-slate-900">{{ getUserFullName() }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </nav>

    <!-- Side Navigation -->
    <div class="flex">
      <aside class="w-64 bg-white/60 backdrop-blur-sm border-r border-emerald-100 min-h-screen p-6 hidden lg:block">
        <nav class="space-y-2">
          <button :class="['w-full justify-start flex items-center px-4 py-2 rounded', activeSection === 'dashboard' ? 'bg-emerald-600 text-white' : 'hover:bg-emerald-50 text-slate-700']" @click="activeSection = 'dashboard'">
            <div class="w-5 h-5 mr-3 rounded bg-emerald-100 flex items-center justify-center"><div class="w-2 h-2 bg-emerald-600 rounded"></div></div>
            Dashboard
          </button>
          <button :class="['w-full justify-start flex items-center px-4 py-2 rounded', activeSection === 'sign' ? 'bg-emerald-600 text-white' : 'hover:bg-emerald-50 text-slate-700']" @click="activeSection = 'sign'">
            <span v-html="renderIcon('pen', 'w-5 h-5 mr-3')"></span>
            Firmar Documentos
          </button>
          <button :class="['w-full justify-start flex items-center px-4 py-2 rounded', activeSection === 'upload' ? 'bg-emerald-600 text-white' : 'hover:bg-emerald-50 text-slate-700']" @click="activeSection = 'upload'">
            <span v-html="renderIcon('upload', 'w-5 h-5 mr-3')"></span>
            Subir Archivos
          </button>
          <button :class="['w-full justify-start flex items-center px-4 py-2 rounded', activeSection === 'documents' ? 'bg-emerald-600 text-white' : 'hover:bg-emerald-50 text-slate-700']" @click="activeSection = 'documents'">
            <span v-html="renderIcon('file', 'w-5 h-5 mr-3')"></span>
            Mis Documentos
          </button>
          <button v-if="!isAdmin" :class="['w-full justify-start flex items-center px-4 py-2 rounded', activeSection === 'certificate-request' ? 'bg-emerald-600 text-white' : 'hover:bg-emerald-50 text-slate-700']" @click="activeSection = 'certificate-request'">
            <span v-html="renderIcon('shield', 'w-5 h-5 mr-3')"></span>
            Solicitar Certificado
          </button>
          <button v-if="isAdmin" :class="['w-full justify-start flex items-center px-4 py-2 rounded', activeSection === 'admin-requests' ? 'bg-emerald-600 text-white' : 'hover:bg-emerald-50 text-slate-700']" @click="activeSection = 'admin-requests'">
            <span v-html="renderIcon('users', 'w-5 h-5 mr-3')"></span>
            Gestionar Solicitudes
          </button>
          <button :class="['w-full justify-start flex items-center px-4 py-2 rounded', activeSection === 'group-signature' ? 'bg-emerald-600 text-white' : 'hover:bg-emerald-50 text-slate-700']" @click="activeSection = 'group-signature'">
            <span v-html="renderIcon('users', 'w-5 h-5 mr-3')"></span>
            Solicitar Firma Grupal
          </button>
          <button :class="['w-full justify-start flex items-center px-4 py-2 rounded', activeSection === 'pending-signature' ? 'bg-emerald-600 text-white' : 'hover:bg-emerald-50 text-slate-700']" @click="activeSection = 'pending-signature'">
            <span v-html="renderIcon('zap', 'w-5 h-5 mr-3')"></span>
            Solicitudes de Firma
          </button>
        </nav>
        <!-- User Card -->
        <div class="mt-8 border-0 bg-gradient-to-r from-emerald-500 to-teal-600 text-white rounded-xl p-4">
          <div class="flex items-center space-x-3">
            <div class="w-10 h-10 rounded-full bg-white/20 flex items-center justify-center text-white font-bold">{{ getUserInitials() }}</div>
            <div>
              <p class="font-medium">{{ getUserFullName() }}</p>
            </div>
          </div>
          <button class="w-full mt-3 text-white hover:bg-white/20 rounded py-2" @click="handleSignOut">Cerrar Sesión</button>
        </div>
      </aside>

      <!-- Main Content - Responsive para todos los dispositivos -->
      <main class="flex-1 p-4 lg:p-6 xl:p-8 pt-20 lg:pt-6">
        <div class="max-w-6xl mx-auto">
          <!-- Render Section Based on Active Section -->
          <UploadSection v-if="activeSection === 'upload'" />
          <SignDocuments v-else-if="activeSection === 'sign'" />
          <MyDocuments v-else-if="activeSection === 'documents'" />
          <RequestSignature v-else-if="activeSection === 'request-signature'" />
          <CertificateRequestForm v-else-if="activeSection === 'certificate-request'" />
          <AdminCertificateRequests v-else-if="activeSection === 'admin-requests'" />
          <RequestGroupSignature v-else-if="activeSection === 'group-signature'" />
          <PendingSignatureRequests v-else-if="activeSection === 'pending-signature'" />
          
          <!-- Dashboard Content - Se adapta automáticamente -->
          <template v-else>
            <!-- Acciones Rápidas -->
            <div class="space-y-6 lg:space-y-8">
              <div class="border-0 shadow-sm bg-white/80 backdrop-blur-sm rounded-xl">
                <div class="p-4 lg:p-6">
                  <h2 class="text-lg lg:text-xl text-slate-900 font-bold mb-1">Acciones Rápidas</h2>
                  <p class="text-slate-600 mb-4 text-sm lg:text-base">Accede rápidamente a las funciones principales</p>
                  <div class="grid grid-cols-2 lg:grid-cols-4 gap-3 lg:gap-4">
                    <button v-for="(action, index) in quickActions" :key="index" @click="action.action" class="h-20 lg:h-24 flex flex-col items-center justify-center space-y-2 border border-emerald-200 hover:bg-emerald-50 hover:border-emerald-300 transition-all duration-200 bg-transparent rounded-xl">
                      <span v-html="renderIcon(action.icon, 'w-5 h-5 lg:w-6 lg:h-6 text-emerald-600')"></span>
                      <span class="text-xs lg:text-sm font-medium text-slate-700 text-center px-1">{{ action.label }}</span>
                    </button>
                  </div>
                </div>
              </div>
              
              <!-- Documentos Pendientes -->
              <div class="border-0 shadow-sm bg-white/80 backdrop-blur-sm rounded-xl">
                <div class="p-4 lg:p-6 border-b border-emerald-100">
                  <h3 class="text-lg lg:text-xl font-semibold text-slate-900 mb-2">Documentos Pendientes de Firma</h3>
                  <p class="text-slate-600 text-sm">Documentos que requieren tu firma digital</p>
                </div>
                <div class="p-4 lg:p-6">
                  <div v-if="documents.filter(doc => !doc.signed && doc.status !== 'Firmado').length === 0" class="text-center py-8 lg:py-12">
                    <div class="w-16 h-16 lg:w-20 lg:h-20 bg-emerald-100 rounded-full flex items-center justify-center mx-auto mb-3 lg:mb-4">
                      <span v-html="renderIcon('file', 'w-8 h-8 lg:w-10 lg:h-10 text-emerald-600')"></span>
                    </div>
                    <p class="text-base lg:text-lg text-slate-700 mb-3 lg:mb-4 font-medium">No tienes documentos pendientes</p>
                    <button class="bg-emerald-600 hover:bg-emerald-700 text-white px-4 lg:px-6 py-2 lg:py-3 rounded-lg text-sm lg:text-base font-medium shadow-lg" @click="activeSection = 'upload'">Subir Documento</button>
                  </div>
                  <div v-else class="space-y-3 lg:space-y-4">
                    <div v-for="doc in documents.filter(doc => !doc.signed && doc.status !== 'Firmado').slice(0, 5)" :key="doc.id" class="flex items-center justify-between p-3 lg:p-4 rounded-lg bg-slate-50 hover:bg-slate-100 transition-colors border border-emerald-100">
                      <div class="flex items-center space-x-3 lg:space-x-4">
                        <div class="w-10 h-10 lg:w-12 lg:h-12 bg-emerald-100 rounded-lg flex items-center justify-center">
                          <span v-html="renderIcon('file', 'w-5 h-5 lg:w-6 lg:h-6 text-emerald-600')"></span>
                        </div>
                        <div>
                          <p class="font-medium text-slate-900 text-sm lg:text-base">{{ doc.fileName || doc.name || 'Documento sin nombre' }}</p>
                          <p class="text-xs lg:text-sm text-slate-600">{{ doc.uploadDate ? new Date(doc.uploadDate).toLocaleDateString() : 'Fecha no disponible' }}</p>
                        </div>
                      </div>
                      <div class="flex items-center space-x-2 lg:space-x-3">
                        <span class="bg-amber-100 text-amber-700 px-2 lg:px-3 py-1 rounded-full text-xs font-semibold">Pendiente</span>
                        <button @click="activeSection = 'sign'" class="bg-emerald-600 hover:bg-emerald-700 text-white px-3 lg:px-4 py-1 lg:py-2 rounded-lg text-xs lg:text-sm font-medium transition-colors">
                          Firmar Ahora
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- Documentos Recientemente Firmados -->
              <div class="border-0 shadow-sm bg-white/80 backdrop-blur-sm rounded-xl">
                <div class="p-4 lg:p-6 border-b border-emerald-100">
                  <h3 class="text-lg lg:text-xl font-semibold text-slate-900 mb-2">Documentos Recientemente Firmados</h3>
                  <p class="text-slate-600 text-sm">Tus documentos firmados más recientes</p>
                </div>
                <div class="p-4 lg:p-6">
                  <div v-if="documents.filter(doc => doc.signed || doc.status === 'Firmado').length === 0" class="text-center py-8 lg:py-12">
                    <div class="w-16 h-16 lg:w-20 lg:h-20 bg-emerald-100 rounded-full flex items-center justify-center mx-auto mb-3 lg:mb-4">
                      <span v-html="renderIcon('file', 'w-8 h-8 lg:w-10 lg:h-10 text-emerald-600')"></span>
                    </div>
                    <p class="text-base lg:text-lg text-slate-700 mb-3 lg:mb-4 font-medium">No hay documentos firmados</p>
                    <p class="text-slate-500 text-sm lg:text-base">Los documentos que firmes aparecerán aquí</p>
                  </div>
                  <div v-else class="space-y-3 lg:space-y-4">
                    <div v-for="doc in documents.filter(doc => doc.signed || doc.status === 'Firmado').slice(0, 3)" :key="doc.id" class="flex items-center justify-between p-3 lg:p-4 rounded-lg bg-slate-50 hover:bg-slate-100 transition-colors border border-emerald-100">
                      <div class="flex items-center space-x-3 lg:space-x-4">
                        <div class="w-10 h-10 lg:w-12 lg:h-12 bg-emerald-100 rounded-lg flex items-center justify-center">
                          <span v-html="renderIcon('file', 'w-5 h-5 lg:w-6 lg:h-6 text-emerald-600')"></span>
                        </div>
                        <div>
                          <p class="font-medium text-slate-900 text-sm lg:text-base">{{ doc.fileName || doc.name || 'Documento sin nombre' }}</p>
                          <p class="text-xs lg:text-sm text-slate-600">{{ doc.uploadDate ? new Date(doc.uploadDate).toLocaleDateString() : 'Fecha no disponible' }}</p>
                        </div>
                      </div>
                      <div class="flex items-center space-x-2 lg:space-x-3">
                        <span class="bg-emerald-100 text-emerald-700 px-2 lg:px-3 py-1 rounded-full text-xs font-semibold">Firmado</span>
                        <button @click="activeSection = 'documents'" class="text-emerald-600 hover:text-emerald-700 font-medium text-xs lg:text-sm">
                          Ver Detalles
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </main>
    </div>

    <!-- Componente de notificaciones -->
    <NotificationToast />
    <NotificationList />
  </div>
</template>