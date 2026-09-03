package com.partharoypc.smartads.utils;

import com.partharoypc.smartads.AdSource;
import com.partharoypc.smartads.SmartAdsConfig;
import com.partharoypc.smartads.house.HouseAd;
import com.partharoypc.smartads.house.HouseAdsRemoteParser;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utility to map remote configuration keys (e.g. from Firebase Remote Config,
 * JSON endpoints, or Key-Value stores) directly into a SmartAdsConfig.Builder.
 */
public final class SmartAdsRemoteConfigMapper {

    private SmartAdsRemoteConfigMapper() {
    }

    /**
     * Applies remote configuration values from a Map to an existing or new SmartAdsConfig.Builder.
     *
     * @param builder Base builder (or newly instantiated builder).
     * @param remoteValues Map containing remote key-value pairs.
     * @return The updated builder.
     */
    public static SmartAdsConfig.Builder applyMap(SmartAdsConfig.Builder builder, Map<String, ?> remoteValues) {
        return applyRemoteConfig(builder, remoteValues);
    }

    /**
     * Alias for applyMap.
     */
    public static SmartAdsConfig.Builder applyRemoteConfig(SmartAdsConfig.Builder builder, Map<String, ?> remoteValues) {
        if (builder == null || remoteValues == null) {
            return builder != null ? builder : new SmartAdsConfig.Builder();
        }

        for (Map.Entry<String, ?> entry : remoteValues.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (key == null || val == null) continue;

            String lowerKey = key.toLowerCase(Locale.ROOT);

            if (lowerKey.startsWith("preload_")) {
                String screen = key.substring(8);
                builder.addScreenPreloadRule(screen, String.valueOf(val));
                continue;
            }

            switch (lowerKey) {
                case "enable_ads":
                case "ads_enabled":
                    builder.setAdsEnabled(parseBoolean(val));
                    break;
                case "is_premium":
                case "premium_user":
                case "premium":
                case "user_premium":
                    builder.setPremium(parseBoolean(val));
                    break;
                case "is_test_mode":
                case "test_mode":
                case "debug_mode":
                    builder.setTestModeEnabled(parseBoolean(val));
                    break;
                case "enable_banner":
                case "banner_enabled":
                case "banner_enable":
                case "enable_bn":
                case "bn_enable":
                    builder.setBannerEnabled(parseBoolean(val));
                    break;
                case "enable_interstitial":
                case "interstitial_enabled":
                case "interstitial_enable":
                case "enable_iv":
                case "iv_enable":
                    builder.setInterstitialEnabled(parseBoolean(val));
                    break;
                case "enable_rewarded":
                case "rewarded_enabled":
                case "rewarded_enable":
                case "enable_rv":
                case "rv_enable":
                    builder.setRewardedEnabled(parseBoolean(val));
                    break;
                case "enable_rewarded_interstitial":
                case "rewarded_interstitial_enabled":
                case "rewarded_interstitial_enable":
                case "enable_ri":
                case "ri_enable":
                    builder.setRewardedInterstitialEnabled(parseBoolean(val));
                    break;
                case "enable_native":
                case "native_enabled":
                case "native_enable":
                case "enable_nt":
                case "nt_enable":
                    builder.setNativeEnabled(parseBoolean(val));
                    break;
                case "enable_app_open":
                case "app_open_enabled":
                case "app_open_enable":
                case "enable_ao":
                case "ao_enable":
                    builder.setAppOpenEnabled(parseBoolean(val));
                    break;
                case "enable_collapsible_banner":
                case "collapsible_banner_enabled":
                case "collapsible_banner_enable":
                    builder.setCollapsibleBannerEnabled(parseBoolean(val));
                    break;
                case "collapsible_banner_position":
                    builder.setCollapsibleBannerPosition(String.valueOf(val));
                    break;
                case "enable_house_ads":
                case "house_ads_enabled":
                case "house_ads_enable":
                    builder.setHouseAdsEnabled(parseBoolean(val));
                    break;
                case "house_ads_auto_fallback":
                case "auto_fallback_house_ads":
                    builder.setHouseAdsAutoFallback(parseBoolean(val));
                    break;
                case "house_ads_json":
                    String jsonString = String.valueOf(val).trim();
                    if (jsonString.startsWith("[")) {
                        List<HouseAd> remoteHouseAds = HouseAdsRemoteParser.parseFromJson(jsonString);
                        if (!remoteHouseAds.isEmpty()) {
                            builder.setHouseAds(remoteHouseAds);
                        }
                    }
                    break;
                case "bn_source":
                case "banner_source":
                    builder.setBannerSource(parseAdSource(val));
                    break;
                case "iv_source":
                case "interstitial_source":
                    builder.setInterstitialSource(parseAdSource(val));
                    break;
                case "rv_source":
                case "rewarded_source":
                    builder.setRewardedSource(parseAdSource(val));
                    break;
                case "ri_source":
                case "rewarded_interstitial_source":
                    builder.setRewardedInterstitialSource(parseAdSource(val));
                    break;
                case "nt_source":
                case "native_source":
                    builder.setNativeSource(parseAdSource(val));
                    break;
                case "ao_source":
                case "app_open_source":
                    builder.setAppOpenSource(parseAdSource(val));
                    break;
                case "banner_id":
                case "admob_banner_id":
                    builder.setAdMobBannerId(String.valueOf(val));
                    break;
                case "interstitial_id":
                case "admob_interstitial_id":
                    builder.setAdMobInterstitialId(String.valueOf(val));
                    break;
                case "rewarded_id":
                case "admob_rewarded_id":
                    builder.setAdMobRewardedId(String.valueOf(val));
                    break;
                case "rewarded_interstitial_id":
                case "admob_rewarded_interstitial_id":
                    builder.setAdMobRewardedInterstitialId(String.valueOf(val));
                    break;
                case "native_id":
                case "admob_native_id":
                    builder.setAdMobNativeId(String.valueOf(val));
                    break;
                case "app_open_id":
                case "admob_app_open_id":
                    builder.setAdMobAppOpenId(String.valueOf(val));
                    break;
                case "frequency_cap_seconds":
                case "frequency_cap":
                    builder.setFrequencyCapSeconds(parseLong(val, 30L));
                    break;
                case "iv_interval":
                case "interstitial_interval":
                case "interstitial_interval_seconds":
                    builder.setInterstitialIntervalSeconds(parseLong(val, 30L));
                    break;
                case "rv_to_iv_delay":
                case "delay_after_rewarded":
                case "delay_after_rewarded_seconds":
                    builder.setDelayAfterRewardedSeconds(parseLong(val, 30L));
                    break;
                case "ao_interval":
                case "app_open_interval":
                case "app_open_interval_seconds":
                    builder.setAppOpenIntervalSeconds(parseLong(val, 15L));
                    break;
                default:
                    break;
            }
        }

        return builder;
    }

    private static AdSource parseAdSource(Object val) {
        if (val == null) return AdSource.ADMOB;
        String s = String.valueOf(val).trim().toUpperCase(Locale.ROOT);
        if ("HOUSE".equals(s) || "HOUSE_ADS".equals(s) || "HOUSEAD".equals(s)) {
            return AdSource.HOUSE;
        }
        return AdSource.ADMOB;
    }

    private static boolean parseBoolean(Object val) {
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        return "true".equalsIgnoreCase(String.valueOf(val)) || "1".equals(String.valueOf(val));
    }

    private static long parseLong(Object val, long defaultVal) {
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(val));
        } catch (Exception ignored) {
            return defaultVal;
        }
    }
}

