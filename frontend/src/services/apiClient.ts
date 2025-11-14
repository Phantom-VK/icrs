import axios from "axios";

// ✅ Centralized backend base URL
// No "/api" prefix since your Spring Boot routes are plain like "/auth/*", "/users/*", etc.
const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

// ✅ Create the Axios instance
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// ✅ Automatically attach JWT token if present
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ✅ Global error interceptor
// Handles expired tokens and backend 401s gracefully
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    if (status === 401) {
      console.warn("🔒 Unauthorized — clearing expired token");
      localStorage.removeItem("token");
      localStorage.removeItem("tokenExpiry");

      // Optional: you can redirect to login if using React Router
      // window.location.href = "/login";
    }

    return Promise.reject(error);
  }
);

export default apiClient;
