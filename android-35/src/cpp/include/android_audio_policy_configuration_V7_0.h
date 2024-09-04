#ifndef ANDROID_AUDIO_POLICY_CONFIGURATION_V7_0_H
#define ANDROID_AUDIO_POLICY_CONFIGURATION_V7_0_H

#include <array>
#include <map>
#include <optional>
#include <string>
#include <vector>
#include <sstream>

#if __has_include(<libxml/parser.h>)
#include <libxml/parser.h>
#include <libxml/xinclude.h>
#else
#error Require libxml2 library. Please add libxml2 to shared_libs or static_libs
#endif

#include "android_audio_policy_configuration_V7_0_enums.h"

namespace android {
    namespace audio {
        namespace policy {
            namespace configuration {
                namespace V7_0 {
                    class GlobalConfiguration;
                    class Modules;
                    class AttachedDevices;
                    class MixPorts;
                    class Profile;
                    class Gains;
                    class DevicePorts;
                    class Routes;
                    class Volumes;
                    class Volume;
                    class Reference;
                    class SurroundSound;
                    class SurroundFormats;
                    class AudioPolicyConfiguration;
                    std::optional<AudioPolicyConfiguration> read(const char* configFile);

                    std::optional<AudioPolicyConfiguration> parse(const char* xml);

                    class GlobalConfiguration {
                        private:
                        const bool speaker_drc_enabled_;
                        const std::optional<bool> call_screen_mode_supported_;
                        const std::optional<EngineSuffix> engine_library_;
                        public:
                        GlobalConfiguration(bool speaker_drc_enabled, std::optional<bool> call_screen_mode_supported, std::optional<EngineSuffix> engine_library);
                        const bool& getSpeaker_drc_enabled() const;
                        bool hasSpeaker_drc_enabled() const;
                        const bool& getCall_screen_mode_supported() const;
                        bool hasCall_screen_mode_supported() const;
                        const EngineSuffix& getEngine_library() const;
                        bool hasEngine_library() const;
                        static GlobalConfiguration read(xmlNode *root);
                    };

                    class Modules {
                        public:
                        class Module {
                            private:
                            const std::vector<AttachedDevices> attachedDevices_;
                            const std::optional<std::string> defaultOutputDevice_;
                            const std::vector<MixPorts> mixPorts_;
                            const std::vector<DevicePorts> devicePorts_;
                            const std::vector<Routes> routes_;
                            const std::string name_;
                            const HalVersion halVersion_;
                            public:
                            Module(std::vector<AttachedDevices> attachedDevices, std::optional<std::string> defaultOutputDevice, std::vector<MixPorts> mixPorts, std::vector<DevicePorts> devicePorts, std::vector<Routes> routes, std::string name, HalVersion halVersion);
                            const std::vector<AttachedDevices>& getAttachedDevices() const;
                            bool hasAttachedDevices() const;
                            const AttachedDevices* getFirstAttachedDevices() const;
                            const std::string& getDefaultOutputDevice() const;
                            bool hasDefaultOutputDevice() const;
                            const std::vector<MixPorts>& getMixPorts() const;
                            bool hasMixPorts() const;
                            const MixPorts* getFirstMixPorts() const;
                            const std::vector<DevicePorts>& getDevicePorts() const;
                            bool hasDevicePorts() const;
                            const DevicePorts* getFirstDevicePorts() const;
                            const std::vector<Routes>& getRoutes() const;
                            bool hasRoutes() const;
                            const Routes* getFirstRoutes() const;
                            const std::string& getName() const;
                            bool hasName() const;
                            const HalVersion& getHalVersion() const;
                            bool hasHalVersion() const;
                            static Modules::Module read(xmlNode *root);
                        };


                        private:
                        const std::vector<Modules::Module> _module_;
                        public:
                        explicit Modules(std::vector<Module> _module);
                        const std::vector<Modules::Module>& get_module() const;
                        bool has_module() const;
                        const Modules::Module* getFirst_module() const;
                        static Modules read(xmlNode *root);
                    };

                    class AttachedDevices {
                        private:
                        const std::vector<std::string> item_;
                        public:
                        explicit AttachedDevices(std::vector<std::string> item);
                        const std::vector<std::string>& getItem() const;
                        bool hasItem() const;
                        const std::string* getFirstItem() const;
                        static AttachedDevices read(xmlNode *root);
                    };

