<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import axios from 'axios';
import { format, startOfWeek, endOfWeek, parseISO } from 'date-fns';

// PrimeVue Imports
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Card from 'primevue/card';
import DatePicker from 'primevue/datepicker';
import Tag from 'primevue/tag';
import Button from 'primevue/button';
import Dialog from 'primevue/dialog';
import Select from 'primevue/select';
import InputNumber from 'primevue/inputnumber';
import ConfirmDialog from 'primevue/confirmdialog';
import { useConfirm } from "primevue/useconfirm";
import Skeleton from 'primevue/skeleton';

// Local Components
import ErrorDisplay from '@/components/ui/ErrorDisplay.vue';


// --- Component State ---
const confirm = useConfirm();
const appointments = ref([]);
const isLoading = ref(true);
const error = ref(null); // Added for robust error handling

const selectedDate = ref([new Date()]);

const isEditDialogVisible = ref(false);
const appointmentToEdit = ref({});

const statusOptions = ref(['Scheduled', 'Completed', 'Cancelled']);
const paymentStatusOptions = ref(['Paid', 'Unpaid']);


// --- API Functions ---
const getAppointments = async () => {
    isLoading.value = true;
    error.value = null; // Reset error on each new fetch
    try {
        const params = {};
        if (Array.isArray(selectedDate.value) && selectedDate.value.length > 0 && selectedDate.value[0]) {
            params.start_date = format(selectedDate.value[0], 'yyyy-MM-dd');
            params.end_date = format(selectedDate.value[1] || selectedDate.value[0], 'yyyy-MM-dd');
        }

        const response = await axios.get('/api/appointments', { params });
        appointments.value = response.data;
    } catch (err) {
        console.error("Error fetching appointments:", err);
        error.value = "Could not load appointments. Please try again later.";
    } finally {
        isLoading.value = false;
    }
};

const saveAppointment = async () => {
    try {
        const payload = {
            status: appointmentToEdit.value.status,
            payment_status: appointmentToEdit.value.payment_status,
            amount: appointmentToEdit.value.amount,
        };

        const response = await axios.put(`/api/appointments/${appointmentToEdit.value.id}`, payload);

        const index = appointments.value.findIndex(app => app.id === appointmentToEdit.value.id);
        if (index !== -1) {
            appointments.value[index] = response.data;
        }

        isEditDialogVisible.value = false;
    } catch (error) {
        console.error("Error updating appointment:", error);
        alert("Failed to update appointment.");
    }
};

const deleteAppointment = async (appointment) => {
    try {
        await axios.delete(`/api/appointments/${appointment.id}`);
        appointments.value = appointments.value.filter(app => app.id !== appointment.id);
    } catch (error) {
        console.error("Error deleting appointment:", error);
        alert("Failed to delete appointment.");
    }
};

// --- Dialog and Confirmation Functions ---
const openEditDialog = (appointment) => {
    appointmentToEdit.value = { ...appointment };
    isEditDialogVisible.value = true;
};

const confirmDeleteAppointment = (appointment) => {
    confirm.require({
        message: 'Are you sure you want to delete this appointment?',
        header: 'Delete Confirmation',
        icon: 'pi pi-exclamation-triangle',
        acceptClass: 'p-button-danger',
        accept: () => {
            deleteAppointment(appointment);
        },
    });
};

// --- Quick Filter Functions ---
const viewToday = () => {
    selectedDate.value = [new Date()];
};

const viewThisWeek = () => {
    const today = new Date();
    const startDate = startOfWeek(today, { weekStartsOn: 1 });
    const endDate = endOfWeek(today, { weekStartsOn: 1 });
    selectedDate.value = [startDate, endDate];
};

// --- Watcher & Lifecycle Hooks ---
watch(selectedDate, () => {
    getAppointments();
});

onMounted(() => {
    getAppointments();
});


// --- Helper Functions for Display ---
const getStatusSeverity = (status) => {
    switch (status) {
        case 'Scheduled': return 'info';
        case 'Completed': return 'success';
        case 'Cancelled': return 'danger';
        default: return 'secondary';
    }
};

const getPaymentSeverity = (status) => {
    return status === 'Paid' ? 'success' : 'warning';
};

