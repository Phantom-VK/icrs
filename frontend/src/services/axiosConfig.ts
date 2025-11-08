import axios from "axios";

// Base configuration for all requests
const api = axios.create({
  baseURL: "http://localhost:8080", // backend server URL
  headers: {
    "Content-Type": "application/json",
  },
});

// ✅ Automatically attach JWT token if it exists
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ✅ Handle 401/403 globally (optional improvement)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      console.warn("Unauthorized or Forbidden — redirecting to login...");
      localStorage.removeItem("token");
      window.location.href = "/auth/login";
    }
    return Promise.reject(error);
  }
);

export default api;
