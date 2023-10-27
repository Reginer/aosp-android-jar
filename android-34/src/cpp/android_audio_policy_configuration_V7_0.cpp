#define LOG_TAG "android.audio.policy.configuration.V7_0"
#include "android_audio_policy_configuration_V7_0.h"

#include <assert.h>
#ifndef __BIONIC__
#define __assert2(f,n,fun,e) do { fprintf(stderr, "%s:%d: %s: Assertion `%s' failed", (f), (n), (fun), (e)); abort(); } while (false)
#endif
#define _xsdc_assert(e) do if (!(e)) __assert2(__FILE__, __LINE__, __PRETTY_FUNCTION__, #e); while (false)

namespace android {
    namespace audio {
        namespace policy {
            namespace configuration {
                namespace V7_0 {
                    template <class T>
                    constexpr void (*xmlDeleter)(T* t);
                    template <>
                    constexpr auto xmlDeleter<xmlDoc> = xmlFreeDoc;
                    template <>
                    auto xmlDeleter<xmlChar> = [](xmlChar *s) { xmlFree(s); };

                    template <class T>
                    constexpr auto make_xmlUnique(T *t) {
                        auto deleter = [](T *t) { xmlDeleter<T>(t); };
                        return std::unique_ptr<T, decltype(deleter)>{t, deleter};
                    }

                    static std::string getXmlAttribute(const xmlNode *cur, const char *attribute) {
                        auto xmlValue = make_xmlUnique(xmlGetProp(cur, reinterpret_cast<const xmlChar*>(attribute)));
                        if (xmlValue == nullptr) {
                            return "";
                        }
                        std::string value(reinterpret_cast<const char*>(xmlValue.get()));
                        return value;
                    }

                    std::optional<AudioPolicyConfiguration> read(const char* configFile) {
                        auto doc = make_xmlUnique(xmlParseFile(configFile));
                        if (doc == nullptr) {
                            return std::nullopt;
                        }
                        xmlNodePtr _child = xmlDocGetRootElement(doc.get());
                        if (_child == nullptr) {
                            return std::nullopt;
                        }
                        if (xmlXIncludeProcess(doc.get()) < 0) {
                            return std::nullopt;
                        }

                        if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("audioPolicyConfiguration"))) {
                            AudioPolicyConfiguration _value = AudioPolicyConfiguration::read(_child);
                            return _value;
                        }
                        return std::nullopt;
                    }

                    std::optional<AudioPolicyConfiguration> parse(const char* xml) {
                        auto doc = make_xmlUnique(xmlParseDoc(reinterpret_cast<const xmlChar*>(xml)));
                        if (doc == nullptr) {
                            return std::nullopt;
                        }
                        xmlNodePtr _child = xmlDocGetRootElement(doc.get());
                        if (_child == nullptr) {
                            return std::nullopt;
                        }
                        if (xmlXIncludeProcess(doc.get()) < 0) {
                            return std::nullopt;
                        }

                        if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("audioPolicyConfiguration"))) {
                            AudioPolicyConfiguration _value = AudioPolicyConfiguration::read(_child);
                            return _value;
                        }
                        return std::nullopt;
                    }


                    GlobalConfiguration::GlobalConfiguration(bool speaker_drc_enabled, std::optional<bool> call_screen_mode_supported, std::optional<EngineSuffix> engine_library) : speaker_drc_enabled_(speaker_drc_enabled), call_screen_mode_supported_(call_screen_mode_supported), engine_library_(engine_library) {
                    }

                    const bool& GlobalConfiguration::getSpeaker_drc_enabled() const {
                        return speaker_drc_enabled_;
                    }

                    bool GlobalConfiguration::hasSpeaker_drc_enabled() const {
                        return true;
                    }

                    const bool& GlobalConfiguration::getCall_screen_mode_supported() const {
                        _xsdc_assert(hasCall_screen_mode_supported());
                        return call_screen_mode_supported_.value();
                    }

                    bool GlobalConfiguration::hasCall_screen_mode_supported() const {
                        return call_screen_mode_supported_.has_value();
                    }

                    const EngineSuffix& GlobalConfiguration::getEngine_library() const {
                        _xsdc_assert(hasEngine_library());
                        return engine_library_.value();
                    }

                    bool GlobalConfiguration::hasEngine_library() const {
                        return engine_library_.has_value();
                    }

                    GlobalConfiguration GlobalConfiguration::read(xmlNode *root) {
                        std::string _raw;
                        _raw = getXmlAttribute(root, "speaker_drc_enabled");
                        bool speaker_drc_enabled{};
                        if (_raw != "") {
                            bool _value = _raw == "true";
                            speaker_drc_enabled = _value;
                        }
                        _raw = getXmlAttribute(root, "call_screen_mode_supported");
                        std::optional<bool> call_screen_mode_supported = std::nullopt;
                        if (_raw != "") {
                            bool _value = _raw == "true";
                            call_screen_mode_supported = _value;
                        }
                        _raw = getXmlAttribute(root, "engine_library");
                        std::optional<EngineSuffix> engine_library = std::nullopt;
                        if (_raw != "") {
                            EngineSuffix _value = stringToEngineSuffix(_raw);
                            engine_library = _value;
                        }
                        GlobalConfiguration instance(speaker_drc_enabled, call_screen_mode_supported, engine_library);
                        return instance;
                    }

                    Modules::Module::Module(std::vector<AttachedDevices> attachedDevices, std::optional<std::string> defaultOutputDevice, std::vector<MixPorts> mixPorts, std::vector<DevicePorts> devicePorts, std::vector<Routes> routes, std::string name, HalVersion halVersion) : attachedDevices_(std::move(attachedDevices)), defaultOutputDevice_(std::move(defaultOutputDevice)), mixPorts_(std::move(mixPorts)), devicePorts_(std::move(devicePorts)), routes_(std::move(routes)), name_(std::move(name)), halVersion_(halVersion) {
                    }

                    const std::vector<AttachedDevices>& Modules::Module::getAttachedDevices() const {
                        return attachedDevices_;
                    }

                    bool Modules::Module::hasAttachedDevices() const {
                        return !(attachedDevices_.empty());
                    }

