/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static android.telephony.TelephonyManager.HAL_SERVICE_DATA;
import static android.telephony.TelephonyManager.HAL_SERVICE_IMS;
import static android.telephony.TelephonyManager.HAL_SERVICE_MESSAGING;
import static android.telephony.TelephonyManager.HAL_SERVICE_MODEM;
import static android.telephony.TelephonyManager.HAL_SERVICE_NETWORK;
import static android.telephony.TelephonyManager.HAL_SERVICE_SIM;
import static android.telephony.TelephonyManager.HAL_SERVICE_VOICE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.android.telephony.Rlog;

/** This class provides wrapper APIs for binding interfaces to mock service. */
public class MockModem {
    private static final String TAG = "MockModem";
    private static final String BIND_IRADIOMODEM = "android.telephony.mockmodem.iradiomodem";
    private static final String BIND_IRADIOSIM = "android.telephony.mockmodem.iradiosim";
    private static final String BIND_IRADIOMESSAGING =
            "android.telephony.mockmodem.iradiomessaging";
    private static final String BIND_IRADIODATA = "android.telephony.mockmodem.iradiodata";
    private static final String BIND_IRADIONETWORK = "android.telephony.mockmodem.iradionetwork";
    private static final String BIND_IRADIOVOICE = "android.telephony.mockmodem.iradiovoice";
    private static final String BIND_IRADIOIMS = "android.telephony.mockmodem.iradioims";
    private static final String BIND_IRADIOCONFIG = "android.telephony.mockmodem.iradioconfig";
    private static final String PHONE_ID = "phone_id";

    private static final byte DEFAULT_PHONE_ID = 0x00;

    static final int RADIOCONFIG_SERVICE = RIL.MAX_SERVICE_IDX + 1;

    static final int BINDER_RETRY_MILLIS = 3 * 100;
    static final int BINDER_MAX_RETRY = 10;

    private Context mContext;
    private String mServiceName;
    private String mPackageName;

    private IBinder mModemBinder;
    private IBinder mSimBinder;
    private IBinder mMessagingBinder;
    private IBinder mDataBinder;
    private IBinder mNetworkBinder;
    private IBinder mVoiceBinder;
    private IBinder mImsBinder;
    private IBinder mConfigBinder;
    private ServiceConnection mModemServiceConnection;
    private ServiceConnection mSimServiceConnection;
    private ServiceConnection mMessagingServiceConnection;
    private ServiceConnection mDataServiceConnection;
    private ServiceConnection mNetworkServiceConnection;
    private ServiceConnection mVoiceServiceConnection;
    private ServiceConnection mImsServiceConnection;
    private ServiceConnection mConfigServiceConnection;

    private byte mPhoneId;
    private String mTag;

    MockModem(Context context, String serviceName) {
        this(context, serviceName, 0);
    }

    MockModem(Context context, String serviceName, int phoneId) {
        mPhoneId = (byte) phoneId;
        mTag = TAG + "-" + mPhoneId;
        mContext = context;
        String[] componentInfo = serviceName.split("/", 2);
        mPackageName = componentInfo[0];
        mServiceName = componentInfo[1];
    }

    private class MockModemConnection implements ServiceConnection {
        private int mService;

        MockModemConnection(int module) {
            mService = module;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Rlog.d(mTag, "IRadio " + getModuleName(mService) + "  - onServiceConnected");

            if (mService == HAL_SERVICE_MODEM) {
                mModemBinder = binder;
            } else if (mService == HAL_SERVICE_SIM) {
                mSimBinder = binder;
            } else if (mService == HAL_SERVICE_MESSAGING) {
                mMessagingBinder = binder;
            } else if (mService == HAL_SERVICE_DATA) {
                mDataBinder = binder;
            } else if (mService == HAL_SERVICE_NETWORK) {
                mNetworkBinder = binder;
            } else if (mService == HAL_SERVICE_VOICE) {
                mVoiceBinder = binder;
            } else if (mService == HAL_SERVICE_IMS) {
                mImsBinder = binder;
            } else if (mService == RADIOCONFIG_SERVICE) {
                mConfigBinder = binder;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Rlog.d(mTag, "IRadio " + getModuleName(mService) + "  - onServiceDisconnected");

            if (mService == HAL_SERVICE_MODEM) {
                mModemBinder = null;
            } else if (mService == HAL_SERVICE_SIM) {
                mSimBinder = null;
            } else if (mService == HAL_SERVICE_MESSAGING) {
                mMessagingBinder = null;
            } else if (mService == HAL_SERVICE_DATA) {
                mDataBinder = null;
            } else if (mService == HAL_SERVICE_NETWORK) {
                mNetworkBinder = null;
            } else if (mService == HAL_SERVICE_VOICE) {
                mVoiceBinder = null;
            } else if (mService == HAL_SERVICE_IMS) {
                mImsBinder = null;
            } else if (mService == RADIOCONFIG_SERVICE) {
                mConfigBinder = null;
            }
        }
    }

