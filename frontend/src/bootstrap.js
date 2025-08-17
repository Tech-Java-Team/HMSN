import axios from "axios";

axios.defaults.baseURL = "http://127.0.0.1:8081";
window.axios = axios;

window.axios.defaults.headers.common["X-Requested-With"] = "XMLHttpRequest";
window.axios.defaults.headers.common["Content-Type"] = "application/json";

axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("auth_token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

axios.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("auth_token");
      // Optional: Redirect to login or emit event
      // window.location.href = '/login';
      // or emit a global event that components can listen to
      window.dispatchEvent(new CustomEvent('auth-expired'));
    }
    return Promise.reject(error);
  }
);