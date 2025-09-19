package com.eldercare.eldercare.model;

import java.util.List;

public class FaceScanData {
    private String id;
    private long timestamp;
    private List<FacePoint> points;
    private FaceGeometry geometry;
    private String textureData;

    public static class FacePoint {
        private float x, y, z;
        private float confidence;

        public FacePoint(float x, float y, float z, float confidence) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.confidence = confidence;
        }

        // Getters and setters
        public float getX() { return x; }
        public void setX(float x) { this.x = x; }

        public float getY() { return y; }
        public void setY(float y) { this.y = y; }

        public float getZ() { return z; }
        public void setZ(float z) { this.z = z; }

        public float getConfidence() { return confidence; }
        public void setConfidence(float confidence) { this.confidence = confidence; }
    }

    public static class FaceGeometry {
        private List<Triangle> triangles;
        private BoundingBox boundingBox;

        public static class Triangle {
            private int[] vertexIndices = new int[3];

            public Triangle(int v1, int v2, int v3) {
                vertexIndices[0] = v1;
                vertexIndices[1] = v2;
                vertexIndices[2] = v3;
            }

            public int[] getVertexIndices() { return vertexIndices; }
        }

        public static class BoundingBox {
            private float minX, minY, minZ;
            private float maxX, maxY, maxZ;

            public BoundingBox(float minX, float minY, float minZ,
                               float maxX, float maxY, float maxZ) {
                this.minX = minX;
                this.minY = minY;
                this.minZ = minZ;
                this.maxX = maxX;
                this.maxY = maxY;
                this.maxZ = maxZ;
            }

            // Getters
            public float getMinX() { return minX; }
            public float getMinY() { return minY; }
            public float getMinZ() { return minZ; }
            public float getMaxX() { return maxX; }
            public float getMaxY() { return maxY; }
            public float getMaxZ() { return maxZ; }
        }

        // Getters and setters
        public List<Triangle> getTriangles() { return triangles; }
        public void setTriangles(List<Triangle> triangles) { this.triangles = triangles; }

        public BoundingBox getBoundingBox() { return boundingBox; }
        public void setBoundingBox(BoundingBox boundingBox) { this.boundingBox = boundingBox; }
    }

    public FaceScanData() {
        this.timestamp = System.currentTimeMillis();
        this.id = "face_scan_" + timestamp;
    }

    // Main getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public List<FacePoint> getPoints() { return points; }
    public void setPoints(List<FacePoint> points) { this.points = points; }

    public FaceGeometry getGeometry() { return geometry; }
    public void setGeometry(FaceGeometry geometry) { this.geometry = geometry; }

    public String getTextureData() { return textureData; }
    public void setTextureData(String textureData) { this.textureData = textureData; }
}