import api from "./apiClient";
import { getErrorMessage } from "../utils/error";
import type { Category } from "../types/category";
import type { Grievance } from "../types/grievance";
import { sortGrievancesByLatest } from "../utils/grievanceFilters";

export interface GrievanceData {
  title: string;
  description: string;
  category?: string;
  subcategory?: string;
  categoryId?: number;
  subcategoryId?: number;
  registrationNumber?: string | number;
  priority?: string; // optional if backend supports it
}

export interface Comment {
  id: number;
  body: string;
  authorName?: string;
  authorEmail?: string;
  createdAt?: string;
}

let myGrievancesRequest: Promise<Grievance[]> | null = null;
let categoriesRequest: Promise<Category[]> | null = null;
const allGrievancesRequests = new Map<string, Promise<{ content?: Grievance[] } | Grievance[]>>();

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
    if (myGrievancesRequest) {
      return myGrievancesRequest;
    }

    myGrievancesRequest = (async () => {
      try {
        console.log("Fetching grievances of logged-in student...");
        const response = await api.get("/grievances/student/me");
        console.log("Grievances fetched:", response.data);
        return sortGrievancesByLatest(response.data as Grievance[]);
      } catch (error: any) {
        const message = getErrorMessage(error, "Unable to fetch your grievances.");
        console.error("Failed to fetch student's grievances:", message);
        throw new Error(message);
      } finally {
        myGrievancesRequest = null;
      }
    })();

    return myGrievancesRequest;
  },

  getById: async (id: number) => {
    try {
      console.log(`Fetching grievance with ID: ${id}`);
      const response = await api.get(`/grievances/${id}`);
      return response.data as Grievance;
    } catch (error: any) {
      const message = getErrorMessage(error, "Grievance not found.");
      console.error("Failed to fetch grievance:", message);
      throw new Error(message);
    }
  },

  getAll: async (page = 0, size = 10, sortBy = "createdAt", direction = "desc") => {
    const requestKey = JSON.stringify({ page, size, sortBy, direction });
    const existingRequest = allGrievancesRequests.get(requestKey);
    if (existingRequest) {
      return existingRequest;
    }

    const request = (async () => {
      try {
        console.log("Fetching all grievances...");
        const response = await api.get("/grievances", {
          params: { page, size, sortBy, direction },
        });
        console.log("All grievances fetched:", response.data);
        return response.data as { content?: Grievance[] } | Grievance[];
      } catch (error: any) {
        const message = getErrorMessage(error, "Unable to fetch grievances.");
        console.error("Failed to fetch grievances:", message);
        throw new Error(message);
      } finally {
        allGrievancesRequests.delete(requestKey);
      }
    })();

    allGrievancesRequests.set(requestKey, request);
    return request;
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

  getComments: async (grievanceId: number) => {
    try {
      const response = await api.get(`/grievances/${grievanceId}/comments`);
      return response.data as Comment[];
    } catch (error: any) {
      const message =
        error?.response?.data?.message ||
        error?.response?.data ||
        "Failed to load comments.";
      throw new Error(message);
    }
  },

  addComment: async (grievanceId: number, body: string) => {
    try {
      const response = await api.post(`/grievances/${grievanceId}/comments`, {
        body,
      });
      return response.data as Comment;
    } catch (error: any) {
      const message =
        error?.response?.data?.message ||
        error?.response?.data ||
        "Failed to add comment.";
      throw new Error(message);
    }
  },

  getCategories: async (): Promise<Category[]> => {
    if (categoriesRequest) {
      return categoriesRequest;
    }

    categoriesRequest = (async () => {
      try {
        const response = await api.get("/categories");
        return response.data as Category[];
      } catch (error: any) {
        const message = getErrorMessage(error, "Unable to load categories.");
        throw new Error(message);
      } finally {
        categoriesRequest = null;
      }
    })();

    return categoriesRequest;
  },
};

export default grievanceService;
