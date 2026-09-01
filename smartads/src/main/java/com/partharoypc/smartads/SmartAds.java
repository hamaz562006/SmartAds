package com.partharoypc.smartads;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.partharoypc.smartads.analytics.SmartAdsAdjustAdapter;
import com.partharoypc.smartads.analytics.SmartAdsAnalyticsListener;
import com.partharoypc.smartads.listeners.AppOpenAdListener;
import com.partharoypc.smartads.listeners.BannerAdListener;
import com.partharoypc.smartads.listeners.InterstitialAdListener;
import com.partharoypc.smartads.listeners.NativeAdListener;
import com.partharoypc.smartads.listeners.RewardedAdListener;
import com.partharoypc.smartads.listeners.RewardedInterstitialAdListener;
import com.partharoypc.smartads.managers.AdFrequencyManager;
import com.partharoypc.smartads.managers.AppOpenAdManager;
import com.partharoypc.smartads.managers.BannerAdManager;
import com.partharoypc.smartads.managers.InterstitialAdManager;
import com.partharoypc.smartads.managers.NativeAdManager;
import com.partharoypc.smartads.managers.RewardedAdManager;
import com.partharoypc.smartads.managers.RewardedInterstitialAdManager;
import com.partharoypc.smartads.ui.NativeAdBinder;
import com.partharoypc.smartads.utils.SmartAdsPreloadHelper;

public class SmartAds {

    private static volatile SmartAds instance;
    private SmartAdsConfig config;
    private boolean adsEnabled;
    private AppOpenAdManager appOpenAdManager;
    private InterstitialAdManager interstitialAdManager;
    private RewardedAdManager rewardedAdManager;
    private RewardedInterstitialAdManager rewardedInterstitialAdManager;

    private BannerAdManager bannerAdManager;
    private NativeAdManager nativeAdManager;

    private SmartAdsAnalyticsListener analyticsListener;
    private SmartAdsAdjustAdapter adjustAdapter;

    private Application application;

    private SmartAds() {
    }

