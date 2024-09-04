/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.net.module.util;

import static android.net.DnsResolver.CLASS_IN;

import static com.android.internal.annotations.VisibleForTesting.Visibility.PACKAGE;
import static com.android.internal.annotations.VisibleForTesting.Visibility.PRIVATE;
import static com.android.net.module.util.DnsPacket.ParseException;

import android.annotation.NonNull;
import android.text.TextUtils;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/**
 * A class for an SVCB record.
 * https://www.iana.org/assignments/dns-svcb/dns-svcb.xhtml
 * @hide
 */
@VisibleForTesting(visibility = PACKAGE)
public final class DnsSvcbRecord extends DnsPacket.DnsRecord {
    /**
     * The following SvcParamKeys KEY_* are defined in
     * https://www.iana.org/assignments/dns-svcb/dns-svcb.xhtml.
     */

    // The SvcParamKey "mandatory". The associated implementation of SvcParam is SvcParamMandatory.
    private static final int KEY_MANDATORY = 0;

    // The SvcParamKey "alpn". The associated implementation of SvcParam is SvcParamAlpn.
    private static final int KEY_ALPN = 1;

    // The SvcParamKey "no-default-alpn". The associated implementation of SvcParam is
    // SvcParamNoDefaultAlpn.
    private static final int KEY_NO_DEFAULT_ALPN = 2;

    // The SvcParamKey "port". The associated implementation of SvcParam is SvcParamPort.
    private static final int KEY_PORT = 3;

    // The SvcParamKey "ipv4hint". The associated implementation of SvcParam is SvcParamIpv4Hint.
    private static final int KEY_IPV4HINT = 4;

    // The SvcParamKey "ech". The associated implementation of SvcParam is SvcParamEch.
    private static final int KEY_ECH = 5;

    // The SvcParamKey "ipv6hint". The associated implementation of SvcParam is SvcParamIpv6Hint.
    private static final int KEY_IPV6HINT = 6;

    // The SvcParamKey "dohpath". The associated implementation of SvcParam is SvcParamDohPath.
    private static final int KEY_DOHPATH = 7;

    // The minimal size of a SvcParam.
    // https://www.ietf.org/archive/id/draft-ietf-dnsop-svcb-https-12.html#name-rdata-wire-format
    private static final int MINSVCPARAMSIZE = 4;

    private static final String TAG = DnsSvcbRecord.class.getSimpleName();

    private final int mSvcPriority;

    @NonNull
    private final String mTargetName;

    @NonNull
    private final SparseArray<SvcParam> mAllSvcParams = new SparseArray<>();

    @VisibleForTesting(visibility = PACKAGE)
    public DnsSvcbRecord(@DnsPacket.RecordType int rType, @NonNull ByteBuffer buff)
            throws IllegalStateException, ParseException {
        super(rType, buff);
        if (nsType != DnsPacket.TYPE_SVCB) {
            throw new IllegalStateException("incorrect nsType: " + nsType);
        }
        if (nsClass != CLASS_IN) {
            throw new ParseException("incorrect nsClass: " + nsClass);
        }

        // DNS Record in Question Section doesn't have Rdata.
        if (rType == DnsPacket.QDSECTION) {
            mSvcPriority = 0;
            mTargetName = "";
            return;
        }

        final byte[] rdata = getRR();
        if (rdata == null) {
            throw new ParseException("SVCB rdata is empty");
        }

        final ByteBuffer buf = ByteBuffer.wrap(rdata).asReadOnlyBuffer();
        mSvcPriority = Short.toUnsignedInt(buf.getShort());
        mTargetName = DnsPacketUtils.DnsRecordParser.parseName(buf, 0 /* Parse depth */,
                false /* isNameCompressionSupported */);

        if (mTargetName.length() > DnsPacket.DnsRecord.MAXNAMESIZE) {
            throw new ParseException(
                    "Failed to parse SVCB target name, name size is too long: "
                            + mTargetName.length());
        }
        while (buf.remaining() >= MINSVCPARAMSIZE) {
            final SvcParam svcParam = parseSvcParam(buf);
            final int key = svcParam.getKey();
            if (mAllSvcParams.get(key) != null) {
                throw new ParseException("Invalid DnsSvcbRecord, key " + key + " is repeated");
            }
            mAllSvcParams.put(key, svcParam);
        }
        if (buf.hasRemaining()) {
            throw new ParseException("Invalid DnsSvcbRecord. Got "
                    + buf.remaining() + " remaining bytes after parsing");
        }
    }

