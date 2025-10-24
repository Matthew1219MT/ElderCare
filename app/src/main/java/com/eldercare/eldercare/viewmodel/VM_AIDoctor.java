package com.eldercare.eldercare.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.eldercare.eldercare.model.M_AIDoctor;

import java.util.function.Consumer;

public class VM_AIDoctor extends AndroidViewModel {
    public enum FragmentType {
        Disclaimer,
        Query,
        Reply
    }

    private M_AIDoctor model;
    private MutableLiveData<FragmentType> currentFragment = new MutableLiveData<>();
    private String response = "";
    public VM_AIDoctor(@NonNull Application application) {
        super(application);
        currentFragment.setValue(FragmentType.Disclaimer);
        model = new M_AIDoctor(application.getApplicationContext());
    }

    public LiveData<FragmentType> getCurrentFragment() {
        return currentFragment;
    }

    public void switchFragment(FragmentType fragment) {
        currentFragment.setValue(fragment);
    }

    public void sendQuery(String query, Consumer<String> onSuccess , Consumer<String> onError) {
        model.askAI(
            query,
            onSuccess::accept,
            onError::accept
        );
    }

    public void setReponse(String r) {
        response = r;
    }

    public String getResponse() {
        return response;
    }
}
