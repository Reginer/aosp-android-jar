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

package android.net.nsd;

import static com.android.net.module.util.HexDump.toHexString;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.compat.annotation.UnsupportedAppUsage;
import android.net.Network;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import com.android.net.module.util.InetAddressUtils;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * A class representing service information for network service discovery
 * @see NsdManager
 */
public final class NsdServiceInfo implements Parcelable {

    private static final String TAG = "NsdServiceInfo";

    @Nullable
    private String mServiceName;

    @Nullable
    private String mServiceType;

    private final Set<String> mSubtypes;

    private final ArrayMap<String, byte[]> mTxtRecord;

    private final List<InetAddress> mHostAddresses;

    @Nullable
    private String mHostname;

    private int mPort;

    @Nullable
    private byte[] mPublicKey;

    @Nullable
    private Network mNetwork;

    private int mInterfaceIndex;

    // The timestamp that one or more resource records associated with this service are considered
    // invalid.
    @Nullable
    private Instant mExpirationTime;

    public NsdServiceInfo() {
        mSubtypes = new ArraySet<>();
        mTxtRecord = new ArrayMap<>();
        mHostAddresses = new ArrayList<>();
    }

    /** @hide */
    public NsdServiceInfo(String sn, String rt) {
        this();
        mServiceName = sn;
        mServiceType = rt;
    }

    /**
     * Creates a copy of {@code other}.
     *
     * @hide
     */
    public NsdServiceInfo(@NonNull NsdServiceInfo other) {
        mServiceName = other.getServiceName();
        mServiceType = other.getServiceType();
        mSubtypes = new ArraySet<>(other.getSubtypes());
        mTxtRecord = new ArrayMap<>(other.mTxtRecord);
        mHostAddresses = new ArrayList<>(other.getHostAddresses());
        mHostname = other.getHostname();
        mPort = other.getPort();
        mNetwork = other.getNetwork();
        mInterfaceIndex = other.getInterfaceIndex();
        mExpirationTime = other.getExpirationTime();
    }

    /** Get the service name */
    public String getServiceName() {
        return mServiceName;
    }

    /** Set the service name */
    public void setServiceName(String s) {
        mServiceName = s;
    }

    /** Get the service type */
    public String getServiceType() {
        return mServiceType;
    }

    /** Set the service type */
    public void setServiceType(String s) {
        mServiceType = s;
    }

    /**
     * Get the host address. The host address is valid for a resolved service.
     *
     * @deprecated Use {@link #getHostAddresses()} to get the entire list of addresses for the host.
     */
    @Deprecated
    public InetAddress getHost() {
        return mHostAddresses.size() == 0 ? null : mHostAddresses.get(0);
    }

    /**
     * Set the host address
     *
     * @deprecated Use {@link #setHostAddresses(List)} to set multiple addresses for the host.
     */
    @Deprecated
    public void setHost(InetAddress s) {
        setHostAddresses(Collections.singletonList(s));
    }

    /**
     * Get port number. The port number is valid for a resolved service.
     *
     * The port is valid for all addresses.
     * @see #getHostAddresses()
     */
    public int getPort() {
        return mPort;
    }

    /** Set port number */
    public void setPort(int p) {
        mPort = p;
    }

    /**
     * Get the host addresses.
     *
     * All host addresses are valid for the resolved service.
     * All addresses share the same port
     * @see #getPort()
     */
    @NonNull
    public List<InetAddress> getHostAddresses() {
        return new ArrayList<>(mHostAddresses);
    }

    /** Set the host addresses */
    public void setHostAddresses(@NonNull List<InetAddress> addresses) {
        mHostAddresses.clear();
        mHostAddresses.addAll(addresses);
    }

    /**
     * Get the hostname.
     *
     * <p>When a service is resolved, it returns the hostname of the resolved service . The top
     * level domain ".local." is omitted.
     *
     * <p>For example, it returns "MyHost" when the service's hostname is "MyHost.local.".
     *
     * @hide
     */
//    @FlaggedApi(NsdManager.Flags.NSD_CUSTOM_HOSTNAME_ENABLED)
    @Nullable
    public String getHostname() {
        return mHostname;
    }

