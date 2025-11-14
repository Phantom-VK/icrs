import api from "./apiClient";

const authService = {
  /** ✅ Register / Signup */
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
      console.log("✅ Registration success:", response.data);
      console.log(payload);
      return response.data;
    } catch (error: any) {
      console.error("❌ Registration failed:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Signup failed");
    }
  },

  /** ✅ Login (stores JWT token + expiry) */
  login: async (email: string, password: string) => {
    try {
      const payload = { email, password };
      const response = await api.post("/auth/login", payload);

      const { token, expiresIn } = response.data || {};

      if (!token) throw new Error("Invalid login response from server.");

      // Store token + expiry
      localStorage.setItem("token", token);
      localStorage.setItem("tokenExpiry", String(Date.now() + expiresIn * 1000));

      console.log("🔹 Token stored successfully:", token);
      return response.data;
    } catch (error: any) {
      console.error("❌ Login failed:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Invalid credentials");
    }
  },

  /** ✅ Verify user email */
  verify: async (email: string, verificationCode: string) => {
    try {
      const response = await api.post("/auth/verify", { email, verificationCode });
      return { message: response.data || "Account verified successfully." };
    } catch (error: any) {
      console.error("❌ Verification failed:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Verification failed");
    }
  },

  /** ✅ Resend verification code */
  resend: async (email: string) => {
    try {
      const response = await api.post(`/auth/resend?email=${email}`);
      return { message: response.data || "Verification code resent successfully." };
    } catch (error: any) {
      console.error("❌ Resend verification failed:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Failed to resend verification code");
    }
  },

  /** ✅ Get current user (using token) */
  getCurrentUser: async () => {
    try {
      console.log("🌐 Calling:", api.defaults.baseURL + "/users/me");
      const response = await api.get("/users/me");
      console.log("✅ /users/me response:", response.data);
      return response.data;
    } catch (error: any) {
      console.error("❌ Failed to fetch current user:", error.response?.status || error.message);
      throw new Error(error.response?.data?.message || "Failed to fetch user profile");
    }
  },

  /** ✅ Logout */
  logout: () => {
    localStorage.removeItem("token");
    localStorage.removeItem("tokenExpiry");
    localStorage.setItem("isLoggedIn", "false");
    console.log("🔹 Logged out and cleared token");
  },
};

export default authService;
