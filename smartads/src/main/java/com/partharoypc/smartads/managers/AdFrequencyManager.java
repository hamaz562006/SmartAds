package com.partharoypc.smartads.managers;

import android.content.Context;
import com.partharoypc.smartads.SmartAdsConfig;
import com.partharoypc.smartads.SmartAdsLogger;
import com.partharoypc.smartads.utils.SmartAdsPreferencesHelper;

/**
 * Manages time-based cross-format frequency capping for full-screen ads
 * (Interstitial, Rewarded, Rewarded Interstitial, and App Open).
 *
 * Enforces:
 * 1. Minimum interval between consecutive Interstitial ads.
 * 2. Minimum cooldown/delay after showing a Rewarded ad before showing an Interstitial ad
 *    (preventing user ad-bombardment right after watching a rewarded ad).
 * 3. Minimum interval between consecutive App Open ads.
 */
public class AdFrequencyManager {

    public static final String KEY_LAST_INTERSTITIAL = "freq_last_iv";
    public static final String KEY_LAST_REWARDED = "freq_last_rv";
    public static final String KEY_LAST_APP_OPEN = "freq_last_ao";

    private static volatile AdFrequencyManager instance;

    private volatile long lastInterstitialShowTime = 0L;
    private volatile long lastRewardedShowTime = 0L;
    private volatile long lastAppOpenShowTime = 0L;

    private volatile long interstitialIntervalMs = 30_000L;    // default 30s
    private volatile long delayAfterRewardedMs = 30_000L;      // default 30s
    private volatile long appOpenIntervalMs = 15_000L;         // default 15s

    private AdFrequencyManager() {
    }

    public static AdFrequencyManager getInstance() {
        if (instance == null) {
            synchronized (AdFrequencyManager.class) {
                if (instance == null) {
                    instance = new AdFrequencyManager();
                }
            }
        }
        return instance;
    }

    /**
     * Initializes timestamps from persisted preferences so frequency limits survive process death.
     */
    public void init(Context context) {
        try {
            SmartAdsPreferencesHelper prefs;
            try {
                prefs = SmartAdsPreferencesHelper.getInstance();
            } catch (IllegalStateException e) {
                SmartAdsPreferencesHelper.init(context);
                prefs = SmartAdsPreferencesHelper.getInstance();
            }
            this.lastInterstitialShowTime = prefs.getLong(KEY_LAST_INTERSTITIAL, 0L);
            this.lastRewardedShowTime = prefs.getLong(KEY_LAST_REWARDED, 0L);
            this.lastAppOpenShowTime = prefs.getLong(KEY_LAST_APP_OPEN, 0L);
            SmartAdsLogger.d("AdFrequencyManager initialized: IV=" + lastInterstitialShowTime
                    + ", RV=" + lastRewardedShowTime + ", AO=" + lastAppOpenShowTime);
        } catch (Exception e) {
            SmartAdsLogger.e("Failed to initialize AdFrequencyManager from preferences: " + e.getMessage());
        }
    }

    /**
     * Updates frequency limits from SmartAdsConfig.
     */
    public void updateConfig(SmartAdsConfig config) {
        if (config != null) {
            this.interstitialIntervalMs = config.getInterstitialIntervalSeconds() * 1000L;
            this.delayAfterRewardedMs = config.getDelayAfterRewardedSeconds() * 1000L;
            this.appOpenIntervalMs = config.getAppOpenIntervalSeconds() * 1000L;
            SmartAdsLogger.d(String.format("AdFrequencyManager updated: InterstitialInterval=%ds, RewardedToInterstitialDelay=%ds, AppOpenInterval=%ds",
                    config.getInterstitialIntervalSeconds(),
                    config.getDelayAfterRewardedSeconds(),
                    config.getAppOpenIntervalSeconds()));
        }
    }

    /**
     * Checks if an Interstitial ad can be displayed based on both Interstitial interval
     * and Rewarded-to-Interstitial cooldown.
     *
     * @return true if allowed to show, false if frequency-capped.
     */
    public boolean canShowInterstitial() {
        return canShowInterstitial(interstitialIntervalMs / 1000L, delayAfterRewardedMs / 1000L);
    }

