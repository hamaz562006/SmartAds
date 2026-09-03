package com.partharoypc.smartads.managers;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.partharoypc.smartads.AdStatus;
import com.partharoypc.smartads.SmartAds;
import com.partharoypc.smartads.SmartAdsConfig;
import com.partharoypc.smartads.SmartAdsLogger;
import com.partharoypc.smartads.TestAdIds;
import com.partharoypc.smartads.house.HouseAd;
import com.partharoypc.smartads.house.HouseAdLoader;
import com.partharoypc.smartads.house.HouseInterstitialActivity;
import com.partharoypc.smartads.listeners.AppOpenAdListener;
import com.partharoypc.smartads.analytics.SmartAdsLifecycleListener;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages App Open Ads, including automatic lifecycle-based triggers and House
 * Ad fallback.
 */
public class AppOpenAdManager extends BaseFullScreenAdManager
        implements DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {
    private final Application application;
    private AppOpenAd appOpenAd = null;
    private Activity currentActivity;
    private boolean isShowingAd = false;
    private long loadTimeMs = 0L;
    private static final long MAX_AD_AGE_MS = 4L * 60L * 60L * 1000L;
    private AppOpenAdListener developerListener;
    private final Set<Class<? extends Activity>> disabledActivities = Collections.synchronizedSet(new HashSet<>());

    public AppOpenAdManager(Application application) {
        this.application = application;
        this.application.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        this.isAutoReloadEnabled = true;
    }

    /**
     * Disables automatic App Open Ad triggers when this activity is in foreground.
     */
    public void disableAppOpenForActivity(Class<? extends Activity> activityClass) {
        if (activityClass != null) {
            disabledActivities.add(activityClass);
            SmartAdsLogger.d("Disabled App Open Ad for: " + activityClass.getSimpleName());
        }
    }

    /**
     * Re-enables automatic App Open Ad triggers for this activity.
     */
    public void enableAppOpenForActivity(Class<? extends Activity> activityClass) {
        if (activityClass != null) {
            disabledActivities.remove(activityClass);
            SmartAdsLogger.d("Re-enabled App Open Ad for: " + activityClass.getSimpleName());
        }
    }

    /**
     * Sets the listener for App Open Ad events.
     */
    public void setListener(AppOpenAdListener listener) {
        this.developerListener = listener;
    }

    public Activity getCurrentActivityForUmp() {
        return currentActivity;
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (SmartAds.getInstance().canShowAds()) {
            showAdIfAvailable();
        }
    }

    private boolean isHouseAdReady = false;
    private HouseAd selectedHouseAd;
    private int selectedHouseAdIndex = -1;

    /**
     * Fetches a new App Open Ad from AdMob or House Ads.
     */
    public void fetchAd() {
        if (!SmartAds.getInstance().areAdsEnabled() || !SmartAds.getInstance().getConfig().isAppOpenEnabled()) {
            SmartAdsLogger.d("App Open Ad is disabled. Skipping request.");
            return;
        }

        if ((appOpenAd != null && isAdFresh()) || (isHouseAdReady && selectedHouseAd != null)) {
            SmartAdsLogger.d("App Open Ad is already fresh/ready. Skipping fetch.");
            return;
        }
        if (isLoading) {
            SmartAdsLogger.d("App Open Ad is currently loading. Skipping fetch.");
            return;
        }

        if (SmartAds.getInstance().getConfig().getAppOpenSource() == com.partharoypc.smartads.AdSource.HOUSE) {
            SmartAdsLogger.d("App Open source set to HOUSE. Loading House Ad directly.");
            SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
            if (ll != null) {
                ll.onAdLoadStarted("AppOpen", "HOUSE");
            }
            isLoading = true;
            adStatus = AdStatus.LOADING;
            isHouseAdReady = false;
            selectedHouseAd = null;
            if (SmartAds.getInstance().getConfig().isHouseAdsEnabled()) {
                List<HouseAd> houseAds = SmartAds.getInstance().getConfig().getHouseAds();
                selectedHouseAd = HouseAdLoader.selectAd(houseAds);
                if (selectedHouseAd != null) {
                    selectedHouseAdIndex = houseAds.indexOf(selectedHouseAd);
                    isHouseAdReady = true;
                    if (ll != null) {
                        ll.onAdLoadSuccess("AppOpen", "HOUSE");
                    }
                    onAdLoadedBase();
                    return;
                }
            }
            if (ll != null) {
                ll.onAdLoadFailed("AppOpen", "HOUSE", "No House Ad available.");
            }
            isLoading = false;
            adStatus = AdStatus.FAILED;
            return;
        }

        String adUnitId = (SmartAds.getInstance().getConfig().getAdMobAppOpenId() != null
                && !SmartAds.getInstance().getConfig().getAdMobAppOpenId().isEmpty())
                        ? SmartAds.getInstance().getConfig().getAdMobAppOpenId()
                        : (SmartAds.getInstance().getConfig().isTestMode() ? TestAdIds.ADMOB_APP_OPEN_ID : null);

        if (adUnitId == null || adUnitId.isEmpty()) {
            if (SmartAds.getInstance().getConfig().isHouseAdsAutoFallback() && SmartAds.getInstance().getConfig().isHouseAdsEnabled()) {
                SmartAdsLogger.d("AdMob App Open ID not set. Trying House Ad.");
                SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                if (ll != null) {
                    ll.onAdLoadStarted("AppOpen", "HOUSE");
                }
                List<HouseAd> houseAds = SmartAds.getInstance().getConfig().getHouseAds();
                selectedHouseAd = HouseAdLoader.selectAd(houseAds);
                if (selectedHouseAd != null) {
                    selectedHouseAdIndex = houseAds.indexOf(selectedHouseAd);
                    isHouseAdReady = true;
                    if (ll != null) {
                        ll.onAdLoadSuccess("AppOpen", "HOUSE");
                    }
                    onAdLoadedBase();
                    return;
                }
                if (ll != null) {
                    ll.onAdLoadFailed("AppOpen", "HOUSE", "No House Ad available.");
                }
            }

            isLoading = false;
            adStatus = AdStatus.FAILED;
            return;
        }

        // 3. AdMob NO_FILL Rate Limiting
        if (com.partharoypc.smartads.utils.AdMobRateLimiter.isRateLimited(adUnitId)) {
            SmartAdsLogger.d("AdMob Rate Limiter active (NO_FILL Cooldown). Skipping AdMob App Open Request.");
            if (SmartAds.getInstance().getConfig().isHouseAdsAutoFallback() && SmartAds.getInstance().getConfig().isHouseAdsEnabled()) {
                SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                if (ll != null) {
                    ll.onAdLoadStarted("AppOpen", "HOUSE");
                }
                List<HouseAd> houseAds = SmartAds.getInstance().getConfig().getHouseAds();
                selectedHouseAd = HouseAdLoader.selectAd(houseAds);
                if (selectedHouseAd != null) {
                    selectedHouseAdIndex = houseAds.indexOf(selectedHouseAd);
                    isHouseAdReady = true;
                    if (ll != null) {
                        ll.onAdLoadSuccess("AppOpen", "HOUSE");
                    }
                    onAdLoadedBase();
                    return;
                }
                if (ll != null) {
                    ll.onAdLoadFailed("AppOpen", "HOUSE", "No House Ad available.");
                }
            }
            isLoading = false;
            adStatus = AdStatus.FAILED;
            return;
        }

        isLoading = true;
        adStatus = AdStatus.LOADING;
        isHouseAdReady = false;
        selectedHouseAd = null;

        SmartAdsLogger.d("Fetching App Open Ad...");
        SmartAdsLifecycleListener llInit = SmartAds.getInstance().getLifecycleListener();
        if (llInit != null) {
            llInit.onAdLoadStarted("AppOpen", "ADMOB");
        }

        AppOpenAd.AppOpenAdLoadCallback loadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {
                SmartAdsLogger.d("✅ App Open Ad LOADED.");
                SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                if (ll != null) {
                    ll.onAdLoadSuccess("AppOpen", "ADMOB");
                }
                AppOpenAdManager.this.appOpenAd = ad;
                loadTimeMs = System.currentTimeMillis();

                ad.setOnPaidEventListener(adValue -> {
                    SmartAds.getInstance().reportPaidEvent(adValue, ad.getResponseInfo(),
                            SmartAds.getInstance().getConfig().getAdMobAppOpenId(), "App Open");
                });

                onAdLoadedBase();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                SmartAdsLogger.e("❌ App Open Ad Failed to Load: " + loadAdError.getMessage());
                SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                if (ll != null) {
                    ll.onAdLoadFailed("AppOpen", "ADMOB", loadAdError.getMessage());
                }
                if (loadAdError.getCode() == com.google.android.gms.ads.AdRequest.ERROR_CODE_NO_FILL) {
                    com.partharoypc.smartads.utils.AdMobRateLimiter.recordNoFill(adUnitId);
                }
                // FALLBACK TO HOUSE AD
                if (SmartAds.getInstance().getConfig().isHouseAdsAutoFallback() && SmartAds.getInstance().getConfig().isHouseAdsEnabled()) {
                    if (ll != null) {
                        ll.onAdLoadStarted("AppOpen", "HOUSE");
                    }
                    List<HouseAd> houseAds = SmartAds.getInstance().getConfig().getHouseAds();
                    selectedHouseAd = HouseAdLoader.selectAd(houseAds);
                    if (selectedHouseAd != null) {
                        SmartAdsLogger.d("Fallback to House App Open Ad.");
                        selectedHouseAdIndex = houseAds.indexOf(selectedHouseAd);
                        isHouseAdReady = true;
                        if (ll != null) {
                            ll.onAdLoadSuccess("AppOpen", "HOUSE");
                        }
                        onAdLoadedBase();
                        return;
                    }
                    if (ll != null) {
                        ll.onAdLoadFailed("AppOpen", "HOUSE", "No House Ad available.");
                    }
                }

                onAdFailedToLoadBase();
                // Only retry if not house ad either
                scheduleRetry(application, null, loadAdError, AppOpenAdManager.this::fetchAd);
            }
        };

        if (checkNetworkAndFallback(application, SmartAds.getInstance().getConfig(), () -> {
            if (SmartAds.getInstance().getConfig().isHouseAdsAutoFallback() && SmartAds.getInstance().getConfig().isHouseAdsEnabled()) {
                List<HouseAd> houseAds = SmartAds.getInstance().getConfig().getHouseAds();
                selectedHouseAd = HouseAdLoader.selectAd(houseAds);
                if (selectedHouseAd != null) {
                    selectedHouseAdIndex = houseAds.indexOf(selectedHouseAd);
                    isHouseAdReady = true;
                    onAdLoadedBase();
                } else {
                    isLoading = false;
                    adStatus = AdStatus.FAILED;
                }
            } else {
                isLoading = false;
                adStatus = AdStatus.FAILED;
            }
        })) {
            return;
        }

        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(application, adUnitId, request, loadCallback);
    }

    public void showAdIfAvailable() {
        if (currentActivity == null || SmartAds.getInstance().isAnyAdShowing()) {
            return;
        }

        SmartAdsConfig config = SmartAds.getInstance().getConfig();
        if (config == null || !config.isAppOpenEnabled()) {
            return;
        }
        long aoInterval = config.getAppOpenIntervalSeconds();

        if (!AdFrequencyManager.getInstance().canShowAppOpen(aoInterval) || isFrequencyCapped(config)) {
            SmartAdsLogger.d("App Open Ad Frequency Capped. Checking House Ad fallback...");
            if (config.isHouseAdsEnabled() && !config.getHouseAds().isEmpty()) {
                if (!isShowingAd && !(currentActivity instanceof HouseInterstitialActivity) && !disabledActivities.contains(currentActivity.getClass())) {
                    if (isHouseAdReady && selectedHouseAd != null) {
                        SmartAdsLogger.d("Showing ready House App Open Ad due to frequency cap.");
                        showHouseAppOpen();
                        return;
                    } else {
                        List<HouseAd> houseAds = config.getHouseAds();
                        selectedHouseAd = HouseAdLoader.selectAd(houseAds);
                        if (selectedHouseAd != null) {
                            selectedHouseAdIndex = houseAds.indexOf(selectedHouseAd);
                            isHouseAdReady = true;
                            SmartAdsLogger.d("Showing newly loaded House App Open Ad due to frequency cap.");
                            showHouseAppOpen();
                            return;
                        }
                    }
                }
            }
            SmartAdsLogger.d("App Open Ad Frequency Capped and no House Ad available. Skipping.");
            return;
        }

        // Prevent showing ad on top of House Ad Activity, disabled activities, or if already showing
        if (isShowingAd || currentActivity instanceof HouseInterstitialActivity || disabledActivities.contains(currentActivity.getClass())) {
            if (disabledActivities.contains(currentActivity.getClass())) {
                SmartAdsLogger.d("App Open Ad skipped for disabled Activity: " + currentActivity.getClass().getSimpleName());
            }
            return;
        }

        if (appOpenAd != null && isAdFresh()) {
            SmartAdsLogger.d("Showing AdMob App Open Ad.");
            showAdMobAppOpen();
        } else if (isHouseAdReady && selectedHouseAd != null) {
            SmartAdsLogger.d("Showing House App Open Ad.");
            showHouseAppOpen();
        } else {
            SmartAdsLogger.d("App Open Ad not ready. Fetching new one.");
            fetchAd();
        }
    }

    private void showAdMobAppOpen() {
        isShowingAd = true;
        FullScreenContentCallback fullScreenContentCallback = new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                SmartAdsLogger.d("App Open Ad Dismissed.");
                SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                if (ll != null) {
                    ll.onAdClosed("AppOpen", "ADMOB");
                }
                AppOpenAdManager.this.appOpenAd = null;
                isShowingAd = false;
                fetchAd();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                SmartAdsLogger.d("App Open Ad Shown.");
                isShowingAd = true;
                adStatus = AdStatus.SHOWN;
                lastShownTime = System.currentTimeMillis();
                AdFrequencyManager.getInstance().recordAppOpenShown();
                SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                if (ll != null) {
                    ll.onAdShowSuccess("AppOpen", "ADMOB");
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                SmartAdsLogger.e("App Open Ad Failed to Show: " + adError.getMessage());
                SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                if (ll != null) {
                    ll.onAdShowFailed("AppOpen", "ADMOB", adError.getMessage());
                }
                AppOpenAdManager.this.appOpenAd = null;
                isShowingAd = false;
                fetchAd();
            }

            @Override
            public void onAdImpression() {
                if (developerListener != null) {
                    developerListener.onAdImpression();
                }
            }

            @Override
            public void onAdClicked() {
                SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                if (ll != null) {
                    ll.onAdClicked("AppOpen", "ADMOB");
                }
                if (developerListener != null) {
                    developerListener.onAdClicked();
                }
            }
        };
        appOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
        appOpenAd.show(currentActivity);
    }

    private void showHouseAppOpen() {
        isShowingAd = true;
        HouseInterstitialActivity.start(currentActivity, selectedHouseAdIndex,
                new HouseInterstitialActivity.HouseInterstitialListener() {
                    @Override
                    public void onAdDismissed() {
                        SmartAdsLogger.d("House App Open Ad Dismissed.");
                        SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                        if (ll != null) {
                            ll.onAdClosed("AppOpen", "HOUSE");
                        }
                        isShowingAd = false;
                        isHouseAdReady = false;
                        selectedHouseAd = null;
                        if (developerListener != null)
                            developerListener.onAdDismissed();
                        fetchAd();
                    }

                    @Override
                    public void onAdClicked() {
                        SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                        if (ll != null) {
                            ll.onAdClicked("AppOpen", "HOUSE");
                        }
                        if (developerListener != null)
                            developerListener.onAdClicked();
                    }

                    @Override
                    public void onAdImpression() {
                        adStatus = AdStatus.SHOWN;
                        isShowingAd = true;
                        lastShownTime = System.currentTimeMillis();
                        AdFrequencyManager.getInstance().recordAppOpenShown();
                        SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                        if (ll != null) {
                            ll.onAdShowSuccess("AppOpen", "HOUSE");
                        }
                        if (developerListener != null)
                            developerListener.onAdImpression();
                    }
                });
    }

    public boolean isShowingAd() {
        return isShowingAd;
    }

    public void destroy(Application application) {
        if (application != null) {
            application.unregisterActivityLifecycleCallbacks(this);
            ProcessLifecycleOwner.get().getLifecycle().removeObserver(this);
        }
        appOpenAd = null;
        currentActivity = null;
        developerListener = null;
        isShowingAd = false;
        SmartAdsLogger.d("AppOpenAdManager destroyed.");
    }

    private boolean isAdFresh() {
        return appOpenAd != null && (System.currentTimeMillis() - loadTimeMs) < MAX_AD_AGE_MS;
    }

    public boolean isAdAvailable() {
        return (appOpenAd != null && isAdFresh()) || (isHouseAdReady && selectedHouseAd != null);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (!(activity instanceof HouseInterstitialActivity)) {
            currentActivity = activity;
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (!(activity instanceof HouseInterstitialActivity)) {
            currentActivity = activity;
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }
}