                    class MixPorts {
                        public:
                        class MixPort {
                            private:
                            const std::vector<Profile> profile_;
                            const std::vector<Gains> gains_;
                            const std::string name_;
                            const Role role_;
                            const std::optional<std::vector<AudioInOutFlag>> flags_;
                            const std::optional<unsigned int> maxOpenCount_;
                            const std::optional<unsigned int> maxActiveCount_;
                            const std::optional<std::vector<AudioUsage>> preferredUsage_;
                            public:
                            MixPort(std::vector<Profile> profile, std::vector<Gains> gains, std::string name, Role role, std::optional<std::vector<AudioInOutFlag>> flags, std::optional<unsigned int> maxOpenCount, std::optional<unsigned int> maxActiveCount, std::optional<std::vector<AudioUsage>> preferredUsage);
                            const std::vector<Profile>& getProfile() const;
                            bool hasProfile() const;
                            const Profile* getFirstProfile() const;
                            const std::vector<Gains>& getGains() const;
                            bool hasGains() const;
                            const Gains* getFirstGains() const;
                            const std::string& getName() const;
                            bool hasName() const;
                            const Role& getRole() const;
                            bool hasRole() const;
                            const std::vector<AudioInOutFlag>& getFlags() const;
                            bool hasFlags() const;
                            const unsigned int& getMaxOpenCount() const;
                            bool hasMaxOpenCount() const;
                            const unsigned int& getMaxActiveCount() const;
                            bool hasMaxActiveCount() const;
                            const std::vector<AudioUsage>& getPreferredUsage() const;
                            bool hasPreferredUsage() const;
                            static MixPorts::MixPort read(xmlNode *root);
                        };


                        private:
                        const std::vector<MixPorts::MixPort> mixPort_;
                        public:
                        explicit MixPorts(std::vector<MixPort> mixPort);
                        const std::vector<MixPorts::MixPort>& getMixPort() const;
                        bool hasMixPort() const;
                        const MixPorts::MixPort* getFirstMixPort() const;
                        static MixPorts read(xmlNode *root);
                    };

                    class Profile {
                        private:
                        const std::optional<std::string> name_;
                        const std::optional<std::string> format_;
                        const std::optional<std::vector<int64_t>> samplingRates_;
                        const std::optional<std::vector<AudioChannelMask>> channelMasks_;
                        const std::optional<AudioEncapsulationType> encapsulationType_;
                        public:
                        Profile(std::optional<std::string> name, std::optional<std::string> format, std::optional<std::vector<int64_t>> samplingRates, std::optional<std::vector<AudioChannelMask>> channelMasks, std::optional<AudioEncapsulationType> encapsulationType);
                        const std::string& getName() const;
                        bool hasName() const;
                        const std::string& getFormat() const;
                        bool hasFormat() const;
                        const std::vector<int64_t>& getSamplingRates() const;
                        bool hasSamplingRates() const;
                        const std::vector<AudioChannelMask>& getChannelMasks() const;
                        bool hasChannelMasks() const;
                        const AudioEncapsulationType& getEncapsulationType() const;
                        bool hasEncapsulationType() const;
                        static Profile read(xmlNode *root);
                    };

