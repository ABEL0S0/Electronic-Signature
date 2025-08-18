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
  <div class="min-h-screen bg-gradient-to-br from-emerald-50 via-white to-teal-50 flex items-center justify-center p-4 sm:p-6 lg:p-8">
    <div class="w-full max-w-sm sm:max-w-md lg:max-w-lg">
      <!-- Header -->
      <div class="text-center mb-6 sm:mb-8">
        <img 
          src="../assets/E.png" 
          alt="E-Signature Logo" 
          class="mx-auto mb-4 sm:mb-6 rounded-3xl shadow-lg w-15 h-15 sm:w-20 sm:h-20" 
          style="object-fit: contain;"
        />
        <h1 class="text-2xl sm:text-3xl lg:text-4xl font-bold text-slate-900 mb-2 sm:mb-3">E-Signature</h1>
        <p class="text-slate-600 text-base sm:text-lg lg:text-xl">Bienvenido de vuelta</p>
        <p class="text-slate-500 text-sm sm:text-base">Ingresa a tu cuenta para continuar</p>
      </div>
      
      <!-- Form Container -->
      <div class="bg-white/80 backdrop-blur-sm rounded-2xl shadow-2xl p-6 sm:p-8 lg:p-10">
        <h2 class="text-lg sm:text-xl lg:text-2xl font-semibold text-center mb-2">Iniciar Sesión</h2>
        <p class="text-center text-slate-600 mb-6 text-sm sm:text-base">Accede a tu plataforma de firmas electrónicas</p>
        
        <form @submit.prevent="handleLogin" class="space-y-4 sm:space-y-6">
          <!-- Email -->
          <div>
            <label for="email" class="block text-slate-700 font-medium mb-1 sm:mb-2 text-sm sm:text-base">Correo Electrónico</label>
            <div class="relative">
              <!-- Mail icon -->
              <svg class="absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400 w-4 h-4 sm:w-5 sm:h-5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25H4.5a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-.876 1.797l-7.5 5.625a2.25 2.25 0 01-2.748 0l-7.5-5.625A2.25 2.25 0 012.25 6.993V6.75"/>
              </svg>
              <input
                v-model="email"
                id="email"
                type="email"
                placeholder="tu@email.com"
                required
                class="pl-10 sm:pl-12 pr-3 py-2.5 sm:py-3 h-11 sm:h-12 w-full border border-emerald-200 rounded-md shadow-sm focus:ring-emerald-400 focus:border-emerald-400 text-sm sm:text-base transition-colors duration-200"
              />
            </div>
          </div>
          
          <!-- Password -->
          <div>
            <label for="password" class="block text-slate-700 font-medium mb-1 sm:mb-2 text-sm sm:text-base">Contraseña</label>
            <div class="relative">
              <!-- Lock icon -->
              <svg class="absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400 w-4 h-4 sm:w-5 sm:h-5" fill="none" stroke="currentColor" stroke-width="1.5" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M16.5 10.5V7.875A4.125 4.125 0 008.25 7.875V10.5m8.25 0H7.5m9 0a2.25 2.25 0 012.25 2.25v4.5A2.25 2.25 0 0116.5 19.5h-9a2.25 2.25 0 01-2.25-2.25v-4.5A2.25 2.25 0 017.5 10.5m9 0V7.875A4.125 4.125 0 008.25 7.875V10.5"/>
              </svg>
              <input
                :type="showPassword ? 'text' : 'password'"
                v-model="password"
                id="password"
                placeholder="Tu contraseña"
                required
                class="pl-10 sm:pl-12 pr-10 sm:pr-12 py-2.5 sm:py-3 h-11 sm:h-12 w-full border border-emerald-200 rounded-md shadow-sm focus:ring-emerald-400 focus:border-emerald-400 text-sm sm:text-base transition-colors duration-200"
              />
              <button 
                type="button" 
                @click="showPassword = !showPassword" 
                class="absolute right-3 top-1/2 transform -translate-y-1/2 text-slate-400 hover:text-slate-600 focus:outline-none transition-colors duration-200"
              >
                <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4 sm:w-5 sm:h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12.02c2.12 4.06 6.36 7.48 10.066 7.48 1.886 0 3.68-.5 5.22-1.36M21.12 15.197A10.477 10.477 0 0022.066 12.02c-2.12-4.06-6.36-7.48-10.066-7.48-1.886 0-3.68.5-5.22 1.36M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3 3l18 18" />
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4 sm:w-5 sm:h-5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 12c0-1.243.25-2.427.708-3.516a10.477 10.477 0 013.022-4.197C8.32 2.5 10.114 2 12 2c1.886 0 3.68.5 5.22 1.36a10.477 10.477 0 013.022 4.197c.458 1.089.708 2.273.708 3.516s-.25 2.427-.708 3.516a10.477 10.477 0 01-3.022 4.197C15.68 21.5 13.886 22 12 22c-1.886 0-3.68-.5-5.22-1.36a10.477 10.477 0 01-3.022-4.197A10.477 10.477 0 012.25 12z" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </button>
            </div>
          </div>
          
          <!-- Remember Me and Forgot Password -->
          <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between space-y-3 sm:space-y-0">
            <div class="flex items-center space-x-2">
              <input
                v-model="rememberMe"
                id="remember-me"
                type="checkbox"
                class="h-4 w-4 text-emerald-600 focus:ring-emerald-500 border-gray-300 rounded"
              />
              <label for="remember-me" class="text-sm text-slate-700">Recordarme</label>
            </div>
            <button 
              type="button" 
              class="text-sm text-emerald-600 hover:text-emerald-700 font-medium transition-colors duration-200"
            >
              ¿Olvidaste tu contraseña?
            </button>
          </div>
          
          <!-- Error Message -->
          <div v-if="errorMessage" class="bg-red-50 border border-red-200 rounded-md p-3 sm:p-4">
            <div class="flex">
              <svg class="flex-shrink-0 h-5 w-5 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
              <div class="ml-3">
                <p class="text-sm text-red-800">{{ errorMessage }}</p>
              </div>
            </div>
          </div>
          
          <!-- Login Button -->
          <button
            type="submit"
            class="w-full bg-emerald-600 hover:bg-emerald-700 focus:ring-4 focus:ring-emerald-200 text-white font-semibold py-2.5 sm:py-3 px-4 rounded-lg transition-all duration-200 transform hover:scale-[1.02] active:scale-[0.98] text-sm sm:text-base"
          >
            Iniciar Sesión
          </button>
        </form>
        
        <!-- Divider -->
        <div class="my-6 sm:my-8">
          <div class="relative">
            <div class="absolute inset-0 flex items-center">
              <div class="w-full border-t border-gray-300"></div>
            </div>
            <div class="relative flex justify-center text-sm">
              <span class="px-2 bg-white text-gray-500">¿No tienes una cuenta?</span>
            </div>
          </div>
        </div>
        
        <!-- Register Button -->
        <button
          @click="goToRegister"
          class="w-full bg-white hover:bg-gray-50 border border-emerald-300 text-emerald-700 hover:text-emerald-800 font-semibold py-2.5 sm:py-3 px-4 rounded-lg transition-all duration-200 transform hover:scale-[1.02] active:scale-[0.98] text-sm sm:text-base"
        >
          Crear Cuenta
        </button>
      </div>
      
      <!-- Footer -->
      <div class="text-center mt-6 sm:mt-8">
        <p class="text-xs sm:text-sm text-slate-500">
          Al continuar, aceptas nuestros 
          <a href="#" class="text-emerald-600 hover:text-emerald-700 underline">Términos de Servicio</a> 
          y 
          <a href="#" class="text-emerald-600 hover:text-emerald-700 underline">Política de Privacidad</a>
        </p>
      </div>
    </div>
  </div>
</template>
