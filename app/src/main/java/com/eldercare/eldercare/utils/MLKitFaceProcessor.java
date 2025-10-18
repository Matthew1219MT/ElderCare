package com.eldercare.eldercare.utils;

import android.graphics.Bitmap;
import android.util.Log;
import androidx.annotation.NonNull;
import com.eldercare.eldercare.model.FaceScanData;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.facemesh.FaceMesh;
import com.google.mlkit.vision.facemesh.FaceMeshDetection;
import com.google.mlkit.vision.facemesh.FaceMeshDetector;
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions;
import com.google.mlkit.vision.facemesh.FaceMeshPoint;
import java.util.ArrayList;
import java.util.List;

public class MLKitFaceProcessor {
    private static final String TAG = "MLKitFaceProcessor";
    private static final int MIN_FACE_POINTS = 400;

    public interface MLKitFaceCallback {
        void onFaceDetected(FaceScanData faceScanData);
        void onFaceProcessingComplete(FaceScanData faceScanData);
        void onError(String error);
    }

    private FaceMeshDetector detector;
    private MLKitFaceCallback callback;
    private int imageWidth;
    private int imageHeight;

    public MLKitFaceProcessor(MLKitFaceCallback callback) {
        this.callback = callback;
        initializeDetector();
    }

    private void initializeDetector() {
        FaceMeshDetectorOptions options = new FaceMeshDetectorOptions.Builder()
                .setUseCase(FaceMeshDetectorOptions.FACE_MESH)
                .build();

        detector = FaceMeshDetection.getClient(options);
        Log.d(TAG, "ML Kit Face Mesh detector initialized");
    }

