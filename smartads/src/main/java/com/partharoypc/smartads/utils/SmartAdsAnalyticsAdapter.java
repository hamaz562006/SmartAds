package com.partharoypc.smartads.utils;

import android.os.Bundle;

import com.partharoypc.smartads.analytics.SmartAdsAnalyticsListener;

/**
 * Standard adapter to convert SmartAds ad revenue events into Android Bundle format,
 * compatible with Firebase Analytics, AppsFlyer, Adjust, or custom analytics endpoints.
 */
public abstract class SmartAdsAnalyticsAdapter implements SmartAdsAnalyticsListener {

    /**
     * Called whenever an ad impression records revenue, providing a pre-formatted Bundle
     * following standard ad_impression event schemas.
     *
     * @param eventName The event name, typically "ad_impression".
     * @param params    The bundle containing value, currency, ad_platform, ad_unit_name, and ad_format.
     */
    public abstract void onAdRevenueEvent(String eventName, Bundle params);

    @Override
    public void onAdRevenuePaid(String adUnitId, String adFormat, String adNetwork,
                                long valueMicros, String currencyCode, int precision,
                                Bundle extras) {
        Bundle bundle = new Bundle();
        if (extras != null) {
            bundle.putAll(extras);
        }
        double value = valueMicros / 1000000.0;
        bundle.putDouble("value", value);
        bundle.putString("currency", currencyCode != null ? currencyCode : "USD");
        bundle.putString("ad_platform", adNetwork != null ? adNetwork : "Google AdMob");
        bundle.putString("ad_unit_name", adUnitId != null ? adUnitId : "");
        bundle.putString("ad_format", adFormat != null ? adFormat : "");
        bundle.putInt("precision_type", precision);

        onAdRevenueEvent("ad_impression", bundle);
    }
}
