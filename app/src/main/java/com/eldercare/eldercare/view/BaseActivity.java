package com.eldercare.eldercare.view;

import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.eldercare.eldercare.utils.LocaleHelper;

/**
 * Base Activity that applies locale to all child activities
 * All activities should extend this instead of AppCompatActivity
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        String languageCode = LocaleHelper.getLanguage(newBase);
        Context context = LocaleHelper.setLocale(newBase, languageCode);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Apply locale when activity is created
        applyLocale();
    }

    private void applyLocale() {
        String languageCode = LocaleHelper.getLanguage(this);
        LocaleHelper.setLocale(this, languageCode);
    }
}
