<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { format, parseISO } from 'date-fns';

// PrimeVue Imports
import Button from 'primevue/button';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Dialog from 'primevue/dialog';
import InputText from 'primevue/inputtext';
import Textarea from 'primevue/textarea';
import Select from 'primevue/select';
import DatePicker from 'primevue/datepicker';
import Tag from 'primevue/tag';
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
const illnesses = ref([]);
const isLoading = ref(true);
const error = ref(null);
const isDialogVisible = ref(false);
const editingIllness = ref(null);
const illnessFormData = ref({});
const severityOptions = ref([
    { label: 'Mild', value: 'Mild' },
    { label: 'Moderate', value: 'Moderate' },
    { label: 'Severe', value: 'Severe' }
]);

// --- Validation Schema ---
const schema = yup.object({
    name: yup.string().required().label('Illness Name'),
    severity: yup.string().nullable().label('Severity'),
    date_of_diagnosis: yup.date().nullable().label('Diagnosis Date'),
    medications: yup.string().nullable(),
    notes: yup.string().nullable(),
});

// --- Helper Functions ---
const getSeverityTag = (severity) => {
    if (!severity) return null;
    switch (severity.toLowerCase()) {
        case 'severe': return 'danger';
        case 'moderate': return 'warning';
        case 'mild':
        default:
            return 'info';
    }
};

// --- API Functions ---
const getIllnesses = async () => {
    isLoading.value = true;
    error.value = null;
    
    try {
        const response = await axios.get(`/api/patients/${props.patientId}/chronicIllnesses`);
        illnesses.value = response.data;
    } catch (err) {
        console.error("Failed to fetch chronic illnesses:", err);
        error.value = "Could not load chronic illness data.";
    } finally {
        isLoading.value = false;
    }
};

const saveIllness = async (values) => {
    try {
        if (editingIllness.value) {
            const response = await axios.put(`/api/chronicIllnesses/${editingIllness.value.id}`, values);
            const index = illnesses.value.findIndex(i => i.id === editingIllness.value.id);
            if (index !== -1) {
                illnesses.value[index] = response.data;
            }
        } else {
            const response = await axios.post(`/api/patients/${props.patientId}/chronicIllnesses`, values);
            illnesses.value.unshift(response.data);
        }
        isDialogVisible.value = false;
    } catch (error) {
        console.error("Failed to save illness:", error.response?.data);
        alert('Failed to save illness.');
    }
};

const deleteIllness = async (illness) => {
    try {
        await axios.delete(`/api/chronicIllnesses/${illness.id}`);
        illnesses.value = illnesses.value.filter(i => i.id !== illness.id);
    } catch (error) {
        console.error("Failed to delete illness:", error);
        alert('Failed to delete illness.');
    }
};

// --- Dialog and Form Functions ---
const openAddDialog = () => {
    editingIllness.value = null;
    illnessFormData.value = {
        name: '',
        severity: null, // Changed from 'Mild' to null for better UX
        date_of_diagnosis: null,
        medications: '',
        notes: ''
    };
    isDialogVisible.value = true;
};

const openEditDialog = (illnessToEdit) => {
    editingIllness.value = illnessToEdit;
    illnessFormData.value = {
        ...illnessToEdit,
        date_of_diagnosis: illnessToEdit.date_of_diagnosis ? parseISO(illnessToEdit.date_of_diagnosis) : null
    };
    isDialogVisible.value = true;
};

const confirmDelete = (illness) => {
    confirm.require({
        message: `Are you sure you want to delete the chronic illness "${illness.name}"?`,
        header: 'Delete Confirmation',
        icon: 'pi pi-exclamation-triangle',
        acceptClass: 'p-button-danger',
        accept: () => deleteIllness(illness),
        reject: () => {
        }
    });
};

// --- Lifecycle Hook ---
onMounted(() => {
    getIllnesses();
});
</script>