    private boolean bindModuleToMockModemService(
            String actionName, ServiceConnection serviceConnection) {
        return bindModuleToMockModemService(DEFAULT_PHONE_ID, actionName, serviceConnection);
    }

    private boolean bindModuleToMockModemService(
            byte phoneId, String actionName, ServiceConnection serviceConnection) {
        boolean status = false;

        Intent intent = new Intent();
        intent.setComponent(new ComponentName(mPackageName, mServiceName));
        intent.setAction(actionName + phoneId);
        intent.putExtra(PHONE_ID, phoneId);

        status = mContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        return status;
    }

    /** waitForBinder */
    public IBinder getServiceBinder(int service) {
        switch (service) {
            case HAL_SERVICE_MODEM:
                return mModemBinder;
            case HAL_SERVICE_SIM:
                return mSimBinder;
            case HAL_SERVICE_MESSAGING:
                return mMessagingBinder;
            case HAL_SERVICE_DATA:
                return mDataBinder;
            case HAL_SERVICE_NETWORK:
                return mNetworkBinder;
            case HAL_SERVICE_VOICE:
                return mVoiceBinder;
            case HAL_SERVICE_IMS:
                return mImsBinder;
            case RADIOCONFIG_SERVICE:
                return mConfigBinder;
            default:
                return null;
        }
    }

    /** Binding interfaces with mock modem service */
    public void bindAllMockModemService() {
        for (int service = RIL.MIN_SERVICE_IDX; service <= RIL.MAX_SERVICE_IDX; service++) {
            bindToMockModemService(service);
        }
    }

    /** bindToMockModemService */
    public void bindToMockModemService(int service) {
        if (service == RADIOCONFIG_SERVICE) {
            if (mConfigBinder == null) {
                mConfigServiceConnection = new MockModemConnection(RADIOCONFIG_SERVICE);

                boolean status =
                        bindModuleToMockModemService(BIND_IRADIOCONFIG, mConfigServiceConnection);
                if (!status) {
                    Rlog.d(mTag, "IRadio Config bind fail");
                    mConfigServiceConnection = null;
                }
            } else {
                Rlog.d(mTag, "IRadio Config is bound");
            }
        } else if (service == HAL_SERVICE_MODEM) {
            if (mModemBinder == null) {
                mModemServiceConnection = new MockModemConnection(HAL_SERVICE_MODEM);

                boolean status =
                        bindModuleToMockModemService(
                                mPhoneId, BIND_IRADIOMODEM, mModemServiceConnection);
                if (!status) {
                    Rlog.d(mTag, "IRadio Modem bind fail");
                    mModemServiceConnection = null;
                }
            } else {
                Rlog.d(mTag, "IRadio Modem is bound");
            }
        } else if (service == HAL_SERVICE_SIM) {
            if (mSimBinder == null) {
                mSimServiceConnection = new MockModemConnection(HAL_SERVICE_SIM);

                boolean status =
                        bindModuleToMockModemService(
                                mPhoneId, BIND_IRADIOSIM, mSimServiceConnection);
                if (!status) {
                    Rlog.d(mTag, "IRadio Sim bind fail");
                    mSimServiceConnection = null;
                }
            } else {
                Rlog.d(mTag, "IRadio Sim is bound");
            }
        } else if (service == HAL_SERVICE_MESSAGING) {
            if (mMessagingBinder == null) {
                mMessagingServiceConnection = new MockModemConnection(HAL_SERVICE_MESSAGING);

                boolean status =
                        bindModuleToMockModemService(
                                mPhoneId, BIND_IRADIOMESSAGING, mMessagingServiceConnection);
                if (!status) {
                    Rlog.d(mTag, "IRadio Messaging bind fail");
                    mMessagingServiceConnection = null;
                }
            } else {
                Rlog.d(mTag, "IRadio Messaging is bound");
            }
        } else if (service == HAL_SERVICE_DATA) {
            if (mDataBinder == null) {
                mDataServiceConnection = new MockModemConnection(HAL_SERVICE_DATA);

                boolean status =
                        bindModuleToMockModemService(
                                mPhoneId, BIND_IRADIODATA, mDataServiceConnection);
                if (!status) {
                    Rlog.d(mTag, "IRadio Data bind fail");
                    mDataServiceConnection = null;
                }
            } else {
                Rlog.d(mTag, "IRadio Data is bound");
            }
        } else if (service == HAL_SERVICE_NETWORK) {
            if (mNetworkBinder == null) {
                mNetworkServiceConnection = new MockModemConnection(HAL_SERVICE_NETWORK);

                boolean status =
                        bindModuleToMockModemService(
                                mPhoneId, BIND_IRADIONETWORK, mNetworkServiceConnection);
                if (!status) {
                    Rlog.d(mTag, "IRadio Network bind fail");
                    mNetworkServiceConnection = null;
                }
            } else {
                Rlog.d(mTag, "IRadio Network is bound");
            }
        } else if (service == HAL_SERVICE_VOICE) {
            if (mVoiceBinder == null) {
                mVoiceServiceConnection = new MockModemConnection(HAL_SERVICE_VOICE);

                boolean status =
                        bindModuleToMockModemService(
                                mPhoneId, BIND_IRADIOVOICE, mVoiceServiceConnection);
                if (!status) {
                    Rlog.d(mTag, "IRadio Voice bind fail");
                    mVoiceServiceConnection = null;
                }
            } else {
                Rlog.d(mTag, "IRadio Voice is bound");
            }
        } else if (service == HAL_SERVICE_IMS) {
            if (mImsBinder == null) {
                mImsServiceConnection = new MockModemConnection(HAL_SERVICE_IMS);

                boolean status =
                        bindModuleToMockModemService(
                                mPhoneId, BIND_IRADIOIMS, mImsServiceConnection);
                if (!status) {
                    Rlog.d(TAG, "IRadio Ims bind fail");
                    mImsServiceConnection = null;
                }
            } else {
                Rlog.d(TAG, "IRadio Ims is bound");
            }
        }
    }

