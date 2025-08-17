<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import axios from 'axios';
import { format } from 'date-fns';

// --- PrimeVue Imports ---
import Button from 'primevue/button';
import Panel from 'primevue/panel';
import Select from 'primevue/select';
import DatePicker from 'primevue/datepicker';
import InputMask from 'primevue/inputmask';
import Textarea from 'primevue/textarea';
import Message from 'primevue/message';
import ProgressSpinner from 'primevue/progressspinner';
import Card from 'primevue/card';

// --- Reactive State ---
const doctors = ref([]);
const patients = ref([]);
const services = ref([]);
const availableSlots = ref([]);
const isLoadingSlots = ref(false);
const isBooking = ref(false);
const currentUser = ref(null);

// Form state
const selectedDoctor = ref(null);
const selectedService = ref(null);
const appointmentDate = ref(null);
const selectedPatient = ref(null);
const startTime = ref('');
const endTime = ref('');
const notes = ref('');

// Error handling
const errorMessage = ref('');
const conflictDetails = ref(null);

// --- Computed Properties ---
const isAdmin = computed(() => {
    return currentUser.value?.roles?.some(role => role.name === 'Admin') || false;
});

const isPatient = computed(() => {
    return currentUser.value?.roles?.some(role => role.name === 'Patient') || false;
});

const selectedServicePrice = computed(() => {
    return selectedService.value?.price || 0;
});

const appointmentDuration = computed(() => {
    return selectedService.value?.duration_minutes || 0;
});

// Auto-calculate end time based on service duration
const calculateEndTime = () => {
    if (startTime.value && selectedService.value?.duration_minutes) {
        const [hours, minutes] = startTime.value.split(':').map(Number);
        const startDate = new Date();
        startDate.setHours(hours, minutes, 0, 0);
        
        const endDate = new Date(startDate.getTime() + (selectedService.value.duration_minutes * 60000));
        endTime.value = `${endDate.getHours().toString().padStart(2, '0')}:${endDate.getMinutes().toString().padStart(2, '0')}`;
    }
};

// Watch for start time and service changes to auto-calculate end time
watch([startTime, selectedService], calculateEndTime);

// --- Data Fetching Functions ---
const getCurrentUser = async () => {
    try {
        const response = await axios.get('/api/user');
        currentUser.value = response.data;
    } catch (error) {
        console.error("Error fetching current user:", error);
    }
};

const getDoctors = async () => {
    try {
        const response = await axios.get('/api/doctors');
        doctors.value = response.data.data || response.data; // Handle paginated response
    } catch (error) {
        console.error("Error fetching doctors:", error);
    }
};

const getPatients = async () => {
    if (!isAdmin.value) return;
    
    try {
        const response = await axios.get('/api/patients');
        patients.value = response.data.data || response.data; // Handle paginated response
    } catch (error) {
        console.error("Error fetching patients:", error);
    }
};

const getServices = async () => {
    try {
        const response = await axios.get('/api/services');
        services.value = response.data.data || response.data; // Handle paginated response
    } catch (error) {
        console.error("Error fetching services:", error);
    }
};

// --- Core Logic: Getting Available Slots ---
const getAvailableSlots = async () => {
    // Don't run if we don't have both a doctor and a date
    if (!selectedDoctor.value || !appointmentDate.value) {
        availableSlots.value = [];
        return;
    }

    isLoadingSlots.value = true;
    availableSlots.value = []; // Clear previous slots
    errorMessage.value = '';
    conflictDetails.value = null;

    try {
        const date = format(appointmentDate.value, 'yyyy-MM-dd');
        const response = await axios.get(`/api/doctors/${selectedDoctor.value.id}/available-slots`, {
            params: { 
                date: date,
                service_id: selectedService.value.id
             }
        });
        availableSlots.value = response.data;
    } catch (error) {
        console.error("Error fetching available slots:", error);
        errorMessage.value = 'Could not fetch available slots for the selected date.';
    } finally {
        isLoadingSlots.value = false;
    }
};

