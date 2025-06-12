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
import java.util.Objects;

public class FlagTable {

    private Header mHeader;
    private ByteBufferReader mReader;

    public static FlagTable fromBytes(ByteBuffer bytes) {
        FlagTable flagTable = new FlagTable();
        flagTable.mReader = new ByteBufferReader(bytes);
        flagTable.mHeader = Header.fromBytes(flagTable.mReader);

        return flagTable;
    }

    public Node get(int packageId, String flagName) {
        int numBuckets = (mHeader.mNodeOffset - mHeader.mBucketOffset) / 4;
        int bucketIndex = TableUtils.getBucketIndex(makeKey(packageId, flagName), numBuckets);
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
            Node node = Node.fromBytes(mReader);
            if (Objects.equals(flagName, node.mFlagName) && packageId == node.mPackageId) {
                return node;
            }
            nodeIndex = node.mNextOffset;
        }

        return null;
    }

    public Header getHeader() {
        return mHeader;
    }

    private static byte[] makeKey(int packageId, String flagName) {
        StringBuilder ret = new StringBuilder();
        return ret.append(packageId).append('/').append(flagName).toString().getBytes(UTF_8);
    }

    public static class Header {

        private int mVersion;
        private String mContainer;
        private FileType mFileType;
        private int mFileSize;
        private int mNumFlags;
        private int mBucketOffset;
        private int mNodeOffset;

        public static Header fromBytes(ByteBufferReader reader) {
            Header header = new Header();
            header.mVersion = reader.readInt();
            header.mContainer = reader.readString();
            header.mFileType = FileType.fromInt(reader.readByte());
            header.mFileSize = reader.readInt();
            header.mNumFlags = reader.readInt();
            header.mBucketOffset = reader.readInt();
            header.mNodeOffset = reader.readInt();

            if (header.mFileType != FileType.FLAG_MAP) {
                throw new AconfigStorageException("binary file is not a flag map");
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

        public int getNumFlags() {
            return mNumFlags;
        }

        public int getBucketOffset() {
            return mBucketOffset;
        }

        public int getNodeOffset() {
            return mNodeOffset;
        }
    }

    public static class Node {

        private String mFlagName;
        private FlagType mFlagType;
        private int mPackageId;
        private int mFlagIndex;
        private int mNextOffset;

        public static Node fromBytes(ByteBufferReader reader) {
            Node node = new Node();
            node.mPackageId = reader.readInt();
            node.mFlagName = reader.readString();
            node.mFlagType = FlagType.fromInt(reader.readShort());
            node.mFlagIndex = reader.readShort();
            node.mNextOffset = reader.readInt();
            node.mNextOffset = node.mNextOffset == 0 ? -1 : node.mNextOffset;
            return node;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mFlagName, mFlagType, mPackageId, mFlagIndex, mNextOffset);
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
            return Objects.equals(mFlagName, other.mFlagName)
                    && Objects.equals(mFlagType, other.mFlagType)
                    && mPackageId == other.mPackageId
                    && mFlagIndex == other.mFlagIndex
                    && mNextOffset == other.mNextOffset;
        }

        public String getFlagName() {
            return mFlagName;
        }

        public FlagType getFlagType() {
            return mFlagType;
        }

        public int getPackageId() {
            return mPackageId;
        }

        public int getFlagIndex() {
            return mFlagIndex;
        }

        public int getNextOffset() {
            return mNextOffset;
        }
    }
}
