/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.security.keystore2;

import android.annotation.NonNull;
import android.security.KeyStoreSecurityLevel;
import android.security.keymaster.KeymasterDefs;
import android.security.keystore.KeyProperties;
import android.system.keystore2.Authorization;
import android.system.keystore2.KeyDescriptor;
import android.system.keystore2.KeyMetadata;

import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;

/**
 * {@link ECPublicKey} backed by keystore.
 *
 * @hide
 */
public class AndroidKeyStoreECPublicKey extends AndroidKeyStorePublicKey implements ECPublicKey {

    private final ECParameterSpec mParams;
    private final ECPoint mW;

    public AndroidKeyStoreECPublicKey(@NonNull KeyDescriptor descriptor,
            @NonNull KeyMetadata metadata,
            @NonNull byte[] x509EncodedForm,
            @NonNull KeyStoreSecurityLevel securityLevel,
            @NonNull ECParameterSpec params, @NonNull ECPoint w) {
        super(descriptor, metadata, x509EncodedForm, KeyProperties.KEY_ALGORITHM_EC, securityLevel);
        mParams = params;
        mW = w;
    }

    public AndroidKeyStoreECPublicKey(@NonNull KeyDescriptor descriptor,
            @NonNull KeyMetadata metadata,
            @NonNull KeyStoreSecurityLevel securityLevel, @NonNull ECPublicKey info) {
        this(descriptor, metadata, info.getEncoded(), securityLevel, info.getParams(), info.getW());
        if (!"X.509".equalsIgnoreCase(info.getFormat())) {
            throw new IllegalArgumentException(
                    "Unsupported key export format: " + info.getFormat());
        }
    }

    @Override
    public AndroidKeyStorePrivateKey getPrivateKey() {
        ECParameterSpec params = mParams;
        for (Authorization a : getAuthorizations()) {
            try {
                if (a.keyParameter.tag == KeymasterDefs.KM_TAG_EC_CURVE) {
                    params = KeymasterUtils.getCurveSpec(KeymasterUtils.getEcCurveFromKeymaster(
                            a.keyParameter.value.getEcCurve()));
                    break;
                }
            } catch (Exception e) {
                throw new RuntimeException("Unable to parse EC curve "
                        + a.keyParameter.value.getEcCurve());
            }
        }
        return new AndroidKeyStoreECPrivateKey(
                getUserKeyDescriptor(), getKeyIdDescriptor().nspace, getAuthorizations(),
                getSecurityLevel(), params);
    }

    @Override
    public ECParameterSpec getParams() {
        return mParams;
    }

    @Override
    public ECPoint getW() {
        return mW;
    }
}
