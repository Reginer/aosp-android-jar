/* GENERATED SOURCE. DO NOT MODIFY. */
/*
 * Copyright (C) 2022 The Libphonenumber Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.i18n.phonenumbers.metadata;

import com.android.i18n.phonenumbers.MetadataLoader;
import com.android.i18n.phonenumbers.metadata.init.ClassPathResourceMetadataLoader;
import com.android.i18n.phonenumbers.metadata.init.MetadataParser;
import com.android.i18n.phonenumbers.metadata.source.FormattingMetadataSource;
import com.android.i18n.phonenumbers.metadata.source.FormattingMetadataSourceImpl;
import com.android.i18n.phonenumbers.metadata.source.MetadataSource;
import com.android.i18n.phonenumbers.metadata.source.MetadataSourceImpl;
import com.android.i18n.phonenumbers.metadata.source.MultiFileModeFileNameProvider;
import com.android.i18n.phonenumbers.metadata.source.PhoneMetadataFileNameProvider;
import com.android.i18n.phonenumbers.metadata.source.RegionMetadataSource;
import com.android.i18n.phonenumbers.metadata.source.RegionMetadataSourceImpl;

/**
 * Provides metadata init and source dependencies when metadata is stored in multi-file mode and
 * loaded as a classpath resource.
 * @hide This class is not part of the Android public SDK API
 */
public final class DefaultMetadataDependenciesProvider {

  private static final DefaultMetadataDependenciesProvider INSTANCE = new DefaultMetadataDependenciesProvider();

  public static DefaultMetadataDependenciesProvider getInstance() {
    return INSTANCE;
  }

  private DefaultMetadataDependenciesProvider() {
  }

  private final MetadataParser metadataParser = MetadataParser.newLenientParser();
  private final MetadataLoader metadataLoader = new ClassPathResourceMetadataLoader();

  private final PhoneMetadataFileNameProvider phoneNumberMetadataFileNameProvider =
      new MultiFileModeFileNameProvider(
          "/com/android/i18n/phonenumbers/data/PhoneNumberMetadataProto");
  private final MetadataSource phoneNumberMetadataSource =
      new MetadataSourceImpl(
          phoneNumberMetadataFileNameProvider,
          metadataLoader,
          metadataParser);

  private final PhoneMetadataFileNameProvider shortNumberMetadataFileNameProvider =
      new MultiFileModeFileNameProvider(
          "/com/android/i18n/phonenumbers/data/ShortNumberMetadataProto");
  private final RegionMetadataSource shortNumberMetadataSource =
      new RegionMetadataSourceImpl(
          shortNumberMetadataFileNameProvider,
          metadataLoader,
          metadataParser);

  private final PhoneMetadataFileNameProvider alternateFormatsMetadataFileNameProvider =
      new MultiFileModeFileNameProvider(
          "/com/android/i18n/phonenumbers/data/PhoneNumberAlternateFormatsProto");
  private final FormattingMetadataSource alternateFormatsMetadataSource =
      new FormattingMetadataSourceImpl(
          alternateFormatsMetadataFileNameProvider,
          metadataLoader,
          metadataParser);

  public MetadataParser getMetadataParser() {
    return metadataParser;
  }

  public MetadataLoader getMetadataLoader() {
    return metadataLoader;
  }

  public PhoneMetadataFileNameProvider getPhoneNumberMetadataFileNameProvider() {
    return phoneNumberMetadataFileNameProvider;
  }

  public MetadataSource getPhoneNumberMetadataSource() {
    return phoneNumberMetadataSource;
  }

  public PhoneMetadataFileNameProvider getShortNumberMetadataFileNameProvider() {
    return shortNumberMetadataFileNameProvider;
  }

  public RegionMetadataSource getShortNumberMetadataSource() {
    return shortNumberMetadataSource;
  }

  public PhoneMetadataFileNameProvider getAlternateFormatsMetadataFileNameProvider() {
    return alternateFormatsMetadataFileNameProvider;
  }

  public FormattingMetadataSource getAlternateFormatsMetadataSource() {
    return alternateFormatsMetadataSource;
  }

  public String getCarrierDataDirectory() {
    return "/com/android/i18n/phonenumbers/carrier/data/";
  }

  public String getGeocodingDataDirectory() {
    return "/com/android/i18n/phonenumbers/geocoding/data/";
  }
}
