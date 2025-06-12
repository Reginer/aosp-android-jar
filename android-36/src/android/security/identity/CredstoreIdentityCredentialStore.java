/*
 * Copyright 2019 The Android Open Source Project
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

package android.security.identity;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.os.ServiceManager;

class CredstoreIdentityCredentialStore extends IdentityCredentialStore {

    private static final String TAG = "CredstoreIdentityCredentialStore";

    private Context mContext = null;
    private ICredentialStore mStore = null;
    private int mFeatureVersion;

    static int getFeatureVersion(@NonNull Context context) {
        PackageManager pm = context.getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_IDENTITY_CREDENTIAL_HARDWARE)) {
            FeatureInfo[] infos = pm.getSystemAvailableFeatures();
            for (int n = 0; n < infos.length; n++) {
                FeatureInfo info = infos[n];
                if (info.name.equals(PackageManager.FEATURE_IDENTITY_CREDENTIAL_HARDWARE)) {
                    return info.version;
                }
            }
        }
        // Use of the system feature is not required since Android 12. So for Android 11
        // return 202009 which is the feature version shipped with Android 11.
        return 202009;
    }

    private CredstoreIdentityCredentialStore(@NonNull Context context, ICredentialStore store) {
        mContext = context;
        mStore = store;
        mFeatureVersion = getFeatureVersion(mContext);
    }

    static CredstoreIdentityCredentialStore getInstanceForType(@NonNull Context context,
            int credentialStoreType) {
        ICredentialStoreFactory storeFactory =
                ICredentialStoreFactory.Stub.asInterface(
                    ServiceManager.getService("android.security.identity"));
        if (storeFactory == null) {
            // This can happen if credstore is not running or not installed.
            return null;
        }

        ICredentialStore credStore = null;
        try {
            credStore = storeFactory.getCredentialStore(credentialStoreType);
        } catch (android.os.RemoteException e) {
            throw new RuntimeException("Unexpected RemoteException ", e);
        } catch (android.os.ServiceSpecificException e) {
            if (e.errorCode == ICredentialStore.ERROR_GENERIC) {
                return null;
            } else {
                throw new RuntimeException("Unexpected ServiceSpecificException with code "
                        + e.errorCode, e);
            }
        }
        if (credStore == null) {
            return null;
        }

        return new CredstoreIdentityCredentialStore(context, credStore);
    }

    private static CredstoreIdentityCredentialStore sInstanceDefault = null;
    private static CredstoreIdentityCredentialStore sInstanceDirectAccess = null;

    public static @Nullable IdentityCredentialStore getInstance(@NonNull Context context) {
        if (sInstanceDefault == null) {
            sInstanceDefault = getInstanceForType(context,
                    ICredentialStoreFactory.CREDENTIAL_STORE_TYPE_DEFAULT);
        }
        return sInstanceDefault;
    }

    public static @Nullable IdentityCredentialStore getDirectAccessInstance(@NonNull
            Context context) {
        if (sInstanceDirectAccess == null) {
            sInstanceDirectAccess = getInstanceForType(context,
                    ICredentialStoreFactory.CREDENTIAL_STORE_TYPE_DIRECT_ACCESS);
        }
        return sInstanceDirectAccess;
    }

    @Override
    public @NonNull String[] getSupportedDocTypes() {
        try {
            SecurityHardwareInfoParcel info;
            info = mStore.getSecurityHardwareInfo();
            return info.supportedDocTypes;
        } catch (android.os.RemoteException e) {
            throw new RuntimeException("Unexpected RemoteException ", e);
        } catch (android.os.ServiceSpecificException e) {
            throw new RuntimeException("Unexpected ServiceSpecificException with code "
                    + e.errorCode, e);
        }
    }

    @Override public @NonNull WritableIdentityCredential createCredential(
            @NonNull String credentialName,
            @NonNull String docType) throws AlreadyPersonalizedException,
            DocTypeNotSupportedException {
        try {
            IWritableCredential wc = mStore.createCredential(credentialName, docType);
            return new CredstoreWritableIdentityCredential(mContext, credentialName, docType, wc);
        } catch (android.os.RemoteException e) {
            throw new RuntimeException("Unexpected RemoteException ", e);
        } catch (android.os.ServiceSpecificException e) {
            if (e.errorCode == ICredentialStore.ERROR_ALREADY_PERSONALIZED) {
                throw new AlreadyPersonalizedException(e.getMessage(), e);
            } else if (e.errorCode == ICredentialStore.ERROR_DOCUMENT_TYPE_NOT_SUPPORTED) {
                throw new DocTypeNotSupportedException(e.getMessage(), e);
            } else {
                throw new RuntimeException("Unexpected ServiceSpecificException with code "
                        + e.errorCode, e);
            }
        }
    }

    @Override public @Nullable IdentityCredential getCredentialByName(
            @NonNull String credentialName,
            @Ciphersuite int cipherSuite) throws CipherSuiteNotSupportedException {
        try {
            ICredential credstoreCredential;
            credstoreCredential = mStore.getCredentialByName(credentialName, cipherSuite);
            return new CredstoreIdentityCredential(mContext, credentialName, cipherSuite,
                    credstoreCredential, null, mFeatureVersion);
        } catch (android.os.RemoteException e) {
            throw new RuntimeException("Unexpected RemoteException ", e);
        } catch (android.os.ServiceSpecificException e) {
            if (e.errorCode == ICredentialStore.ERROR_NO_SUCH_CREDENTIAL) {
                return null;
            } else if (e.errorCode == ICredentialStore.ERROR_CIPHER_SUITE_NOT_SUPPORTED) {
                throw new CipherSuiteNotSupportedException(e.getMessage(), e);
            } else {
                throw new RuntimeException("Unexpected ServiceSpecificException with code "
                        + e.errorCode, e);
            }
        }
    }

    @Override
    public @Nullable byte[] deleteCredentialByName(@NonNull String credentialName) {
        ICredential credstoreCredential = null;
        try {
            try {
                credstoreCredential = mStore.getCredentialByName(credentialName,
                        CIPHERSUITE_ECDHE_HKDF_ECDSA_WITH_AES_256_GCM_SHA256);
            } catch (android.os.ServiceSpecificException e) {
                if (e.errorCode == ICredentialStore.ERROR_NO_SUCH_CREDENTIAL) {
                    return null;
                }
            }
            byte[] proofOfDeletion = credstoreCredential.deleteCredential();
            return proofOfDeletion;
        } catch (android.os.RemoteException e) {
            throw new RuntimeException("Unexpected RemoteException ", e);
        } catch (android.os.ServiceSpecificException e) {
            throw new RuntimeException("Unexpected ServiceSpecificException with code "
                    + e.errorCode, e);
        }
    }

    @Override
    public @NonNull PresentationSession createPresentationSession(@Ciphersuite int cipherSuite)
            throws CipherSuiteNotSupportedException {
        try {
            ISession credstoreSession = mStore.createPresentationSession(cipherSuite);
            return new CredstorePresentationSession(mContext, cipherSuite, this, credstoreSession,
                                                    mFeatureVersion);
        } catch (android.os.RemoteException e) {
            throw new RuntimeException("Unexpected RemoteException ", e);
        } catch (android.os.ServiceSpecificException e) {
            if (e.errorCode == ICredentialStore.ERROR_CIPHER_SUITE_NOT_SUPPORTED) {
                throw new CipherSuiteNotSupportedException(e.getMessage(), e);
            } else {
                throw new RuntimeException("Unexpected ServiceSpecificException with code "
                        + e.errorCode, e);
            }
        }
    }

}
