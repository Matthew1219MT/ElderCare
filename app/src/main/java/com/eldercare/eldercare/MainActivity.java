package com.eldercare.eldercare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.eldercare.eldercare.activity.FaceScanActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStartFaceScan = findViewById(R.id.btnStartFaceScan);
        btnStartFaceScan.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FaceScanActivity.class);
            startActivity(intent);
        });
    }
}