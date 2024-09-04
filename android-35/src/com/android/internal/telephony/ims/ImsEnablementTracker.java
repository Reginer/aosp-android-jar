/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.internal.telephony.ims;

import android.content.ComponentName;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.SubscriptionManager;
import android.telephony.ims.aidl.IImsServiceController;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import java.util.HashMap;
import java.util.Map;

/**
 * This class will abstract away all the new enablement logic and take the reset/enable/disable
 * IMS commands as inputs.
 * The IMS commands will call enableIms, disableIms or resetIms to match the enablement state only
 * when it changes.
 */
public class ImsEnablementTracker {
    private static final String LOG_TAG = "ImsEnablementTracker";
    private static final long REQUEST_THROTTLE_TIME_MS = 3 * 1000L; // 3 seconds

    private static final int COMMAND_NONE_MSG = 0;
    // Indicate that the enableIms command has been received.
    @VisibleForTesting
    public static final int COMMAND_ENABLE_MSG = 1;
    // Indicate that the disableIms command has been received.
    @VisibleForTesting
    public static final int COMMAND_DISABLE_MSG = 2;
    // Indicate that the resetIms command has been received.
    private static final int COMMAND_RESET_MSG = 3;
    // Indicate that the internal enable message with delay has been received.
    private static final int COMMAND_ENABLING_DONE = 4;
    // Indicate that the internal disable message with delay has been received.
    private static final int COMMAND_DISABLING_DONE = 5;
    // Indicate that the internal reset message with delay has been received.
    @VisibleForTesting
    public static final int COMMAND_RESETTING_DONE = 6;
    // The ImsServiceController binder is connected.
    private static final int COMMAND_CONNECTED_MSG = 7;
    // The ImsServiceController binder is disconnected.
    private static final int COMMAND_DISCONNECTED_MSG = 8;
    // The subId is changed to INVALID_SUBSCRIPTION_ID.
    private static final int COMMAND_INVALID_SUBID_MSG = 9;
    // Indicate that the internal post reset message with delay has been received.
    @VisibleForTesting
    public static final int COMMAND_POST_RESETTING_DONE = 10;

    private static final Map<Integer, String> EVENT_DESCRIPTION = new HashMap<>();
    static {
        EVENT_DESCRIPTION.put(COMMAND_NONE_MSG, "COMMAND_NONE_MSG");
        EVENT_DESCRIPTION.put(COMMAND_ENABLE_MSG, "COMMAND_ENABLE_MSG");
        EVENT_DESCRIPTION.put(COMMAND_DISABLE_MSG, "COMMAND_DISABLE_MSG");
        EVENT_DESCRIPTION.put(COMMAND_RESET_MSG, "COMMAND_RESET_MSG");
        EVENT_DESCRIPTION.put(COMMAND_ENABLING_DONE, "COMMAND_ENABLING_DONE");
        EVENT_DESCRIPTION.put(COMMAND_DISABLING_DONE, "COMMAND_DISABLING_DONE");
        EVENT_DESCRIPTION.put(COMMAND_RESETTING_DONE, "COMMAND_RESETTING_DONE");
        EVENT_DESCRIPTION.put(COMMAND_CONNECTED_MSG, "COMMAND_CONNECTED_MSG");
        EVENT_DESCRIPTION.put(COMMAND_DISCONNECTED_MSG, "COMMAND_DISCONNECTED_MSG");
        EVENT_DESCRIPTION.put(COMMAND_INVALID_SUBID_MSG, "COMMAND_INVALID_SUBID_MSG");
    }

    @VisibleForTesting
    protected static final int STATE_IMS_DISCONNECTED = 0;
    @VisibleForTesting
    protected static final int STATE_IMS_DEFAULT = 1;
    @VisibleForTesting
    protected static final int STATE_IMS_ENABLED = 2;
    @VisibleForTesting
    protected static final int STATE_IMS_DISABLING = 3;
    @VisibleForTesting
    protected static final int STATE_IMS_DISABLED = 4;
    @VisibleForTesting
    protected static final int STATE_IMS_ENABLING = 5;
    @VisibleForTesting
    protected static final int STATE_IMS_RESETTING = 6;

    @VisibleForTesting
    protected static final int STATE_IMS_POSTRESETTING = 7;

