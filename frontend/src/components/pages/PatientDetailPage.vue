<script setup>
// VUE
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import axios from 'axios';
import { differenceInYears, format, parseISO } from 'date-fns';

// Primevue Components
import Card from 'primevue/card';
import Skeleton from 'primevue/skeleton';
import Tag from 'primevue/tag';
import Accordion from 'primevue/accordion';
import AccordionPanel from 'primevue/accordionpanel';
import AccordionHeader from 'primevue/accordionheader';
import AccordionContent from 'primevue/accordioncontent';
import Button from 'primevue/button';
import Divider from 'primevue/divider';

// Local Components
import ErrorDisplay from '@/components/ui/ErrorDisplay.vue';
import AllergyManager from '@/components/patients/AllergyManager.vue'
import ChronicIllnessManager from '../patients/ChronicIllnessManager.vue';
import ConfirmDialog from 'primevue/confirmdialog';
import VitalsManager from '../patients/VitalsManager.vue';
import MedicationManager from '../patients/MedicationManager.vue';

const route = useRoute();
const patientId = route.params.id;
const patient = ref(null);
const isLoading = ref(true);
const isError = ref(null);

const getPatientDetails = async () => {
    if (!patientId) {
        isError.value = "The page you are trying to get is not available";
        console.log("Failed to get the patient id from params");
        return;
    }

    try {
        const response = await axios.get(`/api/patients/${patientId}`);
        patient.value = response.data;
    } catch (error) {
        isError.value = "The requested patient could not be found.";
        console.error("Failed to fetch patient details:", error);
    } finally {
        isLoading.value = false;
    }
};

onMounted(() => {
    getPatientDetails();
});

const calculateAge = (dob) => {
    if (!dob) return '';
    return differenceInYears(new Date(), parseISO(dob));
};

const getSeverityTag = (severity) => {
    switch (severity?.toLowerCase()) {
        case 'severe': return 'danger';
        case 'moderate': return 'warning';
        case 'mild':
        default:
            return 'info';
    }
};

const getVitalTypeIcon = (vitalTypeId) => {
    const icons = {
        1: 'pi-heart',
        2: 'pi-heart-fill',
        3: 'pi-sun',
        4: 'pi-chart-line',
        5: 'pi-circle'
    };
    return icons[vitalTypeId] || 'pi-circle';
};

const getVitalTypeName = (vitalTypeId) => {
    const types = {
        1: 'Blood Pressure',
        2: 'Heart Rate',
        3: 'Temperature',
        4: 'Weight',
        5: 'Oxygen Saturation'
    };
    return types[vitalTypeId] || 'Unknown';
};

const getVitalUnit = (vitalTypeId) => {
    const units = {
        1: 'mmHg',
        2: 'bpm',
        3: '°F',
        4: 'lbs',
        5: '%'
    };
    return units[vitalTypeId] || '';
};
</script>

