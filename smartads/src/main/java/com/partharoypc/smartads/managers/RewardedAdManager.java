package com.partharoypc.smartads.managers;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.partharoypc.smartads.AdStatus;
import com.partharoypc.smartads.SmartAds;
import com.partharoypc.smartads.SmartAdsConfig;
import com.partharoypc.smartads.TestAdIds;
import com.partharoypc.smartads.SmartAdsLogger;
import com.partharoypc.smartads.house.HouseAd;
import com.partharoypc.smartads.house.HouseAdLoader;
import com.partharoypc.smartads.listeners.RewardedAdListener;

import java.util.List;

/**
 * Manages Rewarded Ads (users earn rewards for watching).
 */
public class RewardedAdManager extends BaseFullScreenAdManager {
    private RewardedAd admobRewardedAd;
    private RewardedAdListener developerListener;

    private boolean isHouseAdReady = false;
    private HouseAd selectedHouseAd;
    private int selectedHouseAdIndex = -1;

    /**
     * Loads a Rewarded Ad.
     */
    public void loadAd(Context context, SmartAdsConfig config) {
        if (!SmartAds.getInstance().areAdsEnabled() || !config.isRewardedEnabled()) {
            SmartAdsLogger.d("Rewarded Ad is disabled. Skipping request.");
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

        if (config.getRewardedSource() == com.partharoypc.smartads.AdSource.HOUSE) {
            SmartAdsLogger.d("Rewarded source set to HOUSE. Loading House Ad directly.");
            if (!loadHouseAd(context, config)) {
                adStatus = AdStatus.IDLE;
                isLoading = false;
            }
            return;
        }

        SmartAdsLogger.d("Loading Rewarded Ad...");
        loadAdMob(context, config);
    }

    private void loadAdMob(Context context, SmartAdsConfig config) {
        if (checkNetworkAndFallback(context, config, () -> {
            if (config.isHouseAdsAutoFallback() && loadHouseAd(context, config)) {
                SmartAdsLogger.d("Fallback to House Rewarded Ad (Offline).");
            } else {
                adStatus = AdStatus.IDLE;
                isLoading = false;
                dismissLoadingDialog();
            }
        })) {
            return;
        }
        String adUnitId = (config.getAdMobRewardedId() != null && !config.getAdMobRewardedId().isEmpty())
                ? config.getAdMobRewardedId()
                : (config.isTestMode() ? TestAdIds.ADMOB_REWARDED_ID : null);
        if (adUnitId == null || adUnitId.isEmpty()) {
            if (config.isHouseAdsAutoFallback() && config.isHouseAdsEnabled()) {
                SmartAdsLogger.d("AdMob Rewarded ID not set. Trying House Ad.");
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
            SmartAdsLogger.d("AdMob Rate Limiter active (NO_FILL Cooldown). Skipping AdMob Rewarded Request.");
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
        RewardedAd.load(context, adUnitId, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                SmartAdsLogger.d("✅ Rewarded Ad LOADED.");
                admobRewardedAd = rewardedAd;
                rewardedAd.setOnPaidEventListener(adValue -> {
                    SmartAds.getInstance().reportPaidEvent(adValue, rewardedAd.getResponseInfo(), adUnitId, "Rewarded");
                });
                onAdLoadedBase();
                checkPendingShow();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                SmartAdsLogger.e("❌ Rewarded Ad Failed to Load: " + loadAdError.getMessage());
                if (loadAdError.getCode() == com.google.android.gms.ads.AdRequest.ERROR_CODE_NO_FILL) {
                    com.partharoypc.smartads.utils.AdMobRateLimiter.recordNoFill(adUnitId);
                }
                // Fallback to House Ad
                if (config.isHouseAdsAutoFallback() && loadHouseAd(context, config)) {
                    SmartAdsLogger.d("Fallback to House Rewarded Ad.");
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
     * Shows the Rewarded Ad and handles the reward callback.
     */
    public void showAd(Activity activity, RewardedAdListener listener) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (listener != null)
                listener.onAdFailedToShow("Activity is invalid.");
            return;
        }

        this.developerListener = listener;

        if (adStatus == AdStatus.LOADING) {
            com.partharoypc.smartads.SmartAdsLogger.d("Rewarded Ad Loading... Activity waiting.");
            isShowPending = true;
            pendingActivity = activity;
            showLoadingDialog(activity);
            return;
        }

        if (isFrequencyCapped(com.partharoypc.smartads.SmartAds.getInstance().getConfig())) {
            SmartAdsConfig config = com.partharoypc.smartads.SmartAds.getInstance().getConfig();
            com.partharoypc.smartads.SmartAdsLogger.d("Rewarded Ad Frequency Capped. Checking House Ad fallback...");
            if (config != null && config.isHouseAdsEnabled() && !config.getHouseAds().isEmpty()) {
                if (isHouseAdReady && selectedHouseAd != null) {
                    com.partharoypc.smartads.SmartAdsLogger.d("Showing ready House Rewarded Ad due to frequency cap.");
                    showHouseRewarded(activity);
                    return;
                } else if (loadHouseAd(activity, config)) {
                    com.partharoypc.smartads.SmartAdsLogger.d("Showing newly loaded House Rewarded Ad due to frequency cap.");
                    showHouseRewarded(activity);
                    return;
                }
            }
            com.partharoypc.smartads.SmartAdsLogger.d("Rewarded Ad Frequency Capped and no House Ad available. Skipping.");
            if (listener != null)
                listener.onAdFailedToShow("Ad is frequency capped.");
            return;
        }

        if (adStatus != AdStatus.LOADED) {
            // Not loaded and not loading -> Start loading and wait
            com.partharoypc.smartads.SmartAdsLogger.d("Rewarded Ad not loaded. Starting load...");
            SmartAdsConfig config = com.partharoypc.smartads.SmartAds.getInstance().getConfig();

            // Check if we can load anything (Network OR House Ads)
            boolean isNetworkAvailable = com.partharoypc.smartads.utils.NetworkUtils.isNetworkAvailable(activity);
            boolean hasHouseAds = config.isHouseAdsEnabled() && !config.getHouseAds().isEmpty();

            if (!isNetworkAvailable && !hasHouseAds) {
                com.partharoypc.smartads.SmartAdsLogger.d("No Internet and No House Ads. Cannot show ad.");
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

        // Ad is LOADED, show it
        if (admobRewardedAd != null) {
            com.partharoypc.smartads.SmartAdsLogger.d("Showing AdMob Rewarded Ad.");
            showAdMobRewarded(activity);
        } else if (isHouseAdReady && selectedHouseAd != null) {
            com.partharoypc.smartads.SmartAdsLogger.d("Showing House Rewarded Ad.");
            showHouseRewarded(activity);
        } else {
            if (developerListener != null)
                listener.onAdFailedToShow("Ad not loaded.");
        }
    }

    private void showAdMobRewarded(Activity activity) {
        admobRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                com.partharoypc.smartads.SmartAdsLogger.d("Rewarded Ad Dismissed.");
                if (developerListener != null)
                    developerListener.onAdDismissed();
                admobRewardedAd = null;
                adStatus = AdStatus.IDLE;
                if (isAutoReloadEnabled) {
                    loadAd(activity, com.partharoypc.smartads.SmartAds.getInstance().getConfig());
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                com.partharoypc.smartads.SmartAdsLogger.e("Rewarded Ad Failed to Show: " + adError.getMessage());
                if (developerListener != null) {
                    developerListener.onAdFailedToShow(adError.getMessage());
                }
                admobRewardedAd = null;
                adStatus = AdStatus.IDLE;
                if (isAutoReloadEnabled) {
                    loadAd(activity, com.partharoypc.smartads.SmartAds.getInstance().getConfig());
                }
            }

            @Override
            public void onAdShowedFullScreenContent() {
                adStatus = AdStatus.SHOWN;
                lastShownTime = System.currentTimeMillis();
                AdFrequencyManager.getInstance().recordRewardedShown();
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
        admobRewardedAd.show(activity, rewardItem -> {
            com.partharoypc.smartads.SmartAdsLogger
                    .d("💰 User Earned Reward: " + rewardItem.getType() + " - " + rewardItem.getAmount());
            if (developerListener != null)
                developerListener.onUserEarnedReward();
        });
    }

    private void showHouseRewarded(Activity activity) {
        com.partharoypc.smartads.house.HouseInterstitialActivity.start(activity, selectedHouseAdIndex,
                new com.partharoypc.smartads.house.HouseInterstitialActivity.HouseInterstitialListener() {
                    @Override
                    public void onAdDismissed() {
                        com.partharoypc.smartads.SmartAdsLogger
                                .d("House Rewarded Ad Dismissed. Granting simulated reward.");
                        // Grant reward on dismissal for House Ads (Simulated Reward)
                        if (developerListener != null) {
                            developerListener.onUserEarnedReward();
                            developerListener.onAdDismissed();
                        }

                        isHouseAdReady = false;
                        adStatus = AdStatus.IDLE;
                        if (isAutoReloadEnabled) {
                            loadAd(activity, com.partharoypc.smartads.SmartAds.getInstance().getConfig());
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
                        AdFrequencyManager.getInstance().recordRewardedShown();
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
