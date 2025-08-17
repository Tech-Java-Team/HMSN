<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';
import { useAuthStore } from '@/stores/auth';
import { format } from 'date-fns';

// PrimeVue Imports
import Button from 'primevue/button';
import Card from 'primevue/card';
import Skeleton from 'primevue/skeleton';
import DataView from 'primevue/dataview';
import Dialog from 'primevue/dialog';
import DatePicker from 'primevue/datepicker';
import Select from 'primevue/select'; // CHANGED: Using Select instead of Dropdown
import Tag from 'primevue/tag';

// Local Components
import ErrorDisplay from '@/components/ui/ErrorDisplay.vue';

// --- Component State ---
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

// --- Page State ---
const doctors = ref([]);
const isLoading = ref(true);
const error = ref(null);
const totalRecords = ref(0);
const pagination = ref({
    first: 0,
    rows: 6,
    page: 0,
});

// --- Booking Dialog State ---
const isBookingDialogVisible = ref(false);
const selectedDoctorForBooking = ref(null);
const selectedService = ref(null);
const selectedDate = ref(null);
const selectedSlot = ref(null);
const availableSlots = ref([]);
const isLoadingSlots = ref(false);
const today = new Date();

// --- API Functions ---
const fetchDoctors = async () => {
    isLoading.value = true;
    error.value = null;
    try {
        const response = await axios.get('/api/public/doctors', {
            params: {
                page: pagination.value.page + 1,
                per_page: pagination.value.rows
            }
        });
        doctors.value = response.data.data;
        totalRecords.value = response.data.meta.total;
    } catch (err) {
        console.error("Failed to fetch doctors:", err);
        error.value = "Could not load the list of doctors at this time. Please try again later.";
    } finally {
        isLoading.value = false;
    }
};

const fetchAvailableSlots = async () => {
    if (!selectedDoctorForBooking.value || !selectedDate.value || !selectedService.value) {
        availableSlots.value = [];
        return;
    }
    isLoadingSlots.value = true;
    availableSlots.value = [];
    selectedSlot.value = null;

    try {
        const params = {
            date: format(selectedDate.value, 'yyyy-MM-dd'),
            service_id: selectedService.value.id
        };
        const response = await axios.get(`/api/doctors/${selectedDoctorForBooking.value.id}/available-slots`, { params });
        availableSlots.value = response.data;
    } catch (err) {
        console.error("Error fetching available slots:", err);
    } finally {
        isLoadingSlots.value = false;
    }
}

// --- Event Handlers & Booking Logic ---
const onPageChange = (event) => {
    pagination.value = event;
    router.push({ query: { page: event.page + 1 } });
    fetchDoctors();
};

const openBookingDialog = (doctor) => {
    if (!authStore.isAuthenticated) {
        router.push({ name: 'login', query: { redirect: route.fullPath } });
        return;
    }
    selectedDoctorForBooking.value = doctor;
    // Reset
    selectedService.value = null;
    selectedDate.value = null;
    selectedSlot.value = null;
    availableSlots.value = [];
    isBookingDialogVisible.value = true;
};


const handleBookingConfirmation = async () => {
    if (!selectedSlot.value || !selectedService.value) {
        alert('Please select both a service and a time slot.');
        return;
    }

    const baseDate = new Date(selectedDate.value);

    console.info("selectedSlot: ", selectedSlot.value);

    const startTime = selectedSlot.value.start_time;
    const endTime = selectedSlot.value.end_time;

    const payload = {
        doctor_id: selectedDoctorForBooking.value.id,
        service_id: selectedService.value.id,
        appointment_date: format(baseDate, 'yyyy-MM-dd'),
        start_time: startTime,
        end_time: endTime,
    };

    console.info("Payload being sent:", payload);

    try {
        const response = await axios.post('/api/appointments', payload);
        isBookingDialogVisible.value = false;
        alert('Appointment booked successfully!');
        console.info("Appointment created:", response.data);
    } catch (err) {
        console.error('Error booking appointment:', err.response?.data || err.message);
        if (err.response?.data?.message) {
            alert(err.response.data.message);
        } else if (err.response?.data?.errors) {
            const errorMessages = Object.values(err.response.data.errors).flat().join('\n');
            alert('Validation errors:\n' + errorMessages);
        } else {
            alert('Failed to book appointment. Please try again.');
        }
    }
};

