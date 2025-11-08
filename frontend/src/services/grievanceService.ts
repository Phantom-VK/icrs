import apiClient from "./apiClient";

export interface GrievanceData {
  category: string;
  subCategory: string;
  description: string;
  registrationNumber: string;
}

const grievanceService = {
  submit: async (data: GrievanceData) => {
    const response = await apiClient.post("/grievances", data);
    return response.data;
  },

  getMyGrievances: async () => {
    const response = await apiClient.get("/grievances/my");
    return response.data;
  },

  getById: async (id: number) => {
    const response = await apiClient.get(`/grievances/${id}`);
    return response.data;
  },
};

export default grievanceService;
