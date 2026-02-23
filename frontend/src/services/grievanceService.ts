import api from "./apiClient";
import { getErrorMessage } from "../utils/error";

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
      const message = getErrorMessage(error, "Failed to submit grievance.");
      console.error("Failed to submit grievance:", message);
      throw new Error(message);
    }
  },

  getMyGrievances: async () => {
    try {
      console.log("Fetching grievances of logged-in student...");
      const response = await api.get("/grievances/student/me");
      console.log("Grievances fetched:", response.data);
      return response.data;
    } catch (error: any) {
      const message = getErrorMessage(error, "Unable to fetch your grievances.");
      console.error("Failed to fetch student's grievances:", message);
      throw new Error(message);
    }
  },

  getById: async (id: number) => {
    try {
      console.log(`Fetching grievance with ID: ${id}`);
      const response = await api.get(`/grievances/${id}`);
      return response.data;
    } catch (error: any) {
      const message = getErrorMessage(error, "Grievance not found.");
      console.error("Failed to fetch grievance:", message);
      throw new Error(message);
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
      const message = getErrorMessage(error, "Unable to fetch grievances.");
      console.error("Failed to fetch grievances:", message);
      throw new Error(message);
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
      const message = getErrorMessage(error, "Unable to update grievance status.");
      console.error("Failed to update grievance status:", message);
      throw new Error(message);
    }
  },
};

export default grievanceService;