// --- Computed Properties ---
const disabledCalendarDays = computed(() => {
    if (!selectedDoctorForBooking.value?.schedules) {
        return [0, 1, 2, 3, 4, 5, 6];
    }
    const dayMap = { 'Sunday': 0, 'Monday': 1, 'Tuesday': 2, 'Wednesday': 3, 'Thursday': 4, 'Friday': 5, 'Saturday': 6 };
    const availableDays = selectedDoctorForBooking.value.schedules.map(s => dayMap[s.day_of_week]);
    const allDays = [0, 1, 2, 3, 4, 5, 6];
    // Return the days that are NOT in the available list
    return allDays.filter(day => !availableDays.includes(day));
});

// --- Watchers ---
watch(selectedDate, fetchAvailableSlots);
watch(selectedService, fetchAvailableSlots);


// --- Lifecycle Hook ---
onMounted(() => {
    const pageFromUrl = parseInt(route.query.page || '1', 10);
    pagination.value.page = pageFromUrl - 1;
    pagination.value.first = (pageFromUrl - 1) * pagination.value.rows;
    fetchDoctors();
});
</script>

<template>
    <div class="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
            <header class="text-center mb-12">
                <h1 class="text-4xl md:text-5xl font-bold tracking-tight text-gray-900 mb-3">
                    Meet Our Specialists
                </h1>
                <p class="text-xl text-gray-600 max-w-3xl mx-auto leading-relaxed">
                    Dedicated professionals committed to your health and well-being.
                    Book an appointment with our experienced medical specialists.
                </p>
            </header>

            <div v-if="isLoading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6" role="status"
                aria-live="polite">
                <div v-for="n in pagination.rows" :key="n"
                    class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                    <div class="h-40 bg-gray-100">
                        <Skeleton width="100%" height="100%" />
                    </div>
                    <div class="p-5 space-y-3">
                        <Skeleton width="70%" height="1.5rem" />
                        <Skeleton width="50%" height="1rem" />
                        <div class="space-y-2">
                            <Skeleton width="100%" height="0.875rem" />
                            <Skeleton width="80%" height="0.875rem" />
                            <Skeleton width="90%" height="0.875rem" />
                        </div>
                        <div class="pt-3">
                            <Skeleton width="100%" height="2.5rem" />
                        </div>
                    </div>
                </div>
            </div>

            <ErrorDisplay v-else-if="error" :message="error" @retry="fetchDoctors" />

            <DataView v-else :value="doctors" :lazy="true" :paginator="true" :rows="pagination.rows"
                :totalRecords="totalRecords" :first="pagination.first" @page="onPageChange($event)"
                paginatorPosition="bottom" :pt="{
                    paginator: {
                        root: 'mt-10 flex justify-center',
                        pages: 'flex gap-2',
                        page: 'min-w-10 h-10 flex items-center justify-center rounded-lg border border-gray-300 hover:bg-teal-50 hover:border-teal-300 transition-colors duration-200',
                        current: 'bg-teal-600 text-white border-teal-600'
                    }
                }" role="main" aria-label="Doctors directory">
                <template #list="slotProps">
                    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 auto-rows-fr">
                        <article v-for="doctor in slotProps.items" :key="doctor.id"
                            class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden hover:shadow-lg hover:border-teal-200 transition-all duration-300 flex flex-col h-full group focus-within:outline-2 focus-within:outline-teal-600 focus-within:outline-offset-2">
                            <div
                                class="relative h-40 bg-gradient-to-br from-teal-50 to-teal-100 flex items-center justify-center overflow-hidden">
                                <div
                                    class="size-16 bg-white rounded-full flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform duration-300">
                                    <i class="pi pi-user text-2xl text-teal-600" aria-hidden="true"></i>
                                </div>
                                <div class="absolute top-3 right-3">
                                    <div class="size-2.5 bg-green-400 rounded-full animate-pulse" title="Available">
                                    </div>
                                </div>
                            </div>

                            <div class="p-5 flex-1 flex flex-col">
                                <header class="mb-3">
                                    <h2
                                        class="text-lg font-bold text-gray-900 mb-1 group-hover:text-teal-700 transition-colors duration-200 leading-tight">
                                        {{ doctor.name }}
                                    </h2>
                                    <p class="text-teal-600 font-semibold text-xs uppercase tracking-wide">
                                        {{ doctor.specialty }}
                                    </p>
                                </header>

                                <div v-if="doctor.services && doctor.services.length > 0" class="mb-4">
                                    <h3 class="font-semibold text-gray-700 text-xs mb-2 flex items-center">
                                        <i class="pi pi-tags mr-1.5 text-teal-600 text-xs" aria-hidden="true"></i>
                                        Services
                                    </h3>
                                    <div class="flex flex-wrap gap-1.5">
                                        <Tag v-for="service in doctor.services.slice(0, 3)" :key="service.id"
                                            :value="service.name"
                                            class="text-xs bg-teal-50 text-teal-700 border border-teal-200 px-2 py-1 rounded-full" />
                                        <Tag v-if="doctor.services.length > 3"
                                            :value="`+${doctor.services.length - 3} more`"
                                            class="text-xs bg-gray-50 text-gray-600 border border-gray-200 px-2 py-1 rounded-full" />
                                    </div>
                                </div>

                                <div class="flex-1 mb-4">
                                    <h3 class="font-semibold text-gray-700 text-xs mb-2 flex items-center">
                                        <i class="pi pi-calendar mr-1.5 text-teal-600 text-xs" aria-hidden="true"></i>
                                        Weekly Schedule
                                    </h3>

                                    <div v-if="doctor.schedules && doctor.schedules.length > 0"
                                        class="bg-gray-50 rounded-lg p-2.5 max-h-32 overflow-y-auto scrollbar-thin scrollbar-thumb-gray-300 scrollbar-track-gray-100">
                                        <div class="space-y-1.5">
                                            <div v-for="schedule in doctor.schedules" :key="schedule.day_of_week"
                                                class="flex justify-between items-center text-xs">
                                                <span class="font-medium text-gray-700 min-w-0 flex-shrink-0">
                                                    {{ schedule.day_of_week.substring(0, 3) }}
                                                </span>
                                                <span
                                                    class="font-mono text-gray-600 bg-white px-1.5 py-0.5 rounded text-xs ml-2 flex-shrink-0">
                                                    {{ schedule.start_time.substring(0, 5) }} - {{
                                                        schedule.end_time.substring(0, 5) }}
                                                </span>
                                            </div>
                                        </div>
                                    </div>

                                    <div v-else class="bg-gray-50 rounded-lg p-2.5">
                                        <p class="text-xs text-gray-500 italic text-center">
                                            Schedule not available
                                        </p>
                                    </div>
                                </div>

                                <div class="mt-auto">
                                    <Button label="Book Appointment" icon="pi pi-calendar-plus"
                                        class="w-full bg-teal-600 hover:bg-teal-700 border-teal-600 hover:border-teal-700 font-semibold py-2.5 text-sm transition-all duration-200 hover:shadow-md focus-visible:outline-2 focus-visible:outline-teal-600 focus-visible:outline-offset-2"
                                        :aria-label="`Book appointment with ${doctor.name}`"
                                        @click="openBookingDialog(doctor)" />
                                </div>
                            </div>
                        </article>
                    </div>
                </template>

                <template #empty>
                    <div class="text-center py-16">
                        <div class="inline-flex items-center justify-center size-20 bg-gray-100 rounded-full mb-6">
                            <i class="pi pi-users text-3xl text-gray-400" aria-hidden="true"></i>
                        </div>
                        <h3 class="text-2xl font-semibold text-gray-900 mb-2">No Doctors Available</h3>
                        <p class="text-gray-600 mb-6 max-w-md mx-auto">
                            We're currently updating our doctor listings. Please check back later or contact us
                            directly.
                        </p>
                        <Button label="Contact Us" icon="pi pi-phone" outlined
                            class="border-teal-600 text-teal-600 hover:bg-teal-50 focus-visible:outline-2 focus-visible:outline-teal-600 focus-visible:outline-offset-2" />
                    </div>
                </template>
            </DataView>
        </div>

        <Dialog v-if="selectedDoctorForBooking" header="Book an Appointment" v-model:visible="isBookingDialogVisible"
            modal :style="{ width: '90vw', maxWidth: '600px' }">
            <div class="p-fluid flex flex-col gap-4 pt-4">
                <p class="text-lg">You are booking an appointment with <strong class="text-teal-600">{{
                    selectedDoctorForBooking.name }}</strong>.</p>

                <div class="field">
                    <label for="service-select">Select a Service *</label>
                    <Select inputId="service-select" v-model="selectedService"
                        :options="selectedDoctorForBooking.services" optionLabel="name" placeholder="Choose a service"
                        class="w-full">
                        <template #option="slotProps">
                            <div class="flex justify-between items-center w-full">
                                <div>
                                    <div class="font-semibold">{{ slotProps.option.name }}</div>
                                    <div class="text-sm text-gray-600">{{ slotProps.option.duration_minutes }} min</div>
                                </div>
                                <div class="text-teal-600 font-semibold">${{ slotProps.option.price }}</div>
                            </div>
                        </template>
                        <template #value="slotProps">
                            <div v-if="slotProps.value" class="flex justify-between items-center w-full">
                                <span>{{ slotProps.value.name }}</span>
                                <span class="text-teal-600 font-semibold">${{ slotProps.value.price }}</span>
                            </div>
                        </template>
                    </Select>
                </div>

                <div class="field">
                    <label for="booking-date">Select a Date *</label>
                    <DatePicker inputId="booking-date" v-model="selectedDate" dateFormat="yy-mm-dd" :minDate="today"
                        :disabledDays="disabledCalendarDays" class="w-full" />
                </div>

                <div v-if="selectedDate && selectedService" class="field">
                    <label>Select an Available Time *</label>
                    <div v-if="isLoadingSlots" class="text-center p-4">
                        <i class="pi pi-spin pi-spinner text-teal-500 text-2xl"></i>
                    </div>
                    <div v-else-if="availableSlots.length > 0" class="flex flex-wrap gap-2">
                        <Button v-for="slot in availableSlots" :key="slot.start_time"
                            :label="`${slot.start_time} - ${slot.end_time}`"
                            :outlined="selectedSlot?.start_time !== slot.start_time" @click="selectedSlot = slot"
                            class="p-button-sm font-mono" />
                    </div>
                    <div v-else class="text-center p-4 bg-gray-100 rounded-md">
                        <p>No available slots for this day. Please select another date.</p>
                    </div>
                </div>

                <div v-if="selectedService" class="bg-teal-50 p-4 rounded-lg border border-teal-200">
                    <h4 class="font-semibold text-teal-800 mb-2">Booking Summary</h4>
                    <div class="text-sm space-y-1">
                        <div><span class="font-medium">Service:</span> {{ selectedService.name }}</div>
                        <div><span class="font-medium">Duration:</span> {{ selectedService.duration_minutes }} minutes
                        </div>
                        <div><span class="font-medium">Price:</span> <span class="text-lg font-bold text-teal-600">${{
                            selectedService.price }}</span></div>
                    </div>
                </div>
            </div>
            <template #footer>
                <Button label="Cancel" icon="pi pi-times" @click="isBookingDialogVisible = false" text />
                <Button label="Confirm Booking" icon="pi pi-check" @click="handleBookingConfirmation"
                    :disabled="!selectedSlot || !selectedService" autofocus />
            </template>
        </Dialog>

    </div>
</template>

<style>
.scrollbar-thin {
    scrollbar-width: thin;
}

.scrollbar-thumb-gray-300::-webkit-scrollbar-thumb {
    background-color: #d1d5db;
    border-radius: 0.25rem;
}

.scrollbar-track-gray-100::-webkit-scrollbar-track {
    background-color: #f3f4f6;
}

.scrollbar-thin::-webkit-scrollbar {
    width: 4px;
}

.scrollbar-thin::-webkit-scrollbar-thumb {
    background-color: #d1d5db;
    border-radius: 0.25rem;
}

.scrollbar-thin::-webkit-scrollbar-track {
    background-color: #f3f4f6;
}
</style>