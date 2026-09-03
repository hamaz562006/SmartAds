package com.partharoypc.smartads.managers;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.partharoypc.smartads.AdStatus;
import com.partharoypc.smartads.SmartAds;
import com.partharoypc.smartads.SmartAdsConfig;
import com.partharoypc.smartads.TestAdIds;
import com.partharoypc.smartads.SmartAdsLogger;
import com.partharoypc.smartads.house.HouseAd;
import com.partharoypc.smartads.house.HouseAdLoader;
import com.partharoypc.smartads.house.HouseInterstitialActivity;
import com.partharoypc.smartads.listeners.RewardedInterstitialAdListener;
import com.partharoypc.smartads.analytics.SmartAdsLifecycleListener;

import java.util.List;

/**
 * Manages Rewarded Interstitial Ads (hybrid rewarded format that does not require opt-in).
 */
public class RewardedInterstitialAdManager extends BaseFullScreenAdManager {
    private RewardedInterstitialAd admobRewardedInterstitialAd;
    private RewardedInterstitialAdListener developerListener;

    private boolean isHouseAdReady = false;
    private HouseAd selectedHouseAd;
    private int selectedHouseAdIndex = -1;

    /**
     * Loads a Rewarded Interstitial Ad.
     */
    public void loadAd(Context context, SmartAdsConfig config) {
        if (!SmartAds.getInstance().areAdsEnabled() || !config.isRewardedInterstitialEnabled()) {
            SmartAdsLogger.d("Rewarded Interstitial Ad is disabled. Skipping request.");
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

        if (config.getRewardedInterstitialSource() == com.partharoypc.smartads.AdSource.HOUSE) {
            SmartAdsLogger.d("Rewarded Interstitial source set to HOUSE. Loading House Ad directly.");
            if (!loadHouseAd(context, config)) {
                adStatus = AdStatus.IDLE;
                isLoading = false;
            }
            return;
        }

        SmartAdsLogger.d("Loading Rewarded Interstitial Ad...");
        loadAdMob(context, config);
    }

    private void loadAdMob(Context context, SmartAdsConfig config) {
        if (checkNetworkAndFallback(context, config, () -> {
            if (config.isHouseAdsAutoFallback() && loadHouseAd(context, config)) {
                SmartAdsLogger.d("Fallback to House Rewarded Interstitial Ad (Offline).");
            } else {
                adStatus = AdStatus.IDLE;
                isLoading = false;
                dismissLoadingDialog();
            }
        })) {
            return;
        }
        String adUnitId = (config.getAdMobRewardedInterstitialId() != null && !config.getAdMobRewardedInterstitialId().isEmpty())
                ? config.getAdMobRewardedInterstitialId()
                : (config.isTestMode() ? TestAdIds.ADMOB_REWARDED_INTERSTITIAL_ID : null);
        if (adUnitId == null || adUnitId.isEmpty()) {
            if (config.isHouseAdsAutoFallback() && config.isHouseAdsEnabled()) {
                SmartAdsLogger.d("AdMob Rewarded Interstitial ID not set. Trying House Ad.");
                loadHouseAd(context, config);
            } else {
                adStatus = AdStatus.FAILED;
                isLoading = false;
                dismissLoadingDialog();
            }
            return;
        }

        // AdMob NO_FILL Rate Limiting
        if (com.partharoypc.smartads.utils.AdMobRateLimiter.isRateLimited(adUnitId)) {
            SmartAdsLogger.d("AdMob Rate Limiter active (NO_FILL Cooldown). Skipping AdMob Rewarded Interstitial Request.");
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
        SmartAdsLifecycleListener llInit = SmartAds.getInstance().getLifecycleListener();
        if (llInit != null) {
            llInit.onAdLoadStarted("RewardedInterstitial", "ADMOB");
        }
        RewardedInterstitialAd.load(context, adUnitId, adRequest, new RewardedInterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedInterstitialAd rewardedInterstitialAd) {
                SmartAdsLogger.d("✅ Rewarded Interstitial Ad LOADED.");
                SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                if (ll != null) {
                    ll.onAdLoadSuccess("RewardedInterstitial", "ADMOB");
                }
                admobRewardedInterstitialAd = rewardedInterstitialAd;
                rewardedInterstitialAd.setOnPaidEventListener(adValue -> {
                    SmartAds.getInstance().reportPaidEvent(adValue, rewardedInterstitialAd.getResponseInfo(), adUnitId, "RewardedInterstitial");
                });
                onAdLoadedBase();
                checkPendingShow();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                SmartAdsLogger.e("❌ Rewarded Interstitial Ad Failed to Load: " + loadAdError.getMessage());
                SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                if (ll != null) {
                    ll.onAdLoadFailed("RewardedInterstitial", "ADMOB", loadAdError.getMessage());
                }
                if (loadAdError.getCode() == com.google.android.gms.ads.AdRequest.ERROR_CODE_NO_FILL) {
                    com.partharoypc.smartads.utils.AdMobRateLimiter.recordNoFill(adUnitId);
                }
                // Fallback to House Ad
                if (config.isHouseAdsAutoFallback() && loadHouseAd(context, config)) {
                    SmartAdsLogger.d("Fallback to House Rewarded Interstitial Ad.");
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
        SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
        if (ll != null) {
            ll.onAdLoadStarted("RewardedInterstitial", "HOUSE");
        }
        List<HouseAd> houseAds = config.getHouseAds();
        selectedHouseAd = HouseAdLoader.selectAd(houseAds);
        if (selectedHouseAd != null) {
            selectedHouseAdIndex = houseAds.indexOf(selectedHouseAd);
            isHouseAdReady = true;
            if (ll != null) {
                ll.onAdLoadSuccess("RewardedInterstitial", "HOUSE");
            }
            onAdLoadedBase();
            checkPendingShow();
            return true;
        }
        if (ll != null) {
            ll.onAdLoadFailed("RewardedInterstitial", "HOUSE", "No House Ad selected.");
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
     * Shows the Rewarded Interstitial Ad.
     */
    public void showAd(Activity activity, RewardedInterstitialAdListener listener) {
        if (!SmartAds.getInstance().areAdsEnabled() || !SmartAds.getInstance().getConfig().isRewardedInterstitialEnabled()) {
            SmartAdsLogger.d("Rewarded Interstitial Ad is disabled. Cannot show.");
            if (listener != null)
                listener.onAdFailedToShow("Rewarded Interstitial ads are disabled.");
            return;
        }

        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (listener != null)
                listener.onAdFailedToShow("Activity is null or finishing.");
            return;
        }

        if (isFrequencyCapped(SmartAds.getInstance().getConfig())) {
            SmartAdsConfig config = SmartAds.getInstance().getConfig();
            SmartAdsLogger.d("Rewarded Interstitial Ad frequency capped. Checking House Ad fallback...");
            if (config != null && config.isHouseAdsEnabled() && !config.getHouseAds().isEmpty()) {
                if (isHouseAdReady && selectedHouseAd != null) {
                    SmartAdsLogger.d("Showing ready House Rewarded Interstitial Ad due to frequency cap.");
                    showHouseRewardedInterstitial(activity);
                    return;
                } else if (loadHouseAd(activity, config)) {
                    SmartAdsLogger.d("Showing newly loaded House Rewarded Interstitial Ad due to frequency cap.");
                    showHouseRewardedInterstitial(activity);
                    return;
                }
            }
            SmartAdsLogger.d("Rewarded Interstitial Ad frequency capped and no House Ad available. Skipping show.");
            if (listener != null)
                listener.onAdFailedToShow("Frequency Capped");
            return;
        }

        this.developerListener = listener;

        if (adStatus == AdStatus.LOADED && admobRewardedInterstitialAd != null) {
            showAdMobRewardedInterstitial(activity);
        } else if (isHouseAdReady && selectedHouseAd != null) {
            showHouseRewardedInterstitial(activity);
        } else if (adStatus == AdStatus.LOADING || isLoading) {
            SmartAdsLogger.d("Rewarded Interstitial Ad is currently loading. Queuing show when loaded.");
            isShowPending = true;
            pendingActivity = activity;
            showLoadingDialog(activity);
        } else {
            SmartAdsLogger.d("Rewarded Interstitial Ad not ready. Requesting and queuing show.");
            isShowPending = true;
            pendingActivity = activity;
            showLoadingDialog(activity);
            loadAd(activity, SmartAds.getInstance().getConfig());
        }
    }

    private void showAdMobRewardedInterstitial(Activity activity) {
        admobRewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                SmartAdsLogger.d("Rewarded Interstitial Ad Dismissed.");
                SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                if (ll != null) {
                    ll.onAdClosed("RewardedInterstitial", "ADMOB");
                }
                admobRewardedInterstitialAd = null;
                adStatus = AdStatus.IDLE;
                if (developerListener != null)
                    developerListener.onAdDismissed();
                if (isAutoReloadEnabled) {
                    loadAd(activity.getApplicationContext(), SmartAds.getInstance().getConfig());
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                SmartAdsLogger.e("Rewarded Interstitial Ad Failed to Show: " + adError.getMessage());
                SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                if (ll != null) {
                    ll.onAdShowFailed("RewardedInterstitial", "ADMOB", adError.getMessage());
                }
                admobRewardedInterstitialAd = null;
                adStatus = AdStatus.FAILED;
                if (developerListener != null)
                    developerListener.onAdFailedToShow(adError.getMessage());
                if (isAutoReloadEnabled) {
                    loadAd(activity.getApplicationContext(), SmartAds.getInstance().getConfig());
                }
            }

            @Override
            public void onAdShowedFullScreenContent() {
                SmartAdsLogger.d("Rewarded Interstitial Ad Shown.");
                adStatus = AdStatus.SHOWN;
                lastShownTime = System.currentTimeMillis();
                AdFrequencyManager.getInstance().recordRewardedShown();
                SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                if (ll != null) {
                    ll.onAdShowSuccess("RewardedInterstitial", "ADMOB");
                }
                dismissLoadingDialog();
            }

            @Override
            public void onAdImpression() {
                SmartAdsLogger.d("Rewarded Interstitial Ad Impression.");
                if (developerListener != null)
                    developerListener.onAdImpression();
            }

            @Override
            public void onAdClicked() {
                SmartAdsLogger.d("Rewarded Interstitial Ad Clicked.");
                SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                if (ll != null) {
                    ll.onAdClicked("RewardedInterstitial", "ADMOB");
                }
                if (developerListener != null)
                    developerListener.onAdClicked();
            }
        });

        admobRewardedInterstitialAd.show(activity, rewardItem -> {
            SmartAdsLogger.d("User Earned Reward from Rewarded Interstitial!");
            if (developerListener != null) {
                developerListener.onUserEarnedReward();
            }
        });
    }

    private void showHouseRewardedInterstitial(Activity activity) {
        HouseInterstitialActivity.start(activity, selectedHouseAdIndex,
                new HouseInterstitialActivity.HouseInterstitialListener() {
                    @Override
                    public void onAdDismissed() {
                        SmartAdsLogger.d("House Rewarded Interstitial Dismissed.");
                        SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                        if (ll != null) {
                            ll.onAdClosed("RewardedInterstitial", "HOUSE");
                        }
                        adStatus = AdStatus.IDLE;
                        isHouseAdReady = false;
                        selectedHouseAd = null;
                        if (developerListener != null) {
                            developerListener.onUserEarnedReward();
                            developerListener.onAdDismissed();
                        }
                        if (isAutoReloadEnabled) {
                            loadAd(activity.getApplicationContext(), SmartAds.getInstance().getConfig());
                        }
                    }

                    @Override
                    public void onAdClicked() {
                        SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                        if (ll != null) {
                            ll.onAdClicked("RewardedInterstitial", "HOUSE");
                        }
                        if (developerListener != null)
                            developerListener.onAdClicked();
                    }

                    @Override
                    public void onAdImpression() {
                        adStatus = AdStatus.SHOWN;
                        lastShownTime = System.currentTimeMillis();
                        AdFrequencyManager.getInstance().recordRewardedShown();
                        SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                        if (ll != null) {
                            ll.onAdShowSuccess("RewardedInterstitial", "HOUSE");
                        }
                        dismissLoadingDialog();
                        if (developerListener != null)
                            developerListener.onAdImpression();
                    }
                });
    }
}
