package com.eldercare.eldercare.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import com.eldercare.eldercare.R;
import com.eldercare.eldercare.databinding.ActivityFaceScanBinding;
import com.eldercare.eldercare.model.FaceScanData;
import com.eldercare.eldercare.network.RetrofitClient;
import com.eldercare.eldercare.repository.FaceScanRepository.ApiResponse;
import com.eldercare.eldercare.utils.FaceOverlayView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class FaceScanActivity extends AppCompatActivity {
    private ActivityFaceScanBinding binding;
    private static final int CAMERA_PERMISSION_CODE = 100;

    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private boolean isFrontCamera = true;
    private boolean isScanning = false;
    private boolean faceDetected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_face_scan);

        setupViews();
        checkCameraPermission();
    }

    private void setupViews() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnSwitchCamera.setOnClickListener(v -> {
            isFrontCamera = !isFrontCamera;
            closeCamera();
            startCamera();
        });

        binding.btnStartScan.setOnClickListener(v -> {
            if (!isScanning) {
                captureImage();
            }
        });

        binding.btnViewResult.setOnClickListener(v -> {
            Toast.makeText(this, "View result clicked", Toast.LENGTH_SHORT).show();
        });

        binding.btnUpload.setOnClickListener(v -> {
            Toast.makeText(this, "Upload clicked", Toast.LENGTH_SHORT).show();
        });

        binding.textureView.setSurfaceTextureListener(textureListener);

        simulateFaceDetection();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            startCamera();
        }
    }

    private void startCamera() {
        if (binding.textureView.isAvailable()) {
            openCamera();
        } else {
            binding.textureView.setSurfaceTextureListener(textureListener);
        }
    }

    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
    };

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = getCameraId(manager);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            imageDimension = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(SurfaceTexture.class)[0];

            Size[] jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(ImageFormat.JPEG);
            int width = 640;
            int height = 480;
            if (jpegSizes != null && jpegSizes.length > 0) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }

            if (imageReader != null) {
                imageReader.close();
            }

            imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image == null) {
                        runOnUiThread(() -> {
                            binding.btnStartScan.setEnabled(true);
                            Toast.makeText(FaceScanActivity.this, "Failed to get image", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);

                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bitmap == null) {
                        runOnUiThread(() -> {
                            binding.btnStartScan.setEnabled(true);
                            Toast.makeText(FaceScanActivity.this, "Failed to decode image", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    if (isFrontCamera) {
                        bitmap = flipBitmap(bitmap);
                    }

                    final Bitmap finalBitmap = bitmap;
                    runOnUiThread(() -> startProcessingAnimation(finalBitmap));

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        binding.btnStartScan.setEnabled(true);
                        Toast.makeText(FaceScanActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                } finally {
                    if (image != null) {
                        image.close();
                    }
                }
            }, backgroundHandler);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            manager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private String getCameraId(CameraManager manager) throws CameraAccessException {
        for (String cameraId : manager.getCameraIdList()) {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

            if (isFrontCamera && facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return cameraId;
            } else if (!isFrontCamera && facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId;
            }
        }
        return manager.getCameraIdList()[0];
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private void createCameraPreview() {
        try {
            SurfaceTexture texture = binding.textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());

            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) return;

                    cameraCaptureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(FaceScanActivity.this, "Configuration failed", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null) return;

        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureImage() {
        if (cameraDevice == null) return;

        try {
            binding.btnStartScan.setEnabled(false);
            binding.tvCameraStatus.setText("Capturing...");

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            cameraCaptureSession.stopRepeating();
            cameraCaptureSession.abortCaptures();

            cameraCaptureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    runOnUiThread(() -> {
                        binding.tvCameraStatus.setText("Image captured!");
                    });
                }

                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session,
                                            @NonNull CaptureRequest request,
                                            @NonNull CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                    runOnUiThread(() -> {
                        binding.btnStartScan.setEnabled(true);
                        binding.tvCameraStatus.setText("Capture failed, try again");
                        Toast.makeText(FaceScanActivity.this, "Capture failed", Toast.LENGTH_SHORT).show();
                    });
                }
            }, backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
            binding.btnStartScan.setEnabled(true);
            binding.tvCameraStatus.setText("Error occurred");
        }
    }

    private Bitmap flipBitmap(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.preScale(-1.0f, 1.0f);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private int getOrientation(int rotation) {
        switch (rotation) {
            case Surface.ROTATION_0:
                return isFrontCamera ? 270 : 90;
            case Surface.ROTATION_90:
                return 0;
            case Surface.ROTATION_180:
                return isFrontCamera ? 90 : 270;
            case Surface.ROTATION_270:
                return 180;
            default:
                return 90;
        }
    }

    private void startProcessingAnimation(Bitmap capturedImage) {
        isScanning = true;
        binding.btnStartScan.setEnabled(false);
        binding.loadingProgress.setVisibility(View.VISIBLE);
        binding.tvCameraStatus.setText("Processing face data...");
        binding.tvScanMethod.setText("ANALYZING");

        binding.faceOverlay.startScanAnimation();

        new Handler().postDelayed(() -> {
            binding.faceOverlay.stopScanAnimation();
            startSendingAnimation(capturedImage);
        }, 2500);
    }

    private void startSendingAnimation(Bitmap capturedImage) {
        binding.tvCameraStatus.setText("Sending data to server...");
        binding.tvScanMethod.setText("UPLOADING");
        binding.loadingProgress.setVisibility(View.VISIBLE);

        new Handler().postDelayed(() -> {
            uploadFaceScan(capturedImage);
        }, 1500);
    }

    private void uploadFaceScan(Bitmap bitmap) {
        String base64Image = bitmapToBase64(bitmap);

        FaceScanData faceScanData = new FaceScanData();
        faceScanData.setImage(base64Image);
        faceScanData.setTimestamp(System.currentTimeMillis());

        RetrofitClient.getInstance().getApiService()
                .uploadFaceScan(faceScanData)
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        isScanning = false;
                        binding.loadingProgress.setVisibility(View.GONE);

                        Intent intent = new Intent(FaceScanActivity.this, FaceScanResultActivity.class);

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse result = response.body();
                            intent.putExtra("success", true);
                            intent.putExtra("healthy", result.isHealthy());
                            intent.putExtra("message", result.getMessage());
                            intent.putExtra("confidence", result.getConfidence());
                            if (result.getConditions() != null) {
                                intent.putStringArrayListExtra("conditions", new ArrayList<>(result.getConditions()));
                            }
                            if (result.getRecommendations() != null) {
                                intent.putStringArrayListExtra("recommendations", new ArrayList<>(result.getRecommendations()));
                            }
                        } else {
                            intent.putExtra("success", false);
                            intent.putExtra("error", "Server returned error code: " + response.code());
                        }

                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse> call, Throwable t) {
                        isScanning = false;
                        binding.loadingProgress.setVisibility(View.GONE);

                        Intent intent = new Intent(FaceScanActivity.this, FaceScanResultActivity.class);
                        intent.putExtra("success", false);

                        String errorMsg = "Connection error";
                        if (t.getMessage() != null) {
                            if (t.getMessage().contains("Unable to resolve host") ||
                                    t.getMessage().contains("Failed to connect")) {
                                errorMsg = "Cannot connect to server. Please check:\n• Backend server is running\n• Network connection\n• IP address is correct";
                            } else if (t.getMessage().contains("timeout")) {
                                errorMsg = "Connection timeout. Server might be slow or unreachable.";
                            } else {
                                errorMsg = t.getMessage();
                            }
                        }

                        intent.putExtra("error", errorMsg);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void simulateFaceDetection() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isScanning) {
                    faceDetected = !faceDetected;

                    if (faceDetected) {
                        binding.tvFaceDetected.setVisibility(View.VISIBLE);
                        binding.tvScanMethod.setText("FACE LOCK");
                        binding.faceOverlay.setFaceDetected(true);

                        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                        fadeIn.setDuration(300);
                        binding.tvFaceDetected.startAnimation(fadeIn);
                    } else {
                        binding.tvFaceDetected.setVisibility(View.GONE);
                        binding.tvScanMethod.setText("DETECTING...");
                        binding.faceOverlay.setFaceDetected(false);
                    }
                }

                new Handler().postDelayed(this, 2000);
            }
        }, 1000);
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (binding.textureView.isAvailable()) {
            openCamera();
        } else {
            binding.textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("Camera Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}