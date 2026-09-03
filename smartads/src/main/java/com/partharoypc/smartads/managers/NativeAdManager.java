package com.partharoypc.smartads.managers;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.partharoypc.smartads.NativeAdSize;
import com.partharoypc.smartads.R;
import com.partharoypc.smartads.SmartAds;
import com.partharoypc.smartads.SmartAdsConfig;
import com.partharoypc.smartads.SmartAdsLogger;
import com.partharoypc.smartads.TestAdIds;
import com.partharoypc.smartads.house.HouseAd;
import com.partharoypc.smartads.house.HouseAdLoader;
import com.partharoypc.smartads.listeners.NativeAdListener;
import com.partharoypc.smartads.ui.NativeAdBinder;
import com.partharoypc.smartads.analytics.SmartAdsLifecycleListener;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Manages loading and displaying Native Ads (AdMob and House Ads fallback).
 */
public class NativeAdManager {
    private final Map<FrameLayout, NativeAd> activeAds = new WeakHashMap<>();
    private final Map<FrameLayout, Boolean> listenerAdded = new WeakHashMap<>();

    /**
     * Loads and shows a Native Ad into a container using a custom layout.
     */
    public void loadAndShowAd(Activity activity, FrameLayout adContainer, @LayoutRes int layoutRes,
            SmartAdsConfig config, NativeAdListener listener) {
        if (config.getNativeSource() == com.partharoypc.smartads.AdSource.HOUSE) {
            com.partharoypc.smartads.SmartAdsLogger.d("Native source set to HOUSE. Loading House Native directly.");
            loadHouseNative(activity, adContainer, layoutRes, config, listener);
            return;
        }
        loadAdMob(activity, adContainer, layoutRes, null, config, listener);
    }

    /**
     * Loads and shows a Native Ad into a container using a custom NativeAdBinder.
     */
    public void loadAndShowAd(Activity activity, FrameLayout adContainer, NativeAdBinder binder,
            SmartAdsConfig config, NativeAdListener listener) {
        if (binder == null) {
            if (listener != null) listener.onAdFailed("NativeAdBinder is null.");
            return;
        }
        if (config.getNativeSource() == com.partharoypc.smartads.AdSource.HOUSE) {
            com.partharoypc.smartads.SmartAdsLogger.d("Native source set to HOUSE. Loading House Native directly.");
            loadHouseNative(activity, adContainer, binder.getLayoutResId(), config, listener);
            return;
        }
        loadAdMob(activity, adContainer, binder.getLayoutResId(), binder, config, listener);
    }

    /**
     * Loads and shows a Native Ad into a container using a predefined
     * size/template.
     */
    public void loadAndShowAd(Activity activity, FrameLayout adContainer, NativeAdSize size,
            SmartAdsConfig config, NativeAdListener listener) {
        int layoutRes;
        switch (size) {
            case SMALL:
                layoutRes = R.layout.smartads_native_ad_small;
                break;
            case MEDIUM:
                layoutRes = R.layout.smartads_native_ad_medium;
                break;
            case LARGE:
                layoutRes = R.layout.smartads_native_ad_large;
                break;
            default:
                layoutRes = R.layout.smartads_native_ad_medium;
                break;
        }
        if (config.getNativeSource() == com.partharoypc.smartads.AdSource.HOUSE) {
            com.partharoypc.smartads.SmartAdsLogger.d("Native source set to HOUSE. Loading House Native directly.");
            loadHouseNative(activity, adContainer, layoutRes, config, listener);
            return;
        }
        loadAdMob(activity, adContainer, layoutRes, null, config, listener);
    }

    private void loadAdMob(Activity activity, FrameLayout adContainer, @LayoutRes int layoutRes,
            NativeAdBinder customBinder, SmartAdsConfig config,
            NativeAdListener listener) {
        if (!SmartAds.getInstance().areAdsEnabled() || !config.isNativeEnabled()) {
            SmartAdsLogger.d("Native Ad is disabled. Skipping request.");
            if (listener != null)
                listener.onAdFailed("Native ads are disabled.");
            return;
        }
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (listener != null)
                listener.onAdFailed("Activity is invalid.");
            return;
        }

