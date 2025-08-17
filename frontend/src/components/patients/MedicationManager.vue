```vue
<script setup>
import { ref, onMounted, watch } from 'vue';
import axios from 'axios';
import { format, parseISO } from 'date-fns';

// PrimeVue Imports
import Button from 'primevue/button';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Dialog from 'primevue/dialog';
import InputText from 'primevue/inputtext';
import Textarea from 'primevue/textarea';
import DatePicker from 'primevue/datepicker';
import Tag from 'primevue/tag';
import { useConfirm } from "primevue/useconfirm";
import ToggleSwitch from 'primevue/toggleswitch';

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
const medications = ref([]);
const isLoading = ref(true);
const error = ref(null);
const isDialogVisible = ref(false);
const editingMedication = ref(null);
const medicationFormData = ref({});
const startDate = ref(null);
const endDate = ref(null);

// --- Validation Schema ---
const schema = yup.object({
    name: yup.string().required().label('Medication Name'),
    dosage: yup.string().nullable(),
    frequency: yup.string().nullable(),
    reason: yup.string().nullable(),
    start_date: yup.date().nullable().label('Start Date'),
    end_date: yup.date().nullable().when('start_date', {
        is: (startDate) => startDate,
        then: (schema) => schema.min(yup.ref('start_date'), "End date cannot be before start date"),
        otherwise: (schema) => schema
    }).label('End Date'),
    is_active: yup.boolean(),
    prescribed_by: yup.string().nullable(),
    notes: yup.string().nullable(),
});

// Watch for start date changes to update end date minimum
watch(() => medicationFormData.value.start_date, (newStartDate) => {
    startDate.value = newStartDate;
    if (medicationFormData.value.end_date && newStartDate && medicationFormData.value.end_date < newStartDate) {
        medicationFormData.value.end_date = null;
    }
});

// --- API Functions ---
const getMedications = async () => {
    isLoading.value = true;
    error.value = null;
    
    try {
        const response = await axios.get(`/api/patients/${props.patientId}/medications`);
        medications.value = response.data;
    } catch (err) {
        console.error("Failed to fetch medications:", err);
        error.value = "Could not load medication data.";
    } finally {
        isLoading.value = false;
    }
};

const saveMedication = async (values) => {
    try {
        if (editingMedication.value) {
            const response = await axios.put(`/api/medications/${editingMedication.value.id}`, values);
            const index = medications.value.findIndex(m => m.id === editingMedication.value.id);
            if (index !== -1) {
                medications.value[index] = response.data;
            }
        } else {
            const response = await axios.post(`/api/patients/${props.patientId}/medications`, values);
            medications.value.unshift(response.data);
        }
        isDialogVisible.value = false;
    } catch (error) {
        console.error("Failed to save medication:", error.response?.data);
        alert('Failed to save medication.');
    }
};

const deleteMedication = async (medication) => {
    try {
        await axios.delete(`/api/medications/${medication.id}`);
        medications.value = medications.value.filter(m => m.id !== medication.id);
    } catch (error) {
        console.error("Failed to delete medication:", error);
        alert('Failed to delete medication.');
    }
};

// --- Dialog and Form Functions ---
const openAddDialog = () => {
    editingMedication.value = null;
    medicationFormData.value = {
        name: '',
        dosage: '',
        frequency: '',
        reason: '',
        start_date: new Date(),
        end_date: null,
        is_active: true,
        prescribed_by: '',
        notes: ''
    };
    startDate.value = new Date();
    endDate.value = null;
    isDialogVisible.value = true;
};

const openEditDialog = (medicationToEdit) => {
    editingMedication.value = medicationToEdit;
    medicationFormData.value = {
        ...medicationToEdit,
        start_date: medicationToEdit.start_date ? parseISO(medicationToEdit.start_date) : null,
        end_date: medicationToEdit.end_date ? parseISO(medicationToEdit.end_date) : null
    };
    startDate.value = medicationToEdit.start_date ? parseISO(medicationToEdit.start_date) : null;
    endDate.value = medicationToEdit.end_date ? parseISO(medicationToEdit.end_date) : null;
    isDialogVisible.value = true;
};

const confirmDelete = (medication, event) => {
    event.stopPropagation();
    confirm.require({
        message: `Are you sure you want to delete the medication "${medication.name}"?`,
        header: 'Delete Confirmation',
        icon: 'pi pi-exclamation-triangle',
        acceptClass: 'p-button-danger',
        accept: () => deleteMedication(medication),
        reject: () => {}
    });
};

// --- Lifecycle Hook ---
onMounted(() => {
    getMedications();
});
</script>

<template>
    <div class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">        
        <!-- Header Section -->
        <header class="px-6 py-4 border-b border-gray-200 bg-purple-50">
            <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
                <div class="flex items-center gap-3">
                    <div>
                        <h2 class="text-lg font-semibold text-purple-800">
                            Medication History
                        </h2>
                        <p class="text-sm text-purple-600">
                            {{ medications.length }} medication{{ medications.length !== 1 ? 's' : '' }} on record
                        </p>
                    </div>
                </div>
                <Button
                    label="Add Medication"
                    icon="pi pi-plus"
                    size="small"
                    @click="openAddDialog"
                    class="bg-purple-600 hover:bg-purple-700 border-purple-600 whitespace-nowrap"
                    :disabled="isLoading"
                    aria-label="Add new medication"
                />
            </div>
        </header>

        <!-- Loading State -->
        <div v-if="isLoading" class="flex items-center justify-center py-12" role="status" aria-live="polite">
            <div class="flex items-center space-x-3">
                <i class="pi pi-spin pi-spinner text-purple-600 text-xl" aria-hidden="true"></i>
                <span class="text-gray-600">Loading medications...</span>
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
                @click="getMedications"
                class="border-red-600 text-red-600 hover:bg-red-50"
                aria-label="Retry loading medications"
            />
        </div>

        <!-- Data Table -->
        <div v-else class="overflow-x-auto">
            <DataTable 
                :value="medications" 
                class="p-datatable-sm"
                :pt="{
                    table: 'min-w-full',
                    thead: 'bg-gray-50',
                    tbody: 'divide-y divide-gray-200'
                }"
                role="table"
                aria-label="Medications history table"
            >
                <template #empty>
                    <div class="text-center py-12">
                        <i class="pi pi-heart text-4xl text-gray-400 mb-3 block" aria-hidden="true"></i>
                        <p class="text-gray-500 font-medium">No medications recorded</p>
                        <p class="text-sm text-gray-400 mb-4">No medications have been recorded for this patient</p>
                        <Button
                            label="Add First Medication"
                            icon="pi pi-plus"
                            outlined
                            @click="openAddDialog"
                            class="border-purple-600 text-purple-600 hover:bg-purple-50"
                            aria-label="Add first medication"
                        />
                    </div>
                </template>

                <Column 
                    field="name" 
                    header="Medication" 
                    style="min-width: 200px"
                    :pt="{
                        headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                        bodyCell: 'px-6 py-4 text-sm'
                    }"
                >
                    <template #body="{ data }">
                        <div class="flex items-center gap-3">
                            <div>
                                <span class="font-semibold text-gray-900">{{ data.name }}</span>
                                <p v-if="data.reason" class="text-xs text-gray-500 mt-1">{{ data.reason }}</p>
                            </div>
                        </div>
                    </template>
                </Column>

                <Column 
                    field="dosage" 
                    header="Dosage" 
                    style="min-width: 120px"
                    :pt="{
                        headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                        bodyCell: 'px-6 py-4 text-sm text-gray-900'
                    }"
                >
                    <template #body="{ data }">
                        <span class="font-medium text-purple-700">{{ data.dosage || '-' }}</span>
                    </template>
                </Column>

                <Column 
                    field="frequency" 
                    header="Frequency" 
                    style="min-width: 150px"
                    :pt="{
                        headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                        bodyCell: 'px-6 py-4 text-sm text-gray-900'
                    }"
                >
                    <template #body="{ data }">
                        {{ data.frequency || '-' }}
                    </template>
                </Column>

                <Column 
                    header="Duration" 
                    style="min-width: 180px"
                    :pt="{
                        headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                        bodyCell: 'px-6 py-4 text-sm'
                    }"
                >
                    <template #body="{ data }">
                        <div class="space-y-1">
                            <p class="text-gray-900">
                                <span class="text-xs text-gray-500">From:</span>
                                {{ data.start_date ? format(parseISO(data.start_date), 'MMM d, yyyy') : 'Not set' }}
                            </p>
                            <p class="text-gray-900">
                                <span class="text-xs text-gray-500">To:</span>
                                {{ data.end_date ? format(parseISO(data.end_date), 'MMM d, yyyy') : 'Ongoing' }}
                            </p>
                        </div>
                    </template>
                </Column>

                <Column 
                    field="is_active" 
                    header="Status" 
                    style="min-width: 100px"
                    :pt="{
                        headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                        bodyCell: 'px-6 py-4 text-sm'
                    }"
                >
                    <template #body="{ data }">
                        <Tag 
                            :value="data.is_active ? 'Active' : 'Inactive'" 
                            :severity="data.is_active ? 'success' : 'secondary'"
                            class="text-xs"
                        />
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
                                :aria-label="`Edit ${data.name} medication`"
                                v-tooltip.top="'Edit medication'"
                            />
                            <Button 
                                icon="pi pi-trash" 
                                severity="danger" 
                                text 
                                rounded 
                                @click="confirmDelete(data, $event)" 
                                class="hover:bg-red-50"
                                :aria-label="`Delete ${data.name} medication`"
                                v-tooltip.top="'Delete medication'"
                            />
                        </div>
                    </template>
                </Column>
            </DataTable>
        </div>

        <!-- Add/Edit Dialog -->
        <Dialog 
            :header="editingMedication ? 'Edit Medication' : 'Add New Medication'" 
            v-model:visible="isDialogVisible" 
            modal 
            :style="{ width: '90vw', maxWidth: '800px' }"
            class="p-fluid"
            :pt="{
                header: 'bg-purple-50 text-purple-800 px-6 py-4 border-b border-purple-200',
                content: 'p-0'
            }"
            role="dialog"
            :aria-label="editingMedication ? 'Edit medication dialog' : 'Add new medication dialog'"
        >
            <Form 
                @submit="saveMedication" 
                :validation-schema="schema" 
                :initial-values="medicationFormData" 
                id="medication-form"
                class="p-fluid"
            >
                <div class="p-6 space-y-6">
                    <!-- Basic Information Section -->
                    <div class="space-y-4">
                        <h3 class="text-lg font-semibold text-gray-900 border-b border-gray-200 pb-2 flex items-center">
                            <i class="pi pi-info-circle mr-2 text-purple-600" aria-hidden="true"></i>
                            Basic Information
                        </h3>
                        
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <Field name="name" v-slot="{ field, errors }" class="md:col-span-2">
                                <div class="flex flex-col gap-2">
                                    <label for="med-name" class="font-semibold text-gray-700">
                                        Medication Name *
                                    </label>
                                    <InputText 
                                        id="med-name" 
                                        v-bind="field" 
                                        :class="{ 'p-invalid': errors.length > 0 }" 
                                        placeholder="Enter medication name"
                                        class="w-full"
                                        aria-describedby="med-name-error"
                                    />
                                    <ErrorMessage 
                                        name="name" 
                                        class="text-red-500 text-sm" 
                                        id="med-name-error"
                                        role="alert"
                                    />
                                </div>
                            </Field>

                            <Field name="dosage" v-slot="{ field }">
                                <div class="flex flex-col gap-2">
                                    <label for="med-dosage" class="font-semibold text-gray-700">
                                        Dosage
                                    </label>
                                    <InputText 
                                        id="med-dosage" 
                                        v-bind="field" 
                                        placeholder="e.g., 500mg, 2 tablets"
                                        class="w-full"
                                    />
                                </div>
                            </Field>

                            <Field name="frequency" v-slot="{ field }">
                                <div class="flex flex-col gap-2">
                                    <label for="med-frequency" class="font-semibold text-gray-700">
                                        Frequency
                                    </label>
                                    <InputText 
                                        id="med-frequency" 
                                        v-bind="field" 
                                        placeholder="e.g., Once daily, Twice a day"
                                        class="w-full"
                                    />
                                </div>
                            </Field>

                            <Field name="prescribed_by" v-slot="{ field }">
                                <div class="flex flex-col gap-2">
                                    <label for="med-prescriber" class="font-semibold text-gray-700">
                                        Prescribed By
                                    </label>
                                    <InputText 
                                        id="med-prescriber" 
                                        v-bind="field" 
                                        placeholder="e.g., Dr. Smith"
                                        class="w-full"
                                    />
                                </div>
                            </Field>

                            <Field name="is_active" v-slot="{ value, handleChange }">
                                <div class="flex flex-col gap-2">
                                    <label class="font-semibold text-gray-700">Status</label>
                                    <div class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
                                        <ToggleSwitch 
                                            :modelValue="value" 
                                            @update:modelValue="handleChange" 
                                            inputId="med-is-active" 
                                        />
                                        <label for="med-is-active" class="text-sm text-gray-700">
                                            {{ value ? 'Currently Active' : 'Inactive' }}
                                        </label>
                                    </div>
                                </div>
                            </Field>
                        </div>
                    </div>

                    <!-- Duration Section -->
                    <div class="space-y-4">
                        <h3 class="text-lg font-semibold text-gray-900 border-b border-gray-200 pb-2 flex items-center">
                            <i class="pi pi-calendar mr-2 text-purple-600" aria-hidden="true"></i>
                            Duration
                        </h3>
                        
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <Field name="start_date" v-slot="{ value, handleChange, errors }">
                                <div class="flex flex-col gap-2">
                                    <label for="med-start-date" class="font-semibold text-gray-700">
                                        Start Date
                                    </label>
                                    <DatePicker 
                                        id="med-start-date" 
                                        :modelValue="value" 
                                        @update:modelValue="handleChange" 
                                        dateFormat="yy-mm-dd"
                                        :maxDate="new Date()"
                                        placeholder="Select start date"
                                        class="w-full"
                                        :class="{ 'p-invalid': errors.length > 0 }"
                                        aria-describedby="med-start-date-error"
                                    />
                                    <ErrorMessage 
                                        name="start_date" 
                                        class="text-red-500 text-sm" 
                                        id="med-start-date-error"
                                        role="alert"
                                    />
                                </div>
                            </Field>

                            <Field name="end_date" v-slot="{ value, handleChange, errors }">
                                <div class="flex flex-col gap-2">
                                    <label for="med-end-date" class="font-semibold text-gray-700">
                                        End Date <span class="text-sm text-gray-500 font-normal">(Optional)</span>
                                    </label>
                                    <DatePicker 
                                        id="med-end-date" 
                                        :modelValue="value" 
                                        @update:modelValue="handleChange" 
                                        dateFormat="yy-mm-dd"
                                        :minDate="medicationFormData.start_date"
                                        placeholder="Select end date"
                                        class="w-full"
                                        :class="{ 'p-invalid': errors.length > 0 }"
                                        aria-describedby="med-end-date-error"
                                    />
                                    <ErrorMessage 
                                        name="end_date" 
                                        class="text-red-500 text-sm" 
                                        id="med-end-date-error"
                                        role="alert"
                                    />
                                    <small class="text-gray-500 text-xs">
                                        Leave empty if medication is ongoing
                                    </small>
                                </div>
                            </Field>
                        </div>
                    </div>

                    <!-- Additional Information Section -->
                    <div class="space-y-4">
                        <h3 class="text-lg font-semibold text-gray-900 border-b border-gray-200 pb-2 flex items-center">
                            <i class="pi pi-file-edit mr-2 text-purple-600" aria-hidden="true"></i>
                            Additional Information
                        </h3>
                        
                        <div class="space-y-4">
                            <Field name="reason" v-slot="{ field }">
                                <div class="flex flex-col gap-2">
                                    <label for="med-reason" class="font-semibold text-gray-700">
                                        Reason for Medication
                                    </label>
                                    <Textarea 
                                        id="med-reason" 
                                        v-bind="field" 
                                        rows="2" 
                                        placeholder="Enter the medical condition or reason for this medication"
                                        class="w-full"
                                    />
                                </div>
                            </Field>

                            <Field name="notes" v-slot="{ field }">
                                <div class="flex flex-col gap-2">
                                    <label for="med-notes" class="font-semibold text-gray-700">
                                        Additional Notes
                                    </label>
                                    <Textarea 
                                        id="med-notes" 
                                        v-bind="field" 
                                        rows="3" 
                                        placeholder="Any additional notes, side effects, or special instructions"
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
                        label="Save Medication" 
                        icon="pi pi-check" 
                        type="submit" 
                        form="medication-form" 
                        autofocus
                        class="bg-purple-600 hover:bg-purple-700 border-purple-600"
                        aria-label="Save medication"
                    />
                </div>
            </template>
        </Dialog>
    </div>
</template>