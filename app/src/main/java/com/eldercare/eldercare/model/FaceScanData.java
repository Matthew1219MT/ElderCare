package com.eldercare.eldercare.model;

import com.google.gson.annotations.SerializedName;

public class FaceScanData {
    @SerializedName("image")
    private String image;

    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("language")
    private String language;

    public FaceScanData() {}

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
