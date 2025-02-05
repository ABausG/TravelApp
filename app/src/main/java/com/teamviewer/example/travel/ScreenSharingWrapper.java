/*
 * Copyright 2019 TeamViewer (www.teamviewer.com).  All rights reserved.
 *
 * Please refer to the end user license agreement (EULA), the app developer agreement
 * and license information associated with this source code for terms and conditions
 * that govern your use of this software.
 */

package com.teamviewer.example.travel;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.teamviewer.example.travel.callbacks.AccessControlCallbackImpl;
import com.teamviewer.example.travel.callbacks.AuthenticationCallbackImpl;
import com.teamviewer.example.travel.callbacks.InputCallbackImpl;
import com.teamviewer.sdk.pilotsessionui.PilotSessionUI;
import com.teamviewer.sdk.screensharing.AccessControlRule;
import com.teamviewer.sdk.screensharing.AccessType;
import com.teamviewer.sdk.screensharing.DefaultLogger;
import com.teamviewer.sdk.screensharing.PilotSession;
import com.teamviewer.sdk.screensharing.ScreenSharingSession;
import com.teamviewer.sdk.screensharing.SessionCallback;
import com.teamviewer.sdk.screensharing.Settings;
import com.teamviewer.sdk.screensharing.TeamViewerSdk;
import com.teamviewer.sdk.screensharing.TeamViewerSession;

/**
 * Encapsulates the TeamViewer Screen Sharing SDK.
 */
public final class ScreenSharingWrapper {

    private static final ScreenSharingWrapper sInstance = new ScreenSharingWrapper();
    /**
     * A valid SDK Token which was created in the
     * <a href="https://login.teamviewer.com/nav/api/create/mobile">TeamViewer Management Console</a>.
     * //TODO Add SDK_TOKEN here in the form "{xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}"
     */
    private static final String SDK_TOKEN = ;

    private RunningStateListener mRunningStateListener;
    private TeamViewerSdk m_teamViewerSdk;
    private TeamViewerSession mCurrentSession;

    public static ScreenSharingWrapper getInstance() {
        return sInstance;
    }

    private ScreenSharingWrapper() {
        // created by the factory method
    }

    /**
     * @param listener A listener object, or {@code null} to unregister.
     */
    public void setRunningStateListener(RunningStateListener listener) {
        mRunningStateListener = listener;
    }

    /**
     * The TeamViewer session will be started asynchronously.
     *
     * @param context The application's context
     */
    public void startTeamViewerSession(Context context, String sessionCode) {
        InputCallbackImpl inputCallback = new InputCallbackImpl(context);

        m_teamViewerSdk = new TeamViewerSdk.Builder(context)
                .withToken(SDK_TOKEN)
                .withLogger(new DefaultLogger())
                .withErrorCallback(errorCode -> {
                    Log.e("TeamViewerSdk", "Error: " + errorCode);
                    Toast.makeText(context, errorCode.toString(), Toast.LENGTH_SHORT).show();
                })
                .withSessionCallback(new SessionCallback() {
                    @Override
                    public void onSessionStarted(TeamViewerSession teamViewerSession) {
                        sessionStarted(context, teamViewerSession);
                    }

                    @Override
                    public void onSessionEnded() {
                        sessionEnded();
                        inputCallback.terminate();
                    }
                })
                .withAuthenticationCallback(new AuthenticationCallbackImpl(context))
                .withSettings(createSettings())
                .withAccessControlCallback(new AccessControlCallbackImpl(context))
                .withInputCallback(inputCallback)
                .build();
        m_teamViewerSdk.connectToSessionCode(sessionCode);
    }

    private Settings createSettings() {
        Settings settings = new Settings();
        settings.accessControlRules.put(
                AccessType.RemoteControl, AccessControlRule.AfterConfirmation);
        return settings;
    }

    public boolean isSessionRunning() {
        return mCurrentSession != null;
    }

    public void stopRunningSession() {
        sessionEnded();

        if (m_teamViewerSdk != null) {
            m_teamViewerSdk.shutdown();
            m_teamViewerSdk = null;
        }
    }

    public void onMicrophonePermissionGranted() {
        if (mCurrentSession != null) {
            mCurrentSession.onMicrophonePermissionGranted();
        }
    }

    private void sessionStarted(Context context, TeamViewerSession teamViewerSession) {
        mCurrentSession = teamViewerSession;
        RunningStateListener.SessionState sessionState = RunningStateListener.SessionState.ScreenSharing;

        if (teamViewerSession instanceof PilotSession) {
            sessionState = RunningStateListener.SessionState.Pilot;
            PilotSessionUI.INSTANCE.setCurrentSession((PilotSession) teamViewerSession);
            context.startActivity(PilotSessionUI.INSTANCE.createIntentForPilotSessionActivity(context));
        }

        if (mRunningStateListener != null) {
            mRunningStateListener.onRunningStateChange(sessionState);
        }
    }

    private void sessionEnded() {
        mCurrentSession = null;
        PilotSessionUI.INSTANCE.setCurrentSession(null);

        if (mRunningStateListener != null) {
            mRunningStateListener.onRunningStateChange(RunningStateListener.SessionState.NoSession);
        }
    }

    public void shareFiles(Uri... files) {
        if (mCurrentSession instanceof ScreenSharingSession) {
            ((ScreenSharingSession) mCurrentSession).shareFiles(files);
        }
    }

    /**
     * Callback to be invoked when the TeamViewer session is started or has
     * ended.
     */
    public interface RunningStateListener {

        enum SessionState {
            /**
             * No session is currently running.
             */
            NoSession,

            /**
             * A screen sharing session is currently running.
             */
            ScreenSharing,

            /**
             * A Pilot session is currently running.
             */
            Pilot
        }

        /**
         * Will be called on running state changes of the TeamViewer session.
         *
         * @param sessionState New session state.
         */
        void onRunningStateChange(@NonNull SessionState sessionState);
    }
}
