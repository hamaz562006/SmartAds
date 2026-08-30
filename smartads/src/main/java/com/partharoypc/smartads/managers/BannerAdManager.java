package com.partharoypc.smartads.managers;

import android.Manifest;
import android.app.Activity;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.partharoypc.smartads.SmartAds;
import com.partharoypc.smartads.SmartAdsConfig;
import com.partharoypc.smartads.TestAdIds;
import android.view.View;
import android.widget.ImageView;

import com.partharoypc.smartads.R;
import com.partharoypc.smartads.SmartAdsLogger;
import com.partharoypc.smartads.house.HouseAd;
import com.partharoypc.smartads.house.HouseAdLoader;
import com.partharoypc.smartads.listeners.BannerAdListener;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Manages loading and displaying Banner Ads (AdMob and House Ads fallback).
 */
public class BannerAdManager {
    private final Map<FrameLayout, Boolean> listenerAdded = new WeakHashMap<>();

    /**
     * Loads and shows a banner ad in the provided container.
     */
    @RequiresPermission(Manifest.permission.INTERNET)
    public void loadAndShowAd(Activity activity, FrameLayout adContainer, SmartAdsConfig config,
            BannerAdListener listener) {
        loadAdMob(activity, adContainer, config, listener);
    }

    @RequiresPermission(Manifest.permission.INTERNET)
    private void loadAdMob(Activity activity, FrameLayout adContainer, SmartAdsConfig config,
            BannerAdListener listener) {
        if (!SmartAds.getInstance().areAdsEnabled() || !config.isBannerEnabled()) {
            SmartAdsLogger.d("Banner Ad is disabled. Skipping request.");
            if (listener != null)
                listener.onAdFailed("Banner ads are disabled.");
            return;
        }
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (listener != null)
                listener.onAdFailed("Activity is not valid.");
            return;
        }

        // 1. Check Internet
        if (!com.partharoypc.smartads.utils.NetworkUtils.isNetworkAvailable(activity)) {
            SmartAdsLogger.d("No Internet Connection. Skipping AdMob Banner.");
            if (config.isHouseAdsEnabled()) {
                loadHouseBanner(activity, adContainer, config, listener);
            } else {
                if (listener != null)
                    listener.onAdFailed("No Internet Connection.");
            }
            return;
        }

        // 2. Check Ad Unit ID
        String adUnitId = (config.getAdMobBannerId() != null && !config.getAdMobBannerId().isEmpty())
                ? config.getAdMobBannerId()
                : (config.isTestMode() ? TestAdIds.ADMOB_BANNER_ID : null);
        if (adUnitId == null || adUnitId.isEmpty()) {
            if (config.isHouseAdsEnabled()) {
                SmartAdsLogger.d("AdMob Banner ID not set. Trying House Ad.");
                loadHouseBanner(activity, adContainer, config, listener);
            } else {
                if (listener != null)
                    listener.onAdFailed("Banner Ad Unit ID is not configured.");
            }
            return;
        }

        // 3. AdMob NO_FILL Rate Limiting
        if (com.partharoypc.smartads.utils.AdMobRateLimiter.isRateLimited(adUnitId)) {
            SmartAdsLogger.d("AdMob Rate Limiter active (NO_FILL Cooldown). Skipping AdMob Banner Request.");
            if (config.isHouseAdsEnabled()) {
                loadHouseBanner(activity, adContainer, config, listener);
            } else {
                if (listener != null)
                    listener.onAdFailed("Rate Limited (NO_FILL Cooldown)");
            }
            return;
        }

        // Clean up existing ads before loading new one
        destroy(adContainer);