<template>
    <section class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden" 
             role="region" 
             aria-labelledby="chronic-illnesses-heading">
        
        
        <!-- Header Section -->
        <header class="px-6 py-4 border-b border-gray-200 bg-orange-50">
            <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
                <div class="flex items-center gap-3">
                    <div>
                        <h2 id="chronic-illnesses-heading" class="text-lg font-semibold text-orange-800">
                            Chronic Illnesses
                        </h2>
                        <p class="text-sm text-orange-600">
                            {{ illnesses.length }} condition{{ illnesses.length !== 1 ? 's' : '' }} on record
                        </p>
                    </div>
                </div>
                <Button 
                    label="Add Illness" 
                    icon="pi pi-plus" 
                    size="small" 
                    @click="openAddDialog"
                    class="bg-orange-600 hover:bg-orange-700 border-orange-600 whitespace-nowrap" 
                    :disabled="isLoading"
                    aria-label="Add new chronic illness"
                />
            </div>
        </header>

        <!-- Loading State -->
        <div v-if="isLoading" class="flex items-center justify-center py-12" role="status" aria-live="polite">
            <div class="flex items-center space-x-3">
                <i class="pi pi-spin pi-spinner text-orange-600 text-xl" aria-hidden="true"></i>
                <span class="text-gray-600">Loading chronic illnesses...</span>
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
                @click="getIllnesses"
                class="border-red-600 text-red-600 hover:bg-red-50"
                aria-label="Retry loading chronic illnesses"
            />
        </div>

        <!-- Data Table -->
        <div v-else class="overflow-x-auto">
            <DataTable 
                :value="illnesses" 
                class="p-datatable-sm"
                :pt="{
                    table: 'min-w-full',
                    thead: 'bg-gray-50',
                    tbody: 'divide-y divide-gray-200'
                }"
                role="table"
                aria-label="Chronic illnesses table"
            >
                <template #empty>
                    <div class="text-center py-12">
                        <i class="pi pi-check-circle text-4xl text-green-500 mb-3 block" aria-hidden="true"></i>
                        <p class="text-gray-500 font-medium">No chronic illnesses recorded</p>
                        <p class="text-sm text-gray-400 mb-4">This patient has no recorded chronic conditions</p>
                        <Button 
                            label="Add First Illness" 
                            icon="pi pi-plus" 
                            outlined 
                            @click="openAddDialog"
                            class="border-orange-600 text-orange-600 hover:bg-orange-50"
                            aria-label="Add first chronic illness"
                        />
                    </div>
                </template>

                <Column 
                    field="name" 
                    header="Illness Name" 
                    style="min-width: 200px"
                    :pt="{
                        headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                        bodyCell: 'px-6 py-4 text-sm'
                    }"
                >
                    <template #body="{ data }">
                        <div class="flex items-center gap-3">
                            <div class="w-8 h-8 bg-orange-100 rounded-full flex items-center justify-center">
                                <i class="pi pi-heart text-orange-600 text-xs" aria-hidden="true"></i>
                            </div>
                            <span class="font-medium text-gray-900">{{ data.name }}</span>
                        </div>
                    </template>
                </Column>

                <Column 
                    field="severity" 
                    header="Severity" 
                    style="min-width: 120px"
                    :pt="{
                        headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                        bodyCell: 'px-6 py-4 text-sm'
                    }"
                >
                    <template #body="{ data }">
                        <Tag 
                            v-if="data.severity" 
                            :value="data.severity" 
                            :severity="getSeverityTag(data.severity)"
                            class="font-medium"
                        />
                        <span v-else class="text-gray-400 italic">Not specified</span>
                    </template>
                </Column>

                <Column 
                    header="Diagnosis Date" 
                    style="min-width: 140px"
                    :pt="{
                        headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                        bodyCell: 'px-6 py-4 text-sm text-gray-900'
                    }"
                >
                    <template #body="{ data }">
                        <div v-if="data.date_of_diagnosis" class="space-y-1">
                            <p class="font-medium">{{ format(parseISO(data.date_of_diagnosis), 'MMM d, yyyy') }}</p>
                            <p class="text-xs text-gray-500">{{ format(parseISO(data.date_of_diagnosis), 'yyyy-MM-dd') }}</p>
                        </div>
                        <span v-else class="text-gray-400 italic">Not recorded</span>
                    </template>
                </Column>

                <Column 
                    field="medications" 
                    header="Medications" 
                    style="min-width: 200px"
                    :pt="{
                        headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                        bodyCell: 'px-6 py-4 text-sm text-gray-900'
                    }"
                >
                    <template #body="{ data }">
                        <span class="line-clamp-2">{{ data.medications || 'No medications listed' }}</span>
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
                                :aria-label="`Edit ${data.name}`"
                                v-tooltip.top="'Edit illness'"
                            />
                            <Button 
                                icon="pi pi-trash" 
                                severity="danger" 
                                text 
                                rounded 
                                @click="confirmDelete(data)"
                                class="hover:bg-red-50"
                                :aria-label="`Delete ${data.name}`"
                                v-tooltip.top="'Delete illness'"
                            />
                        </div>
                    </template>
                </Column>
            </DataTable>
        </div>

        <!-- Add/Edit Dialog -->
        <Dialog 
            :header="editingIllness ? 'Edit Chronic Illness' : 'Add New Chronic Illness'" 
            v-model:visible="isDialogVisible" 
            modal 
            :style="{ width: '90vw', maxWidth: '700px' }"
            class="p-fluid"
            :pt="{
                header: 'bg-orange-50 text-orange-800 px-6 py-4 border-b border-orange-200',
                content: 'p-0'
            }"
            role="dialog"
            :aria-label="editingIllness ? 'Edit chronic illness dialog' : 'Add new chronic illness dialog'"
        >
            <Form 
                @submit="saveIllness" 
                :validation-schema="schema" 
                :initial-values="illnessFormData" 
                id="illness-form"
                class="p-fluid"
            >
                <div class="p-6 space-y-6">
                    <!-- Basic Information Section -->
                    <div class="space-y-4">
                        <h3 class="text-lg font-semibold text-gray-900 border-b border-gray-200 pb-2 flex items-center">
                            <i class="pi pi-info-circle mr-2 text-orange-600" aria-hidden="true"></i>
                            Illness Information
                        </h3>
                        
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <Field name="name" v-slot="{ field, errors }" class="md:col-span-2">
                                <div class="flex flex-col gap-2">
                                    <label for="illness-name" class="font-semibold text-gray-700">
                                        Illness Name *
                                    </label>
                                    <InputText 
                                        id="illness-name" 
                                        v-bind="field" 
                                        :class="{ 'p-invalid': errors.length > 0 }"
                                        placeholder="e.g., Type 2 Diabetes, Hypertension"
                                        class="w-full"
                                        aria-describedby="illness-name-error"
                                    />
                                    <ErrorMessage 
                                        name="name" 
                                        class="text-red-500 text-sm" 
                                        id="illness-name-error"
                                        role="alert"
                                    />
                                </div>
                            </Field>

                            <Field name="severity" v-slot="{ value, handleChange, errors }">
                                <div class="flex flex-col gap-2">
                                    <label for="illness-severity" class="font-semibold text-gray-700">
                                        Severity Level
                                    </label>
                                    <Select 
                                        id="illness-severity" 
                                        :modelValue="value" 
                                        @update:modelValue="handleChange"
                                        :options="severityOptions" 
                                        optionLabel="label" 
                                        optionValue="value"
                                        :class="{ 'p-invalid': errors.length > 0 }" 
                                        placeholder="Select severity level"
                                        class="w-full"
                                        aria-describedby="illness-severity-error"
                                    />
                                    <ErrorMessage 
                                        name="severity" 
                                        class="text-red-500 text-sm" 
                                        id="illness-severity-error"
                                        role="alert"
                                    />
                                </div>
                            </Field>

                            <Field name="date_of_diagnosis" v-slot="{ value, handleChange }">
                                <div class="flex flex-col gap-2">
                                    <label for="illness-diagnosis-date" class="font-semibold text-gray-700">
                                        Date of Diagnosis
                                    </label>
                                    <DatePicker 
                                        id="illness-diagnosis-date" 
                                        :modelValue="value"
                                        @update:modelValue="handleChange" 
                                        dateFormat="yy-mm-dd" 
                                        :maxDate="new Date()"
                                        placeholder="Select diagnosis date"
                                        class="w-full"
                                    />
                                </div>
                            </Field>
                        </div>
                    </div>

                    <!-- Treatment Information Section -->
                    <div class="space-y-4">
                        <h3 class="text-lg font-semibold text-gray-900 border-b border-gray-200 pb-2 flex items-center">
                            <i class="pi pi-tablet mr-2 text-orange-600" aria-hidden="true"></i>
                            Treatment Information
                        </h3>
                        
                        <div class="space-y-4">
                            <Field name="medications" v-slot="{ field }">
                                <div class="flex flex-col gap-2">
                                    <label for="illness-medications" class="font-semibold text-gray-700">
                                        Current Medications
                                    </label>
                                    <Textarea 
                                        id="illness-medications" 
                                        v-bind="field" 
                                        rows="3"
                                        placeholder="List current medications and dosages..."
                                        class="w-full resize-none"
                                        maxlength="1000"
                                    />
                                </div>
                            </Field>

                            <Field name="notes" v-slot="{ field }">
                                <div class="flex flex-col gap-2">
                                    <label for="illness-notes" class="font-semibold text-gray-700">
                                        Additional Notes
                                    </label>
                                    <Textarea 
                                        id="illness-notes" 
                                        v-bind="field" 
                                        rows="3"
                                        placeholder="Any additional information about this condition..."
                                        class="w-full resize-none"
                                        maxlength="1000"
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
                        label="Save Illness" 
                        icon="pi pi-check" 
                        type="submit" 
                        form="illness-form" 
                        autofocus
                        class="bg-orange-600 hover:bg-orange-700 border-orange-600"
                        aria-label="Save chronic illness"
                    />
                </div>
            </template>
        </Dialog>
    </section>
</template>

<style scoped>
.line-clamp-2 {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}
</style>