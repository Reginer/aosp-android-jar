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

package android.net.http;

import org.chromium.net.CronetException;

import java.io.IOException;

public class CronetExceptionTranslationUtils {
    private CronetExceptionTranslationUtils() {}

    public interface CronetRunnable<T> {
        T run() throws IOException;
    }

    public static <T> T executeTranslatingExceptions(
            CronetRunnable<T> work) throws IOException {
        try {
            return work.run();
        } catch (CronetException e) {
            throw translateException(e);
        }
    }

    public static HttpException translateException(CronetException e) {
        if (e instanceof org.chromium.net.QuicException) {
            return new QuicExceptionWrapper((org.chromium.net.QuicException) e);
        }

        if (e instanceof org.chromium.net.NetworkException) {
            return new NetworkExceptionWrapper((org.chromium.net.NetworkException) e);
        }

        if (e instanceof org.chromium.net.CallbackException) {
            return new CallbackExceptionWrapper((org.chromium.net.CallbackException) e);
        }

        return new CronetExceptionWrapper(e);
    }

    public static Throwable maybeTranslateException(Throwable t) {
        if (t instanceof org.chromium.net.InlineExecutionProhibitedException) {
            // InlineExecutionProhibitedException is final, so we can't wrap it.
            android.net.http.InlineExecutionProhibitedException translatedException =
                new android.net.http.InlineExecutionProhibitedException();
            translatedException.initCause(t);
            return translatedException;
        }

        return t;
   }
}
