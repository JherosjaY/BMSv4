export interface BlotterReport {
  id: number;
  caseNumber: string;
  incidentType: string;
  incidentDate: string;
  incidentTime: string;
  incidentLocation: string;
  narrative: string;
  complainantName?: string;
  complainantContact?: string;
  complainantAddress?: string;
  complainantEmail?: string;
  status: "Pending" | "Assigned" | "Ongoing" | "Resolved" | "Closed";
  priority: "Low" | "Normal" | "High" | "Urgent";
  assignedOfficer?: string;
  assignedOfficerIds?: string;
  filedBy?: string;
  filedById?: number;
  audioRecordingUri?: string;
  isArchived: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export interface CreateBlotterReportRequest {
  caseNumber: string;
  incidentType: string;
  incidentDate: string;
  incidentTime: string;
  incidentLocation: string;
  narrative: string;
  complainantName?: string;
  complainantContact?: string;
  complainantAddress?: string;
  complainantEmail?: string;
  status?: string;
  priority?: string;
  filedBy?: string;
  filedById?: number;
}

export interface UpdateBlotterReportRequest {
  status?: string;
  assignedOfficer?: string;
  assignedOfficerIds?: string;
  priority?: string;
  isArchived?: boolean;
}

export interface BlotterReportResponse {
  success: boolean;
  data: BlotterReport;
}

export interface BlotterReportsListResponse {
  success: boolean;
  data: BlotterReport[];
}
