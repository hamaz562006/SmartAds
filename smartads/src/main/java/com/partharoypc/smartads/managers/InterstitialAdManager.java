package com.partharoypc.smartads.managers;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.partharoypc.smartads.AdStatus;
import com.partharoypc.smartads.SmartAds;
import com.partharoypc.smartads.SmartAdsConfig;
import com.partharoypc.smartads.TestAdIds;
import com.partharoypc.smartads.SmartAdsLogger;
import com.partharoypc.smartads.house.HouseAd;
import com.partharoypc.smartads.house.HouseAdLoader;
import com.partharoypc.smartads.house.HouseInterstitialActivity;
import com.partharoypc.smartads.listeners.InterstitialAdListener;

import java.util.List;

/**
 * Manages Interstitial Ads (full-screen static/video ads).
 */
public class InterstitialAdManager extends BaseFullScreenAdManager {
    private InterstitialAd admobInterstitial;
    private InterstitialAdListener developerListener;
    private boolean isHouseAdReady = false;
    private HouseAd selectedHouseAd;
    private int selectedHouseAdIndex = -1;
    private final java.util.concurrent.atomic.AtomicInteger clickCounter = new java.util.concurrent.atomic.AtomicInteger(0);

    /**
     * Shows an Interstitial Ad every 'interval' clicks/triggers.
     * If the interval is not reached, proceeds directly by invoking onAdDismissed().
     *
     * @param activity      Current activity.
     * @param clickInterval Number of actions between ad displays (e.g., show every 3 clicks).
     * @param listener      Ad lifecycle listener.
     */
    public void showAdWithInterval(Activity activity, int clickInterval, InterstitialAdListener listener) {
        if (clickInterval <= 1) {
            showAd(activity, listener);
            return;
        }

        int currentCount = clickCounter.incrementAndGet();
        SmartAdsLogger.d("Interstitial Click Counter: " + currentCount + " / " + clickInterval);

        if (currentCount % clickInterval == 0) {
            SmartAdsLogger.d("Interstitial Interval Reached (" + clickInterval + "). Showing ad.");
            showAd(activity, listener);
        } else {
            SmartAdsLogger.d("Interstitial Interval not reached yet. Skipping ad.");
            if (listener != null) {
                listener.onAdDismissed();
            }
        }
    }

    public int getClickCount() {
        return clickCounter.get();
    }

    public void resetClickCount() {
        clickCounter.set(0);
    }

    /**
     * Loads an Interstitial Ad.
     */
    public void loadAd(Context context, SmartAdsConfig config) {
        if (!SmartAds.getInstance().areAdsEnabled() || !config.isInterstitialEnabled()) {
            SmartAdsLogger.d("Interstitial Ad is disabled. Skipping request.");
            return;
        }
        if (adStatus == AdStatus.LOADING || adStatus == AdStatus.LOADED || isLoading) {
            return;
        }
        adStatus = AdStatus.LOADING;
        isLoading = true;
        isHouseAdReady = false;
        selectedHouseAd = null;
        selectedHouseAdIndex = -1;

        if (config.getInterstitialSource() == com.partharoypc.smartads.AdSource.HOUSE) {
            SmartAdsLogger.d("Interstitial source set to HOUSE. Loading House Interstitial directly.");
            if (!loadHouseAd(context, config)) {
                adStatus = AdStatus.IDLE;
                isLoading = false;
            }
            return;
        }

        SmartAdsLogger.d("Loading Interstitial Ad...");
        loadAdMob(context, config);
    }

