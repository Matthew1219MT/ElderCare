package com.eldercare.eldercare.viewmodel;

import android.app.Application;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.eldercare.eldercare.service.FallDetectionService;

public class VM_HomePage extends AndroidViewModel {

    private final MutableLiveData<Boolean> _showFallDialog = new MutableLiveData<>();
    public LiveData<Boolean> showFallDialog = _showFallDialog;

    private final MutableLiveData<Boolean> _startFallService = new MutableLiveData<>();
    public LiveData<Boolean> startFallService = _startFallService;

    public VM_HomePage(@NonNull Application application) {
        super(application);
    }

    public void onPermissionGranted() {
        _startFallService.setValue(true);
    }

    public void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("SHOW_DIALOG", false)) {
            FallDetectionService.cancelAutoLaunch();
            _showFallDialog.setValue(true);
        }
    }
}