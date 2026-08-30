package com.partharoypc.smartads.listeners;

/**
 * Listener for Rewarded Interstitial Ad lifecycle events.
 */
public interface RewardedInterstitialAdListener {
    /** Called when the user completes the ad and earns a reward. */
    void onUserEarnedReward();

    /** Called when the rewarded interstitial ad is closed/dismissed by the user. */
    void onAdDismissed();

    /** Called when the rewarded interstitial ad fails to show. */
    void onAdFailedToShow(String errorMessage);

    /** Called when the rewarded interstitial ad is successfully loaded. */
    default void onAdLoaded() {
    }

    /** Called when the user clicks on the rewarded interstitial ad. */
    default void onAdClicked() {
    }

    /** Called when the rewarded interstitial ad is displayed and an impression is recorded. */
    default void onAdImpression() {
    }
}
