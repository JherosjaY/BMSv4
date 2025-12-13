export interface Hearing {
  id: number;
  blotterReportId: number;
  hearingDate: string;
  hearingTime: string;
  location: string;
  purpose?: string;
  presider?: string;
  attendees?: string;
  notes?: string;
  status: "Scheduled" | "Ongoing" | "Completed" | "Cancelled";
  createdAt: Date;
}

export interface CreateHearingRequest {
  blotterReportId: number;
  hearingDate: string;
  hearingTime: string;
  location: string;
  purpose?: string;
  presider?: string;
  attendees?: string;
  notes?: string;
  status?: string;
}

export interface UpdateHearingRequest {
  hearingDate?: string;
  hearingTime?: string;
  location?: string;
  purpose?: string;
  presider?: string;
  attendees?: string;
  notes?: string;
  status?: string;
}

export interface HearingResponse {
  success: boolean;
  data: Hearing;
}

export interface HearingsListResponse {
  success: boolean;
  data: Hearing[];
}
