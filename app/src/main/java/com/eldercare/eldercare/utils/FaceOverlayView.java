package com.eldercare.eldercare.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FaceOverlayView extends View {
    private Paint meshPaint;
    private Paint scanLinePaint;
    private Paint pointPaint;

    private boolean faceDetected = false;
    private boolean isScanning = false;
    private float scanLineY = 0f;

    private List<FacePoint> facePoints;
    private List<FaceLine> faceLines;
    private Handler animationHandler;
    private Runnable scanAnimationRunnable;

    public FaceOverlayView(Context context) {
        super(context);
        init();
    }

    public FaceOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FaceOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        meshPaint = new Paint();
        meshPaint.setColor(Color.parseColor("#00E5FF"));
        meshPaint.setStyle(Paint.Style.STROKE);
        meshPaint.setStrokeWidth(2.5f);
        meshPaint.setAlpha(150);
        meshPaint.setAntiAlias(true);

        scanLinePaint = new Paint();
        scanLinePaint.setColor(Color.parseColor("#00FF00"));
        scanLinePaint.setStyle(Paint.Style.STROKE);
        scanLinePaint.setStrokeWidth(3f);
        scanLinePaint.setAlpha(200);
        scanLinePaint.setAntiAlias(true);

        pointPaint = new Paint();
        pointPaint.setColor(Color.parseColor("#00E5FF"));
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setAlpha(180);
        pointPaint.setAntiAlias(true);

        facePoints = new ArrayList<>();
        faceLines = new ArrayList<>();
        animationHandler = new Handler();

        generateFaceMesh();
    }

    private void generateFaceMesh() {
        int centerX = 0;
        int centerY = 0;

        float scale = 1.8f;

        facePoints.add(new FacePoint(centerX, (centerY - 120) * scale));
        facePoints.add(new FacePoint((centerX - 60) * scale, (centerY - 100) * scale));
        facePoints.add(new FacePoint((centerX + 60) * scale, (centerY - 100) * scale));
        facePoints.add(new FacePoint((centerX - 80) * scale, (centerY - 60) * scale));
        facePoints.add(new FacePoint((centerX + 80) * scale, (centerY - 60) * scale));
        facePoints.add(new FacePoint((centerX - 90) * scale, centerY));
        facePoints.add(new FacePoint((centerX + 90) * scale, centerY));
        facePoints.add(new FacePoint((centerX - 85) * scale, (centerY + 40) * scale));
        facePoints.add(new FacePoint((centerX + 85) * scale, (centerY + 40) * scale));
        facePoints.add(new FacePoint((centerX - 70) * scale, (centerY + 80) * scale));
        facePoints.add(new FacePoint((centerX + 70) * scale, (centerY + 80) * scale));
        facePoints.add(new FacePoint((centerX - 40) * scale, (centerY + 110) * scale));
        facePoints.add(new FacePoint((centerX + 40) * scale, (centerY + 110) * scale));
        facePoints.add(new FacePoint(centerX, (centerY + 120) * scale));

        facePoints.add(new FacePoint((centerX - 50) * scale, (centerY - 60) * scale));
        facePoints.add(new FacePoint((centerX + 50) * scale, (centerY - 60) * scale));
        facePoints.add(new FacePoint(centerX, (centerY - 20) * scale));
        facePoints.add(new FacePoint(centerX, (centerY + 20) * scale));
        facePoints.add(new FacePoint((centerX - 30) * scale, (centerY + 60) * scale));
        facePoints.add(new FacePoint((centerX + 30) * scale, (centerY + 60) * scale));

        faceLines.add(new FaceLine(0, 1));
        faceLines.add(new FaceLine(0, 2));
        faceLines.add(new FaceLine(1, 3));
        faceLines.add(new FaceLine(2, 4));
        faceLines.add(new FaceLine(3, 5));
        faceLines.add(new FaceLine(4, 6));
        faceLines.add(new FaceLine(5, 7));
        faceLines.add(new FaceLine(6, 8));
        faceLines.add(new FaceLine(7, 9));
        faceLines.add(new FaceLine(8, 10));
        faceLines.add(new FaceLine(9, 11));
        faceLines.add(new FaceLine(10, 12));
        faceLines.add(new FaceLine(11, 13));
        faceLines.add(new FaceLine(12, 13));

        faceLines.add(new FaceLine(14, 16));
        faceLines.add(new FaceLine(15, 16));
        faceLines.add(new FaceLine(16, 17));
        faceLines.add(new FaceLine(17, 18));
        faceLines.add(new FaceLine(17, 19));
        faceLines.add(new FaceLine(18, 11));
        faceLines.add(new FaceLine(19, 12));

        faceLines.add(new FaceLine(3, 14));
        faceLines.add(new FaceLine(4, 15));
    }

    public void setFaceDetected(boolean detected) {
        this.faceDetected = detected;
        invalidate();
    }

    public void startScanAnimation() {
        isScanning = true;
        scanLineY = 0f;

        scanAnimationRunnable = new Runnable() {
            @Override
            public void run() {
                if (isScanning) {
                    scanLineY += 8f;
                    if (scanLineY > getHeight()) {
                        scanLineY = 0f;
                    }
                    invalidate();
                    animationHandler.postDelayed(this, 16);
                }
            }
        };
        animationHandler.post(scanAnimationRunnable);
    }

    public void stopScanAnimation() {
        isScanning = false;
        if (scanAnimationRunnable != null) {
            animationHandler.removeCallbacks(scanAnimationRunnable);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!faceDetected) {
            return;
        }

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        for (FaceLine line : faceLines) {
            FacePoint p1 = facePoints.get(line.startIndex);
            FacePoint p2 = facePoints.get(line.endIndex);

            float x1 = centerX + p1.x;
            float y1 = centerY + p1.y;
            float x2 = centerX + p2.x;
            float y2 = centerY + p2.y;

            canvas.drawLine(x1, y1, x2, y2, meshPaint);
        }

        for (FacePoint point : facePoints) {
            float x = centerX + point.x;
            float y = centerY + point.y;
            canvas.drawCircle(x, y, 5f, pointPaint);
        }

        if (isScanning) {
            canvas.drawLine(0, scanLineY, getWidth(), scanLineY, scanLinePaint);

            scanLinePaint.setAlpha((int)(100 + Math.abs(Math.sin(scanLineY / 50) * 100)));
        }
    }

    private static class FacePoint {
        float x, y;

        FacePoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class FaceLine {
        int startIndex, endIndex;

        FaceLine(int start, int end) {
            this.startIndex = start;
            this.endIndex = end;
        }
    }
}