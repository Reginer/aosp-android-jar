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

package com.android.setupwizardlib.util;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.RuntimeEnvironment.application;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import com.android.setupwizardlib.R;
import com.android.setupwizardlib.robolectric.ExternalResources;
import com.android.setupwizardlib.robolectric.ExternalResources.Resources;
import com.android.setupwizardlib.util.Partner.ResourceEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK, Config.NEWEST_SDK})
public class PartnerTest {

  private static final String ACTION_PARTNER_CUSTOMIZATION =
      "com.android.setupwizard.action.PARTNER_CUSTOMIZATION";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    Partner.resetForTesting();
  }

  @Test
  public void get_withPartnerPackage_shouldReturnNonNull() {
    new PartnerPackageBuilder("foo.bar")
        .setIsSystem(false)
        .setDirectBootAware(true)
        .injectResources();
    new PartnerPackageBuilder("test.partner.package").setDirectBootAware(true).injectResources();

    Partner partner = Partner.get(application);
    assertThat(partner).isNotNull();
    assertThat(partner.getPackageName()).isEqualTo("test.partner.package");
  }

  @Test
  public void get_noPartnerPackage_shouldReturnNull() {
    Partner partner = Partner.get(application);
    assertThat(partner).isNull();
  }

  @Test
  public void get_nonSystemPartnerPackage_shouldIgnoreAndReturnNull() {
    new PartnerPackageBuilder("foo.bar")
        .setIsSystem(false)
        .setDirectBootAware(true)
        .injectResources();
    new PartnerPackageBuilder("test.partner.package")
        .setIsSystem(false)
        .setDirectBootAware(true)
        .injectResources();

    Partner partner = Partner.get(application);
    assertThat(partner).isNull();
  }

  @Test
  public void getResourceEntry_hasOverlay_shouldReturnOverlayValue() {
    new PartnerPackageBuilder("test.partner.package")
        .injectResources()
        .putInteger("suwTransitionDuration", 5000);

    ResourceEntry entry = Partner.getResourceEntry(application, R.integer.suwTransitionDuration);
    int partnerValue = entry.resources.getInteger(entry.id);
    assertThat(partnerValue).named("partner value").isEqualTo(5000);
    assertThat(entry.isOverlay).isTrue();
  }

  @Test
  public void getColor_partnerValuePresent_shouldReturnPartnerValue() {
    new PartnerPackageBuilder("test.partner.package")
        .injectResources()
        .putColor("suw_color_accent_dark", 0xffff00ff);

    final int color = Partner.getColor(application, R.color.suw_color_accent_dark);
    assertThat(color).isEqualTo(0xffff00ff);
  }

  @Test
  public void getText_partnerValuePresent_shouldReturnPartnerValue() {
    new PartnerPackageBuilder("test.partner.package")
        .injectResources()
        .putText("suw_next_button_label", "partner");

    final CharSequence partnerText = Partner.getText(application, R.string.suw_next_button_label);
    assertThat(partnerText.toString()).isEqualTo("partner");
  }

  @Test
  public void getResourceEntry_partnerValueNotPresent_shouldReturnDefault() {
    new PartnerPackageBuilder("test.partner.package").injectResources();

    ResourceEntry entry = Partner.getResourceEntry(application, R.color.suw_color_accent_dark);
    int partnerValue = entry.resources.getColor(entry.id);
    assertThat(partnerValue).isEqualTo(0xff448aff);
    assertThat(entry.isOverlay).isFalse();
  }

  @Test
  public void getResourceEntry_directBootUnawareNoValueDefined_shouldReturnDefaultValue() {
    new PartnerPackageBuilder("test.partner.package").injectResources();

    ResourceEntry entry = Partner.getResourceEntry(application, R.color.suw_color_accent_dark);
    int partnerValue = entry.resources.getColor(entry.id);
    assertThat(partnerValue).isEqualTo(0xff448aff);
    assertThat(entry.isOverlay).isFalse();
  }

  private static class PartnerPackageBuilder {
    private final String packageName;
    private final ResolveInfo resolveInfo;

    PartnerPackageBuilder(String packageName) {
      this.packageName = packageName;

      resolveInfo = new ResolveInfo();
      resolveInfo.resolvePackageName = packageName;
      ActivityInfo activityInfo = new ActivityInfo();
      ApplicationInfo appInfo = new ApplicationInfo();
      appInfo.flags = ApplicationInfo.FLAG_SYSTEM;
      appInfo.packageName = packageName;
      activityInfo.applicationInfo = appInfo;
      activityInfo.packageName = packageName;
      activityInfo.name = packageName;
      resolveInfo.activityInfo = activityInfo;
    }

    PartnerPackageBuilder setIsSystem(boolean isSystem) {
      if (isSystem) {
        resolveInfo.activityInfo.applicationInfo.flags |= ApplicationInfo.FLAG_SYSTEM;
      } else {
        resolveInfo.activityInfo.applicationInfo.flags &= ~ApplicationInfo.FLAG_SYSTEM;
      }
      return this;
    }

    PartnerPackageBuilder setDirectBootAware(boolean directBootAware) {
      if (VERSION.SDK_INT >= VERSION_CODES.N) {
        resolveInfo.activityInfo.directBootAware = directBootAware;
      }
      return this;
    }

    Resources injectResources() {
      shadowOf(application.getPackageManager())
          .addResolveInfoForIntent(new Intent(ACTION_PARTNER_CUSTOMIZATION), resolveInfo);
      return ExternalResources.injectExternalResources(packageName);
    }
  }
}
