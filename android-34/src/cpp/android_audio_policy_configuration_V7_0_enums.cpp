#include <map>

#include "android_audio_policy_configuration_V7_0_enums.h"

namespace android {
    namespace audio {
        namespace policy {
            namespace configuration {
                namespace V7_0 {
                    const std::map<std::string, Version> VersionString {
                        { "7.0", Version::_7_0 },
                    };

                    Version stringToVersion(const std::string& value) {
                        auto enumValue = VersionString.find(value);
                        return enumValue != VersionString.end() ? enumValue->second : Version::UNKNOWN;
                    }

                    std::string toString(Version o) {
                        switch (o) {
                            case Version::_7_0: return "7.0";
                            default: return std::to_string(static_cast<int>(o));
                        }
                    }

                    const std::map<std::string, HalVersion> HalVersionString {
                        { "2.0", HalVersion::_2_0 },
                        { "3.0", HalVersion::_3_0 },
                    };

                    HalVersion stringToHalVersion(const std::string& value) {
                        auto enumValue = HalVersionString.find(value);
                        return enumValue != HalVersionString.end() ? enumValue->second : HalVersion::UNKNOWN;
                    }

                    std::string toString(HalVersion o) {
                        switch (o) {
                            case HalVersion::_2_0: return "2.0";
                            case HalVersion::_3_0: return "3.0";
                            default: return std::to_string(static_cast<int>(o));
                        }
                    }

                    const std::map<std::string, AudioInOutFlag> AudioInOutFlagString {
                        { "AUDIO_OUTPUT_FLAG_DIRECT", AudioInOutFlag::AUDIO_OUTPUT_FLAG_DIRECT },
                        { "AUDIO_OUTPUT_FLAG_PRIMARY", AudioInOutFlag::AUDIO_OUTPUT_FLAG_PRIMARY },
                        { "AUDIO_OUTPUT_FLAG_FAST", AudioInOutFlag::AUDIO_OUTPUT_FLAG_FAST },
                        { "AUDIO_OUTPUT_FLAG_DEEP_BUFFER", AudioInOutFlag::AUDIO_OUTPUT_FLAG_DEEP_BUFFER },
                        { "AUDIO_OUTPUT_FLAG_COMPRESS_OFFLOAD", AudioInOutFlag::AUDIO_OUTPUT_FLAG_COMPRESS_OFFLOAD },
                        { "AUDIO_OUTPUT_FLAG_NON_BLOCKING", AudioInOutFlag::AUDIO_OUTPUT_FLAG_NON_BLOCKING },
                        { "AUDIO_OUTPUT_FLAG_HW_AV_SYNC", AudioInOutFlag::AUDIO_OUTPUT_FLAG_HW_AV_SYNC },
                        { "AUDIO_OUTPUT_FLAG_TTS", AudioInOutFlag::AUDIO_OUTPUT_FLAG_TTS },
                        { "AUDIO_OUTPUT_FLAG_RAW", AudioInOutFlag::AUDIO_OUTPUT_FLAG_RAW },
                        { "AUDIO_OUTPUT_FLAG_SYNC", AudioInOutFlag::AUDIO_OUTPUT_FLAG_SYNC },
                        { "AUDIO_OUTPUT_FLAG_IEC958_NONAUDIO", AudioInOutFlag::AUDIO_OUTPUT_FLAG_IEC958_NONAUDIO },
                        { "AUDIO_OUTPUT_FLAG_DIRECT_PCM", AudioInOutFlag::AUDIO_OUTPUT_FLAG_DIRECT_PCM },
                        { "AUDIO_OUTPUT_FLAG_MMAP_NOIRQ", AudioInOutFlag::AUDIO_OUTPUT_FLAG_MMAP_NOIRQ },
                        { "AUDIO_OUTPUT_FLAG_VOIP_RX", AudioInOutFlag::AUDIO_OUTPUT_FLAG_VOIP_RX },
                        { "AUDIO_OUTPUT_FLAG_INCALL_MUSIC", AudioInOutFlag::AUDIO_OUTPUT_FLAG_INCALL_MUSIC },
                        { "AUDIO_OUTPUT_FLAG_GAPLESS_OFFLOAD", AudioInOutFlag::AUDIO_OUTPUT_FLAG_GAPLESS_OFFLOAD },
                        { "AUDIO_INPUT_FLAG_FAST", AudioInOutFlag::AUDIO_INPUT_FLAG_FAST },
                        { "AUDIO_INPUT_FLAG_HW_HOTWORD", AudioInOutFlag::AUDIO_INPUT_FLAG_HW_HOTWORD },
                        { "AUDIO_INPUT_FLAG_RAW", AudioInOutFlag::AUDIO_INPUT_FLAG_RAW },
                        { "AUDIO_INPUT_FLAG_SYNC", AudioInOutFlag::AUDIO_INPUT_FLAG_SYNC },
                        { "AUDIO_INPUT_FLAG_MMAP_NOIRQ", AudioInOutFlag::AUDIO_INPUT_FLAG_MMAP_NOIRQ },
                        { "AUDIO_INPUT_FLAG_VOIP_TX", AudioInOutFlag::AUDIO_INPUT_FLAG_VOIP_TX },
                        { "AUDIO_INPUT_FLAG_HW_AV_SYNC", AudioInOutFlag::AUDIO_INPUT_FLAG_HW_AV_SYNC },
                        { "AUDIO_INPUT_FLAG_DIRECT", AudioInOutFlag::AUDIO_INPUT_FLAG_DIRECT },
                    };

                    AudioInOutFlag stringToAudioInOutFlag(const std::string& value) {
                        auto enumValue = AudioInOutFlagString.find(value);
                        return enumValue != AudioInOutFlagString.end() ? enumValue->second : AudioInOutFlag::UNKNOWN;
                    }

                    std::string toString(AudioInOutFlag o) {
                        switch (o) {
                            case AudioInOutFlag::AUDIO_OUTPUT_FLAG_DIRECT: return "AUDIO_OUTPUT_FLAG_DIRECT";
                            case AudioInOutFlag::AUDIO_OUTPUT_FLAG_PRIMARY: return "AUDIO_OUTPUT_FLAG_PRIMARY";
                            case AudioInOutFlag::AUDIO_OUTPUT_FLAG_FAST: return "AUDIO_OUTPUT_FLAG_FAST";
                            case AudioInOutFlag::AUDIO_OUTPUT_FLAG_DEEP_BUFFER: return "AUDIO_OUTPUT_FLAG_DEEP_BUFFER";
                            case AudioInOutFlag::AUDIO_OUTPUT_FLAG_COMPRESS_OFFLOAD: return "AUDIO_OUTPUT_FLAG_COMPRESS_OFFLOAD";
                            case AudioInOutFlag::AUDIO_OUTPUT_FLAG_NON_BLOCKING: return "AUDIO_OUTPUT_FLAG_NON_BLOCKING";
                            case AudioInOutFlag::AUDIO_OUTPUT_FLAG_HW_AV_SYNC: return "AUDIO_OUTPUT_FLAG_HW_AV_SYNC";
                            case AudioInOutFlag::AUDIO_OUTPUT_FLAG_TTS: return "AUDIO_OUTPUT_FLAG_TTS";
                            case AudioInOutFlag::AUDIO_OUTPUT_FLAG_RAW: return "AUDIO_OUTPUT_FLAG_RAW";
                            case AudioInOutFlag::AUDIO_OUTPUT_FLAG_SYNC: return "AUDIO_OUTPUT_FLAG_SYNC";
                            case AudioInOutFlag::AUDIO_OUTPUT_FLAG_IEC958_NONAUDIO: return "AUDIO_OUTPUT_FLAG_IEC958_NONAUDIO";
                            case AudioInOutFlag::AUDIO_OUTPUT_FLAG_DIRECT_PCM: return "AUDIO_OUTPUT_FLAG_DIRECT_PCM";
                            case AudioInOutFlag::AUDIO_OUTPUT_FLAG_MMAP_NOIRQ: return "AUDIO_OUTPUT_FLAG_MMAP_NOIRQ";
                            case AudioInOutFlag::AUDIO_OUTPUT_FLAG_VOIP_RX: return "AUDIO_OUTPUT_FLAG_VOIP_RX";
                            case AudioInOutFlag::AUDIO_OUTPUT_FLAG_INCALL_MUSIC: return "AUDIO_OUTPUT_FLAG_INCALL_MUSIC";
                            case AudioInOutFlag::AUDIO_OUTPUT_FLAG_GAPLESS_OFFLOAD: return "AUDIO_OUTPUT_FLAG_GAPLESS_OFFLOAD";
                            case AudioInOutFlag::AUDIO_INPUT_FLAG_FAST: return "AUDIO_INPUT_FLAG_FAST";
                            case AudioInOutFlag::AUDIO_INPUT_FLAG_HW_HOTWORD: return "AUDIO_INPUT_FLAG_HW_HOTWORD";
                            case AudioInOutFlag::AUDIO_INPUT_FLAG_RAW: return "AUDIO_INPUT_FLAG_RAW";
                            case AudioInOutFlag::AUDIO_INPUT_FLAG_SYNC: return "AUDIO_INPUT_FLAG_SYNC";
                            case AudioInOutFlag::AUDIO_INPUT_FLAG_MMAP_NOIRQ: return "AUDIO_INPUT_FLAG_MMAP_NOIRQ";
                            case AudioInOutFlag::AUDIO_INPUT_FLAG_VOIP_TX: return "AUDIO_INPUT_FLAG_VOIP_TX";
                            case AudioInOutFlag::AUDIO_INPUT_FLAG_HW_AV_SYNC: return "AUDIO_INPUT_FLAG_HW_AV_SYNC";
                            case AudioInOutFlag::AUDIO_INPUT_FLAG_DIRECT: return "AUDIO_INPUT_FLAG_DIRECT";
                            default: return std::to_string(static_cast<int>(o));
                        }
                    }

                    const std::map<std::string, Role> RoleString {
                        { "sink", Role::sink },
                        { "source", Role::source },
                    };

                    Role stringToRole(const std::string& value) {
                        auto enumValue = RoleString.find(value);
                        return enumValue != RoleString.end() ? enumValue->second : Role::UNKNOWN;
                    }

                    std::string toString(Role o) {
                        switch (o) {
                            case Role::sink: return "sink";
                            case Role::source: return "source";
                            default: return std::to_string(static_cast<int>(o));
                        }
                    }

