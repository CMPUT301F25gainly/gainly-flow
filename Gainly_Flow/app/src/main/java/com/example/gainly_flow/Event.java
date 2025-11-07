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

/**
 * Represents an event entity stored in/loaded from Firestore and used across the app UI.
 * <p>
 * This model provides:
 * <ul>
 *   <li>Core descriptive fields (name, description, location, category, poster, organizer)</li>
 *   <li>Scheduling information (date/time or a flexible {@code timeString})</li>
 *   <li>Registration window (open/close), capacity and occupancy tracking</li>
 *   <li>Status helpers (registration status, whether full/active)</li>
 *   <li>Convenience formatting (price display, time display)</li>
 *   <li>Lightweight Firestore mapping via {@link #fromDocument(DocumentSnapshot)} and
 *       asynchronous loading via {@link #load(String, OnSuccessListener)}</li>
 * </ul>
 *
 * <h3>Firestore compatibility</h3>
 * The class includes a public no-arg constructor and public setters/getters for
 * fields that may be serialized/deserialized by Firestore.
 *
 * <p><b>Note:</b> Some properties are provided in multiple representations
 * (e.g., {@link #eventTime} and {@link #timeString}) to allow flexible storage and display.
 */
public class Event {
    /** Firestore document ID for this event. */
    private String id;
    /** Human-readable event name/title. */
    private String name;
    /** Long-form description of the event. */
    private String description;
    /** Calendar date of the event (no time component). */
    private Date eventDate;
    /** Clock time of the event (optional when {@link #timeString} is used). */
    private Time eventTime;
    /** Registration opening datetime. */
    private Date registrationOpen;
    /** Registration closing datetime. */
    private Date registrationClose;
    /** Maximum number of participants allowed. */
    private int capacity;
    /** Whether attendees must provide geolocation to participate. */
    private boolean geolocationRequired;
    /** Identifier for the poster image asset (e.g., Cloud Storage ID). */
    private String posterImageId;
    /** Organizer's user ID. */
    private String organizerId;
    /** URL encoded in the event QR code (e.g., for check-in). */
    private String qrUrl;

    //     public Event(String id) { this.id = id; }
    //     public Event() {}

    // New fields for better compatibility
    /** Venue or address of the event. */
    private String location;
    /** Flexible, display-ready time string (e.g., "6:30 PM MDT"). */
    private String timeString; // For flexible time storage
    /** Current number of registered participants. */
    private int currentParticipants;
    /** Ticket price; zero denotes a free event. */
    private double price;
    /** Category or tag (e.g., "Workshop", "Seminar"). */
    private String category;
    /** Logical active flag; inactive events are treated as cancelled. */
    private boolean isActive = true;

    /**
     * Empty constructor required by Firestore for deserialization.
     */
    public Event() {}

    /**
     * Constructs an {@code Event} with the given identifier.
     *
     * @param id Firestore document ID of the event.
     */
    public Event(String id) {
        this.id = id;
        this.isActive = true;
    }

    // --- Setters ---

    /**
     * Sets the event name/title.
     *
     * @param name human-readable event name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the event description.
     *
     * @param description long-form event description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the event time component.
     * <p>
     * If you prefer a more flexible representation for UI display, consider using
     * {@link #setTimeString(String)}.
     *
     * @param time the clock time of the event.
     */
    public void setEventTime(Time time){
        this.eventTime = time;
    }

    /**
     * Sets the event date component (no time).
     *
     * @param date the calendar date of the event.
     */
    public void setEventDate(Date date){
        this.eventDate = date;
    }

    /**
     * Sets the registration open/close window.
     *
     * @param open  registration opening datetime.
     * @param close registration closing datetime.
     */
    public void setRegistrationPeriod(Date open, Date close) {
        this.registrationOpen = open;
        this.registrationClose = close;
    }

    /**
     * Sets the maximum number of participants.
     *
     * @param capacity maximum capacity; values &lt; 0 are not recommended.
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Sets the poster image identifier.
     *
     * @param imageId identifier for the poster image asset.
     */
    public void setPosterImage(String imageId) {
        this.posterImageId = imageId;
    }

