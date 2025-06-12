// Copyright 2024 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.base.memory;

/** self freeze callback interface. */
@FunctionalInterface
public interface SelfFreezeCallback {
    public void onSelfFreeze();
}