                    const AttachedDevices* Modules::Module::getFirstAttachedDevices() const {
                        if (attachedDevices_.empty()) {
                            return nullptr;
                        }
                        return &attachedDevices_[0];
                    }

                    const std::string& Modules::Module::getDefaultOutputDevice() const {
                        _xsdc_assert(hasDefaultOutputDevice());
                        return defaultOutputDevice_.value();
                    }

                    bool Modules::Module::hasDefaultOutputDevice() const {
                        return defaultOutputDevice_.has_value();
                    }

                    const std::vector<MixPorts>& Modules::Module::getMixPorts() const {
                        return mixPorts_;
                    }

                    bool Modules::Module::hasMixPorts() const {
                        return !(mixPorts_.empty());
                    }

                    const MixPorts* Modules::Module::getFirstMixPorts() const {
                        if (mixPorts_.empty()) {
                            return nullptr;
                        }
                        return &mixPorts_[0];
                    }

                    const std::vector<DevicePorts>& Modules::Module::getDevicePorts() const {
                        return devicePorts_;
                    }

                    bool Modules::Module::hasDevicePorts() const {
                        return !(devicePorts_.empty());
                    }

                    const DevicePorts* Modules::Module::getFirstDevicePorts() const {
                        if (devicePorts_.empty()) {
                            return nullptr;
                        }
                        return &devicePorts_[0];
                    }

                    const std::vector<Routes>& Modules::Module::getRoutes() const {
                        return routes_;
                    }

                    bool Modules::Module::hasRoutes() const {
                        return !(routes_.empty());
                    }

                    const Routes* Modules::Module::getFirstRoutes() const {
                        if (routes_.empty()) {
                            return nullptr;
                        }
                        return &routes_[0];
                    }

                    const std::string& Modules::Module::getName() const {
                        return name_;
                    }

                    bool Modules::Module::hasName() const {
                        return true;
                    }

                    const HalVersion& Modules::Module::getHalVersion() const {
                        return halVersion_;
                    }

                    bool Modules::Module::hasHalVersion() const {
                        return true;
                    }

