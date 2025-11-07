package com.example.gainly_flow;

public abstract class User {
    protected String id;
    protected Profile profile;

    public User(String id, Profile profile) {
        this.id = id;
        this.profile = profile;
    }

    public String getId() { return id; }
    public Profile getProfile() { return profile; }

    public abstract String getRole();
}
