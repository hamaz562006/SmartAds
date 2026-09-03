package com.partharoypc.smartads.analytics;

/**
 * Granular lifecycle listener for SmartAds ad events across formats and sources (AdMob / House).
 */
public interface SmartAdsLifecycleListener {

    void onAdLoadStarted(String adFormat, String adSource);

    void onAdLoadSuccess(String adFormat, String adSource);

    void onAdLoadFailed(String adFormat, String adSource, String error);

    void onAdShowSuccess(String adFormat, String adSource);

    void onAdShowFailed(String adFormat, String adSource, String error);

    void onAdClosed(String adFormat, String adSource);

    void onAdClicked(String adFormat, String adSource);
}
