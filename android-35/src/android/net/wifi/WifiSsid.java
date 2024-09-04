/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.net.wifi;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.compat.annotation.UnsupportedAppUsage;
import android.net.wifi.util.HexEncoding;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Representation of a Wi-Fi Service Set Identifier (SSID).
 */
public final class WifiSsid implements Parcelable {
    private final byte[] mBytes;

    /**
     * Creates a WifiSsid from the raw bytes. If the byte array is null, creates an empty WifiSsid
     * object which will return an empty byte array and empty text.
     * @param bytes the SSID
     */
    private WifiSsid(@Nullable byte[] bytes) {
        if (bytes == null) {
            bytes = new byte[0];
        }
        mBytes = bytes;
        // Duplicate the bytes to #octets for legacy apps.
        octets.write(bytes, 0, bytes.length);
    }

    /**
     * Create a WifiSsid from the raw bytes. If the byte array is null, return an empty WifiSsid
     * object which will return an empty byte array and empty text.
     */
    @NonNull
    public static WifiSsid fromBytes(@Nullable byte[] bytes) {
        return new WifiSsid(bytes);
    }

    /**
     * Returns the raw byte array representing this SSID.
     * @return the SSID
     */
    @NonNull
    public byte[] getBytes() {
        return mBytes.clone();
    }

    /**
     * Create a UTF-8 WifiSsid from unquoted plaintext. If the text is null, return an
     * empty WifiSsid object which will return an empty byte array and empty text.
     * @hide
     */
    @NonNull
    public static WifiSsid fromUtf8Text(@Nullable CharSequence utf8Text) {
        if (utf8Text == null) {
            return new WifiSsid(null);
        }
        return new WifiSsid(utf8Text.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * If the SSID is encoded with UTF-8, this method returns the decoded SSID as plaintext.
     * Otherwise, it returns {@code null}.
     * @return the SSID
     * @hide
     */
    @Nullable
    public CharSequence getUtf8Text() {
        return decodeSsid(mBytes, StandardCharsets.UTF_8);
    }

    /**
     * Create a WifiSsid from a string matching the format of {@link WifiSsid#toString()}.
     * If the string is null, return an empty WifiSsid object which will return an empty byte array
     * and empty text.
     * @throws IllegalArgumentException if the string is unquoted but not hexadecimal,
     *                                  or if the hexadecimal string is odd-length.
     * @hide
     */
    @NonNull
    public static WifiSsid fromString(@Nullable String string) {
        if (string == null) {
            return new WifiSsid(null);
        }
        final int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"') && (string.charAt(length - 1) == '"')) {
            return new WifiSsid(string.substring(1, length - 1).getBytes(StandardCharsets.UTF_8));
        }
        return new WifiSsid(HexEncoding.decode(string));
    }

    /**
     * Returns the string representation of the WifiSsid. If the SSID can be decoded as UTF-8, it
     * will be returned in plain text surrounded by double quotation marks. Otherwise, it is
     * returned as an unquoted string of hex digits. This format is consistent with
     * {@link WifiInfo#getSSID()} and {@link WifiConfiguration#SSID}.
     *
     * @return SSID as double-quoted plain text from UTF-8 or unquoted hex digits
     */
    @Override
    @NonNull
    public String toString() {
        String utf8String = decodeSsid(mBytes, StandardCharsets.UTF_8);
        if (TextUtils.isEmpty(utf8String)) {
            return HexEncoding.encodeToString(mBytes, false /* upperCase */);
        }
        return "\"" + utf8String + "\"";
    }

    /**
     * Returns the given SSID bytes as a String decoded using the given Charset. If the bytes cannot
     * be decoded, then this returns {@code null}.
     * @param ssidBytes SSID as bytes
     * @param charset Charset to decode with
     * @return SSID as string, or {@code null}.
     */
    @Nullable
    private static String decodeSsid(@NonNull byte[] ssidBytes, @NonNull Charset charset) {
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        CharBuffer out = CharBuffer.allocate(32);
        CoderResult result = decoder.decode(ByteBuffer.wrap(ssidBytes), out, true);
        out.flip();
        if (result.isError()) {
            return null;
        }
        return out.toString();
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (!(thatObject instanceof WifiSsid)) {
            return false;
        }
        WifiSsid that = (WifiSsid) thatObject;
        return Arrays.equals(mBytes, that.mBytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(mBytes);
    }

    /** Implement the Parcelable interface */
    @Override
    public int describeContents() {
        return 0;
    }

    /** Implement the Parcelable interface */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeByteArray(mBytes);
    }

    /** Implement the Parcelable interface */
    public static final @NonNull Creator<WifiSsid> CREATOR =
            new Creator<WifiSsid>() {
                @Override
                public WifiSsid createFromParcel(Parcel in) {
                    return new WifiSsid(in.createByteArray());
                }

                @Override
                public WifiSsid[] newArray(int size) {
                    return new WifiSsid[size];
                }
            };

    /**
     * Use {@link #getBytes()} instead.
     * @hide
     */
    // TODO(b/231433398): add maxTargetSdk = Build.VERSION_CODES.S
    @UnsupportedAppUsage(publicAlternatives = "{@link #getBytes()}")
    public final ByteArrayOutputStream octets = new ByteArrayOutputStream(32);

    /**
     * Use {@link android.net.wifi.WifiManager#UNKNOWN_SSID} instead.
     * @hide
     */
    // TODO(b/231433398): add maxTargetSdk = Build.VERSION_CODES.S
    @UnsupportedAppUsage(publicAlternatives = "{@link android.net.wifi.WifiManager#UNKNOWN_SSID}")
    public static final String NONE = WifiManager.UNKNOWN_SSID;

    /**
     * Use {@link #fromBytes(byte[])} instead.
     * @hide
     */
    // TODO(b/231433398): add maxTargetSdk = Build.VERSION_CODES.S
    @UnsupportedAppUsage(publicAlternatives = "{@link #fromBytes(byte[])}")
    public static WifiSsid createFromAsciiEncoded(String asciiEncoded) {
        return fromUtf8Text(asciiEncoded);
    }

    /**
     * Use {@link #getBytes()} instead.
     * @hide
     */
    // TODO(b/231433398): add maxTargetSdk = Build.VERSION_CODES.S
    @UnsupportedAppUsage(publicAlternatives = "{@link #getBytes()}")
    public byte[] getOctets() {
        return getBytes();
    }
}
