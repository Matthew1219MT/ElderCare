package com.eldercare.eldercare.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.eldercare.eldercare.model.FaceScanData;
import com.eldercare.eldercare.repository.FaceScanRepository;

public class FaceScanViewModel extends ViewModel {
    private FaceScanRepository repository;
    private MutableLiveData<ScanState> scanStateLiveData;

    public enum ScanState {
        IDLE,
        SCANNING,
        PROCESSING,
        COMPLETED,
        ERROR
    }

    public FaceScanViewModel() {
        repository = FaceScanRepository.getInstance();
        scanStateLiveData = new MutableLiveData<>(ScanState.IDLE);
    }

    public void startScanning() {
        scanStateLiveData.setValue(ScanState.SCANNING);
    }

    public void processScanData(FaceScanData faceScanData) {
        scanStateLiveData.setValue(ScanState.PROCESSING);
        repository.processScanData(faceScanData);
    }

    public void uploadScanData(FaceScanData faceScanData) {
        repository.uploadFaceScan(faceScanData);
    }

    public void completeScan() {
        scanStateLiveData.setValue(ScanState.COMPLETED);
    }

    public void resetScan() {
        scanStateLiveData.setValue(ScanState.IDLE);
    }

    public void errorOccurred() {
        scanStateLiveData.setValue(ScanState.ERROR);
    }

    // LiveData getters
    public LiveData<ScanState> getScanState() {
        return scanStateLiveData;
    }

    public LiveData<FaceScanData> getScanResult() {
        return repository.getScanResultLiveData();
    }

    public LiveData<String> getUploadStatus() {
        return repository.getUploadStatusLiveData();
    }

    public LiveData<String> getError() {
        return repository.getErrorLiveData();
    }
}