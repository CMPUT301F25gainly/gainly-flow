package com.example.gainly_flow;

public class GeoLocationManager {
    public static class Coordinates {
        public final double lat, lng;
        public Coordinates(double lat, double lng) { this.lat = lat; this.lng = lng; }
    }

    public Coordinates captureJoinLocation(String entrantId, String eventId) { return null; }
    public Coordinates getLocationForEntrant(String entrantId, String eventId) { return null; }
}
