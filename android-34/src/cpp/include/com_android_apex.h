#ifndef COM_ANDROID_APEX_H
#define COM_ANDROID_APEX_H

#include <array>
#include <map>
#include <optional>
#include <string>
#include <vector>
#include <sstream>
#include <iostream>

#if __has_include(<libxml/parser.h>)
#include <libxml/parser.h>
#include <libxml/xinclude.h>
#else
#error Require libxml2 library. Please add libxml2 to shared_libs or static_libs
#endif

#include "com_android_apex_enums.h"

namespace com {
    namespace android {
        namespace apex {
            class ApexInfoList;
            class ApexInfo;
            std::optional<ApexInfoList> readApexInfoList(const char* configFile);

            std::optional<ApexInfoList> parseApexInfoList(const char* xml);

            void write(std::ostream& _out, const ApexInfoList& apexInfoList);

            class ApexInfoList {
                private:
                const std::vector<ApexInfo> apexInfo_;
                public:
                explicit ApexInfoList(std::vector<ApexInfo> apexInfo);
                const std::vector<ApexInfo>& getApexInfo() const;
                bool hasApexInfo() const;
                const ApexInfo* getFirstApexInfo() const;
                static ApexInfoList read(xmlNode *root);
                void write(std::ostream& _out, const std::string& _name) const;
            };

            class ApexInfo {
                private:
                const std::string moduleName_;
                const std::string modulePath_;
                const std::optional<std::string> preinstalledModulePath_;
                const int64_t versionCode_;
                const std::string versionName_;
                const bool isFactory_;
                const bool isActive_;
                const std::optional<int64_t> lastUpdateMillis_;
                const bool provideSharedApexLibs_;
                public:
                ApexInfo(std::string moduleName, std::string modulePath, std::optional<std::string> preinstalledModulePath, int64_t versionCode, std::string versionName, bool isFactory, bool isActive, std::optional<int64_t> lastUpdateMillis, bool provideSharedApexLibs);
                const std::string& getModuleName() const;
                bool hasModuleName() const;
                const std::string& getModulePath() const;
                bool hasModulePath() const;
                const std::string& getPreinstalledModulePath() const;
                bool hasPreinstalledModulePath() const;
                const int64_t& getVersionCode() const;
                bool hasVersionCode() const;
                const std::string& getVersionName() const;
                bool hasVersionName() const;
                const bool& getIsFactory() const;
                bool hasIsFactory() const;
                const bool& getIsActive() const;
                bool hasIsActive() const;
                const int64_t& getLastUpdateMillis() const;
                bool hasLastUpdateMillis() const;
                const bool& getProvideSharedApexLibs() const;
                bool hasProvideSharedApexLibs() const;
                static ApexInfo read(xmlNode *root);
                void write(std::ostream& _out, const std::string& _name) const;
            };

        } // apex
    } // android
} // com
#endif // COM_ANDROID_APEX_H