        // 1. Check Internet
        if (!com.partharoypc.smartads.utils.NetworkUtils.isNetworkAvailable(activity)) {
            SmartAdsLogger.d("No Internet Connection. Skipping AdMob Native.");
            if (config.isHouseAdsAutoFallback() && config.isHouseAdsEnabled()) {
                loadHouseNative(activity, adContainer, layoutRes, config, listener);
            } else {
                if (listener != null)
                    listener.onAdFailed("No Internet Connection.");
            }
            return;
        }

        // 2. Check Ad Unit ID
        String adUnitId = (config.getAdMobNativeId() != null && !config.getAdMobNativeId().isEmpty())
                ? config.getAdMobNativeId()
                : (config.isTestMode() ? TestAdIds.ADMOB_NATIVE_ID : null);
        if (adUnitId == null || adUnitId.isEmpty()) {
            if (config.isHouseAdsAutoFallback() && config.isHouseAdsEnabled()) {
                SmartAdsLogger.d("AdMob Native ID not set. Trying House Ad.");
                loadHouseNative(activity, adContainer, layoutRes, config, listener);
            } else {
                if (listener != null)
                    listener.onAdFailed("Native Ad Unit ID is not configured.");
            }
            return;
        }

        // 3. AdMob NO_FILL Rate Limiting
        if (com.partharoypc.smartads.utils.AdMobRateLimiter.isRateLimited(adUnitId)) {
            SmartAdsLogger.d("AdMob Rate Limiter active (NO_FILL Cooldown). Skipping AdMob Native Request.");
            if (config.isHouseAdsAutoFallback() && config.isHouseAdsEnabled()) {
                loadHouseNative(activity, adContainer, layoutRes, config, listener);
            } else {
                if (listener != null)
                    listener.onAdFailed("Rate Limited (NO_FILL Cooldown)");
            }
            return;
        }

        SmartAdsLogger.d("Loading Native Ad...");
        SmartAdsLifecycleListener llInit = SmartAds.getInstance().getLifecycleListener();
        if (llInit != null) {
            llInit.onAdLoadStarted("Native", "ADMOB");
        }

        AdLoader.Builder builder = new AdLoader.Builder(activity, adUnitId);
        builder.forNativeAd(nativeAd -> {
            SmartAdsLogger.d("✅ Native Ad LOADED.");
            SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
            if (ll != null) {
                ll.onAdLoadSuccess("Native", "ADMOB");
            }
            nativeAd.setOnPaidEventListener(adValue -> {
                SmartAds.getInstance().reportPaidEvent(adValue, nativeAd.getResponseInfo(), adUnitId, "Native");
            });
            if (!isContainerActive(adContainer)) {
                try {
                    nativeAd.destroy();
                } catch (Exception ignored) {
                }
                return;
            }

            // Destroy previous ad in this container if exists
            NativeAd previousAd = activeAds.get(adContainer);
            if (previousAd != null) {
                previousAd.destroy();
            }
            activeAds.put(adContainer, nativeAd);

            NativeAdView adView = (NativeAdView) LayoutInflater.from(activity).inflate(layoutRes, adContainer, false);
            populateAdMobNativeAdView(nativeAd, adView, customBinder);
            adContainer.removeAllViews();
            adContainer.addView(adView);
            if (listener != null)
                listener.onAdLoaded(adView);
        });

