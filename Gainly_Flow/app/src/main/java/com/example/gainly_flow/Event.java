package com.example.gainly_flow;

import java.sql.Time;
import java.util.Date;

public class Event {
    private String id;
    private String name;
    private String description;
    private Date eventDate;

    private Time eventTime;
    private Date registrationOpen;
    private Date registrationClose;
    private int capacity;
    private boolean geolocationRequired;
    private String posterImageId;
    private String organizerId;
    private String qrUrl;

    public Event(String id) { this.id = id; }

    // --- Setters ---
    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEventTime(Time time){
        this.eventTime = time;
    }
    public void setEventDate(Date date){
        this.eventDate = date;
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
    public void setQrUrl(String qrUrl) { this.qrUrl = qrUrl; }

    // --- Getters ---
    public String getId() {
        return id;
    }

    public String getQrUrl() { return qrUrl; }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public Date getEventTime() {
        return eventTime;
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
                ", time='" + eventTime + '\'' +
                ", date='" + eventDate + '\'' +
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