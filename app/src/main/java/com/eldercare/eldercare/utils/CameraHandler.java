package com.eldercare.eldercare.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class CameraHandler {
    private static final String TAG = "CameraHandler";
    private static final int IMAGE_WIDTH = 640;
    private static final int IMAGE_HEIGHT = 480;

    public interface CameraCallback {
        void onImageCaptured(Bitmap bitmap);
        void onError(String error);
    }

    private Context context;
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private CameraCallback callback;
    private Size previewSize;
    private String currentCameraId;
    private boolean isFrontCamera = true;

    public CameraHandler(Context context, TextureView textureView, CameraCallback callback) {
        this.context = context;
        this.textureView = textureView;
        this.callback = callback;
    }

    public void startCamera() {
        startCamera(true); // Default to front camera
    }

    public void startCamera(boolean useFrontCamera) {
        this.isFrontCamera = useFrontCamera;
        startBackgroundThread();

        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    public void switchCamera() {
        Log.d(TAG, "Switching camera");
        stopCamera();
        isFrontCamera = !isFrontCamera;
        startCamera(isFrontCamera);
    }

    public boolean isFrontCamera() {
        return isFrontCamera;
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                    openCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                }

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                }
            };

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        try {
            String cameraId = isFrontCamera ? getFrontCameraId(manager) : getBackCameraId(manager);

            if (cameraId == null) {
                callback.onError("Camera not found");
                return;
            }

            currentCameraId = cameraId;
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            previewSize = new Size(IMAGE_WIDTH, IMAGE_HEIGHT);

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                callback.onError("Camera permission not granted");
                return;
            }

            imageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT,
                    ImageFormat.JPEG, 2);
            imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler);

            manager.openCamera(cameraId, stateCallback, backgroundHandler);
            Log.d(TAG, "Opening camera: " + cameraId + " (Front: " + isFrontCamera + ")");

        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access exception", e);
            callback.onError("Failed to open camera: " + e.getMessage());
        }
    }

    private String getFrontCameraId(CameraManager manager) throws CameraAccessException {
        for (String cameraId : manager.getCameraIdList()) {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return cameraId;
            }
        }
        return null;
    }

    private String getBackCameraId(CameraManager manager) throws CameraAccessException {
        for (String cameraId : manager.getCameraIdList()) {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId;
            }
        }
        return null;
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "Camera opened successfully");
            cameraDevice = camera;
            createCaptureSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, "Camera disconnected");
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "Camera error: " + error);
            camera.close();
            cameraDevice = null;
            callback.onError("Camera error: " + error);
        }
    };

    private void createCaptureSession() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) {
                Log.e(TAG, "SurfaceTexture is null");
                return;
            }

            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface surface = new Surface(texture);
            Surface imageReaderSurface = imageReader.getSurface();

            final CaptureRequest.Builder captureBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureBuilder.addTarget(surface);
            captureBuilder.addTarget(imageReaderSurface);

            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReaderSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (cameraDevice == null) {
                                Log.e(TAG, "Camera device is null in onConfigured");
                                return;
                            }

                            captureSession = session;
                            try {
                                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON);

                                CaptureRequest captureRequest = captureBuilder.build();
                                captureSession.setRepeatingRequest(captureRequest, null, backgroundHandler);
                                Log.d(TAG, "Camera preview started");
                            } catch (CameraAccessException e) {
                                Log.e(TAG, "Error creating capture session", e);
                                callback.onError("Failed to start camera preview");
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "Camera configuration failed");
                            callback.onError("Camera configuration failed");
                        }
                    }, backgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access exception", e);
            callback.onError("Failed to create capture session");
        }
    }

    private final ImageReader.OnImageAvailableListener imageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        if (image != null) {
                            Bitmap bitmap = imageToBitmap(image);
                            if (bitmap != null) {
                                // Mirror front camera images
                                if (isFrontCamera) {
                                    bitmap = flipBitmap(bitmap);
                                }
                                callback.onImageCaptured(bitmap);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing image", e);
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
            };

    private Bitmap imageToBitmap(Image image) {
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error converting image to bitmap", e);
            return null;
        }
    }

    private Bitmap flipBitmap(Bitmap source) {
        Matrix matrix = new Matrix();
        matrix.preScale(-1.0f, 1.0f);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public void stopCamera() {
        Log.d(TAG, "Stopping camera");

        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        stopBackgroundThread();
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping background thread", e);
            }
        }
    }
}