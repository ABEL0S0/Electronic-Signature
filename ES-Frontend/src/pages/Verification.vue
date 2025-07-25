<script setup lang="ts">
import { ref } from "vue";
import { verifyAccount } from "../service/api";
import { useRoute } from "vue-router";

const email = ref("");
const code = ref("");
const errorMessage = ref("");
const successMessage = ref("");
const route = useRoute();

// Obtener email del localStorage o de la query
const pendingEmail = localStorage.getItem('pendingVerificationEmail');
if (pendingEmail) {
  email.value = pendingEmail;
} else if (route && route.query && route.query.email) {
  email.value = route.query.email as string;
}

const handleVerify = async () => {
  errorMessage.value = "";
  successMessage.value = "";

  if (!code.value) {
    errorMessage.value = "Por favor ingresa el código de verificación.";
    return;
  }

  try {
    await verifyAccount(email.value, code.value);
    successMessage.value = "¡Cuenta verificada correctamente! Redirigiendo al login...";
    // Limpiar el email del localStorage después de verificación exitosa
    localStorage.removeItem('pendingVerificationEmail');
    setTimeout(() => {
      window.location.hash = "/";
    }, 1500);
  } catch (err) {
    errorMessage.value = "Código inválido. Intenta de nuevo.";
  }
};

const goToLogin = () => {
  // Limpiar el email del localStorage si el usuario decide volver al login
  localStorage.removeItem('pendingVerificationEmail');
  window.location.hash = "/";
};
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-emerald-50 via-white to-teal-50 flex items-center justify-center p-4">
    <div class="w-full max-w-md">
      <div class="text-center mb-8">
        <img src="../assets/E.png" alt="E-Signature Logo" class="mx-auto mb-6 rounded-3xl shadow-lg" style="width: 80px; height: 80px; object-fit: contain;" />
        <h1 class="text-3xl font-bold text-slate-900 mb-3">E-Signature</h1>
        <p class="text-slate-600 text-lg">Verifica tu cuenta</p>
        <p class="text-slate-500">Ingresa el código enviado a tu correo</p>
      </div>
      <div class="bg-white/80 backdrop-blur-sm rounded-2xl shadow-2xl p-8">
        <form @submit.prevent="handleVerify" class="space-y-6">
          <div class="space-y-2" v-if="!email">
            <label for="email" class="text-slate-700 font-medium">Correo Electrónico</label>
            <div class="relative">
              <svg class="absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400 w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25H4.5a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-.876 1.797l-7.5 5.625a2.25 2.25 0 01-2.748 0l-7.5-5.625A2.25 2.25 0 012.25 6.993V6.75"/></svg>
              <input v-model="email" id="email" type="email" placeholder="tu@email.com" required class="pl-12 h-12 border-emerald-200 focus:border-emerald-400 focus:ring-emerald-400 w-full rounded-md" />
            </div>
          </div>
          <div v-else class="space-y-2">
            <label class="text-slate-700 font-medium">Correo Electrónico</label>
            <div class="relative">
              <svg class="absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400 w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25H4.5a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-.876 1.797l-7.5 5.625a2.25 2.25 0 01-2.748 0l-7.5-5.625A2.25 2.25 0 012.25 6.993V6.75"/></svg>
              <input :value="email" disabled class="pl-12 h-12 border-emerald-200 bg-slate-50 text-slate-600 w-full rounded-md" />
            </div>
          </div>
          <div class="space-y-2">
            <label for="code" class="text-slate-700 font-medium">Código de Verificación</label>
            <div class="relative">
              <svg class="absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400 w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 11c0-1.104.896-2 2-2s2 .896 2 2-.896 2-2 2-2-.896-2-2zm0 0V7m0 4v4" /></svg>
              <input v-model="code" id="code" type="text" placeholder="Código de verificación" required class="pl-12 h-12 border-emerald-200 focus:border-emerald-400 focus:ring-emerald-400 w-full rounded-md" />
            </div>
          </div>
          <button type="submit" class="w-full h-12 bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-600 hover:to-teal-700 text-white font-medium rounded-md shadow-lg">Verificar Cuenta</button>
          <div v-if="successMessage" class="text-center text-green-600 text-sm mt-2">{{ successMessage }}</div>
          <div v-if="errorMessage" class="text-center text-red-600 text-sm mt-2">{{ errorMessage }}</div>
        </form>
        <div class="my-6 flex items-center">
          <div class="flex-grow border-t border-emerald-100"></div>
        </div>
        <div class="text-center">
          <p class="text-sm text-slate-600 mb-4">¿Ya tienes cuenta?</p>
          <button @click="goToLogin" class="w-full h-12 border border-emerald-200 text-emerald-600 hover:text-white hover:bg-gradient-to-r hover:from-emerald-500 hover:to-teal-600 font-medium rounded-md transition">Volver al inicio de sesión</button>
        </div>
      </div>
      <div class="text-center mt-8">
        <p class="text-xs text-slate-500">© 2025 E-Signature. Plataforma segura de firmas electrónicas.</p>
      </div>
    </div>
  </div>
</template>