                    Modules::Module Modules::Module::read(xmlNode *root) {
                        std::string _raw;
                        _raw = getXmlAttribute(root, "name");
                        std::string name{};
                        if (_raw != "") {
                            std::string &_value = _raw;
                            name = _value;
                        }
                        _raw = getXmlAttribute(root, "halVersion");
                        HalVersion halVersion = HalVersion::UNKNOWN;
                        if (_raw != "") {
                            HalVersion _value = stringToHalVersion(_raw);
                            halVersion = _value;
                        }
                        std::vector<AttachedDevices> attachedDevices;
                        std::optional<std::string> defaultOutputDevice;
                        std::vector<MixPorts> mixPorts;
                        std::vector<DevicePorts> devicePorts;
                        std::vector<Routes> routes;
                        for (auto *_child = root->xmlChildrenNode; _child != nullptr; _child = _child->next) {
                            if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("attachedDevices"))) {
                                AttachedDevices _value = AttachedDevices::read(_child);
                                attachedDevices.push_back(std::move(_value));
                            } else if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("defaultOutputDevice"))) {
                                auto xmlValue = make_xmlUnique(xmlNodeListGetString(_child->doc, _child->xmlChildrenNode, 1));
                                if (xmlValue == nullptr) {
                                    _raw = "";
                                } else {
                                    _raw = reinterpret_cast<const char*>(xmlValue.get());
                                }
                                std::string &_value = _raw;
                                defaultOutputDevice = std::move(_value);
                            } else if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("mixPorts"))) {
                                MixPorts _value = MixPorts::read(_child);
                                mixPorts.push_back(std::move(_value));
                            } else if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("devicePorts"))) {
                                DevicePorts _value = DevicePorts::read(_child);
                                devicePorts.push_back(std::move(_value));
                            } else if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("routes"))) {
                                Routes _value = Routes::read(_child);
                                routes.push_back(std::move(_value));
                            }
                        }
                        Modules::Module instance(attachedDevices, defaultOutputDevice, mixPorts, devicePorts, routes, name, halVersion);
                        return instance;
                    }

                    Modules::Modules(std::vector<Module> _module) : _module_(std::move(_module)) {
                    }

                    const std::vector<Modules::Module>& Modules::get_module() const {
                        return _module_;
                    }

                    bool Modules::has_module() const {
                        return !(_module_.empty());
                    }

                    const Modules::Module* Modules::getFirst_module() const {
                        if (_module_.empty()) {
                            return nullptr;
                        }
                        return &_module_[0];
                    }

                    Modules Modules::read(xmlNode *root) {
                        std::string _raw;
                        std::vector<Module> _module;
                        for (auto *_child = root->xmlChildrenNode; _child != nullptr; _child = _child->next) {
                            if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("module"))) {
                                Module _value = Module::read(_child);
                                _module.push_back(std::move(_value));
                            }
                        }
                        Modules instance(_module);
                        return instance;
                    }

                    AttachedDevices::AttachedDevices(std::vector<std::string> item) : item_(std::move(item)) {
                    }

                    const std::vector<std::string>& AttachedDevices::getItem() const {
                        return item_;
                    }

                    bool AttachedDevices::hasItem() const {
                        return !(item_.empty());
                    }

                    const std::string* AttachedDevices::getFirstItem() const {
                        if (item_.empty()) {
                            return nullptr;
                        }
                        return &item_[0];
                    }

                    AttachedDevices AttachedDevices::read(xmlNode *root) {
                        std::string _raw;
                        std::vector<std::string> item;
                        for (auto *_child = root->xmlChildrenNode; _child != nullptr; _child = _child->next) {
                            if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("item"))) {
                                auto xmlValue = make_xmlUnique(xmlNodeListGetString(_child->doc, _child->xmlChildrenNode, 1));
                                if (xmlValue == nullptr) {
                                    _raw = "";
                                } else {
                                    _raw = reinterpret_cast<const char*>(xmlValue.get());
                                }
                                std::string &_value = _raw;
                                item.push_back(std::move(_value));
                            }
                        }
                        AttachedDevices instance(item);
                        return instance;
                    }

                    MixPorts::MixPort::MixPort(std::vector<Profile> profile, std::vector<Gains> gains, std::string name, Role role, std::optional<std::vector<AudioInOutFlag>> flags, std::optional<unsigned int> maxOpenCount, std::optional<unsigned int> maxActiveCount, std::optional<std::vector<AudioUsage>> preferredUsage) : profile_(std::move(profile)), gains_(std::move(gains)), name_(std::move(name)), role_(role), flags_(std::move(flags)), maxOpenCount_(maxOpenCount), maxActiveCount_(maxActiveCount), preferredUsage_(std::move(preferredUsage)) {
                    }

                    const std::vector<Profile>& MixPorts::MixPort::getProfile() const {
                        return profile_;
                    }

                    bool MixPorts::MixPort::hasProfile() const {
                        return !(profile_.empty());
                    }

                    const Profile* MixPorts::MixPort::getFirstProfile() const {
                        if (profile_.empty()) {
                            return nullptr;
                        }
                        return &profile_[0];
                    }

                    const std::vector<Gains>& MixPorts::MixPort::getGains() const {
                        return gains_;
                    }

                    bool MixPorts::MixPort::hasGains() const {
                        return !(gains_.empty());
                    }

                    const Gains* MixPorts::MixPort::getFirstGains() const {
                        if (gains_.empty()) {
                            return nullptr;
                        }
                        return &gains_[0];
                    }

                    const std::string& MixPorts::MixPort::getName() const {
                        return name_;
                    }

                    bool MixPorts::MixPort::hasName() const {
                        return true;
                    }

                    const Role& MixPorts::MixPort::getRole() const {
                        return role_;
                    }

                    bool MixPorts::MixPort::hasRole() const {
                        return true;
                    }

                    const std::vector<AudioInOutFlag>& MixPorts::MixPort::getFlags() const {
                        _xsdc_assert(hasFlags());
                        return flags_.value();
                    }

                    bool MixPorts::MixPort::hasFlags() const {
                        return flags_.has_value();
                    }

                    const unsigned int& MixPorts::MixPort::getMaxOpenCount() const {
                        _xsdc_assert(hasMaxOpenCount());
                        return maxOpenCount_.value();
                    }

                    bool MixPorts::MixPort::hasMaxOpenCount() const {
                        return maxOpenCount_.has_value();
                    }

                    const unsigned int& MixPorts::MixPort::getMaxActiveCount() const {
                        _xsdc_assert(hasMaxActiveCount());
                        return maxActiveCount_.value();
                    }

                    bool MixPorts::MixPort::hasMaxActiveCount() const {
                        return maxActiveCount_.has_value();
                    }

                    const std::vector<AudioUsage>& MixPorts::MixPort::getPreferredUsage() const {
                        _xsdc_assert(hasPreferredUsage());
                        return preferredUsage_.value();
                    }

                    bool MixPorts::MixPort::hasPreferredUsage() const {
                        return preferredUsage_.has_value();
                    }

                    MixPorts::MixPort MixPorts::MixPort::read(xmlNode *root) {
                        std::string _raw;
                        _raw = getXmlAttribute(root, "name");
                        std::string name{};
                        if (_raw != "") {
                            std::string &_value = _raw;
                            name = _value;
                        }
                        _raw = getXmlAttribute(root, "role");
                        Role role = Role::UNKNOWN;
                        if (_raw != "") {
                            Role _value = stringToRole(_raw);
                            role = _value;
                        }
                        _raw = getXmlAttribute(root, "flags");
                        std::optional<std::vector<AudioInOutFlag>> flags = std::nullopt;
                        if (_raw != "") {
                            std::vector<AudioInOutFlag> _value;
                            {
                                std::istringstream _stream(_raw);
                                for(std::string str; _stream >> str; ) {
                                    _value.push_back(stringToAudioInOutFlag(str));
                                }
                            }
                            flags = _value;
                        }
                        _raw = getXmlAttribute(root, "maxOpenCount");
                        std::optional<unsigned int> maxOpenCount = std::nullopt;
                        if (_raw != "") {
                            unsigned int _value = static_cast<unsigned int>(stoul(_raw));
                            maxOpenCount = _value;
                        }
                        _raw = getXmlAttribute(root, "maxActiveCount");
                        std::optional<unsigned int> maxActiveCount = std::nullopt;
                        if (_raw != "") {
                            unsigned int _value = static_cast<unsigned int>(stoul(_raw));
                            maxActiveCount = _value;
                        }
                        _raw = getXmlAttribute(root, "preferredUsage");
                        std::optional<std::vector<AudioUsage>> preferredUsage = std::nullopt;
                        if (_raw != "") {
                            std::vector<AudioUsage> _value;
                            {
                                std::istringstream _stream(_raw);
                                for(std::string str; _stream >> str; ) {
                                    _value.push_back(stringToAudioUsage(str));
                                }
                            }
                            preferredUsage = _value;
                        }
                        std::vector<Profile> profile;
                        std::vector<Gains> gains;
                        for (auto *_child = root->xmlChildrenNode; _child != nullptr; _child = _child->next) {
                            if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("profile"))) {
                                Profile _value = Profile::read(_child);
                                profile.push_back(std::move(_value));
                            } else if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("gains"))) {
                                Gains _value = Gains::read(_child);
                                gains.push_back(std::move(_value));
                            }
                        }
                        MixPorts::MixPort instance(profile, gains, name, role, flags, maxOpenCount, maxActiveCount, preferredUsage);
                        return instance;
                    }

                    MixPorts::MixPorts(std::vector<MixPort> mixPort) : mixPort_(std::move(mixPort)) {
                    }

                    const std::vector<MixPorts::MixPort>& MixPorts::getMixPort() const {
                        return mixPort_;
                    }

                    bool MixPorts::hasMixPort() const {
                        return !(mixPort_.empty());
                    }

                    const MixPorts::MixPort* MixPorts::getFirstMixPort() const {
                        if (mixPort_.empty()) {
                            return nullptr;
                        }
                        return &mixPort_[0];
                    }

                    MixPorts MixPorts::read(xmlNode *root) {
                        std::string _raw;
                        std::vector<MixPort> mixPort;
                        for (auto *_child = root->xmlChildrenNode; _child != nullptr; _child = _child->next) {
                            if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("mixPort"))) {
                                MixPort _value = MixPort::read(_child);
                                mixPort.push_back(std::move(_value));
                            }
                        }
                        MixPorts instance(mixPort);
                        return instance;
                    }

                    Profile::Profile(std::optional<std::string> name, std::optional<std::string> format, std::optional<std::vector<int64_t>> samplingRates, std::optional<std::vector<AudioChannelMask>> channelMasks, std::optional<AudioEncapsulationType> encapsulationType) : name_(std::move(name)), format_(std::move(format)), samplingRates_(std::move(samplingRates)), channelMasks_(std::move(channelMasks)), encapsulationType_(encapsulationType) {
                    }

                    const std::string& Profile::getName() const {
                        _xsdc_assert(hasName());
                        return name_.value();
                    }

                    bool Profile::hasName() const {
                        return name_.has_value();
                    }

                    const std::string& Profile::getFormat() const {
                        _xsdc_assert(hasFormat());
                        return format_.value();
                    }

                    bool Profile::hasFormat() const {
                        return format_.has_value();
                    }

                    const std::vector<int64_t>& Profile::getSamplingRates() const {
                        _xsdc_assert(hasSamplingRates());
                        return samplingRates_.value();
                    }

                    bool Profile::hasSamplingRates() const {
                        return samplingRates_.has_value();
                    }

                    const std::vector<AudioChannelMask>& Profile::getChannelMasks() const {
                        _xsdc_assert(hasChannelMasks());
                        return channelMasks_.value();
                    }

                    bool Profile::hasChannelMasks() const {
                        return channelMasks_.has_value();
                    }

                    const AudioEncapsulationType& Profile::getEncapsulationType() const {
                        _xsdc_assert(hasEncapsulationType());
                        return encapsulationType_.value();
                    }

                    bool Profile::hasEncapsulationType() const {
                        return encapsulationType_.has_value();
                    }

                    Profile Profile::read(xmlNode *root) {
                        std::string _raw;
                        _raw = getXmlAttribute(root, "name");
                        std::optional<std::string> name = std::nullopt;
                        if (_raw != "") {
                            std::string &_value = _raw;
                            name = _value;
                        }
                        _raw = getXmlAttribute(root, "format");
                        std::optional<std::string> format = std::nullopt;
                        if (_raw != "") {
                            std::string &_value = _raw;
                            format = _value;
                        }
                        _raw = getXmlAttribute(root, "samplingRates");
                        std::optional<std::vector<int64_t>> samplingRates = std::nullopt;
                        if (_raw != "") {
                            std::vector<int64_t> _value;
                            {
                                std::istringstream _stream(_raw);
                                for(std::string str; _stream >> str; ) {
                                    _value.push_back(std::stoll(str));
                                }
                            }
                            samplingRates = _value;
                        }
                        _raw = getXmlAttribute(root, "channelMasks");
                        std::optional<std::vector<AudioChannelMask>> channelMasks = std::nullopt;
                        if (_raw != "") {
                            std::vector<AudioChannelMask> _value;
                            {
                                std::istringstream _stream(_raw);
                                for(std::string str; _stream >> str; ) {
                                    _value.push_back(stringToAudioChannelMask(str));
                                }
                            }
                            channelMasks = _value;
                        }
                        _raw = getXmlAttribute(root, "encapsulationType");
                        std::optional<AudioEncapsulationType> encapsulationType = std::nullopt;
                        if (_raw != "") {
                            AudioEncapsulationType _value = stringToAudioEncapsulationType(_raw);
                            encapsulationType = _value;
                        }
                        Profile instance(name, format, samplingRates, channelMasks, encapsulationType);
                        return instance;
                    }

                    Gains::Gain::Gain(std::string name, std::vector<AudioGainMode> mode, std::optional<AudioChannelMask> channel_mask, std::optional<int> minValueMB, std::optional<int> maxValueMB, std::optional<int> defaultValueMB, std::optional<int> stepValueMB, std::optional<int> minRampMs, std::optional<int> maxRampMs, std::optional<bool> useForVolume) : name_(std::move(name)), mode_(std::move(mode)), channel_mask_(channel_mask), minValueMB_(minValueMB), maxValueMB_(maxValueMB), defaultValueMB_(defaultValueMB), stepValueMB_(stepValueMB), minRampMs_(minRampMs), maxRampMs_(maxRampMs), useForVolume_(useForVolume) {
                    }

                    const std::string& Gains::Gain::getName() const {
                        return name_;
                    }

                    bool Gains::Gain::hasName() const {
                        return true;
                    }

                    const std::vector<AudioGainMode>& Gains::Gain::getMode() const {
                        return mode_;
                    }

                    bool Gains::Gain::hasMode() const {
                        return true;
                    }

                    const AudioChannelMask& Gains::Gain::getChannel_mask() const {
                        _xsdc_assert(hasChannel_mask());
                        return channel_mask_.value();
                    }

                    bool Gains::Gain::hasChannel_mask() const {
                        return channel_mask_.has_value();
                    }

                    const int& Gains::Gain::getMinValueMB() const {
                        _xsdc_assert(hasMinValueMB());
                        return minValueMB_.value();
                    }

                    bool Gains::Gain::hasMinValueMB() const {
                        return minValueMB_.has_value();
                    }

                    const int& Gains::Gain::getMaxValueMB() const {
                        _xsdc_assert(hasMaxValueMB());
                        return maxValueMB_.value();
                    }

                    bool Gains::Gain::hasMaxValueMB() const {
                        return maxValueMB_.has_value();
                    }

                    const int& Gains::Gain::getDefaultValueMB() const {
                        _xsdc_assert(hasDefaultValueMB());
                        return defaultValueMB_.value();
                    }

                    bool Gains::Gain::hasDefaultValueMB() const {
                        return defaultValueMB_.has_value();
                    }

                    const int& Gains::Gain::getStepValueMB() const {
                        _xsdc_assert(hasStepValueMB());
                        return stepValueMB_.value();
                    }

                    bool Gains::Gain::hasStepValueMB() const {
                        return stepValueMB_.has_value();
                    }

                    const int& Gains::Gain::getMinRampMs() const {
                        _xsdc_assert(hasMinRampMs());
                        return minRampMs_.value();
                    }

                    bool Gains::Gain::hasMinRampMs() const {
                        return minRampMs_.has_value();
                    }

                    const int& Gains::Gain::getMaxRampMs() const {
                        _xsdc_assert(hasMaxRampMs());
                        return maxRampMs_.value();
                    }

                    bool Gains::Gain::hasMaxRampMs() const {
                        return maxRampMs_.has_value();
                    }

                    const bool& Gains::Gain::getUseForVolume() const {
                        _xsdc_assert(hasUseForVolume());
                        return useForVolume_.value();
                    }

                    bool Gains::Gain::hasUseForVolume() const {
                        return useForVolume_.has_value();
                    }

                    Gains::Gain Gains::Gain::read(xmlNode *root) {
                        std::string _raw;
                        _raw = getXmlAttribute(root, "name");
                        std::string name{};
                        if (_raw != "") {
                            std::string &_value = _raw;
                            name = _value;
                        }
                        _raw = getXmlAttribute(root, "mode");
                        std::vector<AudioGainMode> mode{};
                        if (_raw != "") {
                            std::vector<AudioGainMode> _value;
                            {
                                std::istringstream _stream(_raw);
                                for(std::string str; _stream >> str; ) {
                                    _value.push_back(stringToAudioGainMode(str));
                                }
                            }
                            mode = _value;
                        }
                        _raw = getXmlAttribute(root, "channel_mask");
                        std::optional<AudioChannelMask> channel_mask = std::nullopt;
                        if (_raw != "") {
                            AudioChannelMask _value = stringToAudioChannelMask(_raw);
                            channel_mask = _value;
                        }
                        _raw = getXmlAttribute(root, "minValueMB");
                        std::optional<int> minValueMB = std::nullopt;
                        if (_raw != "") {
                            int _value = std::stoi(_raw);
                            minValueMB = _value;
                        }
                        _raw = getXmlAttribute(root, "maxValueMB");
                        std::optional<int> maxValueMB = std::nullopt;
                        if (_raw != "") {
                            int _value = std::stoi(_raw);
                            maxValueMB = _value;
                        }
                        _raw = getXmlAttribute(root, "defaultValueMB");
                        std::optional<int> defaultValueMB = std::nullopt;
                        if (_raw != "") {
                            int _value = std::stoi(_raw);
                            defaultValueMB = _value;
                        }
                        _raw = getXmlAttribute(root, "stepValueMB");
                        std::optional<int> stepValueMB = std::nullopt;
                        if (_raw != "") {
                            int _value = std::stoi(_raw);
                            stepValueMB = _value;
                        }
                        _raw = getXmlAttribute(root, "minRampMs");
                        std::optional<int> minRampMs = std::nullopt;
                        if (_raw != "") {
                            int _value = std::stoi(_raw);
                            minRampMs = _value;
                        }
                        _raw = getXmlAttribute(root, "maxRampMs");
                        std::optional<int> maxRampMs = std::nullopt;
                        if (_raw != "") {
                            int _value = std::stoi(_raw);
                            maxRampMs = _value;
                        }
                        _raw = getXmlAttribute(root, "useForVolume");
                        std::optional<bool> useForVolume = std::nullopt;
                        if (_raw != "") {
                            bool _value = _raw == "true";
                            useForVolume = _value;
                        }
                        Gains::Gain instance(name, mode, channel_mask, minValueMB, maxValueMB, defaultValueMB, stepValueMB, minRampMs, maxRampMs, useForVolume);
                        return instance;
                    }

                    Gains::Gains(std::vector<Gain> gain) : gain_(std::move(gain)) {
                    }

                    const std::vector<Gains::Gain>& Gains::getGain() const {
                        return gain_;
                    }

                    bool Gains::hasGain() const {
                        return !(gain_.empty());
                    }

                    const Gains::Gain* Gains::getFirstGain() const {
                        if (gain_.empty()) {
                            return nullptr;
                        }
                        return &gain_[0];
                    }

                    Gains Gains::read(xmlNode *root) {
                        std::string _raw;
                        std::vector<Gain> gain;
                        for (auto *_child = root->xmlChildrenNode; _child != nullptr; _child = _child->next) {
                            if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("gain"))) {
                                Gain _value = Gain::read(_child);
                                gain.push_back(std::move(_value));
                            }
                        }
                        Gains instance(gain);
                        return instance;
                    }

                    DevicePorts::DevicePort::DevicePort(std::vector<Profile> profile, std::vector<Gains> gains, std::string tagName, std::string type, Role role, std::optional<std::string> address, std::optional<bool> _default, std::optional<std::vector<std::string>> encodedFormats) : profile_(std::move(profile)), gains_(std::move(gains)), tagName_(std::move(tagName)), type_(std::move(type)), role_(role), address_(std::move(address)), _default_(_default), encodedFormats_(std::move(encodedFormats)) {
                    }

                    const std::vector<Profile>& DevicePorts::DevicePort::getProfile() const {
                        return profile_;
                    }

                    bool DevicePorts::DevicePort::hasProfile() const {
                        return !(profile_.empty());
                    }

                    const Profile* DevicePorts::DevicePort::getFirstProfile() const {
                        if (profile_.empty()) {
                            return nullptr;
                        }
                        return &profile_[0];
                    }

                    const std::vector<Gains>& DevicePorts::DevicePort::getGains() const {
                        return gains_;
                    }

                    bool DevicePorts::DevicePort::hasGains() const {
                        return !(gains_.empty());
                    }

                    const Gains* DevicePorts::DevicePort::getFirstGains() const {
                        if (gains_.empty()) {
                            return nullptr;
                        }
                        return &gains_[0];
                    }

                    const std::string& DevicePorts::DevicePort::getTagName() const {
                        return tagName_;
                    }

                    bool DevicePorts::DevicePort::hasTagName() const {
                        return true;
                    }

                    const std::string& DevicePorts::DevicePort::getType() const {
                        return type_;
                    }

                    bool DevicePorts::DevicePort::hasType() const {
                        return true;
                    }

                    const Role& DevicePorts::DevicePort::getRole() const {
                        return role_;
                    }

                    bool DevicePorts::DevicePort::hasRole() const {
                        return true;
                    }

                    const std::string& DevicePorts::DevicePort::getAddress() const {
                        _xsdc_assert(hasAddress());
                        return address_.value();
                    }

                    bool DevicePorts::DevicePort::hasAddress() const {
                        return address_.has_value();
                    }

                    const bool& DevicePorts::DevicePort::get_default() const {
                        _xsdc_assert(has_default());
                        return _default_.value();
                    }

                    bool DevicePorts::DevicePort::has_default() const {
                        return _default_.has_value();
                    }

                    const std::vector<std::string>& DevicePorts::DevicePort::getEncodedFormats() const {
                        _xsdc_assert(hasEncodedFormats());
                        return encodedFormats_.value();
                    }

                    bool DevicePorts::DevicePort::hasEncodedFormats() const {
                        return encodedFormats_.has_value();
                    }

                    DevicePorts::DevicePort DevicePorts::DevicePort::read(xmlNode *root) {
                        std::string _raw;
                        _raw = getXmlAttribute(root, "tagName");
                        std::string tagName{};
                        if (_raw != "") {
                            std::string &_value = _raw;
                            tagName = _value;
                        }
                        _raw = getXmlAttribute(root, "type");
                        std::string type{};
                        if (_raw != "") {
                            std::string &_value = _raw;
                            type = _value;
                        }
                        _raw = getXmlAttribute(root, "role");
                        Role role = Role::UNKNOWN;
                        if (_raw != "") {
                            Role _value = stringToRole(_raw);
                            role = _value;
                        }
                        _raw = getXmlAttribute(root, "address");
                        std::optional<std::string> address = std::nullopt;
                        if (_raw != "") {
                            std::string &_value = _raw;
                            address = _value;
                        }
                        _raw = getXmlAttribute(root, "default");
                        std::optional<bool> _default = std::nullopt;
                        if (_raw != "") {
                            bool _value = _raw == "true";
                            _default = _value;
                        }
                        _raw = getXmlAttribute(root, "encodedFormats");
                        std::optional<std::vector<std::string>> encodedFormats = std::nullopt;
                        if (_raw != "") {
                            std::vector<std::string> _value;
                            {
                                std::istringstream _stream(_raw);
                                for(std::string str; _stream >> str; ) {
                                    _value.push_back(str);
                                }
                            }
                            encodedFormats = _value;
                        }
                        std::vector<Profile> profile;
                        std::vector<Gains> gains;
                        for (auto *_child = root->xmlChildrenNode; _child != nullptr; _child = _child->next) {
                            if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("profile"))) {
                                Profile _value = Profile::read(_child);
                                profile.push_back(std::move(_value));
                            } else if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("gains"))) {
                                Gains _value = Gains::read(_child);
                                gains.push_back(std::move(_value));
                            }
                        }
                        DevicePorts::DevicePort instance(profile, gains, tagName, type, role, address, _default, encodedFormats);
                        return instance;
                    }

                    DevicePorts::DevicePorts(std::vector<DevicePort> devicePort) : devicePort_(std::move(devicePort)) {
                    }

                    const std::vector<DevicePorts::DevicePort>& DevicePorts::getDevicePort() const {
                        return devicePort_;
                    }

                    bool DevicePorts::hasDevicePort() const {
                        return !(devicePort_.empty());
                    }

                    const DevicePorts::DevicePort* DevicePorts::getFirstDevicePort() const {
                        if (devicePort_.empty()) {
                            return nullptr;
                        }
                        return &devicePort_[0];
                    }

                    DevicePorts DevicePorts::read(xmlNode *root) {
                        std::string _raw;
                        std::vector<DevicePort> devicePort;
                        for (auto *_child = root->xmlChildrenNode; _child != nullptr; _child = _child->next) {
                            if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("devicePort"))) {
                                DevicePort _value = DevicePort::read(_child);
                                devicePort.push_back(std::move(_value));
                            }
                        }
                        DevicePorts instance(devicePort);
                        return instance;
                    }

                    Routes::Route::Route(MixType type, std::string sink, std::string sources) : type_(type), sink_(std::move(sink)), sources_(std::move(sources)) {
                    }

                    const MixType& Routes::Route::getType() const {
                        return type_;
                    }

                    bool Routes::Route::hasType() const {
                        return true;
                    }

                    const std::string& Routes::Route::getSink() const {
                        return sink_;
                    }

                    bool Routes::Route::hasSink() const {
                        return true;
                    }

                    const std::string& Routes::Route::getSources() const {
                        return sources_;
                    }

                    bool Routes::Route::hasSources() const {
                        return true;
                    }

                    Routes::Route Routes::Route::read(xmlNode *root) {
                        std::string _raw;
                        _raw = getXmlAttribute(root, "type");
                        MixType type = MixType::UNKNOWN;
                        if (_raw != "") {
                            MixType _value = stringToMixType(_raw);
                            type = _value;
                        }
                        _raw = getXmlAttribute(root, "sink");
                        std::string sink{};
                        if (_raw != "") {
                            std::string &_value = _raw;
                            sink = _value;
                        }
                        _raw = getXmlAttribute(root, "sources");
                        std::string sources{};
                        if (_raw != "") {
                            std::string &_value = _raw;
                            sources = _value;
                        }
                        Routes::Route instance(type, sink, sources);
                        return instance;
                    }

                    Routes::Routes(std::vector<Route> route) : route_(std::move(route)) {
                    }

                    const std::vector<Routes::Route>& Routes::getRoute() const {
                        return route_;
                    }

                    bool Routes::hasRoute() const {
                        return !(route_.empty());
                    }

                    const Routes::Route* Routes::getFirstRoute() const {
                        if (route_.empty()) {
                            return nullptr;
                        }
                        return &route_[0];
                    }

                    Routes Routes::read(xmlNode *root) {
                        std::string _raw;
                        std::vector<Route> route;
                        for (auto *_child = root->xmlChildrenNode; _child != nullptr; _child = _child->next) {
                            if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("route"))) {
                                Route _value = Route::read(_child);
                                route.push_back(std::move(_value));
                            }
                        }
                        Routes instance(route);
                        return instance;
                    }

                    Volumes::Volumes(std::vector<Volume> volume, std::vector<Reference> reference) : volume_(std::move(volume)), reference_(std::move(reference)) {
                    }

                    const std::vector<Volume>& Volumes::getVolume() const {
                        return volume_;
                    }

                    bool Volumes::hasVolume() const {
                        return !(volume_.empty());
                    }

                    const Volume* Volumes::getFirstVolume() const {
                        if (volume_.empty()) {
                            return nullptr;
                        }
                        return &volume_[0];
                    }

                    const std::vector<Reference>& Volumes::getReference() const {
                        return reference_;
                    }

                    bool Volumes::hasReference() const {
                        return !(reference_.empty());
                    }

                    const Reference* Volumes::getFirstReference() const {
                        if (reference_.empty()) {
                            return nullptr;
                        }
                        return &reference_[0];
                    }

                    Volumes Volumes::read(xmlNode *root) {
                        std::string _raw;
                        std::vector<Volume> volume;
                        std::vector<Reference> reference;
                        for (auto *_child = root->xmlChildrenNode; _child != nullptr; _child = _child->next) {
                            if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("volume"))) {
                                Volume _value = Volume::read(_child);
                                volume.push_back(std::move(_value));
                            } else if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("reference"))) {
                                Reference _value = Reference::read(_child);
                                reference.push_back(std::move(_value));
                            }
                        }
                        Volumes instance(volume, reference);
                        return instance;
                    }

                    Volume::Volume(std::vector<std::string> point, std::optional<AudioStreamType> stream, std::optional<DeviceCategory> deviceCategory, std::optional<std::string> ref) : point_(std::move(point)), stream_(stream), deviceCategory_(deviceCategory), ref_(std::move(ref)) {
                    }

                    const std::vector<std::string>& Volume::getPoint() const {
                        return point_;
                    }

                    bool Volume::hasPoint() const {
                        return !(point_.empty());
                    }

                    const std::string* Volume::getFirstPoint() const {
                        if (point_.empty()) {
                            return nullptr;
                        }
                        return &point_[0];
                    }

                    const AudioStreamType& Volume::getStream() const {
                        _xsdc_assert(hasStream());
                        return stream_.value();
                    }

                    bool Volume::hasStream() const {
                        return stream_.has_value();
                    }

                    const DeviceCategory& Volume::getDeviceCategory() const {
                        _xsdc_assert(hasDeviceCategory());
                        return deviceCategory_.value();
                    }

                    bool Volume::hasDeviceCategory() const {
                        return deviceCategory_.has_value();
                    }

                    const std::string& Volume::getRef() const {
                        _xsdc_assert(hasRef());
                        return ref_.value();
                    }

                    bool Volume::hasRef() const {
                        return ref_.has_value();
                    }

                    Volume Volume::read(xmlNode *root) {
                        std::string _raw;
                        _raw = getXmlAttribute(root, "stream");
                        std::optional<AudioStreamType> stream = std::nullopt;
                        if (_raw != "") {
                            AudioStreamType _value = stringToAudioStreamType(_raw);
                            stream = _value;
                        }
                        _raw = getXmlAttribute(root, "deviceCategory");
                        std::optional<DeviceCategory> deviceCategory = std::nullopt;
                        if (_raw != "") {
                            DeviceCategory _value = stringToDeviceCategory(_raw);
                            deviceCategory = _value;
                        }
                        _raw = getXmlAttribute(root, "ref");
                        std::optional<std::string> ref = std::nullopt;
                        if (_raw != "") {
                            std::string &_value = _raw;
                            ref = _value;
                        }
                        std::vector<std::string> point;
                        for (auto *_child = root->xmlChildrenNode; _child != nullptr; _child = _child->next) {
                            if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("point"))) {
                                auto xmlValue = make_xmlUnique(xmlNodeListGetString(_child->doc, _child->xmlChildrenNode, 1));
                                if (xmlValue == nullptr) {
                                    _raw = "";
                                } else {
                                    _raw = reinterpret_cast<const char*>(xmlValue.get());
                                }
                                std::string &_value = _raw;
                                point.push_back(std::move(_value));
                            }
                        }
                        Volume instance(point, stream, deviceCategory, ref);
                        return instance;
                    }

                    Reference::Reference(std::vector<std::string> point, std::string name) : point_(std::move(point)), name_(std::move(name)) {
                    }

                    const std::vector<std::string>& Reference::getPoint() const {
                        return point_;
                    }

                    bool Reference::hasPoint() const {
                        return !(point_.empty());
                    }

                    const std::string* Reference::getFirstPoint() const {
                        if (point_.empty()) {
                            return nullptr;
                        }
                        return &point_[0];
                    }

                    const std::string& Reference::getName() const {
                        return name_;
                    }

                    bool Reference::hasName() const {
                        return true;
                    }

                    Reference Reference::read(xmlNode *root) {
                        std::string _raw;
                        _raw = getXmlAttribute(root, "name");
                        std::string name{};
                        if (_raw != "") {
                            std::string &_value = _raw;
                            name = _value;
                        }
                        std::vector<std::string> point;
                        for (auto *_child = root->xmlChildrenNode; _child != nullptr; _child = _child->next) {
                            if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("point"))) {
                                auto xmlValue = make_xmlUnique(xmlNodeListGetString(_child->doc, _child->xmlChildrenNode, 1));
                                if (xmlValue == nullptr) {
                                    _raw = "";
                                } else {
                                    _raw = reinterpret_cast<const char*>(xmlValue.get());
                                }
                                std::string &_value = _raw;
                                point.push_back(std::move(_value));
                            }
                        }
                        Reference instance(point, name);
                        return instance;
                    }

                    SurroundSound::SurroundSound(std::vector<SurroundFormats> formats) : formats_(std::move(formats)) {
                    }

                    const std::vector<SurroundFormats>& SurroundSound::getFormats() const {
                        return formats_;
                    }

                    bool SurroundSound::hasFormats() const {
                        return !(formats_.empty());
                    }

                    const SurroundFormats* SurroundSound::getFirstFormats() const {
                        if (formats_.empty()) {
                            return nullptr;
                        }
                        return &formats_[0];
                    }

                    SurroundSound SurroundSound::read(xmlNode *root) {
                        std::string _raw;
                        std::vector<SurroundFormats> formats;
                        for (auto *_child = root->xmlChildrenNode; _child != nullptr; _child = _child->next) {
                            if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("formats"))) {
                                SurroundFormats _value = SurroundFormats::read(_child);
                                formats.push_back(std::move(_value));
                            }
                        }
                        SurroundSound instance(formats);
                        return instance;
                    }

                    SurroundFormats::Format::Format(std::string name, std::optional<std::vector<std::string>> subformats) : name_(std::move(name)), subformats_(std::move(subformats)) {
                    }

                    const std::string& SurroundFormats::Format::getName() const {
                        return name_;
                    }

                    bool SurroundFormats::Format::hasName() const {
                        return true;
                    }

                    const std::vector<std::string>& SurroundFormats::Format::getSubformats() const {
                        _xsdc_assert(hasSubformats());
                        return subformats_.value();
                    }

                    bool SurroundFormats::Format::hasSubformats() const {
                        return subformats_.has_value();
                    }

                    SurroundFormats::Format SurroundFormats::Format::read(xmlNode *root) {
                        std::string _raw;
                        _raw = getXmlAttribute(root, "name");
                        std::string name{};
                        if (_raw != "") {
                            std::string &_value = _raw;
                            name = _value;
                        }
                        _raw = getXmlAttribute(root, "subformats");
                        std::optional<std::vector<std::string>> subformats = std::nullopt;
                        if (_raw != "") {
                            std::vector<std::string> _value;
                            {
                                std::istringstream _stream(_raw);
                                for(std::string str; _stream >> str; ) {
                                    _value.push_back(str);
                                }
                            }
                            subformats = _value;
                        }
                        SurroundFormats::Format instance(name, subformats);
                        return instance;
                    }

                    SurroundFormats::SurroundFormats(std::vector<Format> format) : format_(std::move(format)) {
                    }

                    const std::vector<SurroundFormats::Format>& SurroundFormats::getFormat() const {
                        return format_;
                    }

                    bool SurroundFormats::hasFormat() const {
                        return !(format_.empty());
                    }

                    const SurroundFormats::Format* SurroundFormats::getFirstFormat() const {
                        if (format_.empty()) {
                            return nullptr;
                        }
                        return &format_[0];
                    }

                    SurroundFormats SurroundFormats::read(xmlNode *root) {
                        std::string _raw;
                        std::vector<Format> format;
                        for (auto *_child = root->xmlChildrenNode; _child != nullptr; _child = _child->next) {
                            if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("format"))) {
                                Format _value = Format::read(_child);
                                format.push_back(std::move(_value));
                            }
                        }
                        SurroundFormats instance(format);
                        return instance;
                    }

                    AudioPolicyConfiguration::AudioPolicyConfiguration(std::vector<GlobalConfiguration> globalConfiguration, std::vector<Modules> modules, std::vector<Volumes> volumes, std::vector<SurroundSound> surroundSound, std::optional<Version> version) : globalConfiguration_(std::move(globalConfiguration)), modules_(std::move(modules)), volumes_(std::move(volumes)), surroundSound_(std::move(surroundSound)), version_(version) {
                    }

                    const std::vector<GlobalConfiguration>& AudioPolicyConfiguration::getGlobalConfiguration() const {
                        return globalConfiguration_;
                    }

                    bool AudioPolicyConfiguration::hasGlobalConfiguration() const {
                        return !(globalConfiguration_.empty());
                    }

                    const GlobalConfiguration* AudioPolicyConfiguration::getFirstGlobalConfiguration() const {
                        if (globalConfiguration_.empty()) {
                            return nullptr;
                        }
                        return &globalConfiguration_[0];
                    }

                    const std::vector<Modules>& AudioPolicyConfiguration::getModules() const {
                        return modules_;
                    }

                    bool AudioPolicyConfiguration::hasModules() const {
                        return !(modules_.empty());
                    }

                    const Modules* AudioPolicyConfiguration::getFirstModules() const {
                        if (modules_.empty()) {
                            return nullptr;
                        }
                        return &modules_[0];
                    }

                    const std::vector<Volumes>& AudioPolicyConfiguration::getVolumes() const {
                        return volumes_;
                    }

                    bool AudioPolicyConfiguration::hasVolumes() const {
                        return !(volumes_.empty());
                    }

                    const Volumes* AudioPolicyConfiguration::getFirstVolumes() const {
                        if (volumes_.empty()) {
                            return nullptr;
                        }
                        return &volumes_[0];
                    }

                    const std::vector<SurroundSound>& AudioPolicyConfiguration::getSurroundSound() const {
                        return surroundSound_;
                    }

                    bool AudioPolicyConfiguration::hasSurroundSound() const {
                        return !(surroundSound_.empty());
                    }

                    const SurroundSound* AudioPolicyConfiguration::getFirstSurroundSound() const {
                        if (surroundSound_.empty()) {
                            return nullptr;
                        }
                        return &surroundSound_[0];
                    }

                    const Version& AudioPolicyConfiguration::getVersion() const {
                        _xsdc_assert(hasVersion());
                        return version_.value();
                    }

                    bool AudioPolicyConfiguration::hasVersion() const {
                        return version_.has_value();
                    }

                    AudioPolicyConfiguration AudioPolicyConfiguration::read(xmlNode *root) {
                        std::string _raw;
                        _raw = getXmlAttribute(root, "version");
                        std::optional<Version> version = std::nullopt;
                        if (_raw != "") {
                            Version _value = stringToVersion(_raw);
                            version = _value;
                        }
                        std::vector<GlobalConfiguration> globalConfiguration;
                        std::vector<Modules> modules;
                        std::vector<Volumes> volumes;
                        std::vector<SurroundSound> surroundSound;
                        for (auto *_child = root->xmlChildrenNode; _child != nullptr; _child = _child->next) {
                            if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("globalConfiguration"))) {
                                GlobalConfiguration _value = GlobalConfiguration::read(_child);
                                globalConfiguration.push_back(std::move(_value));
                            } else if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("modules"))) {
                                Modules _value = Modules::read(_child);
                                modules.push_back(std::move(_value));
                            } else if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("volumes"))) {
                                Volumes _value = Volumes::read(_child);
                                volumes.push_back(std::move(_value));
                            } else if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("surroundSound"))) {
                                SurroundSound _value = SurroundSound::read(_child);
                                surroundSound.push_back(std::move(_value));
                            }
                        }
                        AudioPolicyConfiguration instance(globalConfiguration, modules, volumes, surroundSound, version);
                        return instance;
                    }
                } // V7_0
            } // configuration
        } // policy
    } // audio
} // android
