package com.example.gainly_flow;

import java.util.Date;

/**
 * Represents the location data for an entrant when they joined an event's waiting list.
 * Stores latitude, longitude, and timestamp of when they joined.
 */
public class EntrantLocation {
    private String entrantId;
    private String entrantName;
    private double latitude;
    private double longitude;
    private Date joinedAt;

    public EntrantLocation() {
        // Default constructor required for Firestore
    }

    public EntrantLocation(String entrantId, String entrantName, double latitude, double longitude, Date joinedAt) {
        this.entrantId = entrantId;
        this.entrantName = entrantName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.joinedAt = joinedAt;
    }

    // Getters
    public String getEntrantId() { return entrantId; }
    public String getEntrantName() { return entrantName; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Date getJoinedAt() { return joinedAt; }

    // Setters
    public void setEntrantId(String entrantId) { this.entrantId = entrantId; }
    public void setEntrantName(String entrantName) { this.entrantName = entrantName; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setJoinedAt(Date joinedAt) { this.joinedAt = joinedAt; }

    @Override
    public String toString() {
        return "EntrantLocation{" +
                "entrantId='" + entrantId + '\'' +
                ", entrantName='" + entrantName + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", joinedAt=" + joinedAt +
                '}';
    }
}