<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { format } from 'date-fns';

// --- PrimeVue Imports ---
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Card from 'primevue/card';

// --- Reactive State ---
const todaysAppointments = ref([]);
const isLoading = ref(true);
const error = ref(null);
const today = ref(format(new Date(), 'EEEE, MMMM do, yyyy'));

// --- Data Fetching Function ---
const getTodaysAppointments = async () => {
    try {
        const response = await axios.get('/api/doctor/dashboard-appointments');
        todaysAppointments.value = response.data;
    } catch (err) {
        console.error("Error fetching appointments:", err);
        // Correctly update the ref's value
        error.value = "Could not load appointments. Please ensure you are logged in as a doctor.";
    } finally {
        // This block runs after both try and catch, so it's the perfect place
        // to set loading to false in all cases.
        isLoading.value = false;
    }
};

// --- Lifecycle Hooks ---
onMounted(() => {
    getTodaysAppointments();
});
</script>

<template>
    <div>
        <Card>
            <template #title>
                <h2 class="text-2xl font-bold">Doctor's Dashboard</h2>
            </template>
            <template #subtitle>
                Appointments for Today: {{ today }}
            </template>
            <template #content>
                <div v-if="isLoading">Loading appointments...</div>
                <div v-else-if="error" class="p-4 text-center text-red-700 bg-red-100 rounded-lg">{{ error }}</div>
                <DataTable v-else :value="todaysAppointments" responsiveLayout="scroll">
                    <template #empty>
                        No appointments scheduled for today.
                    </template>
                    <Column header="Time">
                        <template #body="slotProps">
                            {{ format(new Date(slotProps.data.appointment_time), 'HH:mm') }}
                        </template>
                    </Column>
                    <Column field="patient.full_name" header="Patient Name"></Column>
                    <Column field="status" header="Status"></Column>
                    <Column field="payment_status" header="Payment"></Column>
                </DataTable>
            </template>
        </Card>
    </div>
</template>