    protected final Object mLock = new Object();
    private IImsServiceController mIImsServiceController;
    private long mLastImsOperationTimeMs = 0L;
    private final ComponentName mComponentName;
    private final SparseArray<ImsEnablementTrackerStateMachine> mStateMachines;

    private final Looper mLooper;
    private final int mState;

    /**
     * Provides Ims Enablement Tracker State Machine responsible for ims enable/disable/reset
     * command interactions with Ims service controller binder.
     * The enable/disable/reset ims commands have a time interval of at least
     * {@link ImsEnablementTracker#REQUEST_THROTTLE_TIME_MS} second between
     * processing each command.
     * For example, the enableIms command is received and the binder's enableIms is called.
     * After that, if the disableIms command is received, the binder's disableIms will be
     * called after {@link ImsEnablementTracker#REQUEST_THROTTLE_TIME_MS} second.
     * A time of {@link ImsEnablementTracker#REQUEST_THROTTLE_TIME_MS} will be used
     * {@link Handler#sendMessageDelayed(Message, long)},
     * and the enabled, disabled and reset states are responsible for waiting for
     * that delay message.
     */
    class ImsEnablementTrackerStateMachine extends StateMachine {
        /**
         * The initial state of this class and waiting for an ims commands.
         */
        @VisibleForTesting
        public final Default mDefault;

        /**
         * Indicates that {@link IImsServiceController#enableIms(int, int)} has been called and
         * waiting for an ims commands.
         * Common transitions are to
         * {@link #mDisabling} state when the disable command is received
         * or {@link #mResetting} state when the reset command is received
         * or {@link #mDisconnected} if the binder is disconnected.
         */
        @VisibleForTesting
        public final Enabled mEnabled;

        /**
         * Indicates that the state waiting for the throttle time to elapse before calling
         * {@link IImsServiceController#disableIms(int, int)}.
         * Common transitions are to
         * {@link #mEnabled} when the enable command is received.
         * or {@link #mResetting} when the reset command is received.
         * or {@link #mDisabled} the previous binder API call has passed
         * {@link ImsEnablementTracker#REQUEST_THROTTLE_TIME_MS} second, and if
         * {@link IImsServiceController#disableIms(int, int)} called.
         * or {@link #mDisabling} received a disableIms message and the previous binder API call
         * has not passed {@link ImsEnablementTracker#REQUEST_THROTTLE_TIME_MS} second.
         * Then send a disableIms message with delay.
         * or {@link #mDisconnected} if the binder is disconnected.
         */
        @VisibleForTesting
        public final Disabling mDisabling;

        /**
         * Indicates that {@link IImsServiceController#disableIms(int, int)} has been called and
         * waiting for an ims commands.
         * Common transitions are to
         * {@link #mEnabling} state when the enable command is received.
         * or {@link #mDisconnected} if the binder is disconnected.
         */
        @VisibleForTesting
        public final Disabled mDisabled;

        /**
         * Indicates that the state waiting for the throttle time to elapse before calling
         * {@link IImsServiceController#enableIms(int, int)}.
         * Common transitions are to
         * {@link #mEnabled} the previous binder API call has passed
         * {@link ImsEnablementTracker#REQUEST_THROTTLE_TIME_MS} second, and
         * {@link IImsServiceController#enableIms(int, int)} called.
         * or {@link #mDisabled} when the disable command is received.
         * or {@link #mEnabling} received an enableIms message and the previous binder API call
         * has not passed {@link ImsEnablementTracker#REQUEST_THROTTLE_TIME_MS} second.
         * Then send an enableIms message with delay.
         * or {@link #mDisconnected} if the binder is disconnected.
         */
        @VisibleForTesting
        public final Enabling mEnabling;

        /**
         * Indicates that the state waiting for the throttle time to elapse before calling
         * {@link IImsServiceController#resetIms(int, int)}.
         * Common transitions are to
         * {@link #mPostResetting} state to call either enableIms or disableIms after calling
         * {@link IImsServiceController#resetIms(int, int)}
         * or {@link #mDisconnected} if the binder is disconnected.
         */
        @VisibleForTesting
        public final Resetting mResetting;

