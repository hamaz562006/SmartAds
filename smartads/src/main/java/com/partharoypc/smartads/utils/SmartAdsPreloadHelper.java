package com.partharoypc.smartads.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.partharoypc.smartads.SmartAds;
import com.partharoypc.smartads.SmartAdsLogger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Manages automatic and configuration-driven preloading of ads per-Activity and per-Fragment.
 * Supports defining preload rules locally or dynamically via Remote Config keys
 * (e.g. PRELOAD_MainActivity = "INTERSTITIAL,REWARDED").
 */
public final class SmartAdsPreloadHelper {

    private static final Map<String, Set<String>> screenPreloadMap = Collections.synchronizedMap(new HashMap<>());
    private static boolean isAutoLifecycleRegistered = false;

    private SmartAdsPreloadHelper() {
    }

    /**
     * Registers preload helper with application and config rules.
     *
     * @param application Application instance.
     * @param config      SmartAdsConfig containing screenPreloadRules.
     */
    public static void register(Application application, com.partharoypc.smartads.SmartAdsConfig config) {
        if (application == null) return;
        if (config != null && config.getScreenPreloadRules() != null) {
            applyPreloadRules(config.getScreenPreloadRules());
        }
        enableAutoActivityPreload(application);
    }

    /**
     * Registers a preload rule for a specific screen/activity/fragment name.
     *
     * @param screenName Screen or Class simple name (e.g. "MainActivity", "DetailFragment").
     * @param formats    Ad formats to preload (e.g. "INTERSTITIAL", "REWARDED", "APP_OPEN").
     */
    public static void registerPreloadForScreen(String screenName, String... formats) {
        if (screenName == null || formats == null) return;
        String normalizedKey = normalizeScreenName(screenName);
        Set<String> formatSet = screenPreloadMap.computeIfAbsent(normalizedKey, k -> new HashSet<>());
        for (String fmt : formats) {
            if (fmt != null && !fmt.trim().isEmpty()) {
                formatSet.add(fmt.trim().toUpperCase(Locale.ROOT));
            }
        }
        SmartAdsLogger.d("Registered preload rule for [" + normalizedKey + "]: " + formatSet);
    }

    /**
     * Applies a batch of preload rules (e.g. parsed from Remote Config).
     *
     * @param rules Map of screenName -> comma-separated ad formats.
     */
    public static void applyPreloadRules(Map<String, String> rules) {
        if (rules == null) return;
        for (Map.Entry<String, String> entry : rules.entrySet()) {
            String screen = entry.getKey();
            String formatsStr = entry.getValue();
            if (screen != null && formatsStr != null) {
                String[] formats = formatsStr.split("[,;|]");
                registerPreloadForScreen(screen, formats);
            }
        }
    }

    /**
     * Executes ad preloading for a given screen name according to its registered rules.
     *
     * @param context    Context / Activity.
     * @param screenName Screen identifier.
     */
    public static void preloadForScreen(Context context, String screenName) {
        if (context == null || screenName == null || !SmartAds.isInitialized()) return;
        if (!SmartAds.getInstance().canShowAds()) return;

        String normalizedKey = normalizeScreenName(screenName);
        Set<String> formats = screenPreloadMap.get(normalizedKey);
        if (formats == null || formats.isEmpty()) {
            return;
        }

        SmartAdsLogger.d("Executing automatic preload for screen [" + normalizedKey + "]: " + formats);
        SmartAds smartAds = SmartAds.getInstance();

        for (String format : formats) {
            switch (format) {
                case "INTERSTITIAL":
                case "IV":
                    smartAds.loadInterstitialAd(context);
                    break;
                case "REWARDED":
                case "RV":
                    smartAds.loadRewardedAd(context);
                    break;
                case "REWARDED_INTERSTITIAL":
                case "RI":
                    smartAds.loadRewardedInterstitialAd(context);
                    break;
                case "APP_OPEN":
                case "AO":
                    smartAds.preloadAds(context);
                    break;
                default:
                    SmartAdsLogger.d("Preload format not supported for background preload: " + format);
                    break;
            }
        }
    }

    /**
     * Enables automatic Activity lifecycle preloading. When enabled, any Activity that matches
     * a registered preload rule will automatically trigger preloads in onActivityResumed.
     *
     * @param application Android Application instance.
     */
    public static synchronized void enableAutoActivityPreload(Application application) {
        if (application == null || isAutoLifecycleRegistered) return;
        isAutoLifecycleRegistered = true;

        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                if (!(activity instanceof com.partharoypc.smartads.house.HouseInterstitialActivity)) {
                    String simpleName = activity.getClass().getSimpleName();
                    preloadForScreen(activity, simpleName);
                }
            }

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
            @Override
            public void onActivityStarted(@NonNull Activity activity) {}
            @Override
            public void onActivityPaused(@NonNull Activity activity) {}
            @Override
            public void onActivityStopped(@NonNull Activity activity) {}
            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });

        SmartAdsLogger.d("Automatic Activity Preload Lifecycle registered.");
    }

    private static String normalizeScreenName(String screenName) {
        String cleaned = screenName.trim();
        if (cleaned.startsWith("PRELOAD_") || cleaned.startsWith("preload_")) {
            cleaned = cleaned.substring(8);
        }
        return cleaned.toLowerCase(Locale.ROOT);
    }
}