                    const std::map<std::string, AudioDevice> AudioDeviceString {
                        { "AUDIO_DEVICE_NONE", AudioDevice::AUDIO_DEVICE_NONE },
                        { "AUDIO_DEVICE_OUT_EARPIECE", AudioDevice::AUDIO_DEVICE_OUT_EARPIECE },
                        { "AUDIO_DEVICE_OUT_SPEAKER", AudioDevice::AUDIO_DEVICE_OUT_SPEAKER },
                        { "AUDIO_DEVICE_OUT_WIRED_HEADSET", AudioDevice::AUDIO_DEVICE_OUT_WIRED_HEADSET },
                        { "AUDIO_DEVICE_OUT_WIRED_HEADPHONE", AudioDevice::AUDIO_DEVICE_OUT_WIRED_HEADPHONE },
                        { "AUDIO_DEVICE_OUT_BLUETOOTH_SCO", AudioDevice::AUDIO_DEVICE_OUT_BLUETOOTH_SCO },
                        { "AUDIO_DEVICE_OUT_BLUETOOTH_SCO_HEADSET", AudioDevice::AUDIO_DEVICE_OUT_BLUETOOTH_SCO_HEADSET },
                        { "AUDIO_DEVICE_OUT_BLUETOOTH_SCO_CARKIT", AudioDevice::AUDIO_DEVICE_OUT_BLUETOOTH_SCO_CARKIT },
                        { "AUDIO_DEVICE_OUT_BLUETOOTH_A2DP", AudioDevice::AUDIO_DEVICE_OUT_BLUETOOTH_A2DP },
                        { "AUDIO_DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES", AudioDevice::AUDIO_DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES },
                        { "AUDIO_DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER", AudioDevice::AUDIO_DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER },
                        { "AUDIO_DEVICE_OUT_HDMI", AudioDevice::AUDIO_DEVICE_OUT_HDMI },
                        { "AUDIO_DEVICE_OUT_HDMI_EARC", AudioDevice::AUDIO_DEVICE_OUT_HDMI_EARC },
                        { "AUDIO_DEVICE_OUT_AUX_DIGITAL", AudioDevice::AUDIO_DEVICE_OUT_AUX_DIGITAL },
                        { "AUDIO_DEVICE_OUT_ANLG_DOCK_HEADSET", AudioDevice::AUDIO_DEVICE_OUT_ANLG_DOCK_HEADSET },
                        { "AUDIO_DEVICE_OUT_DGTL_DOCK_HEADSET", AudioDevice::AUDIO_DEVICE_OUT_DGTL_DOCK_HEADSET },
                        { "AUDIO_DEVICE_OUT_USB_ACCESSORY", AudioDevice::AUDIO_DEVICE_OUT_USB_ACCESSORY },
                        { "AUDIO_DEVICE_OUT_USB_DEVICE", AudioDevice::AUDIO_DEVICE_OUT_USB_DEVICE },
                        { "AUDIO_DEVICE_OUT_REMOTE_SUBMIX", AudioDevice::AUDIO_DEVICE_OUT_REMOTE_SUBMIX },
                        { "AUDIO_DEVICE_OUT_TELEPHONY_TX", AudioDevice::AUDIO_DEVICE_OUT_TELEPHONY_TX },
                        { "AUDIO_DEVICE_OUT_LINE", AudioDevice::AUDIO_DEVICE_OUT_LINE },
                        { "AUDIO_DEVICE_OUT_HDMI_ARC", AudioDevice::AUDIO_DEVICE_OUT_HDMI_ARC },
                        { "AUDIO_DEVICE_OUT_SPDIF", AudioDevice::AUDIO_DEVICE_OUT_SPDIF },
                        { "AUDIO_DEVICE_OUT_FM", AudioDevice::AUDIO_DEVICE_OUT_FM },
                        { "AUDIO_DEVICE_OUT_AUX_LINE", AudioDevice::AUDIO_DEVICE_OUT_AUX_LINE },
                        { "AUDIO_DEVICE_OUT_SPEAKER_SAFE", AudioDevice::AUDIO_DEVICE_OUT_SPEAKER_SAFE },
                        { "AUDIO_DEVICE_OUT_IP", AudioDevice::AUDIO_DEVICE_OUT_IP },
                        { "AUDIO_DEVICE_OUT_BUS", AudioDevice::AUDIO_DEVICE_OUT_BUS },
                        { "AUDIO_DEVICE_OUT_PROXY", AudioDevice::AUDIO_DEVICE_OUT_PROXY },
                        { "AUDIO_DEVICE_OUT_USB_HEADSET", AudioDevice::AUDIO_DEVICE_OUT_USB_HEADSET },
                        { "AUDIO_DEVICE_OUT_HEARING_AID", AudioDevice::AUDIO_DEVICE_OUT_HEARING_AID },
                        { "AUDIO_DEVICE_OUT_ECHO_CANCELLER", AudioDevice::AUDIO_DEVICE_OUT_ECHO_CANCELLER },
                        { "AUDIO_DEVICE_OUT_BLE_HEADSET", AudioDevice::AUDIO_DEVICE_OUT_BLE_HEADSET },
                        { "AUDIO_DEVICE_OUT_BLE_SPEAKER", AudioDevice::AUDIO_DEVICE_OUT_BLE_SPEAKER },
                        { "AUDIO_DEVICE_OUT_DEFAULT", AudioDevice::AUDIO_DEVICE_OUT_DEFAULT },
                        { "AUDIO_DEVICE_OUT_STUB", AudioDevice::AUDIO_DEVICE_OUT_STUB },
                        { "AUDIO_DEVICE_IN_COMMUNICATION", AudioDevice::AUDIO_DEVICE_IN_COMMUNICATION },
                        { "AUDIO_DEVICE_IN_AMBIENT", AudioDevice::AUDIO_DEVICE_IN_AMBIENT },
                        { "AUDIO_DEVICE_IN_BUILTIN_MIC", AudioDevice::AUDIO_DEVICE_IN_BUILTIN_MIC },
                        { "AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET", AudioDevice::AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET },
                        { "AUDIO_DEVICE_IN_WIRED_HEADSET", AudioDevice::AUDIO_DEVICE_IN_WIRED_HEADSET },
                        { "AUDIO_DEVICE_IN_HDMI", AudioDevice::AUDIO_DEVICE_IN_HDMI },
                        { "AUDIO_DEVICE_IN_AUX_DIGITAL", AudioDevice::AUDIO_DEVICE_IN_AUX_DIGITAL },
                        { "AUDIO_DEVICE_IN_VOICE_CALL", AudioDevice::AUDIO_DEVICE_IN_VOICE_CALL },
                        { "AUDIO_DEVICE_IN_TELEPHONY_RX", AudioDevice::AUDIO_DEVICE_IN_TELEPHONY_RX },
                        { "AUDIO_DEVICE_IN_BACK_MIC", AudioDevice::AUDIO_DEVICE_IN_BACK_MIC },
                        { "AUDIO_DEVICE_IN_REMOTE_SUBMIX", AudioDevice::AUDIO_DEVICE_IN_REMOTE_SUBMIX },
                        { "AUDIO_DEVICE_IN_ANLG_DOCK_HEADSET", AudioDevice::AUDIO_DEVICE_IN_ANLG_DOCK_HEADSET },
                        { "AUDIO_DEVICE_IN_DGTL_DOCK_HEADSET", AudioDevice::AUDIO_DEVICE_IN_DGTL_DOCK_HEADSET },
                        { "AUDIO_DEVICE_IN_USB_ACCESSORY", AudioDevice::AUDIO_DEVICE_IN_USB_ACCESSORY },
                        { "AUDIO_DEVICE_IN_USB_DEVICE", AudioDevice::AUDIO_DEVICE_IN_USB_DEVICE },
                        { "AUDIO_DEVICE_IN_FM_TUNER", AudioDevice::AUDIO_DEVICE_IN_FM_TUNER },
                        { "AUDIO_DEVICE_IN_TV_TUNER", AudioDevice::AUDIO_DEVICE_IN_TV_TUNER },
                        { "AUDIO_DEVICE_IN_LINE", AudioDevice::AUDIO_DEVICE_IN_LINE },
                        { "AUDIO_DEVICE_IN_SPDIF", AudioDevice::AUDIO_DEVICE_IN_SPDIF },
                        { "AUDIO_DEVICE_IN_BLUETOOTH_A2DP", AudioDevice::AUDIO_DEVICE_IN_BLUETOOTH_A2DP },
                        { "AUDIO_DEVICE_IN_LOOPBACK", AudioDevice::AUDIO_DEVICE_IN_LOOPBACK },
                        { "AUDIO_DEVICE_IN_IP", AudioDevice::AUDIO_DEVICE_IN_IP },
                        { "AUDIO_DEVICE_IN_BUS", AudioDevice::AUDIO_DEVICE_IN_BUS },
                        { "AUDIO_DEVICE_IN_PROXY", AudioDevice::AUDIO_DEVICE_IN_PROXY },
                        { "AUDIO_DEVICE_IN_USB_HEADSET", AudioDevice::AUDIO_DEVICE_IN_USB_HEADSET },
                        { "AUDIO_DEVICE_IN_BLUETOOTH_BLE", AudioDevice::AUDIO_DEVICE_IN_BLUETOOTH_BLE },
                        { "AUDIO_DEVICE_IN_HDMI_ARC", AudioDevice::AUDIO_DEVICE_IN_HDMI_ARC },
                        { "AUDIO_DEVICE_IN_HDMI_EARC", AudioDevice::AUDIO_DEVICE_IN_HDMI_EARC },
                        { "AUDIO_DEVICE_IN_ECHO_REFERENCE", AudioDevice::AUDIO_DEVICE_IN_ECHO_REFERENCE },
                        { "AUDIO_DEVICE_IN_BLE_HEADSET", AudioDevice::AUDIO_DEVICE_IN_BLE_HEADSET },
                        { "AUDIO_DEVICE_IN_DEFAULT", AudioDevice::AUDIO_DEVICE_IN_DEFAULT },
                        { "AUDIO_DEVICE_IN_STUB", AudioDevice::AUDIO_DEVICE_IN_STUB },
                    };

                    AudioDevice stringToAudioDevice(const std::string& value) {
                        auto enumValue = AudioDeviceString.find(value);
                        return enumValue != AudioDeviceString.end() ? enumValue->second : AudioDevice::UNKNOWN;
                    }

