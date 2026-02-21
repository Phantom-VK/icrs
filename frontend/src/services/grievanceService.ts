import api from "./apiClient";

export interface GrievanceData {
  title: string;
  description: string;
  category: string;
  subcategory?: string;
  registrationNumber?: string | number;
  priority?: string; // optional if backend supports it
}

const grievanceService = {
  submit: async (data: GrievanceData) => {
    try {
      console.log("Submitting grievance:", data);
      const response = await api.post("/grievances", data);
      console.log("Grievance submitted successfully:", response.data);
      return response.data;
    } catch (error: any) {
      console.error("Failed to submit grievance:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Failed to submit grievance.");
    }
  },

  getMyGrievances: async () => {
    try {
      console.log("Fetching grievances of logged-in student...");
      const response = await api.get("/grievances/student/me");
      console.log("Grievances fetched:", response.data);
      return response.data;
    } catch (error: any) {
      console.error("Failed to fetch student's grievances:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Unable to fetch your grievances.");
    }
  },

  getById: async (id: number) => {
    try {
      console.log(`Fetching grievance with ID: ${id}`);
      const response = await api.get(`/grievances/${id}`);
      return response.data;
    } catch (error: any) {
      console.error("Failed to fetch grievance:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Grievance not found.");
    }
  },

  getAll: async (page = 0, size = 10, sortBy = "createdAt", direction = "desc") => {
    try {
      console.log("Fetching all grievances...");
      const response = await api.get("/grievances", {
        params: { page, size, sortBy, direction },
      });
      console.log("All grievances fetched:", response.data);
      return response.data;
    } catch (error: any) {
      console.error("Failed to fetch grievances:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Unable to fetch grievances.");
    }
  },

  updateStatus: async (id: number, status: string) => {
    try {
      console.log(`Updating grievance #${id} status to: ${status}`);
      const response = await api.patch(`/grievances/${id}/status`, null, {
        params: { status },
      });
      console.log("Grievance status updated:", response.data);
      return response.data;
    } catch (error: any) {
      console.error("Failed to update grievance status:", error.response?.data || error.message);
      throw new Error(error.response?.data?.message || "Unable to update grievance status.");
    }
  },
};

export default grievanceService;
