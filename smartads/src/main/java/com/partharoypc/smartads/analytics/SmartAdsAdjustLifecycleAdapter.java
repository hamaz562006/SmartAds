package com.partharoypc.smartads.analytics;

import com.partharoypc.smartads.SmartAdsLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapter that maps {@link SmartAdsLifecycleListener} ad events into named Adjust events.
 * Uses event token mapping configured by the application developer.
 * <p>
 * Standard internal event names follow the pattern:
 * <code>sa_{format}_{stage}</code>
 * <p>
 * Supported formats:
 * <ul>
 *   <li>interstitial</li>
 *   <li>rewarded</li>
 *   <li>rewarded_interstitial</li>
 *   <li>banner</li>
 *   <li>native</li>
 *   <li>app_open</li>
 * </ul>
 * <p>
 * Supported stages:
 * <ul>
 *   <li>_load</li>
 *   <li>_load_success</li>
 *   <li>_load_fail</li>
 *   <li>_show_success</li>
 *   <li>_show_fail</li>
 *   <li>_closed</li>
 *   <li>_clicked</li>
 * </ul>
 * Example key: <code>sa_interstitial_load_success</code>
 */
public class SmartAdsAdjustLifecycleAdapter implements SmartAdsLifecycleListener {

    private final Map<String, String> eventTokenMap;

    /**
     * Creates an adapter with the provided event token mapping.
     *
     * @param eventTokenMap Map where key is the internal event name (e.g., "sa_interstitial_show_success")
     *                      and value is the Adjust event token string from the Adjust dashboard.
     */
    public SmartAdsAdjustLifecycleAdapter(Map<String, String> eventTokenMap) {
        if (eventTokenMap != null) {
            this.eventTokenMap = new HashMap<>(eventTokenMap);
        } else {
            this.eventTokenMap = Collections.emptyMap();
        }
    }

    private String normalizeFormat(String format) {
        if (format == null) {
            return "unknown";
        }
        String lower = format.toLowerCase().trim();
        switch (lower) {
            case "rewardedinterstitial":
            case "rewarded_interstitial":
                return "rewarded_interstitial";
            case "appopen":
            case "app_open":
                return "app_open";
            case "banner":
                return "banner";
            case "interstitial":
                return "interstitial";
            case "rewarded":
                return "rewarded";
            case "native":
                return "native";
            default:
                return lower;
        }
    }

    private void handleEvent(String format, String stage) {
        if (eventTokenMap.isEmpty()) {
            return;
        }

        String eventKey = "sa_" + normalizeFormat(format) + stage;
        String token = eventTokenMap.get(eventKey);

        if (token == null || token.trim().isEmpty()) {
            return;
        }

        boolean success = SmartAdsAdjustHelper.trackEvent(token.trim());
        if (success) {
            SmartAdsLogger.d("Adjust event tracked: " + eventKey + " -> token: " + token);
        }
    }

    @Override
    public void onAdLoadStarted(String adFormat, String adSource) {
        handleEvent(adFormat, "_load");
    }

    @Override
    public void onAdLoadSuccess(String adFormat, String adSource) {
        handleEvent(adFormat, "_load_success");
    }

    @Override
    public void onAdLoadFailed(String adFormat, String adSource, String errorMessage) {
        handleEvent(adFormat, "_load_fail");
    }

    @Override
    public void onAdShowSuccess(String adFormat, String adSource) {
        handleEvent(adFormat, "_show_success");
    }

    @Override
    public void onAdShowFailed(String adFormat, String adSource, String errorMessage) {
        handleEvent(adFormat, "_show_fail");
    }

    @Override
    public void onAdClosed(String adFormat, String adSource) {
        handleEvent(adFormat, "_closed");
    }

    @Override
    public void onAdClicked(String adFormat, String adSource) {
        handleEvent(adFormat, "_clicked");
    }
}
