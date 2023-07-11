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
    private static final String BIND_IRADIOCONFIG = "android.telephony.mockmodem.iradioconfig";
    private static final String PHONE_ID = "phone_id";

    private static final byte DEFAULT_PHONE_ID = 0x00;

    static final int RADIOCONFIG_SERVICE = RIL.MAX_SERVICE_IDX + 1;

    static final int BINDER_RETRY_MILLIS = 3 * 100;
    static final int BINDER_MAX_RETRY = 3;

    private Context mContext;
    private String mServiceName;
    private String mPackageName;

    private IBinder mModemBinder;
    private IBinder mSimBinder;
    private IBinder mMessagingBinder;
    private IBinder mDataBinder;
    private IBinder mNetworkBinder;
    private IBinder mVoiceBinder;
    private IBinder mConfigBinder;
    private ServiceConnection mModemServiceConnection;
    private ServiceConnection mSimServiceConnection;
    private ServiceConnection mMessagingServiceConnection;
    private ServiceConnection mDataServiceConnection;
    private ServiceConnection mNetworkServiceConnection;
    private ServiceConnection mVoiceServiceConnection;
    private ServiceConnection mConfigServiceConnection;

    private byte mPhoneId;

    MockModem(Context context, String serviceName) {
        this(context, serviceName, 0);
    }

    MockModem(Context context, String serviceName, int phoneId) {
        mPhoneId = (byte) phoneId;
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
            Rlog.d(TAG, "IRadio " + getModuleName(mService) + "  - onServiceConnected");

            if (mService == RIL.MODEM_SERVICE) {
                mModemBinder = binder;
            } else if (mService == RIL.SIM_SERVICE) {
                mSimBinder = binder;
            } else if (mService == RIL.MESSAGING_SERVICE) {
                mMessagingBinder = binder;
            } else if (mService == RIL.DATA_SERVICE) {
                mDataBinder = binder;
            } else if (mService == RIL.NETWORK_SERVICE) {
                mNetworkBinder = binder;
            } else if (mService == RIL.VOICE_SERVICE) {
                mVoiceBinder = binder;
            } else if (mService == RADIOCONFIG_SERVICE) {
                mConfigBinder = binder;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Rlog.d(TAG, "IRadio " + getModuleName(mService) + "  - onServiceDisconnected");

            if (mService == RIL.MODEM_SERVICE) {
                mModemBinder = null;
            } else if (mService == RIL.SIM_SERVICE) {
                mSimBinder = null;
            } else if (mService == RIL.MESSAGING_SERVICE) {
                mMessagingBinder = null;
            } else if (mService == RIL.DATA_SERVICE) {
                mDataBinder = null;
            } else if (mService == RIL.NETWORK_SERVICE) {
                mNetworkBinder = null;
            } else if (mService == RIL.VOICE_SERVICE) {
                mVoiceBinder = null;
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
        intent.setAction(actionName);
        intent.putExtra(PHONE_ID, phoneId);

        status = mContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        return status;
    }

    /** waitForBinder */
    public IBinder getServiceBinder(int service) {
        switch (service) {
            case RIL.MODEM_SERVICE:
                return mModemBinder;
            case RIL.SIM_SERVICE:
                return mSimBinder;
            case RIL.MESSAGING_SERVICE:
                return mMessagingBinder;
            case RIL.DATA_SERVICE:
                return mDataBinder;
            case RIL.NETWORK_SERVICE:
                return mNetworkBinder;
            case RIL.VOICE_SERVICE:
                return mVoiceBinder;
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
                    Rlog.d(TAG, "IRadio Config bind fail");
                    mConfigServiceConnection = null;
                }
            } else {
                Rlog.d(TAG, "IRadio Config is bound");
            }
        } else if (service == RIL.MODEM_SERVICE) {
            if (mModemBinder == null) {
                mModemServiceConnection = new MockModemConnection(RIL.MODEM_SERVICE);

                boolean status =
                        bindModuleToMockModemService(
                                mPhoneId, BIND_IRADIOMODEM, mModemServiceConnection);
                if (!status) {
                    Rlog.d(TAG, "IRadio Modem bind fail");
                    mModemServiceConnection = null;
                }
            } else {
                Rlog.d(TAG, "IRadio Modem is bound");
            }
        } else if (service == RIL.SIM_SERVICE) {
            if (mSimBinder == null) {
                mSimServiceConnection = new MockModemConnection(RIL.SIM_SERVICE);

                boolean status =
                        bindModuleToMockModemService(
                                mPhoneId, BIND_IRADIOSIM, mSimServiceConnection);
                if (!status) {
                    Rlog.d(TAG, "IRadio Sim bind fail");
                    mSimServiceConnection = null;
                }
            } else {
                Rlog.d(TAG, "IRadio Sim is bound");
            }
        } else if (service == RIL.MESSAGING_SERVICE) {
            if (mMessagingBinder == null) {
                mMessagingServiceConnection = new MockModemConnection(RIL.MESSAGING_SERVICE);

                boolean status =
                        bindModuleToMockModemService(
                                mPhoneId, BIND_IRADIOMESSAGING, mMessagingServiceConnection);
                if (!status) {
                    Rlog.d(TAG, "IRadio Messaging bind fail");
                    mMessagingServiceConnection = null;
                }
            } else {
                Rlog.d(TAG, "IRadio Messaging is bound");
            }
        } else if (service == RIL.DATA_SERVICE) {
            if (mDataBinder == null) {
                mDataServiceConnection = new MockModemConnection(RIL.DATA_SERVICE);

                boolean status =
                        bindModuleToMockModemService(
                                mPhoneId, BIND_IRADIODATA, mDataServiceConnection);
                if (!status) {
                    Rlog.d(TAG, "IRadio Data bind fail");
                    mDataServiceConnection = null;
                }
            } else {
                Rlog.d(TAG, "IRadio Data is bound");
            }
        } else if (service == RIL.NETWORK_SERVICE) {
            if (mNetworkBinder == null) {
                mNetworkServiceConnection = new MockModemConnection(RIL.NETWORK_SERVICE);

                boolean status =
                        bindModuleToMockModemService(
                                mPhoneId, BIND_IRADIONETWORK, mNetworkServiceConnection);
                if (!status) {
                    Rlog.d(TAG, "IRadio Network bind fail");
                    mNetworkServiceConnection = null;
                }
            } else {
                Rlog.d(TAG, "IRadio Network is bound");
            }
        } else if (service == RIL.VOICE_SERVICE) {
            if (mVoiceBinder == null) {
                mVoiceServiceConnection = new MockModemConnection(RIL.VOICE_SERVICE);

                boolean status =
                        bindModuleToMockModemService(
                                mPhoneId, BIND_IRADIOVOICE, mVoiceServiceConnection);
                if (!status) {
                    Rlog.d(TAG, "IRadio Voice bind fail");
                    mVoiceServiceConnection = null;
                }
            } else {
                Rlog.d(TAG, "IRadio Voice is bound");
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
                Rlog.d(TAG, "unbind IRadio Config");
            }
        } else if (service == RIL.MODEM_SERVICE) {
            if (mModemServiceConnection != null) {
                mContext.unbindService(mModemServiceConnection);
                mModemServiceConnection = null;
                mModemBinder = null;
                Rlog.d(TAG, "unbind IRadio Modem");
            }
        } else if (service == RIL.SIM_SERVICE) {
            if (mSimServiceConnection != null) {
                mContext.unbindService(mSimServiceConnection);
                mSimServiceConnection = null;
                mSimBinder = null;
                Rlog.d(TAG, "unbind IRadio Sim");
            }
        } else if (service == RIL.MESSAGING_SERVICE) {
            if (mMessagingServiceConnection != null) {
                mContext.unbindService(mMessagingServiceConnection);
                mMessagingServiceConnection = null;
                mMessagingBinder = null;
                Rlog.d(TAG, "unbind IRadio Messaging");
            }
        } else if (service == RIL.DATA_SERVICE) {
            if (mDataServiceConnection != null) {
                mContext.unbindService(mDataServiceConnection);
                mDataServiceConnection = null;
                mDataBinder = null;
                Rlog.d(TAG, "unbind IRadio Data");
            }
        } else if (service == RIL.NETWORK_SERVICE) {
            if (mNetworkServiceConnection != null) {
                mContext.unbindService(mNetworkServiceConnection);
                mNetworkServiceConnection = null;
                mNetworkBinder = null;
                Rlog.d(TAG, "unbind IRadio Network");
            }
        } else if (service == RIL.VOICE_SERVICE) {
            if (mVoiceServiceConnection != null) {
                mContext.unbindService(mVoiceServiceConnection);
                mVoiceServiceConnection = null;
                mVoiceBinder = null;
                Rlog.d(TAG, "unbind IRadio Voice");
            }
        }
    }

    public String getServiceName() {
        return mServiceName;
    }

    private String getModuleName(int service) {
        switch (service) {
            case RIL.MODEM_SERVICE:
                return "modem";
            case RIL.SIM_SERVICE:
                return "sim";
            case RIL.MESSAGING_SERVICE:
                return "messaging";
            case RIL.DATA_SERVICE:
                return "data";
            case RIL.NETWORK_SERVICE:
                return "network";
            case RIL.VOICE_SERVICE:
                return "voice";
            case RADIOCONFIG_SERVICE:
                return "config";
            default:
                return "none";
        }
    }
}
