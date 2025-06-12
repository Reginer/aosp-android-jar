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

public class TableUtils {

    private static final int[] HASH_PRIMES =
            new int[] {
                7,
                17,
                29,
                53,
                97,
                193,
                389,
                769,
                1543,
                3079,
                6151,
                12289,
                24593,
                49157,
                98317,
                196613,
                393241,
                786433,
                1572869,
                3145739,
                6291469,
                12582917,
                25165843,
                50331653,
                100663319,
                201326611,
                402653189,
                805306457,
                1610612741
            };

    public static int getTableSize(int numEntries) {
        for (int i : HASH_PRIMES) {
            if (i < 2 * numEntries) continue;
            return i;
        }
        throw new AconfigStorageException("Number of items in a hash table exceeds limit");
    }

    public static int getBucketIndex(byte[] val, int numBuckets) {
        long hashVal = SipHasher13.hash(val);
        return (int) Long.remainderUnsigned(hashVal, numBuckets);
    }

     public static class StorageFilesBundle {
        public final PackageTable packageTable;
        public final FlagTable flagTable;
        public final FlagValueList flagValueList;

        public StorageFilesBundle (PackageTable pTable, FlagTable fTable, FlagValueList fValueList) {
            this.packageTable = pTable;
            this.flagTable = fTable;
            this.flagValueList = fValueList;
        }
     }
}
