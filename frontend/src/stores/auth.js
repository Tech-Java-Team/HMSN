import { defineStore } from "pinia";
import axios from "axios";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    user: null,
    token: localStorage.getItem("auth_token") || null,
    isLoading: false,
  }),
  getters: {
    isAuthenticated: (state) => !!state.token && !!state.user,
    isDoctor: (state) => state.user?.role === "DOCTOR",
    isAdmin: (state) => state.user?.role === "ADMIN",
    isPatient: (state) => state.user?.role === "PATIENT",
  },
  actions: {
    async login(credentials) {
      this.isLoading = true;
      try {
        const response = await axios.post("/api/v1/auth/authenticate", {
          email: credentials.email,
          password: credentials.password,
        });

        const { token, user } = response.data;

        this.token = token;
        this.user = user;

        localStorage.setItem("auth_token", token);

        return response.data;
      } catch (error) {
        console.error("Login failed:", error);
        this.clearAuth();
        throw error;
      } finally {
        this.isLoading = false;
      }
    },

    async register(data) {
      this.isLoading = true;
      try {
        const response = await axios.post("/api/v1/auth/register", {
          email: data.email,
          password: data.password,
          fullName: data.fullName,
          phoneNumber: data.phoneNumber,
          gender: data.gender,
          bloodType: data.bloodType,
          address: data.address,
          dateOfBirth: data.dateOfBirth,
          emergencyContactName: data.emergencyContactName,
          emergencyContactPhone: data.emergencyContactPhone,
        });

        const { token, user } = response.data;

        this.token = token;
        this.user = user;

        localStorage.setItem("auth_token", token);

        return response.data;
      } catch (error) {
        console.error("Registration failed:", error);
        this.clearAuth();
        throw error;
      } finally {
        this.isLoading = false;
      }
    },

    async fetchUser() {
      if (!this.token) return;

      try {
        const response = await axios.get("/api/v1/profile");
        this.user = response.data;
      } catch (error) {
        console.error("Failed to fetch user:", error);
        this.clearAuth();
        throw error;
      }
    },

    async logout() {
      try {
        //await axios.post("/api/v1/auth/logout");
      } catch (error) {
        console.error("Logout request failed:", error);
      } finally {
        this.clearAuth();
      }
    },

    async initializeAuth() {
      if (this.token) {
        this.isLoading = true;
        try {
          await this.fetchUser();
        } catch (error) {
          console.error("Failed to initialize auth:", error);
          this.clearAuth();
        } finally {
          this.isLoading = false;
        }
      }
    },

    clearAuth() {
      this.user = null;
      this.token = null;
      localStorage.removeItem("auth_token");
    },

    hasToken() {
      return !!this.token;
    },

    hasRole(roleName) {
      return this.user?.roles?.some((role) => role.name.toLowerCase() === roleName.toLowerCase());
    },
  },
});
