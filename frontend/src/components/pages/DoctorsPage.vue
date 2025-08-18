<script setup>
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';
import { useConfirm } from 'primevue/useconfirm';

// --- Validation Imports ---
import { Form, Field, ErrorMessage } from 'vee-validate';
import * as yup from 'yup';

// --- PrimeVue Imports ---
import Button from 'primevue/button';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Dialog from 'primevue/dialog';
import InputText from 'primevue/inputtext';
import Select from 'primevue/select';
import InputMask from 'primevue/inputmask';
import ConfirmDialog from 'primevue/confirmdialog';
import Listbox from 'primevue/listbox';
import Skeleton from 'primevue/skeleton';
import ProgressSpinner from 'primevue/progressspinner';
import ToggleSwitch from 'primevue/toggleswitch';
import DatePicker from 'primevue/datepicker';
import Textarea from 'primevue/textarea';
import InputNumber from 'primevue/inputnumber';

// --- Component State ---
const doctors = ref([]);
const isLoading = ref(true);
const isDoctorDialogVisible = ref(false);
const editingDoctor = ref(null);
const isSavingDoctor = ref(false);

const isServicesDialogVisible = ref(false);
const selectedDoctorForServices = ref(null);
const allServices = ref([]);
const selectedServices = ref([]);
const isLoadingServices = ref(false);
const isSavingServices = ref(false);

const initialNewDoctorState = {
    // User fields
    fullName: '',
    email: '',
    password: '',
    password_confirmation: '',
    phoneNumber: '',
    gender: '',
    dateOfBirth: null,
    address: '',
    emergencyContactName: '',
    emergencyContactPhone: '',

    // Doctor fields
    specialty: '',
    licenseNumber: '',
    yearsOfExperience: 0,

    // Schedules
    schedules: [
        {
            dayOfWeek: 1,
            dayOfWeekName: 'Monday',
            startTime: '09:00',
            endTime: '17:00',
            isActive: true
        }
    ]
};

const newDoctor = ref({ ...initialNewDoctorState });

const daysOfWeek = ref([
    { label: 'Sunday', value: 0, name: 'SUNDAY' },
    { label: 'Monday', value: 1, name: 'MONDAY' },
    { label: 'Tuesday', value: 2, name: 'TUESDAY' },
    { label: 'Wednesday', value: 3, name: 'WEDNESDAY' },
    { label: 'Thursday', value: 4, name: 'THURSDAY' },
    { label: 'Friday', value: 5, name: 'FRIDAY' },
    { label: 'Saturday', value: 6, name: 'SATURDAY' }
]);

const genderOptions = ref([
    { label: 'Male', value: 'MALE' },
    { label: 'Female', value: 'FEMALE' },
    { label: 'Other', value: 'OTHER' }
]);

const confirm = useConfirm();

// --- VALIDATION SCHEMA ---
const schema = computed(() => {
    const baseSchema = {
        // User fields
        fullName: yup.string().required().min(3).max(255).label('Full Name'),
        email: yup.string().required().email().max(255).label('Email Address'),
        phoneNumber: yup.string().min(8).required().label('Phone Number'),
        gender: yup.string().required().oneOf(['MALE', 'FEMALE', 'OTHER']).label('Gender'),
        dateOfBirth: yup.date().required().max(new Date(), 'Date of birth must be before today').label('Date of Birth'),
        address: yup.string().max(500).label('Address'),
        emergencyContactName: yup.string().max(255).label('Emergency Contact Name'),
        emergencyContactPhone: yup.string().min(8).max(20).label('Emergency Contact Phone'),

        // Doctor fields
        specialty: yup.string().required().max(255).label('Specialty'),
        licenseNumber: yup.string().required().max(255).label('License Number'),
        yearsOfExperience: yup.number().required().min(0).max(50).label('Years of Experience'),
    };

    if (!editingDoctor.value) {
        baseSchema.password = yup.string().required().min(8).label('Password');
        baseSchema.password_confirmation = yup.string()
            .required()
            .oneOf([yup.ref('password')], 'Passwords must match')
            .label('Confirm Password');
    }

    return yup.object(baseSchema);
});

// --- API Functions ---
const getDoctors = async () => {
    isLoading.value = true;
    try {
        const response = await axios.get('/api/v1/doctors');
        doctors.value = response.data;
    } catch (error) {
        console.error("Error fetching doctors:", error);
    } finally {
        isLoading.value = false;
    }
};

const getServicesData = async (doctor) => {
    isLoadingServices.value = true;
    try {
        const [servicesResponse, assignedResponse] = await Promise.all([
            axios.get('/api/v1/services'),
            axios.get(`/api/v1/doctors/${doctor.id}/assigned-services`)
        ]);

        allServices.value = servicesResponse.data;
        // Convert assigned service IDs to service objects for Listbox
        const assignedServiceIds = assignedResponse.data;
        selectedServices.value = allServices.value.filter(service =>
            assignedServiceIds.includes(service.id)
        );
    } catch (error) {
        console.error("Error fetching services data:", error);
        alert('Could not load services for this doctor.');
    } finally {
        isLoadingServices.value = false;
    }
};

