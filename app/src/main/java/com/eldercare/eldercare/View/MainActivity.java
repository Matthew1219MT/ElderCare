package com.eldercare.eldercare.View;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.eldercare.eldercare.R;
import com.eldercare.eldercare.viewmodel.MainViewModel;

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

//        // Handle system bars
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homepageLayout), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // Initialize ViewModel
//        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
//
//        // Observe LiveData from ViewModel
//        viewModel.getWelcomeText().observe(this, message -> {
//            TextView textView = findViewById(R.id.title);
//            textView.setText(message);
//        });
    }
}