                    class Gains {
                        public:
                        class Gain {
                            private:
                            const std::string name_;
                            const std::vector<AudioGainMode> mode_;
                            const std::optional<AudioChannelMask> channel_mask_;
                            const std::optional<int> minValueMB_;
                            const std::optional<int> maxValueMB_;
                            const std::optional<int> defaultValueMB_;
                            const std::optional<int> stepValueMB_;
                            const std::optional<int> minRampMs_;
                            const std::optional<int> maxRampMs_;
                            const std::optional<bool> useForVolume_;
                            public:
                            Gain(std::string name, std::vector<AudioGainMode> mode, std::optional<AudioChannelMask> channel_mask, std::optional<int> minValueMB, std::optional<int> maxValueMB, std::optional<int> defaultValueMB, std::optional<int> stepValueMB, std::optional<int> minRampMs, std::optional<int> maxRampMs, std::optional<bool> useForVolume);
                            const std::string& getName() const;
                            bool hasName() const;
                            const std::vector<AudioGainMode>& getMode() const;
                            bool hasMode() const;
                            const AudioChannelMask& getChannel_mask() const;
                            bool hasChannel_mask() const;
                            const int& getMinValueMB() const;
                            bool hasMinValueMB() const;
                            const int& getMaxValueMB() const;
                            bool hasMaxValueMB() const;
                            const int& getDefaultValueMB() const;
                            bool hasDefaultValueMB() const;
                            const int& getStepValueMB() const;
                            bool hasStepValueMB() const;
                            const int& getMinRampMs() const;
                            bool hasMinRampMs() const;
                            const int& getMaxRampMs() const;
                            bool hasMaxRampMs() const;
                            const bool& getUseForVolume() const;
                            bool hasUseForVolume() const;
                            static Gains::Gain read(xmlNode *root);
                        };


                        private:
                        const std::vector<Gains::Gain> gain_;
                        public:
                        explicit Gains(std::vector<Gain> gain);
                        const std::vector<Gains::Gain>& getGain() const;
                        bool hasGain() const;
                        const Gains::Gain* getFirstGain() const;
                        static Gains read(xmlNode *root);
                    };

                    class DevicePorts {
                        public:
                        class DevicePort {
                            private:
                            const std::vector<Profile> profile_;
                            const std::vector<Gains> gains_;
                            const std::string tagName_;
                            const std::string type_;
                            const Role role_;
                            const std::optional<std::string> address_;
                            const std::optional<bool> _default_;
                            const std::optional<std::vector<std::string>> encodedFormats_;
                            public:
                            DevicePort(std::vector<Profile> profile, std::vector<Gains> gains, std::string tagName, std::string type, Role role, std::optional<std::string> address, std::optional<bool> _default, std::optional<std::vector<std::string>> encodedFormats);
                            const std::vector<Profile>& getProfile() const;
                            bool hasProfile() const;
                            const Profile* getFirstProfile() const;
                            const std::vector<Gains>& getGains() const;
                            bool hasGains() const;
                            const Gains* getFirstGains() const;
                            const std::string& getTagName() const;
                            bool hasTagName() const;
                            const std::string& getType() const;
                            bool hasType() const;
                            const Role& getRole() const;
                            bool hasRole() const;
                            const std::string& getAddress() const;
                            bool hasAddress() const;
                            const bool& get_default() const;
                            bool has_default() const;
                            const std::vector<std::string>& getEncodedFormats() const;
                            bool hasEncodedFormats() const;
                            static DevicePorts::DevicePort read(xmlNode *root);
                        };


                        private:
                        const std::vector<DevicePorts::DevicePort> devicePort_;
                        public:
                        explicit DevicePorts(std::vector<DevicePort> devicePort);
                        const std::vector<DevicePorts::DevicePort>& getDevicePort() const;
                        bool hasDevicePort() const;
                        const DevicePorts::DevicePort* getFirstDevicePort() const;
                        static DevicePorts read(xmlNode *root);
                    };

                    class Routes {
                        public:
                        class Route {
                            private:
                            const MixType type_;
                            const std::string sink_;
                            const std::string sources_;
                            public:
                            Route(MixType type, std::string sink, std::string sources);
                            const MixType& getType() const;
                            bool hasType() const;
                            const std::string& getSink() const;
                            bool hasSink() const;
                            const std::string& getSources() const;
                            bool hasSources() const;
                            static Routes::Route read(xmlNode *root);
                        };


                        private:
                        const std::vector<Routes::Route> route_;
                        public:
                        explicit Routes(std::vector<Route> route);
                        const std::vector<Routes::Route>& getRoute() const;
                        bool hasRoute() const;
                        const Routes::Route* getFirstRoute() const;
                        static Routes read(xmlNode *root);
                    };

                    class Volumes {
                        private:
                        const std::vector<Volume> volume_;
                        const std::vector<Reference> reference_;
                        public:
                        Volumes(std::vector<Volume> volume, std::vector<Reference> reference);
                        const std::vector<Volume>& getVolume() const;
                        bool hasVolume() const;
                        const Volume* getFirstVolume() const;
                        const std::vector<Reference>& getReference() const;
                        bool hasReference() const;
                        const Reference* getFirstReference() const;
                        static Volumes read(xmlNode *root);
                    };