    /**
     * Returns the TargetName.
     */
    @VisibleForTesting(visibility = PACKAGE)
    @NonNull
    public String getTargetName() {
        return mTargetName;
    }

    /**
     * Returns an unmodifiable list of alpns from SvcParam alpn.
     */
    @VisibleForTesting(visibility = PACKAGE)
    @NonNull
    public List<String> getAlpns() {
        final SvcParamAlpn sp = (SvcParamAlpn) mAllSvcParams.get(KEY_ALPN);
        final List<String> list = (sp != null) ? sp.getValue() : Collections.EMPTY_LIST;
        return Collections.unmodifiableList(list);
    }

    /**
     * Returns the port number from SvcParam port.
     */
    @VisibleForTesting(visibility = PACKAGE)
    public int getPort() {
        final SvcParamPort sp = (SvcParamPort) mAllSvcParams.get(KEY_PORT);
        return (sp != null) ? sp.getValue() : -1;
    }

    /**
     * Returns a list of the IP addresses from both of SvcParam ipv4hint and ipv6hint.
     */
    @VisibleForTesting(visibility = PACKAGE)
    @NonNull
    public List<InetAddress> getAddresses() {
        final List<InetAddress> out = new ArrayList<>();
        final SvcParamIpHint sp4 = (SvcParamIpHint) mAllSvcParams.get(KEY_IPV4HINT);
        if (sp4 != null) {
            out.addAll(sp4.getValue());
        }
        final SvcParamIpHint sp6 = (SvcParamIpHint) mAllSvcParams.get(KEY_IPV6HINT);
        if (sp6 != null) {
            out.addAll(sp6.getValue());
        }
        return out;
    }

    /**
     * Returns the doh path from SvcParam dohPath.
     */
    @VisibleForTesting(visibility = PACKAGE)
    @NonNull
    public String getDohPath() {
        final SvcParamDohPath sp = (SvcParamDohPath) mAllSvcParams.get(KEY_DOHPATH);
        return (sp != null) ? sp.getValue() : "";
    }

    @Override
    public String toString() {
        if (rType == DnsPacket.QDSECTION) {
            return dName + " IN SVCB";
        }

        final StringJoiner sj = new StringJoiner(" ");
        for (int i = 0; i < mAllSvcParams.size(); i++) {
            sj.add(mAllSvcParams.valueAt(i).toString());
        }
        return dName + " " + ttl + " IN SVCB " + mSvcPriority + " " + mTargetName + " "
                + sj.toString();
    }

    private static SvcParam parseSvcParam(@NonNull ByteBuffer buf) throws ParseException {
        try {
            final int key = Short.toUnsignedInt(buf.getShort());
            switch (key) {
                case KEY_MANDATORY: return new SvcParamMandatory(buf);
                case KEY_ALPN: return new SvcParamAlpn(buf);
                case KEY_NO_DEFAULT_ALPN: return new SvcParamNoDefaultAlpn(buf);
                case KEY_PORT: return new SvcParamPort(buf);
                case KEY_IPV4HINT: return new SvcParamIpv4Hint(buf);
                case KEY_ECH: return new SvcParamEch(buf);
                case KEY_IPV6HINT: return new SvcParamIpv6Hint(buf);
                case KEY_DOHPATH: return new SvcParamDohPath(buf);
                default: return new SvcParamGeneric(key, buf);
            }
        } catch (BufferUnderflowException e) {
            throw new ParseException("Malformed packet", e);
        }
    }

    /**
     * The base class for all SvcParam.
     */
    private abstract static class SvcParam<T> {
        private final int mKey;

        SvcParam(int key) {
            mKey = key;
        }

        int getKey() {
            return mKey;
        }

        abstract T getValue();
    }

    private static class SvcParamMandatory extends SvcParam<short[]> {
        private final short[] mValue;

        private SvcParamMandatory(@NonNull ByteBuffer buf) throws BufferUnderflowException,
                ParseException {
            super(KEY_MANDATORY);
            // The caller already read 2 bytes for SvcParamKey.
            final int len = Short.toUnsignedInt(buf.getShort());
            final ByteBuffer svcParamValue = sliceAndAdvance(buf, len);
            mValue = SvcParamValueUtil.toShortArray(svcParamValue);
            if (mValue.length == 0) {
                throw new ParseException("mandatory value must be non-empty");
            }
        }

        @Override
        short[] getValue() {
            /* Not yet implemented */
            return null;
        }

        @Override
        public String toString() {
            final StringJoiner valueJoiner = new StringJoiner(",");
            for (short key : mValue) {
                valueJoiner.add(toKeyName(key));
            }
            return toKeyName(getKey()) + "=" + valueJoiner.toString();
        }
    }

