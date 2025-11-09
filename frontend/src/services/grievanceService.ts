// src/services/grievanceService.ts
import api from "./apiClient";

export interface GrievanceData {
  title: string;
  description: string;
  category: string;
  subcategory?: string;
  registrationNumber?: string | number;
}

const grievanceService = {
  /**
   * ✅ Submit a new grievance (student)
   */
  submit: async (data: GrievanceData) => {
    console.log("📤 Submitting grievance:", data);
    const response = await api.post("/grievances", data);
    console.log("✅ Grievance submission response:", response.data);
    return response.data;
  },

  /**
   * ✅ Get grievances of logged-in student (uses /api/grievances/student/me)
   */
  getMyGrievances: async () => {
    console.log("📥 Fetching logged-in student's grievances...");
    const response = await api.get("/grievances/student/me");
    console.log("✅ Grievances fetched:", response.data);
    return response.data;
  },

  /**
   * ✅ Get a specific grievance by ID
   */
  getById: async (id: number) => {
    const response = await api.get(`/grievances/${id}`);
    return response.data;
  },

  /**
   * ✅ Get all grievances (faculty/admin)
   */
  getAll: async (
    page = 0,
    size = 10,
    sortBy = "createdAt",
    direction = "desc"
  ) => {
    const response = await api.get(`/grievances`, {
      params: { page, size, sortBy, direction },
    });
    return response.data;
  },

  /**
   * ✅ Update grievance status (faculty/admin)
   */
  updateStatus: async (id: number, status: string) => {
    const response = await api.patch(`/grievances/${id}/status`, null, {
      params: { status },
    });
    return response.data;
  },
};

export default grievanceService;
