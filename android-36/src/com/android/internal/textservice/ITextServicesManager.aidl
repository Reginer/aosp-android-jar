/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.internal.textservice;

import com.android.internal.textservice.ISpellCheckerSessionListener;
import com.android.internal.textservice.ITextServicesSessionListener;

import android.content.ComponentName;
import android.os.Bundle;
import android.view.textservice.SpellCheckerInfo;
import android.view.textservice.SpellCheckerSubtype;

/**
 * Interface to the text service manager.
 * @hide
 */
interface ITextServicesManager {
    SpellCheckerInfo getCurrentSpellChecker(int userId, String locale);
    SpellCheckerSubtype getCurrentSpellCheckerSubtype(int userId,
            boolean allowImplicitlySelectedSubtype);
    oneway void getSpellCheckerService(int userId, String sciId, in String locale,
            in ITextServicesSessionListener tsListener,
            in ISpellCheckerSessionListener scListener, in Bundle bundle, int supportedAttributes);
    oneway void finishSpellCheckerService(int userId, in ISpellCheckerSessionListener listener);
    boolean isSpellCheckerEnabled(int userId);
    SpellCheckerInfo[] getEnabledSpellCheckers(int userId);
}
