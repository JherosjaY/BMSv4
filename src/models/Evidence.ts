export interface Evidence {
  id: number;
  blotterReportId: number;
  evidenceType: string;
  description: string;
  locationFound?: string;
  photoUri?: string;
  collectedBy?: string;
  createdAt: Date;
}

export interface CreateEvidenceRequest {
  blotterReportId: number;
  evidenceType: string;
  description: string;
  locationFound?: string;
  photoUri?: string;
  collectedBy?: string;
}

export interface UpdateEvidenceRequest {
  evidenceType?: string;
  description?: string;
  locationFound?: string;
  photoUri?: string;
  collectedBy?: string;
}

export interface EvidenceResponse {
  success: boolean;
  data: Evidence;
}

export interface EvidencesListResponse {
  success: boolean;
  data: Evidence[];
}