        AdLoader adLoader = builder.withAdListener(new com.google.android.gms.ads.AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                com.partharoypc.smartads.SmartAdsLogger.e("❌ Native Ad Failed to Load: " + loadAdError.getMessage());
                SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                if (ll != null) {
                    ll.onAdLoadFailed("Native", "ADMOB", loadAdError.getMessage());
                }
                if (loadAdError.getCode() == com.google.android.gms.ads.AdRequest.ERROR_CODE_NO_FILL) {
                    com.partharoypc.smartads.utils.AdMobRateLimiter.recordNoFill(adUnitId);
                }
                // FALLBACK TO HOUSE NATIVE
                if (config.isHouseAdsAutoFallback() && config.isHouseAdsEnabled()) {
                    loadHouseNative(activity, adContainer, layoutRes, config, listener);
                } else {
                    if (listener != null) {
                        listener.onAdFailed(loadAdError.getMessage());
                    }
                }
            }

            @Override
            public void onAdClicked() {
                com.partharoypc.smartads.SmartAdsLogger.d("Native Ad Clicked.");
                SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                if (ll != null) {
                    ll.onAdClicked("Native", "ADMOB");
                }
                if (listener != null)
                    listener.onAdClicked();
            }

            @Override
            public void onAdImpression() {
                com.partharoypc.smartads.SmartAdsLogger.d("Native Ad Impression.");
                SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                if (ll != null) {
                    ll.onAdShowSuccess("Native", "ADMOB");
                }
                if (listener != null)
                    listener.onAdImpression();
            }
        }).build();

        adLoader.loadAd(new AdRequest.Builder().build());

        // Ensure we clean up when container is detached (e.g. in RecyclerView)
        if (listenerAdded.get(adContainer) == null) {
            adContainer.addOnAttachStateChangeListener(new android.view.View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    destroy(adContainer);
                }
            });
            listenerAdded.put(adContainer, Boolean.TRUE);
        }
    }

    private void loadHouseNative(Activity activity, FrameLayout adContainer, @LayoutRes int layoutRes,
            SmartAdsConfig config,
            NativeAdListener listener) {
        SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
        if (ll != null) {
            ll.onAdLoadStarted("Native", "HOUSE");
        }
        HouseAd houseAd = HouseAdLoader.selectAd(config.getHouseAds());
        if (houseAd == null) {
            com.partharoypc.smartads.SmartAdsLogger.e("No House Native Ad available.");
            if (ll != null) {
                ll.onAdLoadFailed("Native", "HOUSE", "No House Ads available.");
            }
            if (listener != null)
                listener.onAdFailed("No House Ads available.");
            return;
        }

        com.partharoypc.smartads.SmartAdsLogger.d("Showing House Native Ad.");
        if (ll != null) {
            ll.onAdLoadSuccess("Native", "HOUSE");
            ll.onAdShowSuccess("Native", "HOUSE");
        }

        // Inflate the same layout. Even if root is NativeAdView, we treat it as View.
        View adView = LayoutInflater.from(activity).inflate(layoutRes, adContainer, false);

        // Populate standard views
        HouseAdLoader.populateView(adView, houseAd);

        adContainer.removeAllViews();
        adContainer.addView(adView);

        if (listener != null) {
            listener.onAdLoaded(adView);
        }
    }

    private void populateAdMobNativeAdView(NativeAd nativeAd, NativeAdView adView, NativeAdBinder binder) {
        if (binder != null) {
            if (binder.getMediaViewId() != 0) adView.setMediaView(adView.findViewById(binder.getMediaViewId()));
            if (binder.getHeadlineViewId() != 0) adView.setHeadlineView(adView.findViewById(binder.getHeadlineViewId()));
            if (binder.getBodyViewId() != 0) adView.setBodyView(adView.findViewById(binder.getBodyViewId()));
            if (binder.getCallToActionViewId() != 0) adView.setCallToActionView(adView.findViewById(binder.getCallToActionViewId()));
            if (binder.getIconViewId() != 0) adView.setIconView(adView.findViewById(binder.getIconViewId()));
            if (binder.getStarRatingViewId() != 0) adView.setStarRatingView(adView.findViewById(binder.getStarRatingViewId()));
            if (binder.getAdvertiserViewId() != 0) adView.setAdvertiserView(adView.findViewById(binder.getAdvertiserViewId()));
            if (binder.getPriceViewId() != 0) adView.setPriceView(adView.findViewById(binder.getPriceViewId()));
            if (binder.getStoreViewId() != 0) adView.setStoreView(adView.findViewById(binder.getStoreViewId()));
        } else {
            adView.setMediaView(adView.findViewById(R.id.smartads_ad_media));
            adView.setHeadlineView(adView.findViewById(R.id.smartads_ad_headline));
            adView.setBodyView(adView.findViewById(R.id.smartads_ad_body));
            adView.setCallToActionView(adView.findViewById(R.id.smartads_ad_call_to_action));
            adView.setIconView(adView.findViewById(R.id.smartads_ad_app_icon));
            adView.setStarRatingView(adView.findViewById(R.id.smartads_ad_stars));
            adView.setAdvertiserView(adView.findViewById(R.id.smartads_ad_advertiser));
        }

        // Headline
        if (adView.getHeadlineView() != null) {
            ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        }

        // Media
        if (adView.getMediaView() != null) {
            adView.getMediaView().setMediaContent(nativeAd.getMediaContent());
        }

        // Body
        if (adView.getBodyView() != null) {
            if (nativeAd.getBody() == null) {
                adView.getBodyView().setVisibility(View.INVISIBLE);
            } else {
                adView.getBodyView().setVisibility(View.VISIBLE);
                ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
            }
        }

        // Call to Action
        if (adView.getCallToActionView() != null) {
            if (nativeAd.getCallToAction() == null) {
                adView.getCallToActionView().setVisibility(View.INVISIBLE);
            } else {
                adView.getCallToActionView().setVisibility(View.VISIBLE);
                ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
            }
        }

        // Icon
        if (adView.getIconView() != null) {
            if (nativeAd.getIcon() == null) {
                adView.getIconView().setVisibility(View.GONE);
            } else {
                ((ImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
                adView.getIconView().setVisibility(View.VISIBLE);
            }
        }

        // Star Rating
        if (adView.getStarRatingView() != null) {
            if (nativeAd.getStarRating() == null) {
                adView.getStarRatingView().setVisibility(View.INVISIBLE);
            } else {
                ((RatingBar) adView.getStarRatingView()).setRating(nativeAd.getStarRating().floatValue());
                adView.getStarRatingView().setVisibility(View.VISIBLE);
            }
        }

        // Advertiser
        if (adView.getAdvertiserView() != null) {
            if (nativeAd.getAdvertiser() == null) {
                adView.getAdvertiserView().setVisibility(View.INVISIBLE);
            } else {
                ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
                adView.getAdvertiserView().setVisibility(View.VISIBLE);
            }
        }

        // Price
        if (adView.getPriceView() != null) {
            if (nativeAd.getPrice() == null) {
                adView.getPriceView().setVisibility(View.INVISIBLE);
            } else {
                adView.getPriceView().setVisibility(View.VISIBLE);
                ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
            }
        }

        // Store
        if (adView.getStoreView() != null) {
            if (nativeAd.getStore() == null) {
                adView.getStoreView().setVisibility(View.INVISIBLE);
            } else {
                adView.getStoreView().setVisibility(View.VISIBLE);
                ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
            }
        }

        adView.setNativeAd(nativeAd);
    }

    public void destroy(FrameLayout adContainer) {
        try {
            NativeAd ad = activeAds.remove(adContainer);
            if (ad != null) {
                ad.destroy();
                SmartAdsLifecycleListener ll = SmartAds.getInstance().getLifecycleListener();
                if (ll != null) {
                    ll.onAdClosed("Native", "ADMOB");
                }
            }
        } catch (Exception ignored) {
        }
        try {
            adContainer.removeAllViews();
        } catch (Exception ignored) {
        }
    }

    private boolean isContainerActive(FrameLayout adContainer) {
        // Relaxed check: allow loading even if not yet fully attached.
        return adContainer != null;
    }
}
