/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.aconfig.storage;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PackageTable {

    private static final int FINGERPRINT_BYTES = 8;
    // int: mPackageId + int: mBooleanStartIndex + int: mNextOffset
    private static final int NODE_SKIP_BYTES = 12;

    private Header mHeader;
    private ByteBufferReader mReader;

    public static PackageTable fromBytes(ByteBuffer bytes) {
        PackageTable packageTable = new PackageTable();
        packageTable.mReader = new ByteBufferReader(bytes);
        packageTable.mHeader = Header.fromBytes(packageTable.mReader);

        return packageTable;
    }

    public Node get(String packageName) {
        int numBuckets = (mHeader.mNodeOffset - mHeader.mBucketOffset) / 4;
        int bucketIndex = TableUtils.getBucketIndex(packageName.getBytes(UTF_8), numBuckets);
        int newPosition = mHeader.mBucketOffset + bucketIndex * 4;
        if (newPosition >= mHeader.mNodeOffset) {
            return null;
        }
        mReader.position(newPosition);
        int nodeIndex = mReader.readInt();

        if (nodeIndex < mHeader.mNodeOffset || nodeIndex >= mHeader.mFileSize) {
            return null;
        }

        while (nodeIndex != -1) {
            mReader.position(nodeIndex);
            Node node = Node.fromBytes(mReader, mHeader.mVersion);
            if (Objects.equals(packageName, node.mPackageName)) {
                return node;
            }
            nodeIndex = node.mNextOffset;
        }

        return null;
    }

    public List<String> getPackageList() {
        List<String> list = new ArrayList<>(mHeader.mNumPackages);
        mReader.position(mHeader.mNodeOffset);
        int fingerprintBytes = mHeader.mVersion == 1 ? 0 : FINGERPRINT_BYTES;
        int skipBytes = fingerprintBytes + NODE_SKIP_BYTES;
        for (int i = 0; i < mHeader.mNumPackages; i++) {
            list.add(mReader.readString());
            mReader.position(mReader.position() + skipBytes);
        }
        return list;
    }

    public Header getHeader() {
        return mHeader;
    }

    public static class Header {

        private int mVersion;
        private String mContainer;
        private FileType mFileType;
        private int mFileSize;
        private int mNumPackages;
        private int mBucketOffset;
        private int mNodeOffset;

        private static Header fromBytes(ByteBufferReader reader) {
            Header header = new Header();
            header.mVersion = reader.readInt();
            header.mContainer = reader.readString();
            header.mFileType = FileType.fromInt(reader.readByte());
            header.mFileSize = reader.readInt();
            header.mNumPackages = reader.readInt();
            header.mBucketOffset = reader.readInt();
            header.mNodeOffset = reader.readInt();

            if (header.mFileType != FileType.PACKAGE_MAP) {
                throw new AconfigStorageException("binary file is not a package map");
            }

            return header;
        }

        public int getVersion() {
            return mVersion;
        }

        public String getContainer() {
            return mContainer;
        }

        public FileType getFileType() {
            return mFileType;
        }

        public int getFileSize() {
            return mFileSize;
        }

        public int getNumPackages() {
            return mNumPackages;
        }

        public int getBucketOffset() {
            return mBucketOffset;
        }

        public int getNodeOffset() {
            return mNodeOffset;
        }
    }

    public static class Node {

        private String mPackageName;
        private int mPackageId;
        private long mPackageFingerprint;
        private int mBooleanStartIndex;
        private int mNextOffset;
        private boolean mHasPackageFingerprint;

        private static Node fromBytes(ByteBufferReader reader, int version) {
            switch (version) {
                case 1:
                    return fromBytesV1(reader);
                case 2:
                    return fromBytesV2(reader);
                default:
                    // Do we want to throw here?
                    return new Node();
            }
        }

        private static Node fromBytesV1(ByteBufferReader reader) {
            Node node = new Node();
            node.mPackageName = reader.readString();
            node.mPackageId = reader.readInt();
            node.mBooleanStartIndex = reader.readInt();
            node.mNextOffset = reader.readInt();
            node.mNextOffset = node.mNextOffset == 0 ? -1 : node.mNextOffset;
            return node;
        }

        private static Node fromBytesV2(ByteBufferReader reader) {
            Node node = new Node();
            node.mPackageName = reader.readString();
            node.mPackageId = reader.readInt();
            node.mPackageFingerprint = reader.readLong();
            node.mBooleanStartIndex = reader.readInt();
            node.mNextOffset = reader.readInt();
            node.mNextOffset = node.mNextOffset == 0 ? -1 : node.mNextOffset;
            node.mHasPackageFingerprint = true;
            return node;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mPackageName, mPackageId, mBooleanStartIndex, mNextOffset);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj == null || !(obj instanceof Node)) {
                return false;
            }

            Node other = (Node) obj;
            return Objects.equals(mPackageName, other.mPackageName)
                    && mPackageId == other.mPackageId
                    && mBooleanStartIndex == other.mBooleanStartIndex
                    && mNextOffset == other.mNextOffset;
        }

        public String getPackageName() {
            return mPackageName;
        }

        public int getPackageId() {
            return mPackageId;
        }

        public long getPackageFingerprint() {
            return mPackageFingerprint;
        }

        public int getBooleanStartIndex() {
            return mBooleanStartIndex;
        }

        public int getNextOffset() {
            return mNextOffset;
        }

        public boolean hasPackageFingerprint() {
            return mHasPackageFingerprint;
        }
    }
}
