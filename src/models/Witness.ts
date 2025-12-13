export interface Witness {
  id: number;
  blotterReportId: number;
  name: string;
  contactNumber?: string;
  address?: string;
  statement?: string;
  createdAt: Date;
}

export interface CreateWitnessRequest {
  blotterReportId: number;
  name: string;
  contactNumber?: string;
  address?: string;
  statement?: string;
}

export interface UpdateWitnessRequest {
  name?: string;
  contactNumber?: string;
  address?: string;
  statement?: string;
}

export interface WitnessResponse {
  success: boolean;
  data: Witness;
}

export interface WitnessesListResponse {
  success: boolean;
  data: Witness[];
}
