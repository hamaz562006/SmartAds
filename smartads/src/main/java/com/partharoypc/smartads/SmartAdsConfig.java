package com.partharoypc.smartads;

import com.google.android.gms.ads.RequestConfiguration;
import com.partharoypc.smartads.house.HouseAd;

import java.util.ArrayList;
import java.util.List;

public class SmartAdsConfig {
    // Ad Unit IDs
    private final String adMobAppOpenId;
    private final String adMobBannerId;
    private final String adMobInterstitialId;
    private final String adMobRewardedId;
    private final String adMobRewardedInterstitialId;
    private final String adMobNativeId;

    // Flags
    private final boolean adsEnabled;
    private final boolean isPremium;
    private final boolean isTestMode;
    private final boolean isLoggingEnabled;
    private final boolean collapsibleBannerEnabled;
    private final String collapsibleBannerPosition;
    private final boolean useUmpConsent;
    private final boolean houseAdsEnabled;
    private final int umpDebugGeography;
    private final String umpTestDeviceHashedId;

    // Granular format switches
    private final boolean bannerEnabled;
    private final boolean interstitialEnabled;
    private final boolean rewardedEnabled;
    private final boolean rewardedInterstitialEnabled;
    private final boolean nativeEnabled;
    private final boolean appOpenEnabled;

    // Targeting & Limits
    private final String maxAdContentRating;
    private final int tagForChildDirectedTreatment;
    private final int tagForUnderAgeOfConsent;
    private final long frequencyCapSeconds;
    private final long interstitialIntervalSeconds;
    private final long delayAfterRewardedSeconds;
    private final long appOpenIntervalSeconds;

    // UI Customization
    private final Integer dialogBackgroundColor;
    private final Integer dialogTextColor;
    private final Integer dialogSubTextColor;
    private final Integer dialogProgressColor;
    private final String dialogText;
    private final String dialogSubText;

    // Testing & House Ads & Preload
    private final List<String> testDeviceIds;
    private final List<HouseAd> houseAds;
    private final java.util.Map<String, String> screenPreloadRules;

    public Builder toBuilder() {
        return new Builder(this);
    }

    private SmartAdsConfig(Builder builder) {
        this.adMobAppOpenId = builder.adMobAppOpenId;
        this.adMobBannerId = builder.adMobBannerId;
        this.adMobInterstitialId = builder.adMobInterstitialId;
        this.adMobRewardedId = builder.adMobRewardedId;
        this.adMobRewardedInterstitialId = builder.adMobRewardedInterstitialId;
        this.adMobNativeId = builder.adMobNativeId;

        this.adsEnabled = builder.adsEnabled;
        this.isPremium = builder.isPremium;
        this.isTestMode = builder.isTestMode;
        this.isLoggingEnabled = builder.isLoggingEnabled;
        this.collapsibleBannerEnabled = builder.collapsibleBannerEnabled;
        this.collapsibleBannerPosition = builder.collapsibleBannerPosition;
        this.useUmpConsent = builder.useUmpConsent;
        this.houseAdsEnabled = builder.houseAdsEnabled;
        this.umpDebugGeography = builder.umpDebugGeography;
        this.umpTestDeviceHashedId = builder.umpTestDeviceHashedId;

        this.bannerEnabled = builder.bannerEnabled;
        this.interstitialEnabled = builder.interstitialEnabled;
        this.rewardedEnabled = builder.rewardedEnabled;
        this.rewardedInterstitialEnabled = builder.rewardedInterstitialEnabled;
        this.nativeEnabled = builder.nativeEnabled;
        this.appOpenEnabled = builder.appOpenEnabled;

        this.maxAdContentRating = builder.maxAdContentRating;
        this.tagForChildDirectedTreatment = builder.tagForChildDirectedTreatment;
        this.tagForUnderAgeOfConsent = builder.tagForUnderAgeOfConsent;
        this.frequencyCapSeconds = builder.frequencyCapSeconds;
        this.interstitialIntervalSeconds = builder.interstitialIntervalSeconds;
        this.delayAfterRewardedSeconds = builder.delayAfterRewardedSeconds;
        this.appOpenIntervalSeconds = builder.appOpenIntervalSeconds;

        this.dialogBackgroundColor = builder.dialogBackgroundColor;
        this.dialogTextColor = builder.dialogTextColor;
        this.dialogSubTextColor = builder.dialogSubTextColor;
        this.dialogProgressColor = builder.dialogProgressColor;
        this.dialogText = builder.dialogText;
        this.dialogSubText = builder.dialogSubText;

        this.testDeviceIds = List.copyOf(builder.testDeviceIds);
        this.houseAds = List.copyOf(builder.houseAds);
        this.screenPreloadRules = java.util.Collections.unmodifiableMap(new java.util.HashMap<>(builder.screenPreloadRules));
    }

