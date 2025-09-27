package com.eldercare.eldercare.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.eldercare.eldercare.R;
import com.eldercare.eldercare.viewmodel.VM_AIDoctor;

public class F_AIDoctorReply extends Fragment {

    private VM_AIDoctor viewModel;

    private TextView reply;
    private Button askBtn;
    private ImageButton backBtn;


    private android.os.Handler typingHandler = new android.os.Handler();
    private Runnable typingRunnable;

    public F_AIDoctorReply() {
        super(R.layout.fragment_ai_doctor_reply);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(VM_AIDoctor.class);
        reply = view.findViewById(R.id.reply);
        typeText(viewModel.getResponse());
        askBtn = view.findViewById(R.id.ask_btn);
        askBtn.setOnClickListener(v -> {
            viewModel.setReponse("");
            viewModel.switchFragment(VM_AIDoctor.FragmentType.Disclaimer);
        });
        backBtn = view.findViewById(R.id.back_button);
        backBtn.setOnClickListener(v->{
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
    }

    private void typeText(String content) {
        final int[] word_index = {0};

        typingRunnable = new Runnable() {
            @Override
            public void run() {
                if (word_index[0] < content.length()) {
                    String current_text = content.substring(0, word_index[0] + 1);
                    reply.setText(current_text);
                    word_index[0]++;
                    typingHandler.postDelayed(this, 50); // Adjust the delay as needed
                }
            }
        };
        typingHandler.post(typingRunnable);
    }
}
