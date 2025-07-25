<script setup>
import { ref, computed } from "vue";
import TitleLogo from "../components/TitleLogo.vue";
import { register } from "../utils/api";
import { Icon } from "@iconify/vue";
import { authService } from "../service/Auth";

const firstName = ref("");
const lastName = ref("");
const email = ref("");
const password = ref("");
const confirmPassword = ref("");
const agreeTerms = ref(false);
const errorMessage = ref("");

const passwordStrength = computed(() => {
  if (!password.value) return 0;
  let strength = 0;

  // Length check
  if (password.value.length >= 8) strength += 1;

  // Character variety checks
  if (/[A-Z]/.test(password.value)) strength += 1;
  if (/[a-z]/.test(password.value)) strength += 1;
  if (/[0-9]/.test(password.value)) strength += 1;
  if (/[^A-Za-z0-9]/.test(password.value)) strength += 1;

  return Math.min(strength, 4);
});

const passwordStrengthText = computed(() => {
  const strength = passwordStrength.value;
  if (strength === 0) return "";
  if (strength === 1) return "Weak";
  if (strength === 2) return "Fair";
  if (strength === 3) return "Good";
  return "Strong";
});

const passwordStrengthColor = computed(() => {
  const strength = passwordStrength.value;
  if (strength === 0) return "bg-gray-200";
  if (strength === 1) return "bg-red-500";
  if (strength === 2) return "bg-yellow-500";
  if (strength === 3) return "bg-blue-500";
  return "bg-green-500";
});

const handleRegister = () => {
  // Reset error message
  errorMessage.value = "";

  // Form validation
  if (
    !firstName.value ||
    !lastName.value ||
    !email.value ||
    !password.value ||
    !confirmPassword.value
  ) {
    errorMessage.value = "Please fill in all fields";
    return;
  }

  if (!agreeTerms.value) {
    errorMessage.value = "You must agree to the Terms & Conditions";
    return;
  }

  if (password.value !== confirmPassword.value) {
    errorMessage.value = "Passwords do not match";
    return;
  }

  if (passwordStrength.value < 2) {
    errorMessage.value = "Please choose a stronger password";
    return;
  }

  register(firstName.value, lastName.value, email.value, password.value)
    .then((response) => {
      if (response.status === 201) {
        console.log("Registration successful:", response.data);
        authService.setAuth(response.data.token, response.data.user);

        alert(
          "Registration successful! Please check your email to verify your account."
        );
        window.location.hash = `/verify?email=${encodeURIComponent(email.value)}`;
      } else {
        errorMessage.value = "Registration failed. Please try again.";
      }
    })
    .catch((error) => {
      console.error("Registration error:", error);
      errorMessage.value = "An error occurred during registration";
    });
};
</script>

<template>
  <div
    class="flex flex-col items-center justify-center min-h-screen bg-gradient-to-b from-gray-50 to-gray-100 px-6 py-12"
  >
    <div class="w-full max-w-md">
      <!-- Logo and title -->
      <TitleLogo card-title="Create your account" />

      <!-- Error message -->
      <div
        v-if="errorMessage"
        class="mb-4 p-3 bg-red-50 text-red-700 rounded-lg text-center"
      >
        <Icon
          icon="mdi:alert-circle"
          class="h-5 w-5 inline-block mr-2 align-middle"
        />
        <span class="align-middle">{{ errorMessage }}</span>
      </div>

      <!-- Registration form -->
      <form
        @submit.prevent="handleRegister"
        class="bg-white py-8 px-6 shadow-2xl rounded-2xl space-y-6"
      >
        <!-- Name fields -->
        <div class="grid grid-cols-1 gap-6 sm:grid-cols-2">
          <div>
            <label
              for="first-name"
              class="block text-sm font-medium text-gray-700"
              >First name</label
            >
            <div class="mt-1">
              <input
                v-model="firstName"
                type="text"
                id="first-name"
                name="first-name"
                autocomplete="given-name"
                required
                class="appearance-none block w-full px-3 py-3 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-base"
              />
            </div>
          </div>

          <div>
            <label
              for="last-name"
              class="block text-sm font-medium text-gray-700"
              >Last name</label
            >
            <div class="mt-1">
              <input
                v-model="lastName"
                type="text"
                id="last-name"
                name="last-name"
                autocomplete="family-name"
                required
                class="appearance-none block w-full px-3 py-3 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-base"
              />
            </div>
          </div>
        </div>

        <!-- Email -->
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
          <p class="mt-2 text-xs text-gray-500">
            This will be your account email
          </p>
        </div>

        <!-- Password -->
        <div>
          <label for="password" class="block text-sm font-medium text-gray-700"
            >Password</label
          >
          <div class="mt-1">
            <input
              v-model="password"
              id="password"
              name="password"
              type="password"
              autocomplete="new-password"
              required
              class="appearance-none block w-full px-3 py-3 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-base"
            />
          </div>

          <!-- Password strength meter -->
          <div class="mt-2" v-if="password">
            <div class="w-full h-1 bg-gray-200 rounded">
              <div
                class="h-1 rounded"
                :class="passwordStrengthColor"
                :style="{ width: passwordStrength * 25 + '%' }"
              ></div>
            </div>
            <p
              class="text-xs mt-1"
              :class="{
                'text-red-500': passwordStrength === 1,
                'text-yellow-500': passwordStrength === 2,
                'text-blue-500': passwordStrength === 3,
                'text-green-500': passwordStrength === 4,
              }"
            >
              {{ passwordStrengthText }} password
            </p>
          </div>

          <p class="mt-2 text-xs text-gray-500">
            Use at least 8 characters with uppercase letters, lowercase letters,
            and numbers
          </p>
        </div>

        <!-- Confirm Password -->
        <div>
          <label
            for="confirm-password"
            class="block text-sm font-medium text-gray-700"
            >Confirm password</label
          >
          <div class="mt-1">
            <input
              v-model="confirmPassword"
              id="confirm-password"
              name="confirm-password"
              type="password"
              required
              class="appearance-none block w-full px-3 py-3 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-blue-500 focus:border-blue-500 text-base"
            />
          </div>
        </div>

        <!-- Terms and conditions -->
        <div class="flex items-start">
          <div class="flex items-center h-5">
            <input
              v-model="agreeTerms"
              id="terms"
              name="terms"
              type="checkbox"
              required
              class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
          </div>
          <div class="ml-3 text-sm">
            <label for="terms" class="font-medium text-gray-700">
              I agree to our
              <a href="#" class="text-blue-600 hover:underline"
                >Terms & Conditions</a
              >
              and
              <a href="#" class="text-blue-600 hover:underline"
                >Privacy Policy</a
              >
            </label>
          </div>
        </div>

        <!-- Submit button -->
        <div>
          <button
            type="submit"
            class="w-full flex justify-center py-3 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
          >
            Create Account
          </button>
        </div>
      </form>

      <!-- Sign in link -->
      <div class="mt-6 text-center text-sm">
        <p class="text-gray-600 mb-2">Already have an account?</p>
        <a href="#/" class="font-medium text-blue-600 hover:text-blue-500">
          Sign in →
        </a>
      </div>
    </div>
  </div>
</template>
