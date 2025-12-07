package com.example.blottermanagementsystem.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "person")
public class Person {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String firstName;
    private String lastName;
    private String middleName;
    private String gender;
    private Long birthDate;
    private String address;
    private String contactNumber;
    private String email;
    private String photoUri;
    private String personType;
    private long dateAdded;
    private long lastUpdated;
    private boolean isActive;
    private String notes;
    private String normalizedName;  // ✅ For case-insensitive matching

    public Person() {
        this.dateAdded = System.currentTimeMillis();
        this.lastUpdated = System.currentTimeMillis();
        this.isActive = true;
    }

    @Ignore
    public Person(String firstName, String lastName, String personType) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.personType = personType;
        this.dateAdded = System.currentTimeMillis();
        this.lastUpdated = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public Long getBirthDate() { return birthDate; }
    public void setBirthDate(Long birthDate) { this.birthDate = birthDate; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhotoUri() { return photoUri; }
    public void setPhotoUri(String photoUri) { this.photoUri = photoUri; }
    public String getPersonType() { return personType; }
    public void setPersonType(String personType) { this.personType = personType; }
    public long getDateAdded() { return dateAdded; }
    public void setDateAdded(long dateAdded) { this.dateAdded = dateAdded; }
    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(String normalizedName) { this.normalizedName = normalizedName; }

    // Alias for compatibility
    public long getCreatedAt() { return dateAdded; }
    public void setCreatedAt(long createdAt) { this.dateAdded = createdAt; }
    
    // Helper methods for full name
    public String getName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return "";
    }
    
    public void setName(String fullName) {
        if (fullName != null && !fullName.isEmpty()) {
            String[] parts = fullName.trim().split("\\s+", 2);
            if (parts.length == 2) {
                this.firstName = parts[0];
                this.lastName = parts[1];
            } else {
                this.firstName = parts[0];
                this.lastName = "";
            }
            // ✅ Auto-set normalized name
            this.normalizedName = fullName.toLowerCase().trim();
        }
    }
    
    // Alias for setDateAdded
    public void setCreatedDate(long createdDate) { this.dateAdded = createdDate; }
}
