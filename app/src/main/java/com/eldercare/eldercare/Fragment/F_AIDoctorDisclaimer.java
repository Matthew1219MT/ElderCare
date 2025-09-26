package com.eldercare.eldercare.Fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.eldercare.eldercare.R;
import com.eldercare.eldercare.ViewModel.VM_AIDoctor;

public class F_AIDoctorDisclaimer extends Fragment {

    private Button understand_btn;
    private VM_AIDoctor viewModel;

    public F_AIDoctorDisclaimer() {
        super(R.layout.fragment_ai_doctor_disclaimer);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(VM_AIDoctor.class);
        understand_btn = view.findViewById(R.id.understand_button);
        understand_btn.setOnClickListener(v -> {
            viewModel.switchFragment(VM_AIDoctor.FragmentType.Query);
        });
    }
}
