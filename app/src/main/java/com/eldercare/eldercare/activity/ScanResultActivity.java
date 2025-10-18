package com.eldercare.eldercare.activity;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import com.eldercare.eldercare.R;
import com.eldercare.eldercare.databinding.ActivityScanResultBinding;
import com.eldercare.eldercare.model.FaceScanData;
import com.eldercare.eldercare.renderer.Face3DRenderer;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.ArrayList;

public class ScanResultActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = "ScanResultActivity";

    private ActivityScanResultBinding binding;
    private FaceScanData scanData;
    private Face3DRenderer faceRenderer;

    // Touch and gesture handling (for 3D mode)
    private ScaleGestureDetector scaleDetector;
    private float lastTouchX, lastTouchY;
    private float rotationX = 0f, rotationY = 0f;
    private float scaleFactor = 1.0f;

    // Matrices (for 3D mode)
    private float[] modelMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

    // Display mode
    private boolean usePieChart = true; // Default to pie chart mode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_scan_result);

        // Get scan data from intent
        scanData = (FaceScanData) getIntent().getSerializableExtra("scan_data");

        if (scanData == null) {
            Log.e(TAG, "No scan data provided");
            finish();
            return;
        }

        setupUI();

        if (usePieChart) {
            setupPieChart();
        } else {
            setup3DViewer();
        }
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Setup rotation controls (hidden by default in pie chart mode)
        if (usePieChart) {
            binding.rotationControls.setVisibility(View.GONE);
            binding.glSurfaceView.setVisibility(View.GONE);
            binding.pieChart.setVisibility(View.VISIBLE);
        } else {
            binding.rotationControls.setVisibility(View.VISIBLE);
            binding.glSurfaceView.setVisibility(View.VISIBLE);
            binding.pieChart.setVisibility(View.GONE);

            setupRotationControls();
        }

        // Update scan info (optional, can be hidden)
        if (scanData != null) {
            String scanInfo = String.format(
                    "Scan ID: %s\nDate: %s\nVertices: %d points",
                    scanData.getId() != null ? scanData.getId() : "N/A",
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date()),
                    scanData.getPoints() != null ? scanData.getPoints().size() : 0
            );
            binding.tvScanInfo.setText(scanInfo);
        }
    }

    private void setupRotationControls() {
        binding.btnRotateLeft.setOnClickListener(v -> {
            rotationY -= 15f;
            updateModelMatrix();
        });

        binding.btnRotateRight.setOnClickListener(v -> {
            rotationY += 15f;
            updateModelMatrix();
        });

        binding.btnRotateUp.setOnClickListener(v -> {
            rotationX -= 15f;
            updateModelMatrix();
        });

        binding.btnRotateDown.setOnClickListener(v -> {
            rotationX += 15f;
            updateModelMatrix();
        });

        binding.btnReset.setOnClickListener(v -> {
            rotationX = 0f;
            rotationY = 0f;
            scaleFactor = 1.0f;
            updateModelMatrix();
        });
    }

    private void setupPieChart() {
        PieChart pieChart = binding.pieChart;

        // Calculate asymmetry score based on scan data
        // Higher score = More asymmetry = Higher risk
        // This is a mock calculation - replace with your actual algorithm
        float asymmetryScore = calculateAsymmetryScore();

        // Create pie chart entries
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(asymmetryScore, "Asymmetry"));
        entries.add(new PieEntry(100 - asymmetryScore, "Symmetry"));

        // Create dataset
        PieDataSet dataSet = new PieDataSet(entries, "");

        // Set colors: #BF5F56 for asymmetry (warning), #284259 for symmetry (normal)
        int[] colors = {
                Color.parseColor("#BF5F56"),  // Asymmetry color (red - warning)
                Color.parseColor("#284259")   // Symmetry color (dark blue - normal)
        };
        dataSet.setColors(colors);

        // Customize dataset
        dataSet.setValueTextSize(0f); // Hide percentage values on slices
        dataSet.setSliceSpace(3f); // Space between slices
        dataSet.setSelectionShift(5f); // Shift when selected

        // Create pie data
        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        // Customize pie chart appearance
        pieChart.getDescription().setEnabled(false); // Hide description
        pieChart.getLegend().setEnabled(false); // Hide legend
        pieChart.setDrawHoleEnabled(true); // Enable center hole
        pieChart.setHoleColor(Color.TRANSPARENT); // Transparent hole
        pieChart.setHoleRadius(45f); // Hole size
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setDrawEntryLabels(false); // Hide entry labels
        pieChart.setRotationEnabled(false); // Disable rotation
        pieChart.setHighlightPerTapEnabled(false); // Disable tap highlight

        // Refresh chart
        pieChart.invalidate();

        // Update asymmetry rating text
        binding.tvSimilarityRating.setText(String.format("%.0f%% Asymmetry Detected", asymmetryScore));

        // Update disease description based on asymmetry score
        updateDiseaseDescription(asymmetryScore);
    }

    private float calculateAsymmetryScore() {
        // Mock calculation based on scan data
        // Higher score = More asymmetry = Higher risk
        // Replace this with your actual facial asymmetry algorithm

        if (scanData == null || scanData.getPoints() == null || scanData.getPoints().isEmpty()) {
            return 15.0f; // Default low asymmetry value
        }

        // Example: Use number of detected points as a rough indicator
        // Fewer points might indicate detection issues = higher risk
        int pointCount = scanData.getPoints().size();

        // Normalize to 5-35% range (most faces have low asymmetry)
        float score = 5 + (pointCount % 31); // Will give values between 5-35

        // You can also add random variation for demo purposes
        // score += (new java.util.Random().nextFloat() * 10) - 5; // ±5% variation

        return Math.min(35f, Math.max(5f, score)); // Clamp between 5-35%
    }

    private void updateDiseaseDescription(float asymmetryScore) {
        String description;

        if (asymmetryScore < 10) {
            // Low asymmetry - Normal/Healthy
            description = "Your facial symmetry analysis shows highly balanced features with minimal asymmetry detected. " +
                    "This is within normal ranges and does not indicate signs of acute neurological conditions such as stroke. " +
                    "However, if you experience sudden facial drooping, numbness, confusion, or difficulty speaking, " +
                    "seek immediate medical attention. This screening tool is not a substitute for professional medical diagnosis.";
        } else if (asymmetryScore < 20) {
            // Mild asymmetry - Monitor
            description = "Your facial symmetry analysis shows balanced features with minor asymmetry detected. " +
                    "This is within normal ranges for most individuals. Mild facial asymmetry is common and typically not a cause for concern. " +
                    "If you notice sudden changes in facial symmetry or experience numbness, weakness, or difficulty speaking, " +
                    "seek immediate medical attention. This tool provides screening information only and is not a medical diagnosis.";
        } else if (asymmetryScore < 30) {
            // Moderate asymmetry - Caution advised
            description = "⚠ Your facial symmetry analysis shows moderate asymmetry detected. While some facial asymmetry is normal, " +
                    "this level warrants attention. If this is a sudden change, it could be a warning sign. " +
                    "Monitor for symptoms such as facial drooping, numbness, confusion, difficulty speaking, or severe headache. " +
                    "If you experience any of these symptoms, seek immediate medical attention as they may indicate stroke or other neurological conditions. " +
                    "We recommend consulting a healthcare professional for evaluation. This screening tool is not a substitute for medical diagnosis.";
        } else {
            // High asymmetry - Urgent medical attention recommended
            description = "⚠️ URGENT: Your facial symmetry analysis shows significant asymmetry. This could indicate a serious condition. " +
                    "Sudden facial asymmetry is a key warning sign of stroke (remember F.A.S.T.: Face drooping, Arm weakness, Speech difficulty, Time to call emergency). " +
                    "If this asymmetry appeared suddenly, or if you experience any of the following symptoms, call emergency services IMMEDIATELY:\n" +
                    "• Facial drooping or numbness\n" +
                    "• Arm or leg weakness\n" +
                    "• Confusion or difficulty speaking\n" +
                    "• Severe headache\n" +
                    "• Vision problems\n\n" +
                    "Even if asymptomatic, we STRONGLY recommend consulting a healthcare professional as soon as possible. " +
                    "Remember: This is a screening tool only and NOT a medical diagnosis, but it may indicate the need for urgent evaluation.";
        }

        binding.tvDiseaseDescription.setText(description);
    }

    private void setup3DViewer() {
        binding.glSurfaceView.setEGLContextClientVersion(2);
        binding.glSurfaceView.setRenderer(this);
        binding.glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        faceRenderer = new Face3DRenderer(scanData);

        // Setup gesture detector for scaling
        scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 3.0f));
                updateModelMatrix();
                return true;
            }
        });

        // Setup touch listener for rotation
        binding.glSurfaceView.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (!scaleDetector.isInProgress()) {
                        float deltaX = event.getX() - lastTouchX;
                        float deltaY = event.getY() - lastTouchY;

                        rotationY += deltaX * 0.5f;
                        rotationX += deltaY * 0.5f;

                        updateModelMatrix();

                        lastTouchX = event.getX();
                        lastTouchY = event.getY();
                    }
                    break;
            }

            return true;
        });

        updateModelMatrix();
    }

    private void updateModelMatrix() {
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.scaleM(modelMatrix, 0, scaleFactor, scaleFactor, scaleFactor);
        Matrix.rotateM(modelMatrix, 0, rotationX, 1, 0, 0);
        Matrix.rotateM(modelMatrix, 0, rotationY, 0, 1, 0);

        binding.glSurfaceView.requestRender();
    }

    // ========== GLSurfaceView.Renderer methods (for 3D mode only) ==========

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if (!usePieChart) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_CULL_FACE);

            if (faceRenderer != null) {
                faceRenderer.onSurfaceCreated();
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (!usePieChart) {
            GLES20.glViewport(0, 0, width, height);

            float ratio = (float) width / height;
            Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
            Matrix.setLookAtM(viewMatrix, 0, 0, 0, 5, 0, 0, 0, 0, 1, 0);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (!usePieChart) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            // Calculate MVP matrix
            float[] tempMatrix = new float[16];
            Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

            if (faceRenderer != null) {
                faceRenderer.draw(mvpMatrix);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!usePieChart) {
            binding.glSurfaceView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!usePieChart) {
            binding.glSurfaceView.onPause();
        }
    }
}