        /**
         * Indicates that the state waiting after resetIms for the throttle time to elapse before
         * calling {@link IImsServiceController#enableIms(int, int)} or
         * {@link IImsServiceController#disableIms(int, int)}.
         * Common transitions are to
         * {@link #mEnabled} state when the disable command is received,
         * {@link #mDisabled} state when the enable command is received after calling
         * {@link IImsServiceController#enableIms(int, int)},
         * {@link IImsServiceController#disableIms(int, int)}
         * or {@link #mDisconnected} if the binder is disconnected.
         */
        public final PostResetting mPostResetting;

        /**
         * Indicates that {@link IImsServiceController} has not been set.
         * Common transition is to
         * {@link #mDefault} state when the binder is set.
         * or {@link #mDisabling} If the disable command is received while the binder is
         * disconnected
         * or {@link #mEnabling} If the enable command is received while the binder is
         * disconnected
         */

        private final Disconnected mDisconnected;
        private int mSlotId;
        private int mSubId;

        private final int mPhoneId;

        private IState mPreviousState;

        private int mLastMsg = COMMAND_NONE_MSG;

        ImsEnablementTrackerStateMachine(String name, Looper looper, int state, int slotId) {
            super(name, looper);
            mPhoneId = slotId;
            mDefault = new Default();
            mEnabled = new Enabled();
            mDisabling = new Disabling();
            mDisabled = new Disabled();
            mEnabling = new Enabling();
            mResetting = new Resetting();
            mDisconnected = new Disconnected();
            mPostResetting = new PostResetting();

            addState(mDefault);
            addState(mEnabled);
            addState(mDisabling);
            addState(mDisabled);
            addState(mEnabling);
            addState(mResetting);
            addState(mDisconnected);
            addState(mPostResetting);

            setInitialState(getState(state));
            mPreviousState = getState(state);
        }

        public void clearAllMessage() {
            Log.d(LOG_TAG, "clearAllMessage");
            removeMessages(COMMAND_ENABLE_MSG);
            removeMessages(COMMAND_DISABLE_MSG);
            removeMessages(COMMAND_RESET_MSG);
            removeMessages(COMMAND_ENABLING_DONE);
            removeMessages(COMMAND_DISABLING_DONE);
            removeMessages(COMMAND_RESETTING_DONE);
            removeMessages(COMMAND_POST_RESETTING_DONE);
        }

        public void serviceBinderConnected() {
            clearAllMessage();
            sendMessage(COMMAND_CONNECTED_MSG);
        }

        public void serviceBinderDisconnected() {
            clearAllMessage();
            sendMessage(COMMAND_DISCONNECTED_MSG);
        }

        @VisibleForTesting
        public boolean isState(int state) {
            State expect = null;
            switch (state) {
                case Default.STATE_NO:
                    expect = mDefault;
                    break;
                case Enabled.STATE_NO:
                    expect = mEnabled;
                    break;
                case Disabling.STATE_NO:
                    expect = mDisabling;
                    break;
                case Disabled.STATE_NO:
                    expect = mDisabled;
                    break;
                case Enabling.STATE_NO:
                    expect = mEnabling;
                    break;
                case Resetting.STATE_NO:
                    expect = mResetting;
                    break;
                case Disconnected.STATE_NO:
                    expect = mDisconnected;
                    break;
                case PostResetting.STATE_NO:
                    expect = mPostResetting;
                    break;
                default:
                    break;
            }
            return (getCurrentState() == expect) ? true : false;
        }

        private State getState(int state) {
            switch (state) {
                case ImsEnablementTracker.STATE_IMS_ENABLED:
                    return mEnabled;
                case ImsEnablementTracker.STATE_IMS_DISABLING:
                    return mDisabling;
                case ImsEnablementTracker.STATE_IMS_DISABLED:
                    return mDisabled;
                case ImsEnablementTracker.STATE_IMS_ENABLING:
                    return mEnabling;
                case ImsEnablementTracker.STATE_IMS_RESETTING:
                    return mResetting;
                case ImsEnablementTracker.STATE_IMS_DISCONNECTED:
                    return mDisconnected;
                case ImsEnablementTracker.STATE_IMS_POSTRESETTING:
                    return mPostResetting;
                default:
                    return mDefault;
            }
        }

        private void handleInvalidSubIdMessage() {
            clearAllMessage();
            transitionState(mDefault);
        }

        private void transitionState(State state) {
            mPreviousState = getCurrentState();
            transitionTo(state);
        }

