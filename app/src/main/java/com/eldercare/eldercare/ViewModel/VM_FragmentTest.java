package com.eldercare.eldercare.ViewModel;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.eldercare.eldercare.Fragment.FirstFragment;
import com.eldercare.eldercare.Fragment.SecondFragment;
import com.eldercare.eldercare.Model.M_AIDoctor;
import com.eldercare.eldercare.R;

public class VM_FragmentTest extends AppCompatActivity {

    private Button fragment1Btn;
    private Button fragment2Btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_test);

        Fragment firstFragment = new FirstFragment();
        Fragment secondFragment = new SecondFragment();

        fragment1Btn = findViewById(R.id.btnFragment1);
        fragment2Btn = findViewById(R.id.btnFragment2);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, firstFragment)
                .commit();

        fragment1Btn.setOnClickListener(v -> {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, firstFragment)
                    .commit();
        });

        fragment2Btn.setOnClickListener(v -> {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, secondFragment)
                    .commit();
        });
    }
}