    public void processImage(Bitmap bitmap) {
        if (bitmap == null) {
            callback.onError("Invalid image");
            return;
        }

        imageWidth = bitmap.getWidth();
        imageHeight = bitmap.getHeight();

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        detector.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<FaceMesh>>() {
                    @Override
                    public void onSuccess(List<FaceMesh> faceMeshes) {
                        if (faceMeshes.isEmpty()) {
                            callback.onError("No face detected. Please ensure your face is visible and well-lit.");
                            return;
                        }

                        // Process the first detected face
                        processFaceMesh(faceMeshes.get(0));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Face detection failed", e);
                        callback.onError("Face detection failed: " + e.getMessage());
                    }
                });
    }

    private void processFaceMesh(FaceMesh faceMesh) {
        try {
            FaceScanData faceScanData = new FaceScanData();

            // Extract 3D points from face mesh
            List<FaceMeshPoint> meshPoints = faceMesh.getAllPoints();
            List<FaceScanData.FacePoint> points = extractFacePoints(meshPoints);

            if (points.size() < MIN_FACE_POINTS) {
                callback.onError("Insufficient face data. Please move closer to the camera.");
                return;
            }

            faceScanData.setPoints(points);

            // Extract geometry (triangles)
            FaceScanData.FaceGeometry geometry = extractFaceGeometry(meshPoints);
            faceScanData.setGeometry(geometry);

            Log.d(TAG, "Face processed: " + points.size() + " points");

            if (callback != null) {
                callback.onFaceDetected(faceScanData);
                callback.onFaceProcessingComplete(faceScanData);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing face mesh", e);
            if (callback != null) {
                callback.onError("Failed to process face: " + e.getMessage());
            }
        }
    }

    private List<FaceScanData.FacePoint> extractFacePoints(List<FaceMeshPoint> meshPoints) {
        List<FaceScanData.FacePoint> points = new ArrayList<>();

        // Normalize coordinates to be centered around origin
        float centerX = imageWidth / 2.0f;
        float centerY = imageHeight / 2.0f;
        float scale = 1000.0f; // Scale factor to convert pixels to approximate real-world units

        for (FaceMeshPoint meshPoint : meshPoints) {
            // Convert from image coordinates to 3D coordinates
            // ML Kit provides normalized 3D coordinates
            float x = (meshPoint.getPosition().getX() - centerX) / scale;
            float y = -(meshPoint.getPosition().getY() - centerY) / scale; // Invert Y axis
            float z = meshPoint.getPosition().getZ() / scale;

            // Use index as a simple confidence measure
            // Points with lower indices are typically more reliable
            float confidence = Math.max(0.5f, Math.min(1.0f,
                    1.0f - (meshPoint.getIndex() / (float) meshPoints.size())));

            points.add(new FaceScanData.FacePoint(x, y, z, confidence));
        }

        return points;
    }

    private FaceScanData.FaceGeometry extractFaceGeometry(List<FaceMeshPoint> meshPoints) {
        FaceScanData.FaceGeometry geometry = new FaceScanData.FaceGeometry();

        // ML Kit doesn't provide triangle data directly
        // We'll use a predefined triangulation based on MediaPipe face mesh topology
        List<FaceScanData.FaceGeometry.Triangle> triangles = generateFaceMeshTriangles();
        geometry.setTriangles(triangles);

        // Calculate bounding box from all points
        geometry.setBoundingBox(calculateBoundingBox(meshPoints));

        return geometry;
    }

    /**
     * Generates triangles for the 468-point face mesh using MediaPipe's topology.
     * This is a subset of the full MediaPipe triangulation for demonstration.
     * For production, you should use the complete triangulation data.
     */
    private List<FaceScanData.FaceGeometry.Triangle> generateFaceMeshTriangles() {
        List<FaceScanData.FaceGeometry.Triangle> triangles = new ArrayList<>();

        // MediaPipe Face Mesh Triangulation (subset - key facial features)
        // Full triangulation has ~940 triangles for 468 points
        // Here's a simplified version covering main facial features

        int[][] triangleIndices = {
                // Face outline
                {10, 338, 297}, {10, 297, 332}, {10, 332, 284}, {10, 284, 251},
                {10, 251, 389}, {10, 389, 356}, {10, 356, 454}, {10, 454, 323},
                {10, 323, 361}, {10, 361, 288}, {10, 288, 397}, {10, 397, 365},
                {10, 365, 379}, {10, 379, 378}, {10, 378, 400}, {10, 400, 377},

                // Nose bridge
                {168, 6, 197}, {197, 195, 5}, {5, 4, 195}, {4, 1, 195},
                {1, 44, 195}, {44, 125, 195}, {125, 128, 195}, {128, 129, 195},

                // Left eye
                {33, 246, 161}, {161, 160, 159}, {159, 158, 157}, {157, 173, 133},
                {133, 155, 154}, {154, 153, 145}, {145, 144, 163}, {163, 7, 33},

                // Right eye
                {263, 466, 388}, {388, 387, 386}, {386, 385, 384}, {384, 398, 362},
                {362, 382, 381}, {381, 380, 374}, {374, 373, 390}, {390, 249, 263},

                // Mouth outer
                {61, 185, 40}, {40, 39, 37}, {37, 0, 267}, {267, 269, 270},
                {270, 409, 291}, {291, 375, 321}, {321, 405, 314}, {314, 17, 84},
                {84, 181, 91}, {91, 146, 61},

                // Mouth inner
                {78, 191, 80}, {80, 81, 82}, {82, 13, 312}, {312, 311, 310},
                {310, 415, 308}, {308, 324, 318}, {318, 402, 317}, {317, 14, 87},
                {87, 178, 88}, {88, 95, 78},

                // Forehead
                {10, 109, 67}, {67, 69, 104}, {104, 68, 71}, {71, 139, 34},
                {34, 227, 137}, {137, 177, 215}, {215, 138, 135}, {135, 169, 170},
                {170, 140, 171}, {171, 175, 152}, {152, 148, 176}, {176, 149, 150},

                // Left cheek
                {116, 123, 50}, {50, 101, 36}, {36, 205, 206}, {206, 203, 205},
                {205, 187, 123}, {123, 147, 213}, {213, 192, 214}, {214, 210, 212},

                // Right cheek
                {345, 352, 280}, {280, 330, 266}, {266, 425, 426}, {426, 423, 425},
                {425, 411, 352}, {352, 376, 433}, {433, 416, 434}, {434, 430, 432},

                // Chin
                {152, 377, 400}, {400, 378, 379}, {379, 365, 397}, {397, 288, 361},
                {361, 323, 454}, {454, 356, 389}, {389, 251, 284}, {284, 332, 297}
        };

        for (int[] triangle : triangleIndices) {
            triangles.add(new FaceScanData.FaceGeometry.Triangle(
                    triangle[0], triangle[1], triangle[2]
            ));
        }

        return triangles;
    }

    private FaceScanData.FaceGeometry.BoundingBox calculateBoundingBox(
            List<FaceMeshPoint> points) {

        if (points.isEmpty()) {
            return new FaceScanData.FaceGeometry.BoundingBox(
                    -0.1f, -0.1f, -0.1f, 0.1f, 0.1f, 0.1f);
        }

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE, maxZ = Float.MIN_VALUE;

        float centerX = imageWidth / 2.0f;
        float centerY = imageHeight / 2.0f;
        float scale = 1000.0f;

        for (FaceMeshPoint point : points) {
            float x = (point.getPosition().getX() - centerX) / scale;
            float y = -(point.getPosition().getY() - centerY) / scale;
            float z = point.getPosition().getZ() / scale;

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        return new FaceScanData.FaceGeometry.BoundingBox(
                minX, minY, minZ, maxX, maxY, maxZ);
    }

    public void close() {
        if (detector != null) {
            detector.close();
        }
    }
}