        class Default extends State {
            private static final int STATE_NO = STATE_IMS_DEFAULT;

            @Override
            public void enter() {
                Log.d(LOG_TAG, "[" + mPhoneId + "]Default state:enter");
                mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            }

            @Override
            public boolean processMessage(Message message) {
                Log.d(LOG_TAG, "[" + mPhoneId + "]Default state:processMessage. msg.what="
                        + EVENT_DESCRIPTION.get(message.what) + ",component:" + mComponentName);

                switch (message.what) {
                    // When enableIms() is called, enableIms of binder is call and the state
                    // change to the enabled state.
                    case COMMAND_ENABLE_MSG:
                        sendEnableIms(message.arg1, message.arg2);
                        transitionState(mEnabled);
                        return HANDLED;
                    // When disableIms() is called, disableIms of binder is call and the state
                    // change to the disabled state.
                    case COMMAND_DISABLE_MSG:
                        sendDisableIms(message.arg1, message.arg2);
                        transitionState(mDisabled);
                        return HANDLED;
                    // When resetIms() is called, change to the resetting state to call enableIms
                    // after calling resetIms of binder.
                    case COMMAND_RESET_MSG:
                        mSlotId = message.arg1;
                        mSubId = message.arg2;
                        transitionState(mResetting);
                        return HANDLED;
                    case COMMAND_DISCONNECTED_MSG:
                        transitionState(mDisconnected);
                        return HANDLED;
                    default:
                        return NOT_HANDLED;
                }
            }
        }

        class Enabled extends State {
            private static final int STATE_NO = STATE_IMS_ENABLED;

            @Override
            public void enter() {
                Log.d(LOG_TAG, "[" + mPhoneId + "]Enabled state:enter");
            }

            @Override
            public boolean processMessage(Message message) {
                Log.d(LOG_TAG, "[" + mPhoneId + "]Enabled state:processMessage. msg.what="
                        + EVENT_DESCRIPTION.get(message.what) + ",component:" + mComponentName);

                switch (message.what) {
                    // the disableIms() is called.
                    case COMMAND_DISABLE_MSG:
                        mSlotId = message.arg1;
                        mSubId = message.arg2;
                        transitionState(mDisabling);
                        return HANDLED;
                    // the resetIms() is called.
                    case COMMAND_RESET_MSG:
                        mSlotId = message.arg1;
                        mSubId = message.arg2;
                        transitionState(mResetting);
                        return HANDLED;
                    case COMMAND_DISCONNECTED_MSG:
                        transitionState(mDisconnected);
                        return HANDLED;
                    case COMMAND_INVALID_SUBID_MSG:
                        handleInvalidSubIdMessage();
                        return HANDLED;
                    default:
                        return NOT_HANDLED;
                }
            }
        }

        class Disabling extends State {
            private static final int STATE_NO = STATE_IMS_DISABLING;

            @Override
            public void enter() {
                Log.d(LOG_TAG, "[" + mPhoneId + "]Disabling state:enter");
                sendMessageDelayed(COMMAND_DISABLING_DONE, mSlotId, mSubId,
                        getRemainThrottleTime());
            }

            @Override
            public boolean processMessage(Message message) {
                Log.d(LOG_TAG, "[" + mPhoneId + "]Disabling state:processMessage. msg.what="
                        + EVENT_DESCRIPTION.get(message.what) + ",component:" + mComponentName);

                switch (message.what) {
                    case COMMAND_ENABLE_MSG:
                        mSlotId = message.arg1;
                        mSubId = message.arg2;
                        clearAllMessage();
                        if (mPreviousState == mResetting) {
                            // if we are moving from Resetting -> Disabling and receive
                            // the COMMAND_ENABLE_MSG, we need to send the enableIms command,
                            // so move to Enabling state.
                            transitionState(mEnabling);
                        } else {
                            // When moving from Enabled -> Disabling and we receive an ENABLE_MSG,
                            // we can move straight back to Enabled state because we have not sent
                            // the disableIms command to IMS yet.
                            transitionState(mEnabled);
                        }
                        return HANDLED;
                    case COMMAND_DISABLING_DONE:
                        // If the disable command is received before disableIms is processed,
                        // it will be ignored because the disable command processing is in progress.
                        removeMessages(COMMAND_DISABLE_MSG);
                        sendDisableIms(message.arg1, message.arg2);
                        transitionState(mDisabled);
                        return HANDLED;
                    case COMMAND_RESET_MSG:
                        mSlotId = message.arg1;
                        mSubId = message.arg2;
                        clearAllMessage();
                        transitionState(mResetting);
                        return HANDLED;
                    case COMMAND_DISCONNECTED_MSG:
                        transitionState(mDisconnected);
                        return HANDLED;
                    case COMMAND_INVALID_SUBID_MSG:
                        handleInvalidSubIdMessage();
                        return HANDLED;
                    default:
                        return NOT_HANDLED;
                }
            }
        }

