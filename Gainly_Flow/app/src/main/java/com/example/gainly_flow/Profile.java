package com.example.gainly_flow;

public class Profile {
    private String name;
    private String email;
    private String phone;
    private boolean notificationsEnabled = true;

    public Profile(String name, String email, String phone) {
        this.name = name; this.email = email; this.phone = phone;
    }

    public void update(String name, String email, String phone) {}
    public void delete() {}

    // Getters/setters omitted for brevity
}
