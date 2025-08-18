<template>
  <div class="bg-white/90 rounded-xl shadow p-8">
    <div class="text-center mb-8">
      <div class="w-16 h-16 bg-emerald-100 rounded-full flex items-center justify-center mx-auto mb-4">
        <svg class="w-8 h-8 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 22s8-4 8-10V5.236A2.236 2.236 0 0017.764 3H6.236A2.236 2.236 0 004 5.236V12c0 6 8 10 8 10z"/>
        </svg>
      </div>
      <h2 class="text-2xl font-bold text-slate-900 mb-2">Solicitar Certificado Digital</h2>
      <p class="text-slate-600">Completa el formulario para solicitar tu certificado digital</p>
    </div>

    <form @submit.prevent="handleSubmit" class="space-y-6">
      <div class="grid md:grid-cols-2 gap-6">
        <div class="space-y-2">
          <label for="nombre" class="text-slate-700 font-medium">Nombre Completo</label>
          <input
            v-model="form.nombre"
            id="nombre"
            type="text"
            placeholder="Tu nombre completo"
            required
            class="w-full border border-emerald-200 rounded-md py-3 px-4 focus:ring-emerald-400 focus:border-emerald-400"
          />
        </div>
        
        <div class="space-y-2">
          <label for="correo" class="text-slate-700 font-medium">Correo Electrónico</label>
          <input
            v-model="form.correo"
            id="correo"
            type="email"
            placeholder="tu@email.com"
            required
            readonly
            class="w-full border border-emerald-200 rounded-md py-3 px-4 bg-slate-50 text-slate-600 cursor-not-allowed"
          />
        </div>
      </div>

      <div class="space-y-2">
        <label for="organizacion" class="text-slate-700 font-medium">Organización</label>
        <input
          v-model="form.organizacion"
          id="organizacion"
          type="text"
          placeholder="Nombre de tu empresa u organización"
          required
          class="w-full border border-emerald-200 rounded-md py-3 px-4 focus:ring-emerald-400 focus:border-emerald-400"
        />
      </div>

      <div class="space-y-2">
        <label for="password" class="text-slate-700 font-medium">Contraseña del Certificado</label>
        <div class="relative">
          <input
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            id="password"
            placeholder="Crea una contraseña segura para tu certificado"
            required
            class="w-full border border-emerald-200 rounded-md py-3 px-4 pr-10 focus:ring-emerald-400 focus:border-emerald-400"
            @input="validatePasswordInput"
          />
          <button
            type="button"
            @click="showPassword = !showPassword"
            class="absolute right-3 top-1/2 transform -translate-y-1/2 text-slate-400 hover:text-slate-600"
          >
            <svg v-if="showPassword" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21"/>
            </svg>
            <svg v-else class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
            </svg>
          </button>
        </div>
        
        <!-- Indicador de Fortaleza de Contraseña -->
        <div v-if="form.password" class="bg-gray-50 border border-gray-200 rounded-lg p-4">
          <div class="flex items-center justify-between mb-3">
            <span class="text-sm font-medium text-gray-700">Fortaleza de la contraseña:</span>
            <span :class="['text-sm font-semibold', passwordValidation.color]">{{ passwordValidation.strength }}</span>
          </div>
          
          <!-- Barra de progreso -->
          <div class="w-full bg-gray-200 rounded-full h-2 mb-3">
            <div 
              :class="['h-2 rounded-full transition-all duration-300', getStrengthColor(passwordValidation.score)]"
              :style="{ width: passwordValidation.score + '%' }"
            ></div>
          </div>
          
          <!-- Puntuación -->
          <div class="text-xs text-gray-500 mb-3">
            Puntuación: {{ passwordValidation.score }}/100
          </div>
          
          <!-- Requisitos -->
          <div class="space-y-2">
            <div class="flex items-center text-sm">
              <span :class="['w-4 h-4 mr-2 rounded-full flex items-center justify-center', passwordValidation.requirements.minLength ? 'bg-green-500' : 'bg-red-500']">
                <svg v-if="passwordValidation.requirements.minLength" class="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                </svg>
                <svg v-else class="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
                </svg>
              </span>
              Mínimo 8 caracteres
            </div>
            <div class="flex items-center text-sm">
              <span :class="['w-4 h-4 mr-2 rounded-full flex items-center justify-center', passwordValidation.requirements.hasUppercase ? 'bg-green-500' : 'bg-red-500']">
                <svg v-if="passwordValidation.requirements.hasUppercase" class="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                </svg>
                <svg v-else class="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
                </svg>
              </span>
              Al menos 1 letra mayúscula
            </div>
            <div class="flex items-center text-sm">
              <span :class="['w-4 h-4 mr-2 rounded-full flex items-center justify-center', passwordValidation.requirements.hasLowercase ? 'bg-green-500' : 'bg-red-500']">
                <svg v-if="passwordValidation.requirements.hasLowercase" class="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                </svg>
                <svg v-else class="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
                </svg>
              </span>
              Al menos 1 letra minúscula
            </div>
            <div class="flex items-center text-sm">
              <span :class="['w-4 h-4 mr-2 rounded-full flex items-center justify-center', passwordValidation.requirements.hasNumber ? 'bg-green-500' : 'bg-red-500']">
                <svg v-if="passwordValidation.requirements.hasNumber" class="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                </svg>
                <svg v-else class="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
                </svg>
              </span>
              Al menos 1 número
            </div>
            <div class="flex items-center text-sm">
              <span :class="['w-4 h-4 mr-2 rounded-full flex items-center justify-center', passwordValidation.requirements.hasSpecialChar ? 'bg-green-500' : 'bg-red-500']">
                <svg v-if="passwordValidation.requirements.hasSpecialChar" class="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                </svg>
                <svg v-else class="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
                </svg>
              </span>
              Al menos 1 símbolo especial (!@#$%^&*)
            </div>
          </div>
          
          <!-- Sugerencias -->
          <div v-if="passwordValidation.feedback.length > 0" class="mt-3 p-2 bg-yellow-50 border border-yellow-200 rounded">
            <p class="text-xs text-yellow-800 font-medium mb-1">Sugerencias para mejorar:</p>
            <ul class="text-xs text-yellow-700 space-y-1">
              <li v-for="suggestion in passwordValidation.feedback" :key="suggestion" class="flex items-center">
                <span class="w-1 h-1 bg-yellow-500 rounded-full mr-2"></span>
                {{ suggestion }}
              </li>
            </ul>
          </div>
        </div>
        
        <p class="text-sm text-slate-500">Esta contraseña protegerá tu certificado digital</p>
      </div>

      <div class="bg-amber-50 border border-amber-200 rounded-lg p-4">
        <div class="flex items-start space-x-3">
          <svg class="w-5 h-5 text-amber-500 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z"/>
          </svg>
          <div>
            <h4 class="text-sm font-medium text-amber-800">Información Importante</h4>
            <p class="text-sm text-amber-700 mt-1">
              Tu solicitud será revisada por un administrador. Recibirás una notificación cuando sea aprobada o rechazada.
            </p>
          </div>
        </div>
      </div>

      <button
        type="submit"
        :disabled="loading || !passwordValidation.isValid"
        class="w-full bg-emerald-600 hover:bg-emerald-700 disabled:bg-emerald-400 text-white font-medium py-3 px-6 rounded-lg transition-colors flex items-center justify-center"
      >
        <svg v-if="loading" class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        {{ loading ? 'Enviando solicitud...' : 'Enviar Solicitud' }}
      </button>
    </form>

    <!-- Mensajes de estado -->
    <div v-if="message" class="mt-6 p-4 rounded-lg" :class="messageType === 'success' ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'">
      <div class="flex items-center space-x-3">
        <svg v-if="messageType === 'success'" class="w-5 h-5 text-green-500" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd" />
        </svg>
        <svg v-else class="w-5 h-5 text-red-500" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
        </svg>
        <p class="text-sm" :class="messageType === 'success' ? 'text-green-800' : 'text-red-800'">{{ message }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { createCertificateRequest } from '../utils/api';
import { authState } from '../service/Auth';
import { validatePassword } from '../utils/password-validation';

const loading = ref(false);
const showPassword = ref(false);
const message = ref('');
const messageType = ref<'success' | 'error'>('success');

const form = reactive({
  nombre: '',
  correo: '',
  organizacion: '',
  password: ''
});

// Validación de contraseña en tiempo real
const passwordValidation = computed(() => {
  return validatePassword(form.password);
});

// Función para validar la contraseña cuando se escribe
const validatePasswordInput = () => {
  // La validación se ejecuta automáticamente por el computed
  // Solo necesitamos asegurarnos de que se actualice la UI
};

// Función para obtener el color de la barra de progreso
const getStrengthColor = (score: number): string => {
  if (score < 40) return 'bg-red-500';
  if (score < 60) return 'bg-orange-500';
  if (score < 80) return 'bg-yellow-500';
  if (score < 90) return 'bg-blue-500';
  return 'bg-green-500';
};

// Cargar el correo del usuario actual al montar el componente
onMounted(() => {
  if (authState.user?.email) {
    form.correo = authState.user.email;
  }
});

const handleSubmit = async () => {
  // Validar que la contraseña cumpla con los requisitos
  if (!passwordValidation.value.isValid) {
    message.value = 'La contraseña no cumple con los requisitos de seguridad. Por favor, mejórala antes de continuar.';
    messageType.value = 'error';
    return;
  }

  loading.value = true;
  message.value = '';
  
  try {
    await createCertificateRequest(form);
    message.value = 'Solicitud enviada correctamente. Recibirás una notificación cuando sea procesada.';
    messageType.value = 'success';
    
    // Limpiar formulario
    form.nombre = '';
    form.correo = '';
    form.organizacion = '';
    form.password = '';
    
  } catch (error: any) {
    message.value = error.response?.data?.message || 'Error al enviar la solicitud. Intenta de nuevo.';
    messageType.value = 'error';
  } finally {
    loading.value = false;
  }
};
</script> 