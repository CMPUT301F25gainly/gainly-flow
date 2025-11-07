package com.example.gainly_flow;

import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

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

//     public Event(String id) { this.id = id; }
//     public Event() {}

    // New fields for better compatibility
    private String location;
    private String timeString; // For flexible time storage
    private int currentParticipants;
    private double price;
    private String category;
    private boolean isActive = true;

    // Empty constructor required for Firestore
    public Event() {}

    public Event(String id) {
        this.id = id;
        this.isActive = true;
    }

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

    public void setQrUrl(String qrUrl) {
        this.qrUrl = qrUrl;
    }

    // New setters
    public void setLocation(String location) {
        this.location = location;
    }

    public void setTimeString(String timeString) {
        this.timeString = timeString;
    }

    public void setCurrentParticipants(int currentParticipants) {
        this.currentParticipants = currentParticipants;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // --- Getters ---
    public String getId() {
        return id;
    }

    public String getQrUrl() {
        return qrUrl;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public Time getEventTime() {
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

    // New getters
    public String getLocation() {
        return location;
    }

    public String getTimeString() {
        return timeString;
    }

    public int getCurrentParticipants() {
        return currentParticipants;
    }

    public double getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public boolean isActive() {
        return isActive;
    }

    // --- Helper methods for QR scanner compatibility ---

    /**
     * Gets event time as formatted string for display
     */
    public String getEventTimeString() {
        if (timeString != null && !timeString.isEmpty()) {
            return timeString;
        }
        if (eventTime != null) {
            return eventTime.toString();
        }
        return "Time not specified";
    }

    /**
     * Gets formatted price for display
     */
    public String getFormattedPrice() {
        if (price == 0) {
            return "Free";
        } else {
            return String.format("$%.2f", price);
        }
    }

    /**
     * Checks if event is full
     */
    public boolean isFull() {
        return currentParticipants >= capacity;
    }

    /**
     * Gets available spots
     */
    public int getAvailableSpots() {
        return capacity - currentParticipants;
    }

    // --- Logic helper ---
    public boolean isRegistrationOpen() {
        if (registrationOpen == null || registrationClose == null) {
            return false;
        }
        Date now = new Date();
        return now.after(registrationOpen) && now.before(registrationClose) && isActive && !isFull();
    }

    /**
     * Gets registration status as string
     */
    public String getRegistrationStatus() {
        if (!isActive) {
            return "CANCELLED";
        } else if (isFull()) {
            return "FULL";
        } else if (isRegistrationOpen()) {
            return "OPEN";
        } else {
            return "CLOSED";
        }
    }

    // Event.java
    public String getEventId() {
        return id;
    }

    private static final String TAG = "Event";

    public void fromDocument(@NonNull DocumentSnapshot document) {
        // Minimal, safe mapping so it compiles and runs
        // (add more fields if you have them)
        this.id = document.getId();  // keep Firestore doc id as eventId
        // If you have fields in Firestore, you can optionally copy them:
        Event e = document.toObject(Event.class);
        if (e != null) {
            // copy whatever fields your model defines
            // e.g., this.title = e.title; this.description = e.description; ...
        }
    }


    public void load(String eventId, OnSuccessListener<Event> listener) {
        Database.get("events", eventId, document -> {
            if (document.exists()) {
                fromDocument(document);
                Log.d(TAG, "Loaded event " + eventId);
            } else {
                Log.w(TAG, "Event not found: " + eventId);
                this.id = eventId;   // <-- keep requested id to avoid nulls
            }
            listener.onSuccess(this);
        });
    }

    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", eventDate=" + eventDate +
                ", eventTime=" + eventTime +
                ", timeString='" + timeString + '\'' +
                ", registrationOpen=" + registrationOpen +
                ", registrationClose=" + registrationClose +
                ", capacity=" + capacity +
                ", currentParticipants=" + currentParticipants +
                ", geolocationRequired=" + geolocationRequired +
                ", location='" + location + '\'' +
                ", price=" + price +
                ", category='" + category + '\'' +
                ", isActive=" + isActive +
                ", posterImageId='" + posterImageId + '\'' +
                ", organizerId='" + organizerId + '\'' +
                ", qrUrl='" + qrUrl + '\'' +
                '}';
    }
}