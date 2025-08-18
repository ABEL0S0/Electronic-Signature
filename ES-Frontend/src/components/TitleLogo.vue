<script>
import VueLogo from "../assets/vue.svg";
export default {
  name: "TitleLogo",
  props: {
    cardTitle: {
      type: String,
    },
  },
  data() {
    return {
      VueLogo,
    };
  },
};
</script>

<template>
  <div class="text-center mb-8">
    <div
      class="mx-auto w-20 h-20 mb-4 bg-gray-200 rounded-full flex items-center justify-center"
    >
      <img :src="VueLogo" alt="Vue Logo" class="w-12 h-12" />
    </div>
    <h2 class="text-3xl font-medium text-gray-900">{{ cardTitle }}</h2>
  </div>
</template>
