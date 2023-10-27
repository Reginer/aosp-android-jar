// Copyright 2016 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.net.impl;

import android.net.http.HttpException;

/**
 * Implements {@link HttpException}.
 */
public class CronetExceptionImpl extends HttpException {
    public CronetExceptionImpl(String message, Throwable cause) {
        super(message, cause);
    }
}
