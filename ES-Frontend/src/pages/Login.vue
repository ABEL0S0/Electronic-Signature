<script setup>
import { ref } from "vue";
import TitleLogo from "../components/TitleLogo.vue";
import { login } from "../utils/api";
import { authService } from "../service/Auth";
const email = ref("");
const password = ref("");
const rememberMe = ref(false);
const errorMessage = ref("");

const handleLogin = () => {
  // Basic validation
  if (!email.value || !password.value) {
    errorMessage.value = "Please enter both email and password";
    return;
  }

  login(email.value, password.value)
    .then((response) => {
      if (response.status === 200) {
        console.log("Login successful:", response.data);
        authService.setAuth(response.data.token, response.data.user);

        alert("Login successful!");
        window.location.hash = "/dashboard";
      } else {
        errorMessage.value = "Invalid email or password";
      }
    })
    .catch((error) => {
      console.error("Login error:", error);
      errorMessage.value =
        error.response?.data?.message || "An error occurred during login";
    });
};
</script>

<template>
  <div
    class="flex flex-col items-center justify-center min-h-screen bg-gradient-to-b from-gray-50 to-gray-100 px-6"
  >
    <div class="w-full max-w-md">
      <!-- Logo and title -->
      <TitleLogo card-title="Sign in" />

      <!-- Error message -->
      <div
        v-if="errorMessage"
        class="mb-4 p-3 bg-red-50 text-red-700 rounded-lg text-center"
      >
        {{ errorMessage }}
      </div>

      <!-- Login form -->
      <form
        @submit.prevent="handleLogin"
        class="bg-white py-8 px-6 shadow-2xl rounded-2xl"
      >
        <div class="space-y-6">
          <div>
            <label for="email" class="block text-sm font-medium text-gray-700"
              >Email</label
            >
            <div class="mt-1">
              <input
                v-model="email"
                id="email"
                name="email"
                type="email"
                autocomplete="email"
                required
                class="appearance-none block w-full px-3 py-3 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-base"
                placeholder="name@example.com"
              />
            </div>
          </div>

          <div>
            <label
              for="password"
              class="block text-sm font-medium text-gray-700"
              >Password</label
            >
            <div class="mt-1">
              <input
                v-model="password"
                id="password"
                name="password"
                type="password"
                autocomplete="current-password"
                required
                class="appearance-none block w-full px-3 py-3 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-base"
                placeholder="••••••••"
              />
            </div>
          </div>

          <div class="flex items-center justify-between">
            <div class="flex items-center">
              <input
                v-model="rememberMe"
                id="remember-me"
                name="remember-me"
                type="checkbox"
                class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              />
              <label for="remember-me" class="ml-2 block text-sm text-gray-700">
                Keep me signed in
              </label>
            </div>

            <div class="text-sm">
              <a href="#" class="font-medium text-blue-600 hover:text-blue-500">
                Forgot password?
              </a>
            </div>
          </div>

          <div>
            <button
              type="submit"
              class="w-full flex justify-center py-3 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              Sign In
            </button>
          </div>
        </div>
      </form>

      <!-- Links -->
      <div class="mt-6 text-center text-sm">
        <p class="text-gray-600 mb-2">Don't have an account?</p>
        <a
          href="#/register"
          class="font-medium text-blue-600 hover:text-blue-500"
        >
          Create yours now →
        </a>
      </div>
    </div>
  </div>
</template>
