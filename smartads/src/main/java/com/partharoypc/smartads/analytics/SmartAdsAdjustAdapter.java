package com.partharoypc.smartads.analytics;

import android.os.Bundle;

import com.partharoypc.smartads.SmartAdsLogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Native analytics adapter for Adjust SDK ad revenue tracking (Impression-Level Ad Revenue - ILAR).
 * Converts SmartAds revenue callbacks into AdjustAdRevenue objects and tracks them via Adjust.trackAdRevenue().
 *
 * Implemented using safe reflection so host applications can include or omit the Adjust SDK dependency
 * without encountering ClassNotFoundException or compile errors.
 */
public class SmartAdsAdjustAdapter implements SmartAdsAnalyticsListener {

    private static final String ADJUST_AD_REVENUE_CLASS = "com.adjust.sdk.AdjustAdRevenue";
    private static final String ADJUST_CLASS = "com.adjust.sdk.Adjust";
    private static final String ADJUST_CONFIG_CLASS = "com.adjust.sdk.AdjustConfig";

    private final String adRevenueSource;

    /**
     * Default constructor uses AdMob as the ad revenue source.
     */
    public SmartAdsAdjustAdapter() {
        this("admob_sdk");
    }

    /**
     * Constructor allowing custom ad revenue source identifier.
     *
     * @param adRevenueSource Ad revenue source (e.g., "admob_sdk", AdjustConfig.AD_REVENUE_ADMOB).
     */
    public SmartAdsAdjustAdapter(String adRevenueSource) {
        this.adRevenueSource = adRevenueSource != null ? adRevenueSource : "admob_sdk";
    }

    @Override
    public void onAdRevenuePaid(String adUnitId, String adFormat, String adNetwork,
                                long valueMicros, String currencyCode, int precision,
                                Bundle extras) {
        double revenueAmount = valueMicros / 1_000_000.0;
        String currency = (currencyCode != null && !currencyCode.isEmpty()) ? currencyCode : "USD";
        String network = (adNetwork != null && !adNetwork.isEmpty()) ? adNetwork : "Google AdMob";

        boolean tracked = trackWithAdjustReflection(revenueAmount, currency, network, adUnitId, adFormat);
        if (tracked) {
            SmartAdsLogger.d(String.format("Adjust ILAR tracked: %.6f %s | Unit: %s | Format: %s",
                    revenueAmount, currency, adUnitId, adFormat));
        } else {
            SmartAdsLogger.d("Adjust SDK not present on classpath or error occurred while tracking revenue.");
        }
    }

    /**
     * Direct method to track ad revenue.
     */
    public void trackAdRevenue(String adUnitId, String adFormat, String adNetwork,
                               long valueMicros, String currencyCode, int precision) {
        onAdRevenuePaid(adUnitId, adFormat, adNetwork, valueMicros, currencyCode, precision, null);
    }

    /**
     * Checks if the Adjust SDK is integrated in the application.
     */
    public static boolean isAdjustSdkAvailable() {
        try {
            Class.forName(ADJUST_CLASS);
            Class.forName(ADJUST_AD_REVENUE_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean trackWithAdjustReflection(double revenue, String currency, String network,
                                              String adUnitId, String adFormat) {
        try {
            Class<?> adjustRevenueClass = Class.forName(ADJUST_AD_REVENUE_CLASS);
            Class<?> adjustClass = Class.forName(ADJUST_CLASS);

            Constructor<?> constructor = adjustRevenueClass.getConstructor(String.class);
            Object adjustAdRevenue = constructor.newInstance(adRevenueSource);

            // setRevenue(Double revenue, String currency)
            Method setRevenueMethod = adjustRevenueClass.getMethod("setRevenue", Double.class, String.class);
            setRevenueMethod.invoke(adjustAdRevenue, revenue, currency);

            // setAdRevenueNetwork(String network)
            try {
                Method setNetworkMethod = adjustRevenueClass.getMethod("setAdRevenueNetwork", String.class);
                setNetworkMethod.invoke(adjustAdRevenue, network);
            } catch (NoSuchMethodException ignored) {
            }

            // setAdRevenueUnit(String unit)
            try {
                Method setUnitMethod = adjustRevenueClass.getMethod("setAdRevenueUnit", String.class);
                setUnitMethod.invoke(adjustAdRevenue, adUnitId);
            } catch (NoSuchMethodException ignored) {
            }

            // setAdRevenuePlacement(String placement)
            try {
                Method setPlacementMethod = adjustRevenueClass.getMethod("setAdRevenuePlacement", String.class);
                setPlacementMethod.invoke(adjustAdRevenue, adFormat);
            } catch (NoSuchMethodException ignored) {
            }

            // Adjust.trackAdRevenue(adjustAdRevenue)
            Method trackMethod = adjustClass.getMethod("trackAdRevenue", adjustRevenueClass);
            trackMethod.invoke(null, adjustAdRevenue);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            SmartAdsLogger.e("Failed to forward ad revenue to Adjust SDK: " + e.getMessage());
            return false;
        }
    }
}
