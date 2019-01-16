package io.agora.openlive.poc;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Switch;

import java.io.File;

import io.agora.openlive.R;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

public class PocActivity extends Activity {
    String TAG = PocActivity.class.getSimpleName();

    FrameLayout frameLayout1;
    FrameLayout frameLayout2;
    SurfaceView surfaceView1;
    SurfaceView surfaceView2;
    Switch modeSwitch;

    RtcEngine engine;
    IRtcEngineEventHandler engineEventHandler;

    Handler worker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poc);
        frameLayout1 = findViewById(R.id.surf1);
        frameLayout2 = findViewById(R.id.surf2);
        modeSwitch = findViewById(R.id.switchBtn);

        surfaceView1 = RtcEngine.CreateRendererView(this);
        surfaceView2 = RtcEngine.CreateRendererView(this);
        frameLayout1.addView(surfaceView1);
        frameLayout2.addView(surfaceView2);

        HandlerThread handlerThread = new HandlerThread("poc-rtc-worker");
        handlerThread.start();
        worker = new Handler(handlerThread.getLooper());

        startEngineBroadcastting();
        modeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                worker.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isChecked) {
                            startEngineCommunication();
                        } else {
                            startEngineBroadcastting();
                        }
                    }
                });
            }
        });
    }

    void startEngineBroadcastting() {
        if (engine != null) {
            engine.leaveChannel();
            engine.destroy();
        }

        engineEventHandler = new PocEventHandler();
        try {
            engine = RtcEngine.create(this, getString(R.string.private_app_id), engineEventHandler);
            engine.setLogFile(Environment.getExternalStorageDirectory() + File.separator + PocActivity.this.getPackageName() + "/log/agora-rtc" + engine.hashCode() + "log");
        } catch (Exception e) {
            e.printStackTrace();
        }
        engine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        engine.enableVideo();
        engine.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER);
        engine.setupLocalVideo(new VideoCanvas(surfaceView1));
        engine.switchCamera();
        Log.e(TAG, "startPreview");
        engine.startPreview();
        engine.joinChannel(null, "cname-brdc", "", 0);
    }

    void startEngineCommunication() {
        if (engine != null) {
            engine.leaveChannel();
            engine.destroy();
        }

        engineEventHandler = new PocEventHandler();
        try {
            engine = RtcEngine.create(this, getString(R.string.private_app_id), engineEventHandler);
            engine.setLogFile(Environment.getExternalStorageDirectory() + File.separator + PocActivity.this.getPackageName() + "/log/agora-rtc" + engine.hashCode() + "log");
        } catch (Exception e) {
            e.printStackTrace();
        }
        engine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
        engine.enableVideo();
        engine.setupLocalVideo(new VideoCanvas(surfaceView1));
        engine.switchCamera();
        Log.e(TAG, "startPreview");
        engine.startPreview();
        engine.joinChannel(null, "cname-comm", "", 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (engine != null) {
            engine.leaveChannel();
        }
    }

    class PocEventHandler extends IRtcEngineEventHandler {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            Log.e(TAG, "onJoinChannelSuccess");
        }

        @Override
        public void onRejoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onRejoinChannelSuccess(channel, uid, elapsed);
        }

        @Override
        public void onWarning(int warn) {
            super.onWarning(warn);
//            Log.w(TAG, "onWarning " + warn);

        }

        @Override
        public void onError(int err) {
            super.onError(err);
            Log.e(TAG, "onError " + err);

        }

        @Override
        public void onApiCallExecuted(int error, String api, String result) {
            super.onApiCallExecuted(error, api, result);
            Log.w(TAG, "onApiCallExecuted error: " + error + " api: " + api + " result: " + result);

        }

        @Override
        public void onCameraReady() {
            super.onCameraReady();
            Log.w(TAG, "onCameraReady");

        }

        @Override
        public void onCameraFocusAreaChanged(Rect rect) {
            super.onCameraFocusAreaChanged(rect);
        }

        @Override
        public void onVideoStopped() {
            super.onVideoStopped();
        }

        @Override
        public void onAudioQuality(int uid, int quality, short delay, short lost) {
            super.onAudioQuality(uid, quality, delay, lost);
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            super.onLeaveChannel(stats);
            Log.w(TAG, "onLeaveChannel");

        }

        @Override
        public void onRtcStats(RtcStats stats) {
            super.onRtcStats(stats);
//            Log.w(TAG, "onRtcStats");

        }

        @Override
        public void onAudioVolumeIndication(AudioVolumeInfo[] speakers, int totalVolume) {
            super.onAudioVolumeIndication(speakers, totalVolume);
        }

        @Override
        public void onNetworkQuality(int uid, int txQuality, int rxQuality) {
            super.onNetworkQuality(uid, txQuality, rxQuality);
        }

        @Override
        public void onLastmileQuality(int quality) {
            super.onLastmileQuality(quality);
        }

        @Override
        public void onUserJoined(final int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            Log.e(TAG, "onUserJoined " + uid);
            worker.post(new Runnable() {
                @Override
                public void run() {
                    engine.setupRemoteVideo(new VideoCanvas(surfaceView2, VideoCanvas.RENDER_MODE_HIDDEN, uid));
                }
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            super.onUserOffline(uid, reason);
            Log.w(TAG, "onUserOffline uid: " + uid + " reason: " + reason);

        }

        @Override
        public void onUserMuteAudio(int uid, boolean muted) {
            super.onUserMuteAudio(uid, muted);
        }

        @Override
        public void onUserMuteVideo(int uid, boolean muted) {
            super.onUserMuteVideo(uid, muted);
        }

        @Override
        public void onUserEnableVideo(int uid, boolean enabled) {
            super.onUserEnableVideo(uid, enabled);
            Log.w(TAG, "onUserEnableVideo uid: " + uid + " enabled: " + enabled);

        }

        @Override
        public void onUserEnableLocalVideo(int uid, boolean enabled) {
            super.onUserEnableLocalVideo(uid, enabled);
            Log.w(TAG, "onUserEnableLocalVideo uid: " + uid + " enabled: " + enabled);

        }

        @Override
        public void onLocalVideoStat(int sentBitrate, int sentFrameRate) {
            super.onLocalVideoStat(sentBitrate, sentFrameRate);
        }

        @Override
        public void onRemoteVideoStat(int uid, int delay, int receivedBitrate, int receivedFrameRate) {
            super.onRemoteVideoStat(uid, delay, receivedBitrate, receivedFrameRate);
        }

        @Override
        public void onRemoteVideoStats(RemoteVideoStats stats) {
            super.onRemoteVideoStats(stats);
        }

        @Override
        public void onLocalVideoStats(LocalVideoStats stats) {
            super.onLocalVideoStats(stats);
        }

        @Override
        public void onFirstRemoteVideoFrame(int uid, int width, int height, int elapsed) {
            super.onFirstRemoteVideoFrame(uid, width, height, elapsed);
            Log.w(TAG, "onFirstRemoteVideoFrame uid: " + uid + " width: " + width + " height: " + height + " elapsed: " + elapsed);

        }

        @Override
        public void onFirstLocalVideoFrame(int width, int height, int elapsed) {
            super.onFirstLocalVideoFrame(width, height, elapsed);
            Log.w(TAG, "onFirstLocalVideoFrame width: " + width + " height: " + height + " elapsed: " + elapsed);

        }

        @Override
        public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
            super.onFirstRemoteVideoDecoded(uid, width, height, elapsed);
            Log.w(TAG, "onFirstRemoteVideoDecoded width: " + width + " height: " + height + " elapsed: " + elapsed);

        }

        @Override
        public void onVideoSizeChanged(int uid, int width, int height, int rotation) {
            super.onVideoSizeChanged(uid, width, height, rotation);
        }

        @Override
        public void onConnectionLost() {
            super.onConnectionLost();
            Log.w(TAG, "onConnectionLost");

        }

        @Override
        public void onConnectionInterrupted() {
            super.onConnectionInterrupted();
            Log.w(TAG, "onConnectionInterrupted");

        }

        @Override
        public void onConnectionBanned() {
            super.onConnectionBanned();
        }

        @Override
        public void onStreamMessage(int uid, int streamId, byte[] data) {
            super.onStreamMessage(uid, streamId, data);
        }

        @Override
        public void onRemoteVideoStateChanged(int uid, int state) {
            super.onRemoteVideoStateChanged(uid, state);
        }

        @Override
        public void onStreamMessageError(int uid, int streamId, int error, int missed, int cached) {
            super.onStreamMessageError(uid, streamId, error, missed, cached);
        }

        @Override
        public void onMediaEngineLoadSuccess() {
            super.onMediaEngineLoadSuccess();
        }

        @Override
        public void onMediaEngineStartCallSuccess() {
            super.onMediaEngineStartCallSuccess();
        }

        @Override
        public void onAudioMixingFinished() {
            super.onAudioMixingFinished();
        }

        @Override
        public void onRequestToken() {
            super.onRequestToken();
        }

        @Override
        public void onAudioRouteChanged(int routing) {
            super.onAudioRouteChanged(routing);
        }

        @Override
        public void onFirstLocalAudioFrame(int elapsed) {
            super.onFirstLocalAudioFrame(elapsed);
        }

        @Override
        public void onFirstRemoteAudioFrame(int uid, int elapsed) {
            super.onFirstRemoteAudioFrame(uid, elapsed);
        }

        @Override
        public void onActiveSpeaker(int uid) {
            super.onActiveSpeaker(uid);
        }

        @Override
        public void onAudioEffectFinished(int soundId) {
            super.onAudioEffectFinished(soundId);
        }

        @Override
        public void onClientRoleChanged(int oldRole, int newRole) {
            super.onClientRoleChanged(oldRole, newRole);
        }

        @Override
        public void onStreamPublished(String url, int error) {
            super.onStreamPublished(url, error);
        }

        @Override
        public void onStreamUnpublished(String url) {
            super.onStreamUnpublished(url);
        }

        @Override
        public void onTranscodingUpdated() {
            super.onTranscodingUpdated();
        }

        @Override
        public void onStreamInjectedStatus(String url, int uid, int status) {
            super.onStreamInjectedStatus(url, uid, status);
        }

        @Override
        public void onTokenPrivilegeWillExpire(String token) {
            super.onTokenPrivilegeWillExpire(token);
        }

        @Override
        public void onLocalPublishFallbackToAudioOnly(boolean isFallbackOrRecover) {
            super.onLocalPublishFallbackToAudioOnly(isFallbackOrRecover);
        }

        @Override
        public void onRemoteSubscribeFallbackToAudioOnly(int uid, boolean isFallbackOrRecover) {
            super.onRemoteSubscribeFallbackToAudioOnly(uid, isFallbackOrRecover);
        }

        @Override
        public void onRemoteVideoTransportStats(int uid, int delay, int lost, int rxKBitRate) {
            super.onRemoteVideoTransportStats(uid, delay, lost, rxKBitRate);
        }

        @Override
        public void onRemoteAudioTransportStats(int uid, int delay, int lost, int rxKBitRate) {
            super.onRemoteAudioTransportStats(uid, delay, lost, rxKBitRate);
        }
    }
}
