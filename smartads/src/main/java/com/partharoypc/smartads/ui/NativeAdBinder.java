package com.partharoypc.smartads.ui;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;

/**
 * Configuration binder for rendering custom Native Ads layouts.
 * Enables developers to provide arbitrary XML layouts and map view IDs
 * (headline, body, call to action, media, icon, advertiser, star rating, price, store)
 * instead of being restricted to fixed templates.
 */
public class NativeAdBinder {

    @LayoutRes
    private final int layoutResId;

    @IdRes
    private final int headlineViewId;

    @IdRes
    private final int bodyViewId;

    @IdRes
    private final int callToActionViewId;

    @IdRes
    private final int iconViewId;

    @IdRes
    private final int mediaViewId;

    @IdRes
    private final int advertiserViewId;

    @IdRes
    private final int starRatingViewId;

    @IdRes
    private final int priceViewId;

    @IdRes
    private final int storeViewId;

    private NativeAdBinder(Builder builder) {
        this.layoutResId = builder.layoutResId;
        this.headlineViewId = builder.headlineViewId;
        this.bodyViewId = builder.bodyViewId;
        this.callToActionViewId = builder.callToActionViewId;
        this.iconViewId = builder.iconViewId;
        this.mediaViewId = builder.mediaViewId;
        this.advertiserViewId = builder.advertiserViewId;
        this.starRatingViewId = builder.starRatingViewId;
        this.priceViewId = builder.priceViewId;
        this.storeViewId = builder.storeViewId;
    }

    @LayoutRes
    public int getLayoutResId() {
        return layoutResId;
    }

    @LayoutRes
    public int getLayoutRes() {
        return layoutResId;
    }

    @IdRes
    public int getHeadlineViewId() {
        return headlineViewId;
    }

    @IdRes
    public int getBodyViewId() {
        return bodyViewId;
    }

    @IdRes
    public int getCallToActionViewId() {
        return callToActionViewId;
    }

    @IdRes
    public int getIconViewId() {
        return iconViewId;
    }

    @IdRes
    public int getMediaViewId() {
        return mediaViewId;
    }

    @IdRes
    public int getAdvertiserViewId() {
        return advertiserViewId;
    }

    @IdRes
    public int getStarRatingViewId() {
        return starRatingViewId;
    }

    @IdRes
    public int getPriceViewId() {
        return priceViewId;
    }

    @IdRes
    public int getStoreViewId() {
        return storeViewId;
    }

    public static class Builder {
        @LayoutRes
        private final int layoutResId;

        @IdRes
        private int headlineViewId = 0;

        @IdRes
        private int bodyViewId = 0;

        @IdRes
        private int callToActionViewId = 0;

        @IdRes
        private int iconViewId = 0;

        @IdRes
        private int mediaViewId = 0;

        @IdRes
        private int advertiserViewId = 0;

        @IdRes
        private int starRatingViewId = 0;

        @IdRes
        private int priceViewId = 0;

        @IdRes
        private int storeViewId = 0;

        public Builder(@LayoutRes int layoutResId) {
            this.layoutResId = layoutResId;
        }

        public Builder setHeadlineViewId(@IdRes int headlineViewId) {
            this.headlineViewId = headlineViewId;
            return this;
        }

        public Builder setBodyViewId(@IdRes int bodyViewId) {
            this.bodyViewId = bodyViewId;
            return this;
        }

        public Builder setCallToActionViewId(@IdRes int callToActionViewId) {
            this.callToActionViewId = callToActionViewId;
            return this;
        }

        public Builder setIconViewId(@IdRes int iconViewId) {
            this.iconViewId = iconViewId;
            return this;
        }

        public Builder setMediaViewId(@IdRes int mediaViewId) {
            this.mediaViewId = mediaViewId;
            return this;
        }

        public Builder setAdvertiserViewId(@IdRes int advertiserViewId) {
            this.advertiserViewId = advertiserViewId;
            return this;
        }

        public Builder setStarRatingViewId(@IdRes int starRatingViewId) {
            this.starRatingViewId = starRatingViewId;
            return this;
        }

        public Builder setPriceViewId(@IdRes int priceViewId) {
            this.priceViewId = priceViewId;
            return this;
        }

        public Builder setStoreViewId(@IdRes int storeViewId) {
            this.storeViewId = storeViewId;
            return this;
        }

        public NativeAdBinder build() {
            return new NativeAdBinder(this);
        }
    }
}
