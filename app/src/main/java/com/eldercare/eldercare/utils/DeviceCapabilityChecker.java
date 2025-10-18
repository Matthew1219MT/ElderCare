package com.eldercare.eldercare.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.exceptions.UnavailableException;

public class DeviceCapabilityChecker {
    private static final String TAG = "DeviceCapabilityChecker";

    public enum ScanMethod {
        ARCORE,           // ARCore with AugmentedFace
        MLKIT_FACE_MESH,  // ML Kit Face Mesh Detection
        MLKIT_BASIC,      // ML Kit basic face detection (2D landmarks)
        NONE              // No face scanning available
    }

    public static ScanMethod getBestScanMethod(Context context) {
        // First, check if ARCore is supported and installed
        if (isARCoreSupported(context)) {
            Log.d(TAG, "Using ARCore for face scanning");
            return ScanMethod.ARCORE;
        }

        // Check if device has front camera
        if (!hasFrontCamera(context)) {
            Log.e(TAG, "No front camera available");
            return ScanMethod.NONE;
        }

        // Fall back to ML Kit Face Mesh (provides 3D landmarks)
        Log.d(TAG, "ARCore not available, using ML Kit Face Mesh");
        return ScanMethod.MLKIT_FACE_MESH;
    }

    public static boolean isARCoreSupported(Context context) {
        try {
            ArCoreApk.Availability availability = ArCoreApk.getInstance()
                    .checkAvailability(context);

            switch (availability) {
                case SUPPORTED_INSTALLED:
                    return true;
                case SUPPORTED_APK_TOO_OLD:
                case SUPPORTED_NOT_INSTALLED:
                    // ARCore is supported but needs installation/update
                    Log.w(TAG, "ARCore supported but needs installation/update");
                    return false;
                case UNSUPPORTED_DEVICE_NOT_CAPABLE:
                default:
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking ARCore availability", e);
            return false;
        }
    }

    private static boolean hasFrontCamera(Context context) {
        return context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    public static String getScanMethodDescription(ScanMethod method) {
        switch (method) {
            case ARCORE:
                return "High-precision 3D face scanning with ARCore";
            case MLKIT_FACE_MESH:
                return "3D face scanning with ML Kit Face Mesh";
            case MLKIT_BASIC:
                return "Basic 2D face detection";
            case NONE:
            default:
                return "Face scanning not available";
        }
    }
}