/*
 * Copyright (C) 2013 The Android Open Source Project
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
package android.net.wifi;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enterprise configuration details for Wi-Fi. Stores details about the EAP method
 * and any associated credentials.
 */
public class WifiEnterpriseConfig implements Parcelable {

    /** Key prefix for WAPI AS certificates. */
    public static final String WAPI_AS_CERTIFICATE = "WAPIAS_";

    /** Key prefix for WAPI user certificates. */
    public static final String WAPI_USER_CERTIFICATE = "WAPIUSR_";

    /**
     * Intent extra: name for WAPI AS certificates
     */
    public static final String EXTRA_WAPI_AS_CERTIFICATE_NAME =
            "android.net.wifi.extra.WAPI_AS_CERTIFICATE_NAME";

    /**
     * Intent extra: data for WAPI AS certificates
     */
    public static final String EXTRA_WAPI_AS_CERTIFICATE_DATA =
            "android.net.wifi.extra.WAPI_AS_CERTIFICATE_DATA";

    /**
     * Intent extra: name for WAPI USER certificates
     */
    public static final String EXTRA_WAPI_USER_CERTIFICATE_NAME =
            "android.net.wifi.extra.WAPI_USER_CERTIFICATE_NAME";

    /**
     * Intent extra: data for WAPI USER certificates
     */
    public static final String EXTRA_WAPI_USER_CERTIFICATE_DATA =
            "android.net.wifi.extra.WAPI_USER_CERTIFICATE_DATA";

    /** @hide */
    public static final String EMPTY_VALUE         = "NULL";
    /** @hide */
    public static final String EAP_KEY             = "eap";
    /** @hide */
    public static final String PHASE2_KEY          = "phase2";
    /** @hide */
    public static final String IDENTITY_KEY        = "identity";
    /** @hide */
    public static final String ANON_IDENTITY_KEY   = "anonymous_identity";
    /** @hide */
    public static final String PASSWORD_KEY        = "password";
    /** @hide */
    public static final String SUBJECT_MATCH_KEY   = "subject_match";
    /** @hide */
    public static final String ALTSUBJECT_MATCH_KEY = "altsubject_match";
    /** @hide */
    public static final String DOM_SUFFIX_MATCH_KEY = "domain_suffix_match";
    /** @hide */
    public static final String OPP_KEY_CACHING     = "proactive_key_caching";
    /** @hide */
    public static final String EAP_ERP             = "eap_erp";
    /** @hide */
    public static final String OCSP                = "ocsp";
    /** @hide */
    public static final String DECORATED_IDENTITY_PREFIX_KEY = "decorated_username_prefix";

    /**
     * String representing the keystore OpenSSL ENGINE's ID.
     * @hide
     */
    public static final String ENGINE_ID_KEYSTORE = "keystore";

    /**
     * String representing the keystore URI used for wpa_supplicant.
     * @hide
     */
    public static final String KEYSTORE_URI = "keystore://";

    /**
     * String representing the keystore URI used for wpa_supplicant,
     * Unlike #KEYSTORE_URI, this supports a list of space-delimited aliases
     * @hide
     */
    public static final String KEYSTORES_URI = "keystores://";

    /**
     * String representing a SHA-256 certificate hash used for wpa_supplicant.
     */
    private static final String CERT_HASH_PREFIX = "hash://server/sha256/";

    /**
     * String to set the engine value to when it should be enabled.
     * @hide
     */
    public static final String ENGINE_ENABLE = "1";

    /**
     * String to set the engine value to when it should be disabled.
     * @hide
     */
    public static final String ENGINE_DISABLE = "0";

    /**
     * Key prefix for CA certificates.
     * Note: copied from {@link android.security.Credentials#CA_CERTIFICATE} since it is @hide.
     */
    private static final String CA_CERTIFICATE = "CACERT_";
    /**
     * Key prefix for user certificates.
     * Note: copied from {@link android.security.Credentials#USER_CERTIFICATE} since it is @hide.
     */
    private static final String USER_CERTIFICATE = "USRCERT_";
    /**
     * Key prefix for user private and secret keys.
     * Note: copied from {@link android.security.Credentials#USER_PRIVATE_KEY} since it is @hide.
     */
    private static final String USER_PRIVATE_KEY = "USRPKEY_";

    /** @hide */
    public static final String CA_CERT_PREFIX = KEYSTORE_URI + CA_CERTIFICATE;
    /** @hide */
    public static final String CLIENT_CERT_PREFIX = KEYSTORE_URI + USER_CERTIFICATE;
    /** @hide */
    public static final String CLIENT_CERT_KEY     = "client_cert";
    /** @hide */
    public static final String CA_CERT_KEY         = "ca_cert";
    /** @hide */
    public static final String CA_PATH_KEY         = "ca_path";
    /** @hide */
    public static final String ENGINE_KEY          = "engine";
    /** @hide */
    public static final String ENGINE_ID_KEY       = "engine_id";
    /** @hide */
    public static final String PRIVATE_KEY_ID_KEY  = "key_id";
    /** @hide */
    public static final String REALM_KEY           = "realm";
    /** @hide */
    public static final String PLMN_KEY            = "plmn";
    /** @hide */
    public static final String CA_CERT_ALIAS_DELIMITER = " ";
    /** @hide */
    public static final String WAPI_CERT_SUITE_KEY = "wapi_cert_suite";

    /**
     * Do not use OCSP stapling (TLS certificate status extension)
     * @hide
     */
    @SystemApi
    public static final int OCSP_NONE = 0;

    /**
     * Try to use OCSP stapling, but not require response
     * @hide
     */
    @SystemApi
    public static final int OCSP_REQUEST_CERT_STATUS = 1;

    /**
     * Require valid OCSP stapling response
     * @hide
     */
    @SystemApi
    public static final int OCSP_REQUIRE_CERT_STATUS = 2;

    /**
     * Require valid OCSP stapling response for all not-trusted certificates in the server
     * certificate chain.
     * @apiNote This option is not supported by most SSL libraries and should not be used.
     * Specifying this option will most likely cause connection failures.
     * @hide
     */
    @SystemApi
    public static final int OCSP_REQUIRE_ALL_NON_TRUSTED_CERTS_STATUS = 3;