const saveDoctorServices = async () => {
    isSavingServices.value = true;
    try {
        const serviceIds = selectedServices.value.map(service => service.id);
        await axios.put(`/api/v1/doctors/${selectedDoctorForServices.value.id}/assigned-services`, {
            service_ids: serviceIds
        });
        isServicesDialogVisible.value = false;
    } catch (error) {
        console.error("Error saving doctor services:", error.response?.data);
        alert('Failed to save services.');
    } finally {
        isSavingServices.value = false;
    }
};

const saveDoctor = async (values) => {
    isSavingDoctor.value = true;
    try {
        // Convert schedules to match backend expectations
        const schedulesWithDayOfWeek = newDoctor.value.schedules.map(schedule => ({
            ...schedule,
            dayOfWeek: schedule.dayOfWeek,
            dayOfWeekName: daysOfWeek.value.find(d => d.value === schedule.dayOfWeek)?.name || '',
        }));

        const payload = {
            ...
            values,
            schedules: schedulesWithDayOfWeek
        };

        if (editingDoctor.value) {
            const response = await axios.put(`/api/v1/doctors/${editingDoctor.value.id}`, payload);
            const index = doctors.value.findIndex(d => d.id === editingDoctor.value.id);
            if (index !== -1) {
                doctors.value[index] = response.data;
            }
        } else {
            const response = await axios.post('/api/v1/doctors', payload);
            doctors.value.unshift(response.data);
        }
        isDoctorDialogVisible.value = false;
    } catch (error) {
        console.error("Error saving doctor:", error.response?.data);
        if (error.response?.data?.errors) {
            const errors = error.response.data.errors;
            const errorMessages = Object.values(errors).flat().join('\n');
            alert(`Validation errors:\n${errorMessages}`);
        } else {
            alert('Failed to save doctor. Check console for errors.');
        }
    } finally {
        isSavingDoctor.value = false;
    }
};

const deleteDoctor = async (doctor) => {
    try {
        await axios.delete(`/api/v1/doctors/${doctor.id}`);
        doctors.value = doctors.value.filter(d => d.id !== doctor.id);
    } catch (error) {
        if (error.response && error.response.status === 409) {
            alert(error.response.data.message);
        } else {
            console.error("Error deleting doctor:", error);
            alert('Failed to delete doctor.');
        }
    }
};

// --- Dialog and Form Functions ---
const openAddDialog = () => {
    editingDoctor.value = null;
    newDoctor.value = JSON.parse(JSON.stringify(initialNewDoctorState));
    isDoctorDialogVisible.value = true;
};

const openEditDialog = (doctorToEdit) => {
    editingDoctor.value = doctorToEdit;
    const doctorDataCopy = JSON.parse(JSON.stringify(doctorToEdit));
    newDoctor.value = {
        // User fields
        fullName: doctorDataCopy.user.fullName,
        email: doctorDataCopy.user.email,
        password: '',
        password_confirmation: '',
        phoneNumber: doctorDataCopy.user.phoneNumber || '',
        gender: doctorDataCopy.user.gender || '',
        dateOfBirth: doctorDataCopy.user.dateOfBirth ? new Date(doctorDataCopy.user.dateOfBirth) : null,
        address: doctorDataCopy.user.address || '',
        emergencyContactName: doctorDataCopy.user.emergencyContactName || '',
        emergencyContactPhone: doctorDataCopy.user.emergencyContactPhone || '',

        // Doctor fields
        specialty: doctorDataCopy.specialty,
        licenseNumber: doctorDataCopy.licenseNumber || '',
        yearsOfExperience: doctorDataCopy.yearsOfExperience || 0,

        // Schedules
        schedules: doctorDataCopy.schedules ? doctorDataCopy.schedules.map(schedule => ({
            ...schedule,
            dayOfWeek: schedule.dayOfWeek,
            dayOfWeekName: daysOfWeek.value.find(d => d.value === schedule.dayOfWeek)?.name || '',
            isActive: schedule.isActive !== undefined ? schedule.isActive : true
        })) : []
    };
    isDoctorDialogVisible.value = true;
};

const openServicesDialog = async (doctor) => {
    selectedDoctorForServices.value = doctor;
    selectedServices.value = [];
    isServicesDialogVisible.value = true;
    await getServicesData(doctor);
};

const confirmDeleteDoctor = (doctor) => {
    confirm.require({
        message: 'Are you sure you want to delete this doctor and their user account?',
        header: 'Delete Confirmation',
        icon: 'pi pi-exclamation-triangle',
        acceptClass: 'p-button-danger',
        accept: () => {
            deleteDoctor(doctor);
        }
    });
};

const addScheduleRow = () => {
    if (!newDoctor.value.schedules) {
        newDoctor.value.schedules = [];
    }
    newDoctor.value.schedules.push({
        dayOfWeek: 1,
        dayOfWeekName: 'Monday',
        startTime: '09:00',
        endTime: '17:00',
        isActive: true
    });
};

const removeScheduleRow = (index) => {
    newDoctor.value.schedules.splice(index, 1);
};

// Update dayOfWeekName when dayOfWeek changes
const updateDayOfWeekName = (schedule) => {
    const day = daysOfWeek.value.find(d => d.value === schedule.dayOfWeek);
    if (day) {
        schedule.dayOfWeekName = day.label;
    }
};