                    std::string toString(AudioDevice o) {
                        switch (o) {
                            case AudioDevice::AUDIO_DEVICE_NONE: return "AUDIO_DEVICE_NONE";
                            case AudioDevice::AUDIO_DEVICE_OUT_EARPIECE: return "AUDIO_DEVICE_OUT_EARPIECE";
                            case AudioDevice::AUDIO_DEVICE_OUT_SPEAKER: return "AUDIO_DEVICE_OUT_SPEAKER";
                            case AudioDevice::AUDIO_DEVICE_OUT_WIRED_HEADSET: return "AUDIO_DEVICE_OUT_WIRED_HEADSET";
                            case AudioDevice::AUDIO_DEVICE_OUT_WIRED_HEADPHONE: return "AUDIO_DEVICE_OUT_WIRED_HEADPHONE";
                            case AudioDevice::AUDIO_DEVICE_OUT_BLUETOOTH_SCO: return "AUDIO_DEVICE_OUT_BLUETOOTH_SCO";
                            case AudioDevice::AUDIO_DEVICE_OUT_BLUETOOTH_SCO_HEADSET: return "AUDIO_DEVICE_OUT_BLUETOOTH_SCO_HEADSET";
                            case AudioDevice::AUDIO_DEVICE_OUT_BLUETOOTH_SCO_CARKIT: return "AUDIO_DEVICE_OUT_BLUETOOTH_SCO_CARKIT";
                            case AudioDevice::AUDIO_DEVICE_OUT_BLUETOOTH_A2DP: return "AUDIO_DEVICE_OUT_BLUETOOTH_A2DP";
                            case AudioDevice::AUDIO_DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES: return "AUDIO_DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES";
                            case AudioDevice::AUDIO_DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER: return "AUDIO_DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER";
                            case AudioDevice::AUDIO_DEVICE_OUT_HDMI: return "AUDIO_DEVICE_OUT_HDMI";
                            case AudioDevice::AUDIO_DEVICE_OUT_HDMI_EARC: return "AUDIO_DEVICE_OUT_HDMI_EARC";
                            case AudioDevice::AUDIO_DEVICE_OUT_AUX_DIGITAL: return "AUDIO_DEVICE_OUT_AUX_DIGITAL";
                            case AudioDevice::AUDIO_DEVICE_OUT_ANLG_DOCK_HEADSET: return "AUDIO_DEVICE_OUT_ANLG_DOCK_HEADSET";
                            case AudioDevice::AUDIO_DEVICE_OUT_DGTL_DOCK_HEADSET: return "AUDIO_DEVICE_OUT_DGTL_DOCK_HEADSET";
                            case AudioDevice::AUDIO_DEVICE_OUT_USB_ACCESSORY: return "AUDIO_DEVICE_OUT_USB_ACCESSORY";
                            case AudioDevice::AUDIO_DEVICE_OUT_USB_DEVICE: return "AUDIO_DEVICE_OUT_USB_DEVICE";
                            case AudioDevice::AUDIO_DEVICE_OUT_REMOTE_SUBMIX: return "AUDIO_DEVICE_OUT_REMOTE_SUBMIX";
                            case AudioDevice::AUDIO_DEVICE_OUT_TELEPHONY_TX: return "AUDIO_DEVICE_OUT_TELEPHONY_TX";
                            case AudioDevice::AUDIO_DEVICE_OUT_LINE: return "AUDIO_DEVICE_OUT_LINE";
                            case AudioDevice::AUDIO_DEVICE_OUT_HDMI_ARC: return "AUDIO_DEVICE_OUT_HDMI_ARC";
                            case AudioDevice::AUDIO_DEVICE_OUT_SPDIF: return "AUDIO_DEVICE_OUT_SPDIF";
                            case AudioDevice::AUDIO_DEVICE_OUT_FM: return "AUDIO_DEVICE_OUT_FM";
                            case AudioDevice::AUDIO_DEVICE_OUT_AUX_LINE: return "AUDIO_DEVICE_OUT_AUX_LINE";
                            case AudioDevice::AUDIO_DEVICE_OUT_SPEAKER_SAFE: return "AUDIO_DEVICE_OUT_SPEAKER_SAFE";
                            case AudioDevice::AUDIO_DEVICE_OUT_IP: return "AUDIO_DEVICE_OUT_IP";
                            case AudioDevice::AUDIO_DEVICE_OUT_BUS: return "AUDIO_DEVICE_OUT_BUS";
                            case AudioDevice::AUDIO_DEVICE_OUT_PROXY: return "AUDIO_DEVICE_OUT_PROXY";
                            case AudioDevice::AUDIO_DEVICE_OUT_USB_HEADSET: return "AUDIO_DEVICE_OUT_USB_HEADSET";
                            case AudioDevice::AUDIO_DEVICE_OUT_HEARING_AID: return "AUDIO_DEVICE_OUT_HEARING_AID";
                            case AudioDevice::AUDIO_DEVICE_OUT_ECHO_CANCELLER: return "AUDIO_DEVICE_OUT_ECHO_CANCELLER";
                            case AudioDevice::AUDIO_DEVICE_OUT_BLE_HEADSET: return "AUDIO_DEVICE_OUT_BLE_HEADSET";
                            case AudioDevice::AUDIO_DEVICE_OUT_BLE_SPEAKER: return "AUDIO_DEVICE_OUT_BLE_SPEAKER";
                            case AudioDevice::AUDIO_DEVICE_OUT_DEFAULT: return "AUDIO_DEVICE_OUT_DEFAULT";
                            case AudioDevice::AUDIO_DEVICE_OUT_STUB: return "AUDIO_DEVICE_OUT_STUB";
                            case AudioDevice::AUDIO_DEVICE_IN_COMMUNICATION: return "AUDIO_DEVICE_IN_COMMUNICATION";
                            case AudioDevice::AUDIO_DEVICE_IN_AMBIENT: return "AUDIO_DEVICE_IN_AMBIENT";
                            case AudioDevice::AUDIO_DEVICE_IN_BUILTIN_MIC: return "AUDIO_DEVICE_IN_BUILTIN_MIC";
                            case AudioDevice::AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET: return "AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET";
                            case AudioDevice::AUDIO_DEVICE_IN_WIRED_HEADSET: return "AUDIO_DEVICE_IN_WIRED_HEADSET";
                            case AudioDevice::AUDIO_DEVICE_IN_HDMI: return "AUDIO_DEVICE_IN_HDMI";
                            case AudioDevice::AUDIO_DEVICE_IN_AUX_DIGITAL: return "AUDIO_DEVICE_IN_AUX_DIGITAL";
                            case AudioDevice::AUDIO_DEVICE_IN_VOICE_CALL: return "AUDIO_DEVICE_IN_VOICE_CALL";
                            case AudioDevice::AUDIO_DEVICE_IN_TELEPHONY_RX: return "AUDIO_DEVICE_IN_TELEPHONY_RX";
                            case AudioDevice::AUDIO_DEVICE_IN_BACK_MIC: return "AUDIO_DEVICE_IN_BACK_MIC";
                            case AudioDevice::AUDIO_DEVICE_IN_REMOTE_SUBMIX: return "AUDIO_DEVICE_IN_REMOTE_SUBMIX";
                            case AudioDevice::AUDIO_DEVICE_IN_ANLG_DOCK_HEADSET: return "AUDIO_DEVICE_IN_ANLG_DOCK_HEADSET";
                            case AudioDevice::AUDIO_DEVICE_IN_DGTL_DOCK_HEADSET: return "AUDIO_DEVICE_IN_DGTL_DOCK_HEADSET";
                            case AudioDevice::AUDIO_DEVICE_IN_USB_ACCESSORY: return "AUDIO_DEVICE_IN_USB_ACCESSORY";
                            case AudioDevice::AUDIO_DEVICE_IN_USB_DEVICE: return "AUDIO_DEVICE_IN_USB_DEVICE";
                            case AudioDevice::AUDIO_DEVICE_IN_FM_TUNER: return "AUDIO_DEVICE_IN_FM_TUNER";
                            case AudioDevice::AUDIO_DEVICE_IN_TV_TUNER: return "AUDIO_DEVICE_IN_TV_TUNER";
                            case AudioDevice::AUDIO_DEVICE_IN_LINE: return "AUDIO_DEVICE_IN_LINE";
                            case AudioDevice::AUDIO_DEVICE_IN_SPDIF: return "AUDIO_DEVICE_IN_SPDIF";
                            case AudioDevice::AUDIO_DEVICE_IN_BLUETOOTH_A2DP: return "AUDIO_DEVICE_IN_BLUETOOTH_A2DP";
                            case AudioDevice::AUDIO_DEVICE_IN_LOOPBACK: return "AUDIO_DEVICE_IN_LOOPBACK";
                            case AudioDevice::AUDIO_DEVICE_IN_IP: return "AUDIO_DEVICE_IN_IP";
                            case AudioDevice::AUDIO_DEVICE_IN_BUS: return "AUDIO_DEVICE_IN_BUS";
                            case AudioDevice::AUDIO_DEVICE_IN_PROXY: return "AUDIO_DEVICE_IN_PROXY";
                            case AudioDevice::AUDIO_DEVICE_IN_USB_HEADSET: return "AUDIO_DEVICE_IN_USB_HEADSET";
                            case AudioDevice::AUDIO_DEVICE_IN_BLUETOOTH_BLE: return "AUDIO_DEVICE_IN_BLUETOOTH_BLE";
                            case AudioDevice::AUDIO_DEVICE_IN_HDMI_ARC: return "AUDIO_DEVICE_IN_HDMI_ARC";
                            case AudioDevice::AUDIO_DEVICE_IN_HDMI_EARC: return "AUDIO_DEVICE_IN_HDMI_EARC";
                            case AudioDevice::AUDIO_DEVICE_IN_ECHO_REFERENCE: return "AUDIO_DEVICE_IN_ECHO_REFERENCE";
                            case AudioDevice::AUDIO_DEVICE_IN_BLE_HEADSET: return "AUDIO_DEVICE_IN_BLE_HEADSET";
                            case AudioDevice::AUDIO_DEVICE_IN_DEFAULT: return "AUDIO_DEVICE_IN_DEFAULT";
                            case AudioDevice::AUDIO_DEVICE_IN_STUB: return "AUDIO_DEVICE_IN_STUB";
                            default: return std::to_string(static_cast<int>(o));
                        }
                    }