    /**
     * Sets whether geolocation is required.
     *
     * @param required {@code true} if geolocation is required; otherwise {@code false}.
     */
    public void setGeolocationRequired(boolean required) {
        this.geolocationRequired = required;
    }

    /**
     * Sets the organizer's user ID.
     *
     * @param organizerId organizer identifier.
     */
    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    /**
     * Sets the QR URL used for check-in or event linking.
     *
     * @param qrUrl URL encoded within the event's QR code.
     */
    public void setQrUrl(String qrUrl) {
        this.qrUrl = qrUrl;
    }

    // New setters

    /**
     * Sets the event location/venue.
     *
     * @param location human-readable location or address.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Sets a flexible, display-ready time string.
     * <p>
     * Use this when the event time is better expressed as a formatted string rather than a {@link Time}.
     *
     * @param timeString display-friendly time (e.g., "6:30 PM MDT").
     */
    public void setTimeString(String timeString) {
        this.timeString = timeString;
    }

    /**
     * Sets the current number of registered participants.
     *
     * @param currentParticipants number of participants registered so far.
     */
    public void setCurrentParticipants(int currentParticipants) {
        this.currentParticipants = currentParticipants;
    }

    /**
     * Sets the ticket price in the event's currency.
     * <p>
     * A value of {@code 0} is treated as a free event in {@link #getFormattedPrice()}.
     *
     * @param price non-negative price value.
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * Sets the event category.
     *
     * @param category category/tag such as "Workshop", "Seminar", etc.
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Sets whether the event is currently active.
     * <p>
     * Inactive events are considered cancelled in {@link #getRegistrationStatus()}.
     *
     * @param active {@code true} if active; {@code false} to mark cancelled/inactive.
     */
    public void setActive(boolean active) {
        isActive = active;
    }

    // --- Getters ---

    /**
     * Returns the Firestore document ID of this event.
     *
     * @return event identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the QR URL associated with the event.
     *
     * @return QR code URL.
     */
    public String getQrUrl() {
        return qrUrl;
    }

    /**
     * Returns the event name/title.
     *
     * @return event name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the event description.
     *
     * @return event description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the event calendar date (no time).
     *
     * @return event date or {@code null} if unset.
     */
    public Date getEventDate() {
        return eventDate;
    }

    /**
     * Returns the event clock time.
     *
     * @return {@link Time} or {@code null} if unset.
     */
    public Time getEventTime() {
        return eventTime;
    }

    /**
     * Returns the registration opening datetime.
     *
     * @return registration open time or {@code null}.
     */
    public Date getRegistrationOpen() {
        return registrationOpen;
    }

    /**
     * Returns the registration closing datetime.
     *
     * @return registration close time or {@code null}.
     */
    public Date getRegistrationClose() {
        return registrationClose;
    }

    /**
     * Returns the maximum allowed participants.
     *
     * @return capacity as a non-negative integer.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Indicates whether geolocation is required for attendees.
     *
     * @return {@code true} if required; otherwise {@code false}.
     */
    public boolean isGeolocationRequired() {
        return geolocationRequired;
    }

    /**
     * Returns the poster image identifier.
     *
     * @return poster image ID or {@code null}.
     */
    public String getPosterImageId() {
        return posterImageId;
    }

    /**
     * Returns the organizer's user ID.
     *
     * @return organizer ID or {@code null}.
     */
    public String getOrganizerId() {
        return organizerId;
    }

    // New getters

    /**
     * Returns the human-readable location.
     *
     * @return location/venue string.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Returns the display-friendly time string.
     *
     * @return time string if set, else {@code null}.
     */
    public String getTimeString() {
        return timeString;
    }

    /**
     * Returns the number of currently registered participants.
     *
     * @return current participant count.
     */
    public int getCurrentParticipants() {
        return currentParticipants;
    }

    /**
     * Returns the ticket price.
     *
     * @return price value (zero denotes free).
     */
    public double getPrice() {
        return price;
    }