    private static class SvcParamAlpn extends SvcParam<List<String>> {
        private final List<String> mValue;

        SvcParamAlpn(@NonNull ByteBuffer buf) throws BufferUnderflowException, ParseException {
            super(KEY_ALPN);
            // The caller already read 2 bytes for SvcParamKey.
            final int len = Short.toUnsignedInt(buf.getShort());
            final ByteBuffer svcParamValue = sliceAndAdvance(buf, len);
            mValue = SvcParamValueUtil.toStringList(svcParamValue);
            if (mValue.isEmpty()) {
                throw new ParseException("alpn value must be non-empty");
            }
        }

        @Override
        List<String> getValue() {
            return Collections.unmodifiableList(mValue);
        }

        @Override
        public String toString() {
            return toKeyName(getKey()) + "=" + TextUtils.join(",", mValue);
        }
    }

    private static class SvcParamNoDefaultAlpn extends SvcParam<Void> {
        SvcParamNoDefaultAlpn(@NonNull ByteBuffer buf) throws BufferUnderflowException,
                ParseException {
            super(KEY_NO_DEFAULT_ALPN);
            // The caller already read 2 bytes for SvcParamKey.
            final int len = buf.getShort();
            if (len != 0) {
                throw new ParseException("no-default-alpn value must be empty");
            }
        }

        @Override
        Void getValue() {
            return null;
        }

        @Override
        public String toString() {
            return toKeyName(getKey());
        }
    }

    private static class SvcParamPort extends SvcParam<Integer> {
        private final int mValue;

        SvcParamPort(@NonNull ByteBuffer buf) throws BufferUnderflowException, ParseException {
            super(KEY_PORT);
            // The caller already read 2 bytes for SvcParamKey.
            final int len = buf.getShort();
            if (len != Short.BYTES) {
                throw new ParseException("key port len is not 2 but " + len);
            }
            mValue = Short.toUnsignedInt(buf.getShort());
        }

        @Override
        Integer getValue() {
            return mValue;
        }

        @Override
        public String toString() {
            return toKeyName(getKey()) + "=" + mValue;
        }
    }

    private static class SvcParamIpHint extends SvcParam<List<InetAddress>> {
        private final List<InetAddress> mValue;

        private SvcParamIpHint(int key, @NonNull ByteBuffer buf, int addrLen) throws
                BufferUnderflowException, ParseException {
            super(key);
            // The caller already read 2 bytes for SvcParamKey.
            final int len = Short.toUnsignedInt(buf.getShort());
            final ByteBuffer svcParamValue = sliceAndAdvance(buf, len);
            mValue = SvcParamValueUtil.toInetAddressList(svcParamValue, addrLen);
            if (mValue.isEmpty()) {
                throw new ParseException(toKeyName(getKey()) + " value must be non-empty");
            }
        }

        @Override
        List<InetAddress> getValue() {
            return Collections.unmodifiableList(mValue);
        }

        @Override
        public String toString() {
            final StringJoiner valueJoiner = new StringJoiner(",");
            for (InetAddress ip : mValue) {
                valueJoiner.add(ip.getHostAddress());
            }
            return toKeyName(getKey()) + "=" + valueJoiner.toString();
        }
    }

    private static class SvcParamIpv4Hint extends SvcParamIpHint {
        SvcParamIpv4Hint(@NonNull ByteBuffer buf) throws BufferUnderflowException, ParseException {
            super(KEY_IPV4HINT, buf, NetworkStackConstants.IPV4_ADDR_LEN);
        }
    }

    private static class SvcParamIpv6Hint extends SvcParamIpHint {
        SvcParamIpv6Hint(@NonNull ByteBuffer buf) throws BufferUnderflowException, ParseException {
            super(KEY_IPV6HINT, buf, NetworkStackConstants.IPV6_ADDR_LEN);
        }
    }

    private static class SvcParamEch extends SvcParamGeneric {
        SvcParamEch(@NonNull ByteBuffer buf) throws BufferUnderflowException, ParseException {
            super(KEY_ECH, buf);
        }
    }

    private static class SvcParamDohPath extends SvcParam<String> {
        private final String mValue;

        SvcParamDohPath(@NonNull ByteBuffer buf) throws BufferUnderflowException, ParseException {
            super(KEY_DOHPATH);
            // The caller already read 2 bytes for SvcParamKey.
            final int len = Short.toUnsignedInt(buf.getShort());
            final byte[] value = new byte[len];
            buf.get(value);
            mValue = new String(value, StandardCharsets.UTF_8);
        }

