<script setup lang="ts">
import { ref } from 'vue';
import { requestPasswordReset } from '../utils/api';

const email = ref('');
const errorMessage = ref('');
const successMessage = ref('');

const handleRequest = async () => {
  errorMessage.value = '';
  successMessage.value = '';
  if (!email.value) {
    errorMessage.value = 'Por favor ingresa tu correo electrónico';
    return;
  }
  try {
    await requestPasswordReset(email.value);
    successMessage.value = 'Código enviado. Revisa tu correo.';
    localStorage.setItem('passwordResetEmail', email.value);
    setTimeout(() => {
      window.location.hash = '/password-reset';
    }, 1500);
  } catch (error: any) {
    // Mostrar mensaje de error detallado, si existe
    errorMessage.value = error.response?.data?.message || error.response?.data || error.message || 'Error al enviar código';
  }
};
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-emerald-50 via-white to-teal-50 flex items-center justify-center p-4">
    <div class="w-full max-w-md">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-slate-900 mb-3">Restablecer Contraseña</h1>
        <p class="text-slate-500">Ingresa tu correo para recibir un código de recuperación</p>
      </div>
      <form @submit.prevent="handleRequest" class="bg-white/80 backdrop-blur-sm rounded-2xl shadow-2xl p-8 space-y-6">
        <div class="space-y-2">
          <label for="email" class="text-slate-700 font-medium">Correo Electrónico</label>
          <input
            v-model="email"
            id="email"
            type="email"
            placeholder="tu@email.com"
            required
            class="w-full h-12 border border-emerald-200 rounded-md px-3 shadow-sm focus:ring-emerald-400 focus:border-emerald-400"
          />
        </div>
        <button
          type="submit"
          class="w-full h-12 bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-600 hover:to-teal-700 text-white font-medium rounded-md shadow-lg"
        >
          Enviar Código
        </button>
        <div v-if="successMessage" class="text-green-600 text-center text-sm">{{ successMessage }}</div>
        <div v-if="errorMessage" class="text-red-600 text-center text-sm">{{ errorMessage }}</div>
      </form>
    </div>
  </div>
</template>