                    const std::map<std::string, AudioFormat> AudioFormatString {
                        { "AUDIO_FORMAT_DEFAULT", AudioFormat::AUDIO_FORMAT_DEFAULT },
                        { "AUDIO_FORMAT_PCM_16_BIT", AudioFormat::AUDIO_FORMAT_PCM_16_BIT },
                        { "AUDIO_FORMAT_PCM_8_BIT", AudioFormat::AUDIO_FORMAT_PCM_8_BIT },
                        { "AUDIO_FORMAT_PCM_32_BIT", AudioFormat::AUDIO_FORMAT_PCM_32_BIT },
                        { "AUDIO_FORMAT_PCM_8_24_BIT", AudioFormat::AUDIO_FORMAT_PCM_8_24_BIT },
                        { "AUDIO_FORMAT_PCM_FLOAT", AudioFormat::AUDIO_FORMAT_PCM_FLOAT },
                        { "AUDIO_FORMAT_PCM_24_BIT_PACKED", AudioFormat::AUDIO_FORMAT_PCM_24_BIT_PACKED },
                        { "AUDIO_FORMAT_MP3", AudioFormat::AUDIO_FORMAT_MP3 },
                        { "AUDIO_FORMAT_AMR_NB", AudioFormat::AUDIO_FORMAT_AMR_NB },
                        { "AUDIO_FORMAT_AMR_WB", AudioFormat::AUDIO_FORMAT_AMR_WB },
                        { "AUDIO_FORMAT_AAC", AudioFormat::AUDIO_FORMAT_AAC },
                        { "AUDIO_FORMAT_AAC_MAIN", AudioFormat::AUDIO_FORMAT_AAC_MAIN },
                        { "AUDIO_FORMAT_AAC_LC", AudioFormat::AUDIO_FORMAT_AAC_LC },
                        { "AUDIO_FORMAT_AAC_SSR", AudioFormat::AUDIO_FORMAT_AAC_SSR },
                        { "AUDIO_FORMAT_AAC_LTP", AudioFormat::AUDIO_FORMAT_AAC_LTP },
                        { "AUDIO_FORMAT_AAC_HE_V1", AudioFormat::AUDIO_FORMAT_AAC_HE_V1 },
                        { "AUDIO_FORMAT_AAC_SCALABLE", AudioFormat::AUDIO_FORMAT_AAC_SCALABLE },
                        { "AUDIO_FORMAT_AAC_ERLC", AudioFormat::AUDIO_FORMAT_AAC_ERLC },
                        { "AUDIO_FORMAT_AAC_LD", AudioFormat::AUDIO_FORMAT_AAC_LD },
                        { "AUDIO_FORMAT_AAC_HE_V2", AudioFormat::AUDIO_FORMAT_AAC_HE_V2 },
                        { "AUDIO_FORMAT_AAC_ELD", AudioFormat::AUDIO_FORMAT_AAC_ELD },
                        { "AUDIO_FORMAT_AAC_XHE", AudioFormat::AUDIO_FORMAT_AAC_XHE },
                        { "AUDIO_FORMAT_HE_AAC_V1", AudioFormat::AUDIO_FORMAT_HE_AAC_V1 },
                        { "AUDIO_FORMAT_HE_AAC_V2", AudioFormat::AUDIO_FORMAT_HE_AAC_V2 },
                        { "AUDIO_FORMAT_VORBIS", AudioFormat::AUDIO_FORMAT_VORBIS },
                        { "AUDIO_FORMAT_OPUS", AudioFormat::AUDIO_FORMAT_OPUS },
                        { "AUDIO_FORMAT_AC3", AudioFormat::AUDIO_FORMAT_AC3 },
                        { "AUDIO_FORMAT_E_AC3", AudioFormat::AUDIO_FORMAT_E_AC3 },
                        { "AUDIO_FORMAT_E_AC3_JOC", AudioFormat::AUDIO_FORMAT_E_AC3_JOC },
                        { "AUDIO_FORMAT_DTS", AudioFormat::AUDIO_FORMAT_DTS },
                        { "AUDIO_FORMAT_DTS_HD", AudioFormat::AUDIO_FORMAT_DTS_HD },
                        { "AUDIO_FORMAT_IEC61937", AudioFormat::AUDIO_FORMAT_IEC61937 },
                        { "AUDIO_FORMAT_DOLBY_TRUEHD", AudioFormat::AUDIO_FORMAT_DOLBY_TRUEHD },
                        { "AUDIO_FORMAT_EVRC", AudioFormat::AUDIO_FORMAT_EVRC },
                        { "AUDIO_FORMAT_EVRCB", AudioFormat::AUDIO_FORMAT_EVRCB },
                        { "AUDIO_FORMAT_EVRCWB", AudioFormat::AUDIO_FORMAT_EVRCWB },
                        { "AUDIO_FORMAT_EVRCNW", AudioFormat::AUDIO_FORMAT_EVRCNW },
                        { "AUDIO_FORMAT_AAC_ADIF", AudioFormat::AUDIO_FORMAT_AAC_ADIF },
                        { "AUDIO_FORMAT_WMA", AudioFormat::AUDIO_FORMAT_WMA },
                        { "AUDIO_FORMAT_WMA_PRO", AudioFormat::AUDIO_FORMAT_WMA_PRO },
                        { "AUDIO_FORMAT_AMR_WB_PLUS", AudioFormat::AUDIO_FORMAT_AMR_WB_PLUS },
                        { "AUDIO_FORMAT_MP2", AudioFormat::AUDIO_FORMAT_MP2 },
                        { "AUDIO_FORMAT_QCELP", AudioFormat::AUDIO_FORMAT_QCELP },
                        { "AUDIO_FORMAT_DSD", AudioFormat::AUDIO_FORMAT_DSD },
                        { "AUDIO_FORMAT_FLAC", AudioFormat::AUDIO_FORMAT_FLAC },
                        { "AUDIO_FORMAT_ALAC", AudioFormat::AUDIO_FORMAT_ALAC },
                        { "AUDIO_FORMAT_APE", AudioFormat::AUDIO_FORMAT_APE },
                        { "AUDIO_FORMAT_AAC_ADTS", AudioFormat::AUDIO_FORMAT_AAC_ADTS },
                        { "AUDIO_FORMAT_AAC_ADTS_MAIN", AudioFormat::AUDIO_FORMAT_AAC_ADTS_MAIN },
                        { "AUDIO_FORMAT_AAC_ADTS_LC", AudioFormat::AUDIO_FORMAT_AAC_ADTS_LC },
                        { "AUDIO_FORMAT_AAC_ADTS_SSR", AudioFormat::AUDIO_FORMAT_AAC_ADTS_SSR },
                        { "AUDIO_FORMAT_AAC_ADTS_LTP", AudioFormat::AUDIO_FORMAT_AAC_ADTS_LTP },
                        { "AUDIO_FORMAT_AAC_ADTS_HE_V1", AudioFormat::AUDIO_FORMAT_AAC_ADTS_HE_V1 },
                        { "AUDIO_FORMAT_AAC_ADTS_SCALABLE", AudioFormat::AUDIO_FORMAT_AAC_ADTS_SCALABLE },
                        { "AUDIO_FORMAT_AAC_ADTS_ERLC", AudioFormat::AUDIO_FORMAT_AAC_ADTS_ERLC },
                        { "AUDIO_FORMAT_AAC_ADTS_LD", AudioFormat::AUDIO_FORMAT_AAC_ADTS_LD },
                        { "AUDIO_FORMAT_AAC_ADTS_HE_V2", AudioFormat::AUDIO_FORMAT_AAC_ADTS_HE_V2 },
                        { "AUDIO_FORMAT_AAC_ADTS_ELD", AudioFormat::AUDIO_FORMAT_AAC_ADTS_ELD },
                        { "AUDIO_FORMAT_AAC_ADTS_XHE", AudioFormat::AUDIO_FORMAT_AAC_ADTS_XHE },
                        { "AUDIO_FORMAT_SBC", AudioFormat::AUDIO_FORMAT_SBC },
                        { "AUDIO_FORMAT_APTX", AudioFormat::AUDIO_FORMAT_APTX },
                        { "AUDIO_FORMAT_APTX_HD", AudioFormat::AUDIO_FORMAT_APTX_HD },
                        { "AUDIO_FORMAT_AC4", AudioFormat::AUDIO_FORMAT_AC4 },
                        { "AUDIO_FORMAT_LDAC", AudioFormat::AUDIO_FORMAT_LDAC },
                        { "AUDIO_FORMAT_MAT", AudioFormat::AUDIO_FORMAT_MAT },
                        { "AUDIO_FORMAT_MAT_1_0", AudioFormat::AUDIO_FORMAT_MAT_1_0 },
                        { "AUDIO_FORMAT_MAT_2_0", AudioFormat::AUDIO_FORMAT_MAT_2_0 },
                        { "AUDIO_FORMAT_MAT_2_1", AudioFormat::AUDIO_FORMAT_MAT_2_1 },
                        { "AUDIO_FORMAT_AAC_LATM", AudioFormat::AUDIO_FORMAT_AAC_LATM },
                        { "AUDIO_FORMAT_AAC_LATM_LC", AudioFormat::AUDIO_FORMAT_AAC_LATM_LC },
                        { "AUDIO_FORMAT_AAC_LATM_HE_V1", AudioFormat::AUDIO_FORMAT_AAC_LATM_HE_V1 },
                        { "AUDIO_FORMAT_AAC_LATM_HE_V2", AudioFormat::AUDIO_FORMAT_AAC_LATM_HE_V2 },
                        { "AUDIO_FORMAT_CELT", AudioFormat::AUDIO_FORMAT_CELT },
                        { "AUDIO_FORMAT_APTX_ADAPTIVE", AudioFormat::AUDIO_FORMAT_APTX_ADAPTIVE },
                        { "AUDIO_FORMAT_LHDC", AudioFormat::AUDIO_FORMAT_LHDC },
                        { "AUDIO_FORMAT_LHDC_LL", AudioFormat::AUDIO_FORMAT_LHDC_LL },
                        { "AUDIO_FORMAT_APTX_TWSP", AudioFormat::AUDIO_FORMAT_APTX_TWSP },
                        { "AUDIO_FORMAT_LC3", AudioFormat::AUDIO_FORMAT_LC3 },
                        { "AUDIO_FORMAT_MPEGH_BL_L3", AudioFormat::AUDIO_FORMAT_MPEGH_BL_L3 },
                        { "AUDIO_FORMAT_MPEGH_BL_L4", AudioFormat::AUDIO_FORMAT_MPEGH_BL_L4 },
                        { "AUDIO_FORMAT_MPEGH_LC_L3", AudioFormat::AUDIO_FORMAT_MPEGH_LC_L3 },
                        { "AUDIO_FORMAT_MPEGH_LC_L4", AudioFormat::AUDIO_FORMAT_MPEGH_LC_L4 },
                        { "AUDIO_FORMAT_IEC60958", AudioFormat::AUDIO_FORMAT_IEC60958 },
                        { "AUDIO_FORMAT_DTS_UHD", AudioFormat::AUDIO_FORMAT_DTS_UHD },
                        { "AUDIO_FORMAT_DRA", AudioFormat::AUDIO_FORMAT_DRA },
                    };

                    AudioFormat stringToAudioFormat(const std::string& value) {
                        auto enumValue = AudioFormatString.find(value);
                        return enumValue != AudioFormatString.end() ? enumValue->second : AudioFormat::UNKNOWN;
                    }

