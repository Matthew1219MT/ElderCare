package com.eldercare.eldercare.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

public class FaceScanData implements Parcelable {
    private String id;
    private long timestamp;
    private List<FacePoint> points;
    private FaceGeometry geometry;
    private String textureData;

    public FaceScanData() {
        this.timestamp = System.currentTimeMillis();
        this.id = "face_scan_" + timestamp;
    }

    protected FaceScanData(Parcel in) {
        id = in.readString();
        timestamp = in.readLong();
        points = in.createTypedArrayList(FacePoint.CREATOR);
        geometry = in.readParcelable(FaceGeometry.class.getClassLoader());
        textureData = in.readString();
    }

    public static final Creator<FaceScanData> CREATOR = new Creator<FaceScanData>() {
        @Override
        public FaceScanData createFromParcel(Parcel in) {
            return new FaceScanData(in);
        }

        @Override
        public FaceScanData[] newArray(int size) {
            return new FaceScanData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeLong(timestamp);
        dest.writeTypedList(points);
        dest.writeParcelable(geometry, flags);
        dest.writeString(textureData);
    }

    public static class FacePoint implements Parcelable {
        private float x, y, z;
        private float confidence;

        public FacePoint(float x, float y, float z, float confidence) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.confidence = confidence;
        }

        protected FacePoint(Parcel in) {
            x = in.readFloat();
            y = in.readFloat();
            z = in.readFloat();
            confidence = in.readFloat();
        }

        public static final Creator<FacePoint> CREATOR = new Creator<FacePoint>() {
            @Override
            public FacePoint createFromParcel(Parcel in) {
                return new FacePoint(in);
            }

            @Override
            public FacePoint[] newArray(int size) {
                return new FacePoint[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeFloat(x);
            dest.writeFloat(y);
            dest.writeFloat(z);
            dest.writeFloat(confidence);
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

    public static class FaceGeometry implements Parcelable {
        private List<Triangle> triangles;
        private BoundingBox boundingBox;

        public FaceGeometry() {}

        protected FaceGeometry(Parcel in) {
            triangles = in.createTypedArrayList(Triangle.CREATOR);
            boundingBox = in.readParcelable(BoundingBox.class.getClassLoader());
        }

        public static final Creator<FaceGeometry> CREATOR = new Creator<FaceGeometry>() {
            @Override
            public FaceGeometry createFromParcel(Parcel in) {
                return new FaceGeometry(in);
            }

            @Override
            public FaceGeometry[] newArray(int size) {
                return new FaceGeometry[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeTypedList(triangles);
            dest.writeParcelable(boundingBox, flags);
        }

        public static class Triangle implements Parcelable {
            private int[] vertexIndices = new int[3];

            public Triangle(int v1, int v2, int v3) {
                vertexIndices[0] = v1;
                vertexIndices[1] = v2;
                vertexIndices[2] = v3;
            }

            protected Triangle(Parcel in) {
                in.readIntArray(vertexIndices);
            }

            public static final Creator<Triangle> CREATOR = new Creator<Triangle>() {
                @Override
                public Triangle createFromParcel(Parcel in) {
                    return new Triangle(in);
                }

                @Override
                public Triangle[] newArray(int size) {
                    return new Triangle[size];
                }
            };

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(@NonNull Parcel dest, int flags) {
                dest.writeIntArray(vertexIndices);
            }

            public int[] getVertexIndices() { return vertexIndices; }
        }

        public static class BoundingBox implements Parcelable {
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

            protected BoundingBox(Parcel in) {
                minX = in.readFloat();
                minY = in.readFloat();
                minZ = in.readFloat();
                maxX = in.readFloat();
                maxY = in.readFloat();
                maxZ = in.readFloat();
            }

            public static final Creator<BoundingBox> CREATOR = new Creator<BoundingBox>() {
                @Override
                public BoundingBox createFromParcel(Parcel in) {
                    return new BoundingBox(in);
                }

                @Override
                public BoundingBox[] newArray(int size) {
                    return new BoundingBox[size];
                }
            };

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(@NonNull Parcel dest, int flags) {
                dest.writeFloat(minX);
                dest.writeFloat(minY);
                dest.writeFloat(minZ);
                dest.writeFloat(maxX);
                dest.writeFloat(maxY);
                dest.writeFloat(maxZ);
            }

            public float getMinX() { return minX; }
            public float getMinY() { return minY; }
            public float getMinZ() { return minZ; }
            public float getMaxX() { return maxX; }
            public float getMaxY() { return maxY; }
            public float getMaxZ() { return maxZ; }
        }

        public List<Triangle> getTriangles() { return triangles; }
        public void setTriangles(List<Triangle> triangles) { this.triangles = triangles; }
        public BoundingBox getBoundingBox() { return boundingBox; }
        public void setBoundingBox(BoundingBox boundingBox) { this.boundingBox = boundingBox; }
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