    private void loadAdMob(Context context, SmartAdsConfig config) {
        if (checkNetworkAndFallback(context, config, () -> {
            if (config.isHouseAdsAutoFallback() && loadHouseAd(context, config)) {
                SmartAdsLogger.d("Fallback to House Interstitial (Offline).");
            } else {
                adStatus = AdStatus.IDLE;
                isLoading = false;
                dismissLoadingDialog();
            }
        })) {
            return;
        }
        String adUnitId = (config.getAdMobInterstitialId() != null && !config.getAdMobInterstitialId().isEmpty())
                ? config.getAdMobInterstitialId()
                : (config.isTestMode() ? TestAdIds.ADMOB_INTERSTITIAL_ID : null);
        if (adUnitId == null || adUnitId.isEmpty()) {
            if (config.isHouseAdsAutoFallback() && config.isHouseAdsEnabled()) {
                SmartAdsLogger.d("AdMob Int. ID not set. Trying House Ad.");
                loadHouseAd(context, config);
            } else {
                adStatus = AdStatus.FAILED;
                isLoading = false;
                dismissLoadingDialog();
            }
            return;
        }

        // 3. AdMob NO_FILL Rate Limiting
        if (com.partharoypc.smartads.utils.AdMobRateLimiter.isRateLimited(adUnitId)) {
            SmartAdsLogger.d("AdMob Rate Limiter active (NO_FILL Cooldown). Skipping AdMob Interstitial Request.");
            if (config.isHouseAdsAutoFallback() && config.isHouseAdsEnabled()) {
                loadHouseAd(context, config);
            } else {
                adStatus = AdStatus.IDLE;
                isLoading = false;
                dismissLoadingDialog();
            }
            return;
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(context, adUnitId, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                SmartAdsLogger.d("✅ Interstitial Ad LOADED.");
                admobInterstitial = interstitialAd;
                interstitialAd.setOnPaidEventListener(adValue -> {
                    SmartAds.getInstance().reportPaidEvent(adValue, interstitialAd.getResponseInfo(), adUnitId,
                            "Interstitial");
                });
                onAdLoadedBase();
                checkPendingShow();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                SmartAdsLogger.e("❌ Interstitial Failed to Load: " + loadAdError.getMessage());
                if (loadAdError.getCode() == com.google.android.gms.ads.AdRequest.ERROR_CODE_NO_FILL) {
                    com.partharoypc.smartads.utils.AdMobRateLimiter.recordNoFill(adUnitId);
                }
                // FALLBACK TO HOUSE AD
                if (config.isHouseAdsAutoFallback() && loadHouseAd(context, config)) {
                    SmartAdsLogger.d("Fallback to House Interstitial.");
                    return;
                }

                onAdFailedToLoadBase();

                if (isShowPending && developerListener != null) {
                    developerListener.onAdFailedToShow(loadAdError.getMessage());
                }
                isShowPending = false;
                pendingActivity = null;

                scheduleRetry(context, config, loadAdError, () -> loadAd(context, config));
            }
        });
    }

    private boolean loadHouseAd(Context context, SmartAdsConfig config) {
        if (!config.isHouseAdsEnabled()) {
            return false;
        }
        List<HouseAd> houseAds = config.getHouseAds();
        selectedHouseAd = HouseAdLoader.selectAd(houseAds);
        if (selectedHouseAd != null) {
            selectedHouseAdIndex = houseAds.indexOf(selectedHouseAd);
            isHouseAdReady = true;
            onAdLoadedBase();
            checkPendingShow();
            return true;
        }
        return false;
    }

    private void checkPendingShow() {
        if (isShowPending && pendingActivity != null) {
            showAd(pendingActivity, developerListener);
            isShowPending = false;
            pendingActivity = null;
        }
    }

    /**
     * Shows the Interstitial Ad if loaded.
     */
    public void showAd(Activity activity, InterstitialAdListener listener) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (listener != null)
                listener.onAdFailedToShow("Activity is invalid.");
            return;
        }

        this.developerListener = listener;

        if (adStatus == AdStatus.LOADING) {
            SmartAdsLogger.d("Ad Loading... Activity waiting.");
            isShowPending = true;
            pendingActivity = activity;
            showLoadingDialog(activity);
            return;
        }

        SmartAdsConfig config = SmartAds.getInstance().getConfig();
        long ivInterval = config != null ? config.getInterstitialIntervalSeconds() : 30L;
        long rvDelay = config != null ? config.getDelayAfterRewardedSeconds() : 30L;

        if (!AdFrequencyManager.getInstance().canShowInterstitial(ivInterval, rvDelay) || isFrequencyCapped(config)) {
            SmartAdsLogger.d("Ad Frequency Capped (time-based interval). Checking House Ad fallback...");
            if (config != null && config.isHouseAdsEnabled() && !config.getHouseAds().isEmpty()) {
                if (isHouseAdReady && selectedHouseAd != null) {
                    SmartAdsLogger.d("Showing ready House Interstitial due to frequency cap.");
                    showHouseInterstitial(activity);
                    return;
                } else if (loadHouseAd(activity, config)) {
                    SmartAdsLogger.d("Showing newly loaded House Interstitial due to frequency cap.");
                    showHouseInterstitial(activity);
                    return;
                }
            }
            SmartAdsLogger.d("Ad Frequency Capped and no House Ad available. Skipping.");
            if (listener != null)
                listener.onAdFailedToShow("Ad is frequency capped.");
            return;
        }

        if (adStatus != AdStatus.LOADED) {
            // Not loaded -> load and wait
            SmartAdsLogger.d("Ad not loaded. Starting load...");

            // Check if we can load anything (Network OR House Ads)
            boolean isNetworkAvailable = com.partharoypc.smartads.utils.NetworkUtils.isNetworkAvailable(activity);
            boolean hasHouseAds = config != null && config.isHouseAdsEnabled() && !config.getHouseAds().isEmpty();

            if (!isNetworkAvailable && !hasHouseAds) {
                SmartAdsLogger.d("No Internet and No House Ads. Cannot show ad.");
                if (listener != null) {
                    listener.onAdFailedToShow("No Internet connection and no offline ads available.");
                }
                return;
            }

            isShowPending = true;
            pendingActivity = activity;
            showLoadingDialog(activity);
            loadAd(activity, config);
            return;
        }

        if (admobInterstitial != null) {
            SmartAdsLogger.d("Showing AdMob Interstitial.");
            showAdMobInterstitial(activity);
        } else if (isHouseAdReady && selectedHouseAd != null) {
            SmartAdsLogger.d("Showing House Interstitial.");
            showHouseInterstitial(activity);
        } else {
            if (developerListener != null)
                listener.onAdFailedToShow("Ad not loaded.");
        }
    }

    private void showAdMobInterstitial(Activity activity) {
        admobInterstitial.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                SmartAdsLogger.d("Interstitial Dismissed.");
                if (developerListener != null)
                    developerListener.onAdDismissed();
                admobInterstitial = null;
                adStatus = AdStatus.IDLE;
                if (isAutoReloadEnabled) {
                    loadAd(activity, SmartAds.getInstance().getConfig());
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                SmartAdsLogger.e("Interstitial Failed to Show: " + adError.getMessage());
                if (developerListener != null) {
                    developerListener.onAdFailedToShow(adError.getMessage());
                }
                admobInterstitial = null;
                adStatus = AdStatus.IDLE;
                if (isAutoReloadEnabled) {
                    loadAd(activity, SmartAds.getInstance().getConfig());
                }
            }

            @Override
            public void onAdShowedFullScreenContent() {
                adStatus = AdStatus.SHOWN;
                lastShownTime = System.currentTimeMillis();
                AdFrequencyManager.getInstance().recordInterstitialShown();
                if (developerListener != null) {
                    developerListener.onAdImpression();
                }
            }

            @Override
            public void onAdClicked() {
                if (developerListener != null) {
                    developerListener.onAdClicked();
                }
            }
        });
        admobInterstitial.show(activity);
    }

    private void showHouseInterstitial(Activity activity) {
        HouseInterstitialActivity.start(activity, selectedHouseAdIndex,
                new HouseInterstitialActivity.HouseInterstitialListener() {
                    @Override
                    public void onAdDismissed() {
                        SmartAdsLogger.d("House Interstitial Dismissed.");
                        if (developerListener != null)
                            developerListener.onAdDismissed();
                        isHouseAdReady = false;
                        adStatus = AdStatus.IDLE;
                        if (isAutoReloadEnabled) {
                            loadAd(activity, SmartAds.getInstance().getConfig());
                        }
                    }

                    @Override
                    public void onAdClicked() {
                        if (developerListener != null)
                            developerListener.onAdClicked();
                    }

                    @Override
                    public void onAdImpression() {
                        adStatus = AdStatus.SHOWN;
                        lastShownTime = System.currentTimeMillis();
                        AdFrequencyManager.getInstance().recordInterstitialShown();
                        if (developerListener != null)
                            developerListener.onAdImpression();
                    }
                });
    }

    @Override
    protected void onLoadingTimeout() {
        super.onLoadingTimeout();
        if (developerListener != null) {
            developerListener.onAdFailedToShow("Ad load timed out (5s).");
        }
    }
}
