/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.internal.telephony;

import android.Manifest;
import android.app.AppOpsManager;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.service.carrier.CarrierMessagingService;

import com.android.internal.annotations.VisibleForTesting;
import com.android.telephony.Rlog;

/**
 * Permissions checks for SMS functionality
 */
public class SmsPermissions {
    static final String LOG_TAG = "SmsPermissions";

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private final Phone mPhone;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private final Context mContext;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private final AppOpsManager mAppOps;

    public SmsPermissions(Phone phone, Context context, AppOpsManager appOps) {
        mPhone = phone;
        mContext = context;
        mAppOps = appOps;
    }

    /**
     * Check that the caller can send text messages.
     *
     * For persisted messages, the caller just needs the SEND_SMS permission. For unpersisted
     * messages, the caller must either be the IMS app or a carrier-privileged app, or they must
     * have both the MODIFY_PHONE_STATE and SEND_SMS permissions.
     *
     * @throws SecurityException if the caller is missing all necessary permission declaration or
     *                           has had a necessary runtime permission revoked.
     * @return true unless the caller has all necessary permissions but has a revoked AppOps bit.
     */
    public boolean checkCallingCanSendText(
            boolean persistMessageForNonDefaultSmsApp, String callingPackage,
            String callingAttributionTag, String message) {
        // TODO(b/75978989): Should we allow IMS/carrier apps for persisted messages as well?
        if (!persistMessageForNonDefaultSmsApp) {
            try {
                enforceCallerIsImsAppOrCarrierApp(message);
                // No need to also check SEND_SMS.
                return true;
            } catch (SecurityException e) {
                mContext.enforceCallingPermission(
                        android.Manifest.permission.MODIFY_PHONE_STATE, message);
            }
        }
        return checkCallingCanSendSms(callingPackage, callingAttributionTag, message);
    }

    /**
     * Enforces that the caller is one of the following apps:
     * <ul>
     *     <li> IMS App determined by telephony to implement RCS features
     *     <li> Carrier App
     * </ul>
     */
    public void enforceCallerIsImsAppOrCarrierApp(String message) {
        String imsRcsPackage = CarrierSmsUtils.getImsRcsPackageForIntent(mContext,
                mPhone, new Intent(CarrierMessagingService.SERVICE_INTERFACE));
        if (imsRcsPackage != null && packageNameMatchesCallingUid(imsRcsPackage)) {
            return;
        }
        TelephonyPermissions.enforceCallingOrSelfCarrierPrivilege(
                mContext, mPhone.getSubId(), message);
    }

    /**
     * Check that the caller has SEND_SMS permissions. Can only be called during an IPC.
     *
     * @throws SecurityException if the caller is missing the permission declaration or has had the
     *                           permission revoked at runtime.
     * @return whether the caller has the OP_SEND_SMS AppOps bit.
     */
    public boolean checkCallingCanSendSms(String callingPackage, String callingAttributionTag,
            String message) {
        mContext.enforceCallingPermission(Manifest.permission.SEND_SMS, message);
        return mAppOps.noteOp(AppOpsManager.OPSTR_SEND_SMS, Binder.getCallingUid(), callingPackage,
                callingAttributionTag, null) == AppOpsManager.MODE_ALLOWED;
    }

    /**
     * Check that the caller (or self, if this is not an IPC) has SEND_SMS permissions.
     *
     * @throws SecurityException if the caller is missing the permission declaration or has had the
     *                           permission revoked at runtime.
     * @return whether the caller has the OP_SEND_SMS AppOps bit.
     */
    public boolean checkCallingOrSelfCanSendSms(String callingPackage, String callingAttributionTag,
            String message) {
        mContext.enforceCallingOrSelfPermission(Manifest.permission.SEND_SMS, message);
        return mAppOps.noteOp(AppOpsManager.OPSTR_SEND_SMS, Binder.getCallingUid(), callingPackage,
                callingAttributionTag, null)
                == AppOpsManager.MODE_ALLOWED;
    }

    /**
     * Check that the caller (or self, if this is not an IPC) can get SMSC address from (U)SIM.
     *
     * The default SMS application can get SMSC address, otherwise the caller must have
     * {@link android.Manifest.permission#READ_PRIVILEGED_PHONE_STATE} or carrier privileges.
     *
     * @return true if the caller is default SMS app or has the required permission and privileges.
     *              Otherwise, false;
     */
    public boolean checkCallingOrSelfCanGetSmscAddress(String callingPackage, String message) {
        // Allow it to the default SMS app always.
        if (!isCallerDefaultSmsPackage(callingPackage)) {
            TelephonyPermissions
                        .enforceCallingOrSelfReadPrivilegedPhoneStatePermissionOrCarrierPrivilege(
                                mContext, mPhone.getSubId(), message);
        }
        return true;
    }

    /**
     * Check that the caller (or self, if this is not an IPC) can set SMSC address on (U)SIM.
     *
     * The default SMS application can set SMSC address, otherwise the caller must have
     * {@link android.Manifest.permission#MODIFY_PHONE_STATE} or carrier privileges.
     *
     * @return true if the caller is default SMS app or has the required permission and privileges.
     *              Otherwise, false.
     */
    public boolean checkCallingOrSelfCanSetSmscAddress(String callingPackage, String message) {
        // Allow it to the default SMS app always.
        if (!isCallerDefaultSmsPackage(callingPackage)) {
            // Allow it with MODIFY_PHONE_STATE or Carrier Privileges
            TelephonyPermissions.enforceCallingOrSelfModifyPermissionOrCarrierPrivilege(
                    mContext, mPhone.getSubId(), message);
        }
        return true;
    }

    /** Check if a package is default SMS app. */
    @VisibleForTesting
    public boolean isCallerDefaultSmsPackage(String packageName) {
        if (packageNameMatchesCallingUid(packageName)) {
            return SmsApplication.isDefaultSmsApplication(mContext, packageName);
        }
        return false;
    }

    /**
     * Check if the passed in packageName belongs to the calling uid.
     * @param packageName name of the package to check
     * @return true if package belongs to calling uid, false otherwise
     */
    @VisibleForTesting
    public boolean packageNameMatchesCallingUid(String packageName) {
        try {
            ((AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE))
                    .checkPackage(Binder.getCallingUid(), packageName);
            // If checkPackage doesn't throw an exception then we are the given package
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected void log(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    protected void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }

    protected void loge(String msg, Throwable e) {
        Rlog.e(LOG_TAG, msg, e);
    }
}
