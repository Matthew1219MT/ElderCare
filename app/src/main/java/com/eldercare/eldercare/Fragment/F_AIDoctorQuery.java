package com.eldercare.eldercare.Fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.eldercare.eldercare.R;
import com.eldercare.eldercare.ViewModel.VM_AIDoctor;

public class F_AIDoctorQuery extends Fragment {

    private VM_AIDoctor viewModel;

    private EditText query;

    private Button queryBtn;

    public F_AIDoctorQuery() {
        super(R.layout.fragment_ai_doctor_query);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(VM_AIDoctor.class);
        query = view.findViewById(R.id.symptoms_input);
        query.setHint("Please enter your symptoms");
        queryBtn = view.findViewById(R.id.send_message_btn);
        queryBtn.setOnClickListener(v -> {
            String queryStr = query.getText().toString();
            if (queryStr.isEmpty()) {
                query.setHint("Empty Input! Please enter your symptoms");
            } else {
                query.getText().clear();
                query.setHint("Loading...");
                viewModel.sendQuery(queryStr,
                    onSuccess -> {
                        viewModel.setReponse(onSuccess);
                        viewModel.switchFragment(VM_AIDoctor.FragmentType.Reply);
                    },
                    onError -> {
                        viewModel.setReponse(onError);
                        viewModel.switchFragment(VM_AIDoctor.FragmentType.Reply);
                    }
                );
            }
        });
    }
}
