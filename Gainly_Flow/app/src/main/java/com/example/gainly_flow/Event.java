package com.example.gainly_flow;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import android.util.Log;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents an event in the Gainly Flow system.
 * Includes waiting list, selected/cancelled/enrolled entrants, and organizer details.
 */
public class Event {
    private static final String TAG = "Event";

    // --- Event Categories ---
    public enum Category {
        ALL, SPORT, MUSIC, ART, EDUCATION
    }

    private String id;
    private String name;
    private String description;
    private Date eventDate;
    private Time eventTime;
    private String timeString; // Flexible display time
    private Date registrationOpen;
    private Date registrationClose;
    private int capacity;
    private int currentParticipants;
    private boolean geolocationRequired;
    private String location;
    private double price;
    private Category category; // Changed to enum
    private boolean isActive = true;
    private String posterImageId;
    private Organizer organizer; // Changed from String organizerId
    private String qrUrl;

    // --- Entrant management ---
    private WaitingList waitingList;
    private List<Entrant> selected;
    private List<Entrant> cancelled;
    private List<Entrant> enrolled;

    // --- Constructors ---
    public Event() {
        this.waitingList = new WaitingList();
        this.selected = new ArrayList<>();
        this.cancelled = new ArrayList<>();
        this.enrolled = new ArrayList<>();
        this.category = Category.ALL; // Default category
    }

    public Event(String id) {
        this();
        this.id = id;
    }

    // --- Setters ---
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setEventDate(Date date) { this.eventDate = date; }
    public void setEventTime(Time time) { this.eventTime = time; }
    public void setTimeString(String timeString) { this.timeString = timeString; }
    public void setRegistrationPeriod(Date open, Date close) {
        this.registrationOpen = open;
        this.registrationClose = close;
    }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void setCurrentParticipants(int currentParticipants) { this.currentParticipants = currentParticipants; }
    public void setGeolocationRequired(boolean geolocationRequired) { this.geolocationRequired = geolocationRequired; }
    public void setLocation(String location) { this.location = location; }
    public void setPrice(double price) { this.price = price; }
    public void setCategory(Category category) { this.category = category; }
    public void setActive(boolean active) { this.isActive = active; }
    public void setPosterImageId(String posterImageId) { this.posterImageId = posterImageId; }
    public void setOrganizer(Organizer organizer) { this.organizer = organizer; }
    public void setQrUrl(String qrUrl) { this.qrUrl = qrUrl; }
    public void setWaitingList(WaitingList waitingList) { this.waitingList = waitingList; }
    public void setSelected(List<Entrant> selected) { this.selected = selected; }
    public void setCancelled(List<Entrant> cancelled) { this.cancelled = cancelled; }
    public void setEnrolled(List<Entrant> enrolled) { this.enrolled = enrolled; }

    // --- Getters ---
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Date getEventDate() { return eventDate; }
    public Time getEventTime() { return eventTime; }
    public String getTimeString() { return timeString; }
    public Date getRegistrationOpen() { return registrationOpen; }
    public Date getRegistrationClose() { return registrationClose; }
    public int getCapacity() { return capacity; }
    public int getCurrentParticipants() { return currentParticipants; }
    public boolean isGeolocationRequired() { return geolocationRequired; }
    public String getLocation() { return location; }
    public double getPrice() { return price; }
    public Category getCategory() { return category; }
    public boolean isActive() { return isActive; }
    public String getPosterImageId() { return posterImageId; }
    public Organizer getOrganizer() { return organizer; }
    public String getQrUrl() { return qrUrl; }
    public WaitingList getWaitingList() { return waitingList; }
    public List<Entrant> getSelected() { return selected; }
    public List<Entrant> getCancelled() { return cancelled; }
    public List<Entrant> getEnrolled() { return enrolled; }

    // --- Helper Methods ---
    public boolean isFull() { return currentParticipants >= capacity; }
    public int getAvailableSpots() { return capacity - currentParticipants; }

    public boolean isRegistrationOpen() {
        Date now = new Date();
        return isActive && !isFull() && registrationOpen != null && registrationClose != null &&
                now.after(registrationOpen) && now.before(registrationClose);
    }

    public String getRegistrationStatus() {
        if (!isActive) return "CANCELLED";
        if (isFull()) return "FULL";
        if (isRegistrationOpen()) return "OPEN";
        return "CLOSED";
    }

    public String getFormattedPrice() {
        return price == 0 ? "Free" : String.format("$%.2f", price);
    }

    public String getEventTimeDisplay() {
        if (timeString != null && !timeString.isEmpty()) return timeString;
        if (eventTime != null) return eventTime.toString();
        return "Time not specified";
    }

    public String getFormattedCategory() {
        return category != null ? category.name().charAt(0) + category.name().substring(1).toLowerCase() : "All";
    }

    // --- Firestore integration ---
    public void fromDocument(@NonNull DocumentSnapshot doc) {
        this.id = doc.getId();
        Event e = doc.toObject(Event.class);
        if (e != null) {
            this.name = e.name;
            this.description = e.description;
            this.eventDate = e.eventDate;
            this.eventTime = e.eventTime;
            this.timeString = e.timeString;
            this.registrationOpen = e.registrationOpen;
            this.registrationClose = e.registrationClose;
            this.capacity = e.capacity;
            this.currentParticipants = e.currentParticipants;
            this.geolocationRequired = e.geolocationRequired;
            this.location = e.location;
            this.price = e.price;
            this.category = e.category != null ? e.category : Category.ALL;
            this.isActive = e.isActive;
            this.posterImageId = e.posterImageId;
            this.organizer = e.organizer;
            this.qrUrl = e.qrUrl;
            this.waitingList = e.waitingList != null ? e.waitingList : new WaitingList();
            this.selected = e.selected != null ? e.selected : new ArrayList<>();
            this.cancelled = e.cancelled != null ? e.cancelled : new ArrayList<>();
            this.enrolled = e.enrolled != null ? e.enrolled : new ArrayList<>();
        }
    }

    public void load(String eventId, OnSuccessListener<Event> listener) {
        Database.get("events", eventId, doc -> {
            if (doc.exists()) {
                fromDocument(doc);
                Log.d(TAG, "Loaded event " + eventId);
            } else {
                Log.w(TAG, "Event not found: " + eventId);
                this.id = eventId;
            }
            listener.onSuccess(this);
        });
    }

    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", category=" + getFormattedCategory() +
                ", capacity=" + capacity +
                ", currentParticipants=" + currentParticipants +
                ", waitingList=" + (waitingList != null ? waitingList.getCount() : 0) +
                ", selected=" + selected.size() +
                ", enrolled=" + enrolled.size() +
                ", cancelled=" + cancelled.size() +
                ", isActive=" + isActive +
                '}';
    }
}
