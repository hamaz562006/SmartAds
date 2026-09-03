package com.partharoypc.smartads.firebase;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue;
import com.partharoypc.smartads.SmartAdsLogger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Singleton helper to manage Firebase Remote Config operations including
 * fetching, activating, and accessing configuration values.
 */
public class SmartAdsFirebaseRemoteConfigHelper {

    private static volatile SmartAdsFirebaseRemoteConfigHelper instance;
    private FirebaseRemoteConfig remoteConfig;

    public interface OnRemoteConfigCompleteListener {
        void onComplete(boolean success);
    }

    private SmartAdsFirebaseRemoteConfigHelper() {
    }

    /**
     * Gets the singleton instance of {@link SmartAdsFirebaseRemoteConfigHelper}.
     */
    public static SmartAdsFirebaseRemoteConfigHelper getInstance() {
        if (instance == null) {
            synchronized (SmartAdsFirebaseRemoteConfigHelper.class) {
                if (instance == null) {
                    instance = new SmartAdsFirebaseRemoteConfigHelper();
                }
            }
        }
        return instance;
    }

    private synchronized FirebaseRemoteConfig ensureRemoteConfig(Context context) {
        if (remoteConfig == null) {
            try {
                if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseApp.initializeApp(context);
                }
                remoteConfig = FirebaseRemoteConfig.getInstance();
                FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(3600)
                        .build();
                remoteConfig.setConfigSettingsAsync(configSettings);
            } catch (Exception e) {
                SmartAdsLogger.e("Failed to initialize Firebase Remote Config: " + e.getMessage());
            }
        }
        return remoteConfig;
    }

    /**
     * Fetches and activates remote configuration values.
     *
     * @param context          Application or Activity context.
     * @param defaultsXmlResId Resource ID of XML defaults (pass 0 if none).
     * @param callback         Callback invoked with the operation result.
     */
    public void fetchAndActivate(@Nullable Context context,
                                 @XmlRes int defaultsXmlResId,
                                 @Nullable OnRemoteConfigCompleteListener callback) {
        FirebaseRemoteConfig config = ensureRemoteConfig(context);
        if (config == null) {
            SmartAdsLogger.e("FirebaseRemoteConfig is not initialized.");
            if (callback != null) {
                callback.onComplete(false);
            }
            return;
        }

        Task<Void> setDefaultsTask = null;
        if (defaultsXmlResId > 0) {
            try {
                setDefaultsTask = config.setDefaultsAsync(defaultsXmlResId);
            } catch (Exception e) {
                SmartAdsLogger.e("Failed to set Remote Config defaults: " + e.getMessage());
            }
        }

        Runnable fetchAction = () -> {
            config.fetchAndActivate().addOnCompleteListener(new OnCompleteListener<Boolean>() {
                @Override
                public void onComplete(@NonNull Task<Boolean> task) {
                    boolean success = task.isSuccessful();
                    if (success) {
                        SmartAdsLogger.d("Firebase Remote Config fetch and activate succeeded. Updated: " + task.getResult());
                    } else {
                        SmartAdsLogger.e("Firebase Remote Config fetch failed: "
                                + (task.getException() != null ? task.getException().getMessage() : "unknown error"));
                    }
                    if (callback != null) {
                        callback.onComplete(success);
                    }
                }
            });
        };

        if (setDefaultsTask != null) {
            setDefaultsTask.addOnCompleteListener(task -> fetchAction.run());
        } else {
            fetchAction.run();
        }
    }

    /**
     * Returns string value for the key.
     */
    public String getString(String key) {
        if (remoteConfig == null || key == null) return "";
        return remoteConfig.getString(key);
    }

    /**
     * Returns boolean value for the key.
     */
    public boolean getBoolean(String key) {
        if (remoteConfig == null || key == null) return false;
        return remoteConfig.getBoolean(key);
    }

    /**
     * Returns long value for the key.
     */
    public long getLong(String key) {
        if (remoteConfig == null || key == null) return 0L;
        return remoteConfig.getLong(key);
    }

    /**
     * Returns double value for the key.
     */
    public double getDouble(String key) {
        if (remoteConfig == null || key == null) return 0.0;
        return remoteConfig.getDouble(key);
    }

    /**
     * Returns all active keys in Remote Config.
     */
    public Set<String> getAllKeys() {
        if (remoteConfig == null) {
            return Collections.emptySet();
        }
        try {
            Set<String> keys = remoteConfig.getKeysByPrefix("");
            if (keys != null) {
                return new HashSet<>(keys);
            }
            Map<String, FirebaseRemoteConfigValue> all = remoteConfig.getAll();
            return all != null ? all.keySet() : Collections.emptySet();
        } catch (Exception e) {
            SmartAdsLogger.e("Error getting all Remote Config keys: " + e.getMessage());
            return Collections.emptySet();
        }
    }
}
