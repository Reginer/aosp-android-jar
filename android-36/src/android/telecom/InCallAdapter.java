/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.telecom;

import android.annotation.NonNull;
import android.bluetooth.BluetoothDevice;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.OutcomeReceiver;
import android.os.RemoteException;
import android.os.ResultReceiver;

import com.android.internal.telecom.IInCallAdapter;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Receives commands from {@link InCallService} implementations which should be executed by
 * Telecom. When Telecom binds to a {@link InCallService}, an instance of this class is given to
 * the in-call service through which it can manipulate live (active, dialing, ringing) calls. When
 * the in-call service is notified of new calls, it can use the
 * given call IDs to execute commands such as {@link #answerCall} for incoming calls or
 * {@link #disconnectCall} for active calls the user would like to end. Some commands are only
 * appropriate for calls in certain states; please consult each method for such limitations.
 * <p>
 * The adapter will stop functioning when there are no more calls.
 *
 * @hide
 */
public final class InCallAdapter {
    private final IInCallAdapter mAdapter;

    /**
     * {@hide}
     */
    public InCallAdapter(IInCallAdapter adapter) {
        mAdapter = adapter;
    }

    /**
     * Instructs Telecom to answer the specified call.
     *
     * @param callId The identifier of the call to answer.
     * @param videoState The video state in which to answer the call.
     */
    public void answerCall(String callId, int videoState) {
        try {
            mAdapter.answerCall(callId, videoState);
        } catch (RemoteException e) {
        }
    }

    /**
     * Instructs Telecom to deflect the specified call.
     *
     * @param callId The identifier of the call to deflect.
     * @param address The address to deflect.
     */
    public void deflectCall(String callId, Uri address) {
        try {
            mAdapter.deflectCall(callId, address);
        } catch (RemoteException e) {
        }
    }

    /**
     * Instructs Telecom to reject the specified call.
     *
     * @param callId The identifier of the call to reject.
     * @param rejectWithMessage Whether to reject with a text message.
     * @param textMessage An optional text message with which to respond.
     */
    public void rejectCall(String callId, boolean rejectWithMessage, String textMessage) {
        try {
            mAdapter.rejectCall(callId, rejectWithMessage, textMessage);
        } catch (RemoteException e) {
        }
    }

    /**
     * Instructs Telecom to reject the specified call.
     *
     * @param callId The identifier of the call to reject.
     * @param rejectReason The reason the call was rejected.
     */
    public void rejectCall(String callId, @Call.RejectReason int rejectReason) {
        try {
            mAdapter.rejectCallWithReason(callId, rejectReason);
        } catch (RemoteException e) {
        }
    }

    /**
     * Instructs Telecom to transfer the specified call.
     *
     * @param callId The identifier of the call to transfer.
     * @param targetNumber The address to transfer to.
     * @param isConfirmationRequired if {@code true} it will initiate a confirmed transfer,
     * if {@code false}, it will initiate unconfirmed transfer.
     */
    public void transferCall(@NonNull String callId, @NonNull Uri targetNumber,
            boolean isConfirmationRequired) {
        try {
            mAdapter.transferCall(callId, targetNumber, isConfirmationRequired);
        } catch (RemoteException e) {
        }
    }

    /**
     * Instructs Telecom to transfer the specified call to another ongoing call.
     *
     * @param callId The identifier of the call to transfer.
     * @param otherCallId The identifier of the other call to which this will be transferred.
     */
    public void transferCall(@NonNull String callId, @NonNull String otherCallId) {
        try {
            mAdapter.consultativeTransfer(callId, otherCallId);
        } catch (RemoteException e) {
        }
    }

    /**
     * Instructs Telecom to disconnect the specified call.
     *
     * @param callId The identifier of the call to disconnect.
     */
    public void disconnectCall(String callId) {
        try {
            mAdapter.disconnectCall(callId);
        } catch (RemoteException e) {
        }
    }

    /**
     * Instructs Telecom to put the specified call on hold.
     *
     * @param callId The identifier of the call to put on hold.
     */
    public void holdCall(String callId) {
        try {
            mAdapter.holdCall(callId);
        } catch (RemoteException e) {
        }
    }

    /**
     * Instructs Telecom to release the specified call from hold.
     *
     * @param callId The identifier of the call to release from hold.
     */
    public void unholdCall(String callId) {
        try {
            mAdapter.unholdCall(callId);
        } catch (RemoteException e) {
        }
    }

    /**
     * Mute the microphone.
     *
     * @param shouldMute True if the microphone should be muted.
     */
    public void mute(boolean shouldMute) {
        try {
            mAdapter.mute(shouldMute);
        } catch (RemoteException e) {
        }
    }

    /**
     * Sets the audio route (speaker, bluetooth, etc...). See {@link CallAudioState}.
     *
     * @param route The audio route to use.
     */
    public void setAudioRoute(int route) {
        try {
            mAdapter.setAudioRoute(route, null);
        } catch (RemoteException e) {
        }
    }

    /**
     * @see Call#enterBackgroundAudioProcessing()
     */
    public void enterBackgroundAudioProcessing(String callId) {
        try {
            mAdapter.enterBackgroundAudioProcessing(callId);
        } catch (RemoteException e) {
        }
    }

    /**
     * @see Call#exitBackgroundAudioProcessing(boolean)
     */
    public void exitBackgroundAudioProcessing(String callId, boolean shouldRing) {
        try {
            mAdapter.exitBackgroundAudioProcessing(callId, shouldRing);
        } catch (RemoteException e) {
        }
    }

    /**
     * Request audio routing to a specific bluetooth device. Calling this method may result in
     * the device routing audio to a different bluetooth device than the one specified. A list of
     * available devices can be obtained via {@link CallAudioState#getSupportedBluetoothDevices()}
     *
     * @param bluetoothAddress The address of the bluetooth device to connect to, as returned by
     * {@link BluetoothDevice#getAddress()}, or {@code null} if no device is preferred.
     */
    public void requestBluetoothAudio(String bluetoothAddress) {
        try {
            mAdapter.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH, bluetoothAddress);
        } catch (RemoteException e) {
        }
    }

    /**
     * Request audio routing to a specific CallEndpoint.. See {@link CallEndpoint}.
     *
     * @param endpoint The call endpoint to use.
     * @param executor The executor of where the callback will execute.
     * @param callback The callback to notify the result of the endpoint change.
     */
    public void requestCallEndpointChange(CallEndpoint endpoint, Executor executor,
            OutcomeReceiver<Void, CallEndpointException> callback) {
        try {
            mAdapter.requestCallEndpointChange(endpoint, new ResultReceiver(null) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle result) {
                    super.onReceiveResult(resultCode, result);
                    final long identity = Binder.clearCallingIdentity();
                    try {
                        if (resultCode == CallEndpoint.ENDPOINT_OPERATION_SUCCESS) {
                            executor.execute(() -> callback.onResult(null));
                        } else {
                            executor.execute(() -> callback.onError(
                                    result.getParcelable(CallEndpointException.CHANGE_ERROR,
                                            CallEndpointException.class)));
                        }
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            });
        } catch (RemoteException e) {
            Log.d(this, "Remote exception calling requestCallEndpointChange");
        }
    }

    /**
     * Instructs Telecom to play a dual-tone multi-frequency signaling (DTMF) tone in a call.
     *
     * Any other currently playing DTMF tone in the specified call is immediately stopped.
     *
     * @param callId The unique ID of the call in which the tone will be played.
     * @param digit A character representing the DTMF digit for which to play the tone. This
     *         value must be one of {@code '0'} through {@code '9'}, {@code '*'} or {@code '#'}.
     */
    public void playDtmfTone(String callId, char digit) {
        try {
            mAdapter.playDtmfTone(callId, digit);
        } catch (RemoteException e) {
        }
    }

    /**
     * Instructs Telecom to stop any dual-tone multi-frequency signaling (DTMF) tone currently
     * playing.
     *
     * DTMF tones are played by calling {@link #playDtmfTone(String,char)}. If no DTMF tone is
     * currently playing, this method will do nothing.
     *
     * @param callId The unique ID of the call in which any currently playing tone will be stopped.
     */
    public void stopDtmfTone(String callId) {
        try {
            mAdapter.stopDtmfTone(callId);
        } catch (RemoteException e) {
        }
    }

    /**
     * Instructs Telecom to continue playing a post-dial DTMF string.
     *
     * A post-dial DTMF string is a string of digits entered after a phone number, when dialed,
     * that are immediately sent as DTMF tones to the recipient as soon as the connection is made.
     * While these tones are playing, Telecom will notify the {@link InCallService} that the call
     * is in the post dial state.
     *
     * If the DTMF string contains a {@link TelecomManager#DTMF_CHARACTER_PAUSE} symbol, Telecom
     * will temporarily pause playing the tones for a pre-defined period of time.
     *
     * If the DTMF string contains a {@link TelecomManager#DTMF_CHARACTER_WAIT} symbol, Telecom
     * will pause playing the tones and notify the {@link InCallService} that the call is in the
     * post dial wait state. When the user decides to continue the postdial sequence, the
     * {@link InCallService} should invoke the {@link #postDialContinue(String,boolean)} method.
     *
     * @param callId The unique ID of the call for which postdial string playing should continue.
     * @param proceed Whether or not to continue with the post-dial sequence.
     */
    public void postDialContinue(String callId, boolean proceed) {
        try {
            mAdapter.postDialContinue(callId, proceed);
        } catch (RemoteException e) {
        }
    }

    /**
     * Instructs Telecom to add a PhoneAccountHandle to the specified call.
     *
     * @param callId The identifier of the call.
     * @param accountHandle The PhoneAccountHandle through which to place the call.
     * @param setDefault {@code True} if this account should be set as the default for calls.
     */
    public void phoneAccountSelected(String callId, PhoneAccountHandle accountHandle,
            boolean setDefault) {
        try {
            mAdapter.phoneAccountSelected(callId, accountHandle, setDefault);
        } catch (RemoteException e) {
        }
    }

    /**
     * Instructs Telecom to conference the specified call.
     *
     * @param callId The unique ID of the call.
     * @hide
     */
    public void conference(String callId, String otherCallId) {
        try {
            mAdapter.conference(callId, otherCallId);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Instructs Telecom to pull participants to existing call
     *
     * @param callId The unique ID of the call.
     * @param participants participants to be pulled to existing call.
     */
    public void addConferenceParticipants(String callId, List<Uri> participants) {
        try {
            mAdapter.addConferenceParticipants(callId, participants);
        } catch (RemoteException ignored) {
        }
    }


    /**
     * Instructs Telecom to split the specified call from any conference call with which it may be
     * connected.
     *
     * @param callId The unique ID of the call.
     * @hide
     */
    public void splitFromConference(String callId) {
        try {
            mAdapter.splitFromConference(callId);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Instructs Telecom to merge child calls of the specified conference call.
     */
    public void mergeConference(String callId) {
        try {
            mAdapter.mergeConference(callId);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Instructs Telecom to swap the child calls of the specified conference call.
     */
    public void swapConference(String callId) {
        try {
            mAdapter.swapConference(callId);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Instructs Telecom to pull an external call to the local device.
     *
     * @param callId The callId to pull.
     */
    public void pullExternalCall(String callId) {
        try {
            mAdapter.pullExternalCall(callId);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Intructs Telecom to send a call event.
     *
     * @param callId The callId to send the event for.
     * @param event The event.
     * @param targetSdkVer Target sdk version of the app calling this api
     * @param extras Extras associated with the event.
     */
    public void sendCallEvent(String callId, String event, int targetSdkVer, Bundle extras) {
        try {
            mAdapter.sendCallEvent(callId, event, targetSdkVer, extras);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Intructs Telecom to add extras to a call.
     *
     * @param callId The callId to add the extras to.
     * @param extras The extras.
     */
    public void putExtras(String callId, Bundle extras) {
        try {
            mAdapter.putExtras(callId, extras);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Intructs Telecom to add an extra to a call.
     *
     * @param callId The callId to add the extras to.
     * @param key The extra key.
     * @param value The extra value.
     */
    public void putExtra(String callId, String key, boolean value) {
        try {
            Bundle bundle = new Bundle();
            bundle.putBoolean(key, value);
            mAdapter.putExtras(callId, bundle);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Intructs Telecom to add an extra to a call.
     *
     * @param callId The callId to add the extras to.
     * @param key The extra key.
     * @param value The extra value.
     */
    public void putExtra(String callId, String key, int value) {
        try {
            Bundle bundle = new Bundle();
            bundle.putInt(key, value);
            mAdapter.putExtras(callId, bundle);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Intructs Telecom to add an extra to a call.
     *
     * @param callId The callId to add the extras to.
     * @param key The extra key.
     * @param value The extra value.
     */
    public void putExtra(String callId, String key, String value) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString(key, value);
            mAdapter.putExtras(callId, bundle);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Intructs Telecom to remove extras from a call.
     * @param callId The callId to remove the extras from.
     * @param keys The extra keys to remove.
     */
    public void removeExtras(String callId, List<String> keys) {
        try {
            mAdapter.removeExtras(callId, keys);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Instructs Telecom to turn the proximity sensor on.
     */
    public void turnProximitySensorOn() {
        try {
            mAdapter.turnOnProximitySensor();
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Instructs Telecom to turn the proximity sensor off.
     *
     * @param screenOnImmediately If true, the screen will be turned on immediately if it was
     * previously off. Otherwise, the screen will only be turned on after the proximity sensor
     * is no longer triggered.
     */
    public void turnProximitySensorOff(boolean screenOnImmediately) {
        try {
            mAdapter.turnOffProximitySensor(screenOnImmediately);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Sends an RTT upgrade request to the remote end of the connection.
     */
    public void sendRttRequest(String callId) {
        try {
            mAdapter.sendRttRequest(callId);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Responds to an RTT upgrade request initiated from the remote end.
     *
     * @param id the ID of the request as specified by Telecom
     * @param accept Whether the request should be accepted.
     */
    public void respondToRttRequest(String callId, int id, boolean accept) {
        try {
            mAdapter.respondToRttRequest(callId, id, accept);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Instructs Telecom to shut down the RTT communication channel.
     */
    public void stopRtt(String callId) {
        try {
            mAdapter.stopRtt(callId);
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Sets the RTT audio mode.
     * @param mode the desired RTT audio mode
     */
    public void setRttMode(String callId, int mode) {
        try {
            mAdapter.setRttMode(callId, mode);
        } catch (RemoteException ignored) {
        }
    }


    /**
     * Initiates a handover of this {@link Call} to the {@link ConnectionService} identified
     * by destAcct.
     * @param callId The callId of the Call which calls this function.
     * @param destAcct ConnectionService to which the call should be handed over.
     * @param videoState The video state desired after the handover.
     * @param extras Extra information to be passed to ConnectionService
     */
    public void handoverTo(String callId, PhoneAccountHandle destAcct, int videoState,
                           Bundle extras) {
        try {
            mAdapter.handoverTo(callId, destAcct, videoState, extras);
        } catch (RemoteException ignored) {
        }
    }
}
