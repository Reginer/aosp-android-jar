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

package android.adservices.customaudience;

/**
 * Represents the information necessary for a custom audience to participate in ad selection.
 * <p>
 * A custom audience is an abstract grouping of users with similar interests, as determined by an
 * advertiser.  This class is a collection of on-device data that is necessary to serve
 * advertisements targeting a single custom audience.
 *
 * {@hide}
 */
parcelable CustomAudience;
