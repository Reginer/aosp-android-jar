/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.setupwizardlib.robolectric;

import static org.robolectric.RuntimeEnvironment.application;
import static org.robolectric.Shadows.shadowOf;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.DisplayMetrics;
import java.util.HashMap;
import java.util.Map;
import org.robolectric.res.ResName;
import org.robolectric.res.ResType;
import org.robolectric.res.TypedResource;
import org.robolectric.shadows.ShadowPackageManager;

/**
 * Utility class to inject resources for an "external" application in Robolectric tests. This can be
 * used with {@link org.robolectric.shadows.ShadowPackageManager#resources} to simulate loading
 * resources from another package.
 */
public final class ExternalResources {

  public static Resources injectExternalResources(String packageName) {
    return injectExternalResources(createPackageInfo(packageName));
  }

  public static Resources injectExternalResources(PackageInfo packageInfo) {
    try {
      application.getPackageManager().getPackageInfo(packageInfo.packageName, 0);
    } catch (NameNotFoundException e) {
      // Add the package if it does not exist
      shadowOf(application.getPackageManager()).addPackage(packageInfo);
    }
    Resources resources = Resources.forPackageName(packageInfo.packageName);
    ShadowPackageManager.resources.put(packageInfo.packageName, resources);
    return resources;
  }

  /**
   * Constructed resources for testing, representing resources external to the current package under
   * test.
   */
  public static class Resources extends android.content.res.Resources {

    private final String packageName;

    public static Resources forPackageName(String packageName) {
      android.content.res.Resources res = application.getResources();
      return new Resources(
          packageName, res.getAssets(), res.getDisplayMetrics(), res.getConfiguration());
    }

    private Resources(
        String packageName, AssetManager assets, DisplayMetrics metrics, Configuration config) {
      super(assets, metrics, config);
      this.packageName = packageName;
    }

    @Override
    public int getIdentifier(String name, String defType, String defPackage) {
      Integer resourceId = resourceIds.get(ResName.qualifyResName(name, defPackage, defType));
      if (resourceId == null) {
        return 0;
      }
      return resourceId;
    }

    @Override
    public int getInteger(int id) {
      return (int) get(id, ResType.INTEGER);
    }

    public void putInteger(String name, int value) {
      put(
          ResName.qualifyResName(name, packageName, "integer"),
          new TypedResource<>(value, ResType.INTEGER, null));
    }

    @Override
    public int getColor(int id) {
      return (int) get(id, ResType.COLOR);
    }

    @Override
    public int getColor(int id, @Nullable Theme theme) {
      return (int) get(id, ResType.COLOR);
    }

    public void putColor(String name, int value) {
      put(
          ResName.qualifyResName(name, packageName, "color"),
          new TypedResource<>(value, ResType.COLOR, null));
    }

    @NonNull
    @Override
    public CharSequence getText(int id) {
      return (CharSequence) get(id, ResType.CHAR_SEQUENCE);
    }

    @NonNull
    @Override
    public String getString(int id) {
      return get(id, ResType.CHAR_SEQUENCE).toString();
    }

    public void putText(String name, CharSequence value) {
      put(
          ResName.qualifyResName(name, packageName, "string"),
          new TypedResource<>(value, ResType.CHAR_SEQUENCE, null));
    }

    private final Map<Integer, TypedResource<?>> overrideResources = new HashMap<>();
    private final Map<ResName, Integer> resourceIds = new HashMap<>();
    private int nextId = 1;

    private <T> void put(ResName resName, TypedResource<T> value) {
      int id = nextId++;
      overrideResources.put(id, value);
      resourceIds.put(resName, id);
    }

    private Object get(@AnyRes int id, ResType type) {
      TypedResource<?> override = overrideResources.get(id);
      if (override != null && override.getResType() == type) {
        return override.getData();
      }
      throw new NotFoundException();
    }
  }

  private static PackageInfo createPackageInfo(String packageName) {
    PackageInfo packageInfo = new PackageInfo();
    packageInfo.packageName = packageName;
    return packageInfo;
  }

  private ExternalResources() {}
}
