import './assets/main.css';
import "./bootstrap";

import { createApp } from "vue";
import router from "./router";
import { createPinia } from "pinia";
import App from "./App.vue";

// PrimeVue Imports
import PrimeVue from "primevue/config";
import "primeicons/primeicons.css";
import Aura from "@primeuix/themes/aura";
import ConfirmationService from "primevue/confirmationservice";
import ToastService from "primevue/toastservice";
import Tooltip from "primevue/tooltip";

const app = createApp(App);
const pinia = createPinia()

app.config.errorHandler = (error, instance, info) => {
    console.error("Global error: ", error, "\nHappened at: ", instance, "\nDetails: ", info);
    return false;
};

window.addEventListener("unhandledrejection", (event) => {
    console.error("Unhandled promise rejection: ", event.reason);
    event.preventDefault();
});

app.use(ConfirmationService);
app.use(ToastService);
app.directive("tooltip", Tooltip);
app.use(pinia);
app.use(router);

// complete, production-ready configuration
app.use(PrimeVue, {
    ripple: true,
    theme: {
        preset: Aura,
        options: {
            darkModeSelector: ".dark",
            cssLayer: {
                name: "primevue",
                order: "base, primevue, utilities",
            },
        },
    },
});
app.mount("#app");
