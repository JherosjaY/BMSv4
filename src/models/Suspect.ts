export interface Suspect {
  id: number;
  blotterReportId: number;
  name: string;
  age?: number;
  address?: string;
  description?: string;
  createdAt: Date;
}

export interface CreateSuspectRequest {
  blotterReportId: number;
  name: string;
  age?: number;
  address?: string;
  description?: string;
}

export interface UpdateSuspectRequest {
  name?: string;
  age?: number;
  address?: string;
  description?: string;
}

export interface SuspectResponse {
  success: boolean;
  data: Suspect;
}

export interface SuspectsListResponse {
  success: boolean;
  data: Suspect[];
}
