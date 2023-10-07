/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.clockwork.displayoffload;

import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.RemoteException;

public class FakeHwBinder implements IHwBinder {
    @Override
    public void transact(int code, HwParcel request, android.os.HwParcel reply, int flags)
            throws RemoteException {}

    @Override
    public IHwInterface queryLocalInterface(String descriptor) {
        return null;
    }

    @Override
    public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
        return false;
    }

    @Override
    public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
        return false;
    }
}