    private void requestUmpWithActivity(Application application) {
        if (appOpenAdManager == null)
            return;

        Activity activity = appOpenAdManager.getCurrentActivityForUmp();
        if (activity == null) {
            SmartAdsLogger.d("No Activity available for UMP. Waiting for first Activity...");
            application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityStarted(@NonNull Activity act) {
                    if (!(act instanceof com.partharoypc.smartads.house.HouseInterstitialActivity)) {
                        application.unregisterActivityLifecycleCallbacks(this);
                        requestUmpWithActivity(application);
                    }
                }

                @Override
                public void onActivityCreated(@NonNull Activity a, Bundle b) {
                }

                @Override
                public void onActivityResumed(@NonNull Activity a) {
                }

                @Override
                public void onActivityPaused(@NonNull Activity a) {
                }

                @Override
                public void onActivityStopped(@NonNull Activity a) {
                }

                @Override
                public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle b) {
                }

                @Override
                public void onActivityDestroyed(@NonNull Activity a) {
                }
            });
            return;
        }

        com.google.android.ump.ConsentDebugSettings.Builder debugSettingsBuilder = new com.google.android.ump.ConsentDebugSettings.Builder(
                activity);
        if (config
                .getUmpDebugGeography() != com.google.android.ump.ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_DISABLED) {
            debugSettingsBuilder.setDebugGeography(config.getUmpDebugGeography());
        }
        if (config.getUmpTestDeviceHashedId() != null && !config.getUmpTestDeviceHashedId().isEmpty()) {
            debugSettingsBuilder.addTestDeviceHashedId(config.getUmpTestDeviceHashedId());
        }

        com.google.android.ump.ConsentRequestParameters params = new com.google.android.ump.ConsentRequestParameters.Builder()
                .setConsentDebugSettings(debugSettingsBuilder.build())
                .build();

        com.google.android.ump.ConsentInformation consentInformation = com.google.android.ump.UserMessagingPlatform
                .getConsentInformation(application);
        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () -> {
                    com.google.android.ump.UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                            activity,
                            formError -> initializeSdks(application));
                },
                requestConsentError -> {
                    SmartAdsLogger.e("UMP Consent Request Error: " + requestConsentError.getMessage());
                    initializeSdks(application);
                });
    }

    private void initializeManagersIfNeeded(Application application) {
        if (appOpenAdManager == null) {
            appOpenAdManager = new AppOpenAdManager(application);
        }
        if (interstitialAdManager == null) {
            interstitialAdManager = new InterstitialAdManager();
        }
        if (rewardedAdManager == null) {
            rewardedAdManager = new RewardedAdManager();
        }
        if (rewardedInterstitialAdManager == null) {
            rewardedInterstitialAdManager = new RewardedInterstitialAdManager();
        }
        if (bannerAdManager == null) {
            bannerAdManager = new BannerAdManager();
        }
        if (nativeAdManager == null) {
            nativeAdManager = new NativeAdManager();
        }
    }

    public static SmartAds getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                    "SmartAds not initialized. You must call SmartAds.initialize(application, config) " +
                            "before calling getInstance(). Use SmartAds.isInitialized() to check status.");
        }
        return instance;
    }

    /**
     * Checks if the SmartAds SDK is currently initialized and ready to use.
     *
     * @return true if initialized, false otherwise.
     */
    public static boolean isInitialized() {
        return instance != null;
    }

    /**
     * Returns the current version of the library.
     */
    public static String getVersion() {
        return "5.6.0";
    }

    @RequiresPermission(Manifest.permission.INTERNET)
    public static void initialize(Application application, SmartAdsConfig config) {
        if (instance == null) {
            synchronized (SmartAds.class) {
                if (instance == null) {
                    if (application == null || config == null) {
                        SmartAdsLogger.e("SmartAds Initialize failed: Application or Config is null.");
                        return;
                    }

                    if (androidx.core.content.ContextCompat.checkSelfPermission(application,
                            Manifest.permission.INTERNET) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        SmartAdsLogger.e("⚠️ INTERNET permission is missing! SmartAds requires it to function.");
                    }

                    instance = new SmartAds();
                    instance.application = application;
                    instance.config = config;
                    instance.adsEnabled = config.isAdsEnabled();

                    RequestConfiguration.Builder rcBuilder = new RequestConfiguration.Builder();

                    if (config.getTestDeviceIds() != null && !config.getTestDeviceIds().isEmpty()) {
                        rcBuilder.setTestDeviceIds(config.getTestDeviceIds());
                    }
                    if (config.getMaxAdContentRating() != null) {
                        rcBuilder.setMaxAdContentRating(config.getMaxAdContentRating());
                    }
                    if (config
                            .getTagForChildDirectedTreatment() != RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED) {
                        rcBuilder.setTagForChildDirectedTreatment(config.getTagForChildDirectedTreatment());
                    }
                    if (config
                            .getTagForUnderAgeOfConsent() != RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED) {
                        rcBuilder.setTagForUnderAgeOfConsent(config.getTagForUnderAgeOfConsent());
                    }

                    MobileAds.setRequestConfiguration(rcBuilder.build());

                    SmartAdsLogger.setLoggingEnabled(config.isLoggingEnabled() || config.isTestMode());

                    instance.initializeManagersIfNeeded(application);

                    // Register lifecycle-based screen preloader
                    SmartAdsPreloadHelper.register(application, config);

                    // UMP Consent (optional)
                    if (config.useUmpConsent()) {
                        instance.requestUmpWithActivity(application);
                    } else {
                        instance.initializeSdks(application);
                    }

                    SmartAdsLogger.d("SmartAds Initialized (v" + getVersion() + ")");
                }
            }
        }
    }

    /**
     * Checks if the device currently has an active internet connection.
     * Delegates to
     * {@link com.partharoypc.smartads.utils.NetworkUtils#isNetworkAvailable(Context)}.
     */
    public static boolean isNetworkAvailable(Context context) {
        return com.partharoypc.smartads.utils.NetworkUtils.isNetworkAvailable(context);
    }

    /**
     * Fully shuts down the SmartAds library, unregistering listeners and clearing
     * managers.
     * Should be called if the application is being destroyed or if you want to stop
     * ad services completely.
     */
    public void shutdown() {
        if (application != null && appOpenAdManager != null) {
            appOpenAdManager.destroy(application);
        }
        config = null;
        appOpenAdManager = null;
        interstitialAdManager = null;
        rewardedAdManager = null;
        rewardedInterstitialAdManager = null;
        bannerAdManager = null;
        nativeAdManager = null;
        analyticsListener = null;
        application = null;
        instance = null;
        SmartAdsLogger.d("SmartAds SDK Shutdown complete.");
    }

    /**
     * Disables automatic App Open Ads for a specific Activity class.
     */
    public void disableAppOpenForActivity(Class<? extends Activity> activityClass) {
        if (appOpenAdManager != null) {
            appOpenAdManager.disableAppOpenForActivity(activityClass);
        }
    }

    /**
     * Re-enables automatic App Open Ads for a specific Activity class.
     */
    public void enableAppOpenForActivity(Class<? extends Activity> activityClass) {
        if (appOpenAdManager != null) {
            appOpenAdManager.enableAppOpenForActivity(activityClass);
        }
    }

    /**
     * Launches the AdMob Ad Inspector.
     * Useful for verifying ad network integration.
     *
     * @param activity current activity context.
     */
    public void launchAdInspector(Activity activity) {
        MobileAds.openAdInspector(activity, error -> {
            /* Ad inspector closed or error */
        });
    }

    /**
     * Checks if the user is required to see privacy options (e.g., GDPR).
     *
     * @return true if privacy options are required.
     */
    public boolean isPrivacyOptionsRequired() {
        return UserMessagingPlatform.getConsentInformation(application)
                .getPrivacyOptionsRequirementStatus() == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    /**
     * Pre-loads ads (App Open, Interstitial, Rewarded, Rewarded Interstitial) based on configuration.
     * Should be called after initialization or when appropriate.
     */
    public void preloadAds(Context context) {
        if (!canShowAds())
            return;

        if (appOpenAdManager != null && config.isAppOpenConfigured() && config.isAppOpenEnabled()) {
            appOpenAdManager.fetchAd();
        }
        if (interstitialAdManager != null && config.isInterstitialConfigured() && config.isInterstitialEnabled()) {
            interstitialAdManager.loadAd(context, config);
        }
        if (rewardedAdManager != null && config.isRewardedConfigured() && config.isRewardedEnabled()) {
            rewardedAdManager.loadAd(context, config);
        }
        if (rewardedInterstitialAdManager != null && config.isRewardedInterstitialConfigured() && config.isRewardedInterstitialEnabled()) {
            rewardedInterstitialAdManager.loadAd(context, config);
        }
    }

    private void initializeSdks(Application application) {
        MobileAds.initialize(application, initializationStatus -> {
            try {
                if (appOpenAdManager != null && config != null && config.isAppOpenEnabled()) {
                    appOpenAdManager.fetchAd();
                }
            } catch (Exception e) {
                SmartAdsLogger.e("Pre-fetching App Open Ad failed: " + e.getMessage());
            }
        });

        initializeManagersIfNeeded(application);
    }

    /**
     * Shows the UMP Privacy Options Form to the user.
     */
    public void showPrivacyOptionsForm(Activity activity) {
        try {
            UserMessagingPlatform.showPrivacyOptionsForm(activity, formError -> {
                /* no-op */
            });
        } catch (Exception ignored) {
        }
    }

    // --- App Open Ad (Manual) ---

    /**
     * Manually shows an App Open Ad if available.
     *
     * @param activity The current activity.
     */
    public void showAppOpenAd(Activity activity) {
        if (canShowAds() && config.isAppOpenEnabled() && appOpenAdManager != null) {
            appOpenAdManager.showAdIfAvailable();
        }
    }

    /**
     * Checks if an App Open Ad is currently available to show.
     */
    public boolean isAppOpenAdAvailable() {
        return appOpenAdManager != null && appOpenAdManager.isAdAvailable();
    }

    /**
     * Returns the current status of the App Open Ad.
     */
    public AdStatus getAppOpenAdStatus() {
        if (appOpenAdManager != null) {
            return appOpenAdManager.getAdStatus();
        }
        return AdStatus.IDLE;
    }

    /**
     * Temporarily enables or disables ads (e.g. if user purchased premium).
     */
    public void setAdsEnabled(boolean enabled) {
        this.adsEnabled = enabled;
        if (this.config != null) {
            this.config = this.config.toBuilder().setAdsEnabled(enabled).build();
        }
        SmartAdsLogger.d("Ads globally " + (enabled ? "ENABLED" : "DISABLED"));
    }

    /**
     * Checks if ads are currently enabled in the SDK.
     *
     * @return true if enabled, false if disabled.
     */
    public boolean areAdsEnabled() {
        return this.adsEnabled;
    }

    /**
     * Sets the developer-side listener for App Open Ads.
     *
     * @param listener The listener for App Open Ad events.
     */
    public void setAppOpenAdListener(AppOpenAdListener listener) {
        if (appOpenAdManager != null) {
            appOpenAdManager.setListener(listener);
        }
    }

    /**
     * Sets the global analytics listener for ad revenue events.
     *
     * @param listener The analytics listener.
     */
    public void setAnalyticsListener(SmartAdsAnalyticsListener listener) {
        this.analyticsListener = listener;
    }

    /**
     * Sets the Adjust SDK ILAR adapter.
     */
    public void setAdjustAdapter(SmartAdsAdjustAdapter adapter) {
        this.adjustAdapter = adapter;
    }

    /**
     * Internal method used by Ad Managers to report revenue.
     */
    public void reportPaidEvent(com.google.android.gms.ads.AdValue adValue,
            com.google.android.gms.ads.ResponseInfo responseInfo,
            String adUnitId,
            String adFormat) {
        String networkName = "Google AdMob";

        if (analyticsListener != null) {
            analyticsListener.onAdRevenuePaid(
                    adUnitId,
                    adFormat,
                    networkName,
                    adValue.getValueMicros(),
                    adValue.getCurrencyCode(),
                    adValue.getPrecisionType(),
                    null);
        }

        if (adjustAdapter != null) {
            adjustAdapter.trackAdRevenue(
                    adUnitId,
                    adFormat,
                    networkName,
                    adValue.getValueMicros(),
                    adValue.getCurrencyCode(),
                    adValue.getPrecisionType());
        }

        SmartAdsLogger.d("💰 Paid Event: " +
                adValue.getValueMicros() + " " + adValue.getCurrencyCode() +
                " | Network: " + networkName +
                " | Format: " + adFormat);
    }

    public void updateConfig(SmartAdsConfig newConfig) {
        this.config = newConfig;
        this.adsEnabled = newConfig.isAdsEnabled();
        // Configuration updated. Managers will use new IDs on next load.
    }

    public SmartAdsConfig getConfig() {
        return config;
    }

    /**
     * Checks if the user is premium.
     */
    public boolean isPremium() {
        return config != null && config.isPremium();
    }

    /**
     * Sets user premium status (blocks all ads when true).
     */
    public void setPremium(boolean isPremium) {
        if (this.config != null) {
            this.config = this.config.toBuilder().setPremium(isPremium).build();
        }
        SmartAdsLogger.d("User premium status set to: " + isPremium);
    }

    /**
     * Checks whether any ad can currently be shown (must not be premium, must have adsEnabled = true and ad IDs).
     */
    public boolean canShowAds() {
        if (isPremium()) {
            return false;
        }
        return this.adsEnabled && config != null && config.isAnyAdConfigured();
    }

    /**
     * Checks if an Interstitial ad is permitted to show based on time-based frequency capping and cooldown after rewarded ads.
     */
    public boolean canShowInterstitial() {
        long ivInterval = config != null ? config.getInterstitialIntervalSeconds() : 30L;
        long rvDelay = config != null ? config.getDelayAfterRewardedSeconds() : 30L;
        return AdFrequencyManager.getInstance().canShowInterstitial(ivInterval, rvDelay);
    }

    /**
     * Checks if an App Open ad is permitted to show based on time-based frequency capping.
     */
    public boolean canShowAppOpen() {
        long aoInterval = config != null ? config.getAppOpenIntervalSeconds() : 15L;
        return AdFrequencyManager.getInstance().canShowAppOpen(aoInterval);
    }

    // --- Interstitial Ads ---

    /**
     * Checks if an Interstitial Ad is loaded and ready to show.
     */
    public boolean isInterstitialAdAvailable() {
        return interstitialAdManager != null && interstitialAdManager.getAdStatus() == AdStatus.LOADED;
    }

    /**
     * Manually starts loading an Interstitial Ad.
     */
    public void loadInterstitialAd(Context context) {
        if (canShowAds()) {
            interstitialAdManager.loadAd(context, config);
        }
    }

    /**
     * Shows the Interstitial Ad if available.
     *
     * @param activity The activity context.
     * @param listener The listener for ad events.
     */
    public void showInterstitialAd(Activity activity, InterstitialAdListener listener) {
        if (canShowAds() && config.isInterstitialEnabled()) {
            interstitialAdManager.showAd(activity, listener);
        } else {
            if (listener != null)
                listener.onAdFailedToShow("Ad condition not met or Interstitial ads disabled.");
        }
    }

    /**
     * Shows the Interstitial Ad every 'interval' clicks/triggers.
     *
     * @param activity      Current activity.
     * @param clickInterval Number of actions between ad displays.
     * @param listener      The listener for ad events.
     */
    public void showInterstitialAdWithInterval(Activity activity, int clickInterval, InterstitialAdListener listener) {
        if (canShowAds() && config.isInterstitialEnabled()) {
            interstitialAdManager.showAdWithInterval(activity, clickInterval, listener);
        } else {
            if (listener != null)
                listener.onAdDismissed();
        }
    }

    public AdStatus getInterstitialAdStatus() {
        return interstitialAdManager.getAdStatus();
    }

    // --- Rewarded Ads ---

    /**
     * Checks if a Rewarded Ad is loaded and ready to show.
     */
    public boolean isRewardedAdAvailable() {
        return rewardedAdManager != null && rewardedAdManager.getAdStatus() == AdStatus.LOADED;
    }

    /**
     * Manually starts loading a Rewarded Ad.
     */
    public void loadRewardedAd(Context context) {
        if (canShowAds()) {
            rewardedAdManager.loadAd(context, config);
        }
    }

    /**
     * Shows a Rewarded Ad if loaded.
     *
     * @param activity Current activity.
     * @param listener Callback listener.
     */
    public void showRewardedAd(Activity activity, RewardedAdListener listener) {
        if (canShowAds() && config.isRewardedConfigured() && config.isRewardedEnabled()) {
            rewardedAdManager.showAd(activity, listener);
        } else {
            if (listener != null)
                listener.onAdFailedToShow("Ad condition not met or Rewarded ads disabled.");
        }
    }

    public AdStatus getRewardedAdStatus() {
        return rewardedAdManager.getAdStatus();
    }

    // --- Rewarded Interstitial Ads ---

    /**
     * Checks if a Rewarded Interstitial Ad is loaded and ready to show.
     */
    public boolean isRewardedInterstitialAdAvailable() {
        return rewardedInterstitialAdManager != null && rewardedInterstitialAdManager.getAdStatus() == AdStatus.LOADED;
    }

    /**
     * Manually starts loading a Rewarded Interstitial Ad.
     */
    public void loadRewardedInterstitialAd(Context context) {
        if (canShowAds()) {
            rewardedInterstitialAdManager.loadAd(context, config);
        }
    }

    /**
     * Shows a Rewarded Interstitial Ad if loaded.
     *
     * @param activity Current activity.
     * @param listener Callback listener.
     */
    public void showRewardedInterstitialAd(Activity activity, com.partharoypc.smartads.listeners.RewardedInterstitialAdListener listener) {
        if (canShowAds() && config.isRewardedInterstitialConfigured() && config.isRewardedInterstitialEnabled()) {
            rewardedInterstitialAdManager.showAd(activity, listener);
        } else {
            if (listener != null)
                listener.onAdFailedToShow("Ad condition not met or Rewarded Interstitial ads disabled.");
        }
    }

    public AdStatus getRewardedInterstitialAdStatus() {
        return rewardedInterstitialAdManager != null ? rewardedInterstitialAdManager.getAdStatus() : AdStatus.IDLE;
    }

    public boolean isAnyAdShowing() {
        boolean interShowing = interstitialAdManager != null && interstitialAdManager.getAdStatus() == AdStatus.SHOWN;
        boolean rewardedShowing = rewardedAdManager != null && rewardedAdManager.getAdStatus() == AdStatus.SHOWN;
        boolean rewardedInterShowing = rewardedInterstitialAdManager != null && rewardedInterstitialAdManager.getAdStatus() == AdStatus.SHOWN;
        boolean appOpenShowing = appOpenAdManager != null
                && (appOpenAdManager.getAdStatus() == AdStatus.SHOWN || appOpenAdManager.isShowingAd());
        return interShowing || rewardedShowing || rewardedInterShowing || appOpenShowing;
    }

    // --- Banner Ads ---

    /**
     * Loads and shows a Banner Ad in the provided container.
     *
     * @param activity    Current activity.
     * @param adContainer FrameLayout to hold the banner.
     * @param listener    Callback listener.
     */
    @RequiresPermission(Manifest.permission.INTERNET)
    public void showBannerAd(Activity activity, FrameLayout adContainer, BannerAdListener listener) {
        if (canShowAds() && config.isBannerConfigured() && config.isBannerEnabled()) {
            bannerAdManager.loadAndShowAd(activity, adContainer, config, listener);
        } else {
            if (listener != null)
                listener.onAdFailed("Ad condition not met or Banner ads disabled.");
        }
    }

    // --- Native Ads ---

    /**
     * Loads and shows a Native Ad with a custom layout reference.
     *
     * @param activity    Current activity.
     * @param adContainer FrameLayout to hold the native ad.
     * @param layoutRes   Resource ID of the native ad layout.
     * @param listener    Callback listener.
     */
    public void showNativeAd(Activity activity, FrameLayout adContainer, @LayoutRes int layoutRes,
            NativeAdListener listener) {
        if (canShowAds() && config.isNativeConfigured() && config.isNativeEnabled()) {
            nativeAdManager.loadAndShowAd(activity, adContainer, layoutRes, config, listener);
        } else {
            if (listener != null)
                listener.onAdFailed("Ad condition not met or Native ads disabled.");
        }
    }

    /**
     * Loads and shows a Native Ad using a custom NativeAdBinder for layout and view IDs mapping.
     *
     * @param activity    Current activity.
     * @param adContainer FrameLayout to hold the native ad.
     * @param binder      NativeAdBinder configuration.
     * @param listener    Callback listener.
     */
    public void showNativeAd(Activity activity, FrameLayout adContainer, NativeAdBinder binder,
            NativeAdListener listener) {
        if (canShowAds() && config.isNativeConfigured() && config.isNativeEnabled()) {
            nativeAdManager.loadAndShowAd(activity, adContainer, binder, config, listener);
        } else {
            if (listener != null)
                listener.onAdFailed("Ad condition not met or Native ads disabled.");
        }
    }

    /**
     * Loads and shows a Native Ad with a standard size template.
     *
     * @param activity    Current activity.
     * @param adContainer FrameLayout to hold the native ad.
     * @param size        Size enum (SMALL, MEDIUM, LARGE).
     * @param listener    Callback listener.
     */
    public void showNativeAd(Activity activity, FrameLayout adContainer, NativeAdSize size, NativeAdListener listener) {
        if (canShowAds() && config.isNativeConfigured() && config.isNativeEnabled()) {
            nativeAdManager.loadAndShowAd(activity, adContainer, size, config, listener);
        } else {
            if (listener != null)
                listener.onAdFailed("Ad condition not met or Native ads disabled.");
        }
    }

    // --- Helpers to clean up views to avoid leaks ---

    /**
     * Destroys existing banner ads in the container and clears the view.
     */
    public void destroyBannerIn(FrameLayout adContainer) {
        if (adContainer == null)
            return;
        int childCount = adContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            android.view.View child = adContainer.getChildAt(i);
            if (child instanceof com.google.android.gms.ads.AdView) {
                try {
                    ((com.google.android.gms.ads.AdView) child).destroy();
                } catch (Exception ignored) {
                }
            }
        }
        adContainer.removeAllViews();
    }

    /**
     * Clears potential native ad views from the container.
     */
    public void clearNativeIn(FrameLayout adContainer) {
        if (adContainer == null)
            return;
        adContainer.removeAllViews();
    }
}
