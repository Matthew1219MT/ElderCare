package com.eldercare.eldercare.data;

public class ChatRequest {
    String model;
    java.util.List<Message> messages;
    public ChatRequest(String model, java.util.List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }
}