                    std::string toString(AudioFormat o) {
                        switch (o) {
                            case AudioFormat::AUDIO_FORMAT_DEFAULT: return "AUDIO_FORMAT_DEFAULT";
                            case AudioFormat::AUDIO_FORMAT_PCM_16_BIT: return "AUDIO_FORMAT_PCM_16_BIT";
                            case AudioFormat::AUDIO_FORMAT_PCM_8_BIT: return "AUDIO_FORMAT_PCM_8_BIT";
                            case AudioFormat::AUDIO_FORMAT_PCM_32_BIT: return "AUDIO_FORMAT_PCM_32_BIT";
                            case AudioFormat::AUDIO_FORMAT_PCM_8_24_BIT: return "AUDIO_FORMAT_PCM_8_24_BIT";
                            case AudioFormat::AUDIO_FORMAT_PCM_FLOAT: return "AUDIO_FORMAT_PCM_FLOAT";
                            case AudioFormat::AUDIO_FORMAT_PCM_24_BIT_PACKED: return "AUDIO_FORMAT_PCM_24_BIT_PACKED";
                            case AudioFormat::AUDIO_FORMAT_MP3: return "AUDIO_FORMAT_MP3";
                            case AudioFormat::AUDIO_FORMAT_AMR_NB: return "AUDIO_FORMAT_AMR_NB";
                            case AudioFormat::AUDIO_FORMAT_AMR_WB: return "AUDIO_FORMAT_AMR_WB";
                            case AudioFormat::AUDIO_FORMAT_AAC: return "AUDIO_FORMAT_AAC";
                            case AudioFormat::AUDIO_FORMAT_AAC_MAIN: return "AUDIO_FORMAT_AAC_MAIN";
                            case AudioFormat::AUDIO_FORMAT_AAC_LC: return "AUDIO_FORMAT_AAC_LC";
                            case AudioFormat::AUDIO_FORMAT_AAC_SSR: return "AUDIO_FORMAT_AAC_SSR";
                            case AudioFormat::AUDIO_FORMAT_AAC_LTP: return "AUDIO_FORMAT_AAC_LTP";
                            case AudioFormat::AUDIO_FORMAT_AAC_HE_V1: return "AUDIO_FORMAT_AAC_HE_V1";
                            case AudioFormat::AUDIO_FORMAT_AAC_SCALABLE: return "AUDIO_FORMAT_AAC_SCALABLE";
                            case AudioFormat::AUDIO_FORMAT_AAC_ERLC: return "AUDIO_FORMAT_AAC_ERLC";
                            case AudioFormat::AUDIO_FORMAT_AAC_LD: return "AUDIO_FORMAT_AAC_LD";
                            case AudioFormat::AUDIO_FORMAT_AAC_HE_V2: return "AUDIO_FORMAT_AAC_HE_V2";
                            case AudioFormat::AUDIO_FORMAT_AAC_ELD: return "AUDIO_FORMAT_AAC_ELD";
                            case AudioFormat::AUDIO_FORMAT_AAC_XHE: return "AUDIO_FORMAT_AAC_XHE";
                            case AudioFormat::AUDIO_FORMAT_HE_AAC_V1: return "AUDIO_FORMAT_HE_AAC_V1";
                            case AudioFormat::AUDIO_FORMAT_HE_AAC_V2: return "AUDIO_FORMAT_HE_AAC_V2";
                            case AudioFormat::AUDIO_FORMAT_VORBIS: return "AUDIO_FORMAT_VORBIS";
                            case AudioFormat::AUDIO_FORMAT_OPUS: return "AUDIO_FORMAT_OPUS";
                            case AudioFormat::AUDIO_FORMAT_AC3: return "AUDIO_FORMAT_AC3";
                            case AudioFormat::AUDIO_FORMAT_E_AC3: return "AUDIO_FORMAT_E_AC3";
                            case AudioFormat::AUDIO_FORMAT_E_AC3_JOC: return "AUDIO_FORMAT_E_AC3_JOC";
                            case AudioFormat::AUDIO_FORMAT_DTS: return "AUDIO_FORMAT_DTS";
                            case AudioFormat::AUDIO_FORMAT_DTS_HD: return "AUDIO_FORMAT_DTS_HD";
                            case AudioFormat::AUDIO_FORMAT_IEC61937: return "AUDIO_FORMAT_IEC61937";
                            case AudioFormat::AUDIO_FORMAT_DOLBY_TRUEHD: return "AUDIO_FORMAT_DOLBY_TRUEHD";
                            case AudioFormat::AUDIO_FORMAT_EVRC: return "AUDIO_FORMAT_EVRC";
                            case AudioFormat::AUDIO_FORMAT_EVRCB: return "AUDIO_FORMAT_EVRCB";
                            case AudioFormat::AUDIO_FORMAT_EVRCWB: return "AUDIO_FORMAT_EVRCWB";
                            case AudioFormat::AUDIO_FORMAT_EVRCNW: return "AUDIO_FORMAT_EVRCNW";
                            case AudioFormat::AUDIO_FORMAT_AAC_ADIF: return "AUDIO_FORMAT_AAC_ADIF";
                            case AudioFormat::AUDIO_FORMAT_WMA: return "AUDIO_FORMAT_WMA";
                            case AudioFormat::AUDIO_FORMAT_WMA_PRO: return "AUDIO_FORMAT_WMA_PRO";
                            case AudioFormat::AUDIO_FORMAT_AMR_WB_PLUS: return "AUDIO_FORMAT_AMR_WB_PLUS";
                            case AudioFormat::AUDIO_FORMAT_MP2: return "AUDIO_FORMAT_MP2";
                            case AudioFormat::AUDIO_FORMAT_QCELP: return "AUDIO_FORMAT_QCELP";
                            case AudioFormat::AUDIO_FORMAT_DSD: return "AUDIO_FORMAT_DSD";
                            case AudioFormat::AUDIO_FORMAT_FLAC: return "AUDIO_FORMAT_FLAC";
                            case AudioFormat::AUDIO_FORMAT_ALAC: return "AUDIO_FORMAT_ALAC";
                            case AudioFormat::AUDIO_FORMAT_APE: return "AUDIO_FORMAT_APE";
                            case AudioFormat::AUDIO_FORMAT_AAC_ADTS: return "AUDIO_FORMAT_AAC_ADTS";
                            case AudioFormat::AUDIO_FORMAT_AAC_ADTS_MAIN: return "AUDIO_FORMAT_AAC_ADTS_MAIN";
                            case AudioFormat::AUDIO_FORMAT_AAC_ADTS_LC: return "AUDIO_FORMAT_AAC_ADTS_LC";
                            case AudioFormat::AUDIO_FORMAT_AAC_ADTS_SSR: return "AUDIO_FORMAT_AAC_ADTS_SSR";
                            case AudioFormat::AUDIO_FORMAT_AAC_ADTS_LTP: return "AUDIO_FORMAT_AAC_ADTS_LTP";
                            case AudioFormat::AUDIO_FORMAT_AAC_ADTS_HE_V1: return "AUDIO_FORMAT_AAC_ADTS_HE_V1";
                            case AudioFormat::AUDIO_FORMAT_AAC_ADTS_SCALABLE: return "AUDIO_FORMAT_AAC_ADTS_SCALABLE";
                            case AudioFormat::AUDIO_FORMAT_AAC_ADTS_ERLC: return "AUDIO_FORMAT_AAC_ADTS_ERLC";
                            case AudioFormat::AUDIO_FORMAT_AAC_ADTS_LD: return "AUDIO_FORMAT_AAC_ADTS_LD";
                            case AudioFormat::AUDIO_FORMAT_AAC_ADTS_HE_V2: return "AUDIO_FORMAT_AAC_ADTS_HE_V2";
                            case AudioFormat::AUDIO_FORMAT_AAC_ADTS_ELD: return "AUDIO_FORMAT_AAC_ADTS_ELD";
                            case AudioFormat::AUDIO_FORMAT_AAC_ADTS_XHE: return "AUDIO_FORMAT_AAC_ADTS_XHE";
                            case AudioFormat::AUDIO_FORMAT_SBC: return "AUDIO_FORMAT_SBC";
                            case AudioFormat::AUDIO_FORMAT_APTX: return "AUDIO_FORMAT_APTX";
                            case AudioFormat::AUDIO_FORMAT_APTX_HD: return "AUDIO_FORMAT_APTX_HD";
                            case AudioFormat::AUDIO_FORMAT_AC4: return "AUDIO_FORMAT_AC4";
                            case AudioFormat::AUDIO_FORMAT_LDAC: return "AUDIO_FORMAT_LDAC";
                            case AudioFormat::AUDIO_FORMAT_MAT: return "AUDIO_FORMAT_MAT";
                            case AudioFormat::AUDIO_FORMAT_MAT_1_0: return "AUDIO_FORMAT_MAT_1_0";
                            case AudioFormat::AUDIO_FORMAT_MAT_2_0: return "AUDIO_FORMAT_MAT_2_0";
                            case AudioFormat::AUDIO_FORMAT_MAT_2_1: return "AUDIO_FORMAT_MAT_2_1";
                            case AudioFormat::AUDIO_FORMAT_AAC_LATM: return "AUDIO_FORMAT_AAC_LATM";
                            case AudioFormat::AUDIO_FORMAT_AAC_LATM_LC: return "AUDIO_FORMAT_AAC_LATM_LC";
                            case AudioFormat::AUDIO_FORMAT_AAC_LATM_HE_V1: return "AUDIO_FORMAT_AAC_LATM_HE_V1";
                            case AudioFormat::AUDIO_FORMAT_AAC_LATM_HE_V2: return "AUDIO_FORMAT_AAC_LATM_HE_V2";
                            case AudioFormat::AUDIO_FORMAT_CELT: return "AUDIO_FORMAT_CELT";
                            case AudioFormat::AUDIO_FORMAT_APTX_ADAPTIVE: return "AUDIO_FORMAT_APTX_ADAPTIVE";
                            case AudioFormat::AUDIO_FORMAT_LHDC: return "AUDIO_FORMAT_LHDC";
                            case AudioFormat::AUDIO_FORMAT_LHDC_LL: return "AUDIO_FORMAT_LHDC_LL";
                            case AudioFormat::AUDIO_FORMAT_APTX_TWSP: return "AUDIO_FORMAT_APTX_TWSP";
                            case AudioFormat::AUDIO_FORMAT_LC3: return "AUDIO_FORMAT_LC3";
                            case AudioFormat::AUDIO_FORMAT_MPEGH_BL_L3: return "AUDIO_FORMAT_MPEGH_BL_L3";
                            case AudioFormat::AUDIO_FORMAT_MPEGH_BL_L4: return "AUDIO_FORMAT_MPEGH_BL_L4";
                            case AudioFormat::AUDIO_FORMAT_MPEGH_LC_L3: return "AUDIO_FORMAT_MPEGH_LC_L3";
                            case AudioFormat::AUDIO_FORMAT_MPEGH_LC_L4: return "AUDIO_FORMAT_MPEGH_LC_L4";
                            case AudioFormat::AUDIO_FORMAT_IEC60958: return "AUDIO_FORMAT_IEC60958";
                            case AudioFormat::AUDIO_FORMAT_DTS_UHD: return "AUDIO_FORMAT_DTS_UHD";
                            case AudioFormat::AUDIO_FORMAT_DRA: return "AUDIO_FORMAT_DRA";
                            default: return std::to_string(static_cast<int>(o));
                        }
                    }

                    const std::map<std::string, AudioUsage> AudioUsageString {
                        { "AUDIO_USAGE_UNKNOWN", AudioUsage::AUDIO_USAGE_UNKNOWN },
                        { "AUDIO_USAGE_MEDIA", AudioUsage::AUDIO_USAGE_MEDIA },
                        { "AUDIO_USAGE_VOICE_COMMUNICATION", AudioUsage::AUDIO_USAGE_VOICE_COMMUNICATION },
                        { "AUDIO_USAGE_VOICE_COMMUNICATION_SIGNALLING", AudioUsage::AUDIO_USAGE_VOICE_COMMUNICATION_SIGNALLING },
                        { "AUDIO_USAGE_ALARM", AudioUsage::AUDIO_USAGE_ALARM },
                        { "AUDIO_USAGE_NOTIFICATION", AudioUsage::AUDIO_USAGE_NOTIFICATION },
                        { "AUDIO_USAGE_NOTIFICATION_TELEPHONY_RINGTONE", AudioUsage::AUDIO_USAGE_NOTIFICATION_TELEPHONY_RINGTONE },
                        { "AUDIO_USAGE_ASSISTANCE_ACCESSIBILITY", AudioUsage::AUDIO_USAGE_ASSISTANCE_ACCESSIBILITY },
                        { "AUDIO_USAGE_ASSISTANCE_NAVIGATION_GUIDANCE", AudioUsage::AUDIO_USAGE_ASSISTANCE_NAVIGATION_GUIDANCE },
                        { "AUDIO_USAGE_ASSISTANCE_SONIFICATION", AudioUsage::AUDIO_USAGE_ASSISTANCE_SONIFICATION },
                        { "AUDIO_USAGE_GAME", AudioUsage::AUDIO_USAGE_GAME },
                        { "AUDIO_USAGE_VIRTUAL_SOURCE", AudioUsage::AUDIO_USAGE_VIRTUAL_SOURCE },
                        { "AUDIO_USAGE_ASSISTANT", AudioUsage::AUDIO_USAGE_ASSISTANT },
                        { "AUDIO_USAGE_CALL_ASSISTANT", AudioUsage::AUDIO_USAGE_CALL_ASSISTANT },
                        { "AUDIO_USAGE_EMERGENCY", AudioUsage::AUDIO_USAGE_EMERGENCY },
                        { "AUDIO_USAGE_SAFETY", AudioUsage::AUDIO_USAGE_SAFETY },
                        { "AUDIO_USAGE_VEHICLE_STATUS", AudioUsage::AUDIO_USAGE_VEHICLE_STATUS },
                        { "AUDIO_USAGE_ANNOUNCEMENT", AudioUsage::AUDIO_USAGE_ANNOUNCEMENT },
                    };

                    AudioUsage stringToAudioUsage(const std::string& value) {
                        auto enumValue = AudioUsageString.find(value);
                        return enumValue != AudioUsageString.end() ? enumValue->second : AudioUsage::UNKNOWN;
                    }

