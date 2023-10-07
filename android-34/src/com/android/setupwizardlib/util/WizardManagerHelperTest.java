/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.setupwizardlib.util;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.robolectric.RuntimeEnvironment.application;
import static org.robolectric.Shadows.shadowOf;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import androidx.annotation.StyleRes;
import com.android.setupwizardlib.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Config.NEWEST_SDK)
public class WizardManagerHelperTest {

  @Test
  public void testGetNextIntent() {
    final Intent intent = new Intent("test.intent.ACTION");
    intent.putExtra("scriptUri", "android-resource://test-script");
    intent.putExtra("actionId", "test_action_id");
    intent.putExtra("theme", "test_theme");
    intent.putExtra("ignoreExtra", "poof"); // extra is ignored because it's not known

    final Intent data = new Intent();
    data.putExtra("extraData", "shazam");

    final Intent nextIntent = WizardManagerHelper.getNextIntent(intent, Activity.RESULT_OK, data);
    assertWithMessage("Next intent action should be NEXT")
        .that(nextIntent.getAction())
        .isEqualTo("com.android.wizard.NEXT");
    assertWithMessage("Script URI should be the same as original intent")
        .that(nextIntent.getStringExtra("scriptUri"))
        .isEqualTo("android-resource://test-script");
    assertWithMessage("Action ID should be the same as original intent")
        .that(nextIntent.getStringExtra("actionId"))
        .isEqualTo("test_action_id");
    assertWithMessage("Theme extra should be the same as original intent")
        .that(nextIntent.getStringExtra("theme"))
        .isEqualTo("test_theme");
    assertWithMessage("ignoreExtra should not be in nextIntent")
        .that(nextIntent.hasExtra("ignoreExtra"))
        .isFalse();
    assertWithMessage("Result code extra should be RESULT_OK")
        .that(nextIntent.getIntExtra("com.android.setupwizard.ResultCode", 0))
        .isEqualTo(Activity.RESULT_OK);
    assertWithMessage("Extra data should surface as extra in nextIntent")
        .that(nextIntent.getStringExtra("extraData"))
        .isEqualTo("shazam");
  }

  @Test
  public void testIsSetupWizardTrue() {
    final Intent intent = new Intent();
    intent.putExtra("firstRun", true);
    assertWithMessage("Is setup wizard should be true")
        .that(WizardManagerHelper.isSetupWizardIntent(intent))
        .isTrue();
  }

  @Test
  public void testIsDeferredSetupTrue() {
    final Intent intent = new Intent();
    intent.putExtra("deferredSetup", true);
    assertWithMessage("Is deferred setup wizard should be true")
        .that(WizardManagerHelper.isDeferredSetupWizard(intent))
        .isTrue();
  }

  @Test
  public void testIsPreDeferredSetupTrue() {
    final Intent intent = new Intent();
    intent.putExtra("preDeferredSetup", true);
    assertWithMessage("Is pre-deferred setup wizard should be true")
        .that(WizardManagerHelper.isPreDeferredSetupWizard(intent))
        .isTrue();
  }

  @Test
  public void testIsSetupWizardFalse() {
    final Intent intent = new Intent();
    intent.putExtra("firstRun", false);
    assertWithMessage("Is setup wizard should be true")
        .that(WizardManagerHelper.isSetupWizardIntent(intent))
        .isFalse();
  }

  @Test
  public void isLightTheme_shouldReturnTrue_whenThemeIsLight() {
    List<String> lightThemes =
        Arrays.asList(
            "holo_light", "material_light", "glif_light", "glif_v2_light", "glif_v3_light");
    ArrayList<String> unexpectedIntentThemes = new ArrayList<>();
    ArrayList<String> unexpectedStringThemes = new ArrayList<>();
    for (final String theme : lightThemes) {
      Intent intent = new Intent();
      intent.putExtra(WizardManagerHelper.EXTRA_THEME, theme);
      if (!WizardManagerHelper.isLightTheme(intent, false)) {
        unexpectedIntentThemes.add(theme);
      }
      if (!WizardManagerHelper.isLightTheme(theme, false)) {
        unexpectedStringThemes.add(theme);
      }
    }
    assertWithMessage("Intent themes " + unexpectedIntentThemes + " should be light")
        .that(unexpectedIntentThemes.isEmpty())
        .isTrue();
    assertWithMessage("String themes " + unexpectedStringThemes + " should be light")
        .that(unexpectedStringThemes.isEmpty())
        .isTrue();
  }

  @Test
  public void isLightTheme_shouldReturnFalse_whenThemeIsNotLight() {
    List<String> lightThemes = Arrays.asList("holo", "material", "glif", "glif_v2", "glif_v3");
    ArrayList<String> unexpectedIntentThemes = new ArrayList<>();
    ArrayList<String> unexpectedStringThemes = new ArrayList<>();
    for (final String theme : lightThemes) {
      Intent intent = new Intent();
      intent.putExtra(WizardManagerHelper.EXTRA_THEME, theme);
      if (WizardManagerHelper.isLightTheme(intent, true)) {
        unexpectedIntentThemes.add(theme);
      }
      if (WizardManagerHelper.isLightTheme(theme, true)) {
        unexpectedStringThemes.add(theme);
      }
    }
    assertWithMessage("Intent themes " + unexpectedIntentThemes + " should not be light")
        .that(unexpectedIntentThemes.isEmpty())
        .isTrue();
    assertWithMessage("String themes " + unexpectedStringThemes + " should not be light")
        .that(unexpectedStringThemes.isEmpty())
        .isTrue();
  }

