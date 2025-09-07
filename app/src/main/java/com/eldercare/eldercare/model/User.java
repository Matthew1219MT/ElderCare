package com.eldercare.eldercare.model;

public class User {
    private String username;
    private String hash_password;
    private String contact;

    public User (String username, String hash_password, String contact){
        this.username = username;
        this.hash_password = hash_password;
        this.contact = contact;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHash_password() {
        return hash_password;
    }

    public void setHash_password(String hash_password) {
        this.hash_password = hash_password;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }
}
