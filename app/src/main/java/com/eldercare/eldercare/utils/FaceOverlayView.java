package com.eldercare.eldercare.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class FaceOverlayView extends View {
    private Paint pointPaint;
    private Paint linePaint;
    private Paint boundingBoxPaint;
    private List<PointF> facePoints = new ArrayList<>();
    private List<Line> faceLines = new ArrayList<>();
    private float imageWidth = 1;
    private float imageHeight = 1;
    private float viewWidth = 1;
    private float viewHeight = 1;
    private boolean isFrontCamera = true;

    public static class Line {
        public PointF start;
        public PointF end;

        public Line(PointF start, PointF end) {
            this.start = start;
            this.end = end;
        }
    }

    public FaceOverlayView(Context context) {
        super(context);
        init();
    }

    public FaceOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        pointPaint = new Paint();
        pointPaint.setColor(Color.GREEN);
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setStrokeWidth(6f);
        pointPaint.setAntiAlias(true);

        linePaint = new Paint();
        linePaint.setColor(Color.CYAN);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f);
        linePaint.setAntiAlias(true);
        linePaint.setAlpha(200);

        boundingBoxPaint = new Paint();
        boundingBoxPaint.setColor(Color.YELLOW);
        boundingBoxPaint.setStyle(Paint.Style.STROKE);
        boundingBoxPaint.setStrokeWidth(4f);
        boundingBoxPaint.setAntiAlias(true);
    }

    public void setImageDimensions(float imageWidth, float imageHeight) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    public void setFrontCamera(boolean isFrontCamera) {
        this.isFrontCamera = isFrontCamera;
    }

    public void setFaceData(List<PointF> points, List<Line> lines) {
        this.facePoints = new ArrayList<>(points);
        this.faceLines = new ArrayList<>(lines);
        invalidate();
    }

    public void clear() {
        facePoints.clear();
        faceLines.clear();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
    }

    private PointF transformPoint(PointF point) {
        if (imageWidth == 0 || imageHeight == 0) return point;

        // Calculate scale to fit image in view while maintaining aspect ratio
        float scaleX = viewWidth / imageWidth;
        float scaleY = viewHeight / imageHeight;
        float scale = Math.min(scaleX, scaleY);

        // Calculate offset to center the image
        float scaledImageWidth = imageWidth * scale;
        float scaledImageHeight = imageHeight * scale;
        float offsetX = (viewWidth - scaledImageWidth) / 2;
        float offsetY = (viewHeight - scaledImageHeight) / 2;

        float x = point.x * scale + offsetX;
        float y = point.y * scale + offsetY;

        // Mirror for front camera
        if (isFrontCamera) {
            x = viewWidth - x;
        }

        return new PointF(x, y);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (facePoints.isEmpty()) return;

        // Draw lines first (so points are on top)
        for (Line line : faceLines) {
            PointF start = transformPoint(line.start);
            PointF end = transformPoint(line.end);
            canvas.drawLine(start.x, start.y, end.x, end.y, linePaint);
        }

        // Draw points
        for (PointF point : facePoints) {
            PointF transformed = transformPoint(point);
            canvas.drawCircle(transformed.x, transformed.y, 4f, pointPaint);
        }
    }
}