        int containerWidth = adContainer.getWidth();
        if (containerWidth == 0) {
            // Check if view is actually visible or just not laid out
            if (adContainer.getVisibility() == View.GONE) {
                if (listener != null)
                    listener.onAdFailed("Banner Container is GONE. Cannot load ad.");
                return;
            }
            adContainer.post(() -> {
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    loadAdMob(activity, adContainer, config, listener);
                }
            });
            return;
        }

        SmartAdsLogger.d("Loading Banner Ad...");

        AdView admobBanner = new AdView(activity);
        float density = activity.getResources().getDisplayMetrics().density;
        int adWidthInDp = Math.max(0, (int) (containerWidth / density));
        AdSize adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidthInDp);
        admobBanner.setAdSize(adaptiveSize);
        admobBanner.setAdUnitId(adUnitId);
        admobBanner.setOnPaidEventListener(adValue -> {
            SmartAds.getInstance().reportPaidEvent(adValue, admobBanner.getResponseInfo(), adUnitId, "Banner");
        });
        admobBanner.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                SmartAdsLogger.d("✅ Banner Ad LOADED.");
                if (!isContainerActive(adContainer) || activity.isFinishing() || activity.isDestroyed()) {
                    try {
                        admobBanner.destroy();
                    } catch (Exception ignored) {
                    }
                    return;
                }
                adContainer.removeAllViews();
                adContainer.addView(admobBanner);
                if (listener != null)
                    listener.onAdLoaded(admobBanner);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                SmartAdsLogger.e("❌ Banner Ad Failed to Load: " + loadAdError.getMessage());
                if (loadAdError.getCode() == com.google.android.gms.ads.AdRequest.ERROR_CODE_NO_FILL) {
                    com.partharoypc.smartads.utils.AdMobRateLimiter.recordNoFill(adUnitId);
                }
                // FALLBACK TO HOUSE AD
                if (config.isHouseAdsEnabled()) {
                    loadHouseBanner(activity, adContainer, config, listener);
                } else {
                    if (listener != null) {
                        listener.onAdFailed(loadAdError.getMessage());
                    }
                }
            }

            @Override
            public void onAdOpened() {
                SmartAdsLogger.d("Banner Ad Clicked/Opened.");
                if (listener != null)
                    listener.onAdClicked();
            }

            @Override
            public void onAdImpression() {
                SmartAdsLogger.d("Banner Ad Impression.");
                if (listener != null)
                    listener.onAdImpression();
            }
        });

        AdRequest.Builder builder = new AdRequest.Builder();
        if (config.isCollapsibleBannerEnabled()) {
            android.os.Bundle extras = new android.os.Bundle();
            String pos = (config.getCollapsibleBannerPosition() != null && !config.getCollapsibleBannerPosition().isEmpty())
                    ? config.getCollapsibleBannerPosition()
                    : "bottom";
            extras.putString("collapsible", pos);
            builder.addNetworkExtrasBundle(com.google.ads.mediation.admob.AdMobAdapter.class, extras);
        }
        admobBanner.loadAd(builder.build());
    }

    private void loadHouseBanner(Activity activity, FrameLayout adContainer, SmartAdsConfig config,
            BannerAdListener listener) {
        HouseAd houseAd = HouseAdLoader.selectAd(config.getHouseAds());
        if (houseAd == null) {
            SmartAdsLogger.e("No House Banner Ad available.");
            if (listener != null)
                listener.onAdFailed("AdMob failed and no House Ads available.");
            return;
        }

        SmartAdsLogger.d("Showing House Banner Ad.");

        View houseBannerView = android.view.LayoutInflater.from(activity)
                .inflate(R.layout.smartads_layout_house_banner, adContainer, false);

        ImageView imageView = houseBannerView.findViewById(R.id.smartads_house_banner_image);

        if (houseAd.getImageResId() != 0) {
            imageView.setImageResource(houseAd.getImageResId());
        } else {
            imageView.setBackgroundColor(android.graphics.Color.LTGRAY);
        }

        houseBannerView.setOnClickListener(v -> HouseAdLoader.handleClick(activity, houseAd));

        adContainer.removeAllViews();
        adContainer.addView(houseBannerView);

        if (listener != null) {
            listener.onAdLoaded(houseBannerView);
        }
    }

    private boolean isContainerActive(FrameLayout adContainer) {
        // We only check if the container is not null.
        // The activity validity is checked at the start of load method.
        // We do NOT check window token here because in some cases (RecyclerView, view
        // pager)
        // the view might be detached momentarily or not yet attached.
        return adContainer != null;
    }

    public void destroy(FrameLayout adContainer) {
        try {
            int childCount = adContainer.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = adContainer.getChildAt(i);
                if (child instanceof AdView) {
                    ((AdView) child).destroy();
                }
            }
        } catch (Exception ignored) {
        }
        try {
            adContainer.removeAllViews();
        } catch (Exception ignored) {
        }
    }
}
