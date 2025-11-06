package com.eldercare.eldercare.view;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.eldercare.eldercare.R;
import com.eldercare.eldercare.activity.EmergencyActivity;
import com.eldercare.eldercare.activity.FaceScanActivity;
import com.eldercare.eldercare.service.FallDetectionService;
import com.eldercare.eldercare.utils.LocaleHelper;
import com.eldercare.eldercare.viewmodel.VM_HomePage;

public class V_HomePage extends BaseActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor lightSensor;
    private Sensor tempSensor;
    private float lightLevel;
    private float temperature;
    private CardView facialAnalysis, emergency, emergServices, aiDoctor;
    private VM_HomePage viewModel;
    private ImageButton btnLanguage; // Language switcher button
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_LANGUAGE_SELECTED = "language_selected_manual";


    private TextView emergencyCardText;
    private boolean emergencyIsPressed = false;
    private long emergencyPressStartTime = 0L;
    private final long triggerRequiredHoldingTime = 4000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateCountdown = new Runnable() {
        @Override
        public void run() {
            if (emergencyIsPressed) {
                long elapsed = System.currentTimeMillis() - emergencyPressStartTime;
                int remaining = (int) Math.max((triggerRequiredHoldingTime - elapsed) / 1000, 0);

                if (elapsed >= triggerRequiredHoldingTime) {
                    emergencyCardText.setText(R.string.emergency);
                    emergency.setCardBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                    Toast.makeText(V_HomePage.this, R.string.emergency_activating, Toast.LENGTH_SHORT).show();
                    activateEmergencyService();
                    emergencyIsPressed = false;
                    emergency.setCardBackgroundColor(Color.parseColor("#BF5F56"));
                } else {
                    emergencyCardText.setText(getString(R.string.keep_pressing, remaining));
                    handler.postDelayed(this, 1000);
                }
            }
        }
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        // Apply saved language before creating activity
        String languageCode = LocaleHelper.getLanguage(newBase);
        Context context = LocaleHelper.setLocale(newBase, languageCode);
        super.attachBaseContext(context);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 101){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                detectLocationAndSetLanguage();
                recreate();
            } else {
                LocaleHelper.setLocale(this, LocaleHelper.LANGUAGE_ENGLISH);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean manualSelected = prefs.getBoolean(KEY_LANGUAGE_SELECTED, false);

        if(!manualSelected){
            detectLocationAndSetLanguage();
        } else {
            String lang = LocaleHelper.getLanguage(this);
            LocaleHelper.setLocale(this, lang);
        }
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        if(tempSensor == null) {
            Toast.makeText(this, R.string.temp_sensor_unavailable, Toast.LENGTH_LONG).show();
        }
        lightLevel = -1.0f;
        temperature = -1.0f;

        // Initialize UI elements
        facialAnalysis = findViewById(R.id.facial_analysis_btn);
        emergency = findViewById(R.id.emergency_btn);
        emergencyCardText = findViewById(R.id.emergencyCardText);
        emergServices = findViewById(R.id.emergency_services);
        aiDoctor = findViewById(R.id.ai_doctor_btn);
        btnLanguage = findViewById(R.id.btn_language); // Language button

        // Setup click listeners
        setupClickListeners();
        setupLanguageSwitcher();

        // ViewModel setup
        viewModel = new ViewModelProvider(this).get(VM_HomePage.class);
        observeViewModel();
        checkNotificationPermissions();
        viewModel.handleIntent(getIntent());
    }

    private void setupClickListeners() {
        facialAnalysis.setOnClickListener(v -> {
            Intent intent = new Intent(this, FaceScanActivity.class);
            startActivity(intent);
        });

        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return true;
            }
        });

        emergency.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    emergencyIsPressed = true;
                    emergencyPressStartTime = System.currentTimeMillis();
                    handler.post(updateCountdown);
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    emergencyIsPressed = false;
                    handler.removeCallbacks(updateCountdown);
                    emergencyCardText.setText(R.string.sos);
                    return true;
            }

            return false;
        });

        emergServices.setOnClickListener(v -> {
            startActivity(new Intent(this, EmergencyActivity.class));
        });

        aiDoctor.setOnClickListener(v -> {
            Intent intent = new Intent(this, V_AIDoctor.class);
            startActivity(intent);
        });
    }

    private void setupLanguageSwitcher() {
        if (btnLanguage != null) {
            // Update button icon based on current language
            updateLanguageButton();

            btnLanguage.setOnClickListener(v -> {
                showLanguageDialog();
            });
        }
    }

    private void updateLanguageButton() {
        String currentLanguage = LocaleHelper.getLanguage(this);
        // Update button appearance based on language
        // You can change the icon or add a text indicator
    }

    private void showLanguageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_language);

        String currentLanguage = LocaleHelper.getLanguage(this);
        final String[] languages = {
                getString(R.string.english),
                getString(R.string.indonesian)
        };
        final String[] languageCodes = {
                LocaleHelper.LANGUAGE_ENGLISH,
                LocaleHelper.LANGUAGE_INDONESIAN
        };

        int checkedItem = currentLanguage.equals(LocaleHelper.LANGUAGE_INDONESIAN) ? 1 : 0;

        builder.setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
            String selectedLanguage = languageCodes[which];

            if (!selectedLanguage.equals(currentLanguage)) {
                // Save and apply new language
                LocaleHelper.setLocale(this, selectedLanguage);
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putBoolean(KEY_LANGUAGE_SELECTED, true);
                editor.apply();
                recreate();


                // Show confirmation
                Toast.makeText(this, R.string.language_changed, Toast.LENGTH_SHORT).show();

                // Recreate activity to apply changes
                dialog.dismiss();
                recreate();
            } else {
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (tempSensor != null) {
            sensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            lightLevel = event.values[0];
        } else if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            if(event.values != null && event.values.length>0){
                temperature = event.values[0];
                if(temperature > 100 || temperature < -100){
                    temperature = -1.0f;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        viewModel.handleIntent(intent);
    }

    private void observeViewModel() {
        viewModel.showFallDialog.observe(this, show -> {
            if (show) showFallDetectDialog();
        });

        viewModel.startFallService.observe(this, start -> {
            if (start) {
                Intent serviceIntent = new Intent(this, FallDetectionService.class);
                ContextCompat.startForegroundService(this, serviceIntent);
            }
        });
    }

    private void checkNotificationPermissions(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    100
            );
        } else {
            viewModel.onPermissionGranted();
        }
    }

    private void showFallDetectDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_fall_detect, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_fall_detect_bg);
        dialog.show();

        android.os.Handler handler = new android.os.Handler();
        Runnable autoLaunch = () -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
                activateEmergencyService();
            }
        };

        handler.postDelayed(autoLaunch, 10_000);

        Button cancelBtn = dialogView.findViewById(R.id.fall_detect_cancel_btn);
        Button confirmBtn = dialogView.findViewById(R.id.fall_detect_confirm_btn);

        cancelBtn.setOnClickListener(v -> {
            dialog.dismiss();
        });

        confirmBtn.setOnClickListener(v -> {
            dialog.dismiss();
            activateEmergencyService();
        });
    }

    private void activateEmergencyService() {
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        smsIntent.setData(Uri.parse("smsto:000"));
        smsIntent.putExtra("sms_body", String.format(
                getString(R.string.emergency_sms_body),
                temperature,
                lightLevel
        ));
        startActivity(smsIntent);
    }

    private void detectLocationAndSetLanguage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    101);
            return;
        }

        android.location.LocationManager locationManager =
                (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);

        android.location.Location location = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
        if(location != null){
            setLanguageByLocation(location.getLatitude(), location.getLongitude());
        } else {
            LocaleHelper.setLocale(this, LocaleHelper.LANGUAGE_ENGLISH);
        }
    }

    private void setLanguageByLocation(double latitude, double longitude){
        boolean isInIndonesia = latitude >= -11.0 && latitude <= 6.0 && longitude >= 95.0 && longitude <= 141.0;

        if(isInIndonesia){
            LocaleHelper.setLocale(this, LocaleHelper.LANGUAGE_INDONESIAN);
        } else {
            LocaleHelper.setLocale(this, LocaleHelper.LANGUAGE_ENGLISH);
        }
    }

}
