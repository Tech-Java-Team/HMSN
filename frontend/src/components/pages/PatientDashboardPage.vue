<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';
import { format, parseISO, isToday, isTomorrow, addDays } from 'date-fns';

// PrimeVue Imports
import Button from 'primevue/button';
import Card from 'primevue/card';
import Tag from 'primevue/tag';
import Skeleton from 'primevue/skeleton';
import Dialog from 'primevue/dialog';
import Textarea from 'primevue/textarea';
import ConfirmDialog from 'primevue/confirmdialog';
import { useConfirm } from 'primevue/useconfirm';

// Local Components
import ErrorDisplay from '@/components/ui/ErrorDisplay.vue';

// --- Component State ---
const route = useRoute();
const router = useRouter();
const confirm = useConfirm();

const appointments = ref([]);
const isLoading = ref(true);
const error = ref(null);
const totalCount = ref(0);

// Dialog states
const isRescheduleDialogVisible = ref(false);
const isCancelDialogVisible = ref(false);
const selectedAppointment = ref(null);
const cancellationReason = ref('');
const isCancelling = ref(false);

// --- API Functions ---
const getUpcomingAppointments = async () => {
    isLoading.value = true;
    error.value = null;
    try {
        const response = await axios.get('/api/patient/appointments/active');

        appointments.value = response.data.data;
        totalCount.value = response.data.count;
    } catch (err) {
        console.error("Failed to fetch upcoming appointments:", err);
        error.value = "Could not load your upcoming appointments. Please try again later.";
    } finally {
        isLoading.value = false;
    }
};

const cancelAppointment = async () => {
    if (!selectedAppointment.value) return;

    isCancelling.value = true;
    try {
        await axios.put(`/api/patient/appointments/${selectedAppointment.value.id}/cancel`, {
            reason: cancellationReason.value
        });

        // Remove cancelled appointment from list
        appointments.value = appointments.value.filter(
            apt => apt.id !== selectedAppointment.value.id
        );
        totalCount.value = Math.max(0, totalCount.value - 1);

        isCancelDialogVisible.value = false;
        cancellationReason.value = '';
        selectedAppointment.value = null;

        // Show success message (you can replace with toast notification)
        alert('Appointment cancelled successfully');
    } catch (error) {
        console.error('Error cancelling appointment:', error);
        alert('Failed to cancel appointment. Please try again.');
    } finally {
        isCancelling.value = false;
    }
};

// --- Helper Functions ---
const formatAppointmentDateTime = (date, startTime) => {
    try {
        const dateTimeString = `${date.split('T')[0]}T${startTime}`;
        return parseISO(dateTimeString);
    } catch (error) {
        console.error('Error parsing date:', error);
        return new Date();
    }
};

const getDateLabel = (date, startTime) => {
    const appointmentDate = formatAppointmentDateTime(date, startTime);

    if (isToday(appointmentDate)) {
        return 'Today';
    } else if (isTomorrow(appointmentDate)) {
        return 'Tomorrow';
    } else {
        return format(appointmentDate, 'EEEE, MMM dd');
    }
};

const getPaymentStatusSeverity = (status) => {
    switch (status?.toLowerCase()) {
        case 'paid': return 'success';
        case 'pending': return 'warn';
        case 'failed': return 'danger';
        default: return 'secondary';
    }
};

const openCancelDialog = (appointment) => {
    selectedAppointment.value = appointment;
    cancellationReason.value = '';
    isCancelDialogVisible.value = true;
};

const openRescheduleDialog = (appointment) => {
    selectedAppointment.value = appointment;
    isRescheduleDialogVisible.value = true;
};

// --- Computed Properties ---
const groupedAppointments = computed(() => {
    const groups = {};

    appointments.value.forEach(appointment => {
        const dateKey = appointment.appointment_date.split('T')[0];
        if (!groups[dateKey]) {
            groups[dateKey] = [];
        }
        groups[dateKey].push(appointment);
    });

    // Sort appointments within each group by start time
    Object.keys(groups).forEach(date => {
        groups[date].sort((a, b) => a.start_time.localeCompare(b.start_time));
    });

    return groups;
});

