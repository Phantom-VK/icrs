import api from "./axiosConfig";

const authService = {
  // Signup / Register
  register: async (fullname: string, studentId: string, department: string, email: string, password: string) => {
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

  // Login
  login: async (email: string, password: string) => {
    const payload = { email, password };
    const response = await api.post("/auth/login", payload);

    if (response.data?.token) {
      localStorage.setItem("token", response.data.token);
    }
    return response.data;
  },

  // ✅ Verify account
  verify: async (email: string, verificationCode: string) => {
    const payload = { email, verificationCode };
    const response = await api.post("/auth/verify", payload);
    return { message: response.data || "Account verified successfully." };
  },

  // ✅ Resend verification code
  resend: async (email: string) => {
    const response = await api.post(`/auth/resend?email=${email}`);
    return { message: response.data || "Verification code resent successfully." };
  },

  // Get logged-in user
  getCurrentUser: async () => {
    const response = await api.get("/users/me");
    return response.data;
  },

  // Logout
  logout: () => {
    localStorage.removeItem("token");
  },
};

export default authService;
