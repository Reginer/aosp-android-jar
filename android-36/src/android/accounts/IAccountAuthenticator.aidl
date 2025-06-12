/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.accounts;

import android.accounts.IAccountAuthenticatorResponse;
import android.accounts.Account;
import android.os.Bundle;

/**
 * Service that allows the interaction with an authentication server.
 * @hide
 */
oneway interface IAccountAuthenticator {
    /**
     * prompts the user for account information and adds the result to the IAccountManager
     */
    @EnforcePermission("ACCOUNT_MANAGER")
    @UnsupportedAppUsage
    void addAccount(in IAccountAuthenticatorResponse response, String accountType,
        String authTokenType, in String[] requiredFeatures, in Bundle options);

    /**
     * prompts the user for the credentials of the account
     */
    @EnforcePermission("ACCOUNT_MANAGER")
    @UnsupportedAppUsage
    void confirmCredentials(in IAccountAuthenticatorResponse response, in Account account,
        in Bundle options);

    /**
     * gets the password by either prompting the user or querying the IAccountManager
     */
    @EnforcePermission("ACCOUNT_MANAGER")
    @UnsupportedAppUsage
    void getAuthToken(in IAccountAuthenticatorResponse response, in Account account,
        String authTokenType, in Bundle options);

    /**
     * Gets the user-visible label of the given authtoken type.
     */
    @EnforcePermission("ACCOUNT_MANAGER")
    @UnsupportedAppUsage
    void getAuthTokenLabel(in IAccountAuthenticatorResponse response, String authTokenType);

    /**
     * prompts the user for a new password and writes it to the IAccountManager
     */
    @EnforcePermission("ACCOUNT_MANAGER")
    @UnsupportedAppUsage
    void updateCredentials(in IAccountAuthenticatorResponse response, in Account account,
        String authTokenType, in Bundle options);

    /**
     * launches an activity that lets the user edit and set the properties for an authenticator
     */
    @EnforcePermission("ACCOUNT_MANAGER")
    @UnsupportedAppUsage
    void editProperties(in IAccountAuthenticatorResponse response, String accountType);

    /**
     * returns a Bundle where the boolean value BOOLEAN_RESULT_KEY is set if the account has the
     * specified features
     */
    @EnforcePermission("ACCOUNT_MANAGER")
    @UnsupportedAppUsage
    void hasFeatures(in IAccountAuthenticatorResponse response, in Account account, 
        in String[] features);

    /**
     * Gets whether or not the account is allowed to be removed.
     */
    @EnforcePermission("ACCOUNT_MANAGER")
    @UnsupportedAppUsage
    void getAccountRemovalAllowed(in IAccountAuthenticatorResponse response, in Account account);

    /**
     * Returns a Bundle containing the required credentials to copy the account across users.
     */
    @EnforcePermission("ACCOUNT_MANAGER")
    void getAccountCredentialsForCloning(in IAccountAuthenticatorResponse response,
            in Account account);

    /**
     * Uses the Bundle containing credentials from another instance of the authenticator to create
     * a copy of the account on this user.
     */
    @EnforcePermission("ACCOUNT_MANAGER")
    void addAccountFromCredentials(in IAccountAuthenticatorResponse response, in Account account,
            in Bundle accountCredentials);

    /**
     * Starts the add account session by prompting the user for account information
     * and return a Bundle containing data to finish the session later.
     */
    @EnforcePermission("ACCOUNT_MANAGER")
    void startAddAccountSession(in IAccountAuthenticatorResponse response, String accountType,
        String authTokenType, in String[] requiredFeatures, in Bundle options);

    /**
     * Prompts the user for a new password but does not write it to the IAccountManager.
     */
    @EnforcePermission("ACCOUNT_MANAGER")
    void startUpdateCredentialsSession(in IAccountAuthenticatorResponse response, in Account account,
        String authTokenType, in Bundle options);

    /**
     * Finishes the session started by startAddAccountSession(...) or
     * startUpdateCredentialsSession(...) by adding account to or updating local credentials
     * in the IAccountManager.
     */
    @EnforcePermission("ACCOUNT_MANAGER")
    void finishSession(in IAccountAuthenticatorResponse response, String accountType,
        in Bundle sessionBundle);

    /**
     * Checks if the credentials of the provided account should be updated.
     */
    @EnforcePermission("ACCOUNT_MANAGER")
    void isCredentialsUpdateSuggested(in IAccountAuthenticatorResponse response, in Account account,
        String statusToken);
}
