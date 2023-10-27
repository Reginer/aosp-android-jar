/*
 * Copyright (c) 2019, The Android Open Source Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     - Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     - Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     - Neither the name of The Android Open Source Project nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL MOTOROLA MOBILITY LLC BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package com.android.service.ims.presence;

import android.net.Uri;
import android.text.TextUtils;

public class PresenceUtils {

    public static final String LOG_TAG_PREFIX = "rcs_lib";

    private static final String TEL_SCHEME = "tel:";

    public static String toContactString(String[] contacts) {
        if(contacts == null) {
            return null;
        }

        String result = "";
        for(int i=0; i<contacts.length; i++) {
            result += contacts[i];
            if(i != contacts.length -1) {
                result += ";";
            }
        }

        return result;
    }

    public static Uri convertContactNumber(String number) {
        if (TextUtils.isEmpty(number)) return null;
        Uri possibleNumber = Uri.parse(number);
        // already in the  correct format
        if (TEL_SCHEME.equals(possibleNumber.getScheme())) {
            return possibleNumber;
        }
        // need to append tel scheme
        return Uri.fromParts(TEL_SCHEME, number, null);
    }

    public static String getNumber(Uri numberUri) {
        if (numberUri == null) return null;
        return numberUri.getSchemeSpecificPart();
    }
}
