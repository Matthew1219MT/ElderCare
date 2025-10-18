package com.eldercare.eldercare.utils;

import android.content.Context;
import com.eldercare.eldercare.ar.ARFaceProcessor;
import com.eldercare.eldercare.utils.MLKitFaceProcessor;
import com.eldercare.eldercare.model.FaceScanData;
import com.eldercare.eldercare.utils.DeviceCapabilityChecker;

public class FaceProcessorFactory {

    public interface UnifiedFaceCallback {
        void onFaceDetected(FaceScanData faceScanData);
        void onFaceProcessingComplete(FaceScanData faceScanData);
        void onError(String error);
        void onScanMethodDetermined(DeviceCapabilityChecker.ScanMethod method);
    }

    public static class FaceProcessorWrapper {
        private DeviceCapabilityChecker.ScanMethod scanMethod;
        private ARFaceProcessor arProcessor;
        private MLKitFaceProcessor mlkitProcessor;

        public DeviceCapabilityChecker.ScanMethod getScanMethod() {
            return scanMethod;
        }

        public ARFaceProcessor getArProcessor() {
            return arProcessor;
        }

        public MLKitFaceProcessor getMlkitProcessor() {
            return mlkitProcessor;
        }

        public void cleanup() {
            if (mlkitProcessor != null) {
                mlkitProcessor.close();
            }
        }
    }

    public static FaceProcessorWrapper createFaceProcessor(
            Context context,
            UnifiedFaceCallback callback) {

        FaceProcessorWrapper wrapper = new FaceProcessorWrapper();
        DeviceCapabilityChecker.ScanMethod scanMethod =
                DeviceCapabilityChecker.getBestScanMethod(context);

        wrapper.scanMethod = scanMethod;
        callback.onScanMethodDetermined(scanMethod);

        switch (scanMethod) {
            case ARCORE:
                wrapper.arProcessor = new ARFaceProcessor(
                        new ARFaceProcessor.FaceProcessorCallback() {
                            @Override
                            public void onFaceDetected(FaceScanData faceScanData) {
                                callback.onFaceDetected(faceScanData);
                            }

                            @Override
                            public void onFaceProcessingComplete(FaceScanData faceScanData) {
                                callback.onFaceProcessingComplete(faceScanData);
                            }

                            @Override
                            public void onError(String error) {
                                callback.onError(error);
                            }
                        });
                break;

            case MLKIT_FACE_MESH:
            case MLKIT_BASIC:
                wrapper.mlkitProcessor = new MLKitFaceProcessor(
                        new MLKitFaceProcessor.MLKitFaceCallback() {
                            @Override
                            public void onFaceDetected(FaceScanData faceScanData) {
                                callback.onFaceDetected(faceScanData);
                            }

                            @Override
                            public void onFaceProcessingComplete(FaceScanData faceScanData) {
                                callback.onFaceProcessingComplete(faceScanData);
                            }

                            @Override
                            public void onError(String error) {
                                callback.onError(error);
                            }
                        });
                break;

            case NONE:
                callback.onError("Face scanning is not supported on this device");
                break;
        }

        return wrapper;
    }
}