        class Disabled extends State {
            private static final int STATE_NO = STATE_IMS_DISABLED;

            @Override
            public void enter() {
                Log.d(LOG_TAG, "[" + mPhoneId + "]Disabled state:enter");
            }

            @Override
            public boolean processMessage(Message message) {
                Log.d(LOG_TAG, "[" + mPhoneId + "]Disabled state:processMessage. msg.what="
                        + EVENT_DESCRIPTION.get(message.what) + ",component:" + mComponentName);

                switch (message.what) {
                    case COMMAND_ENABLE_MSG:
                        mSlotId = message.arg1;
                        mSubId = message.arg2;
                        transitionState(mEnabling);
                        return HANDLED;
                    case COMMAND_RESET_MSG:
                        mSlotId = message.arg1;
                        mSubId = message.arg2;
                        transitionState(mResetting);
                        return HANDLED;
                    case COMMAND_DISCONNECTED_MSG:
                        transitionState(mDisconnected);
                        return HANDLED;
                    case COMMAND_INVALID_SUBID_MSG:
                        handleInvalidSubIdMessage();
                        return HANDLED;
                    default:
                        return NOT_HANDLED;
                }
            }
        }

        class Enabling extends State {
            private static final int STATE_NO = STATE_IMS_ENABLING;

            @Override
            public void enter() {
                Log.d(LOG_TAG, "[" + mPhoneId + "]Enabling state:enter");
                sendMessageDelayed(COMMAND_ENABLING_DONE, mSlotId, mSubId, getRemainThrottleTime());
            }

            @Override
            public boolean processMessage(Message message) {
                Log.d(LOG_TAG, "[" + mPhoneId + "]Enabling state:processMessage. msg.what="
                        + EVENT_DESCRIPTION.get(message.what) + ",component:" + mComponentName);

                switch (message.what) {
                    // Enabling state comes from Resetting and disableIms() is called.
                    // In this case disableIms() of binder should be called.
                    // When enabling state comes from disabled, just change state to the disabled.
                    case COMMAND_DISABLE_MSG:
                        mSlotId = message.arg1;
                        mSubId = message.arg2;
                        clearAllMessage();
                        if (mPreviousState == mResetting) {
                            transitionState(mDisabling);
                        } else {
                            transitionState(mDisabled);
                        }
                        return HANDLED;
                    case COMMAND_RESET_MSG:
                        mSlotId = message.arg1;
                        mSubId = message.arg2;
                        transitionState(mResetting);
                        return HANDLED;
                    case COMMAND_ENABLING_DONE:
                        // If the enable command is received before enableIms is processed,
                        // it will be ignored because the enable command processing is in progress.
                        removeMessages(COMMAND_ENABLE_MSG);
                        sendEnableIms(message.arg1, message.arg2);
                        transitionState(mEnabled);
                        return HANDLED;
                    case COMMAND_DISCONNECTED_MSG:
                        transitionState(mDisconnected);
                        return HANDLED;
                    case COMMAND_INVALID_SUBID_MSG:
                        handleInvalidSubIdMessage();
                        return HANDLED;
                    default:
                        return NOT_HANDLED;
                }
            }
        }

        class Resetting extends State {
            private static final int STATE_NO = STATE_IMS_RESETTING;

            @Override
            public void enter() {
                Log.d(LOG_TAG, "[" + mPhoneId + "]Resetting state:enter");
                sendMessageDelayed(COMMAND_RESETTING_DONE, mSlotId, mSubId,
                        getRemainThrottleTime());
            }

