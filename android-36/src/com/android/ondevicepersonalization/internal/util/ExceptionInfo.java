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

package com.android.ondevicepersonalization.internal.util;

import android.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Information about an exception chain in the ODP service or an IsolatedService.
 * @hide
 */
public class ExceptionInfo implements Serializable {
    /** @hide */
    static class ExceptionInfoElement implements Serializable {
        String mExceptionClass;
        String mMessage;
        StackTraceElement[] mStackTrace;

        ExceptionInfoElement(@NonNull Throwable t) {
            Objects.requireNonNull(t);
            mExceptionClass = t.getClass().getName();
            mMessage = t.getMessage();
            mStackTrace = t.getStackTrace();
        }
    }

    private ArrayList<ExceptionInfoElement> mElements;

    ExceptionInfo(Throwable t, int maxDepth) {
        Objects.requireNonNull(t);
        mElements = new ArrayList<>();
        int count = 0;
        while (t != null && count < maxDepth) {
            mElements.add(new ExceptionInfoElement(t));
            t = t.getCause();
            ++count;
        }
    }

    /** Serialize to byte array. */
    public static byte[] toByteArray(Throwable t, int maxDepth) {
        if (t == null) {
            return null;
        }
        try {
            ExceptionInfo info = new ExceptionInfo(t, maxDepth);
            try (ByteArrayOutputStream bs = new ByteArrayOutputStream();
                    ObjectOutputStream os = new ObjectOutputStream(bs)) {
                os.writeObject(info);
                return bs.toByteArray();
            }
        } catch (Exception e) {
            return null;
        }
    }

    /** Deserialize from byte array. */
    public static Exception fromByteArray(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try (ByteArrayInputStream bs = new ByteArrayInputStream(bytes);
                ObjectInputStream os = new ObjectInputStream(bs)) {
            ExceptionInfo info = (ExceptionInfo) os.readObject();
            return info.toException();
        } catch (Exception e) {
            return null;
        }
    }

    Exception toException() {
        try {
            Exception e = null;
            for (int i = mElements.size() - 1; i >= 0; --i) {
                ExceptionInfoElement element = mElements.get(i);
                Exception tmp = new Exception(element.mExceptionClass + ": " + element.mMessage, e);
                tmp.setStackTrace(element.mStackTrace);
                e = tmp;
            }
            return e;
        } catch (Exception e) {
            return null;
        }
    }
}