// --- Computed Properties for Dashboard Cards ---
const totalAppointments = computed(() => appointments.value.length);
const completedAppointments = computed(() => appointments.value.filter(a => a.status === 'Completed').length);
const scheduledAppointments = computed(() => appointments.value.filter(a => a.status === 'Scheduled').length);
const totalRevenue = computed(() => {
    const paidAmount = appointments.value
        .filter(a => a.payment_status === 'Paid')
        .reduce((sum, a) => sum + parseFloat(a.amount), 0);
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(paidAmount);
});

</script>

<template>
    <div class="p-4 md:p-6 max-w-full mx-auto">
        <ConfirmDialog />

        <!-- Header Section -->
        <div class="mb-6">
            <div class="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4">
                <div>
                    <h2 class="text-3xl font-bold !text-gray-900 dark:text-gray-100">Appointment Management</h2>
                    <p class="text-gray-600 dark:text-gray-400 mt-1">View and manage all appointments</p>
                </div>

                <!-- Filter Controls -->
                <div class="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 w-full lg:w-auto">
                    <div class="flex gap-2">
                        <Button label="Today" @click="viewToday" outlined size="small" />
                        <Button label="This Week" @click="viewThisWeek" outlined size="small" />
                    </div>
                    <DatePicker 
                        v-model="selectedDate" 
                        selectionMode="range" 
                        :manualInput="false" 
                        dateFormat="yy-mm-dd"
                        placeholder="Select date range" 
                        class="w-full sm:w-64" 
                    />
                </div>
            </div>
        </div>

        <!-- Statistics Cards -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <Card> <template #content> <div class="flex items-center justify-between"> <div> <p class="text-sm text-gray-600 dark:text-gray-400">Total Appointments</p> <p class="text-2xl font-bold text-gray-900 dark:text-gray-100">{{ totalAppointments }}</p> </div> <div class="w-12 h-12 bg-blue-100 dark:bg-blue-900/50 rounded-lg flex items-center justify-center"> <i class="pi pi-calendar text-blue-600 dark:text-blue-300 text-xl"></i> </div> </div> </template> </Card>
            <Card> <template #content> <div class="flex items-center justify-between"> <div> <p class="text-sm text-gray-600 dark:text-gray-400">Completed</p> <p class="text-2xl font-bold text-green-600 dark:text-green-400">{{ completedAppointments }}</p> </div> <div class="w-12 h-12 bg-green-100 dark:bg-green-900/50 rounded-lg flex items-center justify-center"> <i class="pi pi-check-circle text-green-600 dark:text-green-300 text-xl"></i> </div> </div> </template> </Card>
            <Card> <template #content> <div class="flex items-center justify-between"> <div> <p class="text-sm text-gray-600 dark:text-gray-400">Scheduled</p> <p class="text-2xl font-bold text-yellow-600 dark:text-yellow-400">{{ scheduledAppointments }}</p> </div> <div class="w-12 h-12 bg-yellow-100 dark:bg-yellow-900/50 rounded-lg flex items-center justify-center"> <i class="pi pi-clock text-yellow-600 dark:text-yellow-300 text-xl"></i> </div> </div> </template> </Card>
            <Card> <template #content> <div class="flex items-center justify-between"> <div> <p class="text-sm text-gray-600 dark:text-gray-400">Revenue</p> <p class="text-2xl font-bold text-teal-600 dark:text-teal-400">{{ totalRevenue }}</p> </div> <div class="w-12 h-12 bg-teal-100 dark:bg-teal-900/50 rounded-lg flex items-center justify-center"> <i class="pi pi-dollar text-teal-600 dark:text-teal-300 text-xl"></i> </div> </div> </template> </Card>
        </div>

        <!-- Appointments Table Card -->
        <Card>
            <template #content>
                <!-- THIS IS THE CORRECTED STRUCTURE -->
                <div v-if="isLoading">
                    <DataTable :value="Array(5)">
                        <Column v-for="i in 6" :key="i">
                            <template #body><Skeleton/></template>
                        </Column>
                    </DataTable>
                </div>
                <ErrorDisplay v-else-if="error" :message="error" @retry="getAppointments" />
                <DataTable v-else :value="appointments" paginator :rows="10" :rowsPerPageOptions="[5, 10, 20, 50]" paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown" currentPageReportTemplate="Showing {first} to {last} of {totalRecords} appointments" class="p-datatable-sm">
                    <template #empty>
                        <div class="text-center py-12">
                            <i class="pi pi-calendar-times text-4xl text-gray-400 mb-4 block"></i>
                            <p class="text-gray-500 text-lg font-medium">No appointments found</p>
                            <p class="text-gray-400 mt-2">Try adjusting your date range</p>
                        </div>
                    </template>

                    <Column header="Date & Time" :sortable="true" style="min-width: 180px">
                        <template #body="slotProps">
                            <div class="space-y-1">
                                <p class="font-medium text-gray-900 dark:text-gray-100">{{ format(parseISO(slotProps.data.appointment_time), 'MMM dd, yyyy') }}</p>
                                <p class="text-sm text-gray-500 dark:text-gray-400">{{ format(parseISO(slotProps.data.appointment_time), 'HH:mm') }}</p>
                            </div>
                        </template>
                    </Column>
                    <Column field="patient.full_name" header="Patient" :sortable="true" style="min-width: 200px"></Column>
                    <Column field="doctor.user.name" header="Doctor" :sortable="true" style="min-width: 200px"></Column>
                    <Column field="amount" header="Fee" :sortable="true">
                        <template #body="slotProps">
                            <span class="font-semibold">{{ new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(slotProps.data.amount) }}</span>
                        </template>
                    </Column>
                    <Column field="status" header="Status">
                        <template #body="slotProps">
                            <Tag :value="slotProps.data.status" :severity="getStatusSeverity(slotProps.data.status)" />
                        </template>
                    </Column>
                    <Column field="payment_status" header="Payment">
                        <template #body="slotProps">
                            <Tag :value="slotProps.data.payment_status" :severity="getPaymentSeverity(slotProps.data.payment_status)" />
                        </template>
                    </Column>
                    <Column header="Actions" bodyClass="text-center" style="min-width: 120px">
                        <template #body="slotProps">
                            <div class="flex justify-center space-x-2">
                                <Button icon="pi pi-pencil" severity="info" text rounded @click="openEditDialog(slotProps.data)" v-tooltip="'Edit appointment'" />
                                <Button icon="pi pi-trash" severity="danger" text rounded @click="confirmDeleteAppointment(slotProps.data)" v-tooltip="'Delete appointment'" />
                            </div>
                        </template>
                    </Column>
                </DataTable>
            </template>
        </Card>

        <!-- Edit Appointment Dialog -->
        <Dialog header="Edit Appointment" v-model:visible="isEditDialogVisible" modal :style="{ width: '90vw', maxWidth: '500px' }" class="p-fluid">
            <div class="p-6 space-y-6">
                <div class="flex flex-col gap-2">
                    <label for="status" class="font-semibold text-gray-700">Appointment Status</label>
                    <Select id="status" v-model="appointmentToEdit.status" :options="statusOptions" placeholder="Select status" class="w-full" />
                </div>
                <div class="flex flex-col gap-2">
                    <label for="payment_status" class="font-semibold text-gray-700">Payment Status</label>
                    <Select id="payment_status" v-model="appointmentToEdit.payment_status" :options="paymentStatusOptions" placeholder="Select payment status" class="w-full" />
                </div>
                <div class="flex flex-col gap-2">
                    <label for="amount" class="font-semibold text-gray-700">Appointment Fee</label>
                    <InputNumber inputId="amount" v-model="appointmentToEdit.amount" mode="currency" currency="USD" locale="en-US" />
                </div>
            </div>
            <template #footer>
                <div class="flex justify-end gap-3 p-4 bg-gray-50 border-t border-gray-200">
                    <Button label="Cancel" icon="pi pi-times" @click="isEditDialogVisible = false" severity="secondary" outlined />
                    <Button label="Save Changes" icon="pi pi-check" @click="saveAppointment" autofocus class="bg-teal-600 hover:bg-teal-700 border-teal-600" />
                </div>
            </template>
        </Dialog>
    </div>
</template>