        @Override
        String getValue() {
            return mValue;
        }

        @Override
        public String toString() {
            return toKeyName(getKey()) + "=" + mValue;
        }
    }

    // For other unrecognized and unimplemented SvcParams, they are stored as SvcParamGeneric.
    private static class SvcParamGeneric extends SvcParam<byte[]> {
        private final byte[] mValue;

        SvcParamGeneric(int key, @NonNull ByteBuffer buf) throws BufferUnderflowException,
                ParseException {
            super(key);
            // The caller already read 2 bytes for SvcParamKey.
            final int len = Short.toUnsignedInt(buf.getShort());
            mValue = new byte[len];
            buf.get(mValue);
        }

        @Override
        byte[] getValue() {
            /* Not yet implemented */
            return null;
        }

        @Override
        public String toString() {
            final StringBuilder out = new StringBuilder();
            out.append(toKeyName(getKey()));
            if (mValue != null && mValue.length > 0) {
                out.append("=");
                out.append(HexDump.toHexString(mValue));
            }
            return out.toString();
        }
    }

    private static String toKeyName(int key) {
        switch (key) {
            case KEY_MANDATORY: return "mandatory";
            case KEY_ALPN: return "alpn";
            case KEY_NO_DEFAULT_ALPN: return "no-default-alpn";
            case KEY_PORT: return "port";
            case KEY_IPV4HINT: return "ipv4hint";
            case KEY_ECH: return "ech";
            case KEY_IPV6HINT: return "ipv6hint";
            case KEY_DOHPATH: return "dohpath";
            default: return "key" + key;
        }
    }

    /**
     * Returns a read-only ByteBuffer (with position = 0, limit = `length`, and capacity = `length`)
     * sliced from `buf`'s current position, and moves the position of `buf` by `length`.
     */
    @VisibleForTesting(visibility = PRIVATE)
    public static ByteBuffer sliceAndAdvance(@NonNull ByteBuffer buf, int length)
            throws BufferUnderflowException {
        if (buf.remaining() < length) {
            throw new BufferUnderflowException();
        }
        final int pos = buf.position();

        // `out` equals to `buf.slice(pos, length)` that is supported in API level 34.
        final ByteBuffer out = ((ByteBuffer) buf.slice().limit(length)).slice();

        buf.position(pos + length);
        return out.asReadOnlyBuffer();
    }

    // A utility to convert the byte array of SvcParamValue to other types.
    private static class SvcParamValueUtil {
        // Refer to draft-ietf-dnsop-svcb-https-10#section-7.1 for the wire format of alpn.
        @NonNull
        private static List<String> toStringList(@NonNull ByteBuffer buf)
                throws BufferUnderflowException, ParseException {
            final List<String> out = new ArrayList<>();
            while (buf.hasRemaining()) {
                final int alpnLen = Byte.toUnsignedInt(buf.get());
                if (alpnLen == 0) {
                    throw new ParseException("alpn should not be an empty string");
                }
                final byte[] alpn = new byte[alpnLen];
                buf.get(alpn);
                out.add(new String(alpn, StandardCharsets.UTF_8));
            }
            return out;
        }

        // Refer to draft-ietf-dnsop-svcb-https-10#section-7.5 for the wire format of SvcParamKey
        // "mandatory".
        @NonNull
        private static short[] toShortArray(@NonNull ByteBuffer buf)
                throws BufferUnderflowException, ParseException {
            if (buf.remaining() % Short.BYTES != 0) {
                throw new ParseException("Can't parse whole byte array");
            }
            final ShortBuffer sb = buf.asShortBuffer();
            final short[] out = new short[sb.remaining()];
            sb.get(out);
            return out;
        }

        // Refer to draft-ietf-dnsop-svcb-https-10#section-7.4 for the wire format of ipv4hint and
        // ipv6hint.
        @NonNull
        private static List<InetAddress> toInetAddressList(@NonNull ByteBuffer buf, int addrLen)
                throws BufferUnderflowException, ParseException {
            if (buf.remaining() % addrLen != 0) {
                throw new ParseException("Can't parse whole byte array");
            }

            final List<InetAddress> out = new ArrayList<>();
            final byte[] addr = new byte[addrLen];
            while (buf.remaining() >= addrLen) {
                buf.get(addr);
                try {
                    out.add(InetAddress.getByAddress(addr));
                } catch (UnknownHostException e) {
                    throw new ParseException("Can't parse byte array as an IP address");
                }
            }
            return out;
        }
    }
}
