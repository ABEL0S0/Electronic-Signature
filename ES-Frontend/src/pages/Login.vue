<script setup>
import { ref } from "vue";
import { login } from "../utils/api";
import { authService } from "../service/Auth";
const email = ref("");
const password = ref("");
const rememberMe = ref(false);
const errorMessage = ref("");
const showPassword = ref(false);

const handleLogin = () => {
  if (!email.value || !password.value) {
    errorMessage.value = "Por favor ingresa tu correo y contraseña";
    return;
  }
  login(email.value, password.value)
    .then((response) => {
      if (response.status === 200) {
        authService.setAuth(response.data.token, response.data.user);
        window.location.hash = "/dashboard";
      } else {
        errorMessage.value = "Correo o contraseña inválidos";
      }
    })
    .catch((error) => {
      if (error.response?.status === 403) {
        // Email not verified, redirect to verification page
        localStorage.setItem('pendingVerificationEmail', email.value);
        window.location.hash = '/verify';
      } else {
        errorMessage.value = error.response?.data?.message || "Ocurrió un error al iniciar sesión";
      }
    });
};

const goToRegister = () => {
  window.location.hash = "/register";
};
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-emerald-50 via-white to-teal-50 flex items-center justify-center p-4">
    <div class="w-full max-w-md">
      <!-- Header -->
      <div class="text-center mb-8">
        <img src="../assets/E.png" alt="E-Signature Logo" class="mx-auto mb-6 rounded-3xl shadow-lg" style="width: 80px; height: 80px; object-fit: contain;" />
        <h1 class="text-3xl font-bold text-slate-900 mb-3">E-Signature</h1>
        <p class="text-slate-600 text-lg">Bienvenido de vuelta</p>
        <p class="text-slate-500">Ingresa a tu cuenta para continuar</p>
      </div>
      <div class="bg-white/80 backdrop-blur-sm rounded-2xl shadow-2xl p-8">
        <h2 class="text-xl font-semibold text-center mb-2">Iniciar Sesión</h2>
        <p class="text-center text-slate-600 mb-6 text-sm">Accede a tu plataforma de firmas electrónicas</p>
        <form @submit.prevent="handleLogin" class="space-y-6">
          <!-- Email -->
          <div>
            <label for="email" class="block text-slate-700 font-medium mb-1">Correo Electrónico</label>
            <div class="relative">
              <!-- Mail icon -->
              <svg class="absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400 w-5 h-5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25H4.5a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-.876 1.797l-7.5 5.625a2.25 2.25 0 01-2.748 0l-7.5-5.625A2.25 2.25 0 012.25 6.993V6.75"/></svg>
              <input
                v-model="email"
                id="email"
                type="email"
                placeholder="tu@email.com"
                required
                class="pl-12 pr-3 py-3 h-12 w-full border border-emerald-200 rounded-md shadow-sm focus:ring-emerald-400 focus:border-emerald-400 text-base"
              />
            </div>
          </div>
          <!-- Password -->
          <div>
            <label for="password" class="block text-slate-700 font-medium mb-1">Contraseña</label>
            <div class="relative">
              <!-- Lock icon -->
              <svg class="absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400 w-5 h-5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M16.5 10.5V7.875A4.125 4.125 0 008.25 7.875V10.5m8.25 0H7.5m9 0a2.25 2.25 0 012.25 2.25v4.5A2.25 2.25 0 0116.5 19.5h-9a2.25 2.25 0 01-2.25-2.25v-4.5A2.25 2.25 0 017.5 10.5m9 0V7.875A4.125 4.125 0 008.25 7.875V10.5"/></svg>
              <input
                :type="showPassword ? 'text' : 'password'"
                v-model="password"
                id="password"
                placeholder="Tu contraseña"
                required
                class="pl-12 pr-10 py-3 h-12 w-full border border-emerald-200 rounded-md shadow-sm focus:ring-emerald-400 focus:border-emerald-400 text-base"
              />
              <button type="button" @click="showPassword = !showPassword" class="absolute right-3 top-1/2 transform -translate-y-1/2 text-slate-400 focus:outline-none">
                <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12.02c2.12 4.06 6.36 7.48 10.066 7.48 1.886 0 3.68-.5 5.22-1.36M21.12 15.197A10.477 10.477 0 0022.066 12.02c-2.12-4.06-6.36-7.48-10.066-7.48-1.886 0-3.68.5-5.22 1.36M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3 3l18 18" />
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 12c0-1.243.25-2.427.708-3.516a10.477 10.477 0 013.022-4.197C8.32 2.5 10.114 2 12 2c1.886 0 3.68.5 5.22 1.36a10.477 10.477 0 013.022 4.197c.458 1.089.708 2.273.708 3.516s-.25 2.427-.708 3.516a10.477 10.477 0 01-3.022 4.197C15.68 21.5 13.886 22 12 22c-1.886 0-3.68-.5-5.22-1.36a10.477 10.477 0 01-3.022-4.197A10.477 10.477 0 012.25 12z" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </button>
            </div>
          </div>
          <!-- Recordarme y Olvidaste tu contraseña -->
          <div class="flex items-center justify-between">
            <div class="flex items-center space-x-2">
              <input
                v-model="rememberMe"
                id="remember"
                type="checkbox"
                class="h-4 w-4 text-emerald-600 border-emerald-300 rounded focus:ring-emerald-400"
              />
              <label for="remember" class="text-sm text-slate-600">Recordarme</label>
            </div>
            <a href="#/password-reset-request" class="text-sm text-emerald-600 hover:text-emerald-700 font-medium">¿Olvidaste tu contraseña?</a>
          </div>
          <!-- Botón Iniciar Sesión -->
          <button
            type="submit"
            class="w-full h-12 bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-600 hover:to-teal-700 text-white font-medium rounded-md shadow-lg"
          >
            Iniciar Sesión
          </button>
          <!-- Error -->
          <div v-if="errorMessage" class="text-center text-red-600 text-sm mt-2">{{ errorMessage }}</div>
        </form>
        <!-- Separador -->
        <div class="my-6 flex items-center">
          <div class="flex-grow border-t border-emerald-100"></div>
        </div>
        <!-- Botón Crear cuenta gratuita -->
        <div class="text-center">
          <p class="text-sm text-slate-600 mb-4">¿No tienes cuenta?</p>
          <button
            @click="goToRegister"
            class="w-full h-12 border border-emerald-200 text-emerald-600 hover:text-white hover:bg-gradient-to-r hover:from-emerald-500 hover:to-teal-600 font-medium rounded-md transition"
          >
            Crear cuenta gratuita
          </button>
        </div>
      </div>
      <div class="text-center mt-8">
        <p class="text-xs text-slate-500">© 2025 E-Signature. Plataforma segura de firmas electrónicas.</p>
      </div>
    </div>
  </div>
</template>
