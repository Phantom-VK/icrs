import api from "./apiClient";
import { getErrorMessage } from "../utils/error";

const authService = {
  register: async (
    username: string,
    studentId: string,
    department: string,
    email: string,
    password: string
  ) => {
    try {
      const payload = {
        username: username,
        studentId,
        department,
        email,
        password, 
      };

      const response = await api.post("/auth/signup", payload);
      console.log("Registration success:", response.data);
      return response.data;
    } catch (error: any) {
      const message = getErrorMessage(error, "Signup failed");
      console.error("Registration failed:", message);
      throw new Error(message);
    }
  },

  login: async (email: string, password: string) => {
    try {
      const payload = { email, password };
      const response = await api.post("/auth/login", payload);

      const { token, expiresIn } = response.data || {};

      if (!token) throw new Error("Invalid login response from server.");

      // Store token + expiry
      const fallbackExpiry = 60 * 60 * 1000; // 1h
      const expiryMs = typeof expiresIn === "number" && expiresIn > 0 ? expiresIn : fallbackExpiry;
      localStorage.setItem("token", token);
      localStorage.setItem("tokenExpiry", String(Date.now() + expiryMs));

      console.log("Token stored successfully:", token);
      return response.data;
    } catch (error: any) {
      const message = getErrorMessage(error, "Invalid credentials");
      console.error("Login failed:", message);
      throw new Error(message);
    }
  },

  verify: async (email: string, verificationCode: string) => {
    try {
      const response = await api.post("/auth/verify", { email, verificationCode });
      return { message: response.data || "Account verified successfully." };
    } catch (error: any) {
      const message = getErrorMessage(error, "Verification failed");
      console.error("Verification failed:", message);
      throw new Error(message);
    }
  },

  resend: async (email: string) => {
    try {
      const response = await api.post(`/auth/resend?email=${email}`);
      return { message: response.data || "Verification code resent successfully." };
    } catch (error: any) {
      const message = getErrorMessage(error, "Failed to resend verification code");
      console.error("Resend verification failed:", message);
      throw new Error(message);
    }
  },

  getCurrentUser: async () => {
    try {
      const response = await api.get("/users/me");
      return response.data;
    } catch (error: any) {
      const message = getErrorMessage(error, "Failed to fetch user profile");
      console.error("Failed to fetch current user:", message);
      throw new Error(message);
    }
  },

  logout: () => {
    localStorage.removeItem("token");
    localStorage.removeItem("tokenExpiry");
    localStorage.setItem("isLoggedIn", "false");
    console.log("Logged out and cleared token");
  },
};

export default authService;
