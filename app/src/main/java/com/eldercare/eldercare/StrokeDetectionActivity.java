// app/src/main/java/com/eldercare/eldercare/StrokeDetectionActivity.java
package com.eldercare.eldercare;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class StrokeDetectionActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Button captureButton;
    private Button analyzeButton;
    private Button menuButton;
    private TextView statusTextView;
    private TextView titleText;
    private ProgressBar progressBar;
    private FrameLayout cameraPreview;

    private List<Bitmap> capturedFaces = new ArrayList<>();
    private static final int REQUIRED_IMAGES = 3;
    private FacialAsymmetryDetector asymmetryDetector;
    private int captureCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stroke_detection);

        // Setup the title colors first
        setupTitleColors();

        // Initialize all views and functionality
        initializeViews();
        asymmetryDetector = new FacialAsymmetryDetector();

        // Check and request camera permissions
        if (checkCameraPermission()) {
            setupCamera();
        } else {
            requestCameraPermission();
        }
    }

    private void setupTitleColors() {
        titleText = findViewById(R.id.title_text);
        SpannableString spannableString = new SpannableString("ElderCare");

        // Set "Elder" to #8C2E2E
        spannableString.setSpan(
                new ForegroundColorSpan(Color.parseColor("#8C2E2E")),
                0, 5,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // Set "Care" to #284259
        spannableString.setSpan(
                new ForegroundColorSpan(Color.parseColor("#284259")),
                5, 9,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        titleText.setText(spannableString);
    }

    private void initializeViews() {
        // Initialize all views
        cameraPreview = findViewById(R.id.camera_preview);
        surfaceView = findViewById(R.id.surface_view);
        captureButton = findViewById(R.id.capture_button);
        statusTextView = findViewById(R.id.status_text);
        progressBar = findViewById(R.id.progress_bar);
        menuButton = findViewById(R.id.menu_button);

        // Setup surface holder for camera
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // Setup button click listeners
        captureButton.setOnClickListener(v -> captureImage());

        analyzeButton.setOnClickListener(v -> analyzeImages());
        analyzeButton.setEnabled(false);
        analyzeButton.setVisibility(View.GONE);

        // Menu button - goes back to MainActivity
        menuButton.setOnClickListener(v -> finish());

        // Initially hide progress bar
        progressBar.setVisibility(View.GONE);
        statusTextView.setVisibility(View.GONE);
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera();
            } else {
                Toast.makeText(this, "Camera permission is required for stroke detection",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void setupCamera() {
        try {
            // Use front-facing camera for facial detection
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            Camera.Parameters params = camera.getParameters();

            // Set camera parameters
            if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            // Set preview size if needed
            Camera.Size previewSize = getOptimalPreviewSize(params.getSupportedPreviewSizes());
            if (previewSize != null) {
                params.setPreviewSize(previewSize.width, previewSize.height);
            }

            camera.setParameters(params);
            camera.setDisplayOrientation(90);

        } catch (Exception e) {
            Toast.makeText(this, "Error accessing camera: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes) {
        if (sizes == null) return null;

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) surfaceView.getHeight() / surfaceView.getWidth();
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - surfaceView.getHeight()) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - surfaceView.getHeight());
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - surfaceView.getHeight()) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - surfaceView.getHeight());
                }
            }
        }
        return optimalSize;
    }

    private void captureImage() {
        if (camera != null) {
            // Show brief toast for user feedback
            captureCount++;
            Toast.makeText(this, "Capturing image " + captureCount + "/" + REQUIRED_IMAGES,
                    Toast.LENGTH_SHORT).show();

            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    capturedFaces.add(bitmap);
                    updateStatus();
                    camera.startPreview();
                }
            });
        }
    }

    private void updateStatus() {
        int captured = capturedFaces.size();

        if (captured > 0) {
            statusTextView.setVisibility(View.VISIBLE);
            statusTextView.setText("Captured " + captured + "/" + REQUIRED_IMAGES + " images");
        }

        if (captured >= REQUIRED_IMAGES) {
            // Switch buttons
            captureButton.setVisibility(View.GONE);
            analyzeButton.setVisibility(View.VISIBLE);
            analyzeButton.setEnabled(true);
            statusTextView.setText("Ready for analysis!");

            // Show a toast
            Toast.makeText(this, "All images captured! Tap 'Analyze' to check for asymmetry",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void analyzeImages() {
        progressBar.setVisibility(View.VISIBLE);
        analyzeButton.setEnabled(false);
        statusTextView.setText("Analyzing facial symmetry...");

        // Simulate processing delay for demo
        new Handler().postDelayed(() -> {
            AsymmetryResult result = asymmetryDetector.analyzeImages(capturedFaces);
            progressBar.setVisibility(View.GONE);
            showResults(result);
        }, 3000);
    }

    private void showResults(AsymmetryResult result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Set title based on results
        if (result.isAsymmetryDetected()) {
            builder.setTitle("⚠️ Asymmetry Detected");
            builder.setIcon(android.R.drawable.ic_dialog_alert);
        } else {
            builder.setTitle("✓ Normal Results");
            builder.setIcon(android.R.drawable.ic_dialog_info);
        }

        // Build the message
        String message = "Analysis Complete\n\n" +
                "Asymmetry Score: " + String.format("%.1f", result.getAsymmetryScore()) + "%\n" +
                "Risk Level: " + result.getRiskLevel() + "\n\n";

        if (result.isAsymmetryDetected()) {
            message += "⚠️ Recommendations:\n" +
                    "• Perform FAST test (Face, Arms, Speech, Time)\n" +
                    "• Consider consulting a healthcare provider\n" +
                    "• Monitor for additional symptoms\n" +
                    "• Save these results for medical consultation";
        } else {
            message += "✅ Good News!\n" +
                    "No significant facial asymmetry detected.\n" +
                    "Continue regular monitoring for preventive care.\n" +
                    "Recommended to check monthly.";
        }

        builder.setMessage(message);

        // Add buttons
        builder.setPositiveButton("OK", (dialog, which) -> resetDetection());

        if (result.isAsymmetryDetected()) {
            builder.setNegativeButton("Emergency Contact", (dialog, which) -> {
                // Mock emergency contact for demo
                simulateEmergencyContact();
            });

            builder.setNeutralButton("Save Results", (dialog, which) -> {
                Toast.makeText(this, "Results saved to health records (Demo)",
                        Toast.LENGTH_LONG).show();
                resetDetection();
            });
        }

        builder.setCancelable(false);
        builder.show();
    }

    private void simulateEmergencyContact() {
        // Show emergency contact simulation
        AlertDialog.Builder emergencyBuilder = new AlertDialog.Builder(this);
        emergencyBuilder.setTitle("🚨 Emergency Protocol Activated");
        emergencyBuilder.setMessage(
                "Demo Mode - In real app:\n\n" +
                        "• GPS location would be shared\n" +
                        "• Emergency contacts would be notified\n" +
                        "• Nearest hospital would be contacted\n" +
                        "• Medical history would be transmitted\n\n" +
                        "Current Location: [GPS Coordinates]\n" +
                        "Nearest Hospital: 2.3 miles away"
        );
        emergencyBuilder.setPositiveButton("Understood", (dialog, which) -> resetDetection());
        emergencyBuilder.show();
    }

    private void resetDetection() {
        // Clear captured images and reset UI
        capturedFaces.clear();
        captureCount = 0;

        // Reset buttons
        captureButton.setVisibility(View.VISIBLE);
        captureButton.setEnabled(true);
        analyzeButton.setVisibility(View.GONE);
        analyzeButton.setEnabled(false);

        // Reset status text
        statusTextView.setText("");
        statusTextView.setVisibility(View.GONE);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        try {
            if (camera != null) {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        if (surfaceHolder.getSurface() == null) {
            return;
        }

        try {
            camera.stopPreview();
        } catch (Exception e) {
            // Ignore: tried to stop a non-existent preview
        }

        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview
        if (camera != null) {
            camera.stopPreview();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (surfaceView != null && checkCameraPermission()) {
            setupCamera();
            if (surfaceHolder != null && camera != null) {
                try {
                    camera.setPreviewDisplay(surfaceHolder);
                    camera.startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }
}