                    std::string toString(AudioUsage o) {
                        switch (o) {
                            case AudioUsage::AUDIO_USAGE_UNKNOWN: return "AUDIO_USAGE_UNKNOWN";
                            case AudioUsage::AUDIO_USAGE_MEDIA: return "AUDIO_USAGE_MEDIA";
                            case AudioUsage::AUDIO_USAGE_VOICE_COMMUNICATION: return "AUDIO_USAGE_VOICE_COMMUNICATION";
                            case AudioUsage::AUDIO_USAGE_VOICE_COMMUNICATION_SIGNALLING: return "AUDIO_USAGE_VOICE_COMMUNICATION_SIGNALLING";
                            case AudioUsage::AUDIO_USAGE_ALARM: return "AUDIO_USAGE_ALARM";
                            case AudioUsage::AUDIO_USAGE_NOTIFICATION: return "AUDIO_USAGE_NOTIFICATION";
                            case AudioUsage::AUDIO_USAGE_NOTIFICATION_TELEPHONY_RINGTONE: return "AUDIO_USAGE_NOTIFICATION_TELEPHONY_RINGTONE";
                            case AudioUsage::AUDIO_USAGE_ASSISTANCE_ACCESSIBILITY: return "AUDIO_USAGE_ASSISTANCE_ACCESSIBILITY";
                            case AudioUsage::AUDIO_USAGE_ASSISTANCE_NAVIGATION_GUIDANCE: return "AUDIO_USAGE_ASSISTANCE_NAVIGATION_GUIDANCE";
                            case AudioUsage::AUDIO_USAGE_ASSISTANCE_SONIFICATION: return "AUDIO_USAGE_ASSISTANCE_SONIFICATION";
                            case AudioUsage::AUDIO_USAGE_GAME: return "AUDIO_USAGE_GAME";
                            case AudioUsage::AUDIO_USAGE_VIRTUAL_SOURCE: return "AUDIO_USAGE_VIRTUAL_SOURCE";
                            case AudioUsage::AUDIO_USAGE_ASSISTANT: return "AUDIO_USAGE_ASSISTANT";
                            case AudioUsage::AUDIO_USAGE_CALL_ASSISTANT: return "AUDIO_USAGE_CALL_ASSISTANT";
                            case AudioUsage::AUDIO_USAGE_EMERGENCY: return "AUDIO_USAGE_EMERGENCY";
                            case AudioUsage::AUDIO_USAGE_SAFETY: return "AUDIO_USAGE_SAFETY";
                            case AudioUsage::AUDIO_USAGE_VEHICLE_STATUS: return "AUDIO_USAGE_VEHICLE_STATUS";
                            case AudioUsage::AUDIO_USAGE_ANNOUNCEMENT: return "AUDIO_USAGE_ANNOUNCEMENT";
                            default: return std::to_string(static_cast<int>(o));
                        }
                    }

                    const std::map<std::string, AudioContentType> AudioContentTypeString {
                        { "AUDIO_CONTENT_TYPE_UNKNOWN", AudioContentType::AUDIO_CONTENT_TYPE_UNKNOWN },
                        { "AUDIO_CONTENT_TYPE_SPEECH", AudioContentType::AUDIO_CONTENT_TYPE_SPEECH },
                        { "AUDIO_CONTENT_TYPE_MUSIC", AudioContentType::AUDIO_CONTENT_TYPE_MUSIC },
                        { "AUDIO_CONTENT_TYPE_MOVIE", AudioContentType::AUDIO_CONTENT_TYPE_MOVIE },
                        { "AUDIO_CONTENT_TYPE_SONIFICATION", AudioContentType::AUDIO_CONTENT_TYPE_SONIFICATION },
                    };

                    AudioContentType stringToAudioContentType(const std::string& value) {
                        auto enumValue = AudioContentTypeString.find(value);
                        return enumValue != AudioContentTypeString.end() ? enumValue->second : AudioContentType::UNKNOWN;
                    }

                    std::string toString(AudioContentType o) {
                        switch (o) {
                            case AudioContentType::AUDIO_CONTENT_TYPE_UNKNOWN: return "AUDIO_CONTENT_TYPE_UNKNOWN";
                            case AudioContentType::AUDIO_CONTENT_TYPE_SPEECH: return "AUDIO_CONTENT_TYPE_SPEECH";
                            case AudioContentType::AUDIO_CONTENT_TYPE_MUSIC: return "AUDIO_CONTENT_TYPE_MUSIC";
                            case AudioContentType::AUDIO_CONTENT_TYPE_MOVIE: return "AUDIO_CONTENT_TYPE_MOVIE";
                            case AudioContentType::AUDIO_CONTENT_TYPE_SONIFICATION: return "AUDIO_CONTENT_TYPE_SONIFICATION";
                            default: return std::to_string(static_cast<int>(o));
                        }
                    }

                    const std::map<std::string, AudioChannelMask> AudioChannelMaskString {
                        { "AUDIO_CHANNEL_NONE", AudioChannelMask::AUDIO_CHANNEL_NONE },
                        { "AUDIO_CHANNEL_OUT_MONO", AudioChannelMask::AUDIO_CHANNEL_OUT_MONO },
                        { "AUDIO_CHANNEL_OUT_STEREO", AudioChannelMask::AUDIO_CHANNEL_OUT_STEREO },
                        { "AUDIO_CHANNEL_OUT_2POINT1", AudioChannelMask::AUDIO_CHANNEL_OUT_2POINT1 },
                        { "AUDIO_CHANNEL_OUT_TRI", AudioChannelMask::AUDIO_CHANNEL_OUT_TRI },
                        { "AUDIO_CHANNEL_OUT_TRI_BACK", AudioChannelMask::AUDIO_CHANNEL_OUT_TRI_BACK },
                        { "AUDIO_CHANNEL_OUT_3POINT1", AudioChannelMask::AUDIO_CHANNEL_OUT_3POINT1 },
                        { "AUDIO_CHANNEL_OUT_2POINT0POINT2", AudioChannelMask::AUDIO_CHANNEL_OUT_2POINT0POINT2 },
                        { "AUDIO_CHANNEL_OUT_2POINT1POINT2", AudioChannelMask::AUDIO_CHANNEL_OUT_2POINT1POINT2 },
                        { "AUDIO_CHANNEL_OUT_3POINT0POINT2", AudioChannelMask::AUDIO_CHANNEL_OUT_3POINT0POINT2 },
                        { "AUDIO_CHANNEL_OUT_3POINT1POINT2", AudioChannelMask::AUDIO_CHANNEL_OUT_3POINT1POINT2 },
                        { "AUDIO_CHANNEL_OUT_QUAD", AudioChannelMask::AUDIO_CHANNEL_OUT_QUAD },
                        { "AUDIO_CHANNEL_OUT_QUAD_BACK", AudioChannelMask::AUDIO_CHANNEL_OUT_QUAD_BACK },
                        { "AUDIO_CHANNEL_OUT_QUAD_SIDE", AudioChannelMask::AUDIO_CHANNEL_OUT_QUAD_SIDE },
                        { "AUDIO_CHANNEL_OUT_SURROUND", AudioChannelMask::AUDIO_CHANNEL_OUT_SURROUND },
                        { "AUDIO_CHANNEL_OUT_PENTA", AudioChannelMask::AUDIO_CHANNEL_OUT_PENTA },
                        { "AUDIO_CHANNEL_OUT_5POINT1", AudioChannelMask::AUDIO_CHANNEL_OUT_5POINT1 },
                        { "AUDIO_CHANNEL_OUT_5POINT1_BACK", AudioChannelMask::AUDIO_CHANNEL_OUT_5POINT1_BACK },
                        { "AUDIO_CHANNEL_OUT_5POINT1_SIDE", AudioChannelMask::AUDIO_CHANNEL_OUT_5POINT1_SIDE },
                        { "AUDIO_CHANNEL_OUT_5POINT1POINT2", AudioChannelMask::AUDIO_CHANNEL_OUT_5POINT1POINT2 },
                        { "AUDIO_CHANNEL_OUT_5POINT1POINT4", AudioChannelMask::AUDIO_CHANNEL_OUT_5POINT1POINT4 },
                        { "AUDIO_CHANNEL_OUT_6POINT1", AudioChannelMask::AUDIO_CHANNEL_OUT_6POINT1 },
                        { "AUDIO_CHANNEL_OUT_7POINT1", AudioChannelMask::AUDIO_CHANNEL_OUT_7POINT1 },
                        { "AUDIO_CHANNEL_OUT_7POINT1POINT2", AudioChannelMask::AUDIO_CHANNEL_OUT_7POINT1POINT2 },
                        { "AUDIO_CHANNEL_OUT_7POINT1POINT4", AudioChannelMask::AUDIO_CHANNEL_OUT_7POINT1POINT4 },
                        { "AUDIO_CHANNEL_OUT_13POINT_360RA", AudioChannelMask::AUDIO_CHANNEL_OUT_13POINT_360RA },
                        { "AUDIO_CHANNEL_OUT_22POINT2", AudioChannelMask::AUDIO_CHANNEL_OUT_22POINT2 },
                        { "AUDIO_CHANNEL_OUT_MONO_HAPTIC_A", AudioChannelMask::AUDIO_CHANNEL_OUT_MONO_HAPTIC_A },
                        { "AUDIO_CHANNEL_OUT_STEREO_HAPTIC_A", AudioChannelMask::AUDIO_CHANNEL_OUT_STEREO_HAPTIC_A },
                        { "AUDIO_CHANNEL_OUT_HAPTIC_AB", AudioChannelMask::AUDIO_CHANNEL_OUT_HAPTIC_AB },
                        { "AUDIO_CHANNEL_OUT_MONO_HAPTIC_AB", AudioChannelMask::AUDIO_CHANNEL_OUT_MONO_HAPTIC_AB },
                        { "AUDIO_CHANNEL_OUT_STEREO_HAPTIC_AB", AudioChannelMask::AUDIO_CHANNEL_OUT_STEREO_HAPTIC_AB },
                        { "AUDIO_CHANNEL_IN_MONO", AudioChannelMask::AUDIO_CHANNEL_IN_MONO },
                        { "AUDIO_CHANNEL_IN_STEREO", AudioChannelMask::AUDIO_CHANNEL_IN_STEREO },
                        { "AUDIO_CHANNEL_IN_FRONT_BACK", AudioChannelMask::AUDIO_CHANNEL_IN_FRONT_BACK },
                        { "AUDIO_CHANNEL_IN_6", AudioChannelMask::AUDIO_CHANNEL_IN_6 },
                        { "AUDIO_CHANNEL_IN_2POINT0POINT2", AudioChannelMask::AUDIO_CHANNEL_IN_2POINT0POINT2 },
                        { "AUDIO_CHANNEL_IN_2POINT1POINT2", AudioChannelMask::AUDIO_CHANNEL_IN_2POINT1POINT2 },
                        { "AUDIO_CHANNEL_IN_3POINT0POINT2", AudioChannelMask::AUDIO_CHANNEL_IN_3POINT0POINT2 },
                        { "AUDIO_CHANNEL_IN_3POINT1POINT2", AudioChannelMask::AUDIO_CHANNEL_IN_3POINT1POINT2 },
                        { "AUDIO_CHANNEL_IN_5POINT1", AudioChannelMask::AUDIO_CHANNEL_IN_5POINT1 },
                        { "AUDIO_CHANNEL_IN_VOICE_UPLINK_MONO", AudioChannelMask::AUDIO_CHANNEL_IN_VOICE_UPLINK_MONO },
                        { "AUDIO_CHANNEL_IN_VOICE_DNLINK_MONO", AudioChannelMask::AUDIO_CHANNEL_IN_VOICE_DNLINK_MONO },
                        { "AUDIO_CHANNEL_IN_VOICE_CALL_MONO", AudioChannelMask::AUDIO_CHANNEL_IN_VOICE_CALL_MONO },
                        { "AUDIO_CHANNEL_INDEX_MASK_1", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_1 },
                        { "AUDIO_CHANNEL_INDEX_MASK_2", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_2 },
                        { "AUDIO_CHANNEL_INDEX_MASK_3", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_3 },
                        { "AUDIO_CHANNEL_INDEX_MASK_4", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_4 },
                        { "AUDIO_CHANNEL_INDEX_MASK_5", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_5 },
                        { "AUDIO_CHANNEL_INDEX_MASK_6", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_6 },
                        { "AUDIO_CHANNEL_INDEX_MASK_7", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_7 },
                        { "AUDIO_CHANNEL_INDEX_MASK_8", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_8 },
                        { "AUDIO_CHANNEL_INDEX_MASK_9", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_9 },
                        { "AUDIO_CHANNEL_INDEX_MASK_10", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_10 },
                        { "AUDIO_CHANNEL_INDEX_MASK_11", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_11 },
                        { "AUDIO_CHANNEL_INDEX_MASK_12", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_12 },
                        { "AUDIO_CHANNEL_INDEX_MASK_13", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_13 },
                        { "AUDIO_CHANNEL_INDEX_MASK_14", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_14 },
                        { "AUDIO_CHANNEL_INDEX_MASK_15", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_15 },
                        { "AUDIO_CHANNEL_INDEX_MASK_16", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_16 },
                        { "AUDIO_CHANNEL_INDEX_MASK_17", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_17 },
                        { "AUDIO_CHANNEL_INDEX_MASK_18", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_18 },
                        { "AUDIO_CHANNEL_INDEX_MASK_19", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_19 },
                        { "AUDIO_CHANNEL_INDEX_MASK_20", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_20 },
                        { "AUDIO_CHANNEL_INDEX_MASK_21", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_21 },
                        { "AUDIO_CHANNEL_INDEX_MASK_22", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_22 },
                        { "AUDIO_CHANNEL_INDEX_MASK_23", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_23 },
                        { "AUDIO_CHANNEL_INDEX_MASK_24", AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_24 },
                    };

