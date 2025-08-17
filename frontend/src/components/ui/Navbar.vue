<script setup>
import { computed } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';
import { useDark, useToggle } from '@vueuse/core';
// import cureClinicLogo from '@/assets/logo-transparent.png';

// PrimeVue Imports
import Menubar from 'primevue/menubar';
import InputText from 'primevue/inputtext';
import Button from 'primevue/button';

// --- Theme Toggle Logic ---
const isDark = useDark();
const toggleDark = useToggle(isDark);

// --- Component Logic ---
const authStore = useAuthStore();
const router = useRouter();

const handleLogout = async () => {
    await authStore.logout();
    router.push({ name: 'login' });
};


const menuItems = computed(() => {
    const items = [];
    if (authStore.isAdmin) {
        items.push(
            { label: 'Patients', route: '/patients', icon: 'pi pi-users' },
            { label: 'Doctors', route: '/doctors', icon: 'pi pi-user-plus' },
            { label: 'Book Appointment', route: '/appointments', icon: 'pi pi-calendar-plus' },
            { label: 'Daily View', route: '/all-appointments', icon: 'pi pi-calendar' },
            { label: 'Services', route: '/admin/services', icon: 'pi pi-calendar' },
        );
    }
    if (authStore.isDoctor) {
        items.push({ label: 'Dashboard', route: '/doctor/dashboard', icon: 'pi pi-th-large' });
    }
    return items;
});

</script>

<template>
    <header class="bg-surface-100 shadow-lg">
        <div class="container px-3 py-2 mx-auto">
            <Menubar :model="menuItems" class="bg-transparent text-surface-950 border-none rounded-none px-4">
                <template #start>
                    <router-link to="/" class="flex items-center gap-3 text-xl font-bold mr-6">
                       <!-- 
                        <img :src="cureClinicLogo" alt="Cure Clinic Logo" width="32" height="32" loading="lazy"
                        class="h-22 w-22" />
                        -->
                        <span class="font-serif text-3xl text-primary-700 ms-2">Cure Clinic</span>
                    </router-link>
                    <router-link to="/our-doctors" class="flex items-center gap-3 text-lg mr-6">
                        <span class="font-serif text-primary-700 ms-2">Doctors</span>
                    </router-link>
                    <router-link to="/my-dashboard" class="flex items-center gap-3 text-lg mr-6">
                        <span class="font-serif text-primary-700 ms-2">Dashboard</span>
                    </router-link>
                    <router-link to="/appointment-history" class="flex items-center gap-3 text-lg mr-6">
                        <span class="font-serif text-primary-700 ms-2">History</span>
                    </router-link>
                </template>
                <template #item="{ item, props }">
                    <router-link v-if="item.route" :to="item.route" custom v-slot="{ navigate, href, isActive }">
                        <a :href="href" @click="navigate" v-bind="props.action"
                            class="flex items-center text-surface-950 px-2 py-1 rounded-md transition-colors duration-200 font-semibold"
                            :class="{ '!text-primary-900 bg-primary-900/10': isActive }">
                            <span :class="item.icon" class="mr-0.5"></span>
                            <span>{{ item.label }}</span>
                        </a>
                    </router-link>
                </template>

                <template #end>
                    <div class="flex items-center gap-2">
                        <div class="hidden md:flex items-center">
                            <span class="relative">
                                <i
                                    class="pi pi-search absolute top-2/4 -mt-2 left-3 text-surface-400 dark:text-surface-500"></i>
                                <InputText placeholder="Search..."
                                    class="pl-10 !h-9 bg-surface-100 border-surface-200 dark:border-gray-700 rounded-md" />
                            </span>
                        </div>

                        <Button @click="toggleDark()" text rounded aria-label="Theme Toggle"
                            class="group text-surface-600 hover:!bg-surface-100">
                            <template #icon>
                                <i :class="[
                                    isDark ? 'pi pi-sun' : 'pi pi-moon',
                                    'transition-all duration-300 ease-in-out',
                                    'group-hover:rotate-360 group-hover:scale-125'
                                ]" style="font-size: 1.25rem;"></i>
                            </template>
                        </Button>

                        <div v-if="authStore.isAuthenticated && authStore.user" class="flex items-center gap-2">
                            <router-link to="/patient/" class="hidden sm:inline font-medium">Welcome, {{
                                authStore.user.full_name }}</router-link>
                            <Button label="Logout" icon="pi pi-sign-out" @click="handleLogout" size="small"
                                severity="danger" outlined class="border-0 hover:bg-transparent" />
                        </div>

                        <div v-else class="flex gap-3">
                            <router-link to="/login">
                                <Button label="Login" icon="pi pi-sign-in" severity="primary" size="small" />
                            </router-link>
                            <router-link to="/register">
                                <Button label="Sign up" severity="secondary" size="small" />
                            </router-link>
                        </div>
                    </div>
                </template>
            </Menubar>
        </div>
    </header>
</template>