    // =============================================================================================
    // GETTERS
    // =============================================================================================

    public String getAdMobAppOpenId() {
        return adMobAppOpenId;
    }

    public String getAdMobBannerId() {
        return adMobBannerId;
    }

    public String getAdMobInterstitialId() {
        return adMobInterstitialId;
    }

    public String getAdMobRewardedId() {
        return adMobRewardedId;
    }

    public String getAdMobRewardedInterstitialId() {
        return adMobRewardedInterstitialId;
    }

    public String getAdMobNativeId() {
        return adMobNativeId;
    }

    public boolean isAdsEnabled() {
        return adsEnabled;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public boolean isTestMode() {
        return isTestMode;
    }

    public boolean isLoggingEnabled() {
        return isLoggingEnabled;
    }

    public boolean isCollapsibleBannerEnabled() {
        return collapsibleBannerEnabled;
    }

    public String getCollapsibleBannerPosition() {
        return collapsibleBannerPosition;
    }

    public boolean useUmpConsent() {
        return useUmpConsent;
    }

    public boolean isHouseAdsEnabled() {
        return houseAdsEnabled;
    }

    public int getUmpDebugGeography() {
        return umpDebugGeography;
    }

    public String getUmpTestDeviceHashedId() {
        return umpTestDeviceHashedId;
    }

    public boolean isBannerEnabled() {
        return bannerEnabled;
    }

    public boolean isInterstitialEnabled() {
        return interstitialEnabled;
    }

    public boolean isRewardedEnabled() {
        return rewardedEnabled;
    }

    public boolean isRewardedInterstitialEnabled() {
        return rewardedInterstitialEnabled;
    }

    public boolean isNativeEnabled() {
        return nativeEnabled;
    }

    public boolean isAppOpenEnabled() {
        return appOpenEnabled;
    }

    public String getMaxAdContentRating() {
        return maxAdContentRating;
    }

    public int getTagForChildDirectedTreatment() {
        return tagForChildDirectedTreatment;
    }

    public int getTagForUnderAgeOfConsent() {
        return tagForUnderAgeOfConsent;
    }

    public long getFrequencyCapSeconds() {
        return frequencyCapSeconds;
    }

    public long getInterstitialIntervalSeconds() {
        return interstitialIntervalSeconds;
    }

    public long getDelayAfterRewardedSeconds() {
        return delayAfterRewardedSeconds;
    }

    public long getAppOpenIntervalSeconds() {
        return appOpenIntervalSeconds;
    }

    public java.util.Map<String, String> getScreenPreloadRules() {
        return screenPreloadRules;
    }

    public Integer getDialogBackgroundColor() {
        return dialogBackgroundColor;
    }

    public Integer getDialogTextColor() {
        return dialogTextColor;
    }

    public Integer getDialogSubTextColor() {
        return dialogSubTextColor;
    }

    public Integer getDialogProgressColor() {
        return dialogProgressColor;
    }

    public String getDialogText() {
        return dialogText;
    }

    public String getDialogSubText() {
        return dialogSubText;
    }

    public List<String> getTestDeviceIds() {
        return testDeviceIds;
    }

    public List<HouseAd> getHouseAds() {
        return houseAds;
    }

    // =============================================================================================
    // HELPER METHODS
    // =============================================================================================

    public boolean isAppOpenConfigured() {
        return adMobAppOpenId != null && !adMobAppOpenId.isEmpty();
    }

    public boolean isBannerConfigured() {
        return adMobBannerId != null && !adMobBannerId.isEmpty();
    }

    public boolean isInterstitialConfigured() {
        return adMobInterstitialId != null && !adMobInterstitialId.isEmpty();
    }

    public boolean isRewardedConfigured() {
        return adMobRewardedId != null && !adMobRewardedId.isEmpty();
    }

    public boolean isRewardedInterstitialConfigured() {
        return adMobRewardedInterstitialId != null && !adMobRewardedInterstitialId.isEmpty();
    }

    public boolean isNativeConfigured() {
        return adMobNativeId != null && !adMobNativeId.isEmpty();
    }

    public boolean isAnyAdConfigured() {
        return isAppOpenConfigured() || isBannerConfigured() || isInterstitialConfigured() ||
                isRewardedConfigured() || isRewardedInterstitialConfigured() || isNativeConfigured();
    }

    // =============================================================================================
    // BUILDER
    // =============================================================================================

    public static class Builder {
        // IDs
        private String adMobAppOpenId = "";
        private String adMobBannerId = "";
        private String adMobInterstitialId = "";
        private String adMobRewardedId = "";
        private String adMobRewardedInterstitialId = "";
        private String adMobNativeId = "";

        // Flags (All Defaulted to FALSE)
        private boolean adsEnabled = false;
        private boolean isPremium = false;
        private boolean isTestMode = false;
        private boolean isLoggingEnabled = false;
        private boolean collapsibleBannerEnabled = false;
        private String collapsibleBannerPosition = "bottom";
        private boolean useUmpConsent = false;
        private boolean houseAdsEnabled = false;
        private int umpDebugGeography = com.google.android.ump.ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_DISABLED;
        private String umpTestDeviceHashedId = "";

        // Granular switches (Default to true so they are active unless explicitly
        // disabled)
        private boolean bannerEnabled = true;
        private boolean interstitialEnabled = true;
        private boolean rewardedEnabled = true;
        private boolean rewardedInterstitialEnabled = true;
        private boolean nativeEnabled = true;
        private boolean appOpenEnabled = true;

        // Targeting
        private String maxAdContentRating = RequestConfiguration.MAX_AD_CONTENT_RATING_G;
        private int tagForChildDirectedTreatment = RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED;
        private int tagForUnderAgeOfConsent = RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED;

        // Limits
        private long frequencyCapSeconds = 30;
        private long interstitialIntervalSeconds = 30;
        private long delayAfterRewardedSeconds = 30;
        private long appOpenIntervalSeconds = 15;

        // UI
        private Integer dialogBackgroundColor = null;
        private Integer dialogTextColor = null;
        private Integer dialogSubTextColor = null;
        private Integer dialogProgressColor = null;
        private String dialogText = "Loading Ad...";
        private String dialogSubText = "Optimizing experience...";

        // Lists
        private final List<String> testDeviceIds = new ArrayList<>();
        private List<HouseAd> houseAds = new ArrayList<>();
        private final java.util.Map<String, String> screenPreloadRules = new java.util.HashMap<>();

        public Builder() {
        }

        /**
         * Copy constructor.
         */
        public Builder(SmartAdsConfig config) {
            this.adMobAppOpenId = config.adMobAppOpenId;
            this.adMobBannerId = config.adMobBannerId;
            this.adMobInterstitialId = config.adMobInterstitialId;
            this.adMobRewardedId = config.adMobRewardedId;
            this.adMobRewardedInterstitialId = config.adMobRewardedInterstitialId;
            this.adMobNativeId = config.adMobNativeId;

            this.adsEnabled = config.adsEnabled;
            this.isPremium = config.isPremium;
            this.isTestMode = config.isTestMode;
            this.isLoggingEnabled = config.isLoggingEnabled;
            this.collapsibleBannerEnabled = config.collapsibleBannerEnabled;
            this.collapsibleBannerPosition = config.collapsibleBannerPosition;
            this.useUmpConsent = config.useUmpConsent;
            this.houseAdsEnabled = config.houseAdsEnabled;
            this.umpDebugGeography = config.umpDebugGeography;
            this.umpTestDeviceHashedId = config.umpTestDeviceHashedId;

            this.bannerEnabled = config.bannerEnabled;
            this.interstitialEnabled = config.interstitialEnabled;
            this.rewardedEnabled = config.rewardedEnabled;
            this.rewardedInterstitialEnabled = config.rewardedInterstitialEnabled;
            this.nativeEnabled = config.nativeEnabled;
            this.appOpenEnabled = config.appOpenEnabled;

            this.maxAdContentRating = config.maxAdContentRating;
            this.tagForChildDirectedTreatment = config.tagForChildDirectedTreatment;
            this.tagForUnderAgeOfConsent = config.tagForUnderAgeOfConsent;
            this.frequencyCapSeconds = config.frequencyCapSeconds;
            this.interstitialIntervalSeconds = config.interstitialIntervalSeconds;
            this.delayAfterRewardedSeconds = config.delayAfterRewardedSeconds;
            this.appOpenIntervalSeconds = config.appOpenIntervalSeconds;

            this.dialogBackgroundColor = config.dialogBackgroundColor;
            this.dialogTextColor = config.dialogTextColor;
            this.dialogSubTextColor = config.dialogSubTextColor;
            this.dialogProgressColor = config.dialogProgressColor;
            this.dialogText = config.dialogText;
            this.dialogSubText = config.dialogSubText;

            this.testDeviceIds.addAll(config.testDeviceIds);
            this.houseAds.addAll(config.houseAds);
            this.screenPreloadRules.putAll(config.screenPreloadRules);
        }

        // --- Ad Unit IDs ---

        /**
         * Sets the AdMob App Open Ad unit ID.
         */
        public Builder setAdMobAppOpenId(String id) {
            this.adMobAppOpenId = id;
            return this;
        }

        /**
         * Sets the AdMob Banner Ad unit ID.
         */
        public Builder setAdMobBannerId(String id) {
            this.adMobBannerId = id;
            return this;
        }

        /**
         * Sets the AdMob Interstitial Ad unit ID.
         */
        public Builder setAdMobInterstitialId(String id) {
            this.adMobInterstitialId = id;
            return this;
        }

        /**
         * Sets the AdMob Rewarded Ad unit ID.
         */
        public Builder setAdMobRewardedId(String id) {
            this.adMobRewardedId = id;
            return this;
        }

        /**
         * Sets the AdMob Rewarded Interstitial Ad unit ID.
         */
        public Builder setAdMobRewardedInterstitialId(String id) {
            this.adMobRewardedInterstitialId = id;
            return this;
        }

        /**
         * Sets the AdMob Native Ad unit ID.
         */
        public Builder setAdMobNativeId(String id) {
            this.adMobNativeId = id;
            return this;
        }

        // --- Feature Flags ---

        /**
         * Sets whether ads are globally enabled. Default: false
         */
        public Builder setAdsEnabled(boolean enabled) {
            this.adsEnabled = enabled;
            return this;
        }

        /**
         * Sets whether the current user is premium/paid (blocks all ads when true). Default: false
         */
        public Builder setPremium(boolean isPremium) {
            this.isPremium = isPremium;
            return this;
        }

        /**
         * Sets whether Test Mode is enabled. Default: false
         */
        public Builder setTestModeEnabled(boolean enabled) {
            this.isTestMode = enabled;
            return this;
        }

        /**
         * Sets whether debug logging is enabled. Default: false
         */
        public Builder setLoggingEnabled(boolean enabled) {
            this.isLoggingEnabled = enabled;
            return this;
        }

        /**
         * Sets whether House Ads (internal cross-promotion) are enabled. Default: false
         */
        public Builder setHouseAdsEnabled(boolean enabled) {
            this.houseAdsEnabled = enabled;
            return this;
        }

        /**
         * Sets whether collapsible banners are enabled. Default: false
         */
        public Builder setCollapsibleBannerEnabled(boolean enabled) {
            this.collapsibleBannerEnabled = enabled;
            return this;
        }

        /**
         * Sets the position for collapsible banners ("top" or "bottom"). Default: "bottom"
         */
        public Builder setCollapsibleBannerPosition(String position) {
            this.collapsibleBannerPosition = position;
            return this;
        }

        /**
         * Sets whether to use Google UMP for consent. Default: false
         */
        public Builder setUseUmpConsent(boolean useUmp) {
            this.useUmpConsent = useUmp;
            return this;
        }

        /**
         * Sets UMP Debug Geography for testing GDPR (e.g. DEBUG_GEOGRAPHY_EEA).
         */
        public Builder setUmpDebugGeography(int geography) {
            this.umpDebugGeography = geography;
            return this;
        }

        /**
         * Sets UMP Test Device Hashed ID for debugging consent forms.
         */
        public Builder setUmpTestDeviceHashedId(String hashedId) {
            this.umpTestDeviceHashedId = hashedId;
            return this;
        }

        /**
         * Enables or disables Banner ads. Default: true
         */
        public Builder setBannerEnabled(boolean enabled) {
            this.bannerEnabled = enabled;
            return this;
        }

        /**
         * Enables or disables Interstitial ads. Default: true
         */
        public Builder setInterstitialEnabled(boolean enabled) {
            this.interstitialEnabled = enabled;
            return this;
        }

        /**
         * Enables or disables Rewarded ads. Default: true
         */
        public Builder setRewardedEnabled(boolean enabled) {
            this.rewardedEnabled = enabled;
            return this;
        }

        /**
         * Enables or disables Rewarded Interstitial ads. Default: true
         */
        public Builder setRewardedInterstitialEnabled(boolean enabled) {
            this.rewardedInterstitialEnabled = enabled;
            return this;
        }

        /**
         * Enables or disables Native ads. Default: true
         */
        public Builder setNativeEnabled(boolean enabled) {
            this.nativeEnabled = enabled;
            return this;
        }

        /**
         * Enables or disables App Open ads. Default: true
         */
        public Builder setAppOpenEnabled(boolean enabled) {
            this.appOpenEnabled = enabled;
            return this;
        }

        // --- Configuration & Limits ---

        /**
         * Sets the maximum content rating for ads (e.g.
         * RequestConfiguration.MAX_AD_CONTENT_RATING_G).
         */
        public Builder setMaxAdContentRating(String rating) {
            this.maxAdContentRating = rating;
            return this;
        }

        /**
         * Sets tag for child directed treatment.
         */
        public Builder setTagForChildDirectedTreatment(int tag) {
            this.tagForChildDirectedTreatment = tag;
            return this;
        }

        /**
         * Sets tag for under age of consent.
         */
        public Builder setTagForUnderAgeOfConsent(int tag) {
            this.tagForUnderAgeOfConsent = tag;
            return this;
        }

        /**
         * Sets frequency cap for full-screen ads in seconds. Default: 30s
         */
        public Builder setFrequencyCapSeconds(long seconds) {
            this.frequencyCapSeconds = seconds;
            return this;
        }

        /**
         * Sets minimum time interval between consecutive Interstitial ads in seconds. Default: 30s
         */
        public Builder setInterstitialIntervalSeconds(long seconds) {
            this.interstitialIntervalSeconds = seconds;
            return this;
        }

        /**
         * Sets minimum cooldown delay after watching a Rewarded ad before an Interstitial can show in seconds. Default: 30s
         */
        public Builder setDelayAfterRewardedSeconds(long seconds) {
            this.delayAfterRewardedSeconds = seconds;
            return this;
        }

        /**
         * Sets minimum time interval between consecutive App Open ads in seconds. Default: 15s
         */
        public Builder setAppOpenIntervalSeconds(long seconds) {
            this.appOpenIntervalSeconds = seconds;
            return this;
        }

        /**
         * Sets screen preload mapping rules (screenName -> comma-separated formats e.g. "INTERSTITIAL,REWARDED").
         */
        public Builder setScreenPreloadRules(java.util.Map<String, String> rules) {
            this.screenPreloadRules.clear();
            if (rules != null) {
                this.screenPreloadRules.putAll(rules);
            }
            return this;
        }

        /**
         * Adds a single screen preload rule.
         */
        public Builder addScreenPreloadRule(String screenName, String formats) {
            if (screenName != null && formats != null) {
                this.screenPreloadRules.put(screenName, formats);
            }
            return this;
        }

        // --- UI & Customization ---

        public Builder setLoadingDialogColor(@androidx.annotation.ColorInt int backgroundColor,
                @androidx.annotation.ColorInt int textColor) {
            this.dialogBackgroundColor = backgroundColor;
            this.dialogTextColor = textColor;
            return this;
        }

        public Builder setLoadingDialogSubTextColor(@androidx.annotation.ColorInt int subTextColor) {
            this.dialogSubTextColor = subTextColor;
            return this;
        }

        public Builder setLoadingDialogProgressColor(@androidx.annotation.ColorInt int progressColor) {
            this.dialogProgressColor = progressColor;
            return this;
        }

        public Builder setLoadingDialogText(String headline, String subHeadline) {
            this.dialogText = headline;
            this.dialogSubText = subHeadline;
            return this;
        }

        public Builder setLoadingDialogText(String headline) {
            this.dialogText = headline;
            return this;
        }

        // --- Lists ---

        public Builder addTestDeviceId(String id) {
            if (id != null && !id.isEmpty()) {
                this.testDeviceIds.add(id);
            }
            return this;
        }

        public Builder addHouseAd(HouseAd ad) {
            if (ad != null) {
                this.houseAds.add(ad);
            }
            return this;
        }

        public Builder setHouseAds(List<HouseAd> ads) {
            if (ads != null) {
                this.houseAds = new ArrayList<>(ads);
            }
            return this;
        }

        public SmartAdsConfig build() {
            return new SmartAdsConfig(this);
        }
    }
}
