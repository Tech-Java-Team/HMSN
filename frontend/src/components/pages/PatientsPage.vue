<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { format } from 'date-fns';

// --- PrimeVue Imports ---
import Button from 'primevue/button';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Dialog from 'primevue/dialog';
import InputText from 'primevue/inputtext';
import Select from 'primevue/select';
import DatePicker from 'primevue/datepicker';
import Textarea from 'primevue/textarea';
import ConfirmDialog from 'primevue/confirmdialog';
import { useConfirm } from "primevue/useconfirm";

// --- Component State ---
const confirm = useConfirm();
const patients = ref([]);
const isPatientDialogVisible = ref(false);
const editingPatient = ref(null);
const newPatient = ref({});

const genderOptions = ref([
  { label: 'Male', value: 'Male' },
  { label: 'Female', value: 'Female' },
]);

// --- API Functions ---
const getPatients = async () => {
  try {
    const response = await axios.get('/api/patients');
    patients.value = response.data;
  } catch (error) {
    console.error("Error fetching patients:", error);
  }
};

const savePatient = async () => {
  try {
    if (editingPatient.value) {
      const response = await axios.put(`/api/patients/${editingPatient.value.id}`, newPatient.value);
      const index = patients.value.findIndex(p => p.id === editingPatient.value.id);
      if (index !== -1) {
        patients.value[index] = response.data;
      }
    } else {
      const response = await axios.post('/api/patients', newPatient.value);
      patients.value.unshift(response.data);
    }
    // CORRECTED: Must use .value to update a ref
    isPatientDialogVisible.value = false;
  } catch (error) {
    console.error("Error saving patient:", error.response?.data);
    alert('Failed to save patient. Check console for errors.');
  }
};

const deletePatient = async (patient) => {
  try {
    await axios.delete(`/api/patients/${patient.id}`);
    patients.value = patients.value.filter(p => p.id !== patient.id);
  } catch (error) {
    console.error("Error deleting patient:", error);
    alert('Failed to delete patient.');
  }
};


// --- Dialog and Form Functions ---
const openAddDialog = () => {
  editingPatient.value = null;
  newPatient.value = {
    full_name: '',
    phone_number: '',
    email: '',
    gender: null,
    date_of_birth: '',
    address: '',
    emergency_contact_name: '',
    emergency_contact_phone: '',
  };
  isPatientDialogVisible.value = true;
};

const openEditDialog = (patientToEdit) => {
  editingPatient.value = patientToEdit;
  // CORRECTED: Must use .value to update a ref
  newPatient.value = { ...patientToEdit };
  isPatientDialogVisible.value = true;
};

const confirmDeletePatient = (patient) => {
  confirm.require({
    message: 'Are you sure you want to delete this patient?',
    header: 'Delete Confirmation',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => {
      deletePatient(patient);
    },
    rejectClass: 'p-button-secondary',
    reject: () => { }
  });
};

// --- Lifecycle Hooks ---
onMounted(() => {
  getPatients();
});
</script>

