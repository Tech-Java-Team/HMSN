<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';
import { format, parseISO } from 'date-fns';

// PrimeVue Imports
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Tag from 'primevue/tag';
import Skeleton from 'primevue/skeleton';

// Local Components
import ErrorDisplay from '@/components/ui/ErrorDisplay.vue';

// --- Component State ---
const route = useRoute();
const router = useRouter();
const appointments = ref([]);
const isLoading = ref(true);
const error = ref(null);
const totalRecords = ref(0);
const lazyParams = ref({
    first: 0,
    rows: 10,
    page: 0,
});

// --- API Function ---
const getAppointmentHistory = async () => {
    isLoading.value = true;
    error.value = null;
    try {
        const response = await axios.get('/api/patient/appointments/past', {
            params: {
                page: lazyParams.value.page + 1,
                per_page: lazyParams.value.rows,
            }
        });

        appointments.value = response.data.data;
        totalRecords.value = response.data.total;
    } catch (err) {
        console.error("Failed to fetch appointment history:", err);
        error.value = "Could not load your appointment history. Please try again later.";
    } finally {
        isLoading.value = false;
    }
};

// --- Event Handler for Pagination ---
const onPage = (event) => {
    lazyParams.value = event;
    router.push({ query: { page: event.page + 1 } });
    getAppointmentHistory();
};

// --- Helper Functions ---
const getStatusSeverity = (status) => {
    switch (status.toLowerCase()) {
        case 'completed': return 'success';
        case 'cancelled': return 'danger';
        case 'no_show': return 'warn';
        default: return 'secondary';
    }
};

const getStatusIcon = (status) => {
    switch (status.toLowerCase()) {
        case 'completed': return 'pi-check-circle';
        case 'cancelled': return 'pi-times-circle';
        case 'no_show': return 'pi-exclamation-triangle';
        default: return 'pi-circle';
    }
};

const formatAppointmentDateTime = (date, startTime) => {
    try {
        // Ensure date only part (YYYY-MM-DD) and time part (HH:mm:ss or HH:mm)
        const datePart = date ? date.substring(0, 10) : '';
        const timePart = startTime ? startTime.substring(0, 8) : '00:00:00';

        if (!datePart) throw new Error('Invalid date');

        const dateTimeString = `${datePart}T${timePart}`;
        return parseISO(dateTimeString);
    } catch (error) {
        console.error('Error parsing date:', error);
        return new Date();
    }
};

const formatPaymentStatus = (status) => {
    return status ? status.replace('_', ' ').toUpperCase() : 'PENDING';
};

// --- Lifecycle Hook ---
onMounted(() => {
    const pageFromUrl = parseInt(route.query.page || '1', 10);
    lazyParams.value.page = pageFromUrl - 1;
    lazyParams.value.first = (pageFromUrl - 1) * lazyParams.value.rows;

    getAppointmentHistory();
});
</script>

