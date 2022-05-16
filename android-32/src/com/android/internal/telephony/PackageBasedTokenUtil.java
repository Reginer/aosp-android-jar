/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

/** Utility class for generating token, i.e., hash of package name and certificate. */
public class PackageBasedTokenUtil {
    private static final String TAG = "PackageBasedTokenUtil";
    private static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8");
    private static final String HASH_TYPE = "SHA-256";
    private static final int NUM_HASHED_BYTES = 9; // 9 bytes = 72 bits = 12 Base64s

    static final int NUM_BASE64_CHARS = 11; // truncate 12 into 11 Base64 chars

    /**
     * Generate token and check collision with other packages.
     */
    public static String generateToken(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        String token = generatePackageBasedToken(packageManager, packageName);

        // Check for token confliction
        List<PackageInfo> packages =
                packageManager.getInstalledPackages(PackageManager.GET_META_DATA);

        for (PackageInfo packageInfo : packages) {
            String otherPackageName = packageInfo.packageName;
            if (packageName.equals(otherPackageName)) {
                continue;
            }

            String otherToken = generatePackageBasedToken(packageManager, otherPackageName);
            if (token.equals(otherToken)) {
                Log.e(TAG, "token collides with other installed app.");
                token = null;
            }
        }

        return token;
    }

    private static String generatePackageBasedToken(
            PackageManager packageManager, String packageName) {
        String token = null;
        Signature[] signatures;

        try {
            // It is actually a certificate (public key), not a signature.
            signatures = packageManager.getPackageInfo(
                    packageName, PackageManager.GET_SIGNATURES).signatures;
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Failed to find package with package name: " + packageName);
            return token;
        }

        if (signatures == null) {
            Log.e(TAG, "The certificates is missing.");
        } else {
            MessageDigest messageDigest;
            try {
                messageDigest = MessageDigest.getInstance(HASH_TYPE);
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "NoSuchAlgorithmException" + e);
                return null;
            }

            messageDigest.update(packageName.getBytes(CHARSET_UTF_8));
            String space = " ";
            messageDigest.update(space.getBytes(CHARSET_UTF_8));
            for (int i = 0; i < signatures.length; i++) {
                messageDigest.update(signatures[i].toCharsString().getBytes(CHARSET_UTF_8));
            }
            byte[] hashSignatures = messageDigest.digest();
            // truncated into NUM_HASHED_BYTES
            hashSignatures = Arrays.copyOf(hashSignatures, NUM_HASHED_BYTES);
            // encode into Base64
            token = Base64.encodeToString(hashSignatures, Base64.NO_PADDING | Base64.NO_WRAP);
            token = token.substring(0, NUM_BASE64_CHARS);
        }
        return token;
    }
}
