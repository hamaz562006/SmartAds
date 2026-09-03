package com.partharoypc.smartads.house;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.partharoypc.smartads.R;

import java.util.List;
import java.util.Random;

/**
 * Helper class to load and display House Ads.
 * Handles the selection logic and view population for internal cross-promotion
 * ads.
 */
public class HouseAdLoader {

    private static long lastClickTime = 0;
    private static final long CLICK_DEBOUNCE_MS = 1000L;

    private HouseAdLoader() {
        // Utility class
    }

    /**
     * Selects a House Ad from the list based on a weighted random algorithm
     * favoring higher ratings.
     * 
     * @param ads List of available house ads.
     * @return The selected {@link HouseAd}, or null if the list is empty.
     */
    public static HouseAd selectAd(List<HouseAd> ads) {
        if (ads == null || ads.isEmpty()) {
            return null;
        }

        // Smart Selection: Prioritize higher rated ads (Super Revenue Friendly)
        // We use a weighted random selection based on rating (0.0 - 5.0)
        double totalWeight = 0.0;
        for (HouseAd ad : ads) {
            totalWeight += Math.max(0.1f, ad.getRating()); // Ensure non-zero weight
        }

        double randomValue = new Random().nextDouble() * totalWeight;
        double currentWeight = 0.0;

        for (HouseAd ad : ads) {
            currentWeight += Math.max(0.1f, ad.getRating());
            if (randomValue <= currentWeight) {
                return ad;
            }
        }

        // Fallback
        return ads.get(0);
    }

    /**
     * Handles the click action for a House Ad, opening the destination URL.
     * 
     * @param context The current context.
     * @param ad      The house ad that was clicked.
     */
    public static void handleClick(Context context, HouseAd ad) {
        if (ad == null || ad.getClickUrl() == null || ad.getClickUrl().isEmpty()) {
            com.partharoypc.smartads.SmartAdsLogger.e("House Ad click failed: Ad or URL is null.");
            return;
        }

        // AdMob Policy: Prevent deceptive double-clicks and accidental fire.
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < CLICK_DEBOUNCE_MS) {
            com.partharoypc.smartads.SmartAdsLogger.d("Ignoring duplicate House Ad click (Debounce).");
            return;
        }
        lastClickTime = currentTime;

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(ad.getClickUrl()));
            if (!(context instanceof android.app.Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        } catch (Exception e) {
            com.partharoypc.smartads.SmartAdsLogger.e("Could not open House Ad link: " + e.getMessage());
            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Populates a generic view layout with House Ad data.
     * This mimics the Native AdView behavior for consistent styling.
     *
     * @param rootView The root view container for the ad.
     * @param ad       The house ad data to display.
     */
    public static void populateView(View rootView, HouseAd ad) {
        if (rootView == null || ad == null)
            return;

        TextView headline = rootView.findViewById(R.id.smartads_ad_headline);
        TextView body = rootView.findViewById(R.id.smartads_ad_body);
        TextView cta = rootView.findViewById(R.id.smartads_ad_call_to_action);
        ImageView icon = rootView.findViewById(R.id.smartads_ad_app_icon);
        View mediaView = rootView.findViewById(R.id.smartads_ad_media);
        View advertiser = rootView.findViewById(R.id.smartads_ad_advertiser);
        // Add star rating support if present in layout
        android.widget.RatingBar ratingBar = rootView.findViewById(R.id.smartads_ad_stars);

        if (headline != null) {
            headline.setText(ad.getTitle());
        }

        if (body != null) {
            body.setText(ad.getDescription());
        }

        if (cta != null) {
            cta.setText(ad.getCtaText());
            // Make CTA clickable independently
            cta.setOnClickListener(v -> handleClick(rootView.getContext(), ad));
        }

        if (icon != null) {
            if (ad.getIconUrl() != null && !ad.getIconUrl().trim().isEmpty()) {
                com.bumptech.glide.Glide.with(rootView.getContext())
                        .load(ad.getIconUrl())
                        .into(icon);
                icon.setVisibility(View.VISIBLE);
            } else if (ad.getIconResId() != 0) {
                icon.setImageResource(ad.getIconResId());
                icon.setVisibility(View.VISIBLE);
            } else {
                icon.setVisibility(View.GONE);
            }
        }

        if (ratingBar != null) {
            ratingBar.setRating(ad.getRating());
            ratingBar.setVisibility(View.VISIBLE);
        }

        if (mediaView != null) {
            if (ad.getImageUrl() != null && !ad.getImageUrl().trim().isEmpty()) {
                if (mediaView instanceof ImageView) {
                    com.bumptech.glide.Glide.with(rootView.getContext())
                            .load(ad.getImageUrl())
                            .centerCrop()
                            .into((ImageView) mediaView);
                } else if (mediaView instanceof android.view.ViewGroup) {
                    android.view.ViewGroup mediaGroup = (android.view.ViewGroup) mediaView;
                    mediaGroup.removeAllViews();

                    ImageView imageView = new ImageView(rootView.getContext());
                    imageView.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT));
                    com.bumptech.glide.Glide.with(rootView.getContext())
                            .load(ad.getImageUrl())
                            .centerCrop()
                            .into(imageView);
                    mediaGroup.addView(imageView);
                }
                mediaView.setVisibility(View.VISIBLE);
            } else if (ad.getImageResId() != 0) {
                if (mediaView instanceof ImageView) {
                    ((ImageView) mediaView).setImageResource(ad.getImageResId());
                    ((ImageView) mediaView).setScaleType(ImageView.ScaleType.CENTER_CROP);
                } else if (mediaView instanceof android.view.ViewGroup) {
                    android.view.ViewGroup mediaGroup = (android.view.ViewGroup) mediaView;
                    mediaGroup.removeAllViews();

                    ImageView imageView = new ImageView(rootView.getContext());
                    imageView.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT));
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageView.setImageResource(ad.getImageResId());
                    mediaGroup.addView(imageView);
                }
                mediaView.setVisibility(View.VISIBLE);
            } else {
                mediaView.setVisibility(View.GONE);
            }
        }

        // Hide advertiser text for house ads as it's usually redundant or not set
        if (advertiser != null)
            advertiser.setVisibility(View.GONE);

        // Click listener on entire view
        rootView.setOnClickListener(v -> handleClick(rootView.getContext(), ad));
    }
}