<template>
    <div class="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <!-- Header Section -->
            <header class="mb-8">
                <div class="flex items-center gap-4 mb-6">
                    <div
                        class="size-12 bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl flex items-center justify-center shadow-lg">
                        <i class="pi pi-history text-white text-xl" aria-hidden="true"></i>
                    </div>
                    <div>
                        <h1 class="text-3xl md:text-4xl font-bold text-gray-900 leading-tight">
                            Appointment History
                        </h1>
                        <p class="text-lg text-gray-600 mt-1">
                            Complete record of all your past healthcare visits
                        </p>
                    </div>
                </div>

                <!-- Total Count -->
                <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-200">
                    <div class="flex items-center gap-4">
                        <div
                            class="size-16 bg-gradient-to-br from-blue-100 to-blue-200 rounded-xl flex items-center justify-center">
                            <i class="pi pi-calendar text-blue-600 text-2xl" aria-hidden="true"></i>
                        </div>
                        <div>
                            <p class="text-3xl font-bold text-gray-900">{{ totalRecords }}</p>
                            <p class="text-lg text-gray-600">Total Past Appointments</p>
                            <p class="text-sm text-gray-500 mt-1">
                                {{ appointments.length > 0 ? appointments.length : '' }}
                            </p>
                        </div>
                    </div>
                </div>
            </header>

            <!-- Main Content -->
            <main role="main" aria-label="Appointment history table">
                <div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                    <!-- Table Header -->
                    <div class="px-6 py-4 border-b border-gray-200 bg-gray-50">
                        <div>
                            <h2 class="text-xl font-semibold text-gray-900">Your Past Appointments</h2>
                            <p class="text-sm text-gray-600 mt-1">Review your healthcare visit history</p>
                        </div>
                    </div>

                    <!-- Error State -->
                    <ErrorDisplay v-if="error" :message="error" @retry="getAppointmentHistory" />

                    <!-- Loading State -->
                    <div v-else-if="isLoading" class="p-6">
                        <div v-for="n in 8" :key="n"
                            class="flex items-center gap-4 py-4 border-b border-gray-100 last:border-b-0">
                            <div class="flex items-center gap-4 flex-1">
                                <div class="size-12 bg-gray-100 rounded-lg">
                                    <Skeleton width="100%" height="100%" />
                                </div>
                                <div class="space-y-2 flex-1">
                                    <Skeleton width="150px" height="1rem" />
                                    <Skeleton width="100px" height="0.875rem" />
                                </div>
                            </div>
                            <div class="space-y-2">
                                <Skeleton width="120px" height="1rem" />
                                <Skeleton width="80px" height="0.875rem" />
                            </div>
                            <Skeleton width="80px" height="1.5rem" />
                            <Skeleton width="100px" height="1rem" />
                        </div>
                    </div>

                    <!-- Empty State -->
                    <div v-else-if="!isLoading && appointments.length === 0" class="text-center py-16">
                        <div class="inline-flex items-center justify-center size-20 bg-gray-100 rounded-full mb-6">
                            <i class="pi pi-calendar text-3xl text-gray-400" aria-hidden="true"></i>
                        </div>
                        <h3 class="text-2xl font-semibold text-gray-900 mb-2">No Past Appointments</h3>
                        <p class="text-gray-600 mb-6 max-w-md mx-auto">
                            You don't have any past appointments in your history yet.
                        </p>
                    </div>

                    <!-- Data Table -->
                    <DataTable v-else :value="appointments" :loading="false" paginator :rows="lazyParams.rows"
                        :totalRecords="totalRecords" :first="lazyParams.first" @page="onPage($event)"
                        class="p-datatable-sm"
                        paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
                        currentPageReportTemplate="Showing {first} to {last} of {totalRecords} appointments"
                        :rowsPerPageOptions="[5, 10, 20, 50]" :pt="{
                            table: 'min-w-full',
                            thead: 'bg-gray-50',
                            tbody: 'divide-y divide-gray-200',
                            paginator: {
                                root: 'bg-gray-50 px-6 py-4 border-t border-gray-200',
                                pages: 'flex gap-1',
                                page: 'min-w-10 h-10 flex items-center justify-center rounded-lg border border-gray-300 hover:bg-blue-50 hover:border-blue-300 transition-colors',
                                current: 'bg-blue-600 text-white border-blue-600'
                            }
                        }" role="table" aria-label="Appointment history data table" responsiveLayout="scroll">
                        <!-- Date & Time Column -->
                        <Column header="Date & Time" field="appointment_date" :sortable="true" style="min-width: 200px"
                            :pt="{
                                headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                                bodyCell: 'px-6 py-4'
                            }">
                            <template #body="slotProps">
                                <div class="flex items-center gap-3">
                                    <div
                                        class="size-12 bg-gradient-to-br from-blue-100 to-blue-200 rounded-lg flex flex-col items-center justify-center">
                                        <span class="text-xs font-medium text-blue-700 uppercase">
                                            {{ format(formatAppointmentDateTime(slotProps.data.appointment_date,
                                                slotProps.data.start_time), 'MMM') }}
                                        </span>
                                        <span class="text-sm font-bold text-blue-800">
                                            {{ format(formatAppointmentDateTime(slotProps.data.appointment_date,
                                                slotProps.data.start_time), 'd') }}
                                        </span>
                                    </div>
                                    <div>
                                        <time
                                            :datetime="`${slotProps.data.appointment_date}T${slotProps.data.start_time}`"
                                            class="font-semibold text-gray-900 block">
                                            {{ format(formatAppointmentDateTime(slotProps.data.appointment_date,
                                                slotProps.data.start_time), 'yyyy') }}
                                        </time>
                                        <p class="text-sm text-gray-600">
                                            {{ slotProps.data.start_time.substring(0, 5) }} - {{
                                                slotProps.data.end_time.substring(0, 5) }}
                                        </p>
                                    </div>
                                </div>
                            </template>
                        </Column>

                        <!-- Doctor Column -->
                        <Column field="doctor.user.full_name" header="Healthcare Provider" :sortable="true"
                            style="min-width: 250px" :pt="{
                                headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                                bodyCell: 'px-6 py-4'
                            }">
                            <template #body="slotProps">
                                <div class="flex items-center gap-3">
                                    <div
                                        class="size-10 bg-gradient-to-br from-teal-100 to-teal-200 rounded-full flex items-center justify-center">
                                        <i class="pi pi-user text-teal-600" aria-hidden="true"></i>
                                    </div>
                                    <div>
                                        <p class="font-semibold text-gray-900">
                                            {{ slotProps.data.doctor.user.full_name }}
                                        </p>
                                        <p class="text-sm text-gray-600">
                                            {{ slotProps.data.doctor.specialty }}
                                        </p>
                                        <p v-if="slotProps.data.doctor.user.phone_number" class="text-xs text-gray-500">
                                            {{ slotProps.data.doctor.user.phone_number }}
                                        </p>
                                    </div>
                                </div>
                            </template>
                        </Column>

                        <!-- Service Column -->
                        <Column field="service.name" header="Service" style="min-width: 200px" :pt="{
                            headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                            bodyCell: 'px-6 py-4'
                        }">
                            <template #body="slotProps">
                                <div v-if="slotProps.data.service">
                                    <p class="font-semibold text-gray-900">
                                        {{ slotProps.data.service.name }}
                                    </p>
                                    <p class="text-sm text-gray-600 flex items-center gap-1">
                                        <i class="pi pi-clock text-xs" aria-hidden="true"></i>
                                        {{ slotProps.data.service.duration_minutes }} minutes
                                    </p>
                                </div>
                                <div v-else class="text-gray-500 italic">
                                    Service not specified
                                </div>
                            </template>
                        </Column>

                        <!-- Status Column -->
                        <Column field="status" header="Status" style="min-width: 120px" :pt="{
                            headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                            bodyCell: 'px-6 py-4'
                        }">
                            <template #body="slotProps">
                                <div class="flex items-center gap-2">
                                    <i :class="`pi ${getStatusIcon(slotProps.data.status)} text-sm`"
                                        aria-hidden="true"></i>
                                    <Tag :value="slotProps.data.status.charAt(0).toUpperCase() + slotProps.data.status.slice(1)"
                                        :severity="getStatusSeverity(slotProps.data.status)"
                                        class="text-xs font-medium px-3 py-1" />
                                </div>
                            </template>
                        </Column>

                        <!-- Amount & Payment Column -->
                        <Column field="amount" header="Amount & Payment" style="min-width: 180px" :pt="{
                            headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                            bodyCell: 'px-6 py-4'
                        }">
                            <template #body="slotProps">
                                <div>
                                    <span class="text-lg font-bold text-gray-900 block">
                                        {{ new Intl.NumberFormat('en-US', {
                                            style: 'currency', currency: 'USD'
                                        }).format(slotProps.data.amount) }}
                                    </span>
                                    <span class="text-xs px-2 py-1 rounded-full font-medium" :class="{
                                        'bg-green-100 text-green-800': slotProps.data.payment_status === 'paid',
                                        'bg-yellow-100 text-yellow-800': slotProps.data.payment_status === 'pending',
                                        'bg-red-100 text-red-800': slotProps.data.payment_status === 'failed',
                                        'bg-gray-100 text-gray-800': !slotProps.data.payment_status
                                    }">
                                        {{ formatPaymentStatus(slotProps.data.payment_status) }}
                                    </span>
                                </div>
                            </template>
                        </Column>

                        <!-- Notes Column -->
                        <Column field="notes" header="Notes" style="min-width: 200px" :pt="{
                            headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                            bodyCell: 'px-6 py-4'
                        }">
                            <template #body="slotProps">
                                <div v-if="slotProps.data.notes" class="max-w-xs">
                                    <p class="text-sm text-gray-700 line-clamp-2">
                                        {{ slotProps.data.notes }}
                                    </p>
                                </div>
                                <div v-else class="text-gray-400 italic text-sm">
                                    No notes
                                </div>
                            </template>
                        </Column>
                    </DataTable>
                </div>
            </main>
        </div>
    </div>
</template>

<style scoped>
.line-clamp-2 {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

/* Responsive table improvements */
@media (max-width: 768px) {
    :deep(.p-datatable-wrapper) {
        overflow-x: auto;
    }

    :deep(.p-datatable-thead > tr > th) {
        white-space: nowrap;
    }

    :deep(.p-datatable-tbody > tr > td) {
        white-space: nowrap;
    }
}

/* Custom scrollbar for mobile */
:deep(.p-datatable-wrapper)::-webkit-scrollbar {
    height: 6px;
}

:deep(.p-datatable-wrapper)::-webkit-scrollbar-track {
    background: #f1f5f9;
    border-radius: 3px;
}

:deep(.p-datatable-wrapper)::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 3px;
}

:deep(.p-datatable-wrapper)::-webkit-scrollbar-thumb:hover {
    background: #94a3b8;
}
</style>