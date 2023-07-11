/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.car.setupwizardlib.util;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

/** Derived from {@code com.android.setupwizardlib/WizardManagerHelper.java} */
public final class CarWizardManagerHelper {
  public static final String EXTRA_WIZARD_BUNDLE = "wizardBundle";
  public static final String EXTRA_IS_FIRST_RUN = "firstRun";
  public static final String EXTRA_IS_DEALER = "dealer";
  public static final String EXTRA_IS_DEFERRED_SETUP = "deferredSetup";
  private static final String ACTION_NEXT = "com.android.wizard.NEXT";
  private static final String EXTRA_RESULT_CODE = "com.android.setupwizard.ResultCode";

  private CarWizardManagerHelper() {}

  /**
   * Get an intent that will invoke the next step of setup wizard.
   *
   * @param originalIntent The original intent that was used to start the step, usually via {@link
   *     android.app.Activity#getIntent()}.
   * @param resultCode The result code of the step. See {@link ResultCodes}.
   * @return A new intent that can be used with {@link
   *     android.app.Activity#startActivityForResult(Intent, int)} to start the next step of the
   *     setup flow.
   */
  public static Intent getNextIntent(Intent originalIntent, int resultCode) {
    return getNextIntent(originalIntent, resultCode, null);
  }

  /**
   * Get an intent that will invoke the next step of setup wizard.
   *
   * @param originalIntent The original intent that was used to start the step, usually via {@link
   *     android.app.Activity#getIntent()}.
   * @param resultCode The result code of the step. See {@link ResultCodes}.
   * @param data An intent containing extra result data.
   * @return A new intent that can be used with {@link
   *     android.app.Activity#startActivityForResult(Intent, int)} to start the next step of the
   *     setup flow.
   */
  public static Intent getNextIntent(Intent originalIntent, int resultCode, Intent data) {
    Intent intent = new Intent(ACTION_NEXT);
    copyWizardManagerExtras(originalIntent, intent);
    intent.putExtra(EXTRA_RESULT_CODE, resultCode);
    if (data != null && data.getExtras() != null) {
      intent.putExtras(data.getExtras());
    }

    return intent;
  }

  /**
   * Copy the internal extras used by setup wizard from one intent to another. For low-level use
   * only, such as when using {@link Intent#FLAG_ACTIVITY_FORWARD_RESULT} to relay to another
   * intent.
   *
   * @param srcIntent Intent to get the wizard manager extras from.
   * @param dstIntent Intent to copy the wizard manager extras to.
   */
  public static void copyWizardManagerExtras(Intent srcIntent, Intent dstIntent) {
    dstIntent.putExtra(EXTRA_WIZARD_BUNDLE, srcIntent.getBundleExtra(EXTRA_WIZARD_BUNDLE));
    dstIntent.putExtra(EXTRA_IS_FIRST_RUN, srcIntent.getBooleanExtra(EXTRA_IS_FIRST_RUN, false));
    dstIntent.putExtra(EXTRA_IS_DEALER, srcIntent.getBooleanExtra(EXTRA_IS_DEALER, false));
    dstIntent.putExtra(
        EXTRA_IS_DEFERRED_SETUP, srcIntent.getBooleanExtra(EXTRA_IS_DEFERRED_SETUP, false));
  }

  /**
   * Check whether an intent is intended to be used within the setup wizard flow.
   *
   * @param intent The intent to be checked, usually from {@link android.app.Activity#getIntent()}.
   * @return true if the intent passed in was intended to be used with setup wizard.
   */
  public static boolean isSetupWizardIntent(Intent intent) {
    return intent.getBooleanExtra(EXTRA_IS_FIRST_RUN, false);
  }

  /**
   * Checks whether an intent is running in the deferred setup wizard flow.
   *
   * @param intent The intent to be checked, usually from {@link android.app.Activity#getIntent()}.
   * @return true if the intent passed in was running in deferred setup wizard.
   */
  public static boolean isDeferredIntent(Intent intent) {
    return intent.getBooleanExtra(EXTRA_IS_DEFERRED_SETUP, false);
  }

  /**
   * Check whether an intent is intended for the dealer.
   *
   * @param intent The intent to be checked, usually from {@link android.app.Activity#getIntent()}.
   * @return true if the intent passed in was intended to be used with setup wizard.
   */
  public static boolean isDealerIntent(Intent intent) {
    return intent.getBooleanExtra(EXTRA_IS_DEALER, false);
  }

  /**
   * Checks whether the current user has completed Setup Wizard. This is true if the current user
   * has gone through Setup Wizard. The current user may or may not be the device owner and the
   * device owner may have already completed setup wizard.
   *
   * @param context The context to retrieve the settings.
   * @return true if the current user has completed Setup Wizard.
   * @see #isDeviceProvisioned(Context)
   */
  public static boolean isUserSetupComplete(Context context) {
    return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 0) == 1;
  }

  /**
   * Checks whether the device is provisioned. This means that the device has gone through Setup
   * Wizard at least once. Note that the user can still be in Setup Wizard even if this is true, for
   * a secondary user profile triggered through Settings > Add account.
   *
   * @param context The context to retrieve the settings.
   * @return true if the device is provisioned.
   * @see #isUserSetupComplete(Context)
   */
  public static boolean isDeviceProvisioned(Context context) {
    return Settings.Global.getInt(
            context.getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 0)
        == 1;
  }
  /**
   * Checks whether an intent is running in the initial setup wizard flow.
   *
   * @param intent The intent to be checked, usually from {@link android.app.Activity#getIntent()}
   * @return true if the intent passed in was intended to be used with setup wizard.
   */
  public static boolean isInitialSetupWizard(Intent intent) {
    return intent.getBooleanExtra(EXTRA_IS_FIRST_RUN, false);
  }

  /**
   * Checks whether an intent is running in the deferred setup wizard flow.
   *
   * @param originalIntent The original intent that was used to start the step, usually via {@link
   *     android.app.Activity#getIntent()}.
   * @return true if the intent passed in was running in deferred setup wizard.
   */
  public static boolean isDeferredSetupWizard(Intent originalIntent) {
    return originalIntent != null && originalIntent.getBooleanExtra(EXTRA_IS_DEFERRED_SETUP, false);
  }
}
