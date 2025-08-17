import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../stores/auth";

// You will import your page components here later
// import HomePage from "../components/pages/HomePage.vue";
// import PatientsPage from "../components/pages/PatientsPage.vue";
// import DoctorsPage from "../components/pages/DoctorsPage.vue";
// import AppointmentsPage from "../components/pages/AppointmentsPage.vue";
// import DoctorDashboardpage from "../components/pages/DoctorDashboardpage.vue";
// import LoginPage from "../components/pages/LoginPage.vue";
// import AllAppointmentsPage from "../components/pages/AllAppointmentsPage.vue";

const routes = [
    {
        path: "/",
        name: "home",
        component: () => import("@/components/pages/HomePage.vue"),
        // meta: { requiresAuth: true },
    },
    {
        path: "/patients",
        name: "patients",
        component: () => import("@/components/pages/PatientsPage.vue"),
        meta: { requiresAuth: true, requiredRole: "Admin" },
    },
    {
        path: "/patient/:id",
        name: "patients-detail",
        component: () => import("@/components/pages/PatientDetailPage.vue"),
        meta: { requiresAuth: true, requiredRole: "Admin" },
        props: true,
    },
    {
        path: "/doctors",
        name: "doctors",
        component: () => import("@/components/pages/DoctorsPage.vue"),
        meta: { requiresAuth: true, requiredRole: "Admin" },
    },
    {
        path: "/appointments",
        name: "appointments",
        component: () => import("@/components/pages/AppointmentsPage.vue"),
        meta: { requiresAuth: true, requiredRole: "Admin" },
    },
    {
        path: "/all-appointments",
        name: "all-appointments",
        component: () => import("@/components/pages/AllAppointmentsPage.vue"),
        meta: { requiresAuth: true, requiredRole: "Admin" },
    },
    {
        path: "/admin/services",
        name: "admin.services",
        component: () => import("@/components/pages/ServiceManagementPage.vue"),
        meta: { requiresAuth: true, requiredRole: "Admin" },
    },

    // --- Doctor Routes ---
    {
        path: "/doctor/dashboard",
        name: "doctor.dashboard",
        component: () => import("@/components/pages/DoctorDashboardPage.vue"),
        meta: { requiresAuth: true, requiredRole: "Doctor" },
    },

    // --- Patient Routes ---
    {
        path: "/my-dashboard",
        name: "patient.dashboard",
        component: () => import("@/components/pages/PatientDashboardPage.vue"),
        meta: { requiresAuth: true, requiredRole: "Patient" },
    },
    {
        path: "/appointment-history",
        name: "patient.appointmentHistory",
        component: () =>
            import("@/components/pages/PatientAppointmentListHistory.vue"),
        meta: { requiresAuth: true, requiredRole: "Patient" },
    },
    // --- Guest Routes ---
    {
        path: "/login",
        name: "login",
        component: () => import("@/components/pages/LoginPage.vue"),
    },
    {
        path: "/register",
        name: "register",
        component: () => import("@/components/pages/RegisterPage.vue"),
    },
    {
        path: "/our-doctors",
        name: "public.doctors",
        component: () =>
            import("@/components/pages/public/DoctorsListPage.vue"),
    },
    // --- Catch All Route
    {
        path: "/:pathMatch(.*)*",
        redirect: "/login", // Change to NotFoundPage component
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

function redirectToDashboard(userRoles, to, next) {
    if (
        userRoles.some((role) => role.name === "Admin") &&
        to.name !== "admin.dashboard"
    ) {
        return next({ name: "admin.dashboard" });
    }
    if (
        userRoles.some((role) => role.name === "Doctor") &&
        to.name !== "doctor.dashboard"
    ) {
        return next({ name: "doctor.dashboard" });
    }
    if (
        userRoles.some((role) => role.name === "Patient") &&
        to.name !== "patient.dashboard"
    ) {
        return next({ name: "patient.dashboard" });
    }
    return next();
}

router.beforeEach(async (to, from, next) => {
    const authStore = useAuthStore();
    if (authStore.token && !authStore.user) {
        await authStore.fetchUser();
    }

    const isAuthenticated = authStore.isAuthenticated;
    const userRoles = authStore.user?.roles || [];

    // Not Authenticated User
    if (to.meta.requiresAuth && !isAuthenticated) {
        return next({ name: "login" });
    }

    // Prevent Authenticated User from seeing login or register
    if (isAuthenticated && (to.name === "login" || to.name === "register")) {
        return redirectToDashboard(userRoles, to, next);
    }

    const requiredRole = to.meta.requiredRole;
    if (
        requiredRole &&
        isAuthenticated &&
        !userRoles.some((role) => role.name === requiredRole)
    ) {
        return redirectToDashboard(userRoles, to, next);
    }

    next();
});

export default router;
