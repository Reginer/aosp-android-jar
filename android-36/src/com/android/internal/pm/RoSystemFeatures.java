// This file is auto-generated. DO NOT MODIFY.
// Args: com.android.internal.pm.RoSystemFeatures \
//            --feature=AUTOMOTIVE:UNAVAILABLE \
//            --feature=EMBEDDED:UNAVAILABLE \
//            --feature=LEANBACK:UNAVAILABLE \
//            --feature=PC:UNAVAILABLE \
//            --feature=TELEVISION:UNAVAILABLE \
//            --feature=WATCH:0 \
//            --readonly=true \
//            --metadata-only=false
package com.android.internal.pm;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.util.ArrayMap;
import com.android.aconfig.annotations.AssumeFalseForR8;
import com.android.aconfig.annotations.AssumeTrueForR8;

/**
 * @hide
 */
public final class RoSystemFeatures {
    /**
     * Check for FEATURE_AUTOMOTIVE.
     *
     * @hide
     */
    @AssumeFalseForR8
    public static boolean hasFeatureAutomotive(Context context) {
        return false;
    }

    /**
     * Check for FEATURE_EMBEDDED.
     *
     * @hide
     */
    @AssumeFalseForR8
    public static boolean hasFeatureEmbedded(Context context) {
        return false;
    }

    /**
     * Check for FEATURE_LEANBACK.
     *
     * @hide
     */
    @AssumeFalseForR8
    public static boolean hasFeatureLeanback(Context context) {
        return false;
    }

    /**
     * Check for FEATURE_PC.
     *
     * @hide
     */
    @AssumeFalseForR8
    public static boolean hasFeaturePc(Context context) {
        return false;
    }

    /**
     * Check for FEATURE_TELEVISION.
     *
     * @hide
     */
    @AssumeFalseForR8
    public static boolean hasFeatureTelevision(Context context) {
        return false;
    }

    /**
     * Check for FEATURE_WATCH.
     *
     * @hide
     */
    @AssumeTrueForR8
    public static boolean hasFeatureWatch(Context context) {
        return true;
    }

    private static boolean hasFeatureFallback(Context context, String featureName) {
        return context.getPackageManager().hasSystemFeature(featureName);
    }

    /**
     * @hide
     */
    @Nullable
    public static Boolean maybeHasFeature(String featureName, int version) {
        switch (featureName) {
            case PackageManager.FEATURE_AUTOMOTIVE: return false;
            case PackageManager.FEATURE_EMBEDDED: return false;
            case PackageManager.FEATURE_LEANBACK: return false;
            case PackageManager.FEATURE_PC: return false;
            case PackageManager.FEATURE_TELEVISION: return false;
            case PackageManager.FEATURE_WATCH: return 0 >= version;
            default: break;
        }
        return null;
    }

    /**
     * Gets features marked as available at compile-time, keyed by name.
     *
     * @hide
     */
    @NonNull
    public static ArrayMap<String, FeatureInfo> getReadOnlySystemEnabledFeatures() {
        ArrayMap<String, FeatureInfo> features = new ArrayMap<>(1);
        FeatureInfo fi = new FeatureInfo();
        fi.name = PackageManager.FEATURE_WATCH;
        fi.version = 0;
        features.put(fi.name, new FeatureInfo(fi));
        return features;
    }
}