    /**
     * Set a custom hostname for this service instance for registration.
     *
     * <p>A hostname must be in ".local." domain. The ".local." must be omitted when calling this
     * method.
     *
     * <p>For example, you should call setHostname("MyHost") to use the hostname "MyHost.local.".
     *
     * <p>If a hostname is set with this method, the addresses set with {@link #setHostAddresses}
     * will be registered with the hostname.
     *
     * <p>If the hostname is null (which is the default for a new {@link NsdServiceInfo}), a random
     * hostname is used and the addresses of this device will be registered.
     *
     * @hide
     */
//    @FlaggedApi(NsdManager.Flags.NSD_CUSTOM_HOSTNAME_ENABLED)
    public void setHostname(@Nullable String hostname) {
        mHostname = hostname;
    }

    /**
     * Set the public key RDATA to be advertised in a KEY RR (RFC 2535).
     *
     * <p>This is the public key of the key pair used for signing a DNS message (e.g. SRP). Clients
     * typically don't need this information, but the KEY RR is usually published to claim the use
     * of the DNS name so that another mDNS advertiser can't take over the ownership during a
     * temporary power down of the original host device.
     *
     * <p>When the public key is set to non-null, exactly one KEY RR will be advertised for each of
     * the service and host name if they are not null.
     *
     * @hide // For Thread only
     */
    public void setPublicKey(@Nullable byte[] publicKey) {
        if (publicKey == null) {
            mPublicKey = null;
            return;
        }
        mPublicKey = Arrays.copyOf(publicKey, publicKey.length);
    }

    /**
     * Get the public key RDATA in the KEY RR (RFC 2535) or {@code null} if no KEY RR exists.
     *
     * @hide // For Thread only
     */
    @Nullable
    public byte[] getPublicKey() {
        if (mPublicKey == null) {
            return null;
        }
        return Arrays.copyOf(mPublicKey, mPublicKey.length);
    }

