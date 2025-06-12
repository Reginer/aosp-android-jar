/**
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

package com.android.server.broadcastradio.hal2;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.hardware.broadcastradio.V2_0.AmFmBandRange;
import android.hardware.broadcastradio.V2_0.AmFmRegionConfig;
import android.hardware.broadcastradio.V2_0.Announcement;
import android.hardware.broadcastradio.V2_0.DabTableEntry;
import android.hardware.broadcastradio.V2_0.IdentifierType;
import android.hardware.broadcastradio.V2_0.Metadata;
import android.hardware.broadcastradio.V2_0.MetadataKey;
import android.hardware.broadcastradio.V2_0.ProgramFilter;
import android.hardware.broadcastradio.V2_0.ProgramIdentifier;
import android.hardware.broadcastradio.V2_0.ProgramInfo;
import android.hardware.broadcastradio.V2_0.Properties;
import android.hardware.broadcastradio.V2_0.Result;
import android.hardware.broadcastradio.V2_0.VendorKeyValue;
import android.hardware.radio.ProgramList;
import android.hardware.radio.ProgramSelector;
import android.hardware.radio.RadioManager;
import android.hardware.radio.RadioMetadata;
import android.hardware.radio.RadioTuner;
import android.os.ParcelableException;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;

import com.android.server.utils.Slogf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

final class Convert {

    private static final String TAG = "BcRadio2Srv.convert";

    private Convert() {
        throw new UnsupportedOperationException("Convert class is noninstantiable");
    }

    static void throwOnError(String action, int result) {
        String errorString = action + ": " + Result.toString(result);
        switch (result) {
            case Result.OK:
                return;
            case Result.UNKNOWN_ERROR:
            case Result.INTERNAL_ERROR:
            case Result.TIMEOUT:
                throw new ParcelableException(new RuntimeException(errorString));
            case Result.INVALID_ARGUMENTS:
                throw new IllegalArgumentException(errorString);
            case Result.INVALID_STATE:
                throw new IllegalStateException(errorString);
            case Result.NOT_SUPPORTED:
                throw new UnsupportedOperationException(errorString);
            default:
                throw new ParcelableException(new RuntimeException(
                        action + ": unknown error (" + result + ")"));
        }
    }

    @RadioTuner.TunerResultType
    static int halResultToTunerResult(int result) {
        switch (result) {
            case Result.OK:
                return RadioTuner.TUNER_RESULT_OK;
            case Result.INTERNAL_ERROR:
                return RadioTuner.TUNER_RESULT_INTERNAL_ERROR;
            case Result.INVALID_ARGUMENTS:
                return RadioTuner.TUNER_RESULT_INVALID_ARGUMENTS;
            case Result.INVALID_STATE:
                return RadioTuner.TUNER_RESULT_INVALID_STATE;
            case Result.NOT_SUPPORTED:
                return RadioTuner.TUNER_RESULT_NOT_SUPPORTED;
            case Result.TIMEOUT:
                return RadioTuner.TUNER_RESULT_TIMEOUT;
            case Result.UNKNOWN_ERROR:
            default:
                return RadioTuner.TUNER_RESULT_UNKNOWN_ERROR;
        }
    }

    static @NonNull ArrayList<VendorKeyValue>
    vendorInfoToHal(@Nullable Map<String, String> info) {
        if (info == null) return new ArrayList<>();

        ArrayList<VendorKeyValue> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : info.entrySet()) {
            VendorKeyValue elem = new VendorKeyValue();
            elem.key = entry.getKey();
            elem.value = entry.getValue();
            if (elem.key == null || elem.value == null) {
                Slogf.w(TAG, "VendorKeyValue contains null pointers");
                continue;
            }
            list.add(elem);
        }

        return list;
    }

    static @NonNull Map<String, String> vendorInfoFromHal(@Nullable List<VendorKeyValue> info) {
        Map<String, String> vendorInfoMap = new ArrayMap<>();
        if (info == null) {
            return vendorInfoMap;
        }

        for (VendorKeyValue kvp : info) {
            if (kvp.key == null || kvp.value == null) {
                Slogf.w(TAG, "VendorKeyValue contains null pointers");
                continue;
            }
            vendorInfoMap.put(kvp.key, kvp.value);
        }

        return vendorInfoMap;
    }

    private static @ProgramSelector.ProgramType int identifierTypeToProgramType(
            @ProgramSelector.IdentifierType int idType) {
        switch (idType) {
            case ProgramSelector.IDENTIFIER_TYPE_AMFM_FREQUENCY:
            case ProgramSelector.IDENTIFIER_TYPE_RDS_PI:
                // TODO(b/69958423): verify AM/FM with frequency range
                return ProgramSelector.PROGRAM_TYPE_FM;
            case ProgramSelector.IDENTIFIER_TYPE_HD_STATION_ID_EXT:
                // TODO(b/69958423): verify AM/FM with frequency range
                return ProgramSelector.PROGRAM_TYPE_FM_HD;
            case ProgramSelector.IDENTIFIER_TYPE_DAB_SIDECC:
            case ProgramSelector.IDENTIFIER_TYPE_DAB_ENSEMBLE:
            case ProgramSelector.IDENTIFIER_TYPE_DAB_SCID:
            case ProgramSelector.IDENTIFIER_TYPE_DAB_FREQUENCY:
            case ProgramSelector.IDENTIFIER_TYPE_DAB_DMB_SID_EXT:
                return ProgramSelector.PROGRAM_TYPE_DAB;
            case ProgramSelector.IDENTIFIER_TYPE_DRMO_SERVICE_ID:
            case ProgramSelector.IDENTIFIER_TYPE_DRMO_FREQUENCY:
                return ProgramSelector.PROGRAM_TYPE_DRMO;
            case ProgramSelector.IDENTIFIER_TYPE_SXM_SERVICE_ID:
            case ProgramSelector.IDENTIFIER_TYPE_SXM_CHANNEL:
                return ProgramSelector.PROGRAM_TYPE_SXM;
        }
        if (idType >= ProgramSelector.IDENTIFIER_TYPE_VENDOR_PRIMARY_START
                && idType <= ProgramSelector.IDENTIFIER_TYPE_VENDOR_PRIMARY_END) {
            return idType;
        }
        return ProgramSelector.PROGRAM_TYPE_INVALID;
    }

    private static @NonNull int[]
    identifierTypesToProgramTypes(@NonNull int[] idTypes) {
        Set<Integer> pTypes = new ArraySet<>();

        for (int idType : idTypes) {
            int pType = identifierTypeToProgramType(idType);

            if (pType == ProgramSelector.PROGRAM_TYPE_INVALID) continue;

            pTypes.add(pType);
            if (pType == ProgramSelector.PROGRAM_TYPE_FM) {
                // TODO(b/69958423): verify AM/FM with region info
                pTypes.add(ProgramSelector.PROGRAM_TYPE_AM);
            }
            if (pType == ProgramSelector.PROGRAM_TYPE_FM_HD) {
                // TODO(b/69958423): verify AM/FM with region info
                pTypes.add(ProgramSelector.PROGRAM_TYPE_AM_HD);
            }
        }

        return pTypes.stream().mapToInt(Integer::intValue).toArray();
    }

    private static @NonNull RadioManager.BandDescriptor[]
    amfmConfigToBands(@Nullable AmFmRegionConfig config) {
        if (config == null) return new RadioManager.BandDescriptor[0];

        int len = config.ranges.size();
        List<RadioManager.BandDescriptor> bands = new ArrayList<>(len);

        // Just a placeholder value.
        int region = RadioManager.REGION_ITU_1;

        for (AmFmBandRange range : config.ranges) {
            FrequencyBand bandType = Utils.getBand(range.lowerBound);
            if (bandType == FrequencyBand.UNKNOWN) {
                Slogf.e(TAG, "Unknown frequency band at " + range.lowerBound + "kHz");
                continue;
            }
            if (bandType == FrequencyBand.FM) {
                bands.add(new RadioManager.FmBandDescriptor(region, RadioManager.BAND_FM,
                    range.lowerBound, range.upperBound, range.spacing,

                    // TODO(b/69958777): stereo, rds, ta, af, ea
                    true, true, true, true, true
                ));
            } else {  // AM
                bands.add(new RadioManager.AmBandDescriptor(region, RadioManager.BAND_AM,
                    range.lowerBound, range.upperBound, range.spacing,

                    // TODO(b/69958777): stereo
                    true
                ));
            }
        }

        return bands.toArray(new RadioManager.BandDescriptor[bands.size()]);
    }

    private static @Nullable Map<String, Integer> dabConfigFromHal(
            @Nullable List<DabTableEntry> config) {
        if (config == null) return null;
        return config.stream().collect(Collectors.toMap(e -> e.label, e -> e.frequency));
    }

    static @NonNull RadioManager.ModuleProperties
    propertiesFromHal(int id, @NonNull String serviceName, @NonNull Properties prop,
            @Nullable AmFmRegionConfig amfmConfig, @Nullable List<DabTableEntry> dabConfig) {
        Objects.requireNonNull(serviceName);
        Objects.requireNonNull(prop);

        int[] supportedIdentifierTypes = prop.supportedIdentifierTypes.stream().
                mapToInt(Integer::intValue).toArray();
        int[] supportedProgramTypes = identifierTypesToProgramTypes(supportedIdentifierTypes);

        return new RadioManager.ModuleProperties(
                id,
                serviceName,

                // There is no Class concept in HAL 2.0.
                RadioManager.CLASS_AM_FM,

                prop.maker,
                prop.product,
                prop.version,
                prop.serial,

                /* HAL 2.0 only supports single tuner and audio source per
                 * HAL implementation instance. */
                1,      // numTuners
                1,      // numAudioSources
                false,  // isInitializationRequired
                false,  // isCaptureSupported

                amfmConfigToBands(amfmConfig),
                true,  // isBgScanSupported is deprecated
                supportedProgramTypes,
                supportedIdentifierTypes,
                dabConfigFromHal(dabConfig),
                vendorInfoFromHal(prop.vendorInfo)
        );
    }

    static void programIdentifierToHal(@NonNull ProgramIdentifier hwId,
            @NonNull ProgramSelector.Identifier id) {
        hwId.type = id.getType();
        hwId.value = id.getValue();
    }

    static @NonNull ProgramIdentifier programIdentifierToHal(
            @NonNull ProgramSelector.Identifier id) {
        ProgramIdentifier hwId = new ProgramIdentifier();
        programIdentifierToHal(hwId, id);
        return hwId;
    }

    static @Nullable ProgramSelector.Identifier programIdentifierFromHal(
            @NonNull ProgramIdentifier id) {
        if (id.type == IdentifierType.INVALID) return null;
        return new ProgramSelector.Identifier(id.type, id.value);
    }

    static @NonNull android.hardware.broadcastradio.V2_0.ProgramSelector programSelectorToHal(
            @NonNull ProgramSelector sel) {
        android.hardware.broadcastradio.V2_0.ProgramSelector hwSel =
            new android.hardware.broadcastradio.V2_0.ProgramSelector();

        programIdentifierToHal(hwSel.primaryId, sel.getPrimaryId());
        Arrays.stream(sel.getSecondaryIds()).map(Convert::programIdentifierToHal).
                forEachOrdered(hwSel.secondaryIds::add);

        return hwSel;
    }

    private static boolean isEmpty(
            @NonNull android.hardware.broadcastradio.V2_0.ProgramSelector sel) {
        return sel.primaryId.type == 0 && sel.primaryId.value == 0 && sel.secondaryIds.isEmpty();
    }

    static @Nullable ProgramSelector programSelectorFromHal(
            @NonNull android.hardware.broadcastradio.V2_0.ProgramSelector sel) {
        if (isEmpty(sel)) return null;

        ProgramSelector.Identifier[] secondaryIds = sel.secondaryIds.stream().
                map(Convert::programIdentifierFromHal).map(Objects::requireNonNull).
                toArray(ProgramSelector.Identifier[]::new);

        return new ProgramSelector(
                identifierTypeToProgramType(sel.primaryId.type),
                Objects.requireNonNull(programIdentifierFromHal(sel.primaryId)),
                secondaryIds, /* vendorIds= */ null);
    }

    private enum MetadataType {
        INT, STRING
    }

    private static class MetadataDef {
        private MetadataType type;
        private String key;
        private MetadataDef(MetadataType type, String key) {
            this.type = type;
            this.key = key;
        }
    }

    private static final SparseArray<MetadataDef> METADATA_KEYS;
    static {
        METADATA_KEYS = new SparseArray<>();
        METADATA_KEYS.put(MetadataKey.RDS_PS, new MetadataDef(
                MetadataType.STRING, RadioMetadata.METADATA_KEY_RDS_PS));
        METADATA_KEYS.put(MetadataKey.RDS_PTY, new MetadataDef(
                MetadataType.INT, RadioMetadata.METADATA_KEY_RDS_PTY));
        METADATA_KEYS.put(MetadataKey.RBDS_PTY, new MetadataDef(
                MetadataType.INT, RadioMetadata.METADATA_KEY_RBDS_PTY));
        METADATA_KEYS.put(MetadataKey.RDS_RT, new MetadataDef(
                MetadataType.STRING, RadioMetadata.METADATA_KEY_RDS_RT));
        METADATA_KEYS.put(MetadataKey.SONG_TITLE, new MetadataDef(
                MetadataType.STRING, RadioMetadata.METADATA_KEY_TITLE));
        METADATA_KEYS.put(MetadataKey.SONG_ARTIST, new MetadataDef(
                MetadataType.STRING, RadioMetadata.METADATA_KEY_ARTIST));
        METADATA_KEYS.put(MetadataKey.SONG_ALBUM, new MetadataDef(
                MetadataType.STRING, RadioMetadata.METADATA_KEY_ALBUM));
        METADATA_KEYS.put(MetadataKey.STATION_ICON, new MetadataDef(
                MetadataType.INT, RadioMetadata.METADATA_KEY_ICON));
        METADATA_KEYS.put(MetadataKey.ALBUM_ART, new MetadataDef(
                MetadataType.INT, RadioMetadata.METADATA_KEY_ART));
        METADATA_KEYS.put(MetadataKey.PROGRAM_NAME, new MetadataDef(
                MetadataType.STRING, RadioMetadata.METADATA_KEY_PROGRAM_NAME));
        METADATA_KEYS.put(MetadataKey.DAB_ENSEMBLE_NAME, new MetadataDef(
                MetadataType.STRING, RadioMetadata.METADATA_KEY_DAB_ENSEMBLE_NAME));
        METADATA_KEYS.put(MetadataKey.DAB_ENSEMBLE_NAME_SHORT, new MetadataDef(
                MetadataType.STRING, RadioMetadata.METADATA_KEY_DAB_ENSEMBLE_NAME_SHORT));
        METADATA_KEYS.put(MetadataKey.DAB_SERVICE_NAME, new MetadataDef(
                MetadataType.STRING, RadioMetadata.METADATA_KEY_DAB_SERVICE_NAME));
        METADATA_KEYS.put(MetadataKey.DAB_SERVICE_NAME_SHORT, new MetadataDef(
                MetadataType.STRING, RadioMetadata.METADATA_KEY_DAB_SERVICE_NAME_SHORT));
        METADATA_KEYS.put(MetadataKey.DAB_COMPONENT_NAME, new MetadataDef(
                MetadataType.STRING, RadioMetadata.METADATA_KEY_DAB_COMPONENT_NAME));
        METADATA_KEYS.put(MetadataKey.DAB_COMPONENT_NAME_SHORT, new MetadataDef(
                MetadataType.STRING, RadioMetadata.METADATA_KEY_DAB_COMPONENT_NAME_SHORT));
    }

    private static @NonNull RadioMetadata metadataFromHal(@NonNull ArrayList<Metadata> meta) {
        RadioMetadata.Builder builder = new RadioMetadata.Builder();

        for (Metadata entry : meta) {
            MetadataDef keyDef = METADATA_KEYS.get(entry.key);
            if (keyDef == null) {
                Slogf.i(TAG, "Ignored unknown metadata entry: " + MetadataKey.toString(entry.key));
                continue;
            }
            if (keyDef.type == MetadataType.STRING) {
                builder.putString(keyDef.key, entry.stringValue);
            } else {  // MetadataType.INT
                /* Current java API use 32-bit values for int metadata,
                 * but we might change it in the future */
                builder.putInt(keyDef.key, (int)entry.intValue);
            }
        }

        return builder.build();
    }

    static @NonNull RadioManager.ProgramInfo programInfoFromHal(@NonNull ProgramInfo info) {
        Collection<ProgramSelector.Identifier> relatedContent = info.relatedContent.stream().
                map(id -> Objects.requireNonNull(programIdentifierFromHal(id))).
                collect(Collectors.toList());

        return new RadioManager.ProgramInfo(
                Objects.requireNonNull(programSelectorFromHal(info.selector)),
                programIdentifierFromHal(info.logicallyTunedTo),
                programIdentifierFromHal(info.physicallyTunedTo),
                relatedContent,
                info.infoFlags,
                info.signalQuality,
                metadataFromHal(info.metadata),
                vendorInfoFromHal(info.vendorInfo)
        );
    }

    static @NonNull ProgramFilter programFilterToHal(@Nullable ProgramList.Filter filter) {
        if (filter == null) filter = new ProgramList.Filter();

        ProgramFilter hwFilter = new ProgramFilter();

        filter.getIdentifierTypes().stream().forEachOrdered(hwFilter.identifierTypes::add);
        filter.getIdentifiers().stream().forEachOrdered(
            id -> hwFilter.identifiers.add(programIdentifierToHal(id)));
        hwFilter.includeCategories = filter.areCategoriesIncluded();
        hwFilter.excludeModifications = filter.areModificationsExcluded();

        return hwFilter;
    }

    public static @NonNull android.hardware.radio.Announcement announcementFromHal(
            @NonNull Announcement hwAnnouncement) {
        return new android.hardware.radio.Announcement(
            Objects.requireNonNull(programSelectorFromHal(hwAnnouncement.selector)),
            hwAnnouncement.type,
            vendorInfoFromHal(hwAnnouncement.vendorInfo)
        );
    }

    static <T> @Nullable ArrayList<T> listToArrayList(@Nullable List<T> list) {
        if (list == null) return null;
        if (list instanceof ArrayList) return (ArrayList) list;
        return new ArrayList<>(list);
    }
}
