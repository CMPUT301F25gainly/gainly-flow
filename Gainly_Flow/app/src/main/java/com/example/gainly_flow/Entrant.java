package com.example.gainly_flow;

public class Entrant extends User {
    public Entrant(String id, Profile profile) { super(id, profile); }

    @Override public String getRole() { return "Entrant"; }

    public void joinWaitingList(String eventId) {}
    public void leaveWaitingList(String eventId) {}
    public void acceptInvitation(String eventId) {}
    public void declineInvitation(String eventId) {}
}