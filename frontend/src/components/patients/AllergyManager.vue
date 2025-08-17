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
const allergies = ref([]);
const isLoading = ref(true);
const error = ref(null); // Changed from isError to error
const isDialogVisible = ref(false);
const editingAllergy = ref(null);
const allergyFormData = ref({});

const severityOptions = ref([
    { label: 'Mild', value: 'Mild' },
    { label: 'Moderate', value: 'Moderate' },
    { label: 'Severe', value: 'Severe' }
]);

// --- Validation Schema ---
const schema = yup.object({
    name: yup.string().required().label('Allergy Name'),
    severity: yup.string().required().label('Severity'),
    reaction: yup.string().nullable(),
    date_of_diagnosis: yup.date().nullable().label('Diagnosis Date'),
    notes: yup.string().nullable(),
});

// --- Helper Functions ---
const getSeverityTag = (severity) => {
    switch (severity) {
        case 'Severe': return 'danger';
        case 'Moderate': return 'warning';
        case 'Mild':
        default:
            return 'info';
    }
};

// --- API Functions ---
const getAllergies = async () => {
    isLoading.value = true;
    error.value = null;
    
    try {
        const response = await axios.get(`/api/patients/${props.patientId}/allergies`);
        allergies.value = response.data;
    } catch (err) {
        console.error("Failed to fetch allergies:", err);
        error.value = "Failed to load allergies. Please try again.";
    } finally {
        isLoading.value = false;
    }
};

const saveAllergy = async (values) => {
    try {
        if (editingAllergy.value) {
            const response = await axios.put(`/api/allergies/${editingAllergy.value.id}`, values);
            const index = allergies.value.findIndex(a => a.id === editingAllergy.value.id);
            if (index !== -1) {
                allergies.value[index] = response.data;
            }
        } else {
            const response = await axios.post(`/api/patients/${props.patientId}/allergies`, values);
            allergies.value.unshift(response.data);
        }
        isDialogVisible.value = false;
    } catch (error) {
        console.error("Failed to save allergy:", error.response?.data);
        alert('Failed to save allergy.');
    }
};

const deleteAllergy = async (allergy) => {
    try {
        await axios.delete(`/api/allergies/${allergy.id}`);
        allergies.value = allergies.value.filter(a => a.id !== allergy.id);
    } catch (error) {
        console.error("Failed to delete allergy:", error);
        alert('Failed to delete allergy.');
    }
};

// --- Dialog and Form Functions ---
const openAddDialog = () => {
    editingAllergy.value = null;
    allergyFormData.value = {
        name: '',
        severity: 'Mild',
        reaction: '',
        date_of_diagnosis: null,
        notes: ''
    };
    isDialogVisible.value = true;
};

const openEditDialog = (allergyToEdit) => {
    editingAllergy.value = allergyToEdit;
    allergyFormData.value = {
        ...allergyToEdit,
        date_of_diagnosis: allergyToEdit.date_of_diagnosis ? parseISO(allergyToEdit.date_of_diagnosis) : null
    };
    isDialogVisible.value = true;
};

const confirmDelete = (allergy) => {
    confirm.require({
        message: `Are you sure you want to delete the allergy "${allergy.name}"?`,
        header: 'Delete Confirmation',
        icon: 'pi pi-exclamation-triangle',
        acceptClass: 'p-button-danger',
        accept: () => deleteAllergy(allergy),
    });
};

// --- Lifecycle Hook ---
onMounted(() => {
    getAllergies();
});
</script>

