<template>
  <div class="lg:hidden">
    <!-- Botón de menú móvil -->
    <button
      @click="toggleMobileMenu"
      class="fixed top-4 right-4 z-50 p-2 bg-white rounded-lg shadow-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-primary-500"
      :class="{ 'bg-primary-50': isMobileMenuOpen }"
    >
      <svg
        v-if="!isMobileMenuOpen"
        class="w-6 h-6 text-gray-700"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M4 6h16M4 12h16M4 18h16"
        />
      </svg>
      <svg
        v-else
        class="w-6 h-6 text-gray-700"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M6 18L18 6M6 6l12 12"
        />
      </svg>
    </button>

    <!-- Menú móvil -->
    <div
      v-if="isMobileMenuOpen"
      class="fixed inset-0 z-40 bg-black bg-opacity-50 transition-opacity"
      @click="closeMobileMenu"
    ></div>

    <div
      :class="[
        'fixed top-0 right-0 z-50 h-full w-80 bg-white shadow-2xl transform transition-transform duration-300 ease-in-out',
        isMobileMenuOpen ? 'translate-x-0' : 'translate-x-full'
      ]"
    >
      <!-- Header del menú móvil -->
      <div class="flex items-center justify-between p-6 border-b border-gray-200">
        <div class="flex items-center space-x-3">
          <img
            src="../assets/E.png"
            alt="E-Signature Logo"
            class="w-10 h-10 rounded-xl"
          />
          <span class="text-lg font-semibold text-gray-900">E-Signature</span>
        </div>
        <button
          @click="closeMobileMenu"
          class="p-2 text-gray-400 hover:text-gray-600 focus:outline-none"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M6 18L18 6M6 6l12 12"
            />
          </svg>
        </button>
      </div>

      <!-- Contenido del menú -->
      <div class="flex-1 overflow-y-auto py-6">
        <!-- Información del usuario -->
        <div class="px-6 mb-6">
          <div class="flex items-center space-x-3 mb-4">
            <div class="w-12 h-12 bg-primary-100 rounded-full flex items-center justify-center">
              <span class="text-lg font-semibold text-primary-700">
                {{ getUserInitials() }}
              </span>
            </div>
            <div>
              <p class="font-medium text-gray-900">{{ getUserFullName() }}</p>
              <p class="text-sm text-gray-500">{{ authState.user?.email }}</p>
            </div>
          </div>
        </div>

        <!-- Navegación -->
        <nav class="px-6 space-y-2">
          <button
            v-for="action in mobileActions"
            :key="action.label"
            @click="handleAction(action)"
            class="w-full flex items-center space-x-3 px-4 py-3 text-left text-gray-700 hover:bg-primary-50 hover:text-primary-700 rounded-lg transition-colors duration-200"
            :class="{ 'bg-primary-50 text-primary-700': activeSection === action.section }"
          >
            <div class="w-5 h-5 text-gray-400" v-html="renderIcon(action.icon, 'w-5 h-5')"></div>
            <span class="font-medium">{{ action.label }}</span>
          </button>
        </nav>

        <!-- Separador -->
        <div class="px-6 my-6">
          <hr class="border-gray-200" />
        </div>

        <!-- Acciones adicionales -->
        <div class="px-6 space-y-2">
          <button
            @click="handleSignOut"
            class="w-full flex items-center space-x-3 px-4 py-3 text-left text-red-600 hover:bg-red-50 rounded-lg transition-colors duration-200"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
              />
            </svg>
            <span class="font-medium">Cerrar Sesión</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { authState, authService } from '../service/Auth';

const props = defineProps({
  activeSection: {
    type: String,
    required: true
  }
});

const emit = defineEmits(['section-change', 'sign-out']);

const isMobileMenuOpen = ref(false);

const mobileActions = computed(() => {
  const baseActions = [
    { icon: 'home', label: 'Dashboard', section: 'dashboard', action: () => emit('section-change', 'dashboard') },
    { icon: 'upload', label: 'Subir Documento', section: 'upload', action: () => emit('section-change', 'upload') },
    { icon: 'pen', label: 'Firmar Documentos', section: 'sign', action: () => emit('section-change', 'sign') },
    { icon: 'file', label: 'Mis Documentos', section: 'documents', action: () => emit('section-change', 'documents') },
    { icon: 'users', label: 'Solicitar Firma', section: 'request-signature', action: () => emit('section-change', 'request-signature') },
    { icon: 'users', label: 'Firma Grupal', section: 'group-signature', action: () => emit('section-change', 'group-signature') },
    { icon: 'bell', label: 'Solicitudes Pendientes', section: 'pending-signature', action: () => emit('section-change', 'pending-signature') },
  ];

  // Agregar acciones específicas según el rol
  if (authState.user?.role !== 'ADMIN') {
    baseActions.push({ 
      icon: 'shield', 
      label: 'Solicitar Certificado', 
      section: 'certificate-request', 
      action: () => emit('section-change', 'certificate-request') 
    });
  }

  if (authState.user?.role === 'ADMIN') {
    baseActions.push({ 
      icon: 'users', 
      label: 'Gestionar Solicitudes', 
      section: 'admin-requests', 
      action: () => emit('section-change', 'admin-requests') 
    });
  }

  return baseActions;
});

function renderIcon(icon, classes = '') {
  const iconMap = {
    'home': `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"/></svg>`,
    'upload': `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1M4 12l8-8m0 0l8 8m-8-8v12"/></svg>`,
    'pen': `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16.862 3.487a2.6 2.6 0 113.677 3.677L7.5 20.205l-4 1 1-4 12.362-13.718z"/></svg>`,
    'file': `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7V3a1 1 0 011-1h8a1 1 0 011 1v4m-2 4h2a2 2 0 012 2v7a2 2 0 01-2 2H7a2 2 0 01-2-2v-7a2 2 0 012-2h2m2-4v4m0 0H7m4 0h4"/></svg>`,
    'users': `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a4 4 0 00-3-3.87M9 20H4v-2a4 4 0 013-3.87m9-7a4 4 0 11-8 0 4 4 0 018 0zm6 8a4 4 0 00-3-3.87M6 10a4 4 0 100-8 4 4 0 000 8z"/></svg>`,
    'bell': `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/></svg>`,
    'shield': `<svg class="${classes}" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/></svg>`,
  };
  return iconMap[icon] || '';
}

function getUserInitials() {
  if (!authState.user) return '';
  const { firstName, lastName } = authState.user;
  return `${(firstName?.[0] || '').toUpperCase()}${(lastName?.[0] || '').toUpperCase()}`;
}

function getUserFullName() {
  if (!authState.user) return '';
  const { firstName, lastName } = authState.user;
  return `${firstName || ''} ${lastName || ''}`.trim();
}

function toggleMobileMenu() {
  isMobileMenuOpen.value = !isMobileMenuOpen.value;
}

function closeMobileMenu() {
  isMobileMenuOpen.value = false;
}

function handleAction(action) {
  action.action();
  closeMobileMenu();
}

function handleSignOut() {
  emit('sign-out');
  closeMobileMenu();
}
</script>
