#define LOG_TAG "com.android.apex"
#include "com_android_apex.h"

#include <assert.h>
#ifndef __BIONIC__
#define __assert2(f,n,fun,e) do { fprintf(stderr, "%s:%d: %s: Assertion `%s' failed", (f), (n), (fun), (e)); abort(); } while (false)
#endif
#define _xsdc_assert(e) do if (!(e)) __assert2(__FILE__, __LINE__, __PRETTY_FUNCTION__, #e); while (false)

namespace com {
    namespace android {
        namespace apex {
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

            std::optional<ApexInfoList> readApexInfoList(const char* configFile) {
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

                if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("apex-info-list"))) {
                    ApexInfoList _value = ApexInfoList::read(_child);
                    return _value;
                }
                return std::nullopt;
            }

            std::optional<ApexInfoList> parseApexInfoList(const char* xml) {
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

                if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("apex-info-list"))) {
                    ApexInfoList _value = ApexInfoList::read(_child);
                    return _value;
                }
                return std::nullopt;
            }

            void write(std::ostream& _out, const ApexInfoList& apexInfoList) {
                _out << "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";
                apexInfoList.write(_out, "apex-info-list");
            }

            static int indentIndex = 0;
            std::string printIndent() {
                std::string s = "";
                for (int index = 0; index < indentIndex; ++index) {
                    s += "    ";
                }
                return s;
            }


            ApexInfoList::ApexInfoList(std::vector<ApexInfo> apexInfo) : apexInfo_(std::move(apexInfo)) {
            }

            const std::vector<ApexInfo>& ApexInfoList::getApexInfo() const {
                return apexInfo_;
            }

            bool ApexInfoList::hasApexInfo() const {
                return !(apexInfo_.empty());
            }

            const ApexInfo* ApexInfoList::getFirstApexInfo() const {
                if (apexInfo_.empty()) {
                    return nullptr;
                }
                return &apexInfo_[0];
            }

            ApexInfoList ApexInfoList::read(xmlNode *root) {
                std::string _raw;
                std::vector<ApexInfo> apexInfo;
                for (auto *_child = root->xmlChildrenNode; _child != nullptr; _child = _child->next) {
                    if (!xmlStrcmp(_child->name, reinterpret_cast<const xmlChar*>("apex-info"))) {
                        ApexInfo _value = ApexInfo::read(_child);
                        apexInfo.push_back(std::move(_value));
                    }
                }
                ApexInfoList instance(apexInfo);
                return instance;
            }

            void ApexInfoList::write(std::ostream& _out, const std::string& _name) const {
                _out << printIndent() << "<" << _name;
                _out << ">" << std::endl;
                ++indentIndex;
                for (auto& _value : getApexInfo()) {
                    _value.write(_out, "apex-info");
                }
                --indentIndex;
                _out << printIndent() << "</" << _name << ">" << std::endl;
            }

            ApexInfo::ApexInfo(std::string moduleName, std::string modulePath, std::optional<std::string> preinstalledModulePath, int64_t versionCode, std::string versionName, bool isFactory, bool isActive, std::optional<int64_t> lastUpdateMillis, bool provideSharedApexLibs) : moduleName_(std::move(moduleName)), modulePath_(std::move(modulePath)), preinstalledModulePath_(std::move(preinstalledModulePath)), versionCode_(versionCode), versionName_(std::move(versionName)), isFactory_(isFactory), isActive_(isActive), lastUpdateMillis_(lastUpdateMillis), provideSharedApexLibs_(provideSharedApexLibs) {
            }

            const std::string& ApexInfo::getModuleName() const {
                return moduleName_;
            }

            bool ApexInfo::hasModuleName() const {
                return true;
            }

            const std::string& ApexInfo::getModulePath() const {
                return modulePath_;
            }

            bool ApexInfo::hasModulePath() const {
                return true;
            }

            const std::string& ApexInfo::getPreinstalledModulePath() const {
                _xsdc_assert(hasPreinstalledModulePath());
                return preinstalledModulePath_.value();
            }

            bool ApexInfo::hasPreinstalledModulePath() const {
                return preinstalledModulePath_.has_value();
            }

            const int64_t& ApexInfo::getVersionCode() const {
                return versionCode_;
            }

            bool ApexInfo::hasVersionCode() const {
                return true;
            }

            const std::string& ApexInfo::getVersionName() const {
                return versionName_;
            }

            bool ApexInfo::hasVersionName() const {
                return true;
            }

            const bool& ApexInfo::getIsFactory() const {
                return isFactory_;
            }

            bool ApexInfo::hasIsFactory() const {
                return true;
            }

            const bool& ApexInfo::getIsActive() const {
                return isActive_;
            }

            bool ApexInfo::hasIsActive() const {
                return true;
            }

            const int64_t& ApexInfo::getLastUpdateMillis() const {
                _xsdc_assert(hasLastUpdateMillis());
                return lastUpdateMillis_.value();
            }

            bool ApexInfo::hasLastUpdateMillis() const {
                return lastUpdateMillis_.has_value();
            }

            const bool& ApexInfo::getProvideSharedApexLibs() const {
                return provideSharedApexLibs_;
            }

            bool ApexInfo::hasProvideSharedApexLibs() const {
                return true;
            }

            ApexInfo ApexInfo::read(xmlNode *root) {
                std::string _raw;
                _raw = getXmlAttribute(root, "moduleName");
                std::string moduleName{};
                if (_raw != "") {
                    std::string &_value = _raw;
                    moduleName = _value;
                }
                _raw = getXmlAttribute(root, "modulePath");
                std::string modulePath{};
                if (_raw != "") {
                    std::string &_value = _raw;
                    modulePath = _value;
                }
                _raw = getXmlAttribute(root, "preinstalledModulePath");
                std::optional<std::string> preinstalledModulePath = std::nullopt;
                if (_raw != "") {
                    std::string &_value = _raw;
                    preinstalledModulePath = _value;
                }
                _raw = getXmlAttribute(root, "versionCode");
                int64_t versionCode{};
                if (_raw != "") {
                    int64_t _value = std::stoll(_raw);
                    versionCode = _value;
                }
                _raw = getXmlAttribute(root, "versionName");
                std::string versionName{};
                if (_raw != "") {
                    std::string &_value = _raw;
                    versionName = _value;
                }
                _raw = getXmlAttribute(root, "isFactory");
                bool isFactory{};
                if (_raw != "") {
                    bool _value = _raw == "true";
                    isFactory = _value;
                }
                _raw = getXmlAttribute(root, "isActive");
                bool isActive{};
                if (_raw != "") {
                    bool _value = _raw == "true";
                    isActive = _value;
                }
                _raw = getXmlAttribute(root, "lastUpdateMillis");
                std::optional<int64_t> lastUpdateMillis = std::nullopt;
                if (_raw != "") {
                    int64_t _value = std::stoll(_raw);
                    lastUpdateMillis = _value;
                }
                _raw = getXmlAttribute(root, "provideSharedApexLibs");
                bool provideSharedApexLibs{};
                if (_raw != "") {
                    bool _value = _raw == "true";
                    provideSharedApexLibs = _value;
                }
                ApexInfo instance(moduleName, modulePath, preinstalledModulePath, versionCode, versionName, isFactory, isActive, lastUpdateMillis, provideSharedApexLibs);
                return instance;
            }

            void ApexInfo::write(std::ostream& _out, const std::string& _name) const {
                _out << printIndent() << "<" << _name;
                if (hasModuleName()) {
                    _out << " moduleName=\"";
                    _out << getModuleName();
                    _out << "\"";
                }
                if (hasModulePath()) {
                    _out << " modulePath=\"";
                    _out << getModulePath();
                    _out << "\"";
                }
                if (hasPreinstalledModulePath()) {
                    _out << " preinstalledModulePath=\"";
                    _out << getPreinstalledModulePath();
                    _out << "\"";
                }
                if (hasVersionCode()) {
                    _out << " versionCode=\"";
                    _out << getVersionCode();
                    _out << "\"";
                }
                if (hasVersionName()) {
                    _out << " versionName=\"";
                    _out << getVersionName();
                    _out << "\"";
                }
                if (hasIsFactory()) {
                    _out << " isFactory=\"";
                    _out << (getIsFactory() ? "true" : "false");
                    _out << "\"";
                }
                if (hasIsActive()) {
                    _out << " isActive=\"";
                    _out << (getIsActive() ? "true" : "false");
                    _out << "\"";
                }
                if (hasLastUpdateMillis()) {
                    _out << " lastUpdateMillis=\"";
                    _out << getLastUpdateMillis();
                    _out << "\"";
                }
                if (hasProvideSharedApexLibs()) {
                    _out << " provideSharedApexLibs=\"";
                    _out << (getProvideSharedApexLibs() ? "true" : "false");
                    _out << "\"";
                }
                _out << ">" << std::endl;
                ++indentIndex;
                --indentIndex;
                _out << printIndent() << "</" << _name << ">" << std::endl;
            }
        } // apex
    } // android
} // com
