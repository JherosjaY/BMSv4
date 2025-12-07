package com.example.blottermanagementsystem.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "blotter_reports",
    foreignKeys = @ForeignKey(
        entity = User.class,
        parentColumns = "id",
        childColumns = "userId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("userId")}
)
public class BlotterReport {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private Integer apiId;  // ID from backend API (different from local ID)
    private String caseNumber;
    private String complainantName;
    private String complainantContact;
    private String complainantAddress;
    private String incidentType;
    private long incidentDate;
    private String incidentTime;
    private String incidentLocation;
    private String narrative;
    private String respondentName;
    private String respondentAlias;
    private String respondentAddress;
    private String respondentContact;
    private String accusation;
    private String relationshipToComplainant;
    private String status;
    private long dateFiled;
    private String assignedOfficer;
    private Integer assignedOfficerId;
    private String assignedOfficerIds;
    private int userId;
    private boolean isArchived;
    private Long archivedDate;
    private String archivedBy;
    private String archivedReason;
    private String imageUris;
    private String videoUris;
    private String videoDurations;
    private String audioUri;
    private String audioUris;
    private String audioDurations;
    private double latitude = 0.0;
    private double longitude = 0.0;

    public BlotterReport() {
        // No-arg constructor for Room
        this.status = "pending";
        this.dateFiled = System.currentTimeMillis();
        this.isArchived = false;
    }

    @Ignore
    public BlotterReport(String caseNumber, String complainantName, String complainantContact,
                         String complainantAddress, String incidentType, long incidentDate,
                         String incidentTime, String incidentLocation, String narrative, int userId) {
        this.caseNumber = caseNumber;
        this.complainantName = complainantName;
        this.complainantContact = complainantContact;
        this.complainantAddress = complainantAddress;
        this.incidentType = incidentType;
        this.incidentDate = incidentDate;
        this.incidentTime = incidentTime;
        this.incidentLocation = incidentLocation;
        this.narrative = narrative;
        this.userId = userId;
        this.respondentName = "N/A";
        this.respondentAddress = "N/A";
        this.status = "pending";
        this.dateFiled = System.currentTimeMillis();
        this.assignedOfficer = "";
        this.assignedOfficerIds = "";
        this.isArchived = false;
        this.imageUris = "";
        this.videoUris = "";
        this.videoDurations = "";
        this.audioUri = "";
        this.audioUris = "";
        this.audioDurations = "";
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getApiId() { return apiId; }
    public void setApiId(Integer apiId) { this.apiId = apiId; }

    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }

    public String getComplainantName() { return complainantName; }
    public void setComplainantName(String complainantName) { this.complainantName = complainantName; }

    public String getComplainantContact() { return complainantContact; }
    public void setComplainantContact(String complainantContact) { this.complainantContact = complainantContact; }

    public String getComplainantAddress() { return complainantAddress; }
    public void setComplainantAddress(String complainantAddress) { this.complainantAddress = complainantAddress; }

    public String getIncidentType() { return incidentType; }
    public void setIncidentType(String incidentType) { this.incidentType = incidentType; }

    public long getIncidentDate() { return incidentDate; }
    public void setIncidentDate(long incidentDate) { this.incidentDate = incidentDate; }

    public String getIncidentTime() { return incidentTime; }
    public void setIncidentTime(String incidentTime) { this.incidentTime = incidentTime; }

    public String getIncidentLocation() { return incidentLocation; }
    public void setIncidentLocation(String incidentLocation) { this.incidentLocation = incidentLocation; }

    public String getNarrative() { return narrative; }
    public void setNarrative(String narrative) { this.narrative = narrative; }

    public String getRespondentName() { return respondentName; }
    public void setRespondentName(String respondentName) { this.respondentName = respondentName; }

    public String getRespondentAlias() { return respondentAlias; }
    public void setRespondentAlias(String respondentAlias) { this.respondentAlias = respondentAlias; }

    public String getRespondentAddress() { return respondentAddress; }
    public void setRespondentAddress(String respondentAddress) { this.respondentAddress = respondentAddress; }

    public String getRespondentContact() { return respondentContact; }
    public void setRespondentContact(String respondentContact) { this.respondentContact = respondentContact; }

    public String getAccusation() { return accusation; }
    public void setAccusation(String accusation) { this.accusation = accusation; }

    public String getRelationshipToComplainant() { return relationshipToComplainant; }
    public void setRelationshipToComplainant(String relationshipToComplainant) { this.relationshipToComplainant = relationshipToComplainant; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getDateFiled() { return dateFiled; }
    public void setDateFiled(long dateFiled) { this.dateFiled = dateFiled; }

    public String getAssignedOfficer() { return assignedOfficer; }
    public void setAssignedOfficer(String assignedOfficer) { this.assignedOfficer = assignedOfficer; }

    public Integer getAssignedOfficerId() { return assignedOfficerId; }
    public void setAssignedOfficerId(Integer assignedOfficerId) { this.assignedOfficerId = assignedOfficerId; }

    public String getAssignedOfficerIds() { return assignedOfficerIds; }
    public void setAssignedOfficerIds(String assignedOfficerIds) { this.assignedOfficerIds = assignedOfficerIds; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }

    public Long getArchivedDate() { return archivedDate; }
    public void setArchivedDate(Long archivedDate) { this.archivedDate = archivedDate; }

    public String getArchivedBy() { return archivedBy; }
    public void setArchivedBy(String archivedBy) { this.archivedBy = archivedBy; }

    public String getArchivedReason() { return archivedReason; }
    public void setArchivedReason(String archivedReason) { this.archivedReason = archivedReason; }

    public String getImageUris() { return imageUris; }
    public void setImageUris(String imageUris) { this.imageUris = imageUris; }

    public String getVideoUris() { return videoUris; }
    public void setVideoUris(String videoUris) { this.videoUris = videoUris; }

    public String getVideoDurations() { return videoDurations; }
    public void setVideoDurations(String videoDurations) { this.videoDurations = videoDurations; }

    public String getAudioUri() { return audioUri; }
    public void setAudioUri(String audioUri) { this.audioUri = audioUri; }

    public String getAudioUris() { return audioUris; }
    public void setAudioUris(String audioUris) { this.audioUris = audioUris; }

    public String getAudioDurations() { return audioDurations; }
    public void setAudioDurations(String audioDurations) { this.audioDurations = audioDurations; }

    // Alias methods for compatibility
    public String getDescription() { return narrative; }
    public void setDescription(String description) { this.narrative = description; }

    public String getLocation() { return incidentLocation; }
    public void setLocation(String location) { this.incidentLocation = location; }

    public String getContactNumber() { return complainantContact; }
    public void setContactNumber(String contactNumber) { this.complainantContact = contactNumber; }

    public String getAddress() { return complainantAddress; }
    public void setAddress(String address) { this.complainantAddress = address; }

    public int getReportedById() { return userId; }
    public void setReportedById(int reportedById) { this.userId = reportedById; }

    public long getCreatedAt() { return dateFiled; }
    public void setCreatedAt(long createdAt) { this.dateFiled = createdAt; }

    public long getUpdatedAt() { return dateFiled; }
    public void setUpdatedAt(long updatedAt) { this.dateFiled = updatedAt; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
