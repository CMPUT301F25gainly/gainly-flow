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

    public Event(String id) { this.id = id; }
    public Event() {}


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