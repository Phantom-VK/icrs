import api from "./apiClient";

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
      console.error("Registration failed:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Signup failed");
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
      console.error("Login failed:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Invalid credentials");
    }
  },

  verify: async (email: string, verificationCode: string) => {
    try {
      const response = await api.post("/auth/verify", { email, verificationCode });
      return { message: response.data || "Account verified successfully." };
    } catch (error: any) {
      console.error("Verification failed:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Verification failed");
    }
  },

  resend: async (email: string) => {
    try {
      const response = await api.post(`/auth/resend?email=${email}`);
      return { message: response.data || "Verification code resent successfully." };
    } catch (error: any) {
      console.error("Resend verification failed:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Failed to resend verification code");
    }
  },

  getCurrentUser: async () => {
    try {
      const response = await api.get("/users/me");
      return response.data;
    } catch (error: any) {
      console.error("Failed to fetch current user:", error.response?.status || error.message);
      throw new Error(error.response?.data?.message || "Failed to fetch user profile");
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
