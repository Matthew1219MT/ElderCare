package com.eldercare.eldercare.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import com.eldercare.eldercare.R;
import com.eldercare.eldercare.utils.CameraHandler;
import com.eldercare.eldercare.databinding.ActivityFaceScanBinding;
import com.eldercare.eldercare.utils.FaceProcessorFactory;
import com.eldercare.eldercare.model.FaceScanData;
import com.eldercare.eldercare.utils.DeviceCapabilityChecker;
import com.eldercare.eldercare.viewmodel.FaceScanViewModel;
import com.google.ar.core.*;
import com.google.ar.core.exceptions.*;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.Collection;

public class FaceScanActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = "FaceScanActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private ActivityFaceScanBinding binding;
    private FaceScanViewModel viewModel;
    private FaceProcessorFactory.FaceProcessorWrapper faceProcessor;
    private DeviceCapabilityChecker.ScanMethod scanMethod;

    // ARCore specific
    private Session arSession;
    private boolean installRequested;

    // ML Kit specific
    private CameraHandler cameraHandler;
    private long lastProcessTime = 0;
    private static final long PROCESS_INTERVAL = 500; // Process every 500ms

    // Shared
    private FaceScanData currentScanData;
    private boolean isScanning = false;
    private boolean permissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_face_scan);

        viewModel = new ViewModelProvider(this).get(FaceScanViewModel.class);
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(this);

        setupObservers();
        setupUI();
        checkCameraPermission();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            permissionGranted = true;
            initializeFaceScanning();
        } else {
            requestCameraPermission();
        }
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            // Show explanation dialog
            new AlertDialog.Builder(this)
                    .setTitle("Camera Permission Required")
                    .setMessage("This app needs camera access to scan your face. Please grant camera permission to continue.")
                    .setPositiveButton("Grant Permission", (dialog, which) -> {
                        ActivityCompat.requestPermissions(FaceScanActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                CAMERA_PERMISSION_REQUEST_CODE);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        Toast.makeText(FaceScanActivity.this,
                                "Camera permission is required for face scanning",
                                Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .create()
                    .show();
        } else {
            // Request permission directly
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionGranted = true;
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
                initializeFaceScanning();
            } else {
                permissionGranted = false;
                Toast.makeText(this,
                        "Camera permission is required for face scanning. Please enable it in settings.",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initializeFaceScanning() {
        if (!permissionGranted) {
            Log.e(TAG, "Cannot initialize - permission not granted");
            return;
        }

        // Create unified face processor that detects device capability
        faceProcessor = FaceProcessorFactory.createFaceProcessor(this,
                new FaceProcessorFactory.UnifiedFaceCallback() {
                    @Override
                    public void onScanMethodDetermined(DeviceCapabilityChecker.ScanMethod method) {
                        scanMethod = method;
                        String description = DeviceCapabilityChecker.getScanMethodDescription(method);

                        runOnUiThread(() -> {
                            // Show scan method info
                            binding.tvScanMethod.setText(method.name());
                            Toast.makeText(FaceScanActivity.this, description, Toast.LENGTH_LONG).show();

                            // Setup appropriate UI based on scan method
                            if (method == DeviceCapabilityChecker.ScanMethod.ARCORE) {
                                setupARCoreMode();
                            } else if (method == DeviceCapabilityChecker.ScanMethod.MLKIT_FACE_MESH) {
                                setupMLKitMode();
                            } else if (method == DeviceCapabilityChecker.ScanMethod.NONE) {
                                Toast.makeText(FaceScanActivity.this,
                                        "Face scanning not supported on this device",
                                        Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }

                    @Override
                    public void onFaceDetected(FaceScanData faceScanData) {
                        runOnUiThread(() -> {
                            currentScanData = faceScanData;
                            viewModel.processScanData(faceScanData);
                            int pointCount = faceScanData.getPoints() != null ?
                                    faceScanData.getPoints().size() : 0;
                            binding.tvScanStatus.setText("Face detected - " + pointCount + " points");
                        });
                    }

                    @Override
                    public void onFaceProcessingComplete(FaceScanData faceScanData) {
                        runOnUiThread(() -> {
                            currentScanData = faceScanData;
                            viewModel.completeScan();
                            binding.tvScanStatus.setText("Scan complete!");
                            stopScanning();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            viewModel.errorOccurred();
                            binding.tvScanStatus.setText("Error: " + error);
                            Toast.makeText(FaceScanActivity.this, error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void setupARCoreMode() {
        Log.d(TAG, "Setting up ARCore mode");

        // Show ARCore surface view
        binding.surfaceView.setVisibility(View.VISIBLE);
        binding.cameraPreview.setVisibility(View.GONE);

        // Setup GLSurfaceView for ARCore
        binding.surfaceView.setPreserveEGLContextOnPause(true);
        binding.surfaceView.setEGLContextClientVersion(2);
        binding.surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        binding.surfaceView.setRenderer(this);
        binding.surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Setup AR Session
        setupARSession();
    }

    private void setupMLKitMode() {
        Log.d(TAG, "Setting up ML Kit mode");

        // Hide ARCore surface view, show camera preview container
        binding.surfaceView.setVisibility(View.GONE);
        binding.cameraPreview.setVisibility(View.VISIBLE);

        // Get the TextureView for camera preview
        TextureView textureView = binding.textureView;

        // Setup camera handler for ML Kit with preview
        cameraHandler = new CameraHandler(this, textureView, new CameraHandler.CameraCallback() {
            @Override
            public void onImageCaptured(Bitmap bitmap) {
                // Throttle processing to avoid overwhelming the processor
                long currentTime = System.currentTimeMillis();
                if (isScanning && currentTime - lastProcessTime > PROCESS_INTERVAL) {
                    lastProcessTime = currentTime;

                    if (faceProcessor != null && faceProcessor.getMlkitProcessor() != null) {
                        faceProcessor.getMlkitProcessor().processImage(bitmap);
                    }
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(FaceScanActivity.this, error, Toast.LENGTH_SHORT).show();
                    viewModel.errorOccurred();
                });
            }
        });

        Log.d(TAG, "ML Kit mode setup complete");
    }

    private void setupARSession() {
        if (arSession == null) {
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                arSession = new Session(this);

                Config config = new Config(arSession);
                config.setAugmentedFaceMode(Config.AugmentedFaceMode.MESH3D);
                arSession.configure(config);

                Log.d(TAG, "ARCore session setup successful");

            } catch (UnavailableArcoreNotInstalledException
                     | UnavailableUserDeclinedInstallationException e) {
                Log.e(TAG, "ARCore not available", e);
                Toast.makeText(this, "ARCore is required but not available", Toast.LENGTH_LONG).show();
                finish();
            } catch (UnavailableApkTooOldException e) {
                Toast.makeText(this, "Please update ARCore", Toast.LENGTH_LONG).show();
                finish();
            } catch (UnavailableSdkTooOldException e) {
                Toast.makeText(this, "Please update this app", Toast.LENGTH_LONG).show();
                finish();
            } catch (Exception e) {
                Log.e(TAG, "Error setting up AR session", e);
                Toast.makeText(this, "Failed to setup AR session", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void setupObservers() {
        viewModel.getScanState().observe(this, state -> {
            switch (state) {
                case IDLE:
                    binding.btnStartScan.setVisibility(View.VISIBLE);
                    binding.btnViewResult.setVisibility(View.GONE);
                    binding.btnUpload.setVisibility(View.GONE);
                    binding.progressBar.setVisibility(View.GONE);
                    break;
                case SCANNING:
                    binding.btnStartScan.setVisibility(View.GONE);
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.tvScanStatus.setText("Scanning face...");
                    break;
                case PROCESSING:
                    binding.tvScanStatus.setText("Processing scan data...");
                    break;
                case COMPLETED:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnViewResult.setVisibility(View.VISIBLE);
                    binding.btnUpload.setVisibility(View.VISIBLE);
                    binding.tvScanStatus.setText("Scan completed successfully!");
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnStartScan.setVisibility(View.VISIBLE);
                    break;
            }
        });

        viewModel.getUploadStatus().observe(this, status -> {
            if (status != null) {
                binding.tvScanStatus.setText(status);
                if (status.contains("successful")) {
                    Toast.makeText(this, "Face scan uploaded successfully!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupUI() {
        binding.btnStartScan.setOnClickListener(v -> startFaceScan());
        binding.btnViewResult.setOnClickListener(v -> viewScanResult());
        binding.btnUpload.setOnClickListener(v -> uploadScanData());
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void startFaceScan() {
        if (!permissionGranted) {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            requestCameraPermission();
            return;
        }

        viewModel.startScanning();
        isScanning = true;

        if (scanMethod == DeviceCapabilityChecker.ScanMethod.ARCORE) {
            // Start ARCore scanning
            if (arSession != null) {
                try {
                    arSession.resume();
                    Log.d(TAG, "ARCore scanning started");
                } catch (CameraNotAvailableException e) {
                    Log.e(TAG, "Camera not available", e);
                    viewModel.errorOccurred();
                }
            }
        } else if (scanMethod == DeviceCapabilityChecker.ScanMethod.MLKIT_FACE_MESH) {
            // Start ML Kit scanning
            if (cameraHandler != null) {
                cameraHandler.startCamera();
                Log.d(TAG, "ML Kit scanning started");
            } else {
                Log.e(TAG, "CameraHandler is null");
                Toast.makeText(this, "Camera not initialized", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopScanning() {
        isScanning = false;

        if (scanMethod == DeviceCapabilityChecker.ScanMethod.ARCORE) {
            if (arSession != null) {
                arSession.pause();
            }
        } else if (scanMethod == DeviceCapabilityChecker.ScanMethod.MLKIT_FACE_MESH) {
            if (cameraHandler != null) {
                cameraHandler.stopCamera();
            }
        }
    }

    private void viewScanResult() {
        if (currentScanData != null) {
            Intent intent = new Intent(this, ScanResultActivity.class);
            intent.putExtra("scan_data", (Parcelable) currentScanData);
            startActivity(intent);
        }
    }

    private void uploadScanData() {
        if (currentScanData != null) {
            viewModel.uploadScanData(currentScanData);
        } else {
            Toast.makeText(this, "No scan data available", Toast.LENGTH_SHORT).show();
        }
    }

    // ========== GLSurfaceView.Renderer methods (for ARCore only) ==========

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        try {
            if (arSession != null) {
                arSession.setCameraTextureName(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to set camera texture name", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        if (arSession != null) {
            arSession.setDisplayGeometry(getWindowManager().getDefaultDisplay().getRotation(), width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (arSession == null || scanMethod != DeviceCapabilityChecker.ScanMethod.ARCORE) {
            return;
        }

        try {
            arSession.setCameraTextureName(0);
            Frame frame = arSession.update();

            if (isScanning) {
                Collection<AugmentedFace> faces = arSession.getAllTrackables(AugmentedFace.class);
                if (!faces.isEmpty() && faceProcessor != null && faceProcessor.getArProcessor() != null) {
                    faceProcessor.getArProcessor().processAugmentedFaces(faces);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onDrawFrame", e);
        }
    }

    // ========== Lifecycle methods ==========

    @Override
    protected void onResume() {
        super.onResume();

        if (scanMethod == DeviceCapabilityChecker.ScanMethod.ARCORE) {
            if (arSession != null) {
                try {
                    arSession.resume();
                } catch (CameraNotAvailableException e) {
                    Log.e(TAG, "Camera not available", e);
                    arSession = null;
                }
            }
            binding.surfaceView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopScanning();

        if (scanMethod == DeviceCapabilityChecker.ScanMethod.ARCORE) {
            if (arSession != null) {
                arSession.pause();
            }
            binding.surfaceView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (arSession != null) {
            arSession.close();
            arSession = null;
        }

        if (cameraHandler != null) {
            cameraHandler.stopCamera();
        }

        if (faceProcessor != null) {
            faceProcessor.cleanup();
        }
    }
}