<template>
    <ConfirmDialog />
    <div class="p-6 max-w-7xl mx-auto">
        <!-- Loading State -->
        <div v-if="isLoading" class="space-y-6">
            <div class="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                <div class="flex items-center gap-4 mb-4">
                    <Skeleton shape="circle" size="5rem" />
                    <div class="flex-1">
                        <Skeleton width="60%" height="2.5rem" class="mb-2" />
                        <Skeleton width="40%" height="1.5rem" />
                    </div>
                </div>
                <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                    <Skeleton height="4rem" v-for="i in 4" :key="i" />
                </div>
            </div>
            <Skeleton height="300px" />
        </div>

        <!-- Error State -->
        <ErrorDisplay v-else-if="isError" :message="isError" @retry="getPatientDetails" />

        <!-- Success State -->
        <div v-else-if="patient" class="space-y-6">
            <!-- Patient Profile Header -->
            <div class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
                <!-- Header Background -->
                <div class="bg-gradient-to-r from-teal-500 to-teal-600 px-6 py-8">
                    <div class="flex flex-col lg:flex-row items-start lg:items-center gap-6">
                        <div
                            class="w-20 h-20 bg-white/20 backdrop-blur-sm rounded-full flex items-center justify-center">
                            <i class="pi pi-user text-4xl text-white"></i>
                        </div>
                        <div class="flex-1">
                            <h1 class="text-3xl lg:text-4xl font-bold text-white mb-2">{{ patient.full_name }}</h1>
                            <div class="flex flex-col sm:flex-row gap-4 text-teal-100">
                                <span class="flex items-center gap-2">
                                    <i class="pi pi-envelope text-sm"></i>
                                    {{ patient.email }}
                                </span>
                                <span class="flex items-center gap-2">
                                    <i class="pi pi-phone text-sm"></i>
                                    {{ patient.phone_number }}
                                </span>
                            </div>
                        </div>
                        <div class="flex gap-2">
                            <Button icon="pi pi-pencil" label="Edit" outlined
                                class="text-white border-white hover:bg-white/10" />
                        </div>
                    </div>
                </div>

                <!-- Patient Info Grid -->
                <div class="p-6">
                    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                        <div class="flex items-center gap-3 p-4 bg-gray-50 rounded-lg">
                            <div class="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
                                <i class="pi pi-user text-blue-600 text-xl"></i>
                            </div>
                            <div>
                                <p class="text-sm text-gray-500 font-medium">Gender</p>
                                <p class="text-lg font-semibold text-gray-900">{{ patient.gender }}</p>
                            </div>
                        </div>

                        <div class="flex items-center gap-3 p-4 bg-gray-50 rounded-lg">
                            <div class="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
                                <i class="pi pi-calendar text-green-600 text-xl"></i>
                            </div>
                            <div>
                                <p class="text-sm text-gray-500 font-medium">Age</p>
                                <p class="text-lg font-semibold text-gray-900">{{ calculateAge(patient.date_of_birth) }}
                                    years</p>
                                <p class="text-xs text-gray-400">{{ format(parseISO(patient.date_of_birth), 'MMM d,yyyy') }}</p>
                            </div>
                        </div>

                        <div class="flex items-center gap-3 p-4 bg-gray-50 rounded-lg">
                            <div class="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center">
                                <i class="pi pi-map-marker text-purple-600 text-xl"></i>
                            </div>
                            <div>
                                <p class="text-sm text-gray-500 font-medium">Address</p>
                                <p class="text-sm font-semibold text-gray-900 line-clamp-2">{{ patient.address || 'N/A'
                                }}</p>
                            </div>
                        </div>

                        <div class="flex items-center gap-3 p-4 bg-gray-50 rounded-lg">
                            <div class="w-12 h-12 bg-red-100 rounded-lg flex items-center justify-center">
                                <i class="pi pi-phone text-red-600 text-xl"></i>
                            </div>
                            <div>
                                <p class="text-sm text-gray-500 font-medium">Emergency Contact</p>
                                <p class="text-sm font-semibold text-gray-900">{{ patient.emergency_contact_name }}</p>
                                <p class="text-xs text-gray-400">{{ patient.emergency_contact_phone || 'N/A' }}</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Medical Information Accordion -->
            <div class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
                <div class="px-6 py-4 border-b border-gray-200 bg-gray-50">
                    <h2 class="text-xl font-semibold text-gray-900 flex items-center">
                        <i class="pi pi-heart mr-3 text-teal-600"></i>
                        Medical Information
                    </h2>
                </div>

                <Accordion multiple class="border-none">
                    <!-- Allergies Section -->
                    <AccordionPanel value="0">
                        <AccordionHeader class="px-6 py-4 hover:bg-gray-50 transition-colors">
                            <div class="flex items-center justify-between w-full">
                                <div class="flex items-center gap-3">
                                    <div class="w-10 h-10 bg-red-100 rounded-lg flex items-center justify-center">
                                        <i class="pi pi-exclamation-triangle text-red-600"></i>
                                    </div>
                                    <div>
                                        <span class="font-semibold text-gray-900">Allergies</span>
                                        <p class="text-sm text-gray-500">Known allergic reactions</p>
                                    </div>
                                </div>
                            </div>
                        </AccordionHeader>
                        <AccordionContent class="px-6 pb-6">
                            <AllergyManager :patientId=patient.id />
                        </AccordionContent>
                    </AccordionPanel>

                    <!-- Chronic Illnesses Section -->
                    <AccordionPanel value="1">
                        <AccordionHeader class="px-6 py-4 hover:bg-gray-50 transition-colors">
                            <div class="flex items-center justify-between w-full">
                                <div class="flex items-center gap-3">
                                    <div class="w-10 h-10 bg-orange-100 rounded-lg flex items-center justify-center">
                                        <i class="pi pi-heart text-orange-600"></i>
                                    </div>
                                    <div>
                                        <span class="font-semibold text-gray-900">Chronic Illnesses</span>
                                        <p class="text-sm text-gray-500">Long-term health conditions</p>
                                    </div>
                                </div>
                            </div>
                        </AccordionHeader>
                        <AccordionContent class="px-6 pb-6">
                            <ChronicIllnessManager :patientId=patient.id />
                        </AccordionContent>
                    </AccordionPanel>

                    <!-- Vitals History Section -->
                    <AccordionPanel value="2">
                        <AccordionHeader class="px-6 py-4 hover:bg-gray-50 transition-colors">
                            <div class="flex items-center justify-between w-full">
                                <div class="flex items-center gap-3">
                                    <div class="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                                        <i class="pi pi-chart-line text-blue-600"></i>
                                    </div>
                                    <div>
                                        <span class="font-semibold text-gray-900">Vitals History</span>
                                        <p class="text-sm text-gray-500">Recent vital sign measurements</p>
                                    </div>
                                </div>
                            </div>
                        </AccordionHeader>
                        <AccordionContent class="px-6 pb-6">
                            <VitalsManager :patientId=patient.id />
                        </AccordionContent>
                    </AccordionPanel>

                    <!-- Medications Section -->
                    <AccordionPanel value="3">
                        <AccordionHeader class="px-6 py-4 hover:bg-gray-50 transition-colors">
                            <div class="flex items-center justify-between w-full">
                                <div class="flex items-center gap-3">
                                    <div class="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
                                        <i class="pi pi-tablet text-green-600"></i>
                                    </div>
                                    <div>
                                        <span class="font-semibold text-gray-900">Medications</span>
                                        <p class="text-sm text-gray-500">Current prescriptions and treatments</p>
                                    </div>
                                </div>
                            </div>
                        </AccordionHeader>
                        <AccordionContent class="px-6 pb-6">
                            <MedicationManager :patientId=patient.id />
                        </AccordionContent>
                    </AccordionPanel>
                </Accordion>
            </div>
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
</style>