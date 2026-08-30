package com.partharoypc.smartads.utils;

import com.partharoypc.smartads.SmartAdsConfig;

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
        if (builder == null || remoteValues == null) {
            return builder != null ? builder : new SmartAdsConfig.Builder();
        }

        for (Map.Entry<String, ?> entry : remoteValues.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (key == null || val == null) continue;

            String lowerKey = key.toLowerCase();

            switch (lowerKey) {
                case "enable_ads":
                case "ads_enabled":
                    builder.setAdsEnabled(parseBoolean(val));
                    break;
                case "is_test_mode":
                case "test_mode":
                    builder.setTestModeEnabled(parseBoolean(val));
                    break;
                case "enable_banner":
                case "banner_enabled":
                    builder.setBannerEnabled(parseBoolean(val));
                    break;
                case "enable_interstitial":
                case "interstitial_enabled":
                    builder.setInterstitialEnabled(parseBoolean(val));
                    break;
                case "enable_rewarded":
                case "rewarded_enabled":
                    builder.setRewardedEnabled(parseBoolean(val));
                    break;
                case "enable_rewarded_interstitial":
                case "rewarded_interstitial_enabled":
                    builder.setRewardedInterstitialEnabled(parseBoolean(val));
                    break;
                case "enable_native":
                case "native_enabled":
                    builder.setNativeEnabled(parseBoolean(val));
                    break;
                case "enable_app_open":
                case "app_open_enabled":
                    builder.setAppOpenEnabled(parseBoolean(val));
                    break;
                case "enable_collapsible_banner":
                case "collapsible_banner_enabled":
                    builder.setCollapsibleBannerEnabled(parseBoolean(val));
                    break;
                case "collapsible_banner_position":
                    builder.setCollapsibleBannerPosition(String.valueOf(val));
                    break;
                case "enable_house_ads":
                case "house_ads_enabled":
                    builder.setHouseAdsEnabled(parseBoolean(val));
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
                default:
                    break;
            }
        }

        return builder;
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