<template>
    <div class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
 
        
        <!-- Header Section -->
        <div class="px-6 py-4 border-b border-gray-200 bg-red-50">
            <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
                <div class="flex items-center gap-3">
                    <div>
                        <h4 class="text-lg font-semibold text-red-800">Recorded Allergies</h4>
                        <p class="text-sm text-red-600">{{ allergies.length }} allergy(ies) on record</p>
                    </div>
                </div>
                <Button 
                    label="Add Allergy" 
                    icon="pi pi-plus" 
                    size="small" 
                    @click="openAddDialog"
                    class="bg-red-600 hover:bg-red-700 border-red-600 whitespace-nowrap"
                    :disabled="isLoading"
                />
            </div>
        </div>

        <!-- Loading State -->
        <div v-if="isLoading" class="flex items-center justify-center py-12">
            <div class="flex items-center space-x-3">
                <i class="pi pi-spin pi-spinner text-red-600 text-xl"></i>
                <span class="text-gray-600">Loading allergies...</span>
            </div>
        </div>

        <!-- Error State -->
        <div v-else-if="error" class="text-center py-12">
            <i class="pi pi-exclamation-circle text-4xl text-red-500 mb-3 block"></i>
            <p class="text-red-600 font-medium mb-2">{{ error }}</p>
            <Button 
                label="Try Again" 
                icon="pi pi-refresh" 
                outlined 
                @click="getAllergies"
                class="border-red-600 text-red-600 hover:bg-red-50"
            />
        </div>

        <!-- Data Table - Only show when not loading and no error -->
        <div v-else class="overflow-x-auto">
            <DataTable 
                :value="allergies" 
                class="p-datatable-sm"
                :pt="{
                    table: 'min-w-full',
                    thead: 'bg-gray-50',
                    tbody: 'divide-y divide-gray-200'
                }"
            >
                <template #empty>
                    <div class="text-center py-12">
                        <i class="pi pi-check-circle text-4xl text-green-500 mb-3 block"></i>
                        <p class="text-gray-500 font-medium">No allergies recorded</p>
                        <p class="text-sm text-gray-400 mb-4">This patient has no known allergic reactions</p>
                        <Button 
                            label="Add First Allergy" 
                            icon="pi pi-plus" 
                            outlined 
                            @click="openAddDialog"
                            class="border-red-600 text-red-600 hover:bg-red-50"
                        />
                    </div>
                </template>

                <Column 
                    field="name" 
                    header="Allergy Name" 
                    style="min-width: 200px"
                    :pt="{
                        headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                        bodyCell: 'px-6 py-4 text-sm'
                    }"
                >
                    <template #body="{ data }">
                        <div class="flex items-center gap-3">
                            <div class="w-8 h-8 bg-red-100 rounded-full flex items-center justify-center">
                                <i class="pi pi-exclamation text-red-600 text-xs"></i>
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
                            :value="data.severity" 
                            :severity="getSeverityTag(data.severity)"
                            class="font-medium"
                        />
                    </template>
                </Column>

                <Column 
                    field="reaction" 
                    header="Common Reaction" 
                    style="min-width: 200px"
                    :pt="{
                        headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
                        bodyCell: 'px-6 py-4 text-sm text-gray-900'
                    }"
                >
                    <template #body="{ data }">
                        <span class="line-clamp-2">{{ data.reaction || 'Not specified' }}</span>
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
                                v-tooltip.top="'Edit allergy'"
                                class="hover:bg-blue-50"
                            />
                            <Button 
                                icon="pi pi-trash" 
                                severity="danger" 
                                text 
                                rounded 
                                @click="confirmDelete(data)" 
                                v-tooltip.top="'Delete allergy'"
                                class="hover:bg-red-50"
                            />
                        </div>
                    </template>
                </Column>
            </DataTable>
        </div>

        <!-- Add/Edit Allergy Dialog -->
        <Dialog 
            :header="editingAllergy ? 'Edit Allergy' : 'Add New Allergy'" 
            v-model:visible="isDialogVisible" 
            modal 
            :style="{ width: '90vw', maxWidth: '600px' }"
            class="p-fluid"
            :pt="{
                header: 'bg-red-50 text-red-800 px-6 py-4 border-b border-red-200',
                content: 'p-0'
            }"
        >
            <Form 
                @submit="saveAllergy" 
                :validation-schema="schema" 
                :initial-values="allergyFormData" 
                id="allergy-form" 
                class="p-fluid"
            >
                <div class="p-6 space-y-6">
                    <!-- Basic Information Section -->
                    <div class="space-y-4">
                        <h3 class="text-lg font-semibold text-gray-900 border-b border-gray-200 pb-2 flex items-center">
                            <i class="pi pi-info-circle mr-2 text-red-600"></i>
                            Allergy Information
                        </h3>
                        
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <Field name="name" v-slot="{ field, errors }">
                                <div class="flex flex-col gap-2">
                                    <label for="allergy-name" class="font-semibold text-gray-700">Allergy Name *</label>
                                    <InputText 
                                        id="allergy-name" 
                                        v-bind="field" 
                                        :class="{ 'p-invalid': errors.length > 0 }" 
                                        placeholder="e.g., Penicillin, Peanuts"
                                        class="w-full"
                                    />
                                    <ErrorMessage name="name" class="text-red-500 text-sm" />
                                </div>
                            </Field>

                            <Field name="severity" v-slot="{ value, handleChange, errors }">
                                <div class="flex flex-col gap-2">
                                    <label for="allergy-severity" class="font-semibold text-gray-700">Severity *</label>
                                    <Select 
                                        id="allergy-severity"
                                        :modelValue="value"
                                        @update:modelValue="handleChange"
                                        :options="severityOptions"
                                        optionLabel="label"
                                        optionValue="value"
                                        :class="{ 'p-invalid': errors.length > 0 }"
                                        placeholder="Select severity level"
                                        class="w-full"
                                    />
                                    <ErrorMessage name="severity" class="text-red-500 text-sm" />
                                </div>
                            </Field>

                            <Field name="date_of_diagnosis" v-slot="{ value, handleChange, errors }">
                                <div class="flex flex-col gap-2">
                                    <label for="allergy-diagnosis-date" class="font-semibold text-gray-700">Date of Diagnosis</label>
                                    <DatePicker 
                                        id="allergy-diagnosis-date"
                                        :modelValue="value"
                                        @update:modelValue="handleChange"
                                        dateFormat="yy-mm-dd"
                                        :maxDate="new Date()"
                                        :class="{ 'p-invalid': errors.length > 0 }"
                                        placeholder="Select diagnosis date"
                                        class="w-full"
                                    />
                                    <ErrorMessage name="date_of_diagnosis" class="text-red-500 text-sm" />
                                </div>
                            </Field>
                        </div>
                    </div>

                    <!-- Reaction Details Section -->
                    <div class="space-y-4">
                        <h3 class="text-lg font-semibold text-gray-900 border-b border-gray-200 pb-2 flex items-center">
                            <i class="pi pi-exclamation-triangle mr-2 text-red-600"></i>
                            Reaction Details
                        </h3>
                        
                        <div class="space-y-4">
                            <Field name="reaction" v-slot="{ field }">
                                <div class="flex flex-col gap-2">
                                    <label for="allergy-reaction" class="font-semibold text-gray-700">Common Reaction</label>
                                    <Textarea 
                                        id="allergy-reaction" 
                                        v-bind="field" 
                                        rows="3" 
                                        placeholder="Describe the typical reaction (e.g., hives, difficulty breathing)"
                                        class="w-full resize-none"
                                        maxlength="500"
                                    />
                                </div>
                            </Field>

                            <Field name="notes" v-slot="{ field }">
                                <div class="flex flex-col gap-2">
                                    <label for="allergy-notes" class="font-semibold text-gray-700">Additional Notes</label>
                                    <Textarea 
                                        id="allergy-notes" 
                                        v-bind="field" 
                                        rows="3" 
                                        placeholder="Any additional information about this allergy"
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
                    />
                    <Button 
                        label="Save Allergy" 
                        icon="pi pi-check" 
                        type="submit" 
                        form="allergy-form" 
                        autofocus
                        class="bg-red-600 hover:bg-red-700 border-red-600"
                    />
                </div>
            </template>
        </Dialog>
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