  @Test
  public void getThemeRes_whenOldestSupportedThemeTakeEffect_shouldReturnDefault() {
    Intent intent = new Intent();
    intent.putExtra(WizardManagerHelper.EXTRA_THEME, "material");
    assertThat(WizardManagerHelper.getThemeRes(intent, 0, WizardManagerHelper.THEME_GLIF_V2))
        .isEqualTo(0);
  }

  @Test
  public void getThemeRes_whenOldestSupportedThemeNotTakeEffect_shouldReturnCurrent() {
    Intent intent = new Intent();
    intent.putExtra(WizardManagerHelper.EXTRA_THEME, "glif_v3");
    assertThat(WizardManagerHelper.getThemeRes(intent, 0, WizardManagerHelper.THEME_GLIF_V2))
        .isEqualTo(R.style.SuwThemeGlifV3);
  }

  @Test
  public void testIsLightThemeDefault() {
    final Intent intent = new Intent();
    intent.putExtra("theme", "abracadabra");
    assertWithMessage("isLightTheme should return default value true")
        .that(WizardManagerHelper.isLightTheme(intent, true))
        .isTrue();
    assertWithMessage("isLightTheme should return default value false")
        .that(WizardManagerHelper.isLightTheme(intent, false))
        .isFalse();
  }

  @Test
  public void testIsLightThemeUnspecified() {
    final Intent intent = new Intent();
    assertWithMessage("isLightTheme should return default value true")
        .that(WizardManagerHelper.isLightTheme(intent, true))
        .isTrue();
    assertWithMessage("isLightTheme should return default value false")
        .that(WizardManagerHelper.isLightTheme(intent, false))
        .isFalse();
  }

  @Test
  public void testGetThemeResGlifV3Light() {
    assertThat(WizardManagerHelper.getThemeRes("glif_v3_light", 0))
        .isEqualTo(R.style.SuwThemeGlifV3_Light);
  }

  @Test
  public void testGetThemeResGlifV3() {
    assertThat(WizardManagerHelper.getThemeRes("glif_v3", 0)).isEqualTo(R.style.SuwThemeGlifV3);
  }

  @Test
  public void testGetThemeResGlifV2Light() {
    assertThat(WizardManagerHelper.getThemeRes("glif_v2_light", 0))
        .isEqualTo(R.style.SuwThemeGlifV2_Light);
  }

  @Test
  public void testGetThemeResGlifV2() {
    assertThat(WizardManagerHelper.getThemeRes("glif_v2", 0)).isEqualTo(R.style.SuwThemeGlifV2);
  }

  @Test
  public void testGetThemeResGlifLight() {
    assertThat(WizardManagerHelper.getThemeRes("glif_light", 0))
        .isEqualTo(R.style.SuwThemeGlif_Light);
  }

  @Test
  public void testGetThemeResGlif() {
    assertThat(WizardManagerHelper.getThemeRes("glif", 0)).isEqualTo(R.style.SuwThemeGlif);
  }

  @Test
  public void testGetThemeResMaterialLight() {
    assertThat(WizardManagerHelper.getThemeRes("material_light", 0))
        .isEqualTo(R.style.SuwThemeMaterial_Light);
  }

  @Test
  public void testGetThemeResMaterial() {
    assertThat(WizardManagerHelper.getThemeRes("material", 0)).isEqualTo(R.style.SuwThemeMaterial);
  }

  @Test
  public void testGetThemeResDefault() {
    @StyleRes int def = 123;
    assertThat(WizardManagerHelper.getThemeRes("abracadabra", def)).isEqualTo(def);
  }

  @Test
  public void testGetThemeResNull() {
    @StyleRes int def = 123;
    assertThat(WizardManagerHelper.getThemeRes((String) null, def)).isEqualTo(def);
  }

  @Test
  public void testGetThemeResFromIntent() {
    Intent intent = new Intent();
    intent.putExtra(WizardManagerHelper.EXTRA_THEME, "material");
    assertThat(WizardManagerHelper.getThemeRes(intent, 0)).isEqualTo(R.style.SuwThemeMaterial);
  }

  @Test
  public void testCopyWizardManagerIntent() {
    Bundle wizardBundle = new Bundle();
    wizardBundle.putString("foo", "bar");
    Intent originalIntent =
        new Intent()
            .putExtra(WizardManagerHelper.EXTRA_THEME, "test_theme")
            .putExtra(WizardManagerHelper.EXTRA_WIZARD_BUNDLE, wizardBundle)
            .putExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, true)
            .putExtra(WizardManagerHelper.EXTRA_IS_DEFERRED_SETUP, true)
            .putExtra(WizardManagerHelper.EXTRA_IS_PRE_DEFERRED_SETUP, true)
            // Script URI and Action ID are kept for backwards compatibility
            .putExtra(WizardManagerHelper.EXTRA_SCRIPT_URI, "test_script_uri")
            .putExtra(WizardManagerHelper.EXTRA_ACTION_ID, "test_action_id");

