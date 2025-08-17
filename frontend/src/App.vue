<template>
  <Navbar />
  <div id="main-app-container">
    <main class="container">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';
import Navbar from './components/ui/Navbar.vue';

const authStore = useAuthStore();
const router = useRouter();

const handleLogout = async () => {
  await authStore.logout();
  router.push({ name: 'login' });
};
</script>