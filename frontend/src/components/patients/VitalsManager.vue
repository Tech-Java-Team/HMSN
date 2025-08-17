<script setup>
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';
import { format, parseISO } from 'date-fns';

// PrimeVue Imports
import Button from 'primevue/button';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Dialog from 'primevue/dialog';
import InputText from 'primevue/inputtext';
import Select from 'primevue/select';
import DatePicker from 'primevue/datepicker';
import { useConfirm } from "primevue/useconfirm";

// Validation Imports
import { Form, Field, ErrorMessage } from 'vee-validate';
import * as yup from 'yup';

const props = defineProps({
    patientId: {
        type: [Number, String],
        required: true
    }
});

// --- Component State ---
const confirm = useConfirm();
const vitals = ref([]);
const vitalTypes = ref([]);
const isLoading = ref(true);
const error = ref(null);
const isDialogVisible = ref(false);
const editingVital = ref(null);
const vitalFormData = ref({});

// --- Validation Schema ---
const schema = yup.object({
    vital_type_id: yup.number().required().label('Vital Type'),
    value: yup.string().required().label('Value'),
    recorded_at: yup.date().nullable().label('Time Recorded'),
});

// --- Computed Property to find the unit for the selected vital type ---
const selectedVitalUnit = computed(() => {
    if (vitalFormData.value.vital_type_id) {
        const selectedType = vitalTypes.value.find(t => t.id === vitalFormData.value.vital_type_id);
        return selectedType ? selectedType.unit : '';
    }
    return '';
});

// --- API Functions ---
const getVitalsData = async () => {
    isLoading.value = true;
    error.value = null;
    
    try {
        const [vitalsResponse, typesResponse] = await Promise.all([
            axios.get(`/api/patients/${props.patientId}/vitals`),
            axios.get('/api/vital-types')
        ]);
        vitals.value = vitalsResponse.data;
        vitalTypes.value = typesResponse.data;
    } catch (err) {
        console.error("Failed to fetch vitals data:", err);
        error.value = "Could not load vitals data.";
    } finally {
        isLoading.value = false;
    }
};

const saveVital = async (values) => {
    try {
        if (editingVital.value) {
            const response = await axios.put(`/api/vitals/${editingVital.value.id}`, values);
            const index = vitals.value.findIndex(v => v.id === editingVital.value.id);
            if (index !== -1) {
                vitals.value[index] = response.data;
            }
        } else {
            const response = await axios.post(`/api/patients/${props.patientId}/vitals`, values);
            vitals.value.unshift(response.data);
        }
        isDialogVisible.value = false;
    } catch (error) {
        console.error("Failed to save vital:", error.response?.data);
        alert('Failed to save vital reading.');
    }
};

const deleteVital = async (vital) => {
    try {
        await axios.delete(`/api/vitals/${vital.id}`);
        vitals.value = vitals.value.filter(v => v.id !== vital.id);
    } catch (error) {
        console.error("Failed to delete vital:", error);
        alert('Failed to delete vital reading.');
    }
};

// --- Dialog and Form Functions ---
const openAddDialog = () => {
    editingVital.value = null;
    vitalFormData.value = {
        vital_type_id: null,
        value: '',
        recorded_at: new Date(),
    };
    isDialogVisible.value = true;
};

const openEditDialog = (vitalToEdit) => {
    editingVital.value = vitalToEdit;
    vitalFormData.value = {
        ...vitalToEdit,
        recorded_at: vitalToEdit.recorded_at ? parseISO(vitalToEdit.recorded_at) : null
    };
    isDialogVisible.value = true;
};

const confirmDelete = (vital, event) => {
    event.stopPropagation();
    confirm.require({
        message: `Are you sure you want to delete this vital reading?`,
        header: 'Delete Confirmation',
        icon: 'pi pi-exclamation-triangle',
        acceptClass: 'p-button-danger',
        accept: () => deleteVital(vital),
        reject: () => {}
    });
};

// --- Lifecycle Hook ---
onMounted(() => {
    getVitalsData();
});
</script>

