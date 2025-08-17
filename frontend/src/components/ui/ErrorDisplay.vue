<script setup>
import Button from 'primevue/button'

defineProps({
    message: {
        type: String,
        default: 'An unknown error occurred.'
    },
    buttonLabel: {
        type: String,
        default: 'Retry'
    }
});
const emit = defineEmits(['retry']);
</script>

<template>
    <div class="p-5 text-center text-red-700 bg-red-100 rounded-lg">
        <i class="pi pi-exclamation-triangle mr-2"></i>
        <span>{{ message }}</span>
        <br/>
        <Button :label=buttonLabel severity="danger" @click="emit('retry')" icon="pi pi-refresh" class="p-button-sm px-5 mt-3" />
    </div>
</template>