package com.eldercare.eldercare.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.eldercare.eldercare.utils.FaceOverlayView;
import com.eldercare.eldercare.utils.FaceMeshConnections;
import com.eldercare.eldercare.utils.FaceProcessorFactory;
import com.eldercare.eldercare.model.FaceScanData;
import com.eldercare.eldercare.utils.DeviceCapabilityChecker;
import com.eldercare.eldercare.viewmodel.FaceScanViewModel;
import com.google.ar.core.*;
import com.google.ar.core.exceptions.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.facemesh.FaceMesh;
import com.google.mlkit.vision.facemesh.FaceMeshDetection;
import com.google.mlkit.vision.facemesh.FaceMeshDetector;
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions;
import com.google.mlkit.vision.facemesh.FaceMeshPoint;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    private Bitmap latestFrame;
    private FaceMeshDetector faceMeshDetector;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // Face tracking
    private boolean isFaceDetected = false;
    private long lastTrackingTime = 0;
    private static final long TRACKING_INTERVAL = 100; // Track every 100ms
    private List<int[]> faceConnections;

    // Shared
    private FaceScanData currentScanData;
    private boolean permissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_face_scan);

        viewModel = new ViewModelProvider(this).get(FaceScanViewModel.class);
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(this);

        setupFaceMeshDetector();
        setupObservers();
        setupUI();
        checkCameraPermission();
    }

    private void setupFaceMeshDetector() {
        // Use Face Mesh Detection for 468 3D face points
        FaceMeshDetectorOptions options = new FaceMeshDetectorOptions.Builder()
                .setUseCase(FaceMeshDetectorOptions.FACE_MESH)
                .build();

        faceMeshDetector = FaceMeshDetection.getClient(options);

        // Get face mesh connections for drawing
        faceConnections = FaceMeshConnections.getSimplifiedConnections();

        Log.d(TAG, "Face Mesh Detector initialized with " + faceConnections.size() + " connections");
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

        faceProcessor = FaceProcessorFactory.createFaceProcessor(this,
                new FaceProcessorFactory.UnifiedFaceCallback() {
                    @Override
                    public void onScanMethodDetermined(DeviceCapabilityChecker.ScanMethod method) {
                        scanMethod = method;
                        String description = DeviceCapabilityChecker.getScanMethodDescription(method);

                        runOnUiThread(() -> {
                            binding.tvScanMethod.setText(method.name());
                            Toast.makeText(FaceScanActivity.this, description, Toast.LENGTH_SHORT).show();

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
                            binding.tvCameraStatus.setText("Face detected - " + pointCount + " points");
                        });
                    }

                    @Override
                    public void onFaceProcessingComplete(FaceScanData faceScanData) {
                        runOnUiThread(() -> {
                            currentScanData = faceScanData;
                            viewModel.completeScan();
                            binding.tvCameraStatus.setText("Scan complete!");
                            binding.loadingProgress.setVisibility(View.GONE);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            viewModel.errorOccurred();
                            binding.tvCameraStatus.setText("Error: " + error);
                            binding.loadingProgress.setVisibility(View.GONE);
                            Toast.makeText(FaceScanActivity.this, error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void setupARCoreMode() {
        Log.d(TAG, "Setting up ARCore mode");

        try {
            binding.surfaceView.setVisibility(View.VISIBLE);
            binding.cameraPreview.setVisibility(View.GONE);

            binding.surfaceView.setPreserveEGLContextOnPause(true);
            binding.surfaceView.setEGLContextClientVersion(2);
            binding.surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            binding.surfaceView.setRenderer(this);
            binding.surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

            setupARSession();

        } catch (Exception e) {
            Log.e(TAG, "Failed to setup ARCore mode", e);
            Toast.makeText(this,
                    "ARCore setup failed. Switching to ML Kit mode.",
                    Toast.LENGTH_LONG).show();

            scanMethod = DeviceCapabilityChecker.ScanMethod.MLKIT_FACE_MESH;
            binding.tvScanMethod.setText("MLKIT_FACE_MESH");
            setupMLKitMode();
        }
    }

    private void setupMLKitMode() {
        Log.d(TAG, "Setting up ML Kit mode");

        binding.surfaceView.setVisibility(View.GONE);
        binding.cameraPreview.setVisibility(View.VISIBLE);

        TextureView textureView = binding.textureView;

        cameraHandler = new CameraHandler(this, textureView, new CameraHandler.CameraCallback() {
            @Override
            public void onImageCaptured(Bitmap bitmap) {
                latestFrame = bitmap;

                // Perform real-time face tracking
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastTrackingTime > TRACKING_INTERVAL) {
                    lastTrackingTime = currentTime;
                    trackFaceInRealtime(bitmap);
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

        // Configure overlay
        binding.faceOverlay.setFrontCamera(true);

        cameraHandler.startCamera(true); // Start with front camera
        binding.tvCameraStatus.setText("Ready to capture");

        Log.d(TAG, "ML Kit mode setup complete - camera started");
    }

    private void trackFaceInRealtime(Bitmap bitmap) {
        if (bitmap == null) return;

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        faceMeshDetector.process(image)
                .addOnSuccessListener(faceMeshes -> {
                    if (!faceMeshes.isEmpty()) {
                        if (!isFaceDetected) {
                            isFaceDetected = true;
                            mainHandler.post(() -> {
                                binding.tvFaceDetected.setVisibility(View.VISIBLE);
                            });
                        }

                        // Draw face mesh on overlay
                        FaceMesh faceMesh = faceMeshes.get(0);
                        drawFaceMesh(faceMesh, bitmap.getWidth(), bitmap.getHeight());
                    } else {
                        if (isFaceDetected) {
                            isFaceDetected = false;
                            mainHandler.post(() -> {
                                binding.tvFaceDetected.setVisibility(View.GONE);
                                binding.faceOverlay.clear();
                            });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Face mesh detection failed", e);
                });
    }

    private void drawFaceMesh(FaceMesh faceMesh, int imageWidth, int imageHeight) {
        List<PointF> points = new ArrayList<>();
        List<FaceOverlayView.Line> lines = new ArrayList<>();

        // Get all face mesh points (468 points)
        List<FaceMeshPoint> allPoints = faceMesh.getAllPoints();

        // Convert PointF3D to PointF (we only need x, y for 2D display)
        for (FaceMeshPoint point : allPoints) {
            PointF3D position3D = point.getPosition();
            // Convert PointF3D to PointF by taking only x and y coordinates
            PointF position2D = new PointF(position3D.getX(), position3D.getY());
            points.add(position2D);
        }

        // Create lines between connected points
        for (int[] connection : faceConnections) {
            if (connection.length == 2) {
                int startIdx = connection[0];
                int endIdx = connection[1];

                if (startIdx < allPoints.size() && endIdx < allPoints.size()) {
                    PointF3D start3D = allPoints.get(startIdx).getPosition();
                    PointF3D end3D = allPoints.get(endIdx).getPosition();

                    // Convert to 2D points
                    PointF start = new PointF(start3D.getX(), start3D.getY());
                    PointF end = new PointF(end3D.getX(), end3D.getY());

                    lines.add(new FaceOverlayView.Line(start, end));
                }
            }
        }

        // Update overlay on main thread
        mainHandler.post(() -> {
            binding.faceOverlay.setImageDimensions(imageWidth, imageHeight);
            binding.faceOverlay.setFrontCamera(cameraHandler.isFrontCamera());
            binding.faceOverlay.setFaceData(points, lines);
        });

        Log.d(TAG, "Drew " + points.size() + " points and " + lines.size() + " lines");
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

                try {
                    arSession.configure(config);
                    Log.d(TAG, "ARCore session setup successful with MESH3D");
                } catch (UnsupportedConfigurationException e) {
                    Log.w(TAG, "Augmented Faces not supported, falling back to ML Kit", e);

                    if (arSession != null) {
                        arSession.close();
                        arSession = null;
                    }

                    runOnUiThread(() -> {
                        Toast.makeText(this,
                                "ARCore face tracking not supported. Using ML Kit instead.",
                                Toast.LENGTH_LONG).show();

                        scanMethod = DeviceCapabilityChecker.ScanMethod.MLKIT_FACE_MESH;
                        binding.tvScanMethod.setText("MLKIT_FACE_MESH");
                        setupMLKitMode();
                    });
                    return;
                }

            } catch (UnavailableArcoreNotInstalledException
                     | UnavailableUserDeclinedInstallationException e) {
                Log.e(TAG, "ARCore not available", e);
                Toast.makeText(this, "ARCore not available, using ML Kit", Toast.LENGTH_LONG).show();
                scanMethod = DeviceCapabilityChecker.ScanMethod.MLKIT_FACE_MESH;
                setupMLKitMode();
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
                    binding.loadingProgress.setVisibility(View.GONE);
                    binding.btnStartScan.setEnabled(true);
                    binding.btnStartScan.setText("Take Photo");
                    break;
                case SCANNING:
                    binding.btnStartScan.setEnabled(false);
                    binding.loadingProgress.setVisibility(View.VISIBLE);
                    binding.tvCameraStatus.setText("Processing...");
                    break;
                case PROCESSING:
                    binding.tvCameraStatus.setText("Analyzing face data...");
                    break;
                case COMPLETED:
                    binding.loadingProgress.setVisibility(View.GONE);
                    binding.btnViewResult.setVisibility(View.VISIBLE);
                    binding.btnUpload.setVisibility(View.VISIBLE);
                    binding.btnStartScan.setEnabled(true);
                    binding.tvCameraStatus.setText("Scan completed!");
                    break;
                case ERROR:
                    binding.loadingProgress.setVisibility(View.GONE);
                    binding.btnStartScan.setEnabled(true);
                    binding.tvCameraStatus.setText("Ready to capture");
                    break;
            }
        });

        viewModel.getUploadStatus().observe(this, status -> {
            if (status != null) {
                binding.tvCameraStatus.setText(status);
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
        binding.btnStartScan.setOnClickListener(v -> captureAndProcessFace());
        binding.btnViewResult.setOnClickListener(v -> viewScanResult());
        binding.btnUpload.setOnClickListener(v -> uploadScanData());
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSwitchCamera.setOnClickListener(v -> switchCamera());
    }

    private void switchCamera() {
        if (cameraHandler != null) {
            cameraHandler.switchCamera();
            boolean isFront = cameraHandler.isFrontCamera();

            // Update overlay for camera orientation
            binding.faceOverlay.setFrontCamera(isFront);

            Toast.makeText(this,
                    isFront ? "Switched to front camera" : "Switched to back camera",
                    Toast.LENGTH_SHORT).show();

            // Clear face overlay when switching
            binding.faceOverlay.clear();
            isFaceDetected = false;
            binding.tvFaceDetected.setVisibility(View.GONE);
        }
    }

    private void captureAndProcessFace() {
        if (!permissionGranted) {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            requestCameraPermission();
            return;
        }

        viewModel.startScanning();
        binding.tvCameraStatus.setText("Capturing...");

        if (scanMethod == DeviceCapabilityChecker.ScanMethod.MLKIT_FACE_MESH) {
            if (latestFrame != null && faceProcessor != null && faceProcessor.getMlkitProcessor() != null) {
                Log.d(TAG, "Processing captured frame");
                faceProcessor.getMlkitProcessor().processImage(latestFrame);
            } else {
                Log.e(TAG, "No frame available or processor is null");
                Toast.makeText(this, "Please wait for camera to initialize", Toast.LENGTH_SHORT).show();
                viewModel.errorOccurred();
            }
        } else if (scanMethod == DeviceCapabilityChecker.ScanMethod.ARCORE) {
            Log.d(TAG, "ARCore face capture triggered");
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

            if (viewModel.getScanState().getValue() == FaceScanViewModel.ScanState.SCANNING) {
                Collection<AugmentedFace> faces = arSession.getAllTrackables(AugmentedFace.class);
                if (!faces.isEmpty() && faceProcessor != null && faceProcessor.getArProcessor() != null) {
                    faceProcessor.getArProcessor().processAugmentedFaces(faces);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onDrawFrame", e);
        }
    }

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
        } else if (scanMethod == DeviceCapabilityChecker.ScanMethod.MLKIT_FACE_MESH) {
            if (cameraHandler != null) {
                cameraHandler.startCamera(cameraHandler.isFrontCamera());
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

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

        if (faceMeshDetector != null) {
            faceMeshDetector.close();
        }
    }
}