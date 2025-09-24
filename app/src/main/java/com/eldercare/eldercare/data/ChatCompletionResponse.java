package com.eldercare.eldercare.data;

public class ChatCompletionResponse {
    public java.util.List<Choice> choices;
    public static class Choice {
        public Message message; // reuse the same Message shape: role + content
    }
}