    /**
     * Unpack txt information from a base-64 encoded byte array.
     *
     * @param txtRecordsRawBytes The raw base64 encoded byte array.
     *
     * @hide
     */
    public void setTxtRecords(@NonNull byte[] txtRecordsRawBytes) {
        // There can be multiple TXT records after each other. Each record has to following format:
        //
        // byte                  type                  required   meaning
        // -------------------   -------------------   --------   ----------------------------------
        // 0                     unsigned 8 bit        yes        size of record excluding this byte
        // 1 - n                 ASCII but not '='     yes        key
        // n + 1                 '='                   optional   separator of key and value
        // n + 2 - record size   uninterpreted bytes   optional   value
        //
        // Example legal records:
        // [11, 'm', 'y', 'k', 'e', 'y', '=', 0x0, 0x4, 0x65, 0x7, 0xff]
        // [17, 'm', 'y', 'K', 'e', 'y', 'W', 'i', 't', 'h', 'N', 'o', 'V', 'a', 'l', 'u', 'e', '=']
        // [12, 'm', 'y', 'B', 'o', 'o', 'l', 'e', 'a', 'n', 'K', 'e', 'y']
        //
        // Example corrupted records
        // [3, =, 1, 2]    <- key is empty
        // [3, 0, =, 2]    <- key contains non-ASCII character. We handle this by replacing the
        //                    invalid characters instead of skipping the record.
        // [30, 'a', =, 2] <- length exceeds total left over bytes in the TXT records array, we
        //                    handle this by reducing the length of the record as needed.
        int pos = 0;
        while (pos < txtRecordsRawBytes.length) {
            // recordLen is an unsigned 8 bit value
            int recordLen = txtRecordsRawBytes[pos] & 0xff;
            pos += 1;

            try {
                if (recordLen == 0) {
                    throw new IllegalArgumentException("Zero sized txt record");
                } else if (pos + recordLen > txtRecordsRawBytes.length) {
                    Log.w(TAG, "Corrupt record length (pos = " + pos + "): " + recordLen);
                    recordLen = txtRecordsRawBytes.length - pos;
                }

                // Decode key-value records
                String key = null;
                byte[] value = null;
                int valueLen = 0;
                for (int i = pos; i < pos + recordLen; i++) {
                    if (key == null) {
                        if (txtRecordsRawBytes[i] == '=') {
                            key = new String(txtRecordsRawBytes, pos, i - pos,
                                    StandardCharsets.US_ASCII);
                        }
                    } else {
                        if (value == null) {
                            value = new byte[recordLen - key.length() - 1];
                        }
                        value[valueLen] = txtRecordsRawBytes[i];
                        valueLen++;
                    }
                }

                // If '=' was not found we have a boolean record
                if (key == null) {
                    key = new String(txtRecordsRawBytes, pos, recordLen, StandardCharsets.US_ASCII);
                }

                if (TextUtils.isEmpty(key)) {
                    // Empty keys are not allowed (RFC6763 6.4)
                    throw new IllegalArgumentException("Invalid txt record (key is empty)");
                }

                if (getAttributes().containsKey(key)) {
                    // When we have a duplicate record, the later ones are ignored (RFC6763 6.4)
                    throw new IllegalArgumentException("Invalid txt record (duplicate key \"" + key + "\")");
                }

                setAttribute(key, value);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "While parsing txt records (pos = " + pos + "): " + e.getMessage());
            }

            pos += recordLen;
        }
    }

    /** @hide */
    @UnsupportedAppUsage
    public void setAttribute(String key, byte[] value) {
        if (TextUtils.isEmpty(key)) {
            throw new IllegalArgumentException("Key cannot be empty");
        }

        // Key must be printable US-ASCII, excluding =.
        for (int i = 0; i < key.length(); ++i) {
            char character = key.charAt(i);
            if (character < 0x20 || character > 0x7E) {
                throw new IllegalArgumentException("Key strings must be printable US-ASCII");
            } else if (character == 0x3D) {
                throw new IllegalArgumentException("Key strings must not include '='");
            }
        }

        // Key length + value length must be < 255.
        if (key.length() + (value == null ? 0 : value.length) >= 255) {
            throw new IllegalArgumentException("Key length + value length must be < 255 bytes");
        }

        // Warn if key is > 9 characters, as recommended by RFC 6763 section 6.4.
        if (key.length() > 9) {
            Log.w(TAG, "Key lengths > 9 are discouraged: " + key);
        }

        // Check against total TXT record size limits.
        // Arbitrary 400 / 1300 byte limits taken from RFC 6763 section 6.2.
        int txtRecordSize = getTxtRecordSize();
        int futureSize = txtRecordSize + key.length() + (value == null ? 0 : value.length) + 2;
        if (futureSize > 1300) {
            throw new IllegalArgumentException("Total length of attributes must be < 1300 bytes");
        } else if (futureSize > 400) {
            Log.w(TAG, "Total length of all attributes exceeds 400 bytes; truncation may occur");
        }

        mTxtRecord.put(key, value);
    }

    /**
     * Add a service attribute as a key/value pair.
     *
     * <p> Service attributes are included as DNS-SD TXT record pairs.
     *
     * <p> The key must be US-ASCII printable characters, excluding the '=' character.  Values may
     * be UTF-8 strings or null.  The total length of key + value must be less than 255 bytes.
     *
     * <p> Keys should be short, ideally no more than 9 characters, and unique per instance of
     * {@link NsdServiceInfo}.  Calling {@link #setAttribute} twice with the same key will overwrite
     * first value.
     */
    public void setAttribute(String key, String value) {
        try {
            setAttribute(key, value == null ? (byte []) null : value.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Value must be UTF-8");
        }
    }

    /** Remove an attribute by key */
    public void removeAttribute(String key) {
        mTxtRecord.remove(key);
    }

    /**
     * Retrieve attributes as a map of String keys to byte[] values. The attributes map is only
     * valid for a resolved service.
     *
     * <p> The returned map is unmodifiable; changes must be made through {@link #setAttribute} and
     * {@link #removeAttribute}.
     */
    public Map<String, byte[]> getAttributes() {
        return Collections.unmodifiableMap(mTxtRecord);
    }

    private int getTxtRecordSize() {
        int txtRecordSize = 0;
        for (Map.Entry<String, byte[]> entry : mTxtRecord.entrySet()) {
            txtRecordSize += 2;  // One for the length byte, one for the = between key and value.
            txtRecordSize += entry.getKey().length();
            byte[] value = entry.getValue();
            txtRecordSize += value == null ? 0 : value.length;
        }
        return txtRecordSize;
    }

    /** @hide */
    public @NonNull byte[] getTxtRecord() {
        int txtRecordSize = getTxtRecordSize();
        if (txtRecordSize == 0) {
            return new byte[]{};
        }

        byte[] txtRecord = new byte[txtRecordSize];
        int ptr = 0;
        for (Map.Entry<String, byte[]> entry : mTxtRecord.entrySet()) {
            String key = entry.getKey();
            byte[] value = entry.getValue();

            // One byte to record the length of this key/value pair.
            txtRecord[ptr++] = (byte) (key.length() + (value == null ? 0 : value.length) + 1);

            // The key, in US-ASCII.
            // Note: use the StandardCharsets const here because it doesn't raise exceptions and we
            // already know the key is ASCII at this point.
            System.arraycopy(key.getBytes(StandardCharsets.US_ASCII), 0, txtRecord, ptr,
                    key.length());
            ptr += key.length();

            // US-ASCII '=' character.
            txtRecord[ptr++] = (byte)'=';

            // The value, as any raw bytes.
            if (value != null) {
                System.arraycopy(value, 0, txtRecord, ptr, value.length);
                ptr += value.length;
            }
        }
        return txtRecord;
    }

    /**
     * Get the network where the service can be found.
     *
     * This is set if this {@link NsdServiceInfo} was obtained from
     * {@link NsdManager#discoverServices} or {@link NsdManager#resolveService}, unless the service
     * was found on a network interface that does not have a {@link Network} (such as a tethering
     * downstream, where services are advertised from devices connected to this device via
     * tethering).
     */
    @Nullable
    public Network getNetwork() {
        return mNetwork;
    }

    /**
     * Set the network where the service can be found.
     * @param network The network, or null to search for, or to announce, the service on all
     *                connected networks.
     */
    public void setNetwork(@Nullable Network network) {
        mNetwork = network;
    }

    /**
     * Get the index of the network interface where the service was found.
     *
     * This is only set when the service was found on an interface that does not have a usable
     * Network, in which case {@link #getNetwork()} returns null.
     * @return The interface index as per {@link java.net.NetworkInterface#getIndex}, or 0 if unset.
     * @hide
     */
    public int getInterfaceIndex() {
        return mInterfaceIndex;
    }

    /**
     * Set the index of the network interface where the service was found.
     * @hide
     */
    public void setInterfaceIndex(int interfaceIndex) {
        mInterfaceIndex = interfaceIndex;
    }

    /**
     * Sets the subtypes to be advertised for this service instance.
     *
     * The elements in {@code subtypes} should be the subtype identifiers which have the trailing
     * "._sub" removed. For example, the subtype should be "_printer" for
     * "_printer._sub._http._tcp.local".
     *
     * Only one subtype will be registered if multiple elements of {@code subtypes} have the same
     * case-insensitive value.
     */
    @FlaggedApi(NsdManager.Flags.NSD_SUBTYPES_SUPPORT_ENABLED)
    public void setSubtypes(@NonNull Set<String> subtypes) {
        mSubtypes.clear();
        mSubtypes.addAll(subtypes);
    }

    /**
     * Returns subtypes of this service instance.
     *
     * When this object is returned by the service discovery/browse APIs (etc. {@link
     * NsdManager.DiscoveryListener}), the return value may or may not include the subtypes of this
     * service.
     */
    @FlaggedApi(NsdManager.Flags.NSD_SUBTYPES_SUPPORT_ENABLED)
    @NonNull
    public Set<String> getSubtypes() {
        return Collections.unmodifiableSet(mSubtypes);
    }

    /**
     * Sets the timestamp after when this service is expired.
     *
     * Note: the value after the decimal point (in unit of seconds) will be discarded. For
     * example, {@code 30} seconds will be used when {@code Duration.ofSeconds(30L, 50_000L)}
     * is provided.
     *
     * @hide
     */
    public void setExpirationTime(@Nullable Instant expirationTime) {
        if (expirationTime == null) {
            mExpirationTime = null;
        } else {
            mExpirationTime = Instant.ofEpochSecond(expirationTime.getEpochSecond());
        }
    }

    /**
     * Returns the timestamp after when this service is expired or {@code null} if it's unknown.
     *
     * A service is considered expired if any of its DNS record is expired.
     *
     * Clients that are depending on the refreshness of the service information should not continue
     * use this service after the returned timestamp. Instead, clients may re-send queries for the
     * service to get updated the service information.
     *
     * @hide
     */
    // @FlaggedApi(NsdManager.Flags.NSD_CUSTOM_TTL_ENABLED)
    @Nullable
    public Instant getExpirationTime() {
        return mExpirationTime;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("name: ").append(mServiceName)
                .append(", type: ").append(mServiceType)
                .append(", subtypes: ").append(TextUtils.join(", ", mSubtypes))
                .append(", hostAddresses: ").append(TextUtils.join(", ", mHostAddresses))
                .append(", hostname: ").append(mHostname)
                .append(", port: ").append(mPort)
                .append(", network: ").append(mNetwork)
                .append(", expirationTime: ").append(mExpirationTime);

        final StringJoiner txtJoiner =
                new StringJoiner(", " /* delimiter */, "{" /* prefix */, "}" /* suffix */);

        sb.append(", txtRecord: ");
        for (int i = 0; i < mTxtRecord.size(); i++) {
            txtJoiner.add(mTxtRecord.keyAt(i) + "=" + getPrintableTxtValue(mTxtRecord.valueAt(i)));
        }
        sb.append(txtJoiner.toString());
        return sb.toString();
    }

    /**
     * Returns printable string for {@code txtValue}.
     *
     * If {@code txtValue} contains non-printable ASCII characters, a HEX string with prefix "0x"
     * will be returned. Otherwise, the ASCII string of {@code txtValue} is returned.
     *
     */
    private static String getPrintableTxtValue(@Nullable byte[] txtValue) {
        if (txtValue == null) {
            return "(null)";
        }

        if (containsNonPrintableChars(txtValue)) {
            return "0x" + toHexString(txtValue);
        }

        return new String(txtValue, StandardCharsets.US_ASCII);
    }

    /**
     * Returns {@code true} if {@code txtValue} contains non-printable ASCII characters.
     *
     * The printable characters are in range of [32, 126].
     */
    private static boolean containsNonPrintableChars(byte[] txtValue) {
        for (int i = 0; i < txtValue.length; i++) {
            if (txtValue[i] < 32 || txtValue[i] > 126) {
                return true;
            }
        }
        return false;
    }

    /** Implement the Parcelable interface */
    public int describeContents() {
        return 0;
    }

    /** Implement the Parcelable interface */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mServiceName);
        dest.writeString(mServiceType);
        dest.writeStringList(new ArrayList<>(mSubtypes));
        dest.writeInt(mPort);

        // TXT record key/value pairs.
        dest.writeInt(mTxtRecord.size());
        for (String key : mTxtRecord.keySet()) {
            byte[] value = mTxtRecord.get(key);
            if (value != null) {
                dest.writeInt(1);
                dest.writeInt(value.length);
                dest.writeByteArray(value);
            } else {
                dest.writeInt(0);
            }
            dest.writeString(key);
        }

        dest.writeParcelable(mNetwork, 0);
        dest.writeInt(mInterfaceIndex);
        dest.writeInt(mHostAddresses.size());
        for (InetAddress address : mHostAddresses) {
            InetAddressUtils.parcelInetAddress(dest, address, flags);
        }
        dest.writeString(mHostname);
        dest.writeLong(mExpirationTime != null ? mExpirationTime.getEpochSecond() : -1);
        dest.writeByteArray(mPublicKey);
    }

    /** Implement the Parcelable interface */
    public static final @android.annotation.NonNull Creator<NsdServiceInfo> CREATOR =
        new Creator<NsdServiceInfo>() {
            public NsdServiceInfo createFromParcel(Parcel in) {
                NsdServiceInfo info = new NsdServiceInfo();
                info.mServiceName = in.readString();
                info.mServiceType = in.readString();
                info.setSubtypes(new ArraySet<>(in.createStringArrayList()));
                info.mPort = in.readInt();

                // TXT record key/value pairs.
                int recordCount = in.readInt();
                for (int i = 0; i < recordCount; ++i) {
                    byte[] valueArray = null;
                    if (in.readInt() == 1) {
                        int valueLength = in.readInt();
                        valueArray = new byte[valueLength];
                        in.readByteArray(valueArray);
                    }
                    info.mTxtRecord.put(in.readString(), valueArray);
                }
                info.mNetwork = in.readParcelable(null, Network.class);
                info.mInterfaceIndex = in.readInt();
                int size = in.readInt();
                for (int i = 0; i < size; i++) {
                    info.mHostAddresses.add(InetAddressUtils.unparcelInetAddress(in));
                }
                info.mHostname = in.readString();
                final long seconds = in.readLong();
                info.setExpirationTime(seconds < 0 ? null : Instant.ofEpochSecond(seconds));
                info.mPublicKey = in.createByteArray();
                return info;
            }

            public NsdServiceInfo[] newArray(int size) {
                return new NsdServiceInfo[size];
            }
        };
}