<template>
    <div class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <!-- Header Section -->
        <header class="px-6 py-4 border-b border-gray-200 bg-green-50">
            <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
                <div class="flex items-center gap-3">
                    <div>
                        <h2 class="text-lg font-semibold text-green-800">
                            Vitals History
                        </h2>
                        <p class="text-sm text-green-600">
                            {{ vitals.length }} reading{{ vitals.length !== 1 ? 's' : '' }} on record
                        </p>
                    </div>
                </div>
                <Button
                    label="Add Vitals"
                    icon="pi pi-plus"
                    size="small"
                    @click="openAddDialog"
                    class="bg-green-600 hover:bg-green-700 border-green-600 whitespace-nowrap"
                    :disabled="isLoading"
                    aria-label="Add new vital signs"
                />
            </div>
        </header>

        <!-- Loading State -->
        <div v-if="isLoading" class="flex items-center justify-center py-12" role="status" aria-live="polite">
            <div class="flex items-center space-x-3">
                <i class="pi pi-spin pi-spinner text-green-600 text-xl" aria-hidden="true"></i>
                <span class="text-gray-600">Loading vitals...</span>
            </div>
        </div>

        <!-- Error State -->
        <div v-else-if="error" class="text-center py-12" role="alert" aria-live="assertive">
            <i class="pi pi-exclamation-circle text-4xl text-red-500 mb-3 block" aria-hidden="true"></i>
            <p class="text-red-600 font-medium mb-2">{{ error }}</p>
            <Button
                label="Try Again"
                icon="pi pi-refresh"
                outlined
                @click="getVitalsData"
                class="border-red-600 text-red-600 hover:bg-red-50"
                aria-label="Retry loading vitals"
            />
        </div>

        <!-- Data Table -->
        <div v-else class="overflow-x-auto">
            <DataTable 
                :value="vitals" 
                class="p-datatable-sm"
                :pt="{
                    table: 'min-w-full',
                    thead: 'bg-gray-50',
                    tbody: 'divide-y divide-gray-200'
                }"
                role="table"
                aria-label="Vitals history table"
            >
                <template #empty>
                    <div class="text-center py-12">
                        <i class="pi pi-chart-line text-4xl text-gray-400 mb-3 block" aria-hidden="true"></i>
                        <p class="text-gray-500 font-medium">No vitals recorded</p>
                        <p class="text-sm text-gray-400 mb-4">No vital signs have been recorded for this patient</p>
                        <Button
                            label="Add First Reading"
                            icon="pi pi-plus"
                            outlined
                            @click="openAddDialog"
                            class="border-green-600 text-green-600 hover:bg-green-50"
                            aria-label="Add first vital reading"
                        />
                    </div>
                </template>

                <Column 
                    field="type.name" 
                    header="Vital Sign" 
                    style="min-width: 200px"
                    :pt="{
                        headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                        bodyCell: 'px-6 py-4 text-sm'
                    }"
                >
                    <template #body="{ data }">
                        <div class="flex items-center gap-3">
                            <span class="font-medium text-gray-900">{{ data.type?.name || 'Unknown' }}</span>
                        </div>
                    </template>
                </Column>

                <Column 
                    field="value" 
                    header="Value" 
                    style="min-width: 120px"
                    :pt="{
                        headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                        bodyCell: 'px-6 py-4 text-sm'
                    }"
                >
                    <template #body="{ data }">
                        <span class="font-semibold text-green-700 text-lg">{{ data.value }}</span>
                    </template>
                </Column>

                <Column 
                    field="type.unit" 
                    header="Unit" 
                    style="min-width: 100px"
                    :pt="{
                        headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                        bodyCell: 'px-6 py-4 text-sm text-gray-600'
                    }"
                >
                    <template #body="{ data }">
                        {{ data.type?.unit || '-' }}
                    </template>
                </Column>

                <Column 
                    header="Recorded At" 
                    style="min-width: 180px"
                    :pt="{
                        headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                        bodyCell: 'px-6 py-4 text-sm text-gray-900'
                    }"
                >
                    <template #body="{ data }">
                        <div class="space-y-1">
                            <p class="font-medium">{{ format(parseISO(data.recorded_at), 'MMM d, yyyy') }}</p>
                            <p class="text-xs text-gray-500">{{ format(parseISO(data.recorded_at), 'HH:mm') }}</p>
                        </div>
                    </template>
                </Column>

                <Column 
                    header="Actions" 
                    style="min-width: 120px"
                    :pt="{
                        headerCell: 'px-6 py-4 text-center text-sm font-semibold text-gray-900 bg-gray-50',
                        bodyCell: 'px-6 py-4 text-center'
                    }"
                >
                    <template #body="{ data }">
                        <div class="flex gap-2 justify-center">
                            <Button 
                                icon="pi pi-pencil" 
                                severity="info" 
                                text 
                                rounded 
                                @click="openEditDialog(data)" 
                                class="hover:bg-blue-50"
                                :aria-label="`Edit ${data.type?.name} reading`"
                                v-tooltip.top="'Edit reading'"
                            />
                            <Button 
                                icon="pi pi-trash" 
                                severity="danger" 
                                text 
                                rounded 
                                @click="confirmDelete(data, $event)" 
                                class="hover:bg-red-50"
                                :aria-label="`Delete ${data.type?.name} reading`"
                                v-tooltip.top="'Delete reading'"
                            />
                        </div>
                    </template>
                </Column>
            </DataTable>
        </div>

        <!-- Add/Edit Dialog -->
        <Dialog 
            :header="editingVital ? 'Edit Vital Reading' : 'Add New Vital Reading'" 
            v-model:visible="isDialogVisible" 
            modal 
            :style="{ width: '90vw', maxWidth: '600px' }"
            class="p-fluid"
            :pt="{
                header: 'bg-green-50 text-green-800 px-6 py-4 border-b border-green-200',
                content: 'p-0'
            }"
            role="dialog"
            :aria-label="editingVital ? 'Edit vital reading dialog' : 'Add new vital reading dialog'"
        >
            <Form 
                @submit="saveVital" 
                :validation-schema="schema" 
                :initial-values="vitalFormData" 
                id="vital-form"
                class="p-fluid"
            >
                <div class="p-6 space-y-6">
                    <!-- Vital Information Section -->
                    <div class="space-y-4">
                        <h3 class="text-lg font-semibold text-gray-900 border-b border-gray-200 pb-2 flex items-center">
                            <i class="pi pi-info-circle mr-2 text-green-600" aria-hidden="true"></i>
                            Vital Information
                        </h3>
                        
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <Field name="vital_type_id" v-slot="{ value, handleChange, errors }">
                                <div class="flex flex-col gap-2">
                                    <label for="vital-type" class="font-semibold text-gray-700">
                                        Vital Type *
                                    </label>
                                    <Select
                                        id="vital-type"
                                        :modelValue="value"
                                        @update:modelValue="handleChange"
                                        :options="vitalTypes"
                                        optionLabel="name"
                                        optionValue="id"
                                        :class="{ 'p-invalid': errors.length > 0 }"
                                        placeholder="Select vital type"
                                        class="w-full"
                                        aria-describedby="vital-type-error"
                                    >
                                        <template #option="slotProps">
                                            <div class="flex items-center justify-between w-full">
                                                <span>{{ slotProps.option.name }}</span>
                                                <span class="text-sm text-gray-500">{{ slotProps.option.unit }}</span>
                                            </div>
                                        </template>
                                    </Select>
                                    <ErrorMessage 
                                        name="vital_type_id" 
                                        class="text-red-500 text-sm" 
                                        id="vital-type-error"
                                        role="alert"
                                    />
                                </div>
                            </Field>

                            <Field name="value" v-slot="{ field, errors }">
                                <div class="flex flex-col gap-2">
                                    <label for="vital-value" class="font-semibold text-gray-700">
                                        Value * 
                                        <span v-if="selectedVitalUnit" class="text-sm text-gray-500 font-normal">
                                            ({{ selectedVitalUnit }})
                                        </span>
                                    </label>
                                    <InputText 
                                        id="vital-value" 
                                        v-bind="field" 
                                        :class="{ 'p-invalid': errors.length > 0 }" 
                                        placeholder="Enter measurement value"
                                        class="w-full"
                                        aria-describedby="vital-value-error"
                                    />
                                    <ErrorMessage 
                                        name="value" 
                                        class="text-red-500 text-sm" 
                                        id="vital-value-error"
                                        role="alert"
                                    />
                                </div>
                            </Field>

                            <Field name="recorded_at" v-slot="{ value, handleChange }" class="md:col-span-2">
                                <div class="flex flex-col gap-2">
                                    <label for="vital-recorded-at" class="font-semibold text-gray-700">
                                        Time Recorded
                                    </label>
                                    <DatePicker
                                        id="vital-recorded-at"
                                        :modelValue="value"
                                        @update:modelValue="handleChange"
                                        showTime
                                        hourFormat="24"
                                        :maxDate="new Date()"
                                        placeholder="Select date and time"
                                        class="w-full"
                                    />
                                </div>
                            </Field>
                        </div>
                    </div>
                </div>
            </Form>

            <template #footer>
                <div class="flex justify-end gap-3 p-6 bg-gray-50 border-t border-gray-200">
                    <Button 
                        label="Cancel" 
                        icon="pi pi-times" 
                        @click="isDialogVisible = false" 
                        severity="secondary"
                        outlined
                        aria-label="Cancel and close dialog"
                    />
                    <Button 
                        label="Save Reading" 
                        icon="pi pi-check" 
                        type="submit" 
                        form="vital-form" 
                        autofocus
                        class="bg-green-600 hover:bg-green-700 border-green-600"
                        aria-label="Save vital reading"
                    />
                </div>
            </template>
        </Dialog>
    </div>
</template>