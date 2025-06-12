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
package android.accounts;


import static android.app.admin.DevicePolicyResources.Strings.Core.CANT_ADD_ACCOUNT_MESSAGE;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.android.internal.R;

/**
 * @hide
 * Just shows an error message about the account restrictions for the limited user.
 */
public class CantAddAccountActivity extends Activity {
    public static final String EXTRA_ERROR_CODE = "android.accounts.extra.ERROR_CODE";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_not_authorized);

        TextView view = findViewById(R.id.description);
        String text = getSystemService(DevicePolicyManager.class).getResources().getString(
                CANT_ADD_ACCOUNT_MESSAGE,
                () -> getString(R.string.error_message_change_not_allowed));
        view.setText(text);
    }

    public void onCancelButtonClicked(View view) {
        onBackPressed();
    }
}