                    class Volume {
                        private:
                        const std::vector<std::string> point_;
                        const std::optional<AudioStreamType> stream_;
                        const std::optional<DeviceCategory> deviceCategory_;
                        const std::optional<std::string> ref_;
                        public:
                        Volume(std::vector<std::string> point, std::optional<AudioStreamType> stream, std::optional<DeviceCategory> deviceCategory, std::optional<std::string> ref);
                        const std::vector<std::string>& getPoint() const;
                        bool hasPoint() const;
                        const std::string* getFirstPoint() const;
                        const AudioStreamType& getStream() const;
                        bool hasStream() const;
                        const DeviceCategory& getDeviceCategory() const;
                        bool hasDeviceCategory() const;
                        const std::string& getRef() const;
                        bool hasRef() const;
                        static Volume read(xmlNode *root);
                    };

                    class Reference {
                        private:
                        const std::vector<std::string> point_;
                        const std::string name_;
                        public:
                        Reference(std::vector<std::string> point, std::string name);
                        const std::vector<std::string>& getPoint() const;
                        bool hasPoint() const;
                        const std::string* getFirstPoint() const;
                        const std::string& getName() const;
                        bool hasName() const;
                        static Reference read(xmlNode *root);
                    };

                    class SurroundSound {
                        private:
                        const std::vector<SurroundFormats> formats_;
                        public:
                        explicit SurroundSound(std::vector<SurroundFormats> formats);
                        const std::vector<SurroundFormats>& getFormats() const;
                        bool hasFormats() const;
                        const SurroundFormats* getFirstFormats() const;
                        static SurroundSound read(xmlNode *root);
                    };

                    class SurroundFormats {
                        public:
                        class Format {
                            private:
                            const std::string name_;
                            const std::optional<std::vector<std::string>> subformats_;
                            public:
                            Format(std::string name, std::optional<std::vector<std::string>> subformats);
                            const std::string& getName() const;
                            bool hasName() const;
                            const std::vector<std::string>& getSubformats() const;
                            bool hasSubformats() const;
                            static SurroundFormats::Format read(xmlNode *root);
                        };


                        private:
                        const std::vector<SurroundFormats::Format> format_;
                        public:
                        explicit SurroundFormats(std::vector<Format> format);
                        const std::vector<SurroundFormats::Format>& getFormat() const;
                        bool hasFormat() const;
                        const SurroundFormats::Format* getFirstFormat() const;
                        static SurroundFormats read(xmlNode *root);
                    };

                    class AudioPolicyConfiguration {
                        private:
                        const std::vector<GlobalConfiguration> globalConfiguration_;
                        const std::vector<Modules> modules_;
                        const std::vector<Volumes> volumes_;
                        const std::vector<SurroundSound> surroundSound_;
                        const std::optional<Version> version_;
                        public:
                        AudioPolicyConfiguration(std::vector<GlobalConfiguration> globalConfiguration, std::vector<Modules> modules, std::vector<Volumes> volumes, std::vector<SurroundSound> surroundSound, std::optional<Version> version);
                        const std::vector<GlobalConfiguration>& getGlobalConfiguration() const;
                        bool hasGlobalConfiguration() const;
                        const GlobalConfiguration* getFirstGlobalConfiguration() const;
                        const std::vector<Modules>& getModules() const;
                        bool hasModules() const;
                        const Modules* getFirstModules() const;
                        const std::vector<Volumes>& getVolumes() const;
                        bool hasVolumes() const;
                        const Volumes* getFirstVolumes() const;
                        const std::vector<SurroundSound>& getSurroundSound() const;
                        bool hasSurroundSound() const;
                        const SurroundSound* getFirstSurroundSound() const;
                        const Version& getVersion() const;
                        bool hasVersion() const;
                        static AudioPolicyConfiguration read(xmlNode *root);
                    };

                } // V7_0
            } // configuration
        } // policy
    } // audio
} // android
#endif // ANDROID_AUDIO_POLICY_CONFIGURATION_V7_0_H
