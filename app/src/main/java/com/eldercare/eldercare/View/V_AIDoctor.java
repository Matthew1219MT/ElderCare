package com.eldercare.eldercare.View;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.eldercare.eldercare.Fragment.F_AIDoctorDisclaimer;
import com.eldercare.eldercare.Fragment.F_AIDoctorQuery;
import com.eldercare.eldercare.Fragment.F_AIDoctorReply;
import com.eldercare.eldercare.R;
import com.eldercare.eldercare.ViewModel.VM_AIDoctor;

public class V_AIDoctor extends AppCompatActivity {

    Fragment DisclaimerFragment = new F_AIDoctorDisclaimer();
    Fragment QueryFragment = new F_AIDoctorQuery();
    Fragment ReplyFragment = new F_AIDoctorReply();
    private VM_AIDoctor viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_doctor);

        viewModel = new ViewModelProvider(this).get(VM_AIDoctor.class);

        viewModel.getCurrentFragment().observe(this, this::handleFragmentChange);
    }

    private void handleFragmentChange(VM_AIDoctor.FragmentType fragmentType) {
        switch (fragmentType) {
            case Disclaimer:
                setFragment(DisclaimerFragment);
                break;
            case Query:
                setFragment(QueryFragment);
                break;
            case Reply:
                setFragment(ReplyFragment);
                break;
        }
    }

    private void setFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
