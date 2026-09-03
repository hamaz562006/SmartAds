package com.partharoypc.smartads.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * General SharedPreferences wrapper for SmartAds SDK.
 */
public class SmartAdsPreferencesHelper {

    private static final String PREF_NAME = "smartads_prefs";
    private static volatile SmartAdsPreferencesHelper instance;
    private final SharedPreferences preferences;

    private SmartAdsPreferencesHelper(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Initializes the singleton instance of SmartAdsPreferencesHelper.
     *
     * @param context Application context.
     */
    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new SmartAdsPreferencesHelper(context);
        }
    }

    /**
     * Returns true if the helper has been initialized.
     */
    public static boolean isInitialized() {
        return instance != null;
    }

    /**
     * Returns the singleton instance of SmartAdsPreferencesHelper.
     *
     * @throws IllegalStateException if called before {@link #init(Context)}.
     */
    public static SmartAdsPreferencesHelper getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SmartAdsPreferencesHelper is not initialized. Call init(Context) first.");
        }
        return instance;
    }

    public void saveString(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }

    public String getString(String key, String defaultValue) {
        return preferences.getString(key, defaultValue);
    }

    public void saveInt(String key, int value) {
        preferences.edit().putInt(key, value).apply();
    }

    public int getInt(String key, int defaultValue) {
        return preferences.getInt(key, defaultValue);
    }

    public void saveBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }

    public void saveFloat(String key, float value) {
        preferences.edit().putFloat(key, value).apply();
    }

    public float getFloat(String key, float defaultValue) {
        return preferences.getFloat(key, defaultValue);
    }

    public void saveLong(String key, long value) {
        preferences.edit().putLong(key, value).apply();
    }

    public long getLong(String key, long defaultValue) {
        return preferences.getLong(key, defaultValue);
    }

    public void clearPreferences() {
        preferences.edit().clear().apply();
    }
}
