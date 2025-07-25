<script setup>
import { ref, onMounted } from 'vue';
import UploadSection from '../components/UploadSection.vue';
import SignDocuments from '../components/SignDocuments.vue';
import MyDocuments from '../components/MyDocuments.vue';
import RequestSignature from '../components/RequestSignature.vue';
import { authState } from '../service/Auth';

const activeSection = ref('dashboard');
const isLoggedIn = ref(true); // Simulación de login

const stats = [
  { label: 'Documentos Firmados', value: '24', change: '+12%' },
  { label: 'Pendientes', value: '3', change: '+2' },
  { label: 'Certificados', value: '2', change: 'Activos' },
  { label: 'Este Mes', value: '18', change: '+8%' },
];

const quickActions = [
  { icon: 'upload', label: 'Subir Documento', action: () => (activeSection.value = 'upload') },
  { icon: 'pen', label: 'Firmar Ahora', action: () => (activeSection.value = 'sign') },
  { icon: 'users', label: 'Solicitar Firma', action: () => (activeSection.value = 'request') },
  { icon: 'file', label: 'Ver Documentos', action: () => (activeSection.value = 'documents') },
];

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

onMounted(() => {
  window.addEventListener('go-to-request-signature', () => {
    activeSection.value = 'request';
  });
});
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-emerald-50 via-white to-teal-50">
    <!-- Top Navigation -->
    <nav class="bg-white/90 backdrop-blur-md border-b border-emerald-100 sticky top-0 z-50">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-16">
          <!-- Logo y nombre -->
          <div class="flex items-center space-x-3">
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
            <button class="relative bg-transparent p-2 rounded-full hover:bg-emerald-50">
              <span v-html="renderIcon('bell', 'w-5 h-5 text-slate-600')"></span>
              <span class="absolute -top-1 -right-1 w-5 h-5 p-0 bg-emerald-500 text-white text-xs flex items-center justify-center rounded-full">3</span>
            </button>
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
          <button :class="['w-full justify-start flex items-center px-4 py-2 rounded', activeSection === 'request' ? 'bg-emerald-600 text-white' : 'hover:bg-emerald-50 text-slate-700']" @click="activeSection = 'request'">
            <span v-html="renderIcon('users', 'w-5 h-5 mr-3')"></span>
            Solicitar Firma
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
          <button class="w-full mt-3 text-white hover:bg-white/20 rounded py-2" @click="isLoggedIn = false">Cerrar Sesión</button>
        </div>
      </aside>
      <!-- Main Content -->
      <main class="flex-1 p-6 lg:p-8">
        <div class="max-w-6xl mx-auto">
          <template v-if="activeSection === 'dashboard'">
            <!-- Acciones Rápidas -->
            <div class="space-y-8">
              <div class="border-0 shadow-sm bg-white/80 backdrop-blur-sm rounded-xl">
                <div class="p-6">
                  <h2 class="text-xl text-slate-900 font-bold mb-1">Acciones Rápidas</h2>
                  <p class="text-slate-600 mb-4">Accede rápidamente a las funciones principales</p>
                  <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
                    <button v-for="(action, index) in quickActions" :key="index" @click="action.action" class="h-24 flex flex-col items-center justify-center space-y-2 border border-emerald-200 hover:bg-emerald-50 hover:border-emerald-300 transition-all duration-200 bg-transparent rounded-xl">
                      <span v-html="renderIcon(action.icon, 'w-6 h-6 text-emerald-600')"></span>
                      <span class="text-sm font-medium text-slate-700">{{ action.label }}</span>
                    </button>
                  </div>
                </div>
              </div>
              <!-- Documentos Pendientes grande -->
              <div class="border-0 shadow-sm bg-white/80 backdrop-blur-sm rounded-xl flex flex-col items-center justify-center py-16">
                <div class="flex flex-col items-center">
                  <div class="w-24 h-24 bg-emerald-100 rounded-full flex items-center justify-center mb-6">
                    <span v-html="renderIcon('file', 'w-16 h-16 text-emerald-600')"></span>
                  </div>
                  <p class="text-2xl text-slate-700 mb-4 font-semibold">No tienes documentos pendientes</p>
                  <button class="bg-emerald-600 hover:bg-emerald-700 text-white px-8 py-4 rounded text-lg font-bold shadow-lg" @click="activeSection = 'upload'">Subir Documento</button>
                </div>
              </div>
            </div>
          </template>
          <UploadSection v-else-if="activeSection === 'upload'" />
          <SignDocuments v-else-if="activeSection === 'sign'" />
          <MyDocuments v-else-if="activeSection === 'documents'" />
          <RequestSignature v-else-if="activeSection === 'request'" />
        </div>
      </main>
    </div>
    <!-- Mobile Navigation -->
    <div class="lg:hidden fixed bottom-0 left-0 right-0 bg-white/90 backdrop-blur-md border-t border-emerald-100 p-4">
      <div class="flex justify-around">
        <button :class="['flex-col space-y-1 items-center', activeSection === 'dashboard' ? 'text-emerald-600' : 'text-slate-600']" @click="activeSection = 'dashboard'">
          <div class="w-6 h-6 rounded bg-emerald-100 flex items-center justify-center"><div class="w-2 h-2 bg-emerald-600 rounded"></div></div>
          <span class="text-xs">Inicio</span>
        </button>
        <button :class="['flex-col space-y-1 items-center', activeSection === 'sign' ? 'text-emerald-600' : 'text-slate-600']" @click="activeSection = 'sign'">
          <span v-html="renderIcon('pen', 'w-6 h-6')"></span>
          <span class="text-xs">Firmar</span>
        </button>
        <button :class="['flex-col space-y-1 items-center', activeSection === 'upload' ? 'text-emerald-600' : 'text-slate-600']" @click="activeSection = 'upload'">
          <span v-html="renderIcon('upload', 'w-6 h-6')"></span>
          <span class="text-xs">Subir</span>
        </button>
        <button :class="['flex-col space-y-1 items-center', activeSection === 'documents' ? 'text-emerald-600' : 'text-slate-600']" @click="activeSection = 'documents'">
          <span v-html="renderIcon('file', 'w-6 h-6')"></span>
          <span class="text-xs">Docs</span>
        </button>
      </div>
    </div>
  </div>
</template>