// --- Lifecycle Hooks ---
onMounted(() => {
    getDoctors();
});
</script>

<template>
    <div class="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <ConfirmDialog />

            <!-- Header Section -->
            <header class="mb-8">
                <div class="flex items-center gap-4 mb-6">
                    <div
                        class="size-12 bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl flex items-center justify-center shadow-lg">
                        <i class="pi pi-users text-white text-xl" aria-hidden="true"></i>
                    </div>
                    <div>
                        <h1 class="text-3xl md:text-4xl font-bold text-gray-900 leading-tight">
                            Doctor Management
                        </h1>
                        <p class="text-lg text-gray-600 mt-1">
                            Manage doctors, schedules, and assigned services
                        </p>
                    </div>
                </div>

                <!-- Summary Stats -->
                <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
                    <div class="bg-white rounded-xl p-4 shadow-sm border border-gray-200">
                        <div class="flex items-center gap-3">
                            <div class="size-10 bg-blue-100 rounded-lg flex items-center justify-center">
                                <i class="pi pi-users text-blue-600" aria-hidden="true"></i>
                            </div>
                            <div>
                                <p class="text-2xl font-bold text-gray-900">{{ doctors.length }}</p>
                                <p class="text-sm text-gray-600">Total Doctors</p>
                            </div>
                        </div>
                    </div>
                    <div class="bg-white rounded-xl p-4 shadow-sm border border-gray-200">
                        <div class="flex items-center gap-3">
                            <div class="size-10 bg-green-100 rounded-lg flex items-center justify-center">
                                <i class="pi pi-check-circle text-green-600" aria-hidden="true"></i>
                            </div>
                            <div>
                                <p class="text-2xl font-bold text-gray-900">
                                    {{doctors.filter(d => d.schedules && d.schedules.length > 0).length}}
                                </p>
                                <p class="text-sm text-gray-600">With Schedules</p>
                            </div>
                        </div>
                    </div>
                    <div class="bg-white rounded-xl p-4 shadow-sm border border-gray-200">
                        <div class="flex items-center gap-3">
                            <div class="size-10 bg-purple-100 rounded-lg flex items-center justify-center">
                                <i class="pi pi-heart text-purple-600" aria-hidden="true"></i>
                            </div>
                            <div>
                                <p class="text-2xl font-bold text-gray-900">
                                    {{[...new Set(doctors.map(d => d.specialty))].length}}
                                </p>
                                <p class="text-sm text-gray-600">Specialties</p>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                    <div>
                        <h2 class="text-xl font-semibold text-gray-900">All Doctors</h2>
                        <p class="text-sm text-gray-600 mt-1">Manage your medical staff</p>
                    </div>
                    <Button label="Add New Doctor" icon="pi pi-plus" @click="openAddDialog"
                        class="bg-blue-600 hover:bg-blue-700 border-blue-600 whitespace-nowrap"
                        aria-label="Add new doctor" />
                </div>
            </header>

            <!-- Main Content -->
            <main role="main" aria-label="Doctors management">
                <div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                    <!-- Loading State -->
                    <div v-if="isLoading" class="p-6">
                        <div v-for="n in 5" :key="n"
                            class="flex items-center gap-4 py-4 border-b border-gray-100 last:border-b-0">
                            <div class="size-12 bg-gray-100 rounded-full">
                                <Skeleton width="100%" height="100%" borderRadius="50%" />
                            </div>
                            <div class="flex-1 space-y-2">
                                <Skeleton width="200px" height="1rem" />
                                <Skeleton width="150px" height="0.875rem" />
                            </div>
                            <div class="space-y-2">
                                <Skeleton width="100px" height="1rem" />
                                <Skeleton width="80px" height="0.875rem" />
                            </div>
                            <div class="space-y-2">
                                <Skeleton width="120px" height="1rem" />
                                <Skeleton width="100px" height="0.875rem" />
                            </div>
                            <Skeleton width="120px" height="2rem" />
                        </div>
                    </div>

                    <!-- Empty State -->
                    <div v-else-if="!isLoading && doctors.length === 0" class="text-center py-16">
                        <div class="inline-flex items-center justify-center size-20 bg-gray-100 rounded-full mb-6">
                            <i class="pi pi-users text-3xl text-gray-400" aria-hidden="true"></i>
                        </div>
                        <h3 class="text-2xl font-semibold text-gray-900 mb-2">No Doctors Found</h3>
                        <p class="text-gray-600 mb-6 max-w-md mx-auto">
                            Get started by adding your first doctor to the system.
                        </p>
                        <Button label="Add First Doctor" icon="pi pi-plus" outlined @click="openAddDialog"
                            severity="secondary" class="bg-blue-600 hover:bg-blue-700 border-blue-600" />
                    </div>

                    <!-- Data Table -->
                    <DataTable v-else :value="doctors" responsiveLayout="scroll" :paginator="true" :rows="10"
                        :rowsPerPageOptions="[5, 10, 20]"
                        paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
                        currentPageReportTemplate="Showing {first} to {last} of {totalRecords} doctors"
                        class="p-datatable-sm" :pt="{
                            table: 'min-w-full',
                            thead: 'bg-gray-50',
                            tbody: 'divide-y divide-gray-200'
                        }" role="table" aria-label="Doctors data table">
                        <!-- Doctor Name Column -->
                        <Column field="user.fullName" header="Doctor Name" :sortable="true" style="min-width: 280px"
                            :pt="{
                                headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                                bodyCell: 'px-6 py-4'
                            }">
                            <template #body="slotProps">
                                <div class="flex items-center space-x-4">
                                    <div
                                        class="size-12 bg-gradient-to-br from-blue-100 to-blue-200 rounded-full flex items-center justify-center">
                                        <i class="pi pi-user text-blue-600 text-lg" aria-hidden="true"></i>
                                    </div>
                                    <div class="min-w-0 flex-1">
                                        <p class="font-semibold text-gray-900">{{ slotProps.data.user.fullName }}</p>
                                        <p class="text-sm text-gray-500">{{ slotProps.data.licenseNumber || 'License: N/A' }}</p>
                                        <p class="text-xs text-gray-400">{{ slotProps.data.yearsOfExperience }} years
                                            experience</p>
                                    </div>
                                </div>
                            </template>
                        </Column>

                        <!-- Contact Info Column -->
                        <Column header="Contact" style="min-width: 220px" :pt="{
                            headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                            bodyCell: 'px-6 py-4'
                        }">
                            <template #body="slotProps">
                                <div class="space-y-1">
                                    <a :href="`mailto:${slotProps.data.user.email}`"
                                        class="text-blue-600 hover:text-blue-800 hover:underline text-sm block">
                                        {{ slotProps.data.user.email }}
                                    </a>
                                    <p v-if="slotProps.data.user.phoneNumber"
                                        class="text-sm text-gray-600 flex items-center gap-1">
                                        <i class="pi pi-phone text-xs text-gray-400" aria-hidden="true"></i>
                                        {{ slotProps.data.user.phoneNumber }}
                                    </p>
                                    <p v-if="slotProps.data.user.gender" class="text-xs text-gray-500 capitalize">
                                        {{ slotProps.data.user.gender }}
                                    </p>
                                </div>
                            </template>
                        </Column>

                        <!-- Specialty Column -->
                        <Column field="specialty" header="Specialty" style="min-width: 150px" :pt="{
                            headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                            bodyCell: 'px-6 py-4'
                        }">
                            <template #body="slotProps">
                                <span
                                    class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
                                    {{ slotProps.data.specialty }}
                                </span>
                            </template>
                        </Column>

                        <!-- Schedule Column -->
                        <Column header="Schedule" style="min-width: 200px" :pt="{
                            headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                            bodyCell: 'px-6 py-4'
                        }">
                            <template #body="slotProps">
                                <div v-if="slotProps.data.schedules && slotProps.data.schedules.length > 0"
                                    class="space-y-1">
                                    <div v-for="schedule in slotProps.data.schedules.slice(0, 2)" :key="schedule.id"
                                        class="flex items-center space-x-2 text-sm">
                                        <i class="pi pi-calendar text-gray-400 text-xs" aria-hidden="true"></i>
                                        <span class="w-12 text-gray-600 font-medium">
                                            {{daysOfWeek.find(d => d.value === schedule.dayOfWeek)?.label.slice(0, 3)
                                            || 'N/A' }}:
                                        </span>
                                        <span class="text-gray-900">{{ schedule.startTime }} - {{ schedule.endTime
                                            }}</span>
                                        <i v-if="schedule.isActive !== undefined"
                                            :class="schedule.isActive ? 'pi pi-check-circle text-green-600' : 'pi pi-times-circle text-red-600'"
                                            class="text-xs" aria-hidden="true"></i>
                                    </div>
                                    <div v-if="slotProps.data.schedules.length > 2"
                                        class="text-xs text-blue-600 font-medium">
                                        +{{ slotProps.data.schedules.length - 2 }} more days
                                    </div>
                                </div>
                                <div v-else class="flex items-center gap-2 text-gray-400">
                                    <i class="pi pi-calendar-times text-sm" aria-hidden="true"></i>
                                    <span class="text-sm italic">No schedule set</span>
                                </div>
                            </template>
                        </Column>

                        <!-- Actions Column -->
                        <Column header="Actions" style="min-width: 150px" :pt="{
                            headerCell: 'px-6 py-4 text-center text-sm font-semibold text-gray-900 bg-gray-50',
                            bodyCell: 'px-6 py-4 text-center'
                        }">
                            <template #body="slotProps">
                                <div class="flex justify-center space-x-2">
                                    <Button icon="pi pi-pencil" severity="info" text rounded
                                        @click="openEditDialog(slotProps.data)" class="hover:bg-blue-50"
                                        :aria-label="`Edit ${slotProps.data.user.fullName}`"
                                        v-tooltip.top="'Edit doctor'" />
                                    <Button icon="pi pi-cog" severity="secondary" text rounded
                                        @click="openServicesDialog(slotProps.data)" class="hover:bg-gray-50"
                                        :aria-label="`Manage services for ${slotProps.data.user.fullName}`"
                                        v-tooltip.top="'Manage services'" />
                                    <Button icon="pi pi-trash" severity="danger" text rounded
                                        @click="confirmDeleteDoctor(slotProps.data)" class="hover:bg-red-50"
                                        :aria-label="`Delete ${slotProps.data.user.fullName}`"
                                        v-tooltip.top="'Delete doctor'" />
                                </div>
                            </template>
                        </Column>
                    </DataTable>
                </div>
            </main>

            <!-- Add / Edit Doctor Dialog -->
            <Dialog :header="editingDoctor ? 'Edit Doctor' : 'Add New Doctor'" v-model:visible="isDoctorDialogVisible"
                modal :style="{ width: '95vw', maxWidth: '1000px' }" class="overflow-hidden" :pt="{
                    header: 'bg-blue-50 text-blue-800 px-6 py-4 border-b border-blue-200',
                    content: 'p-0'
                }" role="dialog" :aria-label="editingDoctor ? 'Edit doctor dialog' : 'Add new doctor dialog'">
                <Form @submit="saveDoctor" :validation-schema="schema" :initial-values="newDoctor" class="p-fluid"
                    id="doctor-form">
                    <div class="p-6 space-y-6 max-h-[80vh] overflow-y-auto">
                        <!-- Basic Information Section -->
                        <div class="space-y-4">
                            <h3
                                class="text-lg font-semibold text-gray-900 border-b border-gray-200 pb-2 flex items-center">
                                <i class="pi pi-user mr-2 text-blue-600" aria-hidden="true"></i>
                                Personal Information
                            </h3>

                            <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
                                <div class="flex flex-col gap-2">
                                    <label for="fullName" class="font-semibold text-gray-700">Full Name *</label>
                                    <Field name="fullName" v-slot="{ field, errors }">
                                        <InputText id="fullName" v-bind="field"
                                            :class="{ 'p-invalid': errors.length > 0 }"
                                            placeholder="Enter doctor's full name" class="w-full"
                                            aria-describedby="fullName-error" />
                                    </Field>
                                    <ErrorMessage name="fullName" class="text-red-500 text-sm" id="fullName-error"
                                        role="alert" />
                                </div>

                                <div class="flex flex-col gap-2">
                                    <label for="email" class="font-semibold text-gray-700">Email *</label>
                                    <Field name="email" v-slot="{ field, errors }">
                                        <InputText id="email" v-bind="field" type="email"
                                            :inputProps="{ autocomplete: 'username' }"
                                            autocomplete="username"
                                            :class="{ 'p-invalid': errors.length > 0 }"
                                            placeholder="Enter email address" class="w-full"
                                            aria-describedby="email-error" />
                                    </Field>
                                    <ErrorMessage name="email" class="text-red-500 text-sm" id="email-error"
                                        role="alert" />
                                </div>

                                <div class="flex flex-col gap-2">
                                    <label for="phoneNumber" class="font-semibold text-gray-700">Phone Number *</label>
                                    <Field name="phoneNumber" v-slot="{ field, errors }">
                                        <InputText id="phoneNumber" v-bind="field"
                                            :class="{ 'p-invalid': errors.length > 0 }" placeholder="Enter phone number"
                                            class="w-full" aria-describedby="phoneNumber-error" />
                                    </Field>
                                    <ErrorMessage name="phoneNumber" class="text-red-500 text-sm"
                                        id="phoneNumber-error" role="alert" />
                                </div>

                                <div class="flex flex-col gap-2">
                                    <label for="gender" class="font-semibold text-gray-700">Gender *</label>
                                    <Field name="gender" v-slot="{ value, handleChange, errors }">
                                        <Select id="gender" :modelValue="value" @update:modelValue="handleChange"
                                                :options="genderOptions" optionLabel="label" optionValue="value"
                                                :class="{ 'p-invalid': errors.length > 0 }" placeholder="Select your gender"  />
                                    </Field>
                                    <ErrorMessage name="gender" class="text-red-500 text-sm" id="gender-error"
                                        role="alert" />
                                </div>

                                <div class="flex flex-col gap-2">
                                    <label for="dateOfBirth" class="font-semibold text-gray-700">Date of Birth
                                        *</label>
                                    <Field name="dateOfBirth" v-slot="{ value, handleChange, errors }">
                                        <DatePicker id="dateOfBirth" :modelValue="value" @update:modelValue="handleChange"
                                            dateFormat="yy-mm-dd" :maxDate="new Date(Date.now() - 18 * 365 * 24 * 60 * 60 * 1000)"
                                            :class="{ 'p-invalid': errors.length > 0 }"
                                            placeholder="Select date of birth" class="w-full" showIcon aria-describedby="dateOfBirth-error" />
                                    </Field>
                                    <ErrorMessage name="dateOfBirth" class="text-red-500 text-sm"
                                        id="dateOfBirth-error" role="alert" />
                                </div>

                                <div class="flex flex-col gap-2 lg:col-span-2">
                                    <label for="address" class="font-semibold text-gray-700">Address</label>
                                    <Field name="address" v-slot="{ field, errors }">
                                        <Textarea id="address" v-bind="field"
                                            :class="{ 'p-invalid': errors.length > 0 }"
                                            placeholder="Enter complete address" class="w-full" rows="2"
                                            aria-describedby="address-error" />
                                    </Field>
                                    <ErrorMessage name="address" class="text-red-500 text-sm" id="address-error"
                                        role="alert" />
                                </div>
                            </div>
                        </div>

                        <!-- Emergency Contact Section -->
                        <div class="space-y-4">
                            <h3
                                class="text-lg font-semibold text-gray-900 border-b border-gray-200 pb-2 flex items-center">
                                <i class="pi pi-phone mr-2 text-blue-600" aria-hidden="true"></i>
                                Emergency Contact
                            </h3>

                            <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
                                <div class="flex flex-col gap-2">
                                    <label for="emergencyContactName" class="font-semibold text-gray-700">Contact
                                        Name</label>
                                    <Field name="emergencyContactName" v-slot="{ field, errors }">
                                        <InputText id="emergencyContactName" v-bind="field"
                                            :class="{ 'p-invalid': errors.length > 0 }"
                                            placeholder="Emergency contact full name" class="w-full"
                                            aria-describedby="emergencyContactName-error" />
                                    </Field>
                                    <ErrorMessage name="emergencyContactName" class="text-red-500 text-sm"
                                        id="emergencyContactName-error" role="alert" />
                                </div>

                                <div class="flex flex-col gap-2">
                                    <label for="emergencyContactPhone" class="font-semibold text-gray-700">Contact
                                        Phone</label>
                                    <Field name="emergencyContactPhone" v-slot="{ field, errors }">
                                        <InputText id="emergencyContactPhone" v-bind="field"
                                            :class="{ 'p-invalid': errors.length > 0 }"
                                            placeholder="Emergency contact phone" class="w-full"
                                            aria-describedby="emergencyContactPhone-error" />
                                    </Field>
                                    <ErrorMessage name="emergencyContactPhone" class="text-red-500 text-sm"
                                        id="emergencyContactPhone-error" role="alert" />
                                </div>
                            </div>
                        </div>

                        <!-- Account Security Section (for new doctors) -->
                        <div v-if="!editingDoctor" class="space-y-4">
                            <h3
                                class="text-lg font-semibold text-gray-900 border-b border-gray-200 pb-2 flex items-center">
                                <i class="pi pi-lock mr-2 text-blue-600" aria-hidden="true"></i>
                                Account Security
                            </h3>

                            <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
                                <div class="flex flex-col gap-2">
                                    <label for="password" class="font-semibold text-gray-700">Password *</label>
                                    <Field name="password" v-slot="{ field, errors }">
                                        <InputText id="password" v-bind="field" autocomplete="new-password"
                                            :inputProps="{ autocomplete: 'new-password' }" type="password"
                                            :class="{ 'p-invalid': errors.length > 0 }"
                                            placeholder="Enter secure password" class="w-full"
                                            aria-describedby="password-error" />
                                    </Field>
                                    <ErrorMessage name="password" class="text-red-500 text-sm" id="password-error"
                                        role="alert" />
                                </div>

                                <div class="flex flex-col gap-2">
                                    <label for="password_confirmation" class="font-semibold text-gray-700">Confirm
                                        Password
                                        *</label>
                                    <Field name="password_confirmation" v-slot="{ field, errors }">
                                        <InputText id="password_confirmation" v-bind="field" type="password"
                                            autocomplete="new-password"
                                            :inputProps="{ autocomplete: 'new-password' }"    
                                            :class="{ 'p-invalid': errors.length > 0 }" placeholder="Confirm password"
                                            class="w-full" aria-describedby="password_confirmation-error" />
                                    </Field>
                                    <ErrorMessage name="password_confirmation" class="text-red-500 text-sm"
                                        id="password_confirmation-error" role="alert" />
                                </div>
                            </div>
                        </div>

                        <!-- Professional Information Section -->
                        <div class="space-y-4">
                            <h3
                                class="text-lg font-semibold text-gray-900 border-b border-gray-200 pb-2 flex items-center">
                                <i class="pi pi-briefcase mr-2 text-blue-600" aria-hidden="true"></i>
                                Professional Information
                            </h3>

                            <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
                                <div class="flex flex-col gap-2">
                                    <label for="specialty" class="font-semibold text-gray-700">Specialty *</label>
                                    <Field name="specialty" v-slot="{ field, errors }">
                                        <InputText id="specialty" v-bind="field"
                                            :class="{ 'p-invalid': errors.length > 0 }"
                                            placeholder="e.g., Cardiology, Pediatrics" class="w-full"
                                            aria-describedby="specialty-error" />
                                    </Field>
                                    <ErrorMessage name="specialty" class="text-red-500 text-sm" id="specialty-error"
                                        role="alert" />
                                </div>

                                <div class="flex flex-col gap-2">
                                    <label for="licenseNumber" class="font-semibold text-gray-700">License Number
                                        *</label>
                                    <Field name="licenseNumber" v-slot="{ field, errors }">
                                        <InputText id="licenseNumber" v-bind="field"
                                            :class="{ 'p-invalid': errors.length > 0 }"
                                            placeholder="Medical license number" class="w-full"
                                            aria-describedby="licenseNumber-error" />
                                    </Field>
                                    <ErrorMessage name="licenseNumber" class="text-red-500 text-sm"
                                        id="licenseNumber-error" role="alert" />
                                </div>

                                <div class="flex flex-col gap-2">
                                    <label for="yearsOfExperience" class="font-semibold text-gray-700">Years of
                                        Experience
                                        *</label>
                                    <Field name="yearsOfExperience" v-slot="{ value, handleChange, errors }">
                                        <InputNumber id="yearsOfExperience" :modelValue="value" @update:modelValue="handleChange"
                                            :class="{ 'p-invalid': errors.length > 0 }"
                                            placeholder="Years of experience" class="w-full" :min="0" :max="50"
                                            :showButtons="true" buttonLayout="horizontal" :step="1"
                                            aria-describedby="yearsOfExperience-error" />
                                    </Field>
                                    <ErrorMessage name="yearsOfExperience" class="text-red-500 text-sm"
                                        id="yearsOfExperience-error" role="alert" />
                                </div>
                            </div>
                        </div>

                        <!-- Weekly Schedule Section -->
                        <div class="space-y-4">
                            <h3
                                class="text-lg font-semibold text-gray-900 border-b border-gray-200 pb-2 flex items-center">
                                <i class="pi pi-calendar mr-2 text-blue-600" aria-hidden="true"></i>
                                Weekly Schedule
                            </h3>

                            <div class="bg-gray-50 rounded-lg p-4">
                                <div class="space-y-4">
                                    <div v-for="(schedule, index) in newDoctor.schedules" :key="index"
                                        class="bg-white rounded-lg border border-gray-200 p-4 shadow-sm">
                                        <div class="space-y-4">
                                            <!-- Schedule Details Row -->
                                            <div
                                                class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-12 gap-4 items-end">
                                                <div class="lg:col-span-4 flex flex-col gap-2">
                                                    <label class="text-sm font-medium text-gray-600">Day of Week</label>
                                                    <Select v-model="schedule.dayOfWeek" :options="daysOfWeek"
                                                        optionLabel="label" optionValue="value" placeholder="Select day"
                                                        class="w-full" @change="updateDayOfWeekName(schedule)" />
                                                </div>
                                                <div class="lg:col-span-3 flex flex-col gap-2">
                                                    <label class="text-sm font-medium text-gray-600">Start Time</label>
                                                    <InputMask v-model="schedule.startTime" mask="99:99"
                                                        placeholder="09:00" class="w-full" />
                                                </div>
                                                <div class="lg:col-span-3 flex flex-col gap-2">
                                                    <label class="text-sm font-medium text-gray-600">End Time</label>
                                                    <InputMask v-model="schedule.endTime" mask="99:99"
                                                        placeholder="17:00" class="w-full" />
                                                </div>
                                                <div class="lg:col-span-2 flex justify-end">
                                                    <Button icon="pi pi-trash" severity="danger" text rounded
                                                        @click="removeScheduleRow(index)" class="hover:bg-red-50"
                                                        :aria-label="`Remove schedule for ${schedule.dayOfWeekName}`"
                                                        v-tooltip.top="'Remove this schedule'" />
                                                </div>
                                            </div>

                                            <!-- Schedule Status Toggle Row -->
                                            <div class="pt-2 border-t border-gray-100">
                                                <div class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
                                                    <ToggleSwitch v-model="schedule.isActive"
                                                        :inputId="`schedule-active-toggle-${index}`"
                                                        :aria-describedby="`schedule-active-help-${index}`" />
                                                    <div class="flex-1">
                                                        <label :for="`schedule-active-toggle-${index}`"
                                                            class="font-medium text-gray-900 cursor-pointer">
                                                            {{ schedule.isActive ? 'Active' : 'Inactive' }}
                                                        </label>
                                                        <p :id="`schedule-active-help-${index}`"
                                                            class="text-sm text-gray-600 mt-1">
                                                            {{ schedule.isActive ? 'This schedule is available for appointments' : 'This schedule is disabled' }}
                                                        </p>
                                                    </div>
                                                    <div class="flex items-center gap-2">
                                                        <i :class="schedule.isActive ? 'pi pi-check-circle text-green-600' : 'pi pi-times-circle text-red-600'"
                                                            aria-hidden="true"></i>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    <div v-if="newDoctor.schedules.length === 0" class="text-center py-8 text-gray-500">
                                        <i class="pi pi-calendar text-4xl mb-4 block text-gray-400"
                                            aria-hidden="true"></i>
                                        <p class="font-medium">No schedule added yet</p>
                                        <p class="text-sm mt-1">Add working days and hours for this doctor</p>
                                    </div>

                                    <div class="flex justify-center pt-2">
                                        <Button label="Add Working Day" icon="pi pi-plus" outlined
                                            @click="addScheduleRow"
                                            class="border-blue-600 text-blue-600 hover:bg-blue-50"
                                            aria-label="Add new working day" />
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </Form>

                <template #footer>
                    <div class="flex justify-end gap-3 p-6 bg-gray-50 border-t border-gray-200">
                        <Button label="Cancel" icon="pi pi-times" @click="isDoctorDialogVisible = false"
                            severity="secondary" outlined :disabled="isSavingDoctor"
                            aria-label="Cancel and close dialog" />
                        <Button type="submit" :label="isSavingDoctor ? 'Saving...' : 'Save Doctor'"
                            :icon="isSavingDoctor ? 'pi pi-spin pi-spinner' : 'pi pi-check'" autofocus
                            form="doctor-form" class="bg-blue-600 hover:bg-blue-700 border-blue-600"
                            :disabled="isSavingDoctor" aria-label="Save doctor" />
                    </div>
                </template>
            </Dialog>

            <!-- Manage Services Dialog -->
            <Dialog header="Manage Assigned Services" v-model:visible="isServicesDialogVisible" modal
                :style="{ width: '90vw', maxWidth: '700px' }" :pt="{
                    header: 'bg-purple-50 text-purple-800 px-6 py-4 border-b border-purple-200',
                    content: 'p-0'
                }" class="overflow-hidden" role="dialog" aria-label="Manage doctor services dialog">
                <div class="p-6">
                    <div v-if="selectedDoctorForServices" class="mb-6">
                        <div class="flex items-center gap-3 mb-4">
                            <div
                                class="size-12 bg-gradient-to-br from-purple-100 to-purple-200 rounded-full flex items-center justify-center">
                                <i class="pi pi-user text-purple-600 text-lg" aria-hidden="true"></i>
                            </div>
                            <div>
                                <h3 class="text-lg font-semibold text-gray-900">{{
                                    selectedDoctorForServices.user.fullName }}</h3>
                                <p class="text-sm text-gray-600">Select services this doctor can provide</p>
                            </div>
                        </div>
                    </div>

                    <!-- Loading State -->
                    <div v-if="isLoadingServices" class="space-y-4">
                        <div class="flex justify-center py-8">
                            <ProgressSpinner style="width: 50px; height: 50px" strokeWidth="4" />
                        </div>
                        <p class="text-center text-gray-600">Loading services...</p>
                    </div>

                    <!-- Services Listbox -->
                    <div v-else-if="allServices.length > 0" class="space-y-4">
                        <div class="flex flex-col gap-2">
                            <label class="font-semibold text-gray-700">Available Services</label>
                        </div>

                        <Listbox v-model="selectedServices" :options="allServices" multiple filter optionLabel="name"
                            class="w-full" listStyle="max-height:400px" :pt="{
                                root: 'border border-gray-300 rounded-lg',
                                header: 'p-3 border-b border-gray-200 bg-gray-50',
                                list: 'p-0',
                                item: 'p-3 border-b border-gray-100 last:border-b-0 hover:bg-purple-50 cursor-pointer transition-colors',
                                itemgroup: 'p-3 bg-gray-100 font-semibold'
                            }" :filterPlaceholder="'Search services...'" aria-label="Select services for doctor">
                            <template #option="slotProps">
                                <div class="flex items-center justify-between w-full">
                                    <div class="flex-1">
                                        <div class="font-semibold text-gray-900">{{ slotProps.option.name }}</div>
                                        <div class="text-sm text-gray-600 mt-1">
                                            {{ slotProps.option.description || 'No description available' }}
                                        </div>
                                    </div>
                                    <div class="text-right ml-4 flex-shrink-0">
                                        <div class="font-bold text-green-700">
                                            {{ new Intl.NumberFormat('en-US', {
                                                style: 'currency', currency: 'USD'
                                            }).format(slotProps.option.price) }}
                                        </div>
                                        <div class="text-sm text-gray-500 flex items-center gap-1">
                                            <i class="pi pi-clock text-xs" aria-hidden="true"></i>
                                            {{ slotProps.option.duration_minutes }}min
                                        </div>
                                    </div>
                                </div>
                            </template>
                        </Listbox>

                        <div v-if="selectedServices.length > 0" class="bg-purple-50 rounded-lg p-4">
                            <h4 class="font-semibold text-purple-800 mb-2">Selected Services ({{ selectedServices.length
                                }})</h4>
                            <div class="flex flex-wrap gap-2">
                                <span v-for="service in selectedServices" :key="service.id"
                                    class="inline-flex items-center px-3 py-1 rounded-full text-sm bg-purple-100 text-purple-800">
                                    {{ service.name }}
                                </span>
                            </div>
                        </div>
                    </div>

                    <!-- Empty State -->
                    <div v-else class="text-center py-8">
                        <i class="pi pi-heart text-4xl text-gray-400 mb-4 block" aria-hidden="true"></i>
                        <p class="text-gray-500 font-medium">No services available</p>
                        <p class="text-sm text-gray-400 mt-1">Add services first to assign them to doctors</p>
                    </div>
                </div>

                <template #footer>
                    <div class="flex justify-end gap-3 p-6 bg-gray-50 border-t border-gray-200">
                        <Button label="Cancel" icon="pi pi-times" @click="isServicesDialogVisible = false"
                            severity="secondary" outlined :disabled="isSavingServices"
                            aria-label="Cancel and close dialog" />
                        <Button :label="isSavingServices ? 'Saving...' : 'Save Changes'"
                            :icon="isSavingServices ? 'pi pi-spin pi-spinner' : 'pi pi-check'"
                            @click="saveDoctorServices" autofocus
                            class="bg-purple-600 hover:bg-purple-700 border-purple-600"
                            :disabled="isSavingServices || isLoadingServices" aria-label="Save service assignments" />
                    </div>
                </template>
            </Dialog>
        </div>
    </div>
</template>