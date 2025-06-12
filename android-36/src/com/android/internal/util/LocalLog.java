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

package com.android.internal.util;

import android.util.IndentingPrintWriter;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;

import java.util.ArrayList;

/**
 * Helper class for logging serious issues, which also keeps a small
 * snapshot of the logged events that can be printed later, such as part
 * of a system service's dumpsys output.
 * @hide
 */
// Exported to Mainline modules; cannot use annotations
// @android.ravenwood.annotation.RavenwoodKeepWholeClass
public class LocalLog {
    private final String mTag;
    private final int mMaxLines = 20;
    private final ArrayList<String> mLines = new ArrayList<String>(mMaxLines);

    public LocalLog(String tag) {
        mTag = tag;
    }

    public void w(String msg) {
        synchronized (mLines) {
            Slog.w(mTag, msg);
            if (mLines.size() >= mMaxLines) {
                mLines.remove(0);
            }
            mLines.add(msg);
        }
    }

    public boolean dump(IndentingPrintWriter pw, String header) {
        synchronized (mLines) {
            if (mLines.size() <= 0) {
                return false;
            }
            if (header != null) {
                pw.println(header);
                pw.increaseIndent();
            }
            for (int i=0; i<mLines.size(); i++) {
                pw.println(mLines.get(i));
            }
            if (header != null) {
                pw.decreaseIndent();
            }
            return true;
        }
    }

    public void dumpDebug(ProtoOutputStream proto, long fieldId) {
        final long token = proto.start(fieldId);

        synchronized (mLines) {
            for (int i = 0; i < mLines.size(); ++i) {
                proto.write(LocalLogProto.LINES, mLines.get(i));
            }
        }

        proto.end(token);
    }
}