// --- Core Logic: Booking the Appointment ---
const bookAppointment = async () => {
    // Validate required fields
    const requiredFields = [selectedDoctor.value, selectedService.value, appointmentDate.value, startTime.value, endTime.value];
    
    if (isAdmin.value) {
        requiredFields.push(selectedPatient.value);
    }

    if (requiredFields.some(field => !field)) {
        errorMessage.value = 'Please fill in all required fields.';
        return;
    }

    // Clear previous errors
    errorMessage.value = '';
    conflictDetails.value = null;
    isBooking.value = true;

    const payload = {
        doctor_id: selectedDoctor.value.id,
        service_id: selectedService.value.id,
        appointment_date: format(appointmentDate.value, 'yyyy-MM-dd'),
        start_time: startTime.value,
        end_time: endTime.value,
        notes: notes.value || null,
    };

    // Add patient_id only if admin
    if (isAdmin.value && selectedPatient.value) {
        payload.patient_id = selectedPatient.value.id;
    }

    try {
        const response = await axios.post('/api/appointments', payload);
        
        // Success - show success message and reset form
        errorMessage.value = '';
        alert('Appointment booked successfully!');
        
        // Reset form
        resetForm();
        
        // Refresh available slots
        await getAvailableSlots();
        
    } catch (error) {
        console.error('Error booking appointment:', error.response?.data);
        
        if (error.response?.status === 422) {
            const errorData = error.response.data;
            errorMessage.value = errorData.message || 'Validation error occurred.';
            
            if (errorData.conflict_details) {
                conflictDetails.value = errorData.conflict_details;
            }
        } else {
            errorMessage.value = 'Failed to book appointment. Please try again.';
        }
    } finally {
        isBooking.value = false;
    }
};

// Reset form function
const resetForm = () => {
    selectedService.value = null;
    selectedPatient.value = null;
    startTime.value = '';
    endTime.value = '';
    notes.value = '';
    errorMessage.value = '';
    conflictDetails.value = null;
};

// --- Watchers ---
// Automatically fetch slots when the doctor or date changes
watch([selectedDoctor, appointmentDate, selectedService], getAvailableSlots);

// Reset times when service changes
watch(selectedService, () => {
    startTime.value = '';
    endTime.value = '';
});

// --- Lifecycle Hooks ---
onMounted(async () => {
    await getCurrentUser();
    await Promise.all([
        getDoctors(),
        getServices(),
        ...(isAdmin.value ? [getPatients()] : [])
    ]);
});
</script>