<template>
  <div class="p-6 max-w-full mx-auto">
    <ConfirmDialog />

    <!-- Header Section -->
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
      <h2 class="text-3xl font-bold text-gray-900">Patient Management</h2>
      <Button label="Add New Patient" icon="pi pi-plus" @click="openAddDialog"
        class="bg-teal-600 hover:bg-teal-700 border-teal-600 whitespace-nowrap" />
    </div>

    <!-- Data Table -->
    <div class="bg-surface-50 rounded-lg shadow-sm border border-gray-200 overflow-hidden">
      <DataTable :value="patients" responsiveLayout="scroll" class="p-datatable-sm" :pt="{
        table: 'min-w-full',
        thead: 'bg-surface-100',
        tbody: 'divide-y divide-gray-200'
      }">
        <template #empty>
          <div class="text-center py-8">
            <p class="text-gray-500">No patients found</p>
          </div>
        </template>

        <Column field="id" header="ID" :sortable="true" style="min-width: 80px" :pt="{
          headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
          bodyCell: 'px-6 py-4 text-sm text-gray-900'
        }" />

        <Column field="full_name" header="Full Name" :sortable="true" style="min-width: 200px" :pt="{
          headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
          bodyCell: 'px-6 py-4 text-sm font-medium text-gray-900'
        }">
          <template #body="slotProps">
            <router-link :to="`/patient/${slotProps.data.id}`"
            class="text-teal-600 hover:text-teal-800 hover:underline font-semibold">
              {{ slotProps.data.full_name }}
            </router-link>
          </template>
        </Column>

        <Column field="phone_number" header="Phone" style="min-width: 150px" :pt="{
          headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
          bodyCell: 'px-6 py-4 text-sm text-gray-900'
        }" />

        <Column field="gender" header="Gender" style="min-width: 100px" :pt="{
          headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
          bodyCell: 'px-6 py-4 text-sm text-gray-900'
        }">
          <template #body="slotProps">
            <span class="capitalize">{{ slotProps.data.gender }}</span>
          </template>
        </Column>

        <Column header="Date of Birth" style="min-width: 130px" :pt="{
          headerCell: 'px-6 py-4 text-left text-sm font-semibold text-gray-900 bg-gray-50',
          bodyCell: 'px-6 py-4 text-sm text-gray-900'
        }">
          <template #body="slotProps">
            {{ format(new Date(slotProps.data.date_of_birth), 'yyyy-MM-dd') }}
          </template>
        </Column>

        <Column header="Actions" style="min-width: 120px" :pt="{
          headerCell: 'px-6 py-4 text-center text-sm font-semibold text-gray-900 bg-gray-50',
          bodyCell: 'px-6 py-4 text-center'
        }">
          <template #body="slotProps">
            <div class="flex gap-2 justify-center">
              <Button icon="pi pi-pencil" severity="success" text rounded @click="openEditDialog(slotProps.data)"
                class="hover:bg-green-50" />
              <Button icon="pi pi-trash" severity="danger" text rounded @click="confirmDeletePatient(slotProps.data)"
                class="hover:bg-red-50" />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Dialog -->
    <Dialog :header="editingPatient ? 'Edit Patient' : 'Add a New Patient'" v-model:visible="isPatientDialogVisible"
      modal :style="{ width: '90vw', maxWidth: '800px' }" class="p-fluid overflow-hidden" :pt="{
        header: 'bg-teal-50 text-teal-800 px-6 py-4 border-b border-teal-200',
        content: 'p-0'
      }">
      <div class="p-6">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="flex flex-col gap-2">
            <label for="fullName" class="font-semibold text-gray-700">Full Name</label>
            <InputText id="fullName" v-model="newPatient.full_name" placeholder="Enter full name" class="w-full" />
          </div>

          <div class="flex flex-col gap-2">
            <label for="phone" class="font-semibold text-gray-700">Phone Number</label>
            <InputText id="phone" v-model="newPatient.phone_number" placeholder="Enter phone number" class="w-full" />
          </div>

          <div class="flex flex-col gap-2">
            <label for="email" class="font-semibold text-gray-700">Email</label>
            <InputText id="email" v-model="newPatient.email" type="email" placeholder="Enter email address"
              class="w-full" />
          </div>

          <div class="flex flex-col gap-2">
            <label for="gender" class="font-semibold text-gray-700">Gender</label>
            <Select id="gender" v-model="newPatient.gender" :options="genderOptions" optionLabel="label"
              optionValue="value" placeholder="Select a Gender" class="w-full" />
          </div>

          <div class="flex flex-col gap-2">
            <label for="dob" class="font-semibold text-gray-700">Date of Birth</label>
            <DatePicker id="dob" v-model="newPatient.date_of_birth" dateFormat="yy-mm-dd"
              placeholder="Select date of birth" class="w-full" />
          </div>

          <div class="flex flex-col gap-2 md:col-span-2">
            <label for="address" class="font-semibold text-gray-700">Address</label>
            <Textarea id="address" v-model="newPatient.address" rows="3" maxlength="500"
              placeholder="Enter full address" class="w-full resize-none" :style="{ maxHeight: '120px' }" />
          </div>

          <div class="flex flex-col gap-2">
            <label for="emergencyName" class="font-semibold text-gray-700">Emergency Contact Name</label>
            <InputText id="emergencyName" v-model="newPatient.emergency_contact_name"
              placeholder="Enter emergency contact name" class="w-full" />
          </div>

          <div class="flex flex-col gap-2">
            <label for="emergencyPhone" class="font-semibold text-gray-700">Emergency Contact Phone</label>
            <InputText id="emergencyPhone" v-model="newPatient.emergency_contact_phone"
              placeholder="Enter emergency contact phone" class="w-full" />
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end gap-3 p-6 bg-gray-50 border-t border-gray-200">
          <Button label="Cancel" icon="pi pi-times" @click="isPatientDialogVisible = false" severity="secondary"
            outlined />
          <Button label="Save" icon="pi pi-check" @click="savePatient" autofocus
            class="bg-teal-600 hover:bg-teal-700 border-teal-600" />
        </div>
      </template>
    </Dialog>
  </div>
</template>