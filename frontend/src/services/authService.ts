// src/services/authService.ts
import api from "./apiClient";

const authService = {
  // ✅ Signup / Register
  register: async (
    fullname: string,
    studentId: string,
    department: string,
    email: string,
    password: string
  ) => {
    const payload = {
      username: fullname,
      email,
      password,
      studentId,
      department,
    };

    const response = await api.post("/auth/signup", payload);
    return response.data;
  },

  // ✅ Login (returns and stores token + expiry)
  login: async (email: string, password: string) => {
    const payload = { email, password };
    const response = await api.post("/auth/login", payload);

    const { token, expiresIn } = response.data || {};

    if (!token) throw new Error("Invalid login response from server.");

    // Store token + expiry in localStorage
    localStorage.setItem("token", token);
    localStorage.setItem("tokenExpiry", String(expiresIn));

    console.log("🔹 Token stored successfully");

    return response.data;
  },

  // ✅ Verify user email
  verify: async (email: string, verificationCode: string) => {
    const response = await api.post("/auth/verify", { email, verificationCode });
    return { message: response.data || "Account verified successfully." };
  },

  // ✅ Resend verification code
  resend: async (email: string) => {
    const response = await api.post(`/auth/resend?email=${email}`);
    return { message: response.data || "Verification code resent successfully." };
  },

  // ✅ Get currently logged-in user
  getCurrentUser: async () => {
    try {
      const response = await api.get("/users/me");
      console.log("✅ /users/me response:", response.data);
      return response.data;
    } catch (error: any) {
      console.error("❌ Failed to fetch current user:", error.response?.status || error.message);
      throw error;
    }
  },

  // ✅ Logout
  logout: () => {
    localStorage.removeItem("token");
    localStorage.removeItem("tokenExpiry");
    console.log("🔹 Logged out and cleared token");
  },
};

export default authService;