<template>
    <div class="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <!-- Header Section -->
            <header class="mb-8">
                <div class="flex items-center gap-4 mb-6">
                    <div class="size-12 bg-gradient-to-br from-teal-500 to-teal-600 rounded-xl flex items-center justify-center shadow-lg">
                        <i class="pi pi-calendar-plus text-white text-xl" aria-hidden="true"></i>
                    </div>
                    <div>
                        <h1 class="text-3xl md:text-4xl font-bold text-gray-900 leading-tight">
                            Book New Appointment
                        </h1>
                        <p class="text-lg text-gray-600 mt-1">
                            Schedule an appointment with our medical professionals
                        </p>
                    </div>
                </div>
            </header>

            <!-- Error Message -->
            <Message v-if="errorMessage" severity="error" class="mb-6">
                <div class="space-y-2">
                    <p>{{ errorMessage }}</p>
                    <div v-if="conflictDetails" class="mt-2 p-3 bg-red-50 rounded-lg border border-red-200">
                        <p class="text-sm font-medium text-red-800">Conflict Details:</p>
                        <p class="text-sm text-red-700">
                            Existing appointment: {{ conflictDetails.existing_appointment.date }} 
                            from {{ conflictDetails.existing_appointment.start_time }} 
                            to {{ conflictDetails.existing_appointment.end_time }}
                        </p>
                    </div>
                </div>
            </Message>

            <!-- Main Form Card -->
            <Card class="shadow-sm border border-gray-200 mb-6">
                <template #header>
                    <div class="px-6 py-4 border-b border-gray-200 bg-teal-50">
                        <h3 class="text-lg font-semibold text-teal-800 flex items-center">
                            <i class="pi pi-calendar-plus mr-2" aria-hidden="true"></i>
                            Appointment Details
                        </h3>
                    </div>
                </template>

                <template #content>
                    <div class="p-6">
                        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
                            <!-- Left Column -->
                            <div class="space-y-6">
                                <!-- Doctor Selection -->
                                <div class="flex flex-col gap-2">
                                    <label class="font-semibold text-gray-700 flex items-center">
                                        <i class="pi pi-user-md mr-2 text-teal-600" aria-hidden="true"></i>
                                        Select Doctor *
                                    </label>
                                    <Select 
                                        v-model="selectedDoctor" 
                                        :options="doctors"
                                        optionLabel="user.full_name" 
                                        placeholder="Choose a doctor" 
                                        class="w-full"
                                        :pt="{
                                            trigger: 'border-gray-300 hover:border-teal-500 focus:border-teal-500'
                                        }"
                                    >
                                        <template #option="slotProps">
                                            <div class="flex items-center space-x-3 p-2">
                                                <div class="size-8 bg-teal-100 rounded-full flex items-center justify-center">
                                                    <i class="pi pi-user text-teal-600 text-sm" aria-hidden="true"></i>
                                                </div>
                                                <div>
                                                    <p class="font-medium">{{ slotProps.option.user.full_name }}</p>
                                                    <p class="text-sm text-gray-500">{{ slotProps.option.specialty }}</p>
                                                </div>
                                            </div>
                                        </template>
                                        <template #value="slotProps">
                                            <div v-if="slotProps.value" class="flex items-center space-x-2">
                                                <div class="size-6 bg-teal-100 rounded-full flex items-center justify-center">
                                                    <i class="pi pi-user text-teal-600 text-xs" aria-hidden="true"></i>
                                                </div>
                                                <span>{{ slotProps.value.user.full_name }}</span>
                                            </div>
                                            <span v-else class="text-gray-500">Choose a doctor</span>
                                        </template>
                                    </Select>
                                </div>

                                <!-- Service Selection -->
                                <div class="flex flex-col gap-2">
                                    <label class="font-semibold text-gray-700 flex items-center">
                                        <i class="pi pi-heart mr-2 text-teal-600" aria-hidden="true"></i>
                                        Select Service *
                                    </label>
                                    <Select 
                                        v-model="selectedService" 
                                        :options="services"
                                        optionLabel="name" 
                                        placeholder="Choose a service" 
                                        class="w-full"
                                        :pt="{
                                            trigger: 'border-gray-300 hover:border-teal-500 focus:border-teal-500'
                                        }"
                                    >
                                        <template #option="slotProps">
                                            <div class="p-2">
                                                <div class="font-medium">{{ slotProps.option.name }}</div>
                                                <div class="text-sm text-gray-600 mt-1">{{ slotProps.option.description }}</div>
                                                <div class="flex justify-between items-center mt-2">
                                                    <span class="text-sm font-semibold text-green-600">
                                                        ${{ slotProps.option.price }}
                                                    </span>
                                                    <span class="text-xs text-gray-500 flex items-center gap-1">
                                                        <i class="pi pi-clock" aria-hidden="true"></i>
                                                        {{ slotProps.option.duration_minutes }}min
                                                    </span>
                                                </div>
                                            </div>
                                        </template>
                                        <template #value="slotProps">
                                            <div v-if="slotProps.value" class="flex items-center justify-between">
                                                <span>{{ slotProps.value.name }}</span>
                                                <span class="text-sm text-green-600 font-semibold ml-2">
                                                    ${{ slotProps.value.price }}
                                                </span>
                                            </div>
                                            <span v-else class="text-gray-500">Choose a service</span>
                                        </template>
                                    </Select>
                                </div>

                                <!-- Date Selection -->
                                <div class="flex flex-col gap-2">
                                    <label for="appointment-date" class="font-semibold text-gray-700 flex items-center">
                                        <i class="pi pi-calendar mr-2 text-teal-600" aria-hidden="true"></i>
                                        Appointment Date *
                                    </label>
                                    <DatePicker 
                                        id="appointment-date"
                                        v-model="appointmentDate" 
                                        dateFormat="yy-mm-dd"
                                        :minDate="new Date()" 
                                        placeholder="Choose appointment date" 
                                        class="w-full" 
                                        :pt="{
                                            input: 'border-gray-300 hover:border-teal-500 focus:border-teal-500'
                                        }" 
                                    />
                                </div>

                                <!-- Patient Selection (Admin Only) -->
                                <div v-if="isAdmin" class="flex flex-col gap-2">
                                    <label class="font-semibold text-gray-700 flex items-center">
                                        <i class="pi pi-users mr-2 text-teal-600" aria-hidden="true"></i>
                                        Select Patient *
                                    </label>
                                    <Select 
                                        v-model="selectedPatient" 
                                        :options="patients"
                                        optionLabel="user.full_name" 
                                        placeholder="Choose a patient" 
                                        class="w-full" 
                                        :pt="{
                                            trigger: 'border-gray-300 hover:border-teal-500 focus:border-teal-500'
                                        }"
                                    >
                                        <template #option="slotProps">
                                            <div class="flex items-center space-x-3 p-2">
                                                <div class="size-8 bg-blue-100 rounded-full flex items-center justify-center">
                                                    <i class="pi pi-user text-blue-600 text-sm" aria-hidden="true"></i>
                                                </div>
                                                <div>
                                                    <p class="font-medium">{{ slotProps.option.user.full_name }}</p>
                                                    <p class="text-sm text-gray-500">{{ slotProps.option.user.phone_number }}</p>
                                                </div>
                                            </div>
                                        </template>
                                        <template #value="slotProps">
                                            <div v-if="slotProps.value" class="flex items-center space-x-2">
                                                <div class="size-6 bg-blue-100 rounded-full flex items-center justify-center">
                                                    <i class="pi pi-user text-blue-600 text-xs" aria-hidden="true"></i>
                                                </div>
                                                <span>{{ slotProps.value.user.full_name }}</span>
                                            </div>
                                            <span v-else class="text-gray-500">Choose a patient</span>
                                        </template>
                                    </Select>
                                </div>
                            </div>

                            <!-- Right Column -->
                            <div class="space-y-6">
                                <!-- Time Selection -->
                                <div class="grid grid-cols-2 gap-4">
                                    <div class="flex flex-col gap-2">
                                        <label for="start-time" class="font-semibold text-gray-700 flex items-center">
                                            <i class="pi pi-clock mr-2 text-teal-600" aria-hidden="true"></i>
                                            Start Time *
                                        </label>
                                        <InputMask 
                                            id="start-time"
                                            v-model="startTime" 
                                            mask="99:99" 
                                            placeholder="09:00"
                                            class="w-full"
                                            :pt="{
                                                root: 'border-gray-300 hover:border-teal-500 focus:border-teal-500'
                                            }"
                                        />
                                    </div>
                                    <div class="flex flex-col gap-2">
                                        <label for="end-time" class="font-semibold text-gray-700 flex items-center">
                                            <i class="pi pi-clock mr-2 text-teal-600" aria-hidden="true"></i>
                                            End Time *
                                        </label>
                                        <InputMask 
                                            id="end-time"
                                            v-model="endTime" 
                                            mask="99:99" 
                                            placeholder="10:00"
                                            class="w-full"
                                            :pt="{
                                                root: 'border-gray-300 hover:border-teal-500 focus:border-teal-500'
                                            }"
                                        />
                                    </div>
                                </div>

                                <!-- Service Info Display -->
                                <div v-if="selectedService" class="bg-gradient-to-r from-teal-50 to-blue-50 rounded-lg p-4 border border-teal-200">
                                    <h4 class="font-semibold text-teal-800 mb-3 flex items-center">
                                        <i class="pi pi-info-circle mr-2" aria-hidden="true"></i>
                                        Service Information
                                    </h4>
                                    <div class="space-y-2 text-sm">
                                        <div class="flex justify-between">
                                            <span class="text-gray-600">Duration:</span>
                                            <span class="font-medium">{{ appointmentDuration }} minutes</span>
                                        </div>
                                        <div class="flex justify-between">
                                            <span class="text-gray-600">Price:</span>
                                            <span class="font-semibold text-green-600">${{ selectedServicePrice }}</span>
                                        </div>
                                    </div>
                                </div>

                                <!-- Notes -->
                                <div class="flex flex-col gap-2">
                                    <label for="notes" class="font-semibold text-gray-700 flex items-center">
                                        <i class="pi pi-file-edit mr-2 text-teal-600" aria-hidden="true"></i>
                                        Additional Notes
                                    </label>
                                    <Textarea 
                                        id="notes"
                                        v-model="notes" 
                                        placeholder="Any additional information or special requests..."
                                        rows="3" 
                                        class="w-full"
                                        :maxlength="1000"
                                        :pt="{
                                            root: 'border-gray-300 hover:border-teal-500 focus:border-teal-500'
                                        }"
                                    />
                                    <small class="text-gray-500">{{ notes.length }}/1000 characters</small>
                                </div>
                            </div>
                        </div>
                    </div>
                </template>
            </Card>

            <!-- Available Slots Section -->
            <Card v-if="selectedDoctor && appointmentDate" class="shadow-sm border border-gray-200 mb-6">
                <template #header>
                    <div class="px-6 py-4 border-b border-gray-200 bg-blue-50">
                        <h3 class="text-lg font-semibold text-blue-800 flex items-center">
                            <i class="pi pi-clock mr-2" aria-hidden="true"></i>
                            Available Time Slots
                            <span v-if="appointmentDate" class="ml-2 text-sm font-normal text-blue-600">
                                for {{ appointmentDate.toLocaleDateString() }}
                            </span>
                        </h3>
                    </div>
                </template>

                <template #content>
                    <div class="p-6">
                        <!-- Loading State -->
                        <div v-if="isLoadingSlots" class="flex items-center justify-center py-8">
                            <div class="flex items-center space-x-3">
                                <ProgressSpinner style="width: 32px; height: 32px" strokeWidth="4" />
                                <span class="text-gray-600">Loading available slots...</span>
                            </div>
                        </div>

                        <!-- Available Slots -->
                        <div v-else-if="availableSlots.length > 0" class="space-y-4">
                            <p class="text-sm text-gray-600 mb-4">
                                Suggested available time slots for your appointment:
                            </p>
                            <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 xl:grid-cols-8 gap-3">
                                <Button 
                                    v-for="slot in availableSlots" 
                                    :key="slot" 
                                    :label="slot"
                                    severity="secondary"
                                    outlined
                                    @click="startTime = slot; calculateEndTime();"
                                    class="text-sm transition-all duration-200 hover:border-teal-500 hover:text-teal-600" 
                                />
                            </div>
                        </div>

                        <!-- No Slots Available -->
                        <div v-else class="text-center py-8">
                            <i class="pi pi-calendar-times text-4xl text-gray-400 mb-4 block" aria-hidden="true"></i>
                            <p class="text-gray-600 font-medium mb-2">No pre-defined slots available</p>
                            <p class="text-sm text-gray-500">You can manually enter your preferred time above</p>
                        </div>
                    </div>
                </template>
            </Card>

            <!-- Appointment Summary -->
            <Card v-if="selectedDoctor || appointmentDate || selectedService" class="shadow-sm border border-gray-200 mb-6">
                <template #header>
                    <div class="px-6 py-4 border-b border-gray-200 bg-gray-50">
                        <h4 class="font-semibold text-gray-900 flex items-center">
                            <i class="pi pi-list mr-2 text-gray-600" aria-hidden="true"></i>
                            Appointment Summary
                        </h4>
                    </div>
                </template>

                <template #content>
                    <div class="p-6">
                        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-4 text-sm">
                            <div v-if="selectedDoctor" class="flex flex-col space-y-1">
                                <span class="text-gray-500 text-xs uppercase tracking-wide">Doctor</span>
                                <span class="font-medium">{{ selectedDoctor.user.full_name }}</span>
                                <span class="text-xs text-gray-500">{{ selectedDoctor.specialty }}</span>
                            </div>
                            <div v-if="selectedService" class="flex flex-col space-y-1">
                                <span class="text-gray-500 text-xs uppercase tracking-wide">Service</span>
                                <span class="font-medium">{{ selectedService.name }}</span>
                                <span class="text-xs text-green-600 font-semibold">${{ selectedService.price }}</span>
                            </div>
                            <div v-if="appointmentDate" class="flex flex-col space-y-1">
                                <span class="text-gray-500 text-xs uppercase tracking-wide">Date</span>
                                <span class="font-medium">{{ appointmentDate.toLocaleDateString() }}</span>
                            </div>
                            <div v-if="startTime && endTime" class="flex flex-col space-y-1">
                                <span class="text-gray-500 text-xs uppercase tracking-wide">Time</span>
                                <span class="font-medium">{{ startTime }} - {{ endTime }}</span>
                                <span class="text-xs text-gray-500">{{ appointmentDuration }} minutes</span>
                            </div>
                            <div v-if="isAdmin && selectedPatient" class="flex flex-col space-y-1">
                                <span class="text-gray-500 text-xs uppercase tracking-wide">Patient</span>
                                <span class="font-medium">{{ selectedPatient.user.full_name }}</span>
                            </div>
                        </div>
                    </div>
                </template>
            </Card>

            <!-- Booking Button -->
            <div class="flex justify-center">
                <Button 
                    :label="isBooking ? 'Booking...' : 'Book Appointment'" 
                    :icon="isBooking ? 'pi pi-spin pi-spinner' : 'pi pi-check'" 
                    size="large" 
                    @click="bookAppointment"
                    :disabled="isBooking || !selectedDoctor || !selectedService || !appointmentDate || !startTime || !endTime || (isAdmin && !selectedPatient)"
                    class="px-8 py-3 text-lg font-semibold bg-teal-600 hover:bg-teal-700 border-teal-600 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200" 
                />
            </div>
        </div>
    </div>
</template>