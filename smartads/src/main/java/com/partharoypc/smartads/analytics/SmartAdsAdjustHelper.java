package com.partharoypc.smartads.analytics;

import android.content.Context;

import com.partharoypc.smartads.SmartAdsLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility helper class for initializing and interacting with the Adjust SDK via safe reflection.
 * <p>
 * This feature is completely optional. To enable Adjust attribution:
 * <ol>
 *   <li>Add <code>com.adjust.sdk:adjust-android</code> to your application's <code>build.gradle.kts</code> dependencies.</li>
 *   <li>Call <code>SmartAds.getInstance().enableAdjustAttribution(...)</code> with your App Token (from Adjust dashboard)
 *       and the eventName-to-eventToken mapping map.</li>
 * </ol>
 * All interactions use reflection, ensuring zero compile-time or runtime dependencies when Adjust is not used.
 */
public final class SmartAdsAdjustHelper {

    private static final String ADJUST_CLASS = "com.adjust.sdk.Adjust";
    private static final String ADJUST_CONFIG_CLASS = "com.adjust.sdk.AdjustConfig";
    private static final String ADJUST_EVENT_CLASS = "com.adjust.sdk.AdjustEvent";
    private static final String ADJUST_LOG_LEVEL_CLASS = "com.adjust.sdk.LogLevel";

    private SmartAdsAdjustHelper() {
        // Utility class
    }

    /**
     * Checks whether the Adjust SDK classes are present on the application classpath.
     *
     * @return true if Adjust SDK classes are accessible via reflection, false otherwise.
     */
    public static boolean isAdjustSdkAvailable() {
        try {
            Class.forName(ADJUST_CLASS);
            Class.forName(ADJUST_CONFIG_CLASS);
            Class.forName(ADJUST_EVENT_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Initializes the Adjust SDK using reflection.
     *
     * @param context     Application or activity context.
     * @param appToken    Adjust application token from the dashboard.
     * @param sandboxMode true for AdjustConfig.ENVIRONMENT_SANDBOX, false for AdjustConfig.ENVIRONMENT_PRODUCTION.
     * @return true if initialization succeeded via reflection, false otherwise.
     */
    public static boolean initAdjust(Context context, String appToken, boolean sandboxMode) {
        if (context == null || appToken == null || appToken.trim().isEmpty()) {
            SmartAdsLogger.d("Adjust init skipped: context or appToken is null/empty.");
            return false;
        }

        try {
            Class<?> configClass = Class.forName(ADJUST_CONFIG_CLASS);
            Class<?> adjustClass = Class.forName(ADJUST_CLASS);

            String environmentFieldName = sandboxMode ? "ENVIRONMENT_SANDBOX" : "ENVIRONMENT_PRODUCTION";
            String environmentValue;
            try {
                Field envField = configClass.getField(environmentFieldName);
                environmentValue = (String) envField.get(null);
            } catch (Exception e) {
                environmentValue = sandboxMode ? "sandbox" : "production";
            }

            Constructor<?> configConstructor = configClass.getConstructor(Context.class, String.class, String.class);
            Object configInstance = configConstructor.newInstance(context.getApplicationContext(), appToken, environmentValue);

            // Attempt to call Adjust.initSdk(config) (Adjust v5+) or fallback to Adjust.onCreate(config) (Adjust v4)
            try {
                Method initSdkMethod = adjustClass.getMethod("initSdk", configClass);
                initSdkMethod.invoke(null, configInstance);
            } catch (NoSuchMethodException e) {
                Method onCreateMethod = adjustClass.getMethod("onCreate", configClass);
                onCreateMethod.invoke(null, configInstance);
            }

            SmartAdsLogger.d("Adjust SDK initialized via reflection (Environment: " + environmentValue + ")");
            return true;
        } catch (ClassNotFoundException e) {
            SmartAdsLogger.d("Adjust SDK not present, skipping attribution init");
            return false;
        } catch (Exception e) {
            SmartAdsLogger.d("Adjust SDK not present, skipping attribution init: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tracks a custom Adjust event token via reflection.
     *
     * @param eventToken The Adjust event token (e.g., from Adjust dashboard).
     * @return true if the event was dispatched successfully to Adjust, false otherwise.
     */
    public static boolean trackEvent(String eventToken) {
        if (eventToken == null || eventToken.trim().isEmpty()) {
            return false;
        }

        try {
            Class<?> adjustEventClass = Class.forName(ADJUST_EVENT_CLASS);
            Constructor<?> eventConstructor = adjustEventClass.getConstructor(String.class);
            Object eventInstance = eventConstructor.newInstance(eventToken);

            Class<?> adjustClass = Class.forName(ADJUST_CLASS);
            Method trackEventMethod = adjustClass.getMethod("trackEvent", adjustEventClass);
            trackEventMethod.invoke(null, eventInstance);
            return true;
        } catch (ClassNotFoundException e) {
            SmartAdsLogger.d("Adjust SDK not present, skipping event track for token: " + eventToken);
            return false;
        } catch (Exception e) {
            SmartAdsLogger.d("Failed to track Adjust event via reflection: " + e.getMessage());
            return false;
        }
    }
}
