/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.clockwork.healthservices;

import android.os.Build;

/** Various static definition and utility functions. */
class Utils {
  public static final boolean DEBUG_HAL = Build.IS_DEBUGGABLE;

  public static final int STARTING_FLUSH_ID = 0;

  /** Generates the next FlushID from the current flush ID. */
  public static int generateNextFlushId(int currentFlushId) {
    if (currentFlushId == Integer.MAX_VALUE) {
      // Wrap the flush ID back to 0 in the unlikely event we hit max int.
      return Utils.STARTING_FLUSH_ID;
    }
    return currentFlushId + 1;
  }
}
