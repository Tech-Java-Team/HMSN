<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

// PrimeVue Imports
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import Password from 'primevue/password';
import Select from 'primevue/select';
import DatePicker from 'primevue/datepicker';
import Textarea from 'primevue/textarea';

// Validation Imports
import { Form, Field, ErrorMessage } from 'vee-validate';
import * as yup from 'yup';

const router = useRouter();
const authStore = useAuthStore();
const error = ref(null);
const isLoading = ref(false);

const genderOptions = ref([
    { label: 'Male', value: 'MALE' },
    { label: 'Female', value: 'FEMALE' },
    { label: 'Other', value: 'OTHER' },
]);

const bloodTypeOptions = ref([
    { label: 'A+', value: 'A_POSITIVE' },
    { label: 'A-', value: 'A_NEGATIVE' },
    { label: 'B+', value: 'B_POSITIVE' },
    { label: 'B-', value: 'B_NEGATIVE' },
    { label: 'AB+', value: 'AB_POSITIVE' },
    { label: 'AB-', value: 'AB_NEGATIVE' },
    { label: 'O+', value: 'O_POSITIVE' },
    { label: 'O-', value: 'O_NEGATIVE' },
]);

const schema = yup.object({
    fullName: yup.string().required().min(2).max(255).label('Full Name'),
    email: yup.string().required().email().max(255).label('Email Address'),
    password: yup.string().required().min(8).label('Password'),
    passwordConfirmation: yup.string()
        .oneOf([yup.ref('password')], 'Passwords must match')
        .required()
        .label('Password confirmation'),
    phoneNumber: yup.string().required().min(8).max(20).label('Phone Number'),
    gender: yup.string().required().oneOf(['MALE', 'FEMALE', 'OTHER']).label('Gender'),
    dateOfBirth: yup.date()
        .required()
        .max(new Date(Date.now() - 18 * 365 * 24 * 60 * 60 * 1000), 'You must be at least 18 years old')
        .label('Date of Birth'),
    address: yup.string().max(500).label('Address'),
    emergencyContactName: yup.string().max(255).label('Emergency Contact Name'),
    emergencyContactPhone: yup.string().min(8).max(20).label('Emergency Contact Phone'),

    notes: yup.string().label('Medical Notes'),
    bloodType: yup.string().oneOf([
        'A_POSITIVE',
        'A_NEGATIVE',
        'B_POSITIVE',
        'B_NEGATIVE',
        'AB_POSITIVE',
        'AB_NEGATIVE',
        'O_POSITIVE',
        'O_NEGATIVE'
    ]).label('Blood Type'),
});

const handleRegister = async (values) => {
    isLoading.value = true;
    error.value = null;
    try {
        // Format date properly before sending
        const formattedValues = {
            ...values,
            dateOfBirth: values.dateOfBirth ?
                new Date(values.dateOfBirth).toISOString().split('T')[0] :
                null
        };

        await authStore.register(formattedValues);
        router.push('/');
    } catch (err) {
        if (err.response && err.response.data.errors) {
            error.value = Object.values(err.response.data.errors).flat().join(' ');
        } else if (err.response && err.response.data.message) {
            error.value = err.response.data.message;
        } else {
            error.value = 'An unexpected error occurred. Please try again.';
        }
        console.error('Registration failed:', err);
    } finally {
        isLoading.value = false;
    }
};
</script>

