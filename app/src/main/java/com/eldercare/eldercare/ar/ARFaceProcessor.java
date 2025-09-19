package com.eldercare.eldercare.ar;

import android.util.Log;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.Pose;
import com.eldercare.eldercare.model.FaceScanData;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ARFaceProcessor {
    private static final String TAG = "ARFaceProcessor";
    private static final int MIN_FACE_VERTICES = 468; // Minimum vertices for detailed face mesh

    public interface FaceProcessorCallback {
        void onFaceDetected(FaceScanData faceScanData);
        void onFaceProcessingComplete(FaceScanData faceScanData);
        void onError(String error);
    }

    private FaceProcessorCallback callback;

    public ARFaceProcessor(FaceProcessorCallback callback) {
        this.callback = callback;
    }

    public void processAugmentedFaces(Collection<AugmentedFace> faces) {
        if (faces.isEmpty()) {
            Log.d(TAG, "No faces detected");
            return;
        }

        for (AugmentedFace face : faces) {
            if (face.getTrackingState() == com.google.ar.core.TrackingState.TRACKING) {
                processFace(face);
                break; // Process only the first detected face
            }
        }
    }

    private void processFace(AugmentedFace face) {
        try {
            FaceScanData faceScanData = new FaceScanData();

            // Extract vertices (3D points)
            FloatBuffer vertices = face.getMeshVertices();
            List<FaceScanData.FacePoint> points = extractFacePoints(vertices);

            if (points.size() < MIN_FACE_VERTICES) {
                callback.onError("Insufficient face data detected. Please ensure good lighting and face the camera directly.");
                return;
            }

            faceScanData.setPoints(points);

            // Extract triangles for mesh
            android.opengl.GLES20.glGetError(); // Clear any previous GL errors
            FaceScanData.FaceGeometry geometry = extractFaceGeometry(face);
            faceScanData.setGeometry(geometry);

            // Extract texture coordinates if available
            FloatBuffer textureCoords = face.getMeshTextureCoordinates();
            String textureData = extractTextureData(textureCoords);
            faceScanData.setTextureData(textureData);

            Log.d(TAG, "Face processed: " + points.size() + " vertices");

            if (callback != null) {
                callback.onFaceDetected(faceScanData);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing face", e);
            if (callback != null) {
                callback.onError("Failed to process face data: " + e.getMessage());
            }
        }
    }

    private List<FaceScanData.FacePoint> extractFacePoints(FloatBuffer vertices) {
        List<FaceScanData.FacePoint> points = new ArrayList<>();

        vertices.rewind();
        while (vertices.hasRemaining()) {
            float x = vertices.get();
            float y = vertices.hasRemaining() ? vertices.get() : 0;
            float z = vertices.hasRemaining() ? vertices.get() : 0;

            // Calculate confidence based on distance from face center
            float confidence = calculatePointConfidence(x, y, z);

            points.add(new FaceScanData.FacePoint(x, y, z, confidence));
        }

        return points;
    }

    private FaceScanData.FaceGeometry extractFaceGeometry(AugmentedFace face) {
        FaceScanData.FaceGeometry geometry = new FaceScanData.FaceGeometry();

        // Extract triangle indices
        android.opengl.GLES20.glGetError(); // Clear GL errors
        java.nio.ShortBuffer triangleIndices = face.getMeshTriangleIndices();
        List<FaceScanData.FaceGeometry.Triangle> triangles = new ArrayList<>();

        triangleIndices.rewind();
        while (triangleIndices.remaining() >= 3) {
            int v1 = triangleIndices.get();
            int v2 = triangleIndices.get();
            int v3 = triangleIndices.get();
            triangles.add(new FaceScanData.FaceGeometry.Triangle(v1, v2, v3));
        }

        geometry.setTriangles(triangles);

        // Calculate bounding box
        Pose centerPose = face.getCenterPose();
        float[] translation = centerPose.getTranslation();

        // Estimate bounding box based on typical face dimensions
        float faceWidth = 0.15f;  // ~15cm
        float faceHeight = 0.20f; // ~20cm
        float faceDepth = 0.12f;  // ~12cm

        FaceScanData.FaceGeometry.BoundingBox boundingBox = new FaceScanData.FaceGeometry.BoundingBox(
                translation[0] - faceWidth/2, translation[1] - faceHeight/2, translation[2] - faceDepth/2,
                translation[0] + faceWidth/2, translation[1] + faceHeight/2, translation[2] + faceDepth/2
        );

        geometry.setBoundingBox(boundingBox);

        return geometry;
    }

    private String extractTextureData(FloatBuffer textureCoords) {
        if (textureCoords == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        textureCoords.rewind();

        while (textureCoords.hasRemaining()) {
            float u = textureCoords.get();
            float v = textureCoords.hasRemaining() ? textureCoords.get() : 0;
            sb.append(u).append(",").append(v).append(";");
        }

        return sb.toString();
    }

    private float calculatePointConfidence(float x, float y, float z) {
        // Simple confidence calculation based on distance from origin
        float distance = (float) Math.sqrt(x*x + y*y + z*z);
        return Math.max(0.1f, Math.min(1.0f, 1.0f - (distance / 0.5f)));
    }
}