            @Override
            public boolean processMessage(Message message) {
                Log.d(LOG_TAG, "[" + mPhoneId + "]Resetting state:processMessage. msg.what="
                        + EVENT_DESCRIPTION.get(message.what) + ",component:" + mComponentName);

                switch (message.what) {
                    case COMMAND_DISABLE_MSG:
                        mLastMsg = COMMAND_DISABLE_MSG;
                        return HANDLED;
                    case COMMAND_ENABLE_MSG:
                        mLastMsg = COMMAND_ENABLE_MSG;
                        return HANDLED;
                    case COMMAND_RESETTING_DONE:
                        mSlotId = message.arg1;
                        mSubId = message.arg2;
                        // If the reset command is received before disableIms is processed,
                        // it will be ignored because the reset command processing is in progress.
                        removeMessages(COMMAND_RESET_MSG);
                        sendResetIms(mSlotId, mSubId);
                        transitionState(mPostResetting);
                        return HANDLED;
                    case COMMAND_DISCONNECTED_MSG:
                        transitionState(mDisconnected);
                        return HANDLED;
                    case COMMAND_INVALID_SUBID_MSG:
                        handleInvalidSubIdMessage();
                        return HANDLED;
                    default:
                        return NOT_HANDLED;
                }
            }
        }

        class Disconnected extends State {
            private static final int STATE_NO = STATE_IMS_DISCONNECTED;

            private int mLastMsg = COMMAND_NONE_MSG;

            @Override
            public void enter() {
                Log.d(LOG_TAG, "[" + mPhoneId + "]Disconnected state:enter");
                clearAllMessage();
            }

            @Override
            public boolean processMessage(Message message) {
                Log.d(LOG_TAG, "[" + mPhoneId + "]Disconnected state:processMessage. msg.what="
                        + EVENT_DESCRIPTION.get(message.what) + ",component:" + mComponentName);

                switch (message.what) {
                    case COMMAND_CONNECTED_MSG:
                        clearAllMessage();
                        transitionState(mDefault);
                        if (mLastMsg != COMMAND_NONE_MSG) {
                            sendMessageDelayed(mLastMsg, mSlotId, mSubId, 0);
                            mLastMsg = COMMAND_NONE_MSG;
                        }
                        return HANDLED;
                    case COMMAND_ENABLE_MSG:
                    case COMMAND_DISABLE_MSG:
                    case COMMAND_RESET_MSG:
                        mLastMsg = message.what;
                        mSlotId = message.arg1;
                        mSubId = message.arg2;
                        return HANDLED;
                    default:
                        return NOT_HANDLED;
                }
            }
        }

        class PostResetting extends State {
            private static final int STATE_NO = STATE_IMS_POSTRESETTING;

            @Override
            public void enter() {
                Log.d(LOG_TAG, "[" + mPhoneId + "]PostResetting state:enter");
                sendMessageDelayed(COMMAND_POST_RESETTING_DONE, mSlotId, mSubId,
                        getRemainThrottleTime());
            }

            @Override
            public void exit() {
                mLastMsg = COMMAND_NONE_MSG;
            }

            @Override
            public boolean processMessage(Message message) {
                Log.d(LOG_TAG, "[" + mPhoneId + "]PostResetting state:processMessage. msg.what="
                        + EVENT_DESCRIPTION.get(message.what) + ",component:" + mComponentName);

                switch (message.what) {
                    case COMMAND_POST_RESETTING_DONE:
                        mSlotId = message.arg1;
                        mSubId = message.arg2;
                        if (mLastMsg == COMMAND_DISABLE_MSG) {
                            sendDisableIms(mSlotId, mSubId);
                            transitionState(mDisabled);
                        } else {
                            // if mLastMsg is COMMAND_NONE_MSG or COMMAND_ENABLE_MSG
                            sendEnableIms(mSlotId, mSubId);
                            transitionState(mEnabled);
                        }
                        return HANDLED;
                    case COMMAND_ENABLE_MSG:
                    case COMMAND_DISABLE_MSG:
                        mLastMsg = message.what;
                        mSlotId = message.arg1;
                        mSubId = message.arg2;
                        return HANDLED;
                    case COMMAND_RESET_MSG:
                        // when resetIms() called again, skip to call
                        // IImsServiceController.resetIms(slotId, subId), but after throttle time
                        // IImsServiceController.enableIms(slotId, subId) should be called.
                        mLastMsg = COMMAND_ENABLE_MSG;
                        mSlotId = message.arg1;
                        mSubId = message.arg2;
                        return HANDLED;
                    case COMMAND_DISCONNECTED_MSG:
                        transitionState(mDisconnected);
                        return HANDLED;
                    case COMMAND_INVALID_SUBID_MSG:
                        handleInvalidSubIdMessage();
                        return HANDLED;
                    default:
                        return NOT_HANDLED;
                }
            }
        }
    }