    /**
     * Checks if an Interstitial ad can be displayed based on custom Interstitial interval
     * and Rewarded-to-Interstitial cooldown in seconds.
     */
    public boolean canShowInterstitial(long intervalSeconds, long delayAfterRewardedSeconds) {
        long now = System.currentTimeMillis();
        long intervalMs = intervalSeconds * 1000L;
        long delayMs = delayAfterRewardedSeconds * 1000L;

        // 1. Check time since last Interstitial
        if (lastInterstitialShowTime > 0) {
            long elapsedSinceInterstitial = now - lastInterstitialShowTime;
            if (elapsedSinceInterstitial < intervalMs) {
                long remainingSec = (intervalMs - elapsedSinceInterstitial) / 1000L;
                SmartAdsLogger.d("⏳ Interstitial frequency capped: " + remainingSec + "s remaining before next Interstitial.");
                return false;
            }
        }

        // 2. Check time since last Rewarded (Cross-format cooldown rule)
        if (lastRewardedShowTime > 0) {
            long elapsedSinceRewarded = now - lastRewardedShowTime;
            if (elapsedSinceRewarded < delayMs) {
                long remainingSec = (delayMs - elapsedSinceRewarded) / 1000L;
                SmartAdsLogger.d("⏳ Interstitial capped by post-Rewarded cooldown: " + remainingSec + "s remaining after Rewarded ad.");
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if an App Open ad can be displayed based on App Open interval.
     *
     * @return true if allowed to show, false if frequency-capped.
     */
    public boolean canShowAppOpen() {
        return canShowAppOpen(appOpenIntervalMs / 1000L);
    }

    /**
     * Checks if an App Open ad can be displayed based on custom App Open interval in seconds.
     */
    public boolean canShowAppOpen(long intervalSeconds) {
        long now = System.currentTimeMillis();
        long intervalMs = intervalSeconds * 1000L;
        if (lastAppOpenShowTime > 0) {
            long elapsed = now - lastAppOpenShowTime;
            if (elapsed < intervalMs) {
                long remainingSec = (intervalMs - elapsed) / 1000L;
                SmartAdsLogger.d("⏳ App Open ad frequency capped: " + remainingSec + "s remaining.");
                return false;
            }
        }
        return true;
    }

    /**
     * Records the presentation of an Interstitial ad.
     */
    public void recordInterstitialShown() {
        this.lastInterstitialShowTime = System.currentTimeMillis();
        SmartAdsLogger.d("Recorded Interstitial ad impression timestamp: " + lastInterstitialShowTime);
        try {
            SmartAdsPreferencesHelper.getInstance().saveLong(KEY_LAST_INTERSTITIAL, lastInterstitialShowTime);
        } catch (Exception ignored) {
        }
    }

    /**
     * Records the presentation of a Rewarded or Rewarded Interstitial ad.
     */
    public void recordRewardedShown() {
        this.lastRewardedShowTime = System.currentTimeMillis();
        SmartAdsLogger.d("Recorded Rewarded ad impression timestamp: " + lastRewardedShowTime);
        try {
            SmartAdsPreferencesHelper.getInstance().saveLong(KEY_LAST_REWARDED, lastRewardedShowTime);
        } catch (Exception ignored) {
        }
    }

    /**
     * Records the presentation of an App Open ad.
     */
    public void recordAppOpenShown() {
        this.lastAppOpenShowTime = System.currentTimeMillis();
        SmartAdsLogger.d("Recorded App Open ad impression timestamp: " + lastAppOpenShowTime);
        try {
            SmartAdsPreferencesHelper.getInstance().saveLong(KEY_LAST_APP_OPEN, lastAppOpenShowTime);
        } catch (Exception ignored) {
        }
    }

    /**
     * Resets all timestamps (useful for testing or session reset).
     */
    public void reset() {
        lastInterstitialShowTime = 0L;
        lastRewardedShowTime = 0L;
        lastAppOpenShowTime = 0L;
        try {
            SmartAdsPreferencesHelper prefs = SmartAdsPreferencesHelper.getInstance();
            prefs.saveLong(KEY_LAST_INTERSTITIAL, 0L);
            prefs.saveLong(KEY_LAST_REWARDED, 0L);
            prefs.saveLong(KEY_LAST_APP_OPEN, 0L);
        } catch (Exception ignored) {
        }
    }

    public long getLastInterstitialShowTime() {
        return lastInterstitialShowTime;
    }

    public long getLastRewardedShowTime() {
        return lastRewardedShowTime;
    }

    public long getLastAppOpenShowTime() {
        return lastAppOpenShowTime;
    }
}
