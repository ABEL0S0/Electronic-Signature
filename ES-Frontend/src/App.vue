<script setup>
import { ref, computed } from "vue";
import Register from "./pages/Register.vue";
import NotFound from "./pages/NotFound.vue";
import Login from "./pages/Login.vue";
import Dashboard from "./pages/Dashboard.vue";
import { authService } from "./service/Auth";
import Verification from "./pages/Verification.vue";

const routes = {
  "/": Login,
  "/register": Register,
  "/dashboard": Dashboard,
  "/verify": Verification,
};

const currentPath = ref(window.location.hash);

window.addEventListener("hashchange", () => {
  currentPath.value = window.location.hash;
});

const currentView = computed(() => {
  const path = currentPath.value.slice(1) || "/";

  // Protected routes that require authentication
  const protectedRoutes = ["/dashboard"];

  if (protectedRoutes.includes(path) && !authService.isAuthenticated()) {
    // Redirect to login if trying to access protected route while not authenticated
    window.location.hash = "/";
    return routes["/"];
  }

  return routes[path] || NotFound;
});
</script>

<template>
  <component :is="currentView" />
</template>