    /** @hide */
    @IntDef(prefix = {"OCSP_"}, value = {
            OCSP_NONE,
            OCSP_REQUEST_CERT_STATUS,
            OCSP_REQUIRE_CERT_STATUS,
            OCSP_REQUIRE_ALL_NON_TRUSTED_CERTS_STATUS
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Ocsp {}

    /**
     * Whether to use/require OCSP (Online Certificate Status Protocol) to check server certificate.
     * @hide
     */
    private @Ocsp int mOcsp = OCSP_NONE;

    // Fields to copy verbatim from wpa_supplicant.
    private static final String[] SUPPLICANT_CONFIG_KEYS = new String[] {
            IDENTITY_KEY,
            ANON_IDENTITY_KEY,
            PASSWORD_KEY,
            CLIENT_CERT_KEY,
            CA_CERT_KEY,
            SUBJECT_MATCH_KEY,
            ENGINE_KEY,
            ENGINE_ID_KEY,
            PRIVATE_KEY_ID_KEY,
            ALTSUBJECT_MATCH_KEY,
            DOM_SUFFIX_MATCH_KEY,
            CA_PATH_KEY
    };

    /**
     * Maximum length of a certificate.
     */
    private static final int CERTIFICATE_MAX_LENGTH = 8192;

    /**
     * Maximum length of the {@link #mKeyChainAlias} field.
     */
    private static final int KEYCHAIN_ALIAS_MAX_LENGTH = 256;

    /**
     * Maximum number of elements in a client certificate chain.
     */
    private static final int CLIENT_CERTIFICATE_CHAIN_MAX_ELEMENTS = 5;

    /**
     * Maximum number of elements in a list of CA certificates.
     */
    private static final int CA_CERTIFICATES_MAX_ELEMENTS = 100;

    /**
     * Fields that are supported in {@link #mFields}.
     * Each entry includes the supported field's key and its maximum allowed length.
     */
    private static final Map<String, Integer> SUPPORTED_FIELDS = Map.ofEntries(
            Map.entry(ALTSUBJECT_MATCH_KEY, 1024),
            Map.entry(ANON_IDENTITY_KEY, 1024),
            Map.entry(CA_CERT_KEY, CERTIFICATE_MAX_LENGTH),
            Map.entry(CA_PATH_KEY, 4096),
            Map.entry(CLIENT_CERT_KEY, CERTIFICATE_MAX_LENGTH),
            Map.entry(DECORATED_IDENTITY_PREFIX_KEY, 256),
            Map.entry(DOM_SUFFIX_MATCH_KEY, 256),
            Map.entry(EAP_ERP, 1),
            Map.entry(ENGINE_KEY, 1),
            Map.entry(ENGINE_ID_KEY, 64),
            Map.entry(IDENTITY_KEY, 256),
            Map.entry(OPP_KEY_CACHING, 1),
            Map.entry(PASSWORD_KEY, 256),
            Map.entry(PLMN_KEY, 16),
            Map.entry(PRIVATE_KEY_ID_KEY, 256),
            Map.entry(REALM_KEY, 256),
            Map.entry(SUBJECT_MATCH_KEY, 256),
            Map.entry(WAPI_CERT_SUITE_KEY, CERTIFICATE_MAX_LENGTH)
    );

    /**
     * Fields that have unquoted values in {@link #mFields}.
     */
    private static final List<String> UNQUOTED_KEYS = Arrays.asList(ENGINE_KEY, OPP_KEY_CACHING,
                                                                    EAP_ERP);
    /** Constant definition for TLS v1.0 which is used in {@link #setMinimumTlsVersion(int)} */
    public static final int TLS_V1_0 = 0;

    /** Constant definition for TLS v1.1 which is used in {@link #setMinimumTlsVersion(int)} */
    public static final int TLS_V1_1 = 1;

    /** Constant definition for TLS v1.2 which is used in {@link #setMinimumTlsVersion(int)} */
    public static final int TLS_V1_2 = 2;

    /** Constant definition for TLS v1.3 which is used in {@link #setMinimumTlsVersion(int)} */
    public static final int TLS_V1_3 = 3;

    /**
     * The minimum valid value for a TLS version.
     * @hide
     */
    public static final int TLS_VERSION_MIN = TLS_V1_0;

    /**
     * The maximum valid value for a TLS version.
     * @hide
     */
    public static final int TLS_VERSION_MAX = TLS_V1_3;

    /** @hide */
    @IntDef(prefix = {"TLS_"}, value = {
            TLS_V1_0,
            TLS_V1_1,
            TLS_V1_2,
            TLS_V1_3
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TlsVersion {}

    /**
     * TOFU is not enabled for this configuration.
     * @hide
     */
    public static final int TOFU_STATE_NOT_ENABLED = 0;

    /**
     * TOFU is enabled pre-connection.
     * @hide
     */
    public static final int TOFU_STATE_ENABLED_PRE_CONNECTION = 1;

    /**
     * Root CA was configured post-TOFU connection.
     * @hide
     */

    public static final int TOFU_STATE_CONFIGURE_ROOT_CA = 2;

    /**
     * Certificate pinning was used post-TOFU connection.
     * @hide
     */
    public static final int TOFU_STATE_CERT_PINNING = 3;

    /** @hide */
    @IntDef(prefix = {"TOFU_STATE_"}, value = {
            TOFU_STATE_NOT_ENABLED,
            TOFU_STATE_ENABLED_PRE_CONNECTION,
            TOFU_STATE_CONFIGURE_ROOT_CA,
            TOFU_STATE_CERT_PINNING
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TofuConnectionState {}

    /**
     * TOFU dialog has not been displayed to the user, or state is unknown.
     * @hide
     */
    public static final int TOFU_DIALOG_STATE_UNSPECIFIED = 0;

    /**
     * TOFU dialog was rejected by the user.
     * @hide
     */
    public static final int TOFU_DIALOG_STATE_REJECTED = 1;

    /**
     * TOFU dialog was accepted by the user.
     * @hide
     */
    public static final int TOFU_DIALOG_STATE_ACCEPTED = 2;

    /** @hide */
    @IntDef(prefix = {"TOFU_DIALOG_STATE_"}, value = {
            TOFU_DIALOG_STATE_UNSPECIFIED,
            TOFU_DIALOG_STATE_REJECTED,
            TOFU_DIALOG_STATE_ACCEPTED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TofuDialogState {}

    @UnsupportedAppUsage
    private HashMap<String, String> mFields = new HashMap<String, String>();
    private X509Certificate[] mCaCerts;
    private PrivateKey mClientPrivateKey;
    private X509Certificate[] mClientCertificateChain;
    private int mEapMethod = Eap.NONE;
    private int mPhase2Method = Phase2.NONE;
    private boolean mIsAppInstalledDeviceKeyAndCert = false;
    private boolean mIsAppInstalledCaCert = false;
    private String mKeyChainAlias;
    private boolean mIsTrustOnFirstUseEnabled = false;
    private boolean mUserApproveNoCaCert = false;
    // Default is 1.0, i.e. accept any TLS version.
    private int mMinimumTlsVersion = TLS_V1_0;
    private @TofuDialogState int mTofuDialogState = TOFU_DIALOG_STATE_UNSPECIFIED;
    private @TofuConnectionState int mTofuConnectionState = TOFU_STATE_NOT_ENABLED;

    // Not included in parceling, hashing, or equality because it is an internal, temporary value
    // which is valid only during an actual connection to a Passpoint network with an RCOI-based
    // subscription.
    private long mSelectedRcoi = 0;

    private boolean mIsStrictConservativePeerMode = false;

    private static final String TAG = "WifiEnterpriseConfig";

    public WifiEnterpriseConfig() {
        // Do not set defaults so that the enterprise fields that are not changed
        // by API are not changed underneath
        // This is essential because an app may not have all fields like password
        // available. It allows modification of subset of fields.

    }

    /**
     * Check whether a key is supported by {@link #mFields}.
     * @return true if the key is supported, false otherwise.
     */
    private static boolean isKeySupported(String key) {
        return SUPPORTED_FIELDS.containsKey(key);
    }

    /**
     * Check whether a value from {@link #mFields} has a valid length.
     * @return true if the length is valid, false otherwise.
     */
    private static boolean isFieldLengthValid(String key, String value) {
        int maxLength = SUPPORTED_FIELDS.getOrDefault(key, 0);
        return isFieldLengthValid(value, maxLength);
    }

    private static boolean isFieldLengthValid(String value, int maxLength) {
        if (value == null) return true;
        return value.length() <= maxLength;
    }

    /**
     * Check whether a key/value pair from {@link #mFields} is valid.
     * @return true if the key/value pair is valid, false otherwise.
     */
    private static boolean isFieldValid(String key, String value) {
        return isKeySupported(key) && isFieldLengthValid(key, value);
    }

    /**
     * Convert the {@link #mFields} map to a Bundle for parceling.
     * Unsupported keys will not be included in the Bundle.
     */
    private Bundle fieldMapToBundle() {
        Bundle bundle = new Bundle();
        for (Map.Entry<String, String> entry : mFields.entrySet()) {
            if (isFieldValid(entry.getKey(), entry.getValue())) {
                bundle.putString(entry.getKey(), entry.getValue());
            }
        }
        return bundle;
    }

    /**
     * Convert an unparceled Bundle to the {@link #mFields} map.
     * Unsupported keys will not be included in the map.
     */
    private static HashMap<String, String> bundleToFieldMap(Bundle bundle) {
        HashMap<String, String> fieldMap = new HashMap<>();
        if (bundle == null) return fieldMap;
        for (String key : bundle.keySet()) {
            String value = bundle.getString(key);
            if (isFieldValid(key, value)) {
                fieldMap.put(key, value);
            }
        }
        return fieldMap;
    }

    /**
     * Copy over the contents of the source WifiEnterpriseConfig object over to this object.
     *
     * @param source Source WifiEnterpriseConfig object.
     * @param ignoreMaskedPassword Set to true to ignore masked password field, false otherwise.
     * @param mask if |ignoreMaskedPassword| is set, check if the incoming password field is set
     *             to this value.
     */
    private void copyFrom(WifiEnterpriseConfig source, boolean ignoreMaskedPassword, String mask) {
        for (String key : source.mFields.keySet()) {
            String value = source.mFields.get(key);
            if (ignoreMaskedPassword && key.equals(PASSWORD_KEY)
                    && TextUtils.equals(value, mask)) {
                continue;
            }
            if (isFieldValid(key, value)) {
                mFields.put(key, source.mFields.get(key));
            }
        }
        if (source.mCaCerts != null) {
            mCaCerts = Arrays.copyOf(source.mCaCerts, source.mCaCerts.length);
        } else {
            mCaCerts = null;
        }
        mClientPrivateKey = source.mClientPrivateKey;
        if (source.mClientCertificateChain != null) {
            mClientCertificateChain = Arrays.copyOf(
                    source.mClientCertificateChain,
                    source.mClientCertificateChain.length);
        } else {
            mClientCertificateChain = null;
        }
        mKeyChainAlias = source.mKeyChainAlias;
        mEapMethod = source.mEapMethod;
        mPhase2Method = source.mPhase2Method;
        mIsAppInstalledDeviceKeyAndCert = source.mIsAppInstalledDeviceKeyAndCert;
        mIsAppInstalledCaCert = source.mIsAppInstalledCaCert;
        mOcsp = source.mOcsp;
        mIsTrustOnFirstUseEnabled = source.mIsTrustOnFirstUseEnabled;
        mUserApproveNoCaCert = source.mUserApproveNoCaCert;
        mSelectedRcoi = source.mSelectedRcoi;
        mMinimumTlsVersion = source.mMinimumTlsVersion;
        mIsStrictConservativePeerMode = source.mIsStrictConservativePeerMode;
        mTofuDialogState = source.mTofuDialogState;
        mTofuConnectionState = source.mTofuConnectionState;
    }

    /**
     * Copy constructor.
     * This copies over all the fields verbatim (does not ignore masked password fields).
     *
     * @param source Source WifiEnterpriseConfig object.
     */
    public WifiEnterpriseConfig(WifiEnterpriseConfig source) {
        copyFrom(source, false, "");
    }

    /**
     * Copy fields from the provided external WifiEnterpriseConfig.
     * This is needed to handle the WifiEnterpriseConfig objects which were sent by apps with the
     * password field masked.
     *
     * @param externalConfig External WifiEnterpriseConfig object.
     * @param mask String mask to compare against.
     * @hide
     */
    public void copyFromExternal(WifiEnterpriseConfig externalConfig, String mask) {
        copyFrom(externalConfig, true, convertToQuotedString(mask));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(fieldMapToBundle());
        dest.writeInt(mEapMethod);
        dest.writeInt(mPhase2Method);
        ParcelUtil.writeCertificates(dest, mCaCerts);
        ParcelUtil.writePrivateKey(dest, mClientPrivateKey);
        ParcelUtil.writeCertificates(dest, mClientCertificateChain);
        dest.writeString(mKeyChainAlias);
        dest.writeBoolean(mIsAppInstalledDeviceKeyAndCert);
        dest.writeBoolean(mIsAppInstalledCaCert);
        dest.writeInt(mOcsp);
        dest.writeBoolean(mIsTrustOnFirstUseEnabled);
        dest.writeBoolean(mUserApproveNoCaCert);
        dest.writeInt(mMinimumTlsVersion);
        dest.writeInt(mTofuDialogState);
        dest.writeInt(mTofuConnectionState);
    }

    public static final @android.annotation.NonNull Creator<WifiEnterpriseConfig> CREATOR =
            new Creator<WifiEnterpriseConfig>() {
                @Override
                public WifiEnterpriseConfig createFromParcel(Parcel in) {
                    WifiEnterpriseConfig enterpriseConfig = new WifiEnterpriseConfig();
                    enterpriseConfig.mFields = bundleToFieldMap(in.readBundle());
                    enterpriseConfig.mEapMethod = in.readInt();
                    enterpriseConfig.mPhase2Method = in.readInt();

                    X509Certificate[] caCerts = ParcelUtil.readCertificates(in);
                    if (caCerts != null && caCerts.length > CA_CERTIFICATES_MAX_ELEMENTS) {
                        Log.e(TAG, "List of CA certificates with size "
                                + caCerts.length + " received during unparceling");
                        enterpriseConfig.mCaCerts = null;
                    } else {
                        enterpriseConfig.mCaCerts = caCerts;
                    }

                    PrivateKey privateKey = ParcelUtil.readPrivateKey(in);
                    if (privateKey != null && privateKey.getEncoded() != null
                            && privateKey.getEncoded().length > CERTIFICATE_MAX_LENGTH) {
                        Log.e(TAG, "Invalid private key with size "
                                + privateKey.getEncoded().length + " received during unparceling");
                        enterpriseConfig.mClientPrivateKey = null;
                    } else {
                        enterpriseConfig.mClientPrivateKey = privateKey;
                    }

                    X509Certificate[] clientCertificateChain = ParcelUtil.readCertificates(in);
                    if (clientCertificateChain != null
                            && clientCertificateChain.length
                                    > CLIENT_CERTIFICATE_CHAIN_MAX_ELEMENTS) {
                        Log.e(TAG, "Client certificate chain with size "
                                + clientCertificateChain.length + " received during unparceling");
                        enterpriseConfig.mClientCertificateChain = null;
                    } else {
                        enterpriseConfig.mClientCertificateChain = clientCertificateChain;
                    }

                    String keyChainAlias = in.readString();
                    enterpriseConfig.mKeyChainAlias =
                            isFieldLengthValid(keyChainAlias, KEYCHAIN_ALIAS_MAX_LENGTH)
                                    ? keyChainAlias : "";
                    enterpriseConfig.mIsAppInstalledDeviceKeyAndCert = in.readBoolean();
                    enterpriseConfig.mIsAppInstalledCaCert = in.readBoolean();
                    enterpriseConfig.mOcsp = in.readInt();
                    enterpriseConfig.mIsTrustOnFirstUseEnabled = in.readBoolean();
                    enterpriseConfig.mUserApproveNoCaCert = in.readBoolean();
                    enterpriseConfig.mMinimumTlsVersion = in.readInt();
                    enterpriseConfig.mTofuDialogState = in.readInt();
                    enterpriseConfig.mTofuConnectionState = in.readInt();
                    return enterpriseConfig;
                }

                @Override
                public WifiEnterpriseConfig[] newArray(int size) {
                    return new WifiEnterpriseConfig[size];
                }
            };

    /** The Extensible Authentication Protocol method used */
    public static final class Eap {
        /** No EAP method used. Represents an empty config */
        public static final int NONE    = -1;
        /** Protected EAP */
        public static final int PEAP    = 0;
        /** EAP-Transport Layer Security */
        public static final int TLS     = 1;
        /** EAP-Tunneled Transport Layer Security */
        public static final int TTLS    = 2;
        /** EAP-Password */
        public static final int PWD     = 3;
        /** EAP-Subscriber Identity Module [RFC-4186] */
        public static final int SIM     = 4;
        /** EAP-Authentication and Key Agreement [RFC-4187] */
        public static final int AKA     = 5;
        /** EAP-Authentication and Key Agreement Prime [RFC-5448] */
        public static final int AKA_PRIME = 6;
        /** Hotspot 2.0 r2 OSEN */
        public static final int UNAUTH_TLS = 7;
        /** WAPI Certificate */
        public static final int WAPI_CERT = 8;
        /** @hide */
        public static final String[] strings =
                { "PEAP", "TLS", "TTLS", "PWD", "SIM", "AKA", "AKA'", "WFA-UNAUTH-TLS",
                        "WAPI_CERT" };

        /** Prevent initialization */
        private Eap() {}
    }

    /** The inner authentication method used */
    public static final class Phase2 {
        public static final int NONE        = 0;
        /** Password Authentication Protocol */
        public static final int PAP         = 1;
        /** Microsoft Challenge Handshake Authentication Protocol */
        public static final int MSCHAP      = 2;
        /** Microsoft Challenge Handshake Authentication Protocol v2 */
        public static final int MSCHAPV2    = 3;
        /** Generic Token Card */
        public static final int GTC         = 4;
        /** EAP-Subscriber Identity Module [RFC-4186] */
        public static final int SIM         = 5;
        /** EAP-Authentication and Key Agreement [RFC-4187] */
        public static final int AKA         = 6;
        /** EAP-Authentication and Key Agreement Prime [RFC-5448] */
        public static final int AKA_PRIME   = 7;
        private static final String AUTH_PREFIX = "auth=";
        private static final String AUTHEAP_PREFIX = "autheap=";
        /** @hide */
        public static final String[] strings = {EMPTY_VALUE, "PAP", "MSCHAP",
                "MSCHAPV2", "GTC", "SIM", "AKA", "AKA'" };

        /** Prevent initialization */
        private Phase2() {}
    }

    // Loader and saver interfaces for exchanging data with wpa_supplicant.
    // TODO: Decouple this object (which is just a placeholder of the configuration)
    // from the implementation that knows what wpa_supplicant wants.
    /**
     * Interface used for retrieving supplicant configuration from WifiEnterpriseConfig
     * @hide
     */
    public interface SupplicantSaver {
        /**
         * Set a value within wpa_supplicant configuration
         * @param key index to set within wpa_supplciant
         * @param value the value for the key
         * @return true if successful; false otherwise
         */
        boolean saveValue(String key, String value);
    }

    /**
     * Interface used for populating a WifiEnterpriseConfig from supplicant configuration
     * @hide
     */
    public interface SupplicantLoader {
        /**
         * Returns a value within wpa_supplicant configuration
         * @param key index to set within wpa_supplciant
         * @return string value if successful; null otherwise
         */
        String loadValue(String key);
    }

    /**
     * Internal use only; supply field values to wpa_supplicant config.  The configuration
     * process aborts on the first failed call on {@code saver}.
     * @param saver proxy for setting configuration in wpa_supplciant
     * @return whether the save succeeded on all attempts
     * @hide
     */
    public boolean saveToSupplicant(SupplicantSaver saver) {
        if (!isEapMethodValid()) {
            return false;
        }

        // wpa_supplicant can update the anonymous identity for these kinds of networks after
        // framework reads them, so make sure the framework doesn't try to overwrite them.
        boolean shouldNotWriteAnonIdentity = mEapMethod == WifiEnterpriseConfig.Eap.SIM
                || mEapMethod == WifiEnterpriseConfig.Eap.AKA
                || mEapMethod == WifiEnterpriseConfig.Eap.AKA_PRIME;
        for (String key : mFields.keySet()) {
            String value = mFields.get(key);
            if (!isFieldValid(key, value)) {
                continue;
            }
            if (shouldNotWriteAnonIdentity && ANON_IDENTITY_KEY.equals(key)) {
                continue;
            }
            if (!saver.saveValue(key, value)) {
                return false;
            }
        }

        if (!saver.saveValue(EAP_KEY, Eap.strings[mEapMethod])) {
            return false;
        }

        if (mEapMethod != Eap.TLS && mEapMethod != Eap.UNAUTH_TLS && mPhase2Method != Phase2.NONE) {
            boolean is_autheap = mEapMethod == Eap.TTLS && mPhase2Method == Phase2.GTC;
            String prefix = is_autheap ? Phase2.AUTHEAP_PREFIX : Phase2.AUTH_PREFIX;
            String value = convertToQuotedString(prefix + Phase2.strings[mPhase2Method]);
            return saver.saveValue(PHASE2_KEY, value);
        } else if (mPhase2Method == Phase2.NONE) {
            // By default, send a null phase 2 to clear old configuration values.
            return saver.saveValue(PHASE2_KEY, null);
        } else {
            Log.e(TAG, "WiFi enterprise configuration is invalid as it supplies a "
                    + "phase 2 method but the phase1 method does not support it.");
            return false;
        }
    }

    /**
     * Internal use only; retrieve configuration from wpa_supplicant config.
     * @param loader proxy for retrieving configuration keys from wpa_supplicant
     * @hide
     */
    public void loadFromSupplicant(SupplicantLoader loader) {
        for (String key : SUPPLICANT_CONFIG_KEYS) {
            String value = loader.loadValue(key);
            if (!isFieldValid(key, value)) {
                continue;
            } else if (value == null) {
                mFields.put(key, EMPTY_VALUE);
            } else {
                mFields.put(key, value);
            }
        }
        String eapMethod  = loader.loadValue(EAP_KEY);
        mEapMethod = getStringIndex(Eap.strings, eapMethod, Eap.NONE);

        String phase2Method = removeDoubleQuotes(loader.loadValue(PHASE2_KEY));
        // Remove "auth=" or "autheap=" prefix.
        if (phase2Method.startsWith(Phase2.AUTH_PREFIX)) {
            phase2Method = phase2Method.substring(Phase2.AUTH_PREFIX.length());
        } else if (phase2Method.startsWith(Phase2.AUTHEAP_PREFIX)) {
            phase2Method = phase2Method.substring(Phase2.AUTHEAP_PREFIX.length());
        }
        mPhase2Method = getStringIndex(Phase2.strings, phase2Method, Phase2.NONE);
    }

    /**
     * Set the EAP authentication method.
     * @param  eapMethod is one of {@link Eap}, except for {@link Eap#NONE}
     * @throws IllegalArgumentException on an invalid eap method
     */
    public void setEapMethod(int eapMethod) {
        switch (eapMethod) {
            /** Valid methods */
            case Eap.WAPI_CERT:
                mEapMethod = eapMethod;
                setPhase2Method(Phase2.NONE);
                break;
            case Eap.TLS:
            case Eap.UNAUTH_TLS:
                setPhase2Method(Phase2.NONE);
                /* fall through */
            case Eap.PEAP:
            case Eap.PWD:
            case Eap.TTLS:
            case Eap.SIM:
            case Eap.AKA:
            case Eap.AKA_PRIME:
                mEapMethod = eapMethod;
                setFieldValue(OPP_KEY_CACHING, "1");
                break;
            default:
                throw new IllegalArgumentException("Unknown EAP method");
        }
    }

    /**
     * Get the eap method.
     * @return eap method configured
     */
    public int getEapMethod() {
        return mEapMethod;
    }

    /**
     * Set Phase 2 authentication method. Sets the inner authentication method to be used in
     * phase 2 after setting up a secure channel
     * @param phase2Method is the inner authentication method and can be one of {@link Phase2}
     * @throws IllegalArgumentException on an invalid phase2 method
     */
    public void setPhase2Method(int phase2Method) {
        switch (phase2Method) {
            case Phase2.NONE:
            case Phase2.PAP:
            case Phase2.MSCHAP:
            case Phase2.MSCHAPV2:
            case Phase2.GTC:
            case Phase2.SIM:
            case Phase2.AKA:
            case Phase2.AKA_PRIME:
                mPhase2Method = phase2Method;
                break;
            default:
                throw new IllegalArgumentException("Unknown Phase 2 method");
        }
    }

    /**
     * Get the phase 2 authentication method.
     * @return a phase 2 method defined at {@link Phase2}
     * */
    public int getPhase2Method() {
        return mPhase2Method;
    }

    /**
     * Set the identity
     * @param identity
     */
    public void setIdentity(String identity) {
        setFieldValue(IDENTITY_KEY, identity, "");
    }

    /**
     * Get the identity
     * @return the identity
     */
    public String getIdentity() {
        return getFieldValue(IDENTITY_KEY);
    }

    /**
     * Set anonymous identity. This is used as the unencrypted identity with
     * certain EAP types
     * @param anonymousIdentity the anonymous identity
     */
    public void setAnonymousIdentity(String anonymousIdentity) {
        setFieldValue(ANON_IDENTITY_KEY, anonymousIdentity);
    }

    /**
     * Get the anonymous identity
     * @return anonymous identity
     */
    public String getAnonymousIdentity() {
        return getFieldValue(ANON_IDENTITY_KEY);
    }

    /**
     * Set the password.
     * @param password the password
     */
    public void setPassword(String password) {
        setFieldValue(PASSWORD_KEY, password);
    }

    /**
     * Get the password.
     *
     * Returns locally set password value. For networks fetched from
     * framework, returns "*".
     */
    public String getPassword() {
        return getFieldValue(PASSWORD_KEY);
    }

    /**
     * Encode a CA certificate alias so it does not contain illegal character.
     * @hide
     */
    public static String encodeCaCertificateAlias(String alias) {
        byte[] bytes = alias.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte o : bytes) {
            sb.append(String.format("%02x", o & 0xFF));
        }
        return sb.toString();
    }

    /**
     * Decode a previously-encoded CA certificate alias.
     * @hide
     */
    public static String decodeCaCertificateAlias(String alias) {
        byte[] data = new byte[alias.length() >> 1];
        for (int n = 0, position = 0; n < alias.length(); n += 2, position++) {
            data[position] = (byte) Integer.parseInt(alias.substring(n,  n + 2), 16);
        }
        try {
            return new String(data, StandardCharsets.UTF_8);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return alias;
        }
    }

    /**
     * Set a server certificate hash instead of a CA certificate for a TOFU connection
     *
     * @param certHash Server certificate hash to match against in subsequent connections
     * @hide
     */
    public void setServerCertificateHash(String certHash) {
        setFieldValue(CA_CERT_KEY, certHash, CERT_HASH_PREFIX);
    }

    /**
     * Set CA certificate alias.
     *
     * <p> See the {@link android.security.KeyChain} for details on installing or choosing
     * a certificate
     * </p>
     * @param alias identifies the certificate
     * @hide
     */
    @UnsupportedAppUsage
    public void setCaCertificateAlias(String alias) {
        setFieldValue(CA_CERT_KEY, alias, CA_CERT_PREFIX);
    }

    /**
     * Set CA certificate aliases. When creating installing the corresponding certificate to
     * the keystore, please use alias encoded by {@link #encodeCaCertificateAlias(String)}.
     *
     * <p> See the {@link android.security.KeyChain} for details on installing or choosing
     * a certificate.
     * </p>
     * @param aliases identifies the certificate. Can be null to indicate the absence of a
     *                certificate.
     * @hide
     */
    @SystemApi
    public void setCaCertificateAliases(@Nullable String[] aliases) {
        if (aliases == null) {
            setFieldValue(CA_CERT_KEY, null, CA_CERT_PREFIX);
        } else if (aliases.length == 1) {
            // Backwards compatibility: use the original cert prefix if setting only one alias.
            setCaCertificateAlias(aliases[0]);
        } else {
            // Use KEYSTORES_URI which supports multiple aliases.
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < aliases.length; i++) {
                if (i > 0) {
                    sb.append(CA_CERT_ALIAS_DELIMITER);
                }
                sb.append(encodeCaCertificateAlias(CA_CERTIFICATE + aliases[i]));
            }
            setFieldValue(CA_CERT_KEY, sb.toString(), KEYSTORES_URI);
        }
    }

    /**
     * Indicates whether or not this enterprise config has a CA certificate configured.
     */
    public boolean hasCaCertificate() {
        if (getCaCertificateAliases() != null) return true;
        if (getCaCertificates() != null) return true;
        if (!TextUtils.isEmpty(getCaPath())) return true;
        return false;
    }

    /**
     * Get CA certificate alias
     * @return alias to the CA certificate
     * @hide
     */
    @UnsupportedAppUsage
    public String getCaCertificateAlias() {
        return getFieldValue(CA_CERT_KEY, CA_CERT_PREFIX);
    }

    /**
     * Get CA certificate aliases.
     * @return alias to the CA certificate, or null if unset.
     * @hide
     */
    @Nullable
    @SystemApi
    public String[] getCaCertificateAliases() {
        String value = getFieldValue(CA_CERT_KEY);
        if (value.startsWith(CA_CERT_PREFIX)) {
            // Backwards compatibility: parse the original alias prefix.
            return new String[] {getFieldValue(CA_CERT_KEY, CA_CERT_PREFIX)};
        } else if (value.startsWith(KEYSTORES_URI)) {
            String values = value.substring(KEYSTORES_URI.length());

            String[] aliases = TextUtils.split(values, CA_CERT_ALIAS_DELIMITER);
            for (int i = 0; i < aliases.length; i++) {
                aliases[i] = decodeCaCertificateAlias(aliases[i]);
                if (aliases[i].startsWith(CA_CERTIFICATE)) {
                    aliases[i] = aliases[i].substring(CA_CERTIFICATE.length());
                }
            }
            return aliases.length != 0 ? aliases : null;
        } else {
            return TextUtils.isEmpty(value) ? null : new String[] {value};
        }
    }

    /**
     * Specify a X.509 certificate that identifies the server.
     *
     * <p>A default name is automatically assigned to the certificate and used
     * with this configuration. The framework takes care of installing the
     * certificate when the config is saved and removing the certificate when
     * the config is removed.
     *
     * Note: If no certificate is set for an Enterprise configuration, either by not calling this
     * API (or the {@link #setCaCertificates(X509Certificate[])}, or by calling it with null, then
     * the server certificate validation is skipped - which means that the connection is not secure.
     *
     * @param cert X.509 CA certificate
     * @throws IllegalArgumentException if not a CA certificate
     */
    public void setCaCertificate(@Nullable X509Certificate cert) {
        if (cert != null) {
            if (cert.getBasicConstraints() >= 0) {
                mIsAppInstalledCaCert = true;
                mCaCerts = new X509Certificate[] {cert};
            } else {
                mCaCerts = null;
                throw new IllegalArgumentException("Not a CA certificate");
            }
        } else {
            mCaCerts = null;
        }
    }

    /**
     * Specify a X.509 certificate that identifies the server.
     *
     * This hidden API allows setting self-signed certificate for Trust on First Use.
     *
     * @param cert X.509 CA certificate
     * @throws IllegalArgumentException if Trust on First Use is not enabled.
     * @hide
     */
    public void setCaCertificateForTrustOnFirstUse(@Nullable X509Certificate cert) {
        if (cert != null) {
            if (isTrustOnFirstUseEnabled()) {
                mIsAppInstalledCaCert = true;
                mCaCerts = new X509Certificate[] {cert};
            } else {
                mCaCerts = null;
                throw new IllegalArgumentException("Trust on First Use is not enabled.");
            }
        } else {
            mCaCerts = null;
        }
    }

    /**
     * Get CA certificate. If multiple CA certificates are configured previously,
     * return the first one.
     * @return X.509 CA certificate
     */
    @Nullable public X509Certificate getCaCertificate() {
        if (mCaCerts != null && mCaCerts.length > 0) {
            return mCaCerts[0];
        } else {
            return null;
        }
    }

    /**
     * Specify a list of X.509 certificates that identifies the server. The validation
     * passes if the CA of server certificate matches one of the given certificates.

     * <p>Default names are automatically assigned to the certificates and used
     * with this configuration. The framework takes care of installing the
     * certificates when the config is saved and removing the certificates when
     * the config is removed.
     *
     * Note: If no certificates are set for an Enterprise configuration, either by not calling this
     * API (or the {@link #setCaCertificate(X509Certificate)}, or by calling it with null, then the
     * server certificate validation is skipped - which means that the
     * connection is not secure.
     *
     * @param certs X.509 CA certificates
     * @throws IllegalArgumentException if any of the provided certificates is
     *     not a CA certificate, or if too many CA certificates are provided
     */
    public void setCaCertificates(@Nullable X509Certificate[] certs) {
        if (certs != null) {
            if (certs.length > CA_CERTIFICATES_MAX_ELEMENTS) {
                mCaCerts = null;
                throw new IllegalArgumentException("List of CA certificates contains more "
                        + "than the allowed number of elements");
            }
            X509Certificate[] newCerts = new X509Certificate[certs.length];
            for (int i = 0; i < certs.length; i++) {
                if (certs[i].getBasicConstraints() >= 0) {
                    newCerts[i] = certs[i];
                } else {
                    mCaCerts = null;
                    throw new IllegalArgumentException("Not a CA certificate");
                }
            }
            mCaCerts = newCerts;
            mIsAppInstalledCaCert = true;
        } else {
            mCaCerts = null;
        }
    }

    /**
     * Get CA certificates.
     */
    @Nullable public X509Certificate[] getCaCertificates() {
        if (mCaCerts != null && mCaCerts.length > 0) {
            return mCaCerts;
        } else {
            return null;
        }
    }

    /**
     * @hide
     */
    public void resetCaCertificate() {
        mCaCerts = null;
    }

    /**
     * Set the ca_path directive on wpa_supplicant.
     *
     * From wpa_supplicant documentation:
     *
     * Directory path for CA certificate files (PEM). This path may contain
     * multiple CA certificates in OpenSSL format. Common use for this is to
     * point to system trusted CA list which is often installed into directory
     * like /etc/ssl/certs. If configured, these certificates are added to the
     * list of trusted CAs. ca_cert may also be included in that case, but it is
     * not required.
     *
     * Note: If no certificate path is set for an Enterprise configuration, either by not calling
     * this API, or by calling it with null, and no certificate is set by
     * {@link #setCaCertificate(X509Certificate)} or {@link #setCaCertificates(X509Certificate[])},
     * then the server certificate validation is skipped - which means that the connection is not
     * secure.
     *
     * @param path The path for CA certificate files, or empty string to clear.
     * @hide
     */
    @SystemApi
    public void setCaPath(@NonNull String path) {
        setFieldValue(CA_PATH_KEY, path);
    }

    /**
     * Get the ca_path directive from wpa_supplicant.
     * @return The path for CA certificate files, or an empty string if unset.
     * @hide
     */
    @NonNull
    @SystemApi
    public String getCaPath() {
        return getFieldValue(CA_PATH_KEY);
    }

    /**
     * Set Client certificate alias.
     *
     * <p> See the {@link android.security.KeyChain} for details on installing or choosing
     * a certificate
     * </p>
     * @param alias identifies the certificate, or empty string to clear.
     * @hide
     */
    @SystemApi
    public void setClientCertificateAlias(@NonNull String alias) {
        setFieldValue(CLIENT_CERT_KEY, alias, CLIENT_CERT_PREFIX);
        setFieldValue(PRIVATE_KEY_ID_KEY, alias, USER_PRIVATE_KEY);
        // Also, set engine parameters
        if (TextUtils.isEmpty(alias)) {
            setFieldValue(ENGINE_KEY, ENGINE_DISABLE);
            setFieldValue(ENGINE_ID_KEY, "");
        } else {
            setFieldValue(ENGINE_KEY, ENGINE_ENABLE);
            setFieldValue(ENGINE_ID_KEY, ENGINE_ID_KEYSTORE);
        }
    }

    /**
     * Get client certificate alias.
     * @return alias to the client certificate, or an empty string if unset.
     * @hide
     */
    @NonNull
    @SystemApi
    public String getClientCertificateAlias() {
        return getFieldValue(CLIENT_CERT_KEY, CLIENT_CERT_PREFIX);
    }

    /**
     * Specify a private key and client certificate for client authorization.
     *
     * <p>A default name is automatically assigned to the key entry and used
     * with this configuration.  The framework takes care of installing the
     * key entry when the config is saved and removing the key entry when
     * the config is removed.

     * @param privateKey a PrivateKey instance for the end certificate.
     * @param clientCertificate an X509Certificate representing the end certificate.
     * @throws IllegalArgumentException for an invalid key or certificate.
     */
    public void setClientKeyEntry(PrivateKey privateKey, X509Certificate clientCertificate) {
        X509Certificate[] clientCertificates = null;
        if (clientCertificate != null) {
            clientCertificates = new X509Certificate[] {clientCertificate};
        }
        setClientKeyEntryWithCertificateChain(privateKey, clientCertificates);
    }

    /**
     * Specify a private key and client certificate chain for client authorization.
     *
     * <p>A default name is automatically assigned to the key entry and used
     * with this configuration.  The framework takes care of installing the
     * key entry when the config is saved and removing the key entry when
     * the config is removed.
     *
     * @param privateKey a PrivateKey instance for the end certificate.
     * @param clientCertificateChain an array of X509Certificate instances which starts with
     *         end certificate and continues with additional CA certificates necessary to
     *         link the end certificate with some root certificate known by the authenticator.
     * @throws IllegalArgumentException for an invalid key or certificate.
     */
    public void setClientKeyEntryWithCertificateChain(PrivateKey privateKey,
            X509Certificate[] clientCertificateChain) {
        X509Certificate[] newCerts = null;
        if (clientCertificateChain != null && clientCertificateChain.length > 0) {
            // We validate that this is a well formed chain that starts
            // with an end-certificate and is followed by CA certificates.
            // We don't validate that each following certificate verifies
            // the previous. https://en.wikipedia.org/wiki/Chain_of_trust
            //
            // Basic constraints is an X.509 extension type that defines
            // whether a given certificate is allowed to sign additional
            // certificates and what path length restrictions may exist.
            // We use this to judge whether the certificate is an end
            // certificate or a CA certificate.
            // https://cryptography.io/en/latest/x509/reference/
            if (clientCertificateChain.length > CLIENT_CERTIFICATE_CHAIN_MAX_ELEMENTS) {
                throw new IllegalArgumentException(
                        "Certificate chain contains more than the allowed number of elements");
            }
            if (clientCertificateChain[0].getBasicConstraints() != -1) {
                throw new IllegalArgumentException(
                        "First certificate in the chain must be a client end certificate");
            }

            for (int i = 1; i < clientCertificateChain.length; i++) {
                if (clientCertificateChain[i].getBasicConstraints() == -1) {
                    throw new IllegalArgumentException(
                            "All certificates following the first must be CA certificates");
                }
            }
            newCerts = Arrays.copyOf(clientCertificateChain,
                    clientCertificateChain.length);

            if (privateKey == null) {
                throw new IllegalArgumentException("Client cert without a private key");
            }
            byte[] encodedKey = privateKey.getEncoded();
            if (encodedKey == null) {
                throw new IllegalArgumentException("Private key cannot be encoded");
            }
            if (encodedKey.length > CERTIFICATE_MAX_LENGTH) {
                throw new IllegalArgumentException(
                        "Private key exceeds the maximum allowed length");
            }
        }

        mClientPrivateKey = privateKey;
        mClientCertificateChain = newCerts;
        mIsAppInstalledDeviceKeyAndCert = true;
    }

    /**
     * Specify a key pair via KeyChain alias for client authentication.
     *
     * The alias should refer to a key pair in KeyChain that is allowed for WiFi authentication.
     *
     * @param alias key pair alias
     * @see android.app.admin.DevicePolicyManager#grantKeyPairToWifiAuth(String)
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public void setClientKeyPairAlias(@NonNull String alias) {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        if (!isFieldLengthValid(alias, KEYCHAIN_ALIAS_MAX_LENGTH)) {
            throw new IllegalArgumentException();
        }
        mKeyChainAlias = alias;
    }

    /**
     * Get KeyChain alias to use for client authentication.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public @Nullable String getClientKeyPairAlias() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        return mKeyChainAlias;
    }

    /**
     * Get KeyChain alias to use for client authentication without SDK check.
     * @hide
     */
    public @Nullable String getClientKeyPairAliasInternal() {
        return mKeyChainAlias;
    }

    /**
     * Get client certificate
     *
     * @return X.509 client certificate
     */
    public X509Certificate getClientCertificate() {
        if (mClientCertificateChain != null && mClientCertificateChain.length > 0) {
            return mClientCertificateChain[0];
        } else {
            return null;
        }
    }

    /**
     * Get the complete client certificate chain in the same order as it was last supplied.
     *
     * <p>If the chain was last supplied by a call to
     * {@link #setClientKeyEntry(java.security.PrivateKey, java.security.cert.X509Certificate)}
     * with a non-null * certificate instance, a single-element array containing the certificate
     * will be * returned. If {@link #setClientKeyEntryWithCertificateChain(
     * java.security.PrivateKey, java.security.cert.X509Certificate[])} was last called with a
     * non-empty array, this array will be returned in the same order as it was supplied.
     * Otherwise, {@code null} will be returned.
     *
     * @return X.509 client certificates
     */
    @Nullable public X509Certificate[] getClientCertificateChain() {
        if (mClientCertificateChain != null && mClientCertificateChain.length > 0) {
            return mClientCertificateChain;
        } else {
            return null;
        }
    }

    /**
     * @hide
     */
    public void resetClientKeyEntry() {
        mClientPrivateKey = null;
        mClientCertificateChain = null;
    }

    /**
     * Get the client private key as supplied in {@link #setClientKeyEntryWithCertificateChain}, or
     * null if unset.
     */
    @Nullable
    public PrivateKey getClientPrivateKey() {
        return mClientPrivateKey;
    }

    /**
     * Set subject match (deprecated). This is the substring to be matched against the subject of
     * the authentication server certificate.
     * @param subjectMatch substring to be matched
     * @deprecated in favor of altSubjectMatch
     */
    public void setSubjectMatch(String subjectMatch) {
        setFieldValue(SUBJECT_MATCH_KEY, subjectMatch);
    }

    /**
     * Get subject match (deprecated)
     * @return the subject match string
     * @deprecated in favor of altSubjectMatch
     */
    public String getSubjectMatch() {
        return getFieldValue(SUBJECT_MATCH_KEY);
    }

    /**
     * Set alternate subject match. This is the substring to be matched against the
     * alternate subject of the authentication server certificate.
     *
     * Note: If no alternate subject is set for an Enterprise configuration, either by not calling
     * this API, or by calling it with null, or not setting domain suffix match using the
     * {@link #setDomainSuffixMatch(String)}, then the server certificate validation is incomplete -
     * which means that the connection is not secure.
     *
     * @param altSubjectMatch substring to be matched, for example
     *                     DNS:server.example.com;EMAIL:server@example.com
     */
    public void setAltSubjectMatch(String altSubjectMatch) {
        setFieldValue(ALTSUBJECT_MATCH_KEY, altSubjectMatch);
    }

    /**
     * Get alternate subject match
     * @return the alternate subject match string
     */
    public String getAltSubjectMatch() {
        return getFieldValue(ALTSUBJECT_MATCH_KEY);
    }

    /**
     * Set the domain_suffix_match directive on wpa_supplicant. This is the parameter to use
     * for Hotspot 2.0 defined matching of AAA server certs per WFA HS2.0 spec, section 7.3.3.2,
     * second paragraph.
     *
     * <p>From wpa_supplicant documentation:
     * <p>Constraint for server domain name. If set, this FQDN is used as a suffix match requirement
     * for the AAAserver certificate in SubjectAltName dNSName element(s). If a matching dNSName is
     * found, this constraint is met.
     * <p>Suffix match here means that the host/domain name is compared one label at a time starting
     * from the top-level domain and all the labels in domain_suffix_match shall be included in the
     * certificate. The certificate may include additional sub-level labels in addition to the
     * required labels.
     * <p>More than one match string can be provided by using semicolons to separate the strings
     * (e.g., example.org;example.com). When multiple strings are specified, a match with any one of
     * the values is considered a sufficient match for the certificate, i.e., the conditions are
     * ORed ogether.
     * <p>For example, domain_suffix_match=example.com would match test.example.com but would not
     * match test-example.com.
     *
     * Note: If no domain suffix is set for an Enterprise configuration, either by not calling this
     * API, or by calling it with null, or not setting alternate subject match using the
     * {@link #setAltSubjectMatch(String)}, then the server certificate
     * validation is incomplete - which means that the connection is not secure.
     *
     * @param domain The domain value
     */
    public void setDomainSuffixMatch(String domain) {
        setFieldValue(DOM_SUFFIX_MATCH_KEY, domain);
    }

    /**
     * Get the domain_suffix_match value. See setDomSuffixMatch.
     * @return The domain value.
     */
    public String getDomainSuffixMatch() {
        return getFieldValue(DOM_SUFFIX_MATCH_KEY);
    }

    /**
     * Set realm for Passpoint credential; realm identifies a set of networks where your
     * Passpoint credential can be used
     * @param realm the realm
     */
    public void setRealm(String realm) {
        setFieldValue(REALM_KEY, realm);
    }

    /**
     * Get realm for Passpoint credential; see {@link #setRealm(String)} for more information
     * @return the realm
     */
    public String getRealm() {
        return getFieldValue(REALM_KEY);
    }

    /**
     * Set selected RCOI for Passpoint: Indicates which RCOI was selected on a particular network
     * @param selectedRcoi the selected RCOI on a particular network
     * @hide
     */
    public void setSelectedRcoi(long selectedRcoi) {
        mSelectedRcoi = selectedRcoi;
    }

    /**
     * Get the selected RCOI matched for a Passpoint connection
     * @return the selected RCOI
     * @hide
     */
    public long getSelectedRcoi() {
        return mSelectedRcoi;
    }

    /**
     * Enable or disable the conservative peer mode, this is only meaningful for
     * EAP-SIM/AKA/AKA'
     * @param enable true if the conservative peer mode is enabled.
     * @hide
     */
    public void setStrictConservativePeerMode(boolean enable) {
        mIsStrictConservativePeerMode = enable;
    }

    /**
     * Check if the conservative peer mode is enabled or not, this is only meaningful for
     * EAP-SIM/AKA/AKA'
     * @hide
     */
    public boolean getStrictConservativePeerMode() {
        return mIsStrictConservativePeerMode;
    }

    /**
     * Set plmn (Public Land Mobile Network) of the provider of Passpoint credential
     * @param plmn the plmn value derived from mcc (mobile country code) & mnc (mobile network code)
     */
    public void setPlmn(String plmn) {
        setFieldValue(PLMN_KEY, plmn);
    }

    /**
     * Get plmn (Public Land Mobile Network) for Passpoint credential; see {@link #setPlmn
     * (String)} for more information
     * @return the plmn
     */
    public String getPlmn() {
        return getFieldValue(PLMN_KEY);
    }

    /** See {@link WifiConfiguration#getKeyIdForCredentials} @hide */
    public String getKeyId(WifiEnterpriseConfig current) {
        // If EAP method is not initialized, use current config details
        if (mEapMethod == Eap.NONE) {
            return (current != null) ? current.getKeyId(null) : EMPTY_VALUE;
        }
        if (!isEapMethodValid()) {
            return EMPTY_VALUE;
        }
        return Eap.strings[mEapMethod] + "_" + Phase2.strings[mPhase2Method];
    }

    private String removeDoubleQuotes(String string) {
        if (TextUtils.isEmpty(string)) return "";
        int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"')
                && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }

    private String convertToQuotedString(String string) {
        return "\"" + string + "\"";
    }

    /**
     * Returns the index at which the toBeFound string is found in the array.
     * @param arr array of strings
     * @param toBeFound string to be found
     * @param defaultIndex default index to be returned when string is not found
     * @return the index into array
     */
    private int getStringIndex(String arr[], String toBeFound, int defaultIndex) {
        if (TextUtils.isEmpty(toBeFound)) return defaultIndex;
        for (int i = 0; i < arr.length; i++) {
            if (toBeFound.equals(arr[i])) return i;
        }
        return defaultIndex;
    }

    /**
     * Returns the field value for the key with prefix removed.
     * @param key into the hash
     * @param prefix is the prefix that the value may have
     * @return value
     * @hide
     */
    private String getFieldValue(String key, String prefix) {
        if (!isKeySupported(key)) {
            return "";
        }

        String value = mFields.get(key);
        // Uninitialized or known to be empty after reading from supplicant
        if (TextUtils.isEmpty(value) || EMPTY_VALUE.equals(value)) return "";

        value = removeDoubleQuotes(value);
        if (value.startsWith(prefix)) {
            return value.substring(prefix.length());
        } else {
            return value;
        }
    }

    /**
     * Returns the field value for the key.
     * @param key into the hash
     * @return value
     * @hide
     */
    public String getFieldValue(String key) {
        return getFieldValue(key, "");
    }

    /**
     * Set a value with an optional prefix at key
     * @param key into the hash
     * @param value to be set
     * @param prefix an optional value to be prefixed to actual value
     * @hide
     */
    private void setFieldValue(String key, String value, String prefix) {
        if (!isFieldValid(key, value)) {
            return;
        }
        if (TextUtils.isEmpty(value)) {
            mFields.put(key, EMPTY_VALUE);
        } else {
            String valueToSet;
            if (!UNQUOTED_KEYS.contains(key)) {
                valueToSet = convertToQuotedString(prefix + value);
            } else {
                valueToSet = prefix + value;
            }
            mFields.put(key, valueToSet);
        }
    }

    /**
     * Set a value at key
     * @param key into the hash
     * @param value to be set
     * @hide
     */
    public void setFieldValue(String key, String value) {
        setFieldValue(key, value, "");
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (String key : mFields.keySet()) {
            // Don't display password in toString().
            String value = PASSWORD_KEY.equals(key) ? "<removed>" : mFields.get(key);
            sb.append(key).append(" ").append(value).append("\n");
        }
        if (mEapMethod >= 0 && mEapMethod < Eap.strings.length) {
            sb.append("eap_method: ").append(Eap.strings[mEapMethod]).append("\n");
        }
        if (mPhase2Method > 0 && mPhase2Method < Phase2.strings.length) {
            sb.append("phase2_method: ").append(Phase2.strings[mPhase2Method]).append("\n");
        }
        sb.append(" ocsp: ").append(mOcsp).append("\n");
        sb.append(" trust_on_first_use: ").append(mIsTrustOnFirstUseEnabled).append("\n");
        sb.append(" user_approve_no_ca_cert: ").append(mUserApproveNoCaCert).append("\n");
        sb.append(" selected_rcoi: ").append(mSelectedRcoi).append("\n");
        sb.append(" minimum_tls_version: ").append(mMinimumTlsVersion).append("\n");
        sb.append(" enable_conservative_peer_mode: ")
                .append(mIsStrictConservativePeerMode).append("\n");
        sb.append(" tofu_dialog_state: ").append(mTofuDialogState).append("\n");
        sb.append(" tofu_connection_state: ").append(mTofuConnectionState).append("\n");
        return sb.toString();
    }

    /**
     * Returns whether the EAP method data is valid, i.e., whether mEapMethod and mPhase2Method
     * are valid indices into {@code Eap.strings[]} and {@code Phase2.strings[]} respectively.
     */
    private boolean isEapMethodValid() {
        if (mEapMethod == Eap.NONE) {
            Log.e(TAG, "WiFi enterprise configuration is invalid as it supplies no EAP method.");
            return false;
        }
        if (mEapMethod < 0 || mEapMethod >= Eap.strings.length) {
            Log.e(TAG, "mEapMethod is invald for WiFi enterprise configuration: " + mEapMethod);
            return false;
        }
        if (mPhase2Method < 0 || mPhase2Method >= Phase2.strings.length) {
            Log.e(TAG, "mPhase2Method is invald for WiFi enterprise configuration: "
                    + mPhase2Method);
            return false;
        }
        return true;
    }

    /**
     * Check if certificate was installed by an app, or manually (not by an app). If true,
     * certificate and keys will be removed from key storage when this network is removed. If not,
     * then certificates and keys remain persistent until the user manually removes them.
     *
     * @return true if certificate was installed by an app, false if certificate was installed
     * manually by the user.
     * @hide
     */
    public boolean isAppInstalledDeviceKeyAndCert() {
        return mIsAppInstalledDeviceKeyAndCert;
    }

    /**
     * Initialize the value of the app installed device key and cert flag.
     *
     * @param isAppInstalledDeviceKeyAndCert true or false
     * @hide
     */
    public void initIsAppInstalledDeviceKeyAndCert(boolean isAppInstalledDeviceKeyAndCert) {
        mIsAppInstalledDeviceKeyAndCert = isAppInstalledDeviceKeyAndCert;
    }

    /**
     * Check if CA certificate was installed by an app, or manually (not by an app). If true,
     * CA certificate will be removed from key storage when this network is removed. If not,
     * then certificates and keys remain persistent until the user manually removes them.
     *
     * @return true if CA certificate was installed by an app, false if CA certificate was installed
     * manually by the user.
     * @hide
     */
    public boolean isAppInstalledCaCert() {
        return mIsAppInstalledCaCert;
    }

    /**
     * Initialize the value of the app installed root CA cert flag.
     *
     * @param isAppInstalledCaCert true or false
     * @hide
     */
    public void initIsAppInstalledCaCert(boolean isAppInstalledCaCert) {
        mIsAppInstalledCaCert = isAppInstalledCaCert;
    }

    /**
     * Set the OCSP type.
     * @param ocsp is one of {@link ##OCSP_NONE}, {@link #OCSP_REQUEST_CERT_STATUS},
     *                   {@link #OCSP_REQUIRE_CERT_STATUS} or
     *                   {@link #OCSP_REQUIRE_ALL_NON_TRUSTED_CERTS_STATUS}
     * @throws IllegalArgumentException if the OCSP type is invalid
     * @hide
     */
    @SystemApi
    public void setOcsp(@Ocsp int ocsp) {
        if (ocsp >= OCSP_NONE && ocsp <= OCSP_REQUIRE_ALL_NON_TRUSTED_CERTS_STATUS) {
            mOcsp = ocsp;
        } else {
            throw new IllegalArgumentException("Invalid OCSP type.");
        }
    }

    /**
     * Get the OCSP type.
     * @hide
     */
    @SystemApi
    public @Ocsp int getOcsp() {
        return mOcsp;
    }

    /**
     * Utility method to determine whether the configuration's authentication method is SIM-based.
     *
     * @return true if the credential information requires SIM card for current authentication
     * method, otherwise it returns false.
     */
    public boolean isAuthenticationSimBased() {
        if (mEapMethod == Eap.SIM || mEapMethod == Eap.AKA || mEapMethod == Eap.AKA_PRIME) {
            return true;
        }
        if (mEapMethod == Eap.PEAP) {
            return mPhase2Method == Phase2.SIM || mPhase2Method == Phase2.AKA
                    || mPhase2Method == Phase2.AKA_PRIME;
        }
        return false;
    }

    /**
     * Set the WAPI certificate suite name on wpa_supplicant.
     *
     * If this field is not specified, WAPI-CERT uses ASU ID from WAI packet
     * as the certificate suite name automatically.
     *
     * @param wapiCertSuite The name for WAPI certificate suite, or empty string to clear.
     * @hide
     */
    @SystemApi
    public void setWapiCertSuite(@NonNull String wapiCertSuite) {
        setFieldValue(WAPI_CERT_SUITE_KEY, wapiCertSuite);
    }

    /**
     * Get the WAPI certificate suite name
     * @return the certificate suite name
     * @hide
     */
    @NonNull
    @SystemApi
    public String getWapiCertSuite() {
        return getFieldValue(WAPI_CERT_SUITE_KEY);
    }

    /**
     * Determines whether an Enterprise configuration's EAP method requires a Root CA certification
     * to validate the authentication server i.e. PEAP, TLS, UNAUTH_TLS, or TTLS.
     * @return True if configuration requires a CA certification, false otherwise.
     */
    public boolean isEapMethodServerCertUsed() {
        return mEapMethod == Eap.PEAP || mEapMethod == Eap.TLS || mEapMethod == Eap.TTLS
                || mEapMethod == Eap.UNAUTH_TLS;
    }
    /**
     * Determines whether an Enterprise configuration enables server certificate validation.
     * <p>
     * The caller can determine, along with {@link #isEapMethodServerCertUsed()}, if an
     * Enterprise configuration enables server certificate validation, which is a mandatory
     * requirement for networks that use TLS based EAP methods. A configuration that does not
     * enable server certificate validation will be ignored and will not be considered for
     * network selection. A network suggestion with such a configuration will cause an
     * IllegalArgumentException to be thrown when suggested.
     * Server validation is achieved by the following:
     * - Either certificate or CA path is configured.
     * - Either alternative subject match or domain suffix match is set.
     * @return True for server certificate validation is enabled, false otherwise.
     * @throws IllegalStateException on configuration which doesn't use server certificate.
     * @see #isEapMethodServerCertUsed()
     */
    public boolean isServerCertValidationEnabled() {
        if (!isEapMethodServerCertUsed()) {
            throw new IllegalStateException("Configuration doesn't use server certificates for "
                    + "authentication");
        }
        return isMandatoryParameterSetForServerCertValidation();
    }

    /**
     * Helper method to check if mandatory parameter for server cert validation is set.
     * @hide
     */
    public boolean isMandatoryParameterSetForServerCertValidation() {
        if (TextUtils.isEmpty(getAltSubjectMatch())
                && TextUtils.isEmpty(getDomainSuffixMatch())) {
            // Both subject and domain match are not set, validation is not enabled.
            return false;
        }
        if (mIsAppInstalledCaCert) {
            // CA certificate is installed by App, validation is enabled.
            return true;
        }
        if (getCaCertificateAliases() != null) {
            // CA certificate alias from keyStore is set, validation is enabled.
            return true;
        }
        return !TextUtils.isEmpty(getCaPath());
    }

    /**
     * Check if a given certificate Get the Suite-B cipher from the certificate
     *
     * @param x509Certificate Certificate to process
     * @return true if the certificate OID matches the Suite-B requirements for RSA or ECDSA
     * certificates, or false otherwise.
     * @hide
     */
    public static boolean isSuiteBCipherCert(@Nullable X509Certificate x509Certificate) {
        if (x509Certificate == null) {
            return false;
        }
        final String sigAlgOid = x509Certificate.getSigAlgOID();

        // Wi-Fi alliance requires the use of both ECDSA secp384r1 and RSA 3072 certificates
        // in WPA3-Enterprise 192-bit security networks, which are also known as Suite-B-192
        // networks, even though NSA Suite-B-192 mandates ECDSA only. The use of the term
        // Suite-B was already coined in the IEEE 802.11-2016 specification for
        // AKM 00-0F-AC but the test plan for WPA3-Enterprise 192-bit for APs mandates
        // support for both RSA and ECDSA, and for STAs it mandates ECDSA and optionally
        // RSA. In order to be compatible with all WPA3-Enterprise 192-bit deployments,
        // we are supporting both types here.
        if (sigAlgOid.equals("1.2.840.113549.1.1.12")) {
            // sha384WithRSAEncryption
            if (x509Certificate.getPublicKey() instanceof RSAPublicKey) {
                final RSAPublicKey rsaPublicKey = (RSAPublicKey) x509Certificate.getPublicKey();
                if (rsaPublicKey.getModulus() != null
                        && rsaPublicKey.getModulus().bitLength() >= 3072) {
                    return true;
                }
            }
        } else if (sigAlgOid.equals("1.2.840.10045.4.3.3")) {
            // ecdsa-with-SHA384
            if (x509Certificate.getPublicKey() instanceof ECPublicKey) {
                final ECPublicKey ecPublicKey = (ECPublicKey) x509Certificate.getPublicKey();
                final ECParameterSpec ecParameterSpec = ecPublicKey.getParams();

                if (ecParameterSpec != null && ecParameterSpec.getOrder() != null
                        && ecParameterSpec.getOrder().bitLength() >= 384) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Set a prefix for a decorated identity as per RFC 7542.
     * This prefix must contain a list of realms (could be a list of 1) delimited by a '!'
     * character. e.g. homerealm.example.org! or proxyrealm.example.net!homerealm.example.org!
     * A prefix of "homerealm.example.org!" will generate a decorated identity that
     * looks like: homerealm.example.org!user@otherrealm.example.net
     * Calling with a null parameter will clear the decorated prefix.
     * Note: Caller must verify that the device supports this feature by calling
     * {@link WifiManager#isDecoratedIdentitySupported()}
     *
     * @param decoratedIdentityPrefix The prefix to add to the outer/anonymous identity
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public void setDecoratedIdentityPrefix(@Nullable String decoratedIdentityPrefix) {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        if (!TextUtils.isEmpty(decoratedIdentityPrefix) && !decoratedIdentityPrefix.endsWith("!")) {
            throw new IllegalArgumentException(
                    "Decorated identity prefix must be delimited by '!'");
        }
        setFieldValue(DECORATED_IDENTITY_PREFIX_KEY, decoratedIdentityPrefix);
    }

    /**
     * Get the decorated identity prefix.
     *
     * @return The decorated identity prefix
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public @Nullable String getDecoratedIdentityPrefix() {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        final String decoratedId = getFieldValue(DECORATED_IDENTITY_PREFIX_KEY);
        return decoratedId.isEmpty() ? null : decoratedId;
    }

    /**
     * Enable Trust On First Use.
     *
     * Trust On First Use (TOFU) simplifies manual or partial configurations
     * of TLS-based EAP networks. TOFU operates by installing the Root CA cert
     * which is received from the server during an initial connection to a new network.
     * Such installation is gated by user approval.
     * Use only when it is not possible to configure the Root CA cert for the server.
     * <br>
     * Note: If a Root CA cert is already configured, this option is ignored,
     * e.g. if {@link #setCaCertificate(X509Certificate)}, or
     * {@link #setCaCertificates(X509Certificate[])} is called.
     *
     * @param enable true to enable; false otherwise (the default if the method is not called).
     */
    public void enableTrustOnFirstUse(boolean enable) {
        mIsTrustOnFirstUseEnabled = enable;
        if (mTofuConnectionState != TOFU_STATE_CONFIGURE_ROOT_CA &&
                mTofuConnectionState != TOFU_STATE_CERT_PINNING) {
            // Override the current pre-connection state.
            mTofuConnectionState = enable ?
                    TOFU_STATE_ENABLED_PRE_CONNECTION : TOFU_STATE_NOT_ENABLED;
        }
    }

    /**
     * Indicates whether or not Trust On First Use (TOFU) is enabled.
     *
     * @return Trust On First Use is enabled or not.
     */
    public boolean isTrustOnFirstUseEnabled() {
        return mIsTrustOnFirstUseEnabled;
    }

    /**
     * Set the TOFU connection state.
     * @hide
     */
    public void setTofuConnectionState(@TofuConnectionState int state) {
        if (state < TOFU_STATE_NOT_ENABLED || state > TOFU_STATE_CERT_PINNING) {
            Log.e(TAG, "Invalid TOFU connection state received. state=" + state);
            return;
        }
        mTofuConnectionState = state;
    }

    /**
     * Get the TOFU connection state.
     * @hide
     */
    public @TofuConnectionState int getTofuConnectionState() {
        return mTofuConnectionState;
    }

    /**
     * Indicate whether the user accepted the TOFU dialog.
     * @hide
     */
    public void setTofuDialogApproved(boolean approved) {
        mTofuDialogState = approved ? TOFU_DIALOG_STATE_ACCEPTED : TOFU_DIALOG_STATE_REJECTED;
    }

    /**
     * Set the TOFU dialog state.
     * @hide
     */
    public void setTofuDialogState(@TofuDialogState int state) {
        if (state < TOFU_DIALOG_STATE_UNSPECIFIED || state > TOFU_DIALOG_STATE_ACCEPTED) {
            Log.e(TAG, "Invalid TOFU dialog state received. state=" + state);
            return;
        }
        mTofuDialogState = state;
    }

    /**
     * Get the TOFU dialog state.
     * @hide
     */
    public @TofuDialogState int getTofuDialogState() {
        return mTofuDialogState;
    }

    /**
     * For devices with no TOFU support, indicate that the user approved that a
     * legacy TLS-based EAP configuration from a previous release can be used
     * without a Root CA certificate.
     *
     * @hide
     */
    public void setUserApproveNoCaCert(boolean approved) {
        mUserApproveNoCaCert = approved;
    }

    /**
     * For devices with no TOFU support, indicates if the user approved that a
     * legacy TLS-based EAP configuration from a previous release can be used
     * without a Root CA certificate.
     *
     * @return indicate whether a user approves this no CA cert config.
     * @hide
     */
    public boolean isUserApproveNoCaCert() {
        return mUserApproveNoCaCert;
    }

    /**
     * Set the minimum TLS version for TLS-based EAP methods.
     *
     * {@link WifiManager#isTlsMinimumVersionSupported()} indicates whether or not a minimum
     * TLS version can be set. If not supported, the minimum TLS version is always TLS v1.0.
     * <p>
     * {@link WifiManager#isTlsV13Supported()} indicates whether or not TLS v1.3 is supported.
     * If requested minimum is not supported, it will default to the maximum supported version.
     *
     * @param tlsVersion the TLS version
     * @throws IllegalArgumentException if the TLS version is invalid.
     */
    public void setMinimumTlsVersion(@TlsVersion int tlsVersion) throws IllegalArgumentException {
        if (tlsVersion < TLS_VERSION_MIN || tlsVersion > TLS_VERSION_MAX) {
            throw new IllegalArgumentException(
                    "Invalid TLS version: " + tlsVersion);
        }
        mMinimumTlsVersion = tlsVersion;
    }

    /**
     * Get the minimum TLS version for TLS-based EAP methods.
     *
     * @return the TLS version
     */
    public @TlsVersion int getMinimumTlsVersion() {
        return mMinimumTlsVersion;
    }
}
