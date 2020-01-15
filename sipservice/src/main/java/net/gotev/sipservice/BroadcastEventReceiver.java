package net.gotev.sipservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import net.gotev.sipservice.event.CallComingEvent;
import net.gotev.sipservice.event.CallConfirmedEvent;
import net.gotev.sipservice.event.CallDisconnectEvent;
import net.gotev.sipservice.event.CallOutEvent;
import net.gotev.sipservice.log.TyLog;

import org.greenrobot.eventbus.EventBus;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_status_code;

import java.util.ArrayList;

/**
 * Reference implementation to receive events emitted by the sip service.
 * @author gotev (Aleksandar Gotev)
 */
public class BroadcastEventReceiver extends BroadcastReceiver implements SipServiceConstants{

    private static final String LOG_TAG = "SipServiceBR";

    private Context receiverContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        //save context internally for convenience in subclasses, which can get it with
        //getReceiverContext method
        receiverContext = context;

        String action = intent.getAction();

        if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.REGISTRATION).equals(action)) {
            int stateCode = intent.getIntExtra(PARAM_REGISTRATION_CODE, -1);
            onRegistration(intent.getStringExtra(PARAM_ACCOUNT_ID),
                           pjsip_status_code.swigToEnum(stateCode));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.INCOMING_CALL).equals(action)) {
            onIncomingCall(intent.getStringExtra(PARAM_ACCOUNT_ID),
                    intent.getIntExtra(PARAM_CALL_ID, -1),
                    intent.getStringExtra(PARAM_DISPLAY_NAME),
                    intent.getStringExtra(PARAM_REMOTE_URI),
                    intent.getBooleanExtra(PARAM_IS_VIDEO, false));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.CALL_STATE).equals(action)) {
            int callState = intent.getIntExtra(PARAM_CALL_STATE, -1);
            int callStatus = intent.getIntExtra(PARAM_CALL_STATUS, -1);
            onCallState(intent.getStringExtra(PARAM_ACCOUNT_ID),
                        intent.getIntExtra(PARAM_CALL_ID, -1),
                        pjsip_inv_state.swigToEnum(callState),
                        (callStatus > 0) ? pjsip_status_code.swigToEnum(callStatus) : null,
                        intent.getLongExtra(PARAM_CONNECT_TIMESTAMP, -1),
                        intent.getBooleanExtra(PARAM_LOCAL_HOLD, false),
                        intent.getBooleanExtra(PARAM_LOCAL_MUTE, false),
                        intent.getBooleanExtra(PARAM_LOCAL_VIDEO_MUTE, false));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.OUTGOING_CALL).equals(action)) {
            onOutgoingCall(intent.getStringExtra(PARAM_ACCOUNT_ID),
                    intent.getIntExtra(PARAM_CALL_ID, -1),
                    intent.getStringExtra(PARAM_NUMBER),
                    intent.getBooleanExtra(PARAM_IS_VIDEO, false),
                    intent.getBooleanExtra(PARAM_IS_VIDEO_CONF, false));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.STACK_STATUS).equals(action)) {
            onStackStatus(intent.getBooleanExtra(PARAM_STACK_STARTED, false));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.CODEC_PRIORITIES).equals(action)) {
            ArrayList<CodecPriority> codecList = intent.getParcelableArrayListExtra(PARAM_CODEC_PRIORITIES_LIST);
            onReceivedCodecPriorities(codecList);

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.CODEC_PRIORITIES_SET_STATUS).equals(action)) {
            onCodecPrioritiesSetStatus(intent.getBooleanExtra(PARAM_SUCCESS, false));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.MISSED_CALL).equals(action)) {
            onMissedCall(intent.getStringExtra(PARAM_DISPLAY_NAME),
                    intent.getStringExtra(PARAM_REMOTE_URI));

        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.VIDEO_SIZE).equals(action)) {
            onVideoSize(intent.getIntExtra(PARAM_INCOMING_VIDEO_WIDTH, H264_DEF_WIDTH),
                    intent.getIntExtra(PARAM_INCOMING_VIDEO_HEIGHT, H264_DEF_HEIGHT));
        } else if (BroadcastEventEmitter.getAction(BroadcastEventEmitter.BroadcastAction.CALL_STATS).equals(action)) {
            int callStatus = intent.getIntExtra(PARAM_CALL_STATUS, -1);
            onCallStats(intent.getIntExtra(PARAM_CALL_STATS_DURATION, 0),
                intent.getStringExtra(PARAM_CALL_STATS_AUDIO_CODEC),
                (callStatus > 0) ? pjsip_status_code.swigToEnum(callStatus) : null,
                (RtpStreamStats) intent.getParcelableExtra(PARAM_CALL_STATS_RX_STREAM),
                (RtpStreamStats) intent.getParcelableExtra(PARAM_CALL_STATS_TX_STREAM));
        }
    }

    protected Context getReceiverContext() {
        return receiverContext;
    }

    /**
     * Register this broadcast receiver.
     * It's recommended to register the receiver in Activity's onResume method.
     *
     * @param context context in which to register this receiver
     */
    public void register(final Context context) {

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.REGISTRATION));
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.INCOMING_CALL));
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.CALL_STATE));
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.OUTGOING_CALL));
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.STACK_STATUS));
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.CODEC_PRIORITIES));
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.CODEC_PRIORITIES_SET_STATUS));
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.MISSED_CALL));
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.VIDEO_SIZE));
        intentFilter.addAction(BroadcastEventEmitter.getAction(
                BroadcastEventEmitter.BroadcastAction.CALL_STATS));
        context.registerReceiver(this, intentFilter);
    }

    /**
     * Unregister this broadcast receiver.
     * It's recommended to unregister the receiver in Activity's onPause method.
     *
     * @param context context in which to unregister this receiver
     */
    public void unregister(final Context context) {
        context.unregisterReceiver(this);
    }

    public void onRegistration(String accountID, pjsip_status_code registrationStateCode) {
        TyLog.i("onRegistration - accountID: " + accountID +
                ", registrationStateCode: " + registrationStateCode);
    }

    public void onIncomingCall(String accountID, int callID, String displayName, String remoteUri, boolean isVideo) {
        TyLog.i("onIncomingCall - accountID: " + accountID +
                ", callID: " + callID +
                ", displayName: " + displayName +
                ", remoteUri: " + remoteUri);
        EventBus.getDefault().post(new CallComingEvent(accountID,callID,displayName));
    }

    public void onCallState(String accountID, int callID, pjsip_inv_state callStateCode, pjsip_status_code callStatusCode,
                            long connectTimestamp, boolean isLocalHold, boolean isLocalMute, boolean isLocalVideoMute) {
        TyLog.i("onCallState - accountID: " + accountID +
                ", callID: " + callID +
                ", callStateCode: " + callStateCode +
                ", callStatusCode: " + callStatusCode +
                ", connectTimestamp: " + connectTimestamp +
                ", isLocalHold: " + isLocalHold +
                ", isLocalMute: " + isLocalMute +
                ", isLocalVideoMute: " + isLocalVideoMute);

        //对方接听 callStateCode=PJSIP_INV_STATE_CONFIRMED
        if(callStateCode==pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED){
            EventBus.getDefault().post(new CallConfirmedEvent(callID));
        }
        //挂断 callStateCode=PJSIP_INV_STATE_DISCONNECTED
        else if(callStateCode==pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED){
            EventBus.getDefault().post(new CallDisconnectEvent(callID));
        }
    }

    public void onOutgoingCall(String accountID, int callID, String number, boolean isVideo, boolean isVideoConference) {
        TyLog.i("onOutgoingCall - accountID: " + accountID +
                ", callID: " + callID +
                ", number: " + number);
        EventBus.getDefault().post(new CallOutEvent(accountID,callID,number));
    }

    public void onStackStatus(boolean started) {
        TyLog.i("SIP service stack " + (started ? "started" : "stopped"));
    }

    public void onReceivedCodecPriorities(ArrayList<CodecPriority> codecPriorities) {
        TyLog.i("Received codec priorities");
        for (CodecPriority codec : codecPriorities) {
            TyLog.i(codec.toString());
        }
    }

    public void onCodecPrioritiesSetStatus(boolean success) {
        TyLog.i("Codec priorities " + (success ? "successfully set" : "set error"));
    }

    public void onMissedCall(String displayName, String uri) {
        TyLog.i("Missed call from " + displayName);
    }

    protected void onVideoSize(int width, int height) {
        TyLog.i("Video resolution " + width+"x"+height);
    }

    protected void onCallStats(int duration, String audioCodec, pjsip_status_code callStatusCode, RtpStreamStats rx, RtpStreamStats tx) {
        TyLog.i("Call Stats sent "+duration+" "+audioCodec);
    }
}
