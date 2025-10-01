// src/services/authService.ts
import apiClient from "./apiClient";

const authService = {
  register: async (
    fullname: string,
    studentId: string,
    department: string,
    email: string,
    password: string
  ) => {
    const response = await apiClient.post("/auth/signup", {
      fullname,
      studentId,
      department,
      email,
      password,
    });
    return response.data;
  },

  login: async (email: string, password: string) => {
    const response = await apiClient.post("/auth/login", { email, password });
    if (response.data.token) {
      localStorage.setItem("token", response.data.token);
    }
    return response.data;
  },

  verify: async (email: string, verificationCode: string) => {
    const response = await apiClient.post("/auth/verify", {
      email,
      verificationCode,
    });
    return response.data;
  },

  resend: async (email: string) => {
    const response = await apiClient.post("/auth/resend", { email });
    return response.data;
  },

  logout: () => {
    localStorage.removeItem("token");
  },
};

export default authService;
