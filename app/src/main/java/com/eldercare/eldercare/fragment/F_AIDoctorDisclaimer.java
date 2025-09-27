package com.eldercare.eldercare.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.eldercare.eldercare.R;
import com.eldercare.eldercare.viewmodel.VM_AIDoctor;

public class F_AIDoctorDisclaimer extends Fragment {

    private Button acceptBtn;
    private VM_AIDoctor viewModel;
    private ImageButton backBtn;
    private Button declineBtn;

    public F_AIDoctorDisclaimer() {
        super(R.layout.fragment_ai_doctor_disclaimer);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(VM_AIDoctor.class);
        acceptBtn = view.findViewById(R.id.understand_button);
        acceptBtn.setOnClickListener(v -> {
            viewModel.switchFragment(VM_AIDoctor.FragmentType.Query);
        });
        backBtn = view.findViewById(R.id.back_button);
        backBtn.setOnClickListener(v->{
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
        declineBtn = view.findViewById(R.id.decline_button);
        declineBtn.setOnClickListener(v->{
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
    }
}
