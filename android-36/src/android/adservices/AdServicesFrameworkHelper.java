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

package android.adservices;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * This class specifies the state of the APIs exposed by AdServicesApi apk.
 *
 * @hide
 */
// TODO(b/378164580) move it to shared library
public final class AdServicesFrameworkHelper {
    private AdServicesFrameworkHelper() {}

    /** Returns the stacktrace string of the exception. */
    public static String getExceptionStackTraceString(Throwable e) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        String message = e.getMessage();
        e.printStackTrace(printWriter);
        printWriter.flush();

        String stackTrace = writer.toString();
        return message + stackTrace;
    }
}