    Intent intent = new Intent("test.intent.action");
    WizardManagerHelper.copyWizardManagerExtras(originalIntent, intent);

    assertWithMessage("Intent action should be kept")
        .that(intent.getAction())
        .isEqualTo("test.intent.action");
    assertWithMessage("EXTRA_THEME should be copied")
        .that(intent.getStringExtra(WizardManagerHelper.EXTRA_THEME))
        .isEqualTo("test_theme");
    Bundle copiedWizardBundle = intent.getParcelableExtra(WizardManagerHelper.EXTRA_WIZARD_BUNDLE);
    assertWithMessage("Wizard bundle should be copied")
        .that(copiedWizardBundle.getString("foo"))
        .isEqualTo("bar");

    assertWithMessage("EXTRA_IS_FIRST_RUN should be copied")
        .that(intent.getBooleanExtra(WizardManagerHelper.EXTRA_IS_FIRST_RUN, false))
        .isTrue();
    assertWithMessage("EXTRA_IS_DEFERRED_SETUP should be copied")
        .that(intent.getBooleanExtra(WizardManagerHelper.EXTRA_IS_DEFERRED_SETUP, false))
        .isTrue();
    assertWithMessage("EXTRA_IS_PRE_DEFERRED_SETUP should be copied")
        .that(intent.getBooleanExtra(WizardManagerHelper.EXTRA_IS_PRE_DEFERRED_SETUP, false))
        .isTrue();

    // Script URI and Action ID are replaced by Wizard Bundle in M, but are kept for backwards
    // compatibility
    assertWithMessage("EXTRA_SCRIPT_URI should be copied")
        .that(intent.getStringExtra(WizardManagerHelper.EXTRA_SCRIPT_URI))
        .isEqualTo("test_script_uri");
    assertWithMessage("EXTRA_ACTION_ID should be copied")
        .that(intent.getStringExtra(WizardManagerHelper.EXTRA_ACTION_ID))
        .isEqualTo("test_action_id");
  }

  @TargetApi(VERSION_CODES.JELLY_BEAN_MR1)
  @Test
  public void testIsUserSetupComplete() {
    Settings.Global.putInt(application.getContentResolver(), Global.DEVICE_PROVISIONED, 1);
    Settings.Secure.putInt(application.getContentResolver(), "user_setup_complete", 1);
    assertThat(WizardManagerHelper.isUserSetupComplete(application)).isTrue();

    Settings.Secure.putInt(application.getContentResolver(), "user_setup_complete", 0);
    assertThat(WizardManagerHelper.isUserSetupComplete(application)).isFalse();
  }

  @Test
  @Config(sdk = VERSION_CODES.JELLY_BEAN)
  public void testIsUserSetupCompleteCompat() {
    Settings.Secure.putInt(application.getContentResolver(), Secure.DEVICE_PROVISIONED, 1);
    assertThat(WizardManagerHelper.isUserSetupComplete(application)).isTrue();

    Settings.Secure.putInt(application.getContentResolver(), Secure.DEVICE_PROVISIONED, 0);
    assertThat(WizardManagerHelper.isUserSetupComplete(application)).isFalse();
  }

  @TargetApi(VERSION_CODES.JELLY_BEAN_MR1)
  @Test
  public void testIsDeviceProvisioned() {
    Settings.Global.putInt(application.getContentResolver(), Global.DEVICE_PROVISIONED, 1);
    assertThat(WizardManagerHelper.isDeviceProvisioned(application)).isTrue();
    Settings.Global.putInt(application.getContentResolver(), Global.DEVICE_PROVISIONED, 0);
    assertThat(WizardManagerHelper.isDeviceProvisioned(application)).isFalse();
  }

  @Test
  @Config(sdk = VERSION_CODES.JELLY_BEAN)
  public void testIsDeviceProvisionedCompat() {
    Settings.Secure.putInt(application.getContentResolver(), Secure.DEVICE_PROVISIONED, 1);
    assertThat(WizardManagerHelper.isDeviceProvisioned(application)).isTrue();
    Settings.Secure.putInt(application.getContentResolver(), Secure.DEVICE_PROVISIONED, 0);
    assertThat(WizardManagerHelper.isDeviceProvisioned(application)).isFalse();
  }

  @Test
  public void applyTheme_glifDayNight_shouldApplyThemeToActivity() {
    Activity activity =
        Robolectric.buildActivity(
                Activity.class,
                new Intent()
                    .putExtra(
                        WizardManagerHelper.EXTRA_THEME, WizardManagerHelper.THEME_GLIF_LIGHT))
            .setup()
            .get();

    WizardManagerHelper.applyTheme(activity);

    assertThat(shadowOf(activity).callGetThemeResId()).isEqualTo(R.style.SuwThemeGlif_DayNight);
  }
}