    public ImsEnablementTracker(Looper looper, ComponentName componentName) {
        mIImsServiceController = null;
        mStateMachines = new SparseArray<>();
        mLooper = looper;
        mState = ImsEnablementTracker.STATE_IMS_DISCONNECTED;
        mComponentName = componentName;
    }

    @VisibleForTesting
    public ImsEnablementTracker(Looper looper, IImsServiceController controller, int state,
            int numSlots) {
        mIImsServiceController = controller;
        mStateMachines = new SparseArray<>();
        mLooper = looper;
        mState = state;
        mComponentName = null;

        setNumOfSlots(numSlots);
    }

    /**
     * Set the number of SIM slots.
     * @param numOfSlots the number of SIM slots.
     */
    public void setNumOfSlots(int numOfSlots) {
        int oldNumSlots = mStateMachines.size();
        Log.d(LOG_TAG, "set the slots: old[" + oldNumSlots + "], new[" + numOfSlots + "],"
                + "component:" + mComponentName);
        if (numOfSlots == oldNumSlots) {
            return;
        }
        ImsEnablementTrackerStateMachine enablementStateMachine = null;
        if (oldNumSlots < numOfSlots) {
            for (int i = oldNumSlots; i < numOfSlots; i++) {
                enablementStateMachine = new ImsEnablementTrackerStateMachine(
                        "ImsEnablementTracker", mLooper, mState, i);
                enablementStateMachine.start();
                mStateMachines.put(i, enablementStateMachine);
            }
        } else if (oldNumSlots > numOfSlots) {
            for (int i = (oldNumSlots - 1); i > (numOfSlots - 1); i--) {
                enablementStateMachine = mStateMachines.get(i);
                mStateMachines.remove(i);
                enablementStateMachine.quitNow();
            }
        }
    }

    @VisibleForTesting
    public Handler getHandler(int slotId) {
        return mStateMachines.get(slotId).getHandler();
    }

    /**
     * Check that the current state and the input state are the same.
     * @param state the input state.
     * @return true if the current state and input state are the same or false.
     */
    @VisibleForTesting
    public boolean isState(int slotId, int state) {
        return mStateMachines.get(slotId).isState(state);
    }

    /**
     * Notify the state machine that the subId has changed to invalid.
     * @param slotId subscription id
     */
    public void subIdChangedToInvalid(int slotId) {
        Log.d(LOG_TAG, "[" + slotId + "] subId changed to invalid, component:" + mComponentName);
        ImsEnablementTrackerStateMachine stateMachine = mStateMachines.get(slotId);
        if (stateMachine != null) {
            stateMachine.sendMessage(COMMAND_INVALID_SUBID_MSG, slotId);
        } else {
            Log.w(LOG_TAG, "There is no state machine associated with this slotId.");
        }
    }

    /**
     * Notify ImsService to enable IMS for the framework. This will trigger IMS registration and
     * trigger ImsFeature status updates.
     * @param slotId slot id
     * @param subId subscription id
     */
    public void enableIms(int slotId, int subId) {
        Log.d(LOG_TAG, "[" + slotId + "][" + subId + "]enableIms, component:" + mComponentName);
        ImsEnablementTrackerStateMachine stateMachine = mStateMachines.get(slotId);
        if (stateMachine != null) {
            stateMachine.sendMessage(COMMAND_ENABLE_MSG, slotId, subId);
        } else {
            Log.w(LOG_TAG, "There is no state machine associated with this slotId.");
        }
    }

    /**
     * Notify ImsService to disable IMS for the framework. This will trigger IMS de-registration and
     * trigger ImsFeature capability status to become false.
     * @param slotId slot id
     * @param subId subscription id
     */
    public void disableIms(int slotId, int subId) {
        Log.d(LOG_TAG, "[" + slotId + "][" + subId + "]disableIms, component:" + mComponentName);
        ImsEnablementTrackerStateMachine stateMachine = mStateMachines.get(slotId);
        if (stateMachine != null) {
            stateMachine.sendMessage(COMMAND_DISABLE_MSG, slotId, subId);
        } else {
            Log.w(LOG_TAG, "There is no state machine associated with this slotId.");
        }
    }

