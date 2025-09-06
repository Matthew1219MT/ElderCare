package com.eldercare.eldercare;

import android.os.Bundle;
import android.widget.TextView;

import com.eldercare.eldercare.BuildConfig;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.eldercare.eldercare.data.ChatCompletionResponse;
import com.eldercare.eldercare.data.ChatRequest;
import com.eldercare.eldercare.data.Message;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

public class AIDoctor extends AppCompatActivity {

    TextView data;
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_MODEL = "gpt-4o-mini"; // pick the model you want
    private static final String OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY; // <-

    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_doctor);
        data = findViewById(R.id.data);
        RequestQueue queue = Volley.newRequestQueue(this);

        ChatRequest chatRequest = new ChatRequest(
                OPENAI_MODEL,
                Arrays.asList(new Message("user", "Tell me a joke"))
        );

        String jsonBody = gson.toJson(chatRequest);

        StringRequest request = new StringRequest(
                Request.Method.POST,
                OPENAI_URL,
                responseStr -> {
                    ChatCompletionResponse resp =
                            gson.fromJson(String.valueOf(responseStr), ChatCompletionResponse.class);
                    String reply = null;
                    if (resp != null &&
                        resp.choices != null &&
                        !resp.choices.isEmpty() &&
                        resp.choices.get(0).message != null) {
                        reply = resp.choices.get(0).message.content;
                    }
                    if (reply != null) {
                        data.setText(reply);
                    } else {
                        data.setText("No reply content found");
                    }
                },
                error -> {
                    data.setText("Error Fetching Result");
                }
        ) {
            @Override
            public byte[] getBody() {
                return jsonBody.getBytes(StandardCharsets.UTF_8);
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
        queue.add(request);
    }
}
