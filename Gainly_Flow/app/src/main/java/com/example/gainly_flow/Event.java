package com.example.gainly_flow;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import android.util.Log;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents an event in the Gainly Flow system.
 * Includes waiting list, selected/cancelled/enrolled entrants, and organizer details.
 */
@IgnoreExtraProperties
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
    private String organizerId; // Store organizer ID for Firestore compatibility
    private String qrUrl;

    // --- Entrant management ---
    private List<String> waitingList; // Store entrant IDs instead of full objects
    private List<String> selected; // Store entrant IDs
    private List<String> cancelled; // Store entrant IDs
    private List<String> enrolled; // Store entrant IDs

    // --- Constructors ---
    public Event() {
        this.waitingList = new ArrayList<>();
        this.selected = new ArrayList<>();
        this.cancelled = new ArrayList<>();
        this.enrolled = new ArrayList<>();
        this.category = Category.ALL; // Default category
    }

    public Event(String id) {
        this();
        this.id = id;
    }

    public Event(String id, String name, String description, Date eventDate, String organizerId) {
        this();
        this.id = id;
        this.name = name;
        this.description = description;
        this.eventDate = eventDate;
        this.organizerId = organizerId;
    }

    // --- Setters ---
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setEventDate(Date date) { this.eventDate = date; }
    public void setEventTime(Time time) { this.eventTime = time; }
    public void setTimeString(String timeString) { this.timeString = timeString; }
    public void setRegistrationOpen(Date registrationOpen) { this.registrationOpen = registrationOpen; }
    public void setRegistrationClose(Date registrationClose) { this.registrationClose = registrationClose; }
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
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }
    public void setQrUrl(String qrUrl) { this.qrUrl = qrUrl; }
    public void setWaitingList(List<String> waitingList) { this.waitingList = waitingList != null ? waitingList : new ArrayList<>(); }
    public void setSelected(List<String> selected) { this.selected = selected != null ? selected : new ArrayList<>(); }
    public void setCancelled(List<String> cancelled) { this.cancelled = cancelled != null ? cancelled : new ArrayList<>(); }
    public void setEnrolled(List<String> enrolled) { this.enrolled = enrolled != null ? enrolled : new ArrayList<>(); }

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
    public String getOrganizerId() { return organizerId; }
    public String getQrUrl() { return qrUrl; }
    public List<String> getWaitingList() { return waitingList; }
    public List<String> getSelected() { return selected; }
    public List<String> getCancelled() { return cancelled; }
    public List<String> getEnrolled() { return enrolled; }

    // --- Firestore Helper Methods ---

    /**
     * Gets the Firestore document ID for this event.
     */
    @Exclude
    public String getDocumentId() {
        return id;
    }

    // --- Entrant Management Methods ---

    /**
     * Adds an entrant to the waiting list by their ID.
     */
    public boolean addToWaitingList(String entrantId) {
        if (entrantId != null && !waitingList.contains(entrantId) &&
                !selected.contains(entrantId) && !enrolled.contains(entrantId)) {
            return waitingList.add(entrantId);
        }
        return false;
    }

    /**
     * Removes an entrant from the waiting list.
     */
    public boolean removeFromWaitingList(String entrantId) {
        return waitingList.remove(entrantId);
    }

    /**
     * Selects an entrant from the waiting list (lottery selection).
     */
    public boolean selectEntrant(String entrantId) {
        if (waitingList.contains(entrantId) && !selected.contains(entrantId)) {
            waitingList.remove(entrantId);
            return selected.add(entrantId);
        }
        return false;
    }

    /**
     * Enrolls a selected entrant.
     */
    public boolean enrollEntrant(String entrantId) {
        if (selected.contains(entrantId) && !enrolled.contains(entrantId)) {
            selected.remove(entrantId);
            if (enrolled.add(entrantId)) {
                currentParticipants++;
                return true;
            }
        }
        return false;
    }

    /**
     * Cancels a selected entrant's participation.
     */
    public boolean cancelEntrant(String entrantId) {
        if (selected.contains(entrantId)) {
            selected.remove(entrantId);
            return cancelled.add(entrantId);
        }
        return false;
    }

    /**
     * Checks if an entrant is on the waiting list.
     */
    @Exclude
    public boolean isOnWaitingList(String entrantId) {
        return waitingList.contains(entrantId);
    }

    /**
     * Checks if an entrant is selected.
     */
    @Exclude
    public boolean isSelected(String entrantId) {
        return selected.contains(entrantId);
    }

    /**
     * Checks if an entrant is enrolled.
     */
    @Exclude
    public boolean isEnrolled(String entrantId) {
        return enrolled.contains(entrantId);
    }

    /**
     * Gets the waiting list position of an entrant.
     */
    @Exclude
    public int getWaitingListPosition(String entrantId) {
        return waitingList.indexOf(entrantId) + 1; // 1-based position
    }

    // --- Event Status Methods ---

    @Exclude
    public boolean isFull() {
        return currentParticipants >= capacity;
    }

    @Exclude
    public int getAvailableSpots() {
        return Math.max(0, capacity - currentParticipants);
    }

    @Exclude
    public int getWaitingListSize() {
        return waitingList != null ? waitingList.size() : 0;
    }

    @Exclude
    public int getSelectedCount() {
        return selected != null ? selected.size() : 0;
    }

    @Exclude
    public int getEnrolledCount() {
        return enrolled != null ? enrolled.size() : 0;
    }

    @Exclude
    public boolean isRegistrationOpen() {
        Date now = new Date();
        return isActive && !isFull() && registrationOpen != null && registrationClose != null &&
                now.after(registrationOpen) && now.before(registrationClose);
    }

    @Exclude
    public String getRegistrationStatus() {
        if (!isActive) return "CANCELLED";
        if (isFull()) return "FULL";
        if (isRegistrationOpen()) return "OPEN";
        return "CLOSED";
    }

    // --- Display Methods ---

    @Exclude
    public String getFormattedPrice() {
        return price == 0 ? "Free" : String.format("$%.2f", price);
    }

    @Exclude
    public String getEventTimeDisplay() {
        if (timeString != null && !timeString.isEmpty()) return timeString;
        if (eventTime != null) return eventTime.toString();
        return "Time not specified";
    }

    @Exclude
    public String getFormattedCategory() {
        return category != null ? category.name().charAt(0) + category.name().substring(1).toLowerCase() : "All";
    }

    @Exclude
    public String getShortDescription() {
        if (description == null) return "";
        return description.length() > 100 ? description.substring(0, 100) + "..." : description;
    }

    // --- Firestore integration ---

    /**
     * Populates this event from a Firestore document.
     */
    public void fromDocument(@NonNull DocumentSnapshot doc) {
        this.id = doc.getId();

        // Manually set fields to handle enum conversion
        if (doc.contains("name")) this.name = doc.getString("name");
        if (doc.contains("description")) this.description = doc.getString("description");
        if (doc.contains("eventDate")) this.eventDate = doc.getDate("eventDate");
        if (doc.contains("timeString")) this.timeString = doc.getString("timeString");
        if (doc.contains("registrationOpen")) this.registrationOpen = doc.getDate("registrationOpen");
        if (doc.contains("registrationClose")) this.registrationClose = doc.getDate("registrationClose");
        if (doc.contains("capacity")) this.capacity = doc.getLong("capacity").intValue();
        if (doc.contains("currentParticipants")) this.currentParticipants = doc.getLong("currentParticipants").intValue();
        if (doc.contains("geolocationRequired")) this.geolocationRequired = Boolean.TRUE.equals(doc.getBoolean("geolocationRequired"));
        if (doc.contains("location")) this.location = doc.getString("location");
        if (doc.contains("price")) this.price = doc.getDouble("price");
        if (doc.contains("category")) {
            String categoryStr = doc.getString("category");
            this.category = categoryStr != null ? Category.valueOf(categoryStr) : Category.ALL;
        }
        if (doc.contains("isActive")) this.isActive = Boolean.TRUE.equals(doc.getBoolean("isActive"));
        if (doc.contains("posterImageId")) this.posterImageId = doc.getString("posterImageId");
        if (doc.contains("organizerId")) this.organizerId = doc.getString("organizerId");
        if (doc.contains("qrUrl")) this.qrUrl = doc.getString("qrUrl");
        if (doc.contains("waitingList")) this.waitingList = (List<String>) doc.get("waitingList");
        if (doc.contains("selected")) this.selected = (List<String>) doc.get("selected");
        if (doc.contains("cancelled")) this.cancelled = (List<String>) doc.get("cancelled");
        if (doc.contains("enrolled")) this.enrolled = (List<String>) doc.get("enrolled");

        // Initialize lists if they're null
        if (this.waitingList == null) this.waitingList = new ArrayList<>();
        if (this.selected == null) this.selected = new ArrayList<>();
        if (this.cancelled == null) this.cancelled = new ArrayList<>();
        if (this.enrolled == null) this.enrolled = new ArrayList<>();
    }

    /**
     * Loads an event from Firestore by ID.
     */
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
                ", waitingList=" + getWaitingListSize() +
                ", selected=" + getSelectedCount() +
                ", enrolled=" + getEnrolledCount() +
                ", cancelled=" + (cancelled != null ? cancelled.size() : 0) +
                ", isActive=" + isActive +
                '}';
    }
}