                    AudioChannelMask stringToAudioChannelMask(const std::string& value) {
                        auto enumValue = AudioChannelMaskString.find(value);
                        return enumValue != AudioChannelMaskString.end() ? enumValue->second : AudioChannelMask::UNKNOWN;
                    }

                    std::string toString(AudioChannelMask o) {
                        switch (o) {
                            case AudioChannelMask::AUDIO_CHANNEL_NONE: return "AUDIO_CHANNEL_NONE";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_MONO: return "AUDIO_CHANNEL_OUT_MONO";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_STEREO: return "AUDIO_CHANNEL_OUT_STEREO";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_2POINT1: return "AUDIO_CHANNEL_OUT_2POINT1";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_TRI: return "AUDIO_CHANNEL_OUT_TRI";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_TRI_BACK: return "AUDIO_CHANNEL_OUT_TRI_BACK";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_3POINT1: return "AUDIO_CHANNEL_OUT_3POINT1";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_2POINT0POINT2: return "AUDIO_CHANNEL_OUT_2POINT0POINT2";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_2POINT1POINT2: return "AUDIO_CHANNEL_OUT_2POINT1POINT2";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_3POINT0POINT2: return "AUDIO_CHANNEL_OUT_3POINT0POINT2";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_3POINT1POINT2: return "AUDIO_CHANNEL_OUT_3POINT1POINT2";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_QUAD: return "AUDIO_CHANNEL_OUT_QUAD";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_QUAD_BACK: return "AUDIO_CHANNEL_OUT_QUAD_BACK";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_QUAD_SIDE: return "AUDIO_CHANNEL_OUT_QUAD_SIDE";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_SURROUND: return "AUDIO_CHANNEL_OUT_SURROUND";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_PENTA: return "AUDIO_CHANNEL_OUT_PENTA";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_5POINT1: return "AUDIO_CHANNEL_OUT_5POINT1";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_5POINT1_BACK: return "AUDIO_CHANNEL_OUT_5POINT1_BACK";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_5POINT1_SIDE: return "AUDIO_CHANNEL_OUT_5POINT1_SIDE";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_5POINT1POINT2: return "AUDIO_CHANNEL_OUT_5POINT1POINT2";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_5POINT1POINT4: return "AUDIO_CHANNEL_OUT_5POINT1POINT4";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_6POINT1: return "AUDIO_CHANNEL_OUT_6POINT1";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_7POINT1: return "AUDIO_CHANNEL_OUT_7POINT1";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_7POINT1POINT2: return "AUDIO_CHANNEL_OUT_7POINT1POINT2";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_7POINT1POINT4: return "AUDIO_CHANNEL_OUT_7POINT1POINT4";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_13POINT_360RA: return "AUDIO_CHANNEL_OUT_13POINT_360RA";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_22POINT2: return "AUDIO_CHANNEL_OUT_22POINT2";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_MONO_HAPTIC_A: return "AUDIO_CHANNEL_OUT_MONO_HAPTIC_A";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_STEREO_HAPTIC_A: return "AUDIO_CHANNEL_OUT_STEREO_HAPTIC_A";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_HAPTIC_AB: return "AUDIO_CHANNEL_OUT_HAPTIC_AB";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_MONO_HAPTIC_AB: return "AUDIO_CHANNEL_OUT_MONO_HAPTIC_AB";
                            case AudioChannelMask::AUDIO_CHANNEL_OUT_STEREO_HAPTIC_AB: return "AUDIO_CHANNEL_OUT_STEREO_HAPTIC_AB";
                            case AudioChannelMask::AUDIO_CHANNEL_IN_MONO: return "AUDIO_CHANNEL_IN_MONO";
                            case AudioChannelMask::AUDIO_CHANNEL_IN_STEREO: return "AUDIO_CHANNEL_IN_STEREO";
                            case AudioChannelMask::AUDIO_CHANNEL_IN_FRONT_BACK: return "AUDIO_CHANNEL_IN_FRONT_BACK";
                            case AudioChannelMask::AUDIO_CHANNEL_IN_6: return "AUDIO_CHANNEL_IN_6";
                            case AudioChannelMask::AUDIO_CHANNEL_IN_2POINT0POINT2: return "AUDIO_CHANNEL_IN_2POINT0POINT2";
                            case AudioChannelMask::AUDIO_CHANNEL_IN_2POINT1POINT2: return "AUDIO_CHANNEL_IN_2POINT1POINT2";
                            case AudioChannelMask::AUDIO_CHANNEL_IN_3POINT0POINT2: return "AUDIO_CHANNEL_IN_3POINT0POINT2";
                            case AudioChannelMask::AUDIO_CHANNEL_IN_3POINT1POINT2: return "AUDIO_CHANNEL_IN_3POINT1POINT2";
                            case AudioChannelMask::AUDIO_CHANNEL_IN_5POINT1: return "AUDIO_CHANNEL_IN_5POINT1";
                            case AudioChannelMask::AUDIO_CHANNEL_IN_VOICE_UPLINK_MONO: return "AUDIO_CHANNEL_IN_VOICE_UPLINK_MONO";
                            case AudioChannelMask::AUDIO_CHANNEL_IN_VOICE_DNLINK_MONO: return "AUDIO_CHANNEL_IN_VOICE_DNLINK_MONO";
                            case AudioChannelMask::AUDIO_CHANNEL_IN_VOICE_CALL_MONO: return "AUDIO_CHANNEL_IN_VOICE_CALL_MONO";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_1: return "AUDIO_CHANNEL_INDEX_MASK_1";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_2: return "AUDIO_CHANNEL_INDEX_MASK_2";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_3: return "AUDIO_CHANNEL_INDEX_MASK_3";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_4: return "AUDIO_CHANNEL_INDEX_MASK_4";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_5: return "AUDIO_CHANNEL_INDEX_MASK_5";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_6: return "AUDIO_CHANNEL_INDEX_MASK_6";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_7: return "AUDIO_CHANNEL_INDEX_MASK_7";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_8: return "AUDIO_CHANNEL_INDEX_MASK_8";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_9: return "AUDIO_CHANNEL_INDEX_MASK_9";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_10: return "AUDIO_CHANNEL_INDEX_MASK_10";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_11: return "AUDIO_CHANNEL_INDEX_MASK_11";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_12: return "AUDIO_CHANNEL_INDEX_MASK_12";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_13: return "AUDIO_CHANNEL_INDEX_MASK_13";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_14: return "AUDIO_CHANNEL_INDEX_MASK_14";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_15: return "AUDIO_CHANNEL_INDEX_MASK_15";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_16: return "AUDIO_CHANNEL_INDEX_MASK_16";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_17: return "AUDIO_CHANNEL_INDEX_MASK_17";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_18: return "AUDIO_CHANNEL_INDEX_MASK_18";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_19: return "AUDIO_CHANNEL_INDEX_MASK_19";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_20: return "AUDIO_CHANNEL_INDEX_MASK_20";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_21: return "AUDIO_CHANNEL_INDEX_MASK_21";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_22: return "AUDIO_CHANNEL_INDEX_MASK_22";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_23: return "AUDIO_CHANNEL_INDEX_MASK_23";
                            case AudioChannelMask::AUDIO_CHANNEL_INDEX_MASK_24: return "AUDIO_CHANNEL_INDEX_MASK_24";
                            default: return std::to_string(static_cast<int>(o));
                        }
                    }

                    const std::map<std::string, AudioEncapsulationType> AudioEncapsulationTypeString {
                        { "AUDIO_ENCAPSULATION_TYPE_NONE", AudioEncapsulationType::AUDIO_ENCAPSULATION_TYPE_NONE },
                        { "AUDIO_ENCAPSULATION_TYPE_IEC61937", AudioEncapsulationType::AUDIO_ENCAPSULATION_TYPE_IEC61937 },
                    };

                    AudioEncapsulationType stringToAudioEncapsulationType(const std::string& value) {
                        auto enumValue = AudioEncapsulationTypeString.find(value);
                        return enumValue != AudioEncapsulationTypeString.end() ? enumValue->second : AudioEncapsulationType::UNKNOWN;
                    }

                    std::string toString(AudioEncapsulationType o) {
                        switch (o) {
                            case AudioEncapsulationType::AUDIO_ENCAPSULATION_TYPE_NONE: return "AUDIO_ENCAPSULATION_TYPE_NONE";
                            case AudioEncapsulationType::AUDIO_ENCAPSULATION_TYPE_IEC61937: return "AUDIO_ENCAPSULATION_TYPE_IEC61937";
                            default: return std::to_string(static_cast<int>(o));
                        }
                    }

                    const std::map<std::string, AudioGainMode> AudioGainModeString {
                        { "AUDIO_GAIN_MODE_JOINT", AudioGainMode::AUDIO_GAIN_MODE_JOINT },
                        { "AUDIO_GAIN_MODE_CHANNELS", AudioGainMode::AUDIO_GAIN_MODE_CHANNELS },
                        { "AUDIO_GAIN_MODE_RAMP", AudioGainMode::AUDIO_GAIN_MODE_RAMP },
                    };

                    AudioGainMode stringToAudioGainMode(const std::string& value) {
                        auto enumValue = AudioGainModeString.find(value);
                        return enumValue != AudioGainModeString.end() ? enumValue->second : AudioGainMode::UNKNOWN;
                    }

                    std::string toString(AudioGainMode o) {
                        switch (o) {
                            case AudioGainMode::AUDIO_GAIN_MODE_JOINT: return "AUDIO_GAIN_MODE_JOINT";
                            case AudioGainMode::AUDIO_GAIN_MODE_CHANNELS: return "AUDIO_GAIN_MODE_CHANNELS";
                            case AudioGainMode::AUDIO_GAIN_MODE_RAMP: return "AUDIO_GAIN_MODE_RAMP";
                            default: return std::to_string(static_cast<int>(o));
                        }
                    }

                    const std::map<std::string, MixType> MixTypeString {
                        { "mix", MixType::mix },
                        { "mux", MixType::mux },
                    };

