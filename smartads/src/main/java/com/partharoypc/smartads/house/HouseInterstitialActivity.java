package com.partharoypc.smartads.house;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.partharoypc.smartads.R;
import com.partharoypc.smartads.SmartAds;
import com.partharoypc.smartads.SmartAdsConfig;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class HouseInterstitialActivity extends AppCompatActivity {

    public static final String EXTRA_AD_INDEX = "extra_ad_index";
    public static HouseInterstitialListener listener;

    public interface HouseInterstitialListener {
        void onAdDismissed();

        void onAdClicked();

        void onAdImpression();
    }

    public static void start(Context context, int adIndex, HouseInterstitialListener adListener) {
        listener = adListener;
        Intent intent = new Intent(context, HouseInterstitialActivity.class);
        intent.putExtra(EXTRA_AD_INDEX, adIndex);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        setContentView(R.layout.smartads_activity_house_interstitial);

        int adIndex = getIntent().getIntExtra(EXTRA_AD_INDEX, -1);
        SmartAdsConfig config = SmartAds.getInstance().getConfig();
        List<HouseAd> ads = config.getHouseAds();

        if (adIndex < 0 || adIndex >= ads.size()) {
            finish();
            return;
        }

        HouseAd ad = ads.get(adIndex);

        // Views
        ImageView mainImage = findViewById(R.id.smartads_house_ad_image);
        ImageView iconImage = findViewById(R.id.smartads_house_ad_icon);
        android.widget.TextView titleText = findViewById(R.id.smartads_house_ad_title);
        android.widget.TextView descText = findViewById(R.id.smartads_house_ad_desc);
        android.widget.RatingBar ratingBar = findViewById(R.id.smartads_house_ad_rating);
        android.widget.Button ctaButton = findViewById(R.id.smartads_house_ad_cta);
        ImageButton closeBtn = findViewById(R.id.smartads_house_ad_close_btn);

        // Populate Data
        if (ad.getImageUrl() != null && !ad.getImageUrl().trim().isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(ad.getImageUrl())
                    .centerCrop()
                    .into(mainImage);
            mainImage.setVisibility(android.view.View.VISIBLE);
        } else if (ad.getImageResId() != 0) {
            mainImage.setImageResource(ad.getImageResId());
            mainImage.setVisibility(android.view.View.VISIBLE);
        } else {
            mainImage.setVisibility(android.view.View.GONE);
        }

        if (ad.getIconUrl() != null && !ad.getIconUrl().trim().isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(ad.getIconUrl())
                    .into(iconImage);
            iconImage.setVisibility(android.view.View.VISIBLE);
        } else if (ad.getIconResId() != 0) {
            iconImage.setImageResource(ad.getIconResId());
            iconImage.setVisibility(android.view.View.VISIBLE);
        } else {
            iconImage.setVisibility(android.view.View.GONE);
        }

        titleText.setText(ad.getTitle());
        descText.setText(ad.getDescription());
        ratingBar.setRating(ad.getRating());

        if (ad.getCtaText() != null && !ad.getCtaText().isEmpty()) {
            ctaButton.setText(ad.getCtaText());
        }

        // Clicks
        android.view.View.OnClickListener clickListener = v -> {
            HouseAdLoader.handleClick(this, ad);
            if (listener != null)
                listener.onAdClicked();
            finish();
        };

        mainImage.setOnClickListener(clickListener);
        ctaButton.setOnClickListener(clickListener);
        iconImage.setOnClickListener(clickListener);
        titleText.setOnClickListener(clickListener);

        closeBtn.setOnClickListener(v -> {
            if (listener != null)
                listener.onAdDismissed();
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        if (listener != null)
            listener.onAdImpression();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        super.onBackPressed();
        if (listener != null)
            listener.onAdDismissed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            listener = null;
        }
    }
}
