package com.eldercare.eldercare.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import java.util.Locale;

/**
 * Helper class to manage app language/locale changes
 * Supports English (default) and Indonesian
 */
public class LocaleHelper {
    
    private static final String PREFERENCE_NAME = "ElderCarePrefs";
    private static final String KEY_LANGUAGE = "selected_language";
    
    // Language codes
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_INDONESIAN = "id";
    
    /**
     * Set and persist language preference
     */
    public static Context setLocale(Context context, String languageCode) {
        persist(context, languageCode);
        return updateResources(context, languageCode);
    }
    
    /**
     * Get saved language preference
     */
    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_ENGLISH); // Default to English
    }
    
    /**
     * Save language preference to SharedPreferences
     */
    private static void persist(Context context, String languageCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_LANGUAGE, languageCode);
        editor.apply();
    }
    
    /**
     * Update app resources with new locale
     */
    private static Context updateResources(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        
        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            context = context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        }
        
        return context;
    }
    
    /**
     * Get language display name for UI
     */
    public static String getLanguageName(String languageCode) {
        switch (languageCode) {
            case LANGUAGE_ENGLISH:
                return "English";
            case LANGUAGE_INDONESIAN:
                return "Bahasa Indonesia";
            default:
                return "English";
        }
    }
    
    /**
     * Toggle between English and Indonesian
     */
    public static String toggleLanguage(Context context) {
        String currentLanguage = getLanguage(context);
        String newLanguage = currentLanguage.equals(LANGUAGE_ENGLISH) 
            ? LANGUAGE_INDONESIAN 
            : LANGUAGE_ENGLISH;
        setLocale(context, newLanguage);
        return newLanguage;
    }
}
