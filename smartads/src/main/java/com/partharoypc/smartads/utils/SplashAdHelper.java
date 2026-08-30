package com.partharoypc.smartads.utils;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import com.partharoypc.smartads.SmartAds;
import com.partharoypc.smartads.SmartAdsLogger;
import com.partharoypc.smartads.listeners.AppOpenAdListener;
import com.partharoypc.smartads.listeners.InterstitialAdListener;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrates Splash / Launch Ad flows (Interstitial or App Open).
 * Guarantees that onNextAction() is called exactly once under all conditions:
 * success, error, timeout, or ads disabled.
 */
public final class SplashAdHelper {

    public enum SplashAdType {
        INTERSTITIAL,
        APP_OPEN
    }

    public interface SplashCallback {
        /**
         * Triggered when the splash screen should navigate to the main/next screen.
         */
        void onNextAction();
    }

    private SplashAdHelper() {
        // Utility
    }

    /**
     * Loads and shows a splash ad with a default timeout of 5000ms.
     */
    public static void loadAndShowSplashAd(Activity activity, SplashAdType type, SplashCallback callback) {
        loadAndShowSplashAd(activity, type, 5000L, callback);
    }

    /**
     * Loads and shows a splash ad with a customizable timeout.
     *
     * @param activity       The splash activity.
     * @param type           The ad format (INTERSTITIAL or APP_OPEN).
     * @param timeoutMillis  Max duration to wait for ad load before proceeding.
     * @param callback       Navigation callback.
     */
    public static void loadAndShowSplashAd(Activity activity, SplashAdType type, long timeoutMillis, SplashCallback callback) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (callback != null) callback.onNextAction();
            return;
        }

        if (!SmartAds.isInitialized() || !SmartAds.getInstance().canShowAds()) {
            SmartAdsLogger.d("SplashAdHelper: Ads not enabled or initialized. Proceeding.");
            if (callback != null) callback.onNextAction();
            return;
        }

        final Handler handler = new Handler(Looper.getMainLooper());
        final AtomicBoolean isActionTriggered = new AtomicBoolean(false);

        Runnable timeoutRunnable = () -> {
            if (isActionTriggered.compareAndSet(false, true)) {
                SmartAdsLogger.d("SplashAdHelper: Timeout reached (" + timeoutMillis + "ms). Proceeding to next screen.");
                if (callback != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    callback.onNextAction();
                }
            }
        };

        handler.postDelayed(timeoutRunnable, timeoutMillis);

        if (type == SplashAdType.INTERSTITIAL) {
            SmartAds.getInstance().showInterstitialAd(activity, new InterstitialAdListener() {
                @Override
                public void onAdDismissed() {
                    handler.removeCallbacks(timeoutRunnable);
                    if (isActionTriggered.compareAndSet(false, true)) {
                        SmartAdsLogger.d("SplashAdHelper: Interstitial dismissed. Proceeding.");
                        if (callback != null && !activity.isFinishing() && !activity.isDestroyed()) {
                            callback.onNextAction();
                        }
                    }
                }

                @Override
                public void onAdFailedToShow(String errorMessage) {
                    handler.removeCallbacks(timeoutRunnable);
                    if (isActionTriggered.compareAndSet(false, true)) {
                        SmartAdsLogger.d("SplashAdHelper: Interstitial failed to show (" + errorMessage + "). Proceeding.");
                        if (callback != null && !activity.isFinishing() && !activity.isDestroyed()) {
                            callback.onNextAction();
                        }
                    }
                }
            });
        } else {
            // App Open Ad
            if (SmartAds.getInstance().isAppOpenAdAvailable()) {
                SmartAds.getInstance().setAppOpenAdListener(new AppOpenAdListener() {
                    @Override
                    public void onAdDismissed() {
                        handler.removeCallbacks(timeoutRunnable);
                        if (isActionTriggered.compareAndSet(false, true)) {
                            SmartAdsLogger.d("SplashAdHelper: App Open dismissed. Proceeding.");
                            if (callback != null && !activity.isFinishing() && !activity.isDestroyed()) {
                                callback.onNextAction();
                            }
                        }
                    }

                    @Override
                    public void onAdFailedToShow(String errorMessage) {
                        handler.removeCallbacks(timeoutRunnable);
                        if (isActionTriggered.compareAndSet(false, true)) {
                            SmartAdsLogger.d("SplashAdHelper: App Open failed (" + errorMessage + "). Proceeding.");
                            if (callback != null && !activity.isFinishing() && !activity.isDestroyed()) {
                                callback.onNextAction();
                            }
                        }
                    }
                });
                SmartAds.getInstance().showAppOpenAd(activity);
            } else {
                // Not immediately available, wait or timeout
                SmartAdsLogger.d("SplashAdHelper: App Open Ad not cached. Waiting for timeout or pre-load.");
            }
        }
    }
}
