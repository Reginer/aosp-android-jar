/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.webkit;

import android.content.Context;
import android.util.Log;

/**
 * Localized strings for the error codes defined in EventHandler.
 *
 * @hide
 */
class LegacyErrorStrings {
    private LegacyErrorStrings() { /* Utility class, don't instantiate. */ }

    private static final String LOGTAG = "Http";

    /**
     * Get the localized error message resource for the given error code.
     * If the code is unknown, we'll return a generic error message.
     */
    static String getString(int errorCode, Context context) {
        return context.getText(getResource(errorCode)).toString();
    }

    /**
     * Get the localized error message resource for the given error code.
     * If the code is unknown, we'll return a generic error message.
     */
    private static int getResource(int errorCode) {
        switch(errorCode) {
            case 0: /* EventHandler.OK: */
                return com.android.internal.R.string.httpErrorOk;

            case -1: /* EventHandler.ERROR: */
                return com.android.internal.R.string.httpError;

            case -2: /* EventHandler.ERROR_LOOKUP: */
                return com.android.internal.R.string.httpErrorLookup;

            case -3: /* EventHandler.ERROR_UNSUPPORTED_AUTH_SCHEME: */
                return com.android.internal.R.string.httpErrorUnsupportedAuthScheme;

            case -4: /* EventHandler.ERROR_AUTH: */
                return com.android.internal.R.string.httpErrorAuth;

            case -5: /* EventHandler.ERROR_PROXYAUTH: */
                return com.android.internal.R.string.httpErrorProxyAuth;

            case -6: /* EventHandler.ERROR_CONNECT: */
                return com.android.internal.R.string.httpErrorConnect;

            case -7: /* EventHandler.ERROR_IO: */
                return com.android.internal.R.string.httpErrorIO;

            case -8: /* EventHandler.ERROR_TIMEOUT: */
                return com.android.internal.R.string.httpErrorTimeout;

            case -9: /* EventHandler.ERROR_REDIRECT_LOOP: */
                return com.android.internal.R.string.httpErrorRedirectLoop;

            case -10: /* EventHandler.ERROR_UNSUPPORTED_SCHEME: */
                return com.android.internal.R.string.httpErrorUnsupportedScheme;

            case -11: /* EventHandler.ERROR_FAILED_SSL_HANDSHAKE: */
                return com.android.internal.R.string.httpErrorFailedSslHandshake;

            case -12: /* EventHandler.ERROR_BAD_URL: */
                return com.android.internal.R.string.httpErrorBadUrl;

            case -13: /* EventHandler.FILE_ERROR: */
                return com.android.internal.R.string.httpErrorFile;

            case -14: /* EventHandler.FILE_NOT_FOUND_ERROR: */
                return com.android.internal.R.string.httpErrorFileNotFound;

            case -15: /* EventHandler.TOO_MANY_REQUESTS_ERROR: */
                return com.android.internal.R.string.httpErrorTooManyRequests;

            default:
                Log.w(LOGTAG, "Using generic message for unknown error code: " + errorCode);
                return com.android.internal.R.string.httpError;
        }
    }
}
