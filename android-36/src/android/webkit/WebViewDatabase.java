/*
 * Copyright (C) 2007 The Android Open Source Project
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

package android.webkit;

import android.annotation.Nullable;
import android.content.Context;

/**
 * This class allows developers to determine whether any WebView used in the
 * application has stored any of the following types of browsing data and
 * to clear any such stored data for all WebViews in the application.
 * <ul>
 *  <li>Username/password pairs for web forms</li>
 *  <li>HTTP authentication username/password pairs</li>
 *  <li>Data entered into text fields (e.g. for autocomplete suggestions)</li>
 * </ul>
 */
public abstract class WebViewDatabase {
    /**
     * @deprecated This class should not be constructed by applications, use {@link
     * #getInstance(Context)} instead to fetch the singleton instance.
     */
    // TODO(ntfschr): mark this as @SystemApi after a year.
    @Deprecated
    public WebViewDatabase() {}

    /**
     * @hide Since API level {@link android.os.Build.VERSION_CODES#JELLY_BEAN_MR1}
     */
    protected static final String LOGTAG = "webviewdatabase";

    public static WebViewDatabase getInstance(Context context) {
        return WebViewFactory.getProvider().getWebViewDatabase(context);
    }

    /**
     * Gets whether there are any saved username/password pairs for web forms.
     * Note that these are unrelated to HTTP authentication credentials.
     *
     * @return {@code true} if there are any saved username/password pairs
     * @see WebView#savePassword
     * @see #clearUsernamePassword
     * @deprecated Saving passwords in WebView will not be supported in future versions.
     */
    @Deprecated
    public abstract boolean hasUsernamePassword();

    /**
     * Clears any saved username/password pairs for web forms.
     * Note that these are unrelated to HTTP authentication credentials.
     *
     * @see WebView#savePassword
     * @see #hasUsernamePassword
     * @deprecated Saving passwords in WebView will not be supported in future versions.
     */
    @Deprecated
    public abstract void clearUsernamePassword();

    /**
     * Gets whether there are any saved credentials for HTTP authentication.
     *
     * @return whether there are any saved credentials
     * @see #getHttpAuthUsernamePassword
     * @see #setHttpAuthUsernamePassword
     * @see #clearHttpAuthUsernamePassword
     */
    public abstract boolean hasHttpAuthUsernamePassword();

    /**
     * Clears any saved credentials for HTTP authentication. This method only clears the username
     * and password stored in WebViewDatabase instance. The username and password are not read from
     * the {@link WebViewDatabase} during {@link WebViewClient#onReceivedHttpAuthRequest}. It is up
     * to the app to do this or not.
     * <p>
     * The username and password used for http authentication might be cached in the network stack
     * itself, and are not cleared when this method is called.  WebView does not provide a special
     * mechanism to clear HTTP authentication for implementing client logout. The client logout
     * mechanism should be implemented by the Web site designer (such as server sending a HTTP 401
     * for invalidating credentials).
     *
     * @see #getHttpAuthUsernamePassword
     * @see #setHttpAuthUsernamePassword
     * @see #hasHttpAuthUsernamePassword
     */
    public abstract void clearHttpAuthUsernamePassword();

    /**
     * Stores HTTP authentication credentials for a given host and realm to the {@link WebViewDatabase}
     * instance.
     * <p>
     * To use HTTP authentication, the embedder application has to implement
     * {@link WebViewClient#onReceivedHttpAuthRequest}, and call {@link HttpAuthHandler#proceed}
     * with the correct username and password.
     * <p>
     * The embedder app can get the username and password any way it chooses, and does not have to
     * use {@link WebViewDatabase}.
     * <p>
     * Notes:
     * <li>
     * {@link WebViewDatabase} is provided only as a convenience to store and retrieve http
     * authentication credentials. WebView does not read from it during HTTP authentication.
     * </li>
     * <li>
     * WebView does not provide a special mechanism to clear HTTP authentication credentials for
     * implementing client logout. The client logout mechanism should be implemented by the Web site
     * designer (such as server sending a HTTP 401 for invalidating credentials).
     * </li>
     *
     * @param host the host to which the credentials apply
     * @param realm the realm to which the credentials apply
     * @param username the username
     * @param password the password
     * @see #getHttpAuthUsernamePassword
     * @see #hasHttpAuthUsernamePassword
     * @see #clearHttpAuthUsernamePassword
     */
    public abstract void setHttpAuthUsernamePassword(String host, String realm,
            String username, String password);

   /**
     * Retrieves HTTP authentication credentials for a given host and realm from the {@link
     * WebViewDatabase} instance.
     *
     * @param host the host to which the credentials apply
     * @param realm the realm to which the credentials apply
     * @return the credentials as a String array, if found. The first element
     *         is the username and the second element is the password. {@code null} if
     *         no credentials are found.
     * @see #setHttpAuthUsernamePassword
     * @see #hasHttpAuthUsernamePassword
     * @see #clearHttpAuthUsernamePassword
     */
    @Nullable
    public abstract String[] getHttpAuthUsernamePassword(String host, String realm);

    /**
     * Gets whether there is any saved data for web forms.
     *
     * @return whether there is any saved data for web forms
     * @see #clearFormData
     */
    @Deprecated
    public abstract boolean hasFormData();

    /**
     * Clears any saved data for web forms.
     *
     * @see #hasFormData
     */
    @Deprecated
    public abstract void clearFormData();
}
