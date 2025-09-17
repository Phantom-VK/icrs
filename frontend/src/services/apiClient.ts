// src/services/apiClient.ts
import axios from "axios";

// Base URL of your Spring Boot backend
const API_BASE_URL = "http://localhost:8080/api";  // update if your backend runs on another port

// Create Axios instance
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Optional: Add request interceptor for auth tokens if needed
// apiClient.interceptors.request.use(
//   (config) => {
//     const token = localStorage.getItem("token"); // assuming you store JWT in localStorage
//     if (token) {
//       config.headers.Authorization = `Bearer ${token}`;
//     }
//     return config;
//   },
//   (error) => Promise.reject(error)
// );

export default apiClient;
