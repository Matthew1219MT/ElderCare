package com.eldercare.eldercare.model;

public class Hospital {
    private String place_id;
    private String name;
    private String address;
    private double lat;
    private double lng;

    private String contactNbr;

    public Hospital(String place_id, String address, String name, double lat, double lng) {
        this.place_id = place_id;
        this.address = address;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.contactNbr = "";
    }

    public String getPlace_id() {
        return place_id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public double getLng() {
        return lng;
    }

    public double getLat() {
        return lat;
    }

    public String getContactNbr() {
        return contactNbr;
    }

    public void setContactNbr(String contactNbr) {
        this.contactNbr = contactNbr;
    }
}
