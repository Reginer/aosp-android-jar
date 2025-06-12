/**
 * Copyright (c) 2015, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.security;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.content.Context;
import android.content.pm.PackageManager;
import android.security.net.config.ApplicationConfig;
import android.security.net.config.ManifestConfigSource;

/**
 * Network security policy.
 *
 * <p>Network stacks/components should honor this policy to make it possible to centrally control
 * the relevant aspects of network security behavior.
 */
public class NetworkSecurityPolicy {

    private static final NetworkSecurityPolicy INSTANCE = new NetworkSecurityPolicy();

    private NetworkSecurityPolicy() {}

    /**
     * Gets the policy for this process.
     *
     * <p>It's fine to cache this reference. Any changes to the policy will be immediately visible
     * through the reference.
     */
    public static NetworkSecurityPolicy getInstance() {
        return INSTANCE;
    }

    /**
     * Returns whether cleartext network traffic (e.g. HTTP, FTP, WebSockets, XMPP, IMAP, SMTP --
     * without TLS or STARTTLS) is permitted for all network communication from this process.
     *
     * <p>When cleartext network traffic is not permitted, the platform's components (e.g. HTTP and
     * FTP stacks, {@link android.app.DownloadManager}, {@link android.media.MediaPlayer}) will
     * refuse this process's requests to use cleartext traffic. Third-party libraries are strongly
     * encouraged to honor this setting as well.
     *
     * <p>This flag is honored on a best effort basis because it's impossible to prevent all
     * cleartext traffic from Android applications given the level of access provided to them. For
     * example, there's no expectation that the {@link java.net.Socket} API will honor this flag
     * because it cannot determine whether its traffic is in cleartext. However, most network
     * traffic from applications is handled by higher-level network stacks/components which can
     * honor this aspect of the policy.
     *
     * <p>NOTE: {@link android.webkit.WebView} honors this flag for applications targeting API level
     * 26 and up.
     */
    public boolean isCleartextTrafficPermitted() {
        return libcore.net.NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted();
    }

    /**
     * Returns whether cleartext network traffic (e.g. HTTP, FTP, XMPP, IMAP, SMTP -- without
     * TLS or STARTTLS) is permitted for communicating with {@code hostname} for this process.
     *
     * @see #isCleartextTrafficPermitted()
     */
    public boolean isCleartextTrafficPermitted(String hostname) {
        return libcore.net.NetworkSecurityPolicy.getInstance()
                .isCleartextTrafficPermitted(hostname);
    }

    /**
     * Sets whether cleartext network traffic is permitted for this process.
     *
     * <p>This method is used by the platform early on in the application's initialization to set
     * the policy.
     *
     * @hide
     */
    public void setCleartextTrafficPermitted(boolean permitted) {
        libcore.net.NetworkSecurityPolicy currentPolicy =
                libcore.net.NetworkSecurityPolicy.getInstance();
        OverlayNetworkSecurityPolicy policy =
                new OverlayNetworkSecurityPolicy(currentPolicy, permitted);
        libcore.net.NetworkSecurityPolicy.setInstance(policy);
    }

    /**
     * Returns {@code true} if Certificate Transparency information is required to be verified by
     * the client in TLS connections to {@code hostname}.
     *
     * <p>See RFC6962 section 3.3 for more details.
     *
     * @param hostname hostname to check whether certificate transparency verification is required
     * @return {@code true} if certificate transparency verification is required and {@code false}
     *     otherwise
     */
    @FlaggedApi(Flags.FLAG_CERTIFICATE_TRANSPARENCY_CONFIGURATION)
    public boolean isCertificateTransparencyVerificationRequired(@NonNull String hostname) {
        return libcore.net.NetworkSecurityPolicy.getInstance()
                .isCertificateTransparencyVerificationRequired(hostname);
    }

    /**
     * Handle an update to the system or user certificate stores.
     * @hide
     */
    public void handleTrustStorageUpdate() {
        ApplicationConfig config = ApplicationConfig.getDefaultInstance();
        if (config != null) {
            config.handleTrustStorageUpdate();
        }
    }

    /**
     * Returns an {@link ApplicationConfig} based on the configuration for {@code packageName}.
     *
     * @hide
     */
    public static ApplicationConfig getApplicationConfigForPackage(Context context,
            String packageName) throws PackageManager.NameNotFoundException {
        Context appContext = context.createPackageContext(packageName, 0);
        ManifestConfigSource source = new ManifestConfigSource(appContext);
        return new ApplicationConfig(source);
    }
}
