package com.eldercare.eldercare.Model;

import android.content.Context;
import android.os.Bundle;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class M_AIDoctor {

    //API Url
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    //AI Model Type
    private static final String OPENAI_MODEL = "gpt-4o-mini";
    //API KEY
    private static final String OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY;

    private final RequestQueue requestQueue;
    private final Gson gson;

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

    public void sendQuery(String query, OnSuccessCallBack onSuccess, OnErrorCallBack onError) {
        ChatRequest chat_request = new ChatRequest(
                OPENAI_MODEL,
                Arrays.asList(new Message("user", query))
        );
        String json_body = gson.toJson(chat_request);
        StringRequest request = new StringRequest(
            Request.Method.POST,
            OPENAI_URL,
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
                error -> onError.onError("Error Fetching Result: " + error.getMessage())
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
                headers.put("Authorization", "Bearer " + OPENAI_API_KEY);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
        requestQueue.add(request);
    }
}
