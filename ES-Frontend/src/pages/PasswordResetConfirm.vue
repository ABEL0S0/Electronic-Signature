<script setup lang="ts">
import { ref } from "vue";
import { resetPassword } from "../utils/api";

const email = ref("");
const code = ref("");
const newPassword = ref("");
const confirmPassword = ref("");
const errorMessage = ref("");
const successMessage = ref("");

// Load email from localStorage
const pendingEmail = localStorage.getItem('passwordResetEmail');
if (pendingEmail) {
  email.value = pendingEmail;
}

const handleConfirm = async () => {
  errorMessage.value = '';
  successMessage.value = '';
  if (!code.value || !newPassword.value || !confirmPassword.value) {
    errorMessage.value = 'Por favor completa todos los campos.';
    return;
  }
  if (newPassword.value !== confirmPassword.value) {
    errorMessage.value = 'Las contraseñas no coinciden.';
    return;
  }
  try {
    await resetPassword(email.value, code.value, newPassword.value);
    successMessage.value = 'Contraseña actualizada. Redirigiendo al login...';
    localStorage.removeItem('passwordResetEmail');
    setTimeout(() => window.location.hash = '/', 1500);
  } catch (err: any) {
    errorMessage.value = err.response?.data || 'Error al restablecer contraseña.';
  }
};
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-emerald-50 via-white to-teal-50 flex items-center justify-center p-4">
    <div class="w-full max-w-md">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-slate-900 mb-3">Nueva Contraseña</h1>
        <p class="text-slate-500">Ingresa el código y tu nueva contraseña</p>
      </div>
      <form @submit.prevent="handleConfirm" class="bg-white/80 backdrop-blur-sm rounded-2xl shadow-2xl p-8 space-y-6">
        <div class="space-y-2">
          <label for="code" class="text-slate-700 font-medium">Código de Recuperación</label>
          <input
            v-model="code"
            id="code"
            type="text"
            placeholder="Código de recuperación"
            required
            class="w-full h-12 border border-emerald-200 rounded-md px-3 shadow-sm focus:ring-emerald-400 focus:border-emerald-400"
          />
        </div>
        <div class="space-y-2">
          <label for="newPassword" class="text-slate-700 font-medium">Nueva Contraseña</label>
          <input
            v-model="newPassword"
            id="newPassword"
            type="password"
            placeholder="Nueva contraseña"
            required
            class="w-full h-12 border border-emerald-200 rounded-md px-3 shadow-sm focus:ring-emerald-400 focus:border-emerald-400"
          />
        </div>
        <div class="space-y-2">
          <label for="confirmPassword" class="text-slate-700 font-medium">Confirmar Contraseña</label>
          <input
            v-model="confirmPassword"
            id="confirmPassword"
            type="password"
            placeholder="Confirma tu contraseña"
            required
            class="w-full h-12 border border-emerald-200 rounded-md px-3 shadow-sm focus:ring-emerald-400 focus:border-emerald-400"
          />
        </div>
        <button
          type="submit"
          class="w-full h-12 bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-600 hover:to-teal-700 text-white font-medium rounded-md shadow-lg"
        >
          Actualizar Contraseña
        </button>
        <div v-if="successMessage" class="text-green-600 text-center text-sm">{{ successMessage }}</div>
        <div v-if="errorMessage" class="text-red-600 text-center text-sm">{{ errorMessage }}</div>
      </form>
    </div>
  </div>
</template>
