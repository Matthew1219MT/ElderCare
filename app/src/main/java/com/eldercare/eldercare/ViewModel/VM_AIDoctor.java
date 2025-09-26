package com.eldercare.eldercare.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class VM_AIDoctor extends ViewModel {
    public enum FragmentType {
        Disclaimer,
        Query,
        Reply
    }
    private MutableLiveData<FragmentType> currentFragment = new MutableLiveData<>();

    public VM_AIDoctor() {
        currentFragment.setValue(FragmentType.Disclaimer);
    }

    public LiveData<FragmentType> getCurrentFragment() {
        return currentFragment;
    }

    public void switchFragment(FragmentType fragment) {
        currentFragment.setValue(fragment);
    }
}
