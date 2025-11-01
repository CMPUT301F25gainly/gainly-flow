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

    public void setRegistrationPeriod(Date open, Date close) {}
    public void setCapacity(int capacity) {}
    public void setPosterImage(String imageId) {}
    public void setGeolocationRequired(boolean required) {}
    public boolean isRegistrationOpen() { return false; }
}
