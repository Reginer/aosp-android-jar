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

import java.nio.ByteBuffer;

public class FlagValueList {

    private Header mHeader;
    private ByteBufferReader mReader;

    public static FlagValueList fromBytes(ByteBuffer bytes) {
        FlagValueList flagValueList = new FlagValueList();
        flagValueList.mReader = new ByteBufferReader(bytes);
        flagValueList.mHeader = Header.fromBytes(flagValueList.mReader);
        return flagValueList;
    }

    public boolean getBoolean(int index) {
        return mReader.readByte(mHeader.mBooleanValueOffset + index) == 1;
    }

    public Header getHeader() {
        return mHeader;
    }

    public int size() {
        return mHeader.mNumFlags;
    }

    public static class Header {

        private int mVersion;
        private String mContainer;
        private FileType mFileType;
        private int mFileSize;
        private int mNumFlags;
        private int mBooleanValueOffset;

        public static Header fromBytes(ByteBufferReader reader) {
            Header header = new Header();
            header.mVersion = reader.readInt();
            header.mContainer = reader.readString();
            header.mFileType = FileType.fromInt(reader.readByte());
            header.mFileSize = reader.readInt();
            header.mNumFlags = reader.readInt();
            header.mBooleanValueOffset = reader.readInt();

            if (header.mFileType != FileType.FLAG_VAL) {
                throw new AconfigStorageException("binary file is not a flag value file");
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

        public int getBooleanValueOffset() {
            return mBooleanValueOffset;
        }
    }
}