    /** unbindMockModemService */
    public void unbindMockModemService(int service) {

        if (service == RADIOCONFIG_SERVICE) {
            if (mConfigServiceConnection != null) {
                mContext.unbindService(mConfigServiceConnection);
                mConfigServiceConnection = null;
                mConfigBinder = null;
                Rlog.d(mTag, "unbind IRadio Config");
            }
        } else if (service == HAL_SERVICE_MODEM) {
            if (mModemServiceConnection != null) {
                mContext.unbindService(mModemServiceConnection);
                mModemServiceConnection = null;
                mModemBinder = null;
                Rlog.d(mTag, "unbind IRadio Modem");
            }
        } else if (service == HAL_SERVICE_SIM) {
            if (mSimServiceConnection != null) {
                mContext.unbindService(mSimServiceConnection);
                mSimServiceConnection = null;
                mSimBinder = null;
                Rlog.d(mTag, "unbind IRadio Sim");
            }
        } else if (service == HAL_SERVICE_MESSAGING) {
            if (mMessagingServiceConnection != null) {
                mContext.unbindService(mMessagingServiceConnection);
                mMessagingServiceConnection = null;
                mMessagingBinder = null;
                Rlog.d(mTag, "unbind IRadio Messaging");
            }
        } else if (service == HAL_SERVICE_DATA) {
            if (mDataServiceConnection != null) {
                mContext.unbindService(mDataServiceConnection);
                mDataServiceConnection = null;
                mDataBinder = null;
                Rlog.d(mTag, "unbind IRadio Data");
            }
        } else if (service == HAL_SERVICE_NETWORK) {
            if (mNetworkServiceConnection != null) {
                mContext.unbindService(mNetworkServiceConnection);
                mNetworkServiceConnection = null;
                mNetworkBinder = null;
                Rlog.d(mTag, "unbind IRadio Network");
            }
        } else if (service == HAL_SERVICE_VOICE) {
            if (mVoiceServiceConnection != null) {
                mContext.unbindService(mVoiceServiceConnection);
                mVoiceServiceConnection = null;
                mVoiceBinder = null;
                Rlog.d(mTag, "unbind IRadio Voice");
            }
        } else if (service == HAL_SERVICE_IMS) {
            if (mImsServiceConnection != null) {
                mContext.unbindService(mImsServiceConnection);
                mImsServiceConnection = null;
                mImsBinder = null;
                Rlog.d(TAG, "unbind IRadio Ims");
            }
        }
    }

    public String getServiceName() {
        return mServiceName;
    }

    private String getModuleName(int service) {
        switch (service) {
            case HAL_SERVICE_MODEM:
                return "modem";
            case HAL_SERVICE_SIM:
                return "sim";
            case HAL_SERVICE_MESSAGING:
                return "messaging";
            case HAL_SERVICE_DATA:
                return "data";
            case HAL_SERVICE_NETWORK:
                return "network";
            case HAL_SERVICE_VOICE:
                return "voice";
            case HAL_SERVICE_IMS:
                return "ims";
            case RADIOCONFIG_SERVICE:
                return "config";
            default:
                return "none";
        }
    }
}