<template>
    <div class="min-h-screen px-4 py-20 md:px-12 lg:px-20 flex items-center justify-center bg-cover bg-center">

        <div
            class="px-8 md:px-12 lg:px-16 py-12 flex flex-col items-center gap-8 w-full max-w-4xl backdrop-blur-xl bg-black/30 border border-white/20 rounded-2xl">

            <div class="flex flex-col items-center gap-4 w-full text-center">
                <i class="pi pi-user-plus text-teal-300 text-5xl"></i>
                <h1 class="text-3xl font-medium text-white leading-tight">Create Your Account</h1>
                <div>
                    <span class="text-white/80">Already have an account? </span>
                    <router-link to="/login" class="text-teal-300 hover:text-teal-200 font-semibold underline">Sign
                        in</router-link>
                </div>
            </div>

            <Form @submit="handleRegister" :validation-schema="schema" class="w-full">
                <div class="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-4">

                    <!-- Updated field name from 'name' to 'fullName' -->
                    <Field name="fullName" v-slot="{ field, errors }">
                        <div class="flex flex-col gap-2">
                            <label for="fullName" class="text-white/80">Full Name *</label>
                            <InputText id="fullName" v-bind="field" placeholder="Enter your full name"
                                :class="{ 'p-invalid': errors.length > 0 }" />
                            <ErrorMessage name="fullName" class="text-red-300 text-sm" />
                        </div>
                    </Field>

                    <Field name="email" v-slot="{ field, errors }">
                        <div class="flex flex-col gap-2">
                            <label for="email" class="text-white/80">Email Address *</label>
                            <InputText id="email" v-bind="field" type="email" placeholder="you@example.com"
                                :class="{ 'p-invalid': errors.length > 0 }" />
                            <ErrorMessage name="email" class="text-red-300 text-sm" />
                        </div>
                    </Field>

                    <Field name="password" v-slot="{ field, errors }">
                        <div class="flex flex-col gap-2">
                            <label for="password" class="text-white/80">Password *</label>
                            <Password inputId="password" v-bind="field" :toggleMask="true" :feedback="false"
                                placeholder="Min. 8 characters" :class="{ 'p-invalid': errors.length > 0 }" />
                            <ErrorMessage name="password" class="text-red-300 text-sm" />
                        </div>
                    </Field>

                    <Field name="passwordConfirmation" v-slot="{ field, errors }">
                        <div class="flex flex-col gap-2">
                            <label for="passwordConfirmation" class="text-white/80">Confirm Password *</label>
                            <Password inputId="passwordConfirmation" v-bind="field" :toggleMask="true"
                                :feedback="false" placeholder="Repeat your password"
                                :class="{ 'p-invalid': errors.length > 0 }" />
                            <ErrorMessage name="passwordConfirmation" class="text-red-300 text-sm" />
                        </div>
                    </Field>

                    <Field name="phoneNumber" v-slot="{ field, errors }">
                        <div class="flex flex-col gap-2">
                            <label for="phoneNumber" class="text-white/80">Phone Number *</label>
                            <InputText id="phoneNumber" v-bind="field" placeholder="Enter your phone number"
                                :class="{ 'p-invalid': errors.length > 0 }" />
                            <ErrorMessage name="phoneNumber" class="text-red-300 text-sm" />
                        </div>
                    </Field>

                    <Field name="gender" v-slot="{ value, handleChange, errors }">
                        <div class="flex flex-col gap-2">
                            <label for="gender" class="text-white/80">Gender *</label>
                            <Select id="gender" :modelValue="value" @update:modelValue="handleChange"
                                :options="genderOptions" optionLabel="label" optionValue="value"
                                :class="{ 'p-invalid': errors.length > 0 }" placeholder="Select your gender" />
                            <ErrorMessage name="gender" class="text-red-300 text-sm" />
                        </div>
                    </Field>

                    <Field name="dateOfBirth" v-slot="{ value, handleChange, errors }" class="md:col-span-2">
                        <div class="flex flex-col gap-2">
                            <label for="dateOfBirth" class="text-white/80">Date of Birth *</label>
                            <DatePicker id="dateOfBirth" :modelValue="value" @update:modelValue="handleChange"
                                dateFormat="yy-mm-dd" :maxDate="new Date(Date.now() - 18 * 365 * 24 * 60 * 60 * 1000)"
                                :class="{ 'p-invalid': errors.length > 0 }"
                                placeholder="Select your date of birth (Must be 18+)" />
                            <ErrorMessage name="dateOfBirth" class="text-red-300 text-sm" />
                        </div>
                    </Field>

                    <Field name="address" v-slot="{ field, errors }" class="md:col-span-2">
                        <div class="flex flex-col gap-2">
                            <label for="address" class="text-white/80">Address (Optional)</label>
                            <Textarea id="address" v-bind="field" placeholder="Enter your address"
                                :class="{ 'p-invalid': errors.length > 0 }" rows="2" />
                            <ErrorMessage name="address" class="text-red-300 text-sm" />
                        </div>
                    </Field>

                    <Field name="emergencyContactName" v-slot="{ field, errors }">
                        <div class="flex flex-col gap-2">
                            <label for="emergencyContactName" class="text-white/80">Emergency Contact Name
                                (Optional)</label>
                            <InputText id="emergencyContactName" v-bind="field"
                                placeholder="Enter emergency contact name"
                                :class="{ 'p-invalid': errors.length > 0 }" />
                            <ErrorMessage name="emergencyContactName" class="text-red-300 text-sm" />
                        </div>
                    </Field>

                    <Field name="emergencyContactPhone" v-slot="{ field, errors }">
                        <div class="flex flex-col gap-2">
                            <label for="emergencyContactPhone" class="text-white/80">Emergency Contact Phone
                                (Optional)</label>
                            <InputText id="emergencyContactPhone" v-bind="field"
                                placeholder="Enter emergency contact phone"
                                :class="{ 'p-invalid': errors.length > 0 }" />
                            <ErrorMessage name="emergencyContactPhone" class="text-red-300 text-sm" />
                        </div>
                    </Field>

                    <div class="md:col-span-2 border-t border-white/20 pt-6 mt-4">
                        <h3 class="text-white/90 text-lg font-medium mb-4">Medical Information (Optional)</h3>
                    </div>

                    <Field name="bloodType" v-slot="{ value, handleChange, errors }">
                        <div class="flex flex-col gap-2">
                            <label for="bloodType" class="text-white/80">Blood Type (Optional)</label>
                            <Select id="bloodType" :modelValue="value" @update:modelValue="handleChange"
                                :options="bloodTypeOptions" optionLabel="label" optionValue="value"
                                :class="{ 'p-invalid': errors.length > 0 }" placeholder="Select your blood type" />
                            <ErrorMessage name="bloodType" class="text-red-300 text-sm" />
                        </div>
                    </Field>

                    <Field name="notes" v-slot="{ field, errors }" class="md:col-span-2">
                        <div class="flex flex-col gap-2">
                            <label for="notes" class="text-white/80">Medical Notes (Optional)</label>
                            <Textarea id="notes" v-bind="field"
                                placeholder="Any medical conditions, allergies, or other important notes"
                                :class="{ 'p-invalid': errors.length > 0 }" rows="3" />
                            <ErrorMessage name="notes" class="text-red-300 text-sm" />
                        </div>
                    </Field>
                </div>

                <div v-if="error"
                    class="text-red-300 bg-red-900/50 border border-red-500/50 rounded-lg p-3 w-full text-center mt-6">
                    {{ error }}
                </div>

                <div class="text-white/60 text-sm mt-6 text-center">
                    Fields marked with * are required
                </div>

                <Button type="submit" label="Create Account" :loading="isLoading"
                    class="!w-full !rounded-lg !bg-teal-600 !border !border-teal-500 !text-white hover:!bg-teal-500 !py-3 !font-bold mt-6" />
            </Form>
        </div>
    </div>
</template>

<style>
.p-password-input {
    width: 100%;
}

.p-textarea {
    width: 100%;
    max-height: 300px;
    min-height: 45px;
}
</style>