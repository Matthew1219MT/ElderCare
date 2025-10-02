package com.eldercare.eldercare.activity;

import android.Manifest;
import android.content.Intent;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import com.eldercare.eldercare.R;
import com.eldercare.eldercare.ar.ARFaceProcessor;
import com.eldercare.eldercare.databinding.ActivityFaceScanBinding;
import com.eldercare.eldercare.model.FaceScanData;
import com.eldercare.eldercare.viewmodel.FaceScanViewModel;
import com.google.ar.core.*;
import com.google.ar.core.exceptions.*;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.Collection;

public class FaceScanActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = "FaceScanActivity";

    private ActivityFaceScanBinding binding;
    private com.eldercare.eldercare.viewmodel.FaceScanViewModel viewModel;
    private ARFaceProcessor faceProcessor;

    private Session arSession;
    private boolean installRequested;
    private FaceScanData currentScanData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_face_scan);

        viewModel = new ViewModelProvider(this).get(FaceScanViewModel.class);
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(this);

        setupFaceProcessor();
        setupObservers();
        setupUI();
        checkPermissions();
    }

    private void setupFaceProcessor() {
        faceProcessor = new ARFaceProcessor(new ARFaceProcessor.FaceProcessorCallback() {
            @Override
            public void onFaceDetected(FaceScanData faceScanData) {
                runOnUiThread(() -> {
                    currentScanData = faceScanData;
                    viewModel.processScanData(faceScanData);
                    binding.tvScanStatus.setText("Face detected - " + faceScanData.getPoints().size() + " points");
                });
            }

            @Override
            public void onFaceProcessingComplete(FaceScanData faceScanData) {
                runOnUiThread(() -> {
                    viewModel.completeScan();
                    binding.tvScanStatus.setText("Scan complete!");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    viewModel.errorOccurred();
                    binding.tvScanStatus.setText("Error: " + error);
                    Toast.makeText(FaceScanActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
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
        binding.surfaceView.setPreserveEGLContextOnPause(true);
        binding.surfaceView.setEGLContextClientVersion(2);
        binding.surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        binding.surfaceView.setRenderer(this);
        binding.surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        binding.btnStartScan.setOnClickListener(v -> startFaceScan());
        binding.btnViewResult.setOnClickListener(v -> viewScanResult());
        binding.btnUpload.setOnClickListener(v -> uploadScanData());
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void checkPermissions() {
        Dexter.withContext(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        setupARSession();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(FaceScanActivity.this,
                                "Camera permission is required for face scanning",
                                Toast.LENGTH_LONG).show();
                        finish();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission,
                                                                   PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
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

            } catch (UnavailableArcoreNotInstalledException
                     | UnavailableUserDeclinedInstallationException e) {
                Toast.makeText(this, "ARCore is required for face scanning", Toast.LENGTH_LONG).show();
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

    private void startFaceScan() {
        viewModel.startScanning();
        if (arSession != null) {
            try {
                arSession.resume();
            } catch (CameraNotAvailableException e) {
                Log.e(TAG, "Camera not available", e);
                viewModel.errorOccurred();
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

        if (arSession == null) {
            return;
        }

        try {
            arSession.setCameraTextureName(0);
            Frame frame = arSession.update();

            Collection<AugmentedFace> faces = arSession.getAllTrackables(AugmentedFace.class);
            if (!faces.isEmpty() && viewModel.getScanState().getValue() == FaceScanViewModel.ScanState.SCANNING) {
                faceProcessor.processAugmentedFaces(faces);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onDrawFrame", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

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

    @Override
    protected void onPause() {
        super.onPause();

        if (arSession != null) {
            arSession.pause();
        }

        binding.surfaceView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (arSession != null) {
            arSession.close();
            arSession = null;
        }
    }
}