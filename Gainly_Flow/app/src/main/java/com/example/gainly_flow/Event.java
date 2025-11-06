package com.example.gainly_flow;

import java.util.Date;

public class Event {
    private String id;
    private String name;
    private String description;
    private Date registrationOpen;
    private Date registrationClose;
    private int capacity;
    private boolean geolocationRequired;
    private String posterImageId;
    private String organizerId;

    public Event(String id) { this.id = id; }

    // --- Setters ---
    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRegistrationPeriod(Date open, Date close) {
        this.registrationOpen = open;
        this.registrationClose = close;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void setPosterImage(String imageId) {
        this.posterImageId = imageId;
    }

    public void setGeolocationRequired(boolean required) {
        this.geolocationRequired = required;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    // --- Getters ---
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Date getRegistrationOpen() {
        return registrationOpen;
    }

    public Date getRegistrationClose() {
        return registrationClose;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isGeolocationRequired() {
        return geolocationRequired;
    }

    public String getPosterImageId() {
        return posterImageId;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    // --- Logic helper ---
    public boolean isRegistrationOpen() {
        if (registrationOpen == null || registrationClose == null) {
            return false;
        }
        Date now = new Date();
        return now.after(registrationOpen) && now.before(registrationClose);
    }

    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", registrationOpen=" + registrationOpen +
                ", registrationClose=" + registrationClose +
                ", capacity=" + capacity +
                ", geolocationRequired=" + geolocationRequired +
                ", posterImageId='" + posterImageId + '\'' +
                ", organizerId='" + organizerId + '\'' +
                '}';
    }
}