const sortedDateKeys = computed(() => {
    return Object.keys(groupedAppointments.value).sort();
});

// --- Lifecycle Hook ---
onMounted(() => {
    getUpcomingAppointments();
});
</script>

<template>
    <div class="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <!-- Header Section -->
            <header class="mb-8">
                <div class="flex items-center gap-4 mb-6">
                    <div
                        class="size-12 bg-gradient-to-br from-green-500 to-green-600 rounded-xl flex items-center justify-center shadow-lg">
                        <i class="pi pi-calendar-plus text-white text-xl" aria-hidden="true"></i>
                    </div>
                    <div>
                        <h1 class="text-3xl md:text-4xl font-bold text-gray-900 leading-tight">
                            Upcoming Appointments
                        </h1>
                        <p class="text-lg text-gray-600 mt-1">
                            Manage your scheduled healthcare visits
                        </p>
                    </div>
                       <Button label="Book New Appointment" icon="pi pi-plus"
                        class="bg-green-600 hover:bg-green-700 border-green-600 ml-auto"   @click="router.push('/our-doctors')" />
                </div>
            </header>

            <!-- Main Content -->
            <main role="main" aria-label="Upcoming appointments list">
                <!-- Error State -->
                <ErrorDisplay v-if="error" :message="error" @retry="getUpcomingAppointments" />

                <!-- Loading State -->
                <div v-else-if="isLoading" class="space-y-6">
                    <div v-for="n in 3" :key="n"
                        class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                        <div class="p-6">
                            <div class="flex items-center gap-4 mb-4">
                                <Skeleton width="120px" height="1.5rem" />
                                <Skeleton width="80px" height="1.25rem" />
                            </div>
                            <div class="space-y-4">
                                <div v-for="i in 2" :key="i"
                                    class="flex items-center gap-4 p-4 border border-gray-100 rounded-lg">
                                    <div class="size-16 bg-gray-100 rounded-xl">
                                        <Skeleton width="100%" height="100%" />
                                    </div>
                                    <div class="flex-1 space-y-2">
                                        <Skeleton width="200px" height="1.25rem" />
                                        <Skeleton width="150px" height="1rem" />
                                        <Skeleton width="100px" height="0.875rem" />
                                    </div>
                                    <div class="space-y-2">
                                        <Skeleton width="100px" height="2rem" />
                                        <Skeleton width="100px" height="2rem" />
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Empty State -->
                <div v-else-if="!isLoading && appointments.length === 0"
                    class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                    <div class="text-center py-16">
                        <div class="inline-flex items-center justify-center size-20 bg-gray-100 rounded-full mb-6">
                            <i class="pi pi-calendar text-3xl text-gray-400" aria-hidden="true"></i>
                        </div>
                        <h3 class="text-2xl font-semibold text-gray-900 mb-2">No Upcoming Appointments</h3>
                        <p class="text-gray-600 mb-6 max-w-md mx-auto">
                            You don't have any scheduled appointments. Ready to book your next visit?
                        </p>
                        <Button label="Book Your Next Appointment" icon="pi pi-plus"
                            class="bg-green-600 hover:bg-green-700 border-green-600"
                            @click="router.push('/appointments/book')" />
                    </div>
                </div>

                <!-- Appointments List -->
                <div v-else class="space-y-6">
                    <div v-for="dateKey in sortedDateKeys" :key="dateKey"
                        class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                        <!-- Date Header -->
                        <div class="px-6 py-4 bg-gradient-to-r from-gray-50 to-gray-100 border-b border-gray-200">
                            <div class="flex items-center gap-3">
                                <div
                                    class="size-10 bg-gradient-to-br from-blue-100 to-blue-200 rounded-lg flex items-center justify-center">
                                    <i class="pi pi-calendar text-blue-600" aria-hidden="true"></i>
                                </div>
                                <div>
                                    <h2 class="text-xl font-semibold text-gray-900">
                                        {{ getDateLabel(groupedAppointments[dateKey][0].appointment_date,
                                        groupedAppointments[dateKey][0].start_time) }}
                                    </h2>
                                    <p class="text-sm text-gray-600">
                                        {{ format(parseISO(dateKey), 'EEEE, MMMM dd, yyyy') }}
                                    </p>
                                </div>
                                <div class="ml-auto">
                                    <span
                                        class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-blue-100 text-blue-800">
                                        {{ groupedAppointments[dateKey].length }} appointment{{
                                            groupedAppointments[dateKey].length !== 1 ? 's' : '' }}
                                    </span>
                                </div>
                            </div>
                        </div>

                        <!-- Appointments for this date -->
                        <div class="p-6 space-y-4">
                            <article v-for="appointment in groupedAppointments[dateKey]" :key="appointment.id"
                                class="flex flex-col lg:flex-row items-start lg:items-center gap-4 p-4 border border-gray-200 rounded-lg hover:border-green-300 hover:bg-green-50/30 transition-all duration-200">
                                <!-- Time Badge -->
                                <div class="flex-shrink-0">
                                    <div
                                        class="size-16 bg-gradient-to-br from-green-100 to-green-200 rounded-xl flex flex-col items-center justify-center">
                                        <span class="text-xs font-medium text-green-700 uppercase">
                                            {{ appointment.start_time.substring(0, 5) }}
                                        </span>
                                        <span class="text-xs text-green-600">
                                            {{ appointment.end_time.substring(0, 5) }}
                                        </span>
                                    </div>
                                </div>

                                <!-- Appointment Details -->
                                <div class="flex-1 min-w-0">
                                    <div class="flex flex-col sm:flex-row sm:items-center gap-2 mb-2">
                                        <h3 class="text-lg font-semibold text-gray-900">
                                            {{ appointment.doctor.user.full_name }}
                                        </h3>
                                        <Tag :value="appointment.status.charAt(0).toUpperCase() + appointment.status.slice(1)"
                                            severity="info" class="text-xs font-medium w-fit" />
                                    </div>

                                    <div class="space-y-1 text-sm text-gray-600">
                                        <p class="flex items-center gap-2">
                                            <i class="pi pi-user-md text-teal-600" aria-hidden="true"></i>
                                            <span class="font-medium">{{ appointment.doctor.specialty }}</span>
                                            <span class="text-gray-400">•</span>
                                            <span>{{ appointment.doctor.years_of_experience }} years experience</span>
                                        </p>

                                        <p class="flex items-center gap-2">
                                            <i class="pi pi-heart text-pink-600" aria-hidden="true"></i>
                                            <span class="font-medium">{{ appointment.service.name }}</span>
                                            <span class="text-gray-400">•</span>
                                            <span>{{ appointment.service.duration_minutes }} minutes</span>
                                        </p>

                                        <p class="flex items-center gap-2">
                                            <i class="pi pi-phone text-blue-600" aria-hidden="true"></i>
                                            <span>{{ appointment.doctor.user.phone_number }}</span>
                                        </p>
                                    </div>

                                    <div v-if="appointment.notes"
                                        class="mt-2 p-2 bg-yellow-50 border border-yellow-200 rounded text-sm">
                                        <p class="text-yellow-800">
                                            <i class="pi pi-info-circle mr-1" aria-hidden="true"></i>
                                            {{ appointment.notes }}
                                        </p>
                                    </div>
                                </div>

                                <!-- Amount & Payment -->
                                <div class="flex-shrink-0 text-right">
                                    <div class="mb-2">
                                        <span class="text-2xl font-bold text-gray-900">
                                            {{ new Intl.NumberFormat('en-US', {
                                                style: 'currency', currency: 'USD'
                                            }).format(appointment.amount) }}
                                        </span>
                                    </div>
                                    <Tag :value="appointment.payment_status ? appointment.payment_status.toUpperCase() : 'PENDING'"
                                        :severity="getPaymentStatusSeverity(appointment.payment_status)"
                                        class="text-xs font-medium mb-3" />
                                </div>

                                <!-- Actions -->
                                <div class="flex flex-col sm:flex-row gap-2 flex-shrink-0">

                                    <Button label="Cancel" icon="pi pi-times" outlined severity="danger" size="small"
                                        class="border-red-300 text-red-700 hover:bg-red-50"
                                        @click="openCancelDialog(appointment)" />
                                </div>
                            </article>
                        </div>
                    </div>
                </div>

                <!-- Mobile Book Button -->
                <div class="sm:hidden fixed bottom-6 right-6">
                    <Button icon="pi pi-plus"
                        class="bg-green-600 hover:bg-green-700 border-green-600 size-14 rounded-full shadow-lg"
                        @click="router.push('/appointments/book')" aria-label="Book new appointment" />
                </div>
            </main>
        </div>

        <!-- Cancel Appointment Dialog -->
        <Dialog v-model:visible="isCancelDialogVisible" header="Cancel Appointment" modal
            :style="{ width: '90vw', maxWidth: '500px' }" :pt="{
                header: 'bg-red-50 text-red-800 px-6 py-4 border-b border-red-200',
                content: 'p-0'
            }">
            <div class="p-6 space-y-4">
                <div class="flex items-center gap-3 p-4 bg-red-50 border border-red-200 rounded-lg">
                    <i class="pi pi-exclamation-triangle text-red-600 text-xl" aria-hidden="true"></i>
                    <div>
                        <h4 class="font-semibold text-red-800">Are you sure you want to cancel this appointment?</h4>
                        <p class="text-sm text-red-700 mt-1">This action cannot be undone.</p>
                    </div>
                </div>

                <div v-if="selectedAppointment" class="bg-gray-50 rounded-lg p-4">
                    <h5 class="font-semibold text-gray-900 mb-2">Appointment Details:</h5>
                    <div class="space-y-1 text-sm text-gray-600">
                        <p><strong>Doctor:</strong> {{ selectedAppointment.doctor.user.full_name }}</p>
                        <p><strong>Date:</strong> {{
                            format(formatAppointmentDateTime(selectedAppointment.appointment_date,
                            selectedAppointment.start_time), 'MMM dd, yyyy') }}</p>
                        <p><strong>Time:</strong> {{ selectedAppointment.start_time.substring(0, 5) }} - {{
                            selectedAppointment.end_time.substring(0, 5) }}</p>
                        <p><strong>Service:</strong> {{ selectedAppointment.service.name }}</p>
                    </div>
                </div>

                <div>
                    <label for="cancellation-reason" class="block text-sm font-medium text-gray-700 mb-2">
                        Reason for cancellation (optional)
                    </label>
                    <Textarea id="cancellation-reason" v-model="cancellationReason" rows="3"
                        placeholder="Please let us know why you're cancelling..." class="w-full" />
                </div>
            </div>

            <template #footer>
                <div class="flex justify-end gap-3 p-6 bg-gray-50 border-t border-gray-200">
                    <Button label="Keep Appointment" icon="pi pi-times" @click="isCancelDialogVisible = false"
                        severity="secondary" outlined />
                    <Button label="Cancel Appointment" icon="pi pi-check" @click="cancelAppointment"
                        :loading="isCancelling" severity="danger" class="bg-red-600 hover:bg-red-700 border-red-600" />
                </div>
            </template>
        </Dialog>

    </div>
</template>

<style scoped>
/* Custom animations */
@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(10px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.fade-in {
    animation: fadeIn 0.3s ease-out;
}

/* Responsive improvements */
@media (max-width: 640px) {
    .space-y-6>* {
        margin-top: 1rem;
    }
}

/* Custom scrollbar */
::-webkit-scrollbar {
    width: 6px;
}

::-webkit-scrollbar-track {
    background: #f1f5f9;
    border-radius: 3px;
}

::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
    background: #94a3b8;
}
</style>