    /**
     * Notify ImsService to reset IMS for the framework. This will trigger ImsService to perform
     * de-registration and release all resource. After that, if enaleIms is called, the ImsService
     * performs registration and appropriate initialization to bring up all ImsFeatures.
     * @param slotId slot id
     * @param subId subscription id
     */
    public void resetIms(int slotId, int subId) {
        Log.d(LOG_TAG, "[" + slotId + "][" + subId + "]resetIms, component:" + mComponentName);
        ImsEnablementTrackerStateMachine stateMachine = mStateMachines.get(slotId);
        if (stateMachine != null) {
            stateMachine.sendMessage(COMMAND_RESET_MSG, slotId, subId);
        } else {
            Log.w(LOG_TAG, "There is no state machine associated with this slotId.");
        }
    }

    /**
     * Sets the IImsServiceController instance.
     */
    protected void setServiceController(IBinder serviceController) {
        synchronized (mLock) {
            mIImsServiceController = IImsServiceController.Stub.asInterface(serviceController);
            Log.d(LOG_TAG, "setServiceController with Binder:" + mIImsServiceController
                    + ", component:" + mComponentName);
            ImsEnablementTrackerStateMachine stateMachine = null;
            for (int i = 0; i < mStateMachines.size(); i++) {
                stateMachine = mStateMachines.get(i);
                if (stateMachine == null) {
                    Log.w(LOG_TAG, "There is no state machine associated with"
                            + "the slotId[" + i + "]");
                    continue;
                }
                if (isServiceControllerAvailable()) {
                    stateMachine.serviceBinderConnected();
                } else {
                    stateMachine.serviceBinderDisconnected();
                }
            }
        }
    }

    protected long getLastOperationTimeMillis() {
        return mLastImsOperationTimeMs;
    }

    /**
     * Get remaining throttle time value
     * @return remaining throttle time value
     */
    @VisibleForTesting
    public long getRemainThrottleTime() {
        long remainTime = REQUEST_THROTTLE_TIME_MS - (System.currentTimeMillis()
                - getLastOperationTimeMillis());

        if (remainTime < 0) {
            remainTime = 0L;
        }
        Log.d(LOG_TAG, "getRemainThrottleTime:" + remainTime);

        return remainTime;
    }

    /**
     * Check to see if the service controller is available.
     * @return true if available, false otherwise
     */
    private boolean isServiceControllerAvailable() {
        if (mIImsServiceController != null) {
            return true;
        }
        Log.d(LOG_TAG, "isServiceControllerAvailable : binder is not alive");
        return false;
    }

    private void sendEnableIms(int slotId, int subId) {
        try {
            synchronized (mLock) {
                if (isServiceControllerAvailable()) {
                    Log.d(LOG_TAG, "[" + slotId + "][" + subId + "]sendEnableIms,"
                            + "componentName[" + mComponentName + "]");
                    mIImsServiceController.enableIms(slotId, subId);
                    mLastImsOperationTimeMs = System.currentTimeMillis();
                }
            }
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Couldn't enable IMS: " + e.getMessage());
        }
    }

    private void sendDisableIms(int slotId, int subId) {
        try {
            synchronized (mLock) {
                if (isServiceControllerAvailable()) {
                    Log.d(LOG_TAG, "[" + slotId + "][" + subId + "]sendDisableIms"
                            + " componentName[" + mComponentName + "]");
                    mIImsServiceController.disableIms(slotId, subId);
                    mLastImsOperationTimeMs = System.currentTimeMillis();
                }
            }
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Couldn't disable IMS: " + e.getMessage());
        }
    }

    private void sendResetIms(int slotId, int subId) {
        try {
            synchronized (mLock) {
                if (isServiceControllerAvailable()) {
                    Log.d(LOG_TAG, "[" + slotId + "][" + subId + "]sendResetIms");
                    mIImsServiceController.resetIms(slotId, subId);
                    mLastImsOperationTimeMs = System.currentTimeMillis();
                }
            }
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Couldn't reset IMS: " + e.getMessage());
        }
    }
}
