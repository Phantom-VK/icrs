import type { StatusHistory } from "./statusHistory";

export type GrievanceStatus = "SUBMITTED" | "IN_PROGRESS" | "RESOLVED" | "REJECTED";
export type GrievancePriority = "LOW" | "MEDIUM" | "HIGH" | null;
export type GrievanceSentiment = "VERY_NEGATIVE" | "NEGATIVE" | "NEUTRAL" | "POSITIVE" | null;

export interface Grievance {
  id: number;
  title: string;
  description: string;
  category?: string;
  subcategory?: string;
  registrationNumber?: string;
  maskedRegistrationNumber?: string;
  status: GrievanceStatus;
  priority?: GrievancePriority;
  sentiment?: GrievanceSentiment;
  aiResolved?: boolean;
  aiConfidence?: number | null;
  aiTitle?: string | null;
  aiResolutionText?: string | null;
  aiResolutionComment?: string | null;
  aiModelName?: string | null;
  aiDecisionAt?: string | null;
  assignedToName?: string | null;
  sensitiveCategory?: boolean;
  identityHidden?: boolean;
  statusHistory?: StatusHistory[];
  createdAt?: string;
  updatedAt?: string;
}
