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

package android.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that this policy API can be set by multiple admins.
 *
 * <p>Starting from Android U ({@link Build.VERSION_CODES#UPSIDE_DOWN_CAKE}), multiple management
 * admins will be able to coexist on the same device (e.g. a financed device with an enterprise
 * admin). This requires adding multi-admin support for all the policy-related APIs (mostly in
 * {@link android.app.admin.DevicePolicyManager}). However, for Android U only a subset of
 * APIs will be supported, those will be marked with {@literal @}SupportsCoexistence annotation.
 * </p>
 *
 * Example:
 * <pre>{@code
 * {@literal @}SupportsCoexistence
 * public boolean setPermissionGrantState({@literal @}NonNull ComponentName admin,
 *          {@literal @}NonNull String packageName, {@literal @}NonNull String permission,
 *          {@literal @}PermissionGrantState int grantState) {
 *      throwIfParentInstance("setPermissionGrantState");
 * }</pre>
 *
 * @hide
 */

@Retention(SOURCE)
@Target(METHOD)
public @interface SupportsCoexistence {
}
