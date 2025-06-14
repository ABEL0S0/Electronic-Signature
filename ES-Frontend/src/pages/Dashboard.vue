<script setup>
import { authService } from "../service/Auth";
function handleSignOut() {
  // Clear authentication data
  authService.clearAuth();

  // Redirect to login page
  window.location.hash = "/";
}
</script>

<template>
  <div class="min-h-screen bg-gradient-to-b from-gray-50 to-gray-100">
    <header class="bg-white shadow">
      <div
        class="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 flex justify-between items-center"
      >
        <div>
          <h1 class="text-3xl font-medium tracking-tight text-gray-900">
            Dashboard
          </h1>
          <p class="text-gray-500">Welcome to your Apple account</p>
        </div>
        <div class="flex items-center">
          <span class="mr-4 text-gray-700">john.doe@example.com</span>
          <a
            href="#/"
            @click.prevent="handleSignOut"
            class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-md text-sm font-medium"
            >Sign Out</a
          >
        </div>
      </div>
    </header>
    <main>
      <div class="mx-auto max-w-7xl py-6 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <!-- Profile Card -->
          <div class="bg-white overflow-hidden shadow rounded-lg">
            <div class="px-4 py-5 sm:p-6">
              <div class="flex items-center">
                <div class="flex-shrink-0 bg-blue-100 rounded-full p-3">
                  <svg
                    class="h-6 w-6 text-blue-600"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                    />
                  </svg>
                </div>
                <div class="ml-5 w-0 flex-1">
                  <dt class="text-sm font-medium text-gray-500 truncate">
                    Profile
                  </dt>
                  <dd
                    class="flex items-center text-lg font-medium text-gray-900"
                  >
                    John Doe
                  </dd>
                </div>
              </div>
              <div class="mt-4">
                <a
                  href="#"
                  class="text-sm font-medium text-blue-600 hover:text-blue-500"
                >
                  Edit your profile →
                </a>
              </div>
            </div>
          </div>

          <!-- Security Card -->
          <div class="bg-white overflow-hidden shadow rounded-lg">
            <div class="px-4 py-5 sm:p-6">
              <div class="flex items-center">
                <div class="flex-shrink-0 bg-green-100 rounded-full p-3">
                  <svg
                    class="h-6 w-6 text-green-600"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"
                    />
                  </svg>
                </div>
                <div class="ml-5 w-0 flex-1">
                  <dt class="text-sm font-medium text-gray-500 truncate">
                    Security
                  </dt>
                  <dd
                    class="flex items-center text-lg font-medium text-gray-900"
                  >
                    Two-factor authentication enabled
                  </dd>
                </div>
              </div>
              <div class="mt-4">
                <a
                  href="#"
                  class="text-sm font-medium text-blue-600 hover:text-blue-500"
                >
                  Review security settings →
                </a>
              </div>
            </div>
          </div>

          <!-- Devices Card -->
          <div class="bg-white overflow-hidden shadow rounded-lg">
            <div class="px-4 py-5 sm:p-6">
              <div class="flex items-center">
                <div class="flex-shrink-0 bg-purple-100 rounded-full p-3">
                  <svg
                    class="h-6 w-6 text-purple-600"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
                    />
                  </svg>
                </div>
                <div class="ml-5 w-0 flex-1">
                  <dt class="text-sm font-medium text-gray-500 truncate">
                    Devices
                  </dt>
                  <dd
                    class="flex items-center text-lg font-medium text-gray-900"
                  >
                    3 devices
                  </dd>
                </div>
              </div>
              <div class="mt-4">
                <a
                  href="#"
                  class="text-sm font-medium text-blue-600 hover:text-blue-500"
                >
                  Manage your devices →
                </a>
              </div>
            </div>
          </div>

          <!-- Subscriptions Card -->
          <div class="bg-white overflow-hidden shadow rounded-lg">
            <div class="px-4 py-5 sm:p-6">
              <div class="flex items-center">
                <div class="flex-shrink-0 bg-yellow-100 rounded-full p-3">
                  <svg
                    class="h-6 w-6 text-yellow-600"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2"
                    />
                  </svg>
                </div>
                <div class="ml-5 w-0 flex-1">
                  <dt class="text-sm font-medium text-gray-500 truncate">
                    Subscriptions
                  </dt>
                  <dd
                    class="flex items-center text-lg font-medium text-gray-900"
                  >
                    Apple One, iCloud+
                  </dd>
                </div>
              </div>
              <div class="mt-4">
                <a
                  href="#"
                  class="text-sm font-medium text-blue-600 hover:text-blue-500"
                >
                  Manage subscriptions →
                </a>
              </div>
            </div>
          </div>

          <!-- Payment Card -->
          <div class="bg-white overflow-hidden shadow rounded-lg">
            <div class="px-4 py-5 sm:p-6">
              <div class="flex items-center">
                <div class="flex-shrink-0 bg-red-100 rounded-full p-3">
                  <svg
                    class="h-6 w-6 text-red-600"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"
                    />
                  </svg>
                </div>
                <div class="ml-5 w-0 flex-1">
                  <dt class="text-sm font-medium text-gray-500 truncate">
                    Payment Methods
                  </dt>
                  <dd
                    class="flex items-center text-lg font-medium text-gray-900"
                  >
                    2 cards on file
                  </dd>
                </div>
              </div>
              <div class="mt-4">
                <a
                  href="#"
                  class="text-sm font-medium text-blue-600 hover:text-blue-500"
                >
                  Update payment info →
                </a>
              </div>
            </div>
          </div>

          <!-- Privacy Card -->
          <div class="bg-white overflow-hidden shadow rounded-lg">
            <div class="px-4 py-5 sm:p-6">
              <div class="flex items-center">
                <div class="flex-shrink-0 bg-pink-100 rounded-full p-3">
                  <svg
                    class="h-6 w-6 text-pink-600"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M8 11V7a4 4 0 118 0m-4 8v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2z"
                    />
                  </svg>
                </div>
                <div class="ml-5 w-0 flex-1">
                  <dt class="text-sm font-medium text-gray-500 truncate">
                    Privacy
                  </dt>
                  <dd
                    class="flex items-center text-lg font-medium text-gray-900"
                  >
                    Privacy settings
                  </dd>
                </div>
              </div>
              <div class="mt-4">
                <a
                  href="#"
                  class="text-sm font-medium text-blue-600 hover:text-blue-500"
                >
                  Manage privacy settings →
                </a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>
