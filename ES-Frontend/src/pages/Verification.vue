<script setup lang="ts">
import { ref } from "vue";
import { verifyAccount } from "../service/api";
import { useRouter } from "vue-router";

const email = ref("");
const code = ref("");
const errorMessage = ref("");
const successMessage = ref("");
const router = useRouter();

const handleVerify = async () => {
  errorMessage.value = "";
  successMessage.value = "";

  if (!email.value || !code.value) {
    errorMessage.value = "Please enter your email and verification code.";
    return;
  }

  try {
    const res = await verifyAccount(email.value, code.value);
    successMessage.value = "Your account has been verified successfully!";
    setTimeout(() => {
        window.location.hash = "/" // redirigir a login o dashboard
    }, 2000);
  } catch (err) {
    errorMessage.value = "Invalid code or email. Please try again.";
  }
};
</script>

<template>
  <div class="flex flex-col items-center justify-center min-h-screen bg-gray-100 px-4 py-8">
    <div class="w-full max-w-md bg-white p-6 rounded-lg shadow-md">
      <h2 class="text-2xl font-semibold text-center mb-4">Email Verification</h2>

      <div v-if="errorMessage" class="text-red-600 text-sm mb-3 text-center">
        {{ errorMessage }}
      </div>

      <div v-if="successMessage" class="text-green-600 text-sm mb-3 text-center">
        {{ successMessage }}
      </div>

      <form @submit.prevent="handleVerify" class="space-y-4">
        <div>
          <label for="email" class="block text-sm font-medium text-gray-700">Email</label>
          <input
            v-model="email"
            type="email"
            id="email"
            class="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2 shadow-sm focus:ring-blue-500 focus:border-blue-500"
            required
          />
        </div>

        <div>
          <label for="code" class="block text-sm font-medium text-gray-700">Verification Code</label>
          <input
            v-model="code"
            type="text"
            id="code"
            class="mt-1 block w-full border border-gray-300 rounded-md px-3 py-2 shadow-sm focus:ring-blue-500 focus:border-blue-500"
            required
          />
        </div>

        <button
          type="submit"
          class="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700"
        >
          Verify Account
        </button>
      </form>
    </div>
  </div>
</template>
