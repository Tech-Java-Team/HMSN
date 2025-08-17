<script setup>
// User request: No changes to core logic, only adding imports for new UI components.
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
// import cureClinicLogo from '/assets/images/logo-transparent.png';

// PrimeVue Imports
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import Password from 'primevue/password';
import IconField from 'primevue/iconfield';
import InputIcon from 'primevue/inputicon';
import Card from 'primevue/card';

const router = useRouter();
const authStore = useAuthStore();
const email = ref('');
const password = ref('');
const error = ref(null);
const isLoading = ref(false);

const handleLogin = async () => {
    isLoading.value = true;
    error.value = null;
    try {
        await authStore.login({
            email: email.value,
            password: password.value,
        });

        if (authStore.isAdmin) {
            router.push('/');
        } else if (authStore.isDoctor) {
            router.push('/doctor/dashboard');
        } else {
            router.push('/');
        }

    } catch (err) {
        error.value = 'Invalid credentials. Please try again.';
        console.error('Login failed:', err);
    } finally {
        isLoading.value = false;
    }
};
</script>

<template>
    <!-- Main container with background image -->
    <div class="min-h-screen px-4 py-20 md:px-12 lg:px-20 flex items-center justify-center bg-cover bg-surface-900 bg-center"
        astyle="background-image: url('https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?q=80&w=2070&auto=format&fit=crop')">
        <div
            class="px-8 md:px-12 lg:px-16 py-12 flex flex-col items-center gap-8 w-full max-w-lg backdrop-blur-xl bg-surface-300/20 border border-white/20 rounded-2xl">

            <!-- Header section -->
            <div class="flex flex-col items-center gap-4 w-full">
                <!-- <img :src="cureClinicLogo" alt="Cure Clinic Logo" width="32" height="32" loading="lazy" decoding="async"
                    class="h-22 w-22" /> -->
                <div class="flex flex-col gap-2 w-full text-center">
                    <h1 class="text-3xl font-medium text-white leading-tight">Welcome Back</h1>
                    <div>
                        <span class="text-white/80">Don't have an account? </span>
                        <router-link to="/register"
                            class="text-primary-400 hover:text-primary-300 font-semibold underline">Sign up</router-link>
                    </div>
                </div>
            </div>
            <form @submit.prevent="handleLogin" class="flex flex-col items-center gap-8 w-full">
                <div class="flex flex-col gap-4 w-full">
                    <!-- Email Input with Icon -->
                    <IconField>
                        <InputIcon class="pi pi-at" />
                        <InputText v-model="email" type="email" placeholder="Email" autocomplete="email"
                            class="!appearance-none !w-full !outline-0 !rounded-lg !shadow-sm !py-3 !pl-10" />
                    </IconField>

                    <!-- Password Input with Icon -->
                    <IconField>
                        <InputIcon class="pi pi-lock" />
                        <Password v-model="password" placeholder="Password" :toggleMask="true"
                            autocomplete="current-password" fluid :inputProps="{ autocomplete: 'current-password' }" :inputStyle="{ 'padding-left': '2.5rem', 'width': '100%' }"
                            inputClass="!appearance-none !border !border-white/20 !w-full !outline-0 !bg-white/10 !text-white placeholder:!text-white/70 !rounded-lg !shadow-sm !py-3" />
                    </IconField>
                </div>

                <!-- Error Message Display -->
                <div v-if="error"
                    class="text-red-300 bg-red-900/50 border border-red-500/50 rounded-lg p-3 w-full text-center">
                    {{ error }}
                </div>

                <!-- Login Button with loading state -->
                <Button type="submit" label="Sign In" :loading="isLoading"
                    class="w-full rounded-lg bg-primary !border border-primary-500 hover:!bg-teal-500 !py-3 !font-bold" />
            </form>

            <!-- Forgot Password Link -->
            <a href="#" class="text-white/70 cursor-pointer hover:text-white text-sm">Forgot Password?</a>
        </div>
    </div>
</template>