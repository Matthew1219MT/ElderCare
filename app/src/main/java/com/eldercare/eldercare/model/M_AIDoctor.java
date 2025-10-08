package com.eldercare.eldercare.model;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.eldercare.eldercare.BuildConfig;
import com.eldercare.eldercare.data.ChatCompletionResponse;
import com.eldercare.eldercare.data.ChatRequest;
import com.eldercare.eldercare.data.Message;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class M_AIDoctor {

    //API Url
    private static final String openaiURL = "https://api.openai.com/v1/chat/completions";
    //AI Model Type
    private static final String openaiModel = "gpt-4o-mini";
    //API KEY
    private static final String openaiApiKey = BuildConfig.OPENAI_API_KEY;

    private final RequestQueue requestQueue;
    private final Gson gson;

    private final String prePrompt = "You are an AI Doctor prototype designed to help elders analyze possible conditions based on their reported symptoms. Keep your answers short and simple.";

    //Callback Interfaces
    public interface OnSuccessCallBack {
        void onSuccess(String response);
    }

    public interface OnErrorCallBack {
        void onError(String error);
    }

    public M_AIDoctor (Context context) {
        this.requestQueue = Volley.newRequestQueue(context);
        this.gson = new Gson();
    }

    public void askAI(String query, OnSuccessCallBack onSuccess, OnErrorCallBack onError) {
        ChatRequest chat_request = new ChatRequest(
                openaiModel,
                List.of(
                    new Message("system", prePrompt),
                    new Message("user", query)
                )
        );
        String json_body = gson.toJson(chat_request);
        StringRequest request = new StringRequest(
            Request.Method.POST,
            openaiURL,
        responseStr -> {
                    try {
                        ChatCompletionResponse resp = gson.fromJson(responseStr, ChatCompletionResponse.class);
                        String reply = null;
                        if (resp != null &&
                                resp.choices != null &&
                                !resp.choices.isEmpty() &&
                                resp.choices.get(0).message != null) {
                            reply = resp.choices.get(0).message.content;
                        }

                        if (reply != null) {
                            onSuccess.onSuccess(reply);
                        } else {
                            onError.onError("No reply content found");
                        }
                    } catch (Exception e) {
                        onError.onError("Error parsing response: " + e.getMessage());
                    }
                },
            error -> {
                    Log.e("M_AIDoctor", "Full error: " + error.toString());
                    if (error.networkResponse != null) {
                        Log.e("M_AIDoctor", "Status code: " + error.networkResponse.statusCode);
                        Log.e("M_AIDoctor", "Response: " + new String(error.networkResponse.data));
                    }
                    onError.onError("Error Fetching Result: " + new String(error.networkResponse.data));}
        ) {
            @Override
            public byte[] getBody() {
                return json_body.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + openaiApiKey);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
        requestQueue.add(request);
    }

    public void askAITest(String query, OnSuccessCallBack onSuccess, OnErrorCallBack onError) {
        // Simulate network delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // Fixed responses based on query keywords
            String response;
            String query_lower = query.toLowerCase();

            if (query_lower.contains("headache") || query_lower.contains("head")) {
                response = "For headaches, try resting in a quiet, dark room. Stay hydrated and consider over-the-counter pain relievers if appropriate. If headaches persist or worsen, please consult a healthcare professional.";
            } else if (query_lower.contains("fever") || query_lower.contains("temperature")) {
                response = "For fever, rest and stay hydrated. Monitor your temperature regularly. If fever exceeds 101.3°F (38.5°C) or persists for more than 3 days, seek medical attention immediately.";
            } else if (query_lower.contains("cough")) {
                response = "For persistent cough, stay hydrated, use honey (for adults), and avoid irritants. If cough lasts more than 2 weeks or is accompanied by blood, fever, or difficulty breathing, consult a doctor.";
            } else if (query_lower.contains("stomach") || query_lower.contains("nausea")) {
                response = "For stomach issues, try eating bland foods like rice, bananas, or toast. Stay hydrated with small sips of water. Avoid dairy and fatty foods. Seek medical help if symptoms persist or worsen.";
            } else {
                response = "Thank you for your question about: '" + query + "'. This is a test response. Please consult with a healthcare professional for proper medical advice. Remember, this AI assistant is for informational purposes only.";
            }

            // Simulate success callback
            onSuccess.onSuccess(response);
            //onError.onError("Test method error");
        }, 1500); // 1.5 second delay to simulate network request
    }
}