    /**
     * Returns the event category or tag.
     *
     * @return category string.
     */
    public String getCategory() {
        return category;
    }

    /**
     * Indicates whether this event is marked active.
     *
     * @return {@code true} if active; otherwise {@code false}.
     */
    public boolean isActive() {
        return isActive;
    }

    // --- Helper methods for QR scanner compatibility ---

    /**
     * Returns the event time in a display-friendly string.
     * <p>
     * If {@link #timeString} is set and non-empty, it is returned.
     * Otherwise, if {@link #eventTime} is set, its {@code toString()} is returned.
     * If neither is set, returns {@code "Time not specified"}.
     *
     * @return display-ready time string.
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
     * Returns the price as a formatted string for UI display.
     * <p>
     * Returns {@code "Free"} when {@link #price} equals zero; otherwise returns
     * a currency string like {@code "$12.99"}.
     *
     * @return formatted price string.
     */
    public String getFormattedPrice() {
        if (price == 0) {
            return "Free";
        } else {
            return String.format("$%.2f", price);
        }
    }

    /**
     * Indicates whether the event has reached full capacity.
     *
     * @return {@code true} if {@link #currentParticipants} ≥ {@link #capacity}; otherwise {@code false}.
     */
    public boolean isFull() {
        return currentParticipants >= capacity;
    }

    /**
     * Returns the number of remaining registration spots.
     * <p>
     * If the event is over capacity, the result may be negative.
     *
     * @return {@code capacity - currentParticipants}.
     */
    public int getAvailableSpots() {
        return capacity - currentParticipants;
    }

    // --- Logic helper ---

    /**
     * Indicates whether registration is currently open.
     * <p>
     * Registration is considered open if:
     * <ul>
     *   <li>{@link #registrationOpen} and {@link #registrationClose} are non-null</li>
     *   <li>The current time is between the open and close times</li>
     *   <li>The event is active and not full</li>
     * </ul>
     *
     * @return {@code true} if registration is open under the above rules; otherwise {@code false}.
     */
    public boolean isRegistrationOpen() {
        if (registrationOpen == null || registrationClose == null) {
            return false;
        }
        Date now = new Date();
        return now.after(registrationOpen) && now.before(registrationClose) && isActive && !isFull();
    }

    /**
     * Returns a high-level registration status string for UI.
     * <p>
     * Possible values:
     * <ul>
     *   <li>{@code "CANCELLED"} – when not active</li>
     *   <li>{@code "FULL"} – when capacity is reached</li>
     *   <li>{@code "OPEN"} – when {@link #isRegistrationOpen()} is true</li>
     *   <li>{@code "CLOSED"} – otherwise</li>
     * </ul>
     *
     * @return registration status label.
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

    /**
     * Returns the Firestore event ID.
     * <p>
     * Alias of {@link #getId()} provided for legacy compatibility.
     *
     * @return event ID.
     */
    public String getEventId() {
        return id;
    }

    /** Tag used for logcat messages from this class. */
    private static final String TAG = "Event";

    /**
     * Populates this instance from a Firestore document snapshot.
     * <p>
     * The current implementation copies the document ID into {@link #id} and
     * attempts to deserialize an {@code Event} object from the document for any
     * future field-by-field synchronization needs.
     *
     * @param document non-null Firestore document snapshot.
     */
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

    /**
     * Asynchronously loads the event from Firestore and invokes a callback with this instance.
     * <p>
     * This method uses {@link Database#get(String, String, OnSuccessListener)} to fetch the
     * {@code events/{eventId}} document. If the document exists, {@link #fromDocument(DocumentSnapshot)}
     * is called to populate this instance; if not, the {@link #id} is still set so consumers
     * can avoid {@code null} checks. The provided listener is then invoked with {@code this}.
     *
     * @param eventId  Firestore document ID to load.
     * @param listener success callback receiving this {@code Event} instance after load.
     */
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

    /**
     * Returns a debug-friendly string representation of this event, including core fields.
     *
     * @return string containing the current state of this event.
     */
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
