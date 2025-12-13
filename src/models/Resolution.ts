export interface Resolution {
  id: number;
  blotterReportId: number;
  resolutionDate: string;
  resolutionType: string;
  description?: string;
  outcome?: string;
  approvedBy?: string;
  documentUri?: string;
  status: "Pending" | "Approved" | "Rejected";
  createdAt: Date;
}

export interface CreateResolutionRequest {
  blotterReportId: number;
  resolutionDate: string;
  resolutionType: string;
  description?: string;
  outcome?: string;
  approvedBy?: string;
  documentUri?: string;
  status?: string;
}

export interface UpdateResolutionRequest {
  resolutionDate?: string;
  resolutionType?: string;
  description?: string;
  outcome?: string;
  approvedBy?: string;
  documentUri?: string;
  status?: string;
}

export interface ResolutionResponse {
  success: boolean;
  data: Resolution;
}

export interface ResolutionsListResponse {
  success: boolean;
  data: Resolution[];
}
