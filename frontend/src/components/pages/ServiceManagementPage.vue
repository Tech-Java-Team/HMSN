```vue
<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

// PrimeVue Imports
import Button from 'primevue/button';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Dialog from 'primevue/dialog';
import InputText from 'primevue/inputtext';
import Textarea from 'primevue/textarea';
import InputNumber from 'primevue/inputnumber';
import ConfirmDialog from 'primevue/confirmdialog';
import Skeleton from 'primevue/skeleton';
import { useConfirm } from "primevue/useconfirm";
import ToggleSwitch from 'primevue/toggleswitch';


// Validation Imports
import { Form, Field, ErrorMessage } from 'vee-validate';
import * as yup from 'yup';

// Local Components
import ErrorDisplay from '@/components/ui/ErrorDisplay.vue';

// --- Component State ---
const confirm = useConfirm();
const services = ref([]);
const isLoading = ref(true);
const error = ref(null);
const isDialogVisible = ref(false);
const editingService = ref(null);
const serviceFormData = ref({});

// --- Validation Schema ---
const schema = yup.object({
    name: yup.string().required().label('Service Name'),
    description: yup.string().nullable(),
    price: yup.number().required().min(0).label('Price'),
    duration_minutes: yup.number().required().integer().min(5).label('Duration'),
    is_active: yup.boolean().default(true).label('Active Status'),
});

// --- API Functions ---
const getServices = async () => {
    isLoading.value = true;
    error.value = null;
    try {
        const response = await axios.get('/api/services');
        services.value = response.data;
    } catch (err) {
        console.error("Failed to fetch services:", err);
        error.value = "Could not load services.";
    } finally {
        isLoading.value = false;
    }
};

const saveService = async (values) => {
    try {
        if (editingService.value) {
            const response = await axios.put(`/api/services/${editingService.value.id}`, values);
            const index = services.value.findIndex(s => s.id === editingService.value.id);
            if (index !== -1) {
                services.value[index] = response.data;
            }
        } else {
            const response = await axios.post('/api/services', values);
            services.value.unshift(response.data);
        }
        isDialogVisible.value = false;
    } catch (error) {
        console.error("Failed to save service:", error.response?.data);
        alert('Failed to save service.');
    }
};

const deleteService = async (service) => {
    try {
        await axios.delete(`/api/services/${service.id}`);
        services.value = services.value.filter(s => s.id !== service.id);
    } catch (error) {
        if (error.response && error.response.status === 409) {
            alert(error.response.data.message);
        } else {
            console.error("Failed to delete service:", error);
            alert('Failed to delete service.');
        }
    }
};

// --- Dialog and Form Functions ---
const openAddDialog = () => {
    editingService.value = null;
    serviceFormData.value = {
        name: '',
        description: '',
        price: 0.00,
        duration_minutes: 0,
        is_active: true,
    };
    isDialogVisible.value = true;
};

const openEditDialog = (serviceToEdit) => {
    editingService.value = serviceToEdit;
    serviceFormData.value = { ...serviceToEdit };
    isDialogVisible.value = true;
};

const confirmDelete = (service, event) => {
    event.stopPropagation();
    confirm.require({
        message: `Are you sure you want to delete the service "${service.name}"?`,
        header: 'Delete Confirmation',
        icon: 'pi pi-exclamation-triangle',
        acceptClass: 'p-button-danger',
        accept: () => deleteService(service),
        reject: () => { }
    });
};

// --- Lifecycle Hook ---
onMounted(() => {
    getServices();
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
                        class="size-12 bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl flex items-center justify-center shadow-lg">
                        <i class="pi pi-cog text-white text-xl" aria-hidden="true"></i>
                    </div>
                    <div>
                        <h1 class="text-3xl md:text-4xl font-bold text-gray-900 leading-tight">
                            Service Management
                        </h1>
                        <p class="text-lg text-gray-600 mt-1">
                            Manage clinic services, pricing, and durations
                        </p>
                    </div>
                </div>

                <!-- Summary Stats -->
                <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <div class="bg-white rounded-xl p-4 shadow-sm border border-gray-200">
                        <div class="flex items-center gap-3">
                            <div class="size-10 bg-purple-100 rounded-lg flex items-center justify-center">
                                <i class="pi pi-list text-purple-600" aria-hidden="true"></i>
                            </div>
                            <div>
                                <p class="text-2xl font-bold text-gray-900">{{ services.length }}</p>
                                <p class="text-sm text-gray-600">Total Services</p>
                            </div>
                        </div>
                    </div>
                    <div class="bg-white rounded-xl p-4 shadow-sm border border-gray-200">
                        <div class="flex items-center gap-3">
                            <div class="size-10 bg-green-100 rounded-lg flex items-center justify-center">
                                <i class="pi pi-dollar text-green-600" aria-hidden="true"></i>
                            </div>
                            <div>
                                <p class="text-2xl font-bold text-gray-900">
                                    {{services.length > 0 ? new Intl.NumberFormat('en-US', {
                                        style: 'currency',
                                        currency: 'USD'
                                    }).format(Math.round(services.reduce((sum, s) => sum + Number(s.price), 0) /
                                        services.length)) : '$0'}}
                                </p>
                                <p class="text-sm text-gray-600">Avg. Price</p>
                            </div>
                        </div>
                    </div>
                    <div class="bg-white rounded-xl p-4 shadow-sm border border-gray-200">
                        <div class="flex items-center gap-3">
                            <div class="size-10 bg-blue-100 rounded-lg flex items-center justify-center">
                                <i class="pi pi-clock text-blue-600" aria-hidden="true"></i>
                            </div>
                            <div>
                                <p class="text-2xl font-bold text-gray-900">
                                    {{services.length > 0 ? Math.round(services.reduce((sum, s) => sum +
                                        s.duration_minutes, 0) / services.length) : 0}}m
                                </p>
                                <p class="text-sm text-gray-600">Avg. Duration</p>
                            </div>
                        </div>
                    </div>
                </div>
            </header>

            <!-- Main Content -->
            <main role="main" aria-label="Services management">
                <div class="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                    <!-- Table Header -->
                    <div class="px-6 py-4 border-b border-gray-200 bg-gray-50">
                        <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                            <div>
                                <h2 class="text-xl font-semibold text-gray-900">Clinic Services</h2>
                                <p class="text-sm text-gray-600 mt-1">Manage your healthcare services</p>
                            </div>
                            <Button label="Add Service" icon="pi pi-plus" size="small" @click="openAddDialog"
                                class="bg-purple-600 hover:bg-purple-700 border-purple-600"
                                aria-label="Add new service" />
                        </div>
                    </div>

                    <!-- Error State -->
                    <ErrorDisplay v-if="error" :message="error" @retry="getServices" />

                    <!-- Loading State -->
                    <div v-else-if="isLoading" class="p-6">
                        <div v-for="n in 6" :key="n"
                            class="flex items-center gap-4 py-4 border-b border-gray-100 last:border-b-0">
                            <div class="flex items-center gap-4 flex-1">
                                <div class="size-10 bg-gray-100 rounded-lg">
                                    <Skeleton width="100%" height="100%" />
                                </div>
                                <div class="space-y-2 flex-1">
                                    <Skeleton width="200px" height="1rem" />
                                    <Skeleton width="150px" height="0.875rem" />
                                </div>
                            </div>
                            <div class="space-y-2">
                                <Skeleton width="80px" height="1rem" />
                                <Skeleton width="60px" height="0.875rem" />
                            </div>
                            <Skeleton width="100px" height="2rem" />
                        </div>
                    </div>

                    <!-- Empty State -->
                    <div v-else-if="!isLoading && services.length === 0" class="text-center py-16">
                        <div class="inline-flex items-center justify-center size-20 bg-gray-100 rounded-full mb-6">
                            <i class="pi pi-cog text-3xl text-gray-400" aria-hidden="true"></i>
                        </div>
                        <h3 class="text-2xl font-semibold text-gray-900 mb-2">No Services Found</h3>
                        <p class="text-gray-600 mb-6 max-w-md mx-auto">
                            Get started by adding your first clinic service.
                        </p>
                        <Button label="Add First Service" icon="pi pi-plus" @click="openAddDialog"
                            class="bg-purple-600 hover:bg-purple-700 border-purple-600" />
                    </div>

                    <!-- Data Table -->
                    <DataTable v-else :value="services" :loading="false" class="p-datatable-sm" :pt="{
                        table: 'min-w-full',
                        thead: 'bg-gray-50',
                        tbody: 'divide-y divide-gray-200'
                    }" role="table" aria-label="Services data table">
                        <!-- Service Name Column -->
                        <Column field="name" header="Service Name" :sortable="true" style="min-width: 250px" :pt="{
                            headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                            bodyCell: 'px-6 py-4'
                        }">
                            <template #body="{ data }">
                                <div class="flex items-center gap-3">
                                    <div
                                        class="size-10 bg-gradient-to-br from-purple-100 to-purple-200 rounded-lg flex items-center justify-center">
                                        <i class="pi pi-heart text-purple-600" aria-hidden="true"></i>
                                    </div>
                                    <div>
                                        <p class="font-semibold text-gray-900">{{ data.name }}</p>
                                        <p v-if="data.description" class="text-sm text-gray-600 truncate max-w-xs">
                                            {{ data.description }}
                                        </p>
                                    </div>
                                </div>
                            </template>
                        </Column>

                        <!-- Price Column -->
                        <Column field="price" header="Price" :sortable="true" style="min-width: 120px" :pt="{
                            headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                            bodyCell: 'px-6 py-4'
                        }">
                            <template #body="{ data }">
                                <div class="text-right">
                                    <span class="text-lg font-bold text-green-700">
                                        {{ new Intl.NumberFormat('en-US', {
                                            style: 'currency', currency: 'USD'
                                        }).format(data.price) }}
                                    </span>
                                    <p class="text-xs text-gray-500">Per session</p>
                                </div>
                            </template>
                        </Column>

                        <!-- Duration Column -->
                        <Column field="duration_minutes" header="Duration" :sortable="true" style="min-width: 120px"
                            :pt="{
                                headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                                bodyCell: 'px-6 py-4'
                            }">
                            <template #body="{ data }">
                                <div class="flex items-center gap-2">
                                    <i class="pi pi-clock text-blue-600 text-sm" aria-hidden="true"></i>
                                    <span class="font-medium text-gray-900">{{ data.duration_minutes }}</span>
                                    <span class="text-sm text-gray-500">min</span>
                                </div>
                            </template>
                        </Column>

                        <!-- Actions Column -->
                        <Column header="Actions" style="min-width: 120px" :pt="{
                            headerCell: 'px-6 py-4 text-center text-sm font-semibold text-gray-900 bg-gray-50',
                            bodyCell: 'px-6 py-4 text-center'
                        }">
                            <template #body="{ data }">
                                <div class="flex gap-2 justify-center">
                                    <Button icon="pi pi-pencil" severity="info" text rounded
                                        @click="openEditDialog(data)" class="hover:bg-blue-50"
                                        :aria-label="`Edit ${data.name} service`" v-tooltip.top="'Edit service'" />
                                    <Button icon="pi pi-trash" severity="danger" text rounded
                                        @click="confirmDelete(data, $event)" class="hover:bg-red-50"
                                        :aria-label="`Delete ${data.name} service`" v-tooltip.top="'Delete service'" />
                                </div>
                            </template>
                        </Column>
                    </DataTable>
                </div>
            </main>

            <!-- Add/Edit Service Dialog -->
            <Dialog :header="editingService ? 'Edit Service' : 'Add New Service'" v-model:visible="isDialogVisible"
                modal :style="{ width: '90vw', maxWidth: '600px' }" class="p-fluid overflow-hidden" :pt="{
                    header: 'bg-purple-50 text-purple-800 px-6 py-4 border-b border-purple-200',
                    content: 'p-0'
                }" role="dialog" :aria-label="editingService ? 'Edit service dialog' : 'Add new service dialog'">
                <Form @submit="saveService" :validation-schema="schema" :initial-values="serviceFormData"
                    id="service-form" class="p-fluid">
                    <div class="p-6 space-y-6">
                        <!-- Service Information Section -->
                        <div class="space-y-4">
                            <h3
                                class="text-lg font-semibold text-gray-900 border-b border-gray-200 pb-2 flex items-center">
                                <i class="pi pi-info-circle mr-2 text-purple-600" aria-hidden="true"></i>
                                Service Information
                            </h3>

                            <Field name="name" v-slot="{ field, errors }">
                                <div class="flex flex-col gap-2">
                                    <label for="service-name" class="font-semibold text-gray-700">
                                        Service Name *
                                    </label>
                                    <InputText id="service-name" v-bind="field"
                                        :class="{ 'p-invalid': errors.length > 0 }" placeholder="Enter service name"
                                        class="w-full" aria-describedby="service-name-error" />
                                    <ErrorMessage name="name" class="text-red-500 text-sm" id="service-name-error"
                                        role="alert" />
                                </div>
                            </Field>

                            <Field name="description" v-slot="{ field }">
                                <div class="flex flex-col gap-2">
                                    <label for="service-description" class="font-semibold text-gray-700">
                                        Description
                                    </label>
                                    <Textarea id="service-description" v-bind="field" rows="3"
                                        placeholder="Describe the service (optional)" class="w-full" />
                                </div>
                            </Field>
                        </div>

                        <!-- Pricing & Duration Section -->
                        <div class="space-y-4">
                            <h3
                                class="text-lg font-semibold text-gray-900 border-b border-gray-200 pb-2 flex items-center">
                                <i class="pi pi-dollar mr-2 text-purple-600" aria-hidden="true"></i>
                                Pricing & Duration
                            </h3>

                            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                                <Field name="price" v-slot="{ value, handleChange, errors }">
                                    <div class="flex flex-col gap-2">
                                        <label for="service-price" class="font-semibold text-gray-700">
                                            Price *
                                        </label>
                                        <InputNumber inputId="service-price" :modelValue="value"
                                            @update:modelValue="handleChange" mode="currency" currency="USD"
                                            locale="en-US" :class="{ 'p-invalid': errors.length > 0 }" />
                                        <ErrorMessage name="price" class="text-red-500 text-sm" id="service-price-error"
                                            role="alert" />
                                    </div>
                                </Field>

                                <Field name="duration_minutes" v-slot="{ value, handleChange, errors }">
                                    <div class="flex flex-col gap-2">
                                        <label for="service-duration" class="font-semibold text-gray-700">
                                            Duration (minutes) *
                                        </label>
                                        <InputNumber inputId="service-duration" v-bind="duration_minutes"
                                            :class="{ 'p-invalid': errors.length > 0 }" @update:modelValue="handleChange" :modelValue="value" placeholder="30" :min="5"
                                            :step="5" class="w-full" aria-describedby="service-duration-error" />
                                        <ErrorMessage name="duration_minutes" class="text-red-500 text-sm"
                                            id="service-duration-error" role="alert" />
                                    </div>
                                </Field>
                            </div>
                            <Field name="is_active" v-slot="{ value, handleChange }">
                                <div class="flex flex-col gap-3">
                                    <label class="font-semibold text-gray-700">Service Status</label>
                                    <div class="flex items-center gap-3 p-4 bg-gray-50 rounded-lg">
                                        <ToggleSwitch :modelValue="value" @update:modelValue="handleChange"
                                            inputId="service-active-toggle" aria-describedby="service-active-help" />
                                        <div class="flex-1">
                                            <label for="service-active-toggle"
                                                class="font-medium text-gray-900 cursor-pointer">
                                                {{ value ? 'Active' : 'Inactive' }}
                                            </label>
                                            <p id="service-active-help" class="text-sm text-gray-600 mt-1">
                                                {{ value ? 'Service is available for booking' : 'Service is hidden from booking' }}
                                            </p>
                                        </div>
                                        <div class="flex items-center gap-2">
                                            <i :class="value ? 'pi pi-check-circle text-green-600' : 'pi pi-times-circle text-red-600'"
                                                aria-hidden="true"></i>
                                        </div>
                                    </div>
                                </div>
                            </Field>
                        </div>
                    </div>
                </Form>

                <template #footer>
                    <div class="flex justify-end gap-3 p-6 bg-gray-50 border-t border-gray-200">
                        <Button label="Cancel" icon="pi pi-times" @click="isDialogVisible = false" severity="secondary"
                            outlined aria-label="Cancel and close dialog" />
                        <Button label="Save Service" icon="pi pi-check" type="submit" form="service-form" autofocus
                            class="bg-purple-600 hover:bg-purple-700 border-purple-600" aria-label="Save service" />
                    </div>
                </template>
            </Dialog>
        </div>
    </div>
</template>
