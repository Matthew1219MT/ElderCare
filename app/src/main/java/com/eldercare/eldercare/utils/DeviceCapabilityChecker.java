package com.eldercare.eldercare.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.core.exceptions.UnsupportedConfigurationException;

public class DeviceCapabilityChecker {
    private static final String TAG = "DeviceCapabilityChecker";

    public enum ScanMethod {
        ARCORE,           // ARCore with AugmentedFace
        MLKIT_FACE_MESH,  // ML Kit Face Mesh Detection
        MLKIT_BASIC,      // ML Kit basic face detection (2D landmarks)
        NONE              // No face scanning available
    }

    public static ScanMethod getBestScanMethod(Context context) {
        // Check if ARCore Augmented Faces is fully supported
        if (isAugmentedFacesSupported(context)) {
            Log.d(TAG, "Using ARCore for face scanning");
            return ScanMethod.ARCORE;
        }

        // Check if device has front camera
        if (!hasFrontCamera(context)) {
            Log.e(TAG, "No front camera available");
            return ScanMethod.NONE;
        }

        // Fall back to ML Kit Face Mesh (provides 3D landmarks)
        Log.d(TAG, "ARCore Augmented Faces not available, using ML Kit Face Mesh");
        return ScanMethod.MLKIT_FACE_MESH;
    }

    /**
     * Check if ARCore Augmented Faces is supported by actually testing the configuration
     */
    public static boolean isAugmentedFacesSupported(Context context) {
        // First check basic ARCore support
        if (!isARCoreSupported(context)) {
            return false;
        }

        // Now check if Augmented Faces specifically is supported
        Session testSession = null;
        try {
            testSession = new Session(context);
            Config config = new Config(testSession);
            config.setAugmentedFaceMode(Config.AugmentedFaceMode.MESH3D);

            // Try to configure - this will throw exception if not supported
            testSession.configure(config);

            Log.d(TAG, "Augmented Faces MESH3D is supported");
            return true;

        } catch (UnsupportedConfigurationException e) {
            Log.w(TAG, "Augmented Faces not supported on this device", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking Augmented Faces support", e);
            return false;
        } finally {
            if (testSession != null) {
                testSession.close();
            }
        }
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
                return "High-precision 3D face scanning with ARCore Augmented Faces";
            case MLKIT_FACE_MESH:
                return "3D face mesh scanning with ML Kit (468 landmarks)";
            case MLKIT_BASIC:
                return "Basic 2D face detection";
            case NONE:
            default:
                return "Face scanning not available";
        }
    }
}