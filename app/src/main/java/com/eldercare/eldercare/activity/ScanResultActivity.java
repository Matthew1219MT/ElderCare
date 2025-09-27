package com.eldercare.eldercare.activity;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import com.eldercare.eldercare.R;
import com.eldercare.eldercare.databinding.ActivityScanResultBinding;
import com.eldercare.eldercare.model.FaceScanData;
import com.eldercare.eldercare.renderer.Face3DRenderer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ScanResultActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = "ScanResultActivity";

    private ActivityScanResultBinding binding;
    private FaceScanData scanData;
    private Face3DRenderer faceRenderer;

    // Touch and gesture handling
    private ScaleGestureDetector scaleDetector;
    private float lastTouchX, lastTouchY;
    private float rotationX = 0f, rotationY = 0f;
    private float scaleFactor = 1.0f;

    // Matrices
    private float[] modelMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

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
        setup3DViewer();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

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

        // Update scan info
        binding.tvScanInfo.setText(String.format(
                "Scan ID: %s\nVertices: %d\nTriangles: %d",
                scanData.getId(),
                scanData.getPoints() != null ? scanData.getPoints().size() : 0,
                scanData.getGeometry() != null && scanData.getGeometry().getTriangles() != null
                        ? scanData.getGeometry().getTriangles().size() : 0
        ));
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

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        if (faceRenderer != null) {
            faceRenderer.onSurfaceCreated();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, 5, 0, 0, 0, 0, 1, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Calculate MVP matrix
        float[] tempMatrix = new float[16];
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

        if (faceRenderer != null) {
            faceRenderer.draw(mvpMatrix);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.glSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.glSurfaceView.onPause();
    }
}