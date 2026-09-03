package com.partharoypc.smartads.house;

import com.partharoypc.smartads.SmartAdsLogger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parser for dynamic House Ads configured as JSON arrays in Remote Config or backend services.
 */
public final class HouseAdsRemoteParser {

    private HouseAdsRemoteParser() {
    }

    /**
     * Parses a JSON array string into a list of {@link HouseAd} objects.
     *
     * @param jsonArrayString JSON array string.
     * @return List of valid, enabled {@link HouseAd} objects.
     */
    public static List<HouseAd> parseFromJson(String jsonArrayString) {
        if (jsonArrayString == null || jsonArrayString.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<HouseAd> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(jsonArrayString);
            for (int i = 0; i < array.length(); i++) {
                try {
                    JSONObject obj = array.getJSONObject(i);
                    // Include if "enabled" key is missing or true. Skip if explicitly false.
                    boolean enabled = obj.optBoolean("enabled", true);
                    if (!enabled) {
                        continue;
                    }

                    HouseAd.Builder builder = new HouseAd.Builder();
                    if (obj.has("id")) {
                        builder.setId(obj.optString("id"));
                    }
                    if (obj.has("title")) {
                        builder.setTitle(obj.optString("title"));
                    }
                    if (obj.has("description")) {
                        builder.setDescription(obj.optString("description"));
                    }
                    if (obj.has("ctaText")) {
                        builder.setCtaText(obj.optString("ctaText"));
                    }
                    if (obj.has("iconUrl")) {
                        builder.setIconUrl(obj.optString("iconUrl"));
                    }
                    if (obj.has("imageUrl")) {
                        builder.setImageUrl(obj.optString("imageUrl"));
                    }
                    if (obj.has("clickUrl")) {
                        builder.setClickUrl(obj.optString("clickUrl"));
                    }
                    if (obj.has("rating")) {
                        builder.setRating((float) obj.optDouble("rating", 5.0));
                    }

                    // Remote house ads have no local drawable res IDs
                    builder.setIconResId(0);
                    builder.setImageResId(0);

                    result.add(builder.build());
                } catch (Exception e) {
                    SmartAdsLogger.e("Skipping invalid house ad item at index " + i + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            SmartAdsLogger.e("Failed to parse house ads JSON array: " + e.getMessage());
            return Collections.emptyList();
        }

        return result;
    }
}