                    MixType stringToMixType(const std::string& value) {
                        auto enumValue = MixTypeString.find(value);
                        return enumValue != MixTypeString.end() ? enumValue->second : MixType::UNKNOWN;
                    }

                    std::string toString(MixType o) {
                        switch (o) {
                            case MixType::mix: return "mix";
                            case MixType::mux: return "mux";
                            default: return std::to_string(static_cast<int>(o));
                        }
                    }

                    const std::map<std::string, AudioStreamType> AudioStreamTypeString {
                        { "AUDIO_STREAM_VOICE_CALL", AudioStreamType::AUDIO_STREAM_VOICE_CALL },
                        { "AUDIO_STREAM_SYSTEM", AudioStreamType::AUDIO_STREAM_SYSTEM },
                        { "AUDIO_STREAM_RING", AudioStreamType::AUDIO_STREAM_RING },
                        { "AUDIO_STREAM_MUSIC", AudioStreamType::AUDIO_STREAM_MUSIC },
                        { "AUDIO_STREAM_ALARM", AudioStreamType::AUDIO_STREAM_ALARM },
                        { "AUDIO_STREAM_NOTIFICATION", AudioStreamType::AUDIO_STREAM_NOTIFICATION },
                        { "AUDIO_STREAM_BLUETOOTH_SCO", AudioStreamType::AUDIO_STREAM_BLUETOOTH_SCO },
                        { "AUDIO_STREAM_ENFORCED_AUDIBLE", AudioStreamType::AUDIO_STREAM_ENFORCED_AUDIBLE },
                        { "AUDIO_STREAM_DTMF", AudioStreamType::AUDIO_STREAM_DTMF },
                        { "AUDIO_STREAM_TTS", AudioStreamType::AUDIO_STREAM_TTS },
                        { "AUDIO_STREAM_ACCESSIBILITY", AudioStreamType::AUDIO_STREAM_ACCESSIBILITY },
                        { "AUDIO_STREAM_ASSISTANT", AudioStreamType::AUDIO_STREAM_ASSISTANT },
                        { "AUDIO_STREAM_REROUTING", AudioStreamType::AUDIO_STREAM_REROUTING },
                        { "AUDIO_STREAM_PATCH", AudioStreamType::AUDIO_STREAM_PATCH },
                        { "AUDIO_STREAM_CALL_ASSISTANT", AudioStreamType::AUDIO_STREAM_CALL_ASSISTANT },
                    };

                    AudioStreamType stringToAudioStreamType(const std::string& value) {
                        auto enumValue = AudioStreamTypeString.find(value);
                        return enumValue != AudioStreamTypeString.end() ? enumValue->second : AudioStreamType::UNKNOWN;
                    }

                    std::string toString(AudioStreamType o) {
                        switch (o) {
                            case AudioStreamType::AUDIO_STREAM_VOICE_CALL: return "AUDIO_STREAM_VOICE_CALL";
                            case AudioStreamType::AUDIO_STREAM_SYSTEM: return "AUDIO_STREAM_SYSTEM";
                            case AudioStreamType::AUDIO_STREAM_RING: return "AUDIO_STREAM_RING";
                            case AudioStreamType::AUDIO_STREAM_MUSIC: return "AUDIO_STREAM_MUSIC";
                            case AudioStreamType::AUDIO_STREAM_ALARM: return "AUDIO_STREAM_ALARM";
                            case AudioStreamType::AUDIO_STREAM_NOTIFICATION: return "AUDIO_STREAM_NOTIFICATION";
                            case AudioStreamType::AUDIO_STREAM_BLUETOOTH_SCO: return "AUDIO_STREAM_BLUETOOTH_SCO";
                            case AudioStreamType::AUDIO_STREAM_ENFORCED_AUDIBLE: return "AUDIO_STREAM_ENFORCED_AUDIBLE";
                            case AudioStreamType::AUDIO_STREAM_DTMF: return "AUDIO_STREAM_DTMF";
                            case AudioStreamType::AUDIO_STREAM_TTS: return "AUDIO_STREAM_TTS";
                            case AudioStreamType::AUDIO_STREAM_ACCESSIBILITY: return "AUDIO_STREAM_ACCESSIBILITY";
                            case AudioStreamType::AUDIO_STREAM_ASSISTANT: return "AUDIO_STREAM_ASSISTANT";
                            case AudioStreamType::AUDIO_STREAM_REROUTING: return "AUDIO_STREAM_REROUTING";
                            case AudioStreamType::AUDIO_STREAM_PATCH: return "AUDIO_STREAM_PATCH";
                            case AudioStreamType::AUDIO_STREAM_CALL_ASSISTANT: return "AUDIO_STREAM_CALL_ASSISTANT";
                            default: return std::to_string(static_cast<int>(o));
                        }
                    }

                    const std::map<std::string, AudioSource> AudioSourceString {
                        { "AUDIO_SOURCE_DEFAULT", AudioSource::AUDIO_SOURCE_DEFAULT },
                        { "AUDIO_SOURCE_MIC", AudioSource::AUDIO_SOURCE_MIC },
                        { "AUDIO_SOURCE_VOICE_UPLINK", AudioSource::AUDIO_SOURCE_VOICE_UPLINK },
                        { "AUDIO_SOURCE_VOICE_DOWNLINK", AudioSource::AUDIO_SOURCE_VOICE_DOWNLINK },
                        { "AUDIO_SOURCE_VOICE_CALL", AudioSource::AUDIO_SOURCE_VOICE_CALL },
                        { "AUDIO_SOURCE_CAMCORDER", AudioSource::AUDIO_SOURCE_CAMCORDER },
                        { "AUDIO_SOURCE_VOICE_RECOGNITION", AudioSource::AUDIO_SOURCE_VOICE_RECOGNITION },
                        { "AUDIO_SOURCE_VOICE_COMMUNICATION", AudioSource::AUDIO_SOURCE_VOICE_COMMUNICATION },
                        { "AUDIO_SOURCE_REMOTE_SUBMIX", AudioSource::AUDIO_SOURCE_REMOTE_SUBMIX },
                        { "AUDIO_SOURCE_UNPROCESSED", AudioSource::AUDIO_SOURCE_UNPROCESSED },
                        { "AUDIO_SOURCE_VOICE_PERFORMANCE", AudioSource::AUDIO_SOURCE_VOICE_PERFORMANCE },
                        { "AUDIO_SOURCE_ECHO_REFERENCE", AudioSource::AUDIO_SOURCE_ECHO_REFERENCE },
                        { "AUDIO_SOURCE_FM_TUNER", AudioSource::AUDIO_SOURCE_FM_TUNER },
                        { "AUDIO_SOURCE_HOTWORD", AudioSource::AUDIO_SOURCE_HOTWORD },
                    };

                    AudioSource stringToAudioSource(const std::string& value) {
                        auto enumValue = AudioSourceString.find(value);
                        return enumValue != AudioSourceString.end() ? enumValue->second : AudioSource::UNKNOWN;
                    }

                    std::string toString(AudioSource o) {
                        switch (o) {
                            case AudioSource::AUDIO_SOURCE_DEFAULT: return "AUDIO_SOURCE_DEFAULT";
                            case AudioSource::AUDIO_SOURCE_MIC: return "AUDIO_SOURCE_MIC";
                            case AudioSource::AUDIO_SOURCE_VOICE_UPLINK: return "AUDIO_SOURCE_VOICE_UPLINK";
                            case AudioSource::AUDIO_SOURCE_VOICE_DOWNLINK: return "AUDIO_SOURCE_VOICE_DOWNLINK";
                            case AudioSource::AUDIO_SOURCE_VOICE_CALL: return "AUDIO_SOURCE_VOICE_CALL";
                            case AudioSource::AUDIO_SOURCE_CAMCORDER: return "AUDIO_SOURCE_CAMCORDER";
                            case AudioSource::AUDIO_SOURCE_VOICE_RECOGNITION: return "AUDIO_SOURCE_VOICE_RECOGNITION";
                            case AudioSource::AUDIO_SOURCE_VOICE_COMMUNICATION: return "AUDIO_SOURCE_VOICE_COMMUNICATION";
                            case AudioSource::AUDIO_SOURCE_REMOTE_SUBMIX: return "AUDIO_SOURCE_REMOTE_SUBMIX";
                            case AudioSource::AUDIO_SOURCE_UNPROCESSED: return "AUDIO_SOURCE_UNPROCESSED";
                            case AudioSource::AUDIO_SOURCE_VOICE_PERFORMANCE: return "AUDIO_SOURCE_VOICE_PERFORMANCE";
                            case AudioSource::AUDIO_SOURCE_ECHO_REFERENCE: return "AUDIO_SOURCE_ECHO_REFERENCE";
                            case AudioSource::AUDIO_SOURCE_FM_TUNER: return "AUDIO_SOURCE_FM_TUNER";
                            case AudioSource::AUDIO_SOURCE_HOTWORD: return "AUDIO_SOURCE_HOTWORD";
                            default: return std::to_string(static_cast<int>(o));
                        }
                    }

                    const std::map<std::string, DeviceCategory> DeviceCategoryString {
                        { "DEVICE_CATEGORY_HEADSET", DeviceCategory::DEVICE_CATEGORY_HEADSET },
                        { "DEVICE_CATEGORY_SPEAKER", DeviceCategory::DEVICE_CATEGORY_SPEAKER },
                        { "DEVICE_CATEGORY_EARPIECE", DeviceCategory::DEVICE_CATEGORY_EARPIECE },
                        { "DEVICE_CATEGORY_EXT_MEDIA", DeviceCategory::DEVICE_CATEGORY_EXT_MEDIA },
                        { "DEVICE_CATEGORY_HEARING_AID", DeviceCategory::DEVICE_CATEGORY_HEARING_AID },
                    };

                    DeviceCategory stringToDeviceCategory(const std::string& value) {
                        auto enumValue = DeviceCategoryString.find(value);
                        return enumValue != DeviceCategoryString.end() ? enumValue->second : DeviceCategory::UNKNOWN;
                    }

                    std::string toString(DeviceCategory o) {
                        switch (o) {
                            case DeviceCategory::DEVICE_CATEGORY_HEADSET: return "DEVICE_CATEGORY_HEADSET";
                            case DeviceCategory::DEVICE_CATEGORY_SPEAKER: return "DEVICE_CATEGORY_SPEAKER";
                            case DeviceCategory::DEVICE_CATEGORY_EARPIECE: return "DEVICE_CATEGORY_EARPIECE";
                            case DeviceCategory::DEVICE_CATEGORY_EXT_MEDIA: return "DEVICE_CATEGORY_EXT_MEDIA";
                            case DeviceCategory::DEVICE_CATEGORY_HEARING_AID: return "DEVICE_CATEGORY_HEARING_AID";
                            default: return std::to_string(static_cast<int>(o));
                        }
                    }

                    const std::map<std::string, EngineSuffix> EngineSuffixString {
                        { "default", EngineSuffix::_default },
                        { "configurable", EngineSuffix::configurable },
                    };

                    EngineSuffix stringToEngineSuffix(const std::string& value) {
                        auto enumValue = EngineSuffixString.find(value);
                        return enumValue != EngineSuffixString.end() ? enumValue->second : EngineSuffix::UNKNOWN;
                    }

                    std::string toString(EngineSuffix o) {
                        switch (o) {
                            case EngineSuffix::_default: return "default";
                            case EngineSuffix::configurable: return "configurable";
                            default: return std::to_string(static_cast<int>(o));
                        }
                    }

                } // V7_0
            } // configuration
        